package br.com.produtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Produto {
    private final String codigo;
    private final String nome;
    private final Categoria categoria;
    private final BigDecimal precoCompra;
    private final BigDecimal precoVenda;
    private final int quantidade;
    private final LocalDate dataFabricacao;
    private final LocalDate dataValidade;

    public Produto(String codigo, String nome, Categoria categoria, BigDecimal precoCompra,
                   BigDecimal precoVenda, int quantidade, LocalDate dataFabricacao, LocalDate dataValidade) {
        this.codigo = codigo;
        this.nome = nome;
        this.categoria = categoria;
        this.precoCompra = precoCompra;
        this.precoVenda = precoVenda;
        this.quantidade = quantidade;
        this.dataFabricacao = dataFabricacao;
        this.dataValidade = dataValidade;
    }

    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public Categoria getCategoria() { return categoria; }
    public BigDecimal getPrecoCompra() { return precoCompra; }
    public BigDecimal getPrecoVenda() { return precoVenda; }
    public int getQuantidade() { return quantidade; }
    public LocalDate getDataFabricacao() { return dataFabricacao; }
    public LocalDate getDataValidade() { return dataValidade; }

    /** Quanto o estoque atual vale se tudo for vendido. */
    public BigDecimal getValorEmEstoque() {
        return precoVenda.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Lucro por unidade (venda - compra). */
    public BigDecimal getLucroUnitario() {
        return precoVenda.subtract(precoCompra);
    }

    /** Lucro total se todo o estoque for vendido. */
    public BigDecimal getLucroEmEstoque() {
        return getLucroUnitario().multiply(BigDecimal.valueOf(quantidade));
    }

    /** Margem de lucro em %: (venda - compra) / venda * 100. */
    public double getMargemPercentual() {
        return getLucroUnitario().doubleValue() / precoVenda.doubleValue() * 100.0;
    }

    @Override
    public String toString() {
        return codigo + " - " + nome;
    }
}
