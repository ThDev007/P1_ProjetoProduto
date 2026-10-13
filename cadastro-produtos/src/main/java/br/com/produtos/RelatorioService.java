package br.com.produtos;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Relatórios gerados com a Stream API (filter, map, reduce, groupingBy...). */
public class RelatorioService {

    /** "Estoque baixo" = MENOS de 10 unidades. */
    public static final int LIMITE_ESTOQUE_BAIXO = 10;
    /** "Próximo ao vencimento" = vence nos próximos 60 dias. */
    public static final int DIAS_VENCIMENTO = 60;

    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String gerar(TipoRelatorio tipo, List<Produto> produtos) {
        if (produtos.isEmpty()) {
            return "Nenhum produto cadastrado.";
        }
        LocalDate hoje = LocalDate.now();
        return switch (tipo) {
            case PROXIMOS_VENCIMENTO -> proximosVencimento(produtos, hoje);
            case ESTOQUE_BAIXO -> estoqueBaixo(produtos);
            case MARGEM_MEDIA_POR_CATEGORIA -> margemMediaPorCategoria(produtos);
            case AGRUPADOS_POR_CATEGORIA -> agrupadosPorCategoria(produtos);
            case LISTA_GERAL -> listaGeral(produtos);
            case RESUMO_GERAL -> resumo(produtos, hoje);
        };
    }

    /** filter: validade entre hoje e hoje + 60 dias. */
    private String proximosVencimento(List<Produto> produtos, LocalDate hoje) {
        LocalDate limite = hoje.plusDays(DIAS_VENCIMENTO);

        String proximos = produtos.stream()
                .filter(p -> !p.getDataValidade().isBefore(hoje) && !p.getDataValidade().isAfter(limite))
                .sorted(Comparator.comparing(Produto::getDataValidade))
                .map(p -> linha(p) + "  -> vence em " + ChronoUnit.DAYS.between(hoje, p.getDataValidade()) + " dia(s)")
                .collect(Collectors.joining("\n"));

        String vencidos = produtos.stream()
                .filter(p -> p.getDataValidade().isBefore(hoje))
                .sorted(Comparator.comparing(Produto::getDataValidade))
                .map(p -> linha(p) + "  -> venceu em " + DATA.format(p.getDataValidade()))
                .collect(Collectors.joining("\n"));

        return "=== PRÓXIMOS AO VENCIMENTO (até " + DATA.format(limite) + ") ===\n"
                + (proximos.isEmpty() ? "Nenhum produto vence nos próximos " + DIAS_VENCIMENTO + " dias." : proximos)
                + "\n\n=== JÁ VENCIDOS ===\n"
                + (vencidos.isEmpty() ? "Nenhum produto vencido." : vencidos);
    }

    /** filter: quantidade < 10. */
    private String estoqueBaixo(List<Produto> produtos) {
        String corpo = produtos.stream()
                .filter(p -> p.getQuantidade() < LIMITE_ESTOQUE_BAIXO)
                .sorted(Comparator.comparingInt(Produto::getQuantidade))
                .map(this::linha)
                .collect(Collectors.joining("\n"));
        return "=== ESTOQUE BAIXO (menos de " + LIMITE_ESTOQUE_BAIXO + " unidades) ===\n"
                + (corpo.isEmpty() ? "Nenhum produto com estoque baixo." : corpo);
    }

    /** groupingBy + averagingDouble. Margem = (venda - compra) / venda. */
    private String margemMediaPorCategoria(List<Produto> produtos) {
        Map<Categoria, Double> mapa = produtos.stream()
                .collect(Collectors.groupingBy(Produto::getCategoria, TreeMap::new,
                        Collectors.averagingDouble(Produto::getMargemPercentual)));
        return "=== MARGEM DE LUCRO MÉDIA POR CATEGORIA ===\n"
                + "(margem = (venda - compra) / venda)\n"
                + mapa.entrySet().stream()
                        .map(e -> String.format("%-14s %6.2f%%", e.getKey(), e.getValue()))
                        .collect(Collectors.joining("\n"));
    }

    /** groupingBy: lista os produtos de cada categoria. */
    private String agrupadosPorCategoria(List<Produto> produtos) {
        Map<Categoria, List<Produto>> mapa = produtos.stream()
                .collect(Collectors.groupingBy(Produto::getCategoria, TreeMap::new, Collectors.toList()));
        return "=== PRODUTOS AGRUPADOS POR CATEGORIA ===\n\n" + mapa.entrySet().stream()
                .map(e -> "[" + e.getKey().toString().toUpperCase() + "] " + e.getValue().size() + " produto(s)\n"
                        + e.getValue().stream()
                                .sorted(Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER))
                                .map(p -> "   " + linha(p))
                                .collect(Collectors.joining("\n")))
                .collect(Collectors.joining("\n\n"));
    }

    private String listaGeral(List<Produto> produtos) {
        return "=== LISTA GERAL ===\n" + produtos.stream()
                .sorted(Comparator.comparing(Produto::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(this::linha)
                .collect(Collectors.joining("\n"));
    }

    /** reduce: soma valores; filter + count: contagens. */
    private String resumo(List<Produto> produtos, LocalDate hoje) {
        int totalUnidades = produtos.stream().mapToInt(Produto::getQuantidade).sum();
        BigDecimal valorVenda = produtos.stream()
                .map(Produto::getValorEmEstoque)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lucro = produtos.stream()
                .map(Produto::getLucroEmEstoque)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double margemGeral = produtos.stream().mapToDouble(Produto::getMargemPercentual).average().orElse(0);
        long baixos = produtos.stream().filter(p -> p.getQuantidade() < LIMITE_ESTOQUE_BAIXO).count();
        LocalDate limite = hoje.plusDays(DIAS_VENCIMENTO);
        long vencendo = produtos.stream()
                .filter(p -> !p.getDataValidade().isBefore(hoje) && !p.getDataValidade().isAfter(limite))
                .count();
        long vencidos = produtos.stream().filter(p -> p.getDataValidade().isBefore(hoje)).count();

        return "=== RESUMO GERAL ===\n"
                + "Produtos cadastrados   : " + produtos.size() + "\n"
                + "Unidades em estoque    : " + totalUnidades + "\n"
                + "Valor de venda total   : " + MOEDA.format(valorVenda) + "\n"
                + "Lucro potencial total  : " + MOEDA.format(lucro) + "\n"
                + String.format("Margem média geral     : %.2f%%%n", margemGeral)
                + "Com estoque baixo      : " + baixos + "\n"
                + "Vencendo em " + DIAS_VENCIMENTO + " dias    : " + vencendo + "\n"
                + "Já vencidos            : " + vencidos;
    }

    private String linha(Produto p) {
        return String.format("%-8s %-22s %-12s compra %10s venda %10s qtd: %3d  val: %s",
                p.getCodigo(), p.getNome(), p.getCategoria(),
                MOEDA.format(p.getPrecoCompra()), MOEDA.format(p.getPrecoVenda()),
                p.getQuantidade(), DATA.format(p.getDataValidade()));
    }
}
