package br.com.produtos;

public enum TipoRelatorio {
    PROXIMOS_VENCIMENTO("Produtos próximos ao vencimento (60 dias)"),
    ESTOQUE_BAIXO("Estoque baixo (menos de 10 unidades)"),
    MARGEM_MEDIA_POR_CATEGORIA("Margem de lucro média por categoria"),
    AGRUPADOS_POR_CATEGORIA("Produtos agrupados por categoria"),
    LISTA_GERAL("Lista geral (ordem alfabética)"),
    RESUMO_GERAL("Resumo geral");

    private final String rotulo;

    TipoRelatorio(String rotulo) {
        this.rotulo = rotulo;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
