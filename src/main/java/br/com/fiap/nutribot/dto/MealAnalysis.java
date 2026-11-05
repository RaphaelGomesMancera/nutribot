package br.com.fiap.nutribot.dto;

import java.util.List;

// Formato do structured output de POST /meals/analyze (preenchido com .entity(...))
public record MealAnalysis(
        List<MealAnalysisItem> items,
        double totalCalories,
        double protein,
        double carbs,
        double fat
) {}
