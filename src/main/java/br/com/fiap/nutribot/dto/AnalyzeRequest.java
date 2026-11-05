package br.com.fiap.nutribot.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalyzeRequest(
        @NotBlank(message = "description is required") String description
) {}
