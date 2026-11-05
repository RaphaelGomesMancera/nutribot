package br.com.fiap.nutribot.service;

import br.com.fiap.nutribot.dto.MealAnalysis;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class MealAnalysisService {

    private final ChatClient analyzerClient;

    public MealAnalysisService(@Qualifier("mealAnalyzerClient") ChatClient analyzerClient) {
        this.analyzerClient = analyzerClient;
    }

    // structured output: o Spring AI converte a resposta do modelo direto para os records
    public MealAnalysis analyze(String description) {
        return analyzerClient.prompt()
                .user(description)
                .call()
                .entity(MealAnalysis.class);
    }
}
