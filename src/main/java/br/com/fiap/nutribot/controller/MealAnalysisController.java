package br.com.fiap.nutribot.controller;

import br.com.fiap.nutribot.dto.AnalyzeRequest;
import br.com.fiap.nutribot.dto.MealAnalysis;
import br.com.fiap.nutribot.service.MealAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Estimativa de uma refeição pela IA (nada é gravado no diário)
@RestController
@RequestMapping("/meals/analyze")
@RequiredArgsConstructor
public class MealAnalysisController {

    private final MealAnalysisService service;

    @PostMapping
    public MealAnalysis analyze(@RequestBody @Valid AnalyzeRequest request) {
        return service.analyze(request.description());
    }
}
