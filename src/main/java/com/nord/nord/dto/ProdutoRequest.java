package com.nord.nord.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProdutoRequest(
        @NotBlank @Size(max = 150) String nome,
        @Size(max = 1000) String descricao,
        @Size(max = 1000)
        @Pattern(regexp = "^(https?://.*|/api/catalogo/imagens/[a-z0-9-]+\\.png)?$", message = "use uma URL HTTP(S) ou imagem do catálogo Nord") String imagem,
        @NotNull @Positive Double preco,
        @NotNull @Min(0) Integer quantidade,
        @Size(max = 100) String tamanho,
        @NotNull Long categoriaId) {
}
