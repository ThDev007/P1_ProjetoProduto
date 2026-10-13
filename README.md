# P1_ProjetoProduto

# Cadastro de Produtos

Aplicação desktop em **JavaFX + Maven** para cadastrar produtos, com validação de dados, persistência em arquivo CSV e relatórios feitos com a **Stream API** do Java.

## Funcionalidades

- Cadastro, edição e exclusão de produtos
- Busca por código, nome ou categoria
- Dados salvos em CSV (separador `;`), carregados ao iniciar e regravados após cada operação
- Relatórios com Stream API
- Exportação dos dados para um arquivo `.csv` ou `.txt` escolhido pelo usuário

## Requisitos

- **JDK 17 ou superior** (recomendado JDK 21)
- **IntelliJ IDEA** (a versão Community serve). O Maven já vem embutido
- **Scene Builder** (opcional, só para editar a tela)
- Internet na primeira execução, para o Maven baixar o JavaFX

## Como iniciar o projeto

### 1. Obter o projeto

- Por ZIP: descompacte o `cadastro-produtos.zip`
- Por Git: `git clone <endereço-do-repositório>`

Confira se a pasta contém `pom.xml`, `src`, `dados` e este `README.md`. Cuidado com pasta dentro de pasta ao descompactar.

### 2. Abrir no IntelliJ

1. **File → Open**
2. Escolha a pasta onde está o `pom.xml`, selecione o próprio **`pom.xml`** e clique em **Open as Project**
3. Confirme **Trust Project**
4. Espere a barra de progresso de baixo terminar (ela baixa o JavaFX)

Se algo parecer errado: botão direito no `pom.xml` → **Maven → Reload project**.

### 3. Configurar o JDK

**File → Project Structure → Project** e selecione um JDK 17 ou superior em *SDK*.

### 4. Executar

Abra `src/main/java/br/com/produtos/Launcher.java`, clique com o botão direito e escolha **Run 'Launcher.main()'**.

> Rode sempre o `Launcher`, e não a `App`. Rodar a `App` direto causa o erro `JavaFX runtime components are missing`.

Alternativas:

- Aba **Maven → Plugins → javafx → javafx:run**
- Terminal, na pasta do projeto: `mvn clean javafx:run`

Os avisos `WARNING` em vermelho no console (principalmente com JDK 24) são normais e podem ser ignorados.

## Estrutura do projeto

```
cadastro-produtos/
├── pom.xml
├── README.md
├── dados/
│   └── Produtos.csv              # dados dos produtos
└── src/main/
    ├── java/br/com/produtos/
    │   ├── Launcher.java         # classe para executar pelo IntelliJ
    │   ├── App.java              # inicia o JavaFX e carrega o FXML
    │   ├── ProdutoController.java
    │   ├── Produto.java          # modelo
    │   ├── Categoria.java        # enum de categorias
    │   ├── Validador.java        # regras de validação
    │   ├── ValidacaoException.java
    │   ├── ProdutoRepository.java  # leitura e gravação do CSV
    │   ├── PersistenciaException.java
    │   ├── RelatorioService.java   # relatórios com Stream API
    │   └── TipoRelatorio.java
    └── resources/br/com/produtos/
        └── Main.fxml             # tela (editável no Scene Builder)
```

## Regras de validação

| Campo | Regra |
|---|---|
| Código | Exatamente 8 caracteres alfanuméricos e único |
| Nome | Não vazio, mínimo de 3 caracteres |
| Data de fabricação | Não pode ser posterior à data atual |
| Data de validade | Não pode ser anterior à data de fabricação |
| Preços | Positivos, e o preço de venda deve ser maior que o de compra |
| Quantidade em estoque | Não negativa |

## Persistência

- Arquivo: `dados/Produtos.csv`, com campos separados por ponto e vírgula
- Colunas: `codigo;nome;categoria;precoCompra;precoVenda;quantidade;dataFabricacao;dataValidade`
- O arquivo é carregado ao iniciar e regravado após cada cadastro, edição ou exclusão
- Se o arquivo não existir, o programa cria um novo
- Se o arquivo estiver em um formato antigo, o programa avisa. Apague-o para que um novo seja criado

## Relatórios (Stream API)

| Relatório | Operações Stream |
|---|---|
| Produtos próximos ao vencimento (60 dias) | `filter`, `sorted`, `map` |
| Estoque baixo (menos de 10 unidades) | `filter`, `sorted`, `map` |
| Margem de lucro média por categoria | `groupingBy`, `averagingDouble` |
| Produtos agrupados por categoria | `groupingBy` |
| Lista geral | `sorted`, `map` |
| Resumo geral | `reduce`, `mapToInt`, `filter`, `count` |

A margem de lucro é calculada como `(venda - compra) / venda`.

## Editando a tela com o Scene Builder

Abra `src/main/resources/br/com/produtos/Main.fxml`. Não altere os `fx:id` nem os `onAction` (`#salvar`, `#novo`, `#excluir`, `#exportar`, `#gerarRelatorio`) sem alterar também o `ProdutoController`.

## Problemas comuns

| Sintoma | Solução |
|---|---|
| Código não reconhecido, ícones estranhos nos arquivos | Pasta errada aberta. Reabra pelo `pom.xml` |
| `JavaFX runtime components are missing` | Rode o `Launcher`, não a `App` |
| Dependências não baixam | **Maven → Reload project** e confira a internet |
| Erro de versão do Java | Troque o SDK do projeto para JDK 17 ou 21 |
| Aviso de formato antigo do CSV | Apague `dados/Produtos.csv` e abra o programa de novo |

## Dica para uso com Git

O arquivo `dados/Produtos.csv` muda toda vez que o programa é executado. Combinem se ele será versionado ou ignorado (`.gitignore`).
