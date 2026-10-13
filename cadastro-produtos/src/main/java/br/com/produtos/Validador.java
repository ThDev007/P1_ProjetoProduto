package br.com.produtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class Validador {

    private static final Pattern CODIGO = Pattern.compile("[A-Za-z0-9]{8}");
    private static final int NOME_MINIMO = 3;
    private static final int NOME_MAXIMO = 60;

    private Validador() {
    }

    /**
     * Valida os dados digitados e devolve um Produto pronto.
     * Se houver erros, lança ValidacaoException com todas as mensagens.
     *
     * @param novo         true se for um cadastro novo (verifica código repetido)
     * @param codigoExiste diz se um código já está cadastrado
     */
    public static Produto validar(String codigo, String nome, Categoria categoria,
                                  String precoCompraTxt, String precoVendaTxt, String quantidadeTxt,
                                  LocalDate fabricacao, LocalDate validade, LocalDate hoje,
                                  boolean novo, Predicate<String> codigoExiste) throws ValidacaoException {
        List<String> erros = new ArrayList<>();

        // Código: 8 caracteres alfanuméricos
        String cod = codigo == null ? "" : codigo.trim().toUpperCase();
        if (!CODIGO.matcher(cod).matches()) {
            erros.add("O código deve ter exatamente 8 caracteres alfanuméricos (letras e números, sem espaços).");
        } else if (novo && codigoExiste.test(cod)) {
            erros.add("Já existe um produto com o código " + cod + ".");
        }

        // Nome: não vazio e com tamanho mínimo
        String nomeLimpo = nome == null ? "" : nome.trim();
        if (nomeLimpo.isEmpty()) {
            erros.add("O nome é obrigatório.");
        } else if (nomeLimpo.length() < NOME_MINIMO) {
            erros.add("O nome deve ter no mínimo " + NOME_MINIMO + " caracteres.");
        } else if (nomeLimpo.length() > NOME_MAXIMO) {
            erros.add("O nome deve ter no máximo " + NOME_MAXIMO + " caracteres.");
        } else if (nomeLimpo.contains(";") || nomeLimpo.contains("\n")) {
            erros.add("O nome não pode conter ponto e vírgula nem quebra de linha.");
        }

        if (categoria == null) {
            erros.add("Selecione uma categoria.");
        }

        // Preços: positivos, venda > compra
        BigDecimal compra = lerPreco(precoCompraTxt, "compra", erros);
        BigDecimal venda = lerPreco(precoVendaTxt, "venda", erros);
        if (compra != null && venda != null && venda.compareTo(compra) <= 0) {
            erros.add("O preço de venda deve ser maior que o preço de compra.");
        }

        // Quantidade: não negativa
        int quantidade = 0;
        String q = quantidadeTxt == null ? "" : quantidadeTxt.trim();
        if (q.isEmpty()) {
            erros.add("A quantidade é obrigatória.");
        } else {
            try {
                quantidade = Integer.parseInt(q);
                if (quantidade < 0) {
                    erros.add("A quantidade em estoque não pode ser negativa.");
                }
            } catch (NumberFormatException e) {
                erros.add("Quantidade inválida. Use apenas números inteiros.");
            }
        }

        // Datas
        if (fabricacao == null) {
            erros.add("Informe uma data de fabricação válida (dd/mm/aaaa).");
        } else if (fabricacao.isAfter(hoje)) {
            erros.add("A data de fabricação não pode ser posterior à data atual.");
        }
        if (validade == null) {
            erros.add("Informe uma data de validade válida (dd/mm/aaaa).");
        } else if (fabricacao != null && validade.isBefore(fabricacao)) {
            erros.add("A data de validade não pode ser anterior à data de fabricação.");
        }

        if (!erros.isEmpty()) {
            throw new ValidacaoException(erros);
        }
        return new Produto(cod, nomeLimpo, categoria, compra, venda, quantidade, fabricacao, validade);
    }

    /** Lê um preço (aceita 1.234,56 ou 1234,56 ou 1234.56). Devolve null e registra o erro se for inválido. */
    private static BigDecimal lerPreco(String texto, String rotulo, List<String> erros) {
        String p = texto == null ? "" : texto.trim().replace("R$", "").trim();
        if (p.isEmpty()) {
            erros.add("O preço de " + rotulo + " é obrigatório.");
            return null;
        }
        try {
            if (p.contains(",")) {
                p = p.replace(".", "").replace(",", ".");
            }
            BigDecimal valor = new BigDecimal(p);
            if (valor.compareTo(BigDecimal.ZERO) <= 0) {
                erros.add("O preço de " + rotulo + " deve ser maior que zero.");
                return null;
            }
            if (valor.scale() > 2) {
                erros.add("O preço de " + rotulo + " deve ter no máximo 2 casas decimais.");
                return null;
            }
            return valor;
        } catch (NumberFormatException e) {
            erros.add("Preço de " + rotulo + " inválido. Exemplo: 19,90");
            return null;
        }
    }
}
