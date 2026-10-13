package br.com.produtos;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Optional;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

public class ProdutoController {

    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private static final StringConverter<LocalDate> CONVERSOR_DATA = new StringConverter<>() {
        @Override
        public String toString(LocalDate data) {
            return data == null ? "" : DATA.format(data);
        }

        @Override
        public LocalDate fromString(String texto) {
            return texto == null || texto.isBlank() ? null : LocalDate.parse(texto.trim(), DATA);
        }
    };

    @FXML private TextField txtBusca;
    @FXML private TextField txtCodigo;
    @FXML private TextField txtNome;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private TextField txtPrecoCompra;
    @FXML private TextField txtPrecoVenda;
    @FXML private TextField txtQuantidade;
    @FXML private DatePicker dpFabricacao;
    @FXML private DatePicker dpValidade;
    @FXML private Button btnSalvar;
    @FXML private Button btnExcluir;

    @FXML private TableView<Produto> tabela;
    @FXML private TableColumn<Produto, String> colCodigo;
    @FXML private TableColumn<Produto, String> colNome;
    @FXML private TableColumn<Produto, Categoria> colCategoria;
    @FXML private TableColumn<Produto, BigDecimal> colPrecoCompra;
    @FXML private TableColumn<Produto, BigDecimal> colPrecoVenda;
    @FXML private TableColumn<Produto, Integer> colQuantidade;
    @FXML private TableColumn<Produto, LocalDate> colFabricacao;
    @FXML private TableColumn<Produto, LocalDate> colValidade;

    @FXML private ComboBox<TipoRelatorio> cmbRelatorio;
    @FXML private TextArea txtRelatorio;
    @FXML private Label lblStatus;

    private final ProdutoRepository repositorio = new ProdutoRepository();
    private final RelatorioService relatorioService = new RelatorioService();
    private final ObservableList<Produto> dados = FXCollections.observableArrayList();
    private FilteredList<Produto> filtrados;
    private Produto selecionado;

