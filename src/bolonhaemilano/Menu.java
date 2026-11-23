package bolonhaemilano;

import java.util.Scanner;

/*
 * MENU: o laço principal do jogo e a única classe que conversa com o jogador.
 *
 * O Menu só LÊ o que foi digitado, CHAMA a classe de regra certa e MOSTRA
 * o resultado. As regras ficam nas classes de serviço.
 */
public class Menu {

    // Lê o que o jogador digita no teclado (System.in).
    private Scanner scanner;

    // Enquanto for true, o menu continua aparecendo. A opção 0 muda para false.
    private boolean emExecucao;

    // O jogo em andamento. Enquanto ninguém escolher "Novo jogo", ficam null.
    private Jogador jogador;
    private Cidade cidade;

    // Classes de serviço: cada uma cuida de um grupo de regras.
    private Mercado mercado;

    public Menu() {
        scanner = new Scanner(System.in);
        emExecucao = true;
        mercado = new Mercado();
    }

    // Laço principal: mostra, lê, processa... até o jogador escolher Sair.
    public void executar() {
        while (emExecucao) {
            exibirOpcoes();
            int opcao = lerOpcao();
            processarOpcao(opcao);
        }
        scanner.close();   // ao sair, fecha o Scanner
        System.out.println("Ate a proxima!");
    }

    // Só imprime o menu na tela.
    private void exibirOpcoes() {
        System.out.println();
        System.out.println("==========================================");
        System.out.println("             BOLONHA E MILANO");
        System.out.println("==========================================");
        System.out.println("1 - Novo jogo");
        System.out.println("2 - Ver status da cidade");
        System.out.println("3 - Mercado");
        System.out.println("4 - Sobre o jogo");
        System.out.println("0 - Sair");
        System.out.print("Escolha uma opcao: ");
    }

    // Lê uma linha e tenta transformar em número.
    // Se não for número, devolve -1 (opção impossível), sem travar o programa.
    private int lerOpcao() {
        String linha = scanner.nextLine();
        try {
            return Integer.parseInt(linha.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Mesma ideia do lerOpcao, mas para quantidades.
    private int lerQuantidade() {
        System.out.print("Quantidade: ");
        String linha = scanner.nextLine();
        try {
            return Integer.parseInt(linha.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Decide o que fazer com a opção escolhida.
    private void processarOpcao(int opcao) {
        switch (opcao) {
            case 1: iniciarNovoJogo(); break;
            case 2: exibirStatus(); break;
            case 3: abrirMercado(); break;
            case 4: exibirSobre(); break;
            case 0: emExecucao = false; break;
            default: System.out.println("Opcao invalida. Tente de novo.");
        }
    }

    // Pergunta os nomes, cria o Jogador e a Cidade e mostra o status inicial.
    private void iniciarNovoJogo() {
        System.out.print("Nome do jogador: ");
        String nomeJogador = scanner.nextLine();
        System.out.print("Nome da cidade: ");
        String nomeCidade = scanner.nextLine();

        jogador = new Jogador(nomeJogador);
        cidade = new Cidade(nomeCidade, jogador);   // a cidade recebe o seu dono

        System.out.println("Novo jogo iniciado!");
        cidade.exibirStatus();
    }

    // Mostra o status, mas avisa se ainda não existe jogo.
    private void exibirStatus() {
        if (cidade == null) {
            System.out.println("Nenhum jogo em andamento. Escolha 1 para comecar.");
            return;
        }
        cidade.exibirStatus();
    }

    // ================= MERCADO =================

    private void abrirMercado() {
        if (cidade == null) {
            System.out.println("Nenhum jogo em andamento. Escolha 1 para comecar.");
            return;
        }
        boolean noSubmenu = true;
        while (noSubmenu) {
            System.out.println();
            System.out.println("--- MERCADO (" + cidade.getNome() + ": " + cidade.getFlorins() + " florins) ---");
            System.out.println("1 - Comprar graos (2 florins cada)");
            System.out.println("2 - Vender graos (2 florins cada)");
            System.out.println("3 - Comprar terras (1 florim por hectare)");
            System.out.println("4 - Vender terras (1 florim por hectare)");
            System.out.println("0 - Voltar");
            System.out.print("Escolha uma opcao: ");
            switch (lerOpcao()) {
                case 1: comprarGraos(); break;
                case 2: venderGraos(); break;
                case 3: comprarTerras(); break;
                case 4: venderTerras(); break;
                case 0: noSubmenu = false; break;
                default: System.out.println("Opcao invalida.");
            }
        }
    }

    // Cada operação só lê a quantidade e chama o Mercado (quem decide é ele).
    private void comprarGraos() {
        if (mercado.comprarGraos(cidade, lerQuantidade())) {
            System.out.println("Compra feita. Graos: " + cidade.getGraos());
        } else {
            System.out.println("Compra recusada (quantidade invalida ou florins insuficientes).");
        }
    }

    private void venderGraos() {
        if (mercado.venderGraos(cidade, lerQuantidade())) {
            System.out.println("Venda feita. Florins: " + cidade.getFlorins());
        } else {
            System.out.println("Venda recusada (quantidade invalida ou graos insuficientes).");
        }
    }

    private void comprarTerras() {
        if (mercado.comprarTerras(cidade, lerQuantidade())) {
            System.out.println("Compra feita. Terras: " + cidade.getTerras() + " hectares");
        } else {
            System.out.println("Compra recusada (quantidade invalida ou florins insuficientes).");
        }
    }

    private void venderTerras() {
        if (mercado.venderTerras(cidade, lerQuantidade())) {
            System.out.println("Venda feita. Florins: " + cidade.getFlorins());
        } else {
            System.out.println("Venda recusada (quantidade invalida ou terras insuficientes).");
        }
    }

    private void exibirSobre() {
        System.out.println();
        System.out.println("Bolonha e Milano e um jogo de estrategia economica");
        System.out.println("na Italia medieval. Cada jogador governa uma cidade-Estado.");
    }
}
