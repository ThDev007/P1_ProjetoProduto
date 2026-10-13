package br.com.produtos;

public enum Categoria {
    ALIMENTOS("Alimentos"),
    ELETRONICOS("Eletrônicos"),
    VESTUARIO("Vestuário"),
    LIVROS("Livros"),
    HIGIENE("Higiene"),
    OUTROS("Outros");

    private final String rotulo;

    Categoria(String rotulo) {
        this.rotulo = rotulo;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