    @FXML
    private void initialize() {
        cmbCategoria.setItems(FXCollections.observableArrayList(Categoria.values()));
        cmbRelatorio.setItems(FXCollections.observableArrayList(TipoRelatorio.values()));
        cmbRelatorio.getSelectionModel().selectFirst();

        dpFabricacao.setConverter(CONVERSOR_DATA);
        dpValidade.setConverter(CONVERSOR_DATA);
        dpFabricacao.setPromptText("dd/mm/aaaa");
        dpValidade.setPromptText("dd/mm/aaaa");

        configurarColunas();

        filtrados = new FilteredList<>(dados, p -> true);
        SortedList<Produto> ordenados = new SortedList<>(filtrados);
        ordenados.comparatorProperty().bind(tabela.comparatorProperty());
        tabela.setItems(ordenados);

        txtBusca.textProperty().addListener((obs, antigo, novo) -> {
            String filtro = novo == null ? "" : novo.trim().toLowerCase();
            filtrados.setPredicate(p -> filtro.isEmpty()
                    || p.getNome().toLowerCase().contains(filtro)
                    || p.getCodigo().toLowerCase().contains(filtro)
                    || p.getCategoria().toString().toLowerCase().contains(filtro));
        });

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null) {
                preencherFormulario(novo);
            }
        });

        try {
            repositorio.carregar();
            dados.setAll(repositorio.listar());
            status(dados.size() + " produto(s) carregado(s) do CSV.");
        } catch (PersistenciaException e) {
            mostrarErro("Erro ao carregar dados", e.getMessage());
            status("Falha ao carregar o CSV.");
        }
        novo();
    }

    private void configurarColunas() {
        colCodigo.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCodigo()));
        colNome.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getNome()));
        colCategoria.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCategoria()));
        colPrecoCompra.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrecoCompra()));
        colPrecoVenda.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getPrecoVenda()));
        colQuantidade.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getQuantidade()));
        colFabricacao.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getDataFabricacao()));
        colValidade.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getDataValidade()));

        colPrecoCompra.setCellFactory(col -> celulaMoeda());
        colPrecoVenda.setCellFactory(col -> celulaMoeda());
        colFabricacao.setCellFactory(col -> celulaData());
        colValidade.setCellFactory(col -> celulaData());
    }

    private TableCell<Produto, BigDecimal> celulaMoeda() {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : MOEDA.format(item));
            }
        };
    }

    private TableCell<Produto, LocalDate> celulaData() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : DATA.format(item));
            }
        };
    }

    /** Lê a data digitada/escolhida; devolve null se estiver vazia ou inválida. */
    private LocalDate lerData(DatePicker seletor) {
        try {
            String texto = seletor.getEditor().getText();
            return texto == null || texto.isBlank() ? null : CONVERSOR_DATA.fromString(texto);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @FXML
    private void salvar() {
        try {
            boolean edicao = selecionado != null;
            String codigo = edicao ? selecionado.getCodigo() : txtCodigo.getText();

            Produto produto = Validador.validar(codigo, txtNome.getText(), cmbCategoria.getValue(),
                    txtPrecoCompra.getText(), txtPrecoVenda.getText(), txtQuantidade.getText(),
                    lerData(dpFabricacao), lerData(dpValidade), LocalDate.now(),
                    !edicao, repositorio::existeCodigo);

            if (edicao) {
                repositorio.atualizar(produto);
            } else {
                repositorio.adicionar(produto);
            }
            dados.setAll(repositorio.listar());
            status(edicao ? "Produto atualizado e salvo no CSV." : "Produto cadastrado e salvo no CSV.");
            novo();
        } catch (ValidacaoException e) {
            Alert alerta = new Alert(AlertType.WARNING);
            alerta.setTitle("Dados inválidos");
            alerta.setHeaderText("Corrija os campos abaixo:");
            alerta.setContentText(String.join("\n", e.getErros()));
            alerta.getDialogPane().setMinWidth(520);
            alerta.showAndWait();
        } catch (PersistenciaException e) {
            mostrarErro("Erro ao gravar dados", e.getMessage());
        }
    }

    @FXML
    private void novo() {
        selecionado = null;
        tabela.getSelectionModel().clearSelection();
        txtCodigo.clear();
        txtCodigo.setDisable(false);
        txtNome.clear();
        txtPrecoCompra.clear();
        txtPrecoVenda.clear();
        txtQuantidade.clear();
        cmbCategoria.getSelectionModel().clearSelection();
        dpFabricacao.setValue(null);
        dpFabricacao.getEditor().clear();
        dpValidade.setValue(null);
        dpValidade.getEditor().clear();
        btnSalvar.setText("Cadastrar");
        btnExcluir.setDisable(true);
        txtCodigo.requestFocus();
    }

    @FXML
    private void excluir() {
        if (selecionado == null) {
            return;
        }
        Alert confirma = new Alert(AlertType.CONFIRMATION,
                "Excluir o produto \"" + selecionado.getNome() + "\"?", ButtonType.YES, ButtonType.NO);
        confirma.setTitle("Confirmar exclusão");
        confirma.setHeaderText(null);
        Optional<ButtonType> resposta = confirma.showAndWait();
        if (resposta.isPresent() && resposta.get() == ButtonType.YES) {
            try {
                repositorio.remover(selecionado.getCodigo());
                dados.setAll(repositorio.listar());
                status("Produto excluído e CSV atualizado.");
                novo();
            } catch (PersistenciaException e) {
                mostrarErro("Erro ao excluir", e.getMessage());
            }
        }
    }

    @FXML
    private void exportar() {
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Salvar produtos em arquivo");
        escolha.setInitialFileName("Produtos.csv");
        File inicial = new File(System.getProperty("user.home"));
        if (inicial.isDirectory()) {
            escolha.setInitialDirectory(inicial);
        }
        escolha.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Planilha CSV (*.csv)", "*.csv"),
                new FileChooser.ExtensionFilter("Texto (*.txt)", "*.txt"));

        File destino = escolha.showSaveDialog(tabela.getScene().getWindow());
        if (destino == null) {
            return; // usuário cancelou
        }
        try {
            String nome = destino.getName().toLowerCase();
            if (nome.endsWith(".txt")) {
                String texto = relatorioService.gerar(TipoRelatorio.LISTA_GERAL, repositorio.listar());
                Files.writeString(destino.toPath(), texto, StandardCharsets.UTF_8);
            } else {
                if (!nome.endsWith(".csv")) {
                    destino = new File(destino.getParentFile(), destino.getName() + ".csv");
                }
                repositorio.exportarCsv(destino.toPath());
            }
        } catch (PersistenciaException | IOException e) {
            mostrarErro("Erro ao salvar o arquivo", e.getMessage());
            return;
        }

        status("Arquivo salvo em " + destino.getAbsolutePath());
        Alert confirma = new Alert(AlertType.CONFIRMATION,
                "Arquivo salvo em:\n" + destino.getAbsolutePath() + "\n\nDeseja abrir agora?",
                ButtonType.YES, ButtonType.NO);
        confirma.setTitle("Arquivo salvo");
        confirma.setHeaderText(null);
        Optional<ButtonType> resposta = confirma.showAndWait();
        if (resposta.isPresent() && resposta.get() == ButtonType.YES) {
            abrirArquivo(destino);
        }
    }

    private void abrirArquivo(File arquivo) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(arquivo);
            }
        } catch (IOException | RuntimeException e) {
            mostrarErro("Não foi possível abrir o arquivo", e.getMessage());
        }
    }

    @FXML
    private void gerarRelatorio() {
        TipoRelatorio tipo = cmbRelatorio.getValue();
        if (tipo == null) {
            return;
        }
        txtRelatorio.setText(relatorioService.gerar(tipo, repositorio.listar()));
        status("Relatório gerado: " + tipo);
    }

    private void preencherFormulario(Produto p) {
        selecionado = p;
        txtCodigo.setText(p.getCodigo());
        txtCodigo.setDisable(true); // o código identifica o produto e não pode mudar
        txtNome.setText(p.getNome());
        cmbCategoria.setValue(p.getCategoria());
        txtPrecoCompra.setText(p.getPrecoCompra().toPlainString().replace('.', ','));
        txtPrecoVenda.setText(p.getPrecoVenda().toPlainString().replace('.', ','));
        txtQuantidade.setText(String.valueOf(p.getQuantidade()));
        dpFabricacao.setValue(p.getDataFabricacao());
        dpValidade.setValue(p.getDataValidade());
        btnSalvar.setText("Atualizar");
        btnExcluir.setDisable(false);
    }

    private void status(String mensagem) {
        lblStatus.setText(mensagem);
    }

    private void mostrarErro(String titulo, String mensagem) {
        Alert alerta = new Alert(AlertType.ERROR);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensagem);
        alerta.getDialogPane().setMinWidth(520);
        alerta.showAndWait();
    }
}
