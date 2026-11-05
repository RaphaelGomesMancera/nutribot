# NutriBot API

Projeto **Diamante — Java Advanced (segundo semestre, FIAP)**.

A **NutriBot** é a nutricionista virtual da **Clínica Equilíbrio**. A API conversa em português com o paciente, conhece a tabela de alimentos e o diário alimentar, registra refeições pelo chat e responde com base no guia da clínica. Foi construída com **Spring Boot** e **Spring AI** em cima do projeto base fornecido pelo professor.

## Equipe

| Nome | RM |
|---|---|
| Guilherme de Andrade Martini | RM566087 |
| Nathan Gonçalves Pereira Mendes | RM564666 |
| Raphael Gomes Mancera | RM562279 |

## Modelo usado

| Uso | Modelo |
|---|---|
| Chat | **llama-3.3-70b-versatile (Groq)** |
| Embeddings (RAG) | **gemini-embedding-001 (Google AI Studio)** |

O projeto usa o starter da OpenAI do Spring AI, que funciona com qualquer API compatível com a da OpenAI. O modelo é definido só por variáveis de ambiente.

## Tecnologias

- Java 21+ e Maven
- Spring Boot 4 (Web MVC, Data JPA, Validation)
- Spring AI 2 (`ChatClient`, `ChatMemory`, `@Tool`, `QuestionAnswerAdvisor`, `SimpleVectorStore`)
- H2 em memória (dados carregados a cada inicialização pelo `DataLoader`)
- Lombok

## Como rodar

Requisitos: Java 21+ e Maven (ou o `mvnw` da IDE).

```bash
# 1. defina as variáveis de ambiente do modelo (veja a seção abaixo)
# 2. suba a API
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`. O arquivo `test.http` tem exemplos de todas as rotas, inclusive as de IA.

No IntelliJ, as variáveis podem ser definidas em *Run → Edit Configurations → Environment variables*.

## Configurando o modelo

**Nenhuma chave de API fica no código ou no repositório.** Tudo vem de variáveis de ambiente lidas pelo `application.yml`.

| Variável | Padrão | Para que serve |
|---|---|---|
| `OPENAI_API_KEY` | — | Chave da API |
| `OPENAI_BASE_URL` | `https://api.openai.com` | Endereço da API (sem `/v1`) |
| `OPENAI_MODEL` | `gpt-4o-mini` | Modelo de chat |
| `OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` | Modelo de embeddings (RAG) |
| `EMBEDDING_API_KEY` | igual a `OPENAI_API_KEY` | Chave dos embeddings, se o provedor for outro |
| `EMBEDDING_BASE_URL` | igual a `OPENAI_BASE_URL` | Endereço dos embeddings, se o provedor for outro |
| `OPENAI_COMPLETIONS_PATH` | `/v1/chat/completions` | Caminho do chat |
| `OPENAI_EMBEDDINGS_PATH` | `/v1/embeddings` | Caminho dos embeddings |

Exemplos:

```bash
# OpenAI
export OPENAI_API_KEY=sk-...

# Ollama local (ollama pull llama3.2 && ollama pull nomic-embed-text)
export OPENAI_API_KEY=ollama
export OPENAI_BASE_URL=http://localhost:11434
export OPENAI_MODEL=llama3.2
export OPENAI_EMBEDDING_MODEL=nomic-embed-text

# Chat na Groq + embeddings no Gemini (a Groq não oferece embeddings)
export OPENAI_API_KEY=sua-chave-da-groq
export OPENAI_BASE_URL=https://api.groq.com/openai
export OPENAI_MODEL=llama-3.3-70b-versatile
export EMBEDDING_API_KEY=sua-chave-do-ai-studio
export EMBEDDING_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
export OPENAI_EMBEDDINGS_PATH=/embeddings
export OPENAI_EMBEDDING_MODEL=gemini-embedding-001

# Google Gemini (endpoint compatível com OpenAI)
export OPENAI_API_KEY=sua-chave-do-ai-studio
export OPENAI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
export OPENAI_COMPLETIONS_PATH=/chat/completions
export OPENAI_EMBEDDINGS_PATH=/embeddings
export OPENAI_MODEL=gemini-2.5-flash
export OPENAI_EMBEDDING_MODEL=gemini-embedding-001
```

## Rotas

### Implementadas pela equipe (IA)

| Método | Rota | Respostas |
|---|---|---|
| POST | `/chat` | 200 `{answer}` · 400 |
| POST | `/meals/analyze` | 200 estimativa · 400 |

### Já vinham no projeto base

| Método | Rota | Descrição |
|---|---|---|
| GET | `/foods?name=` | Busca na tabela de alimentos (valores por 100 g) |
| GET | `/foods/{id}` | Um alimento |
| GET | `/meals?date=` | Refeições do dia (sem `date`, hoje) |
| POST | `/meals` | Registra uma refeição `{date?, mealType, foodId, grams}` → 201 |
| DELETE | `/meals/{id}` | Remove uma refeição → 204 |
| GET | `/summary?date=` | Resumo do dia: calorias, macros, meta e o que falta |
| GET · PUT | `/profile` | Perfil do paciente |

- `mealType`: `BREAKFAST`, `LUNCH`, `DINNER` ou `SNACK`.
- `goal`: `LOSE_WEIGHT`, `MAINTAIN` ou `GAIN_MUSCLE`.
- Todos os erros saem no formato `{"error": "..."}`.

### Exemplos

`POST /chat`

```json
// requisição
{ "conversationId": "app-m1x2-ab12", "message": "Qual é o meu nome?" }

// resposta
{ "answer": "Seu nome é Rafael!" }
```

