package br.com.fiap.nutribot.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

// RAG: na subida, lê os documentos da clínica (resources/docs), divide por seção e grava no vector store
@Slf4j
@Component
public class DocumentLoader implements ApplicationRunner {

    private final VectorStore vectorStore;

    public DocumentLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        List<Document> documents = new ArrayList<>();
        for (Resource resource : new PathMatchingResourcePatternResolver().getResources("classpath:docs/*.md")) {
            String file = resource.getFilename();
            documents.addAll(split(file, resource.getContentAsString(StandardCharsets.UTF_8)));
        }
        try {
            vectorStore.add(documents);
            log.info("RAG: {} trechos dos documentos da clínica carregados no vector store", documents.size());
        } catch (RuntimeException ex) {
            // sem acesso ao modelo de embeddings a API ainda sobe; só o RAG fica sem contexto
            log.warn("RAG: não foi possível gerar os embeddings (confira as variáveis de ambiente): {}", ex.getMessage());
        }
    }

    // um trecho por seção "## ", com o título do documento e da seção no começo para ajudar a busca
    private List<Document> split(String file, String content) {
        List<Document> sections = new ArrayList<>();
        String title = file;
        String heading = "";
        StringBuilder body = new StringBuilder();
        for (String line : content.split("\\R")) {
            if (line.startsWith("# ")) {
                title = line.substring(2).trim();
            } else if (line.startsWith("## ")) {
                addSection(sections, file, title, heading, body);
                heading = line.substring(3).trim();
                body = new StringBuilder();
            } else {
                body.append(line).append('\n');
            }
        }
        addSection(sections, file, title, heading, body);
        return sections;
    }

    private void addSection(List<Document> sections, String file, String title, String heading, StringBuilder body) {
        String text = body.toString().trim();
        if (text.isEmpty()) {
            return;
        }
        String full = title + " — " + (heading.isEmpty() ? "Introdução" : heading) + "\n" + text;
        sections.add(new Document(full, Map.of("source", file, "section", heading)));
    }
}
