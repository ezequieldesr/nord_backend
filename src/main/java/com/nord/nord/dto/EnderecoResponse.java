package com.nord.nord.dto;

public record EnderecoResponse(Long id, String cep, String estado, String cidade, String bairro,
                               String rua, Integer numero, String complemento) {
}
