package br.com.produtos;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Guarda os produtos em memória e persiste tudo em um arquivo CSV (separador ';').
 * Os dados são carregados ao iniciar e o arquivo é regravado após cada operação.
 */
public class ProdutoRepository {

    private static final String CABECALHO =
            "codigo;nome;categoria;precoCompra;precoVenda;quantidade;dataFabricacao;dataValidade";

    private final Path arquivo;
    private final List<Produto> produtos = new ArrayList<>();

    public ProdutoRepository() {
        this(Path.of("dados", "Produtos.csv"));
    }

    public ProdutoRepository(Path arquivo) {
        this.arquivo = arquivo;
    }

    public void carregar() throws PersistenciaException {
        produtos.clear();
        if (!Files.exists(arquivo)) {
            salvarArquivo(); // cria o arquivo com o cabeçalho
            return;
        }
        try {
            List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
            if (linhas.isEmpty()) {
                return;
            }
            String cabecalho = linhas.get(0).replace("\uFEFF", "").trim();
            if (!cabecalho.equals(CABECALHO)) {
                throw new PersistenciaException("O arquivo " + arquivo.toAbsolutePath()
                        + " está em um formato antigo ou inválido.\n"
                        + "Apague esse arquivo e abra o programa de novo (ele cria um novo).");
            }
            int numero = 1;
            for (String linha : linhas.stream().skip(1).collect(Collectors.toList())) {
                numero++;
                if (linha.isBlank()) {
                    continue;
                }
                String[] c = linha.split(";", -1);
                if (c.length < 8) {
                    throw new PersistenciaException("Linha " + numero + " do CSV está incompleta: " + linha);
                }
                try {
                    produtos.add(new Produto(
                            c[0].trim().toUpperCase(),
                            c[1].trim(),
                            Categoria.valueOf(c[2].trim()),
                            new BigDecimal(c[3].trim()),
                            new BigDecimal(c[4].trim()),
                            Integer.parseInt(c[5].trim()),
                            LocalDate.parse(c[6].trim()),
                            LocalDate.parse(c[7].trim())));
                } catch (RuntimeException e) {
                    throw new PersistenciaException("Linha " + numero + " do CSV é inválida: " + linha, e);
                }
            }
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível ler o arquivo " + arquivo, e);
        }
    }

    public List<Produto> listar() {
        return new ArrayList<>(produtos);
    }

    public boolean existeCodigo(String codigo) {
        return produtos.stream().anyMatch(p -> p.getCodigo().equalsIgnoreCase(codigo));
    }

    public void adicionar(Produto produto) throws PersistenciaException {
        produtos.add(produto);
        salvarArquivo();
    }

    public void atualizar(Produto produto) throws PersistenciaException {
        for (int i = 0; i < produtos.size(); i++) {
            if (produtos.get(i).getCodigo().equals(produto.getCodigo())) {
                produtos.set(i, produto);
                salvarArquivo();
                return;
            }
        }
        throw new PersistenciaException("Produto não encontrado: " + produto.getCodigo());
    }

    public void remover(String codigo) throws PersistenciaException {
        produtos.removeIf(p -> p.getCodigo().equals(codigo));
        salvarArquivo();
    }

    /** Salva uma cópia do CSV no caminho escolhido pelo usuário (com BOM, para o Excel ler os acentos). */
    public void exportarCsv(Path destino) throws PersistenciaException {
        List<String> linhas = montarLinhas();
        linhas.set(0, "\uFEFF" + linhas.get(0));
        try {
            Files.write(destino, linhas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível gravar o arquivo " + destino, e);
        }
    }

    private List<String> montarLinhas() {
        List<String> linhas = new ArrayList<>();
        linhas.add(CABECALHO);
        produtos.stream()
                .sorted(Comparator.comparing(Produto::getCodigo))
                .map(p -> String.join(";",
                        p.getCodigo(),
                        p.getNome(),
                        p.getCategoria().name(),
                        p.getPrecoCompra().toPlainString(),
                        p.getPrecoVenda().toPlainString(),
                        String.valueOf(p.getQuantidade()),
                        p.getDataFabricacao().toString(),
                        p.getDataValidade().toString()))
                .forEach(linhas::add);
        return linhas;
    }

    private void salvarArquivo() throws PersistenciaException {
        List<String> linhas = montarLinhas();
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            Files.write(arquivo, linhas, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível gravar o arquivo " + arquivo, e);
        }
    }
}
