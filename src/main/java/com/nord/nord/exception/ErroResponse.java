package com.nord.nord.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(int status, String mensagem, Map<String, String> erros) {

    public ErroResponse(int status, String mensagem) {
        this(status, mensagem, null);
    }
}
