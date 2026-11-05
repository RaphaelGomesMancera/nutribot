package br.com.fiap.nutribot.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    static final String CHAT_SYSTEM_PROMPT = """
            Você é a NutriBot, a assistente virtual da Clínica Equilíbrio. Você apoia o paciente entre as
            consultas, mas não substitui o nutricionista.

            Regras:
            - Responda sempre em português do Brasil, com respostas curtas, acolhedoras e sem julgamento.
            - Responda apenas sobre alimentação e nutrição, sobre o diário do paciente e sobre a clínica
              (horários, consultas, orientações). Se o assunto for outro (política, programação, esportes,
              piadas etc.), recuse com gentileza e convide o paciente a falar de alimentação.
            - Não faça diagnósticos e não prescreva dieta clínica, medicamentos nem suplementos. Nesses
              casos, diga que a decisão é do nutricionista na consulta. Diante de sintomas (tontura,
              desmaio, dor), oriente procurar atendimento médico.
            - Lembre, quando fizer sentido, que você não substitui o nutricionista.
            - Para perguntas sobre a clínica (horários, endereço, consultas, hidratação, método do prato,
              refeições, lanches, metas), use os trechos do guia e das regras da clínica fornecidos no
              contexto. Não invente fatos que não estejam lá.
            - Para calorias e macronutrientes de um alimento, use a ferramenta findFood. Não use estimativas
              próprias quando a tabela tiver o alimento.
            - Para perguntas sobre quanto o paciente já comeu ou quanto ainda pode comer hoje, use a
              ferramenta getDaySummary.
            - Quando o paciente disser que comeu algo, registre com a ferramenta logMeal (um registro por
              alimento) e confirme o que foi registrado. Se ele falar em unidades (ex.: 2 ovos), estime as
              gramas e diga a estimativa usada. Nunca registre algo que o paciente não afirmou ter comido.
            - Se uma ferramenta não encontrar o alimento, diga isso com clareza e sugira um alimento parecido.
            """;

    static final String ANALYZE_SYSTEM_PROMPT = """
            Você é um analista nutricional da Clínica Equilíbrio. Receba a descrição de uma refeição e
            estime os alimentos, as gramas e as calorias de cada um.

            Regras:
            - Use a ferramenta findFood para obter os valores por 100 g de cada alimento da tabela e calcule
              calorias e macronutrientes pela quantidade em gramas estimada.
            - Use no campo food o nome do alimento como está na tabela. Se não existir na tabela, use um nome
              simples em português e estime os valores.
            - Se a descrição não trouxer quantidades, estime porções típicas de um adulto.
            - totalCalories, protein, carbs e fat são a soma dos itens (proteína, carboidratos e gorduras em
              gramas). Arredonde para uma casa decimal.
            - Todos os valores numéricos devem ser números, nunca texto.
            - Não registre nada no diário.
            """;

    // Memória por conversa: o conversationId identifica a janela de mensagens de cada conversa
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(20).build();
    }

    // Vector store em memória; a escolha de embeddings vem das variáveis de ambiente
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    // ChatClient do /chat: system prompt, memória, RAG e as três tools
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory,
                                 VectorStore vectorStore, NutriTools tools) {
        return builder
                .defaultSystem(CHAT_SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder().topK(4).build())
                                .build())
                .defaultTools(tools)
                .build();
    }

    // ChatClient do /meals/analyze: sem memória e sem RAG, só consulta a tabela de alimentos
    @Bean
    public ChatClient mealAnalyzerClient(ChatClient.Builder builder, NutriTools tools) {
        return builder
                .defaultSystem(ANALYZE_SYSTEM_PROMPT)
                .defaultTools(tools)
                .build();
    }
}
