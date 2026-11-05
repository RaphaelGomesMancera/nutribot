package br.com.fiap.nutribot.dto;

// Um alimento estimado pelo modelo: nome, gramas e calorias (números, não texto)
public record MealAnalysisItem(String food, double grams, double calories) {}
