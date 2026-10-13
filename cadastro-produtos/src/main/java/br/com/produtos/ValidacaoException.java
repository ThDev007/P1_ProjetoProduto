package br.com.produtos;

import java.util.List;

public class ValidacaoException extends Exception {
    private final List<String> erros;

    public ValidacaoException(List<String> erros) {
        super(String.join("\n", erros));
        this.erros = erros;
    }

    public List<String> getErros() {
        return erros;
    }
}
