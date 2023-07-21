package com.challenge.semana_7.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GetDepoimentosDto(
        @NotNull
        Long id,
        @NotBlank
        String nome,
        @NotBlank
        String depoimento,
        @NotBlank
        String fotoPath) {
}
