package com.challenge.semana_7.dto;

import jakarta.validation.constraints.NotBlank;

public record UploadDepoimentosDto(
        @NotBlank
        String nome,
        @NotBlank
        String depoimento,
        String fotoPath) {
}