`POST /meals/analyze`

```json
// requisição
{ "description": "arroz, feijão e bife" }

// resposta
{
  "items": [
    { "food": "Arroz branco", "grams": 150, "calories": 192 }
  ],
  "totalCalories": 557,
  "protein": 48.2,
  "carbs": 56.0,
  "fat": 14.1
}
```

## O que a equipe implementou com Spring AI

### 1. Chat e system prompt

`ChatController` → `ChatService` → `ChatClient`. O `ChatClient` é montado **uma vez** com o builder em `AiConfig`, com o system prompt da persona:

- NutriBot, da Clínica Equilíbrio, responde sempre em português;
- recusa assuntos que não sejam nutrição;
- não faz diagnóstico nem prescreve dieta clínica, medicamentos ou suplementos;
- lembra que não substitui o nutricionista;
- usa a tabela de alimentos e o diário para falar de calorias, e o guia da clínica para o resto.

### 2. Memória de conversa

`MessageWindowChatMemory` (janela das últimas 20 mensagens) com o `MessageChatMemoryAdvisor`. O `conversationId` da requisição é passado ao advisor em `ChatService`, então **duas conversas não se misturam**.

Como testar: envie "Meu nome é Rafael" e depois "Qual é o meu nome?" com o mesmo `conversationId`; com outro `conversationId`, o nome não aparece.

### 3. Tool calling

A classe `NutriTools` expõe três métodos com `@Tool`, que reaproveitam os services do projeto base e estão registrados no `ChatClient` do `/chat`:

| Tool | Quando é usada | Service |
|---|---|---|
| `findFood(name)` | "Quantas calorias tem 100 g de tapioca?" | `FoodService` |
| `getDaySummary()` | "Quantas calorias ainda posso comer hoje?" | `MealService.summary` |
| `logMeal(foodName, grams, mealType)` | "Comi 2 ovos no café" grava no diário | `MealService.log` |

A refeição registrada pelo chat aparece no diário (`GET /meals`) e no resumo (`GET /summary`).

### 4. RAG com os documentos da clínica

`DocumentLoader` roda na subida da aplicação: lê os arquivos de `src/main/resources/docs` (guia alimentar e regras de atendimento), divide cada um por seção (`##`) e grava os trechos em um `SimpleVectorStore`. O vector store é ligado ao `/chat` com o `QuestionAnswerAdvisor` (top 4 trechos por pergunta).

- **Vector store:** `SimpleVectorStore` (em memória).
- **Embeddings:** o modelo definido em `OPENAI_EMBEDDING_MODEL`.
- Se o modelo de embeddings não estiver acessível, a API ainda sobe e registra um aviso no log; só o RAG fica sem contexto.

Como testar (perguntas que só os documentos respondem): "Quantos copos de água por dia a clínica recomenda?" e "Qual o horário de atendimento?".

### 5. Structured output

`POST /meals/analyze` recebe `{description}` e devolve a estimativa em records Java, preenchidos com `.entity(MealAnalysis.class)`, sem parse manual de texto e com números como números:

- `MealAnalysis(items, totalCalories, protein, carbs, fat)`
- `MealAnalysisItem(food, grams, calories)`

Esse endpoint usa um `ChatClient` próprio, sem memória e sem RAG, que consulta a tabela de alimentos pela tool `findFood`. **Nada é gravado no diário.**

## Estrutura do projeto

```text
src/main/java/br/com/fiap/nutribot
├── ai/            AiConfig, NutriTools, DocumentLoader   (IA: equipe)
├── controller/    ChatController, MealAnalysisController (equipe) + rotas do projeto base
├── service/       ChatService, MealAnalysisService (equipe) + services do projeto base
├── dto/           ChatRequest/Response, AnalyzeRequest, MealAnalysis(+Item) (equipe) + DTOs do base
├── exception/     GlobalExceptionHandler  → erros no formato {"error": "..."}
├── model/         Food, MealEntry, Profile, MealType, Goal
├── repository/    FoodRepository, MealEntryRepository, ProfileRepository
├── config/        WebConfig (CORS para o app)
└── data/          DataLoader (66 alimentos, perfil e refeições de hoje e ontem)
src/main/resources
├── application.yml
└── docs/          guia-alimentar.md, regras-atendimento.md  (base do RAG)
```

## Tratamento de erros

- Validação (`@NotBlank` em `conversationId`, `message` e `description`) → **400** `{"error": "..."}`.
- JSON inválido ou tipo errado → **400** `{"error": "Invalid request"}`.
- Falha ao falar com o modelo de IA (chave ausente, API fora do ar, resposta inválida) → **502** `{"error": "AI service unavailable"}`, com o detalhe no log.

## Validando com o app

O app mobile que valida o contrato está em https://github.com/joaocarloslima/nutribot-app. Com a API rodando na porta 8080, o app deve funcionar sem ajustes: chat com memória ("Nova conversa" gera um novo `conversationId`), diário, resumo, perfil e análise de refeição.

## Checklist de entrega

- [x] `POST /chat` com system prompt e `ChatClient`
- [x] Memória separada por `conversationId`
- [x] Três tools com `@Tool` usando os services
- [x] RAG com os documentos de `resources/docs`
- [x] `POST /meals/analyze` com structured output
- [x] Nenhuma chave de API no código (somente variáveis de ambiente)
- [ ] Nome e RM de todos os integrantes conferidos neste README
- [ ] Modelo usado preenchido neste README
- [ ] Link do repositório enviado no geminidev
