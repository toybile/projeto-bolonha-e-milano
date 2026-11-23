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
    private Agricultura agricultura;
    private Governo governo;

    public Menu() {
        scanner = new Scanner(System.in);
        emExecucao = true;
        mercado = new Mercado();
        agricultura = new Agricultura();
        governo = new Governo();
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
        System.out.println("4 - Agricultura");
        System.out.println("5 - Governo");
        System.out.println("6 - Sobre o jogo");
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
            case 4: abrirAgricultura(); break;
            case 5: abrirGoverno(); break;
            case 6: exibirSobre(); break;
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

    // ================= AGRICULTURA =================

    private void abrirAgricultura() {
        if (cidade == null) {
            System.out.println("Nenhum jogo em andamento. Escolha 1 para comecar.");
            return;
        }
        boolean noSubmenu = true;
        while (noSubmenu) {
            System.out.println();
            System.out.println("--- AGRICULTURA (" + cidade.getNome() + ": " + cidade.getGraos() + " graos) ---");
            System.out.println("1 - Colher a safra");
            System.out.println("2 - Distribuir alimentos (20% a 80% dos graos)");
            System.out.println("0 - Voltar");
            System.out.print("Escolha uma opcao: ");
            switch (lerOpcao()) {
                case 1: colher(); break;
                case 2: distribuirAlimentos(); break;
                case 0: noSubmenu = false; break;
                default: System.out.println("Opcao invalida.");
            }
        }
    }

    private void colher() {
        int hectares = agricultura.processarColheita(cidade);
        System.out.println("Colheita feita: " + hectares + " hectares cultivados. Graos: " + cidade.getGraos());
    }

    private void distribuirAlimentos() {
        System.out.print("Percentual dos graos a distribuir (20 a 80): ");
        int percentual = lerOpcao();
        int servosAntes = cidade.getServos();
        if (agricultura.distribuirAlimentos(cidade, percentual)) {
            System.out.println("Alimentos distribuidos. Servos: " + servosAntes + " -> " + cidade.getServos());
        } else {
            System.out.println("Percentual recusado: use de 20 a 80.");
        }
    }

    // ================= GOVERNO =================

    private void abrirGoverno() {
        if (cidade == null) {
            System.out.println("Nenhum jogo em andamento. Escolha 1 para comecar.");
            return;
        }
        boolean noSubmenu = true;
        while (noSubmenu) {
            System.out.println();
            System.out.println("--- GOVERNO (taxa " + cidade.getTaxaImposto() + "%, justica "
                    + cidade.getNivelJustica() + ") ---");
            System.out.println("1 - Arrecadar impostos");
            System.out.println("2 - Definir taxa de imposto (0 a 50%)");
            System.out.println("3 - Definir nivel de justica");
            System.out.println("0 - Voltar");
            System.out.print("Escolha uma opcao: ");
            switch (lerOpcao()) {
                case 1: arrecadarImpostos(); break;
                case 2: definirTaxaImposto(); break;
                case 3: definirJustica(); break;
                case 0: noSubmenu = false; break;
                default: System.out.println("Opcao invalida.");
            }
        }
    }

    private void arrecadarImpostos() {
        int arrecadado = governo.arrecadarImpostos(cidade);
        System.out.println("Impostos arrecadados: " + arrecadado + " florins. Florins: " + cidade.getFlorins());
    }

    private void definirTaxaImposto() {
        System.out.print("Nova taxa (0 a 50): ");
        if (governo.definirTaxaImposto(cidade, lerOpcao())) {
            System.out.println("Taxa definida: " + cidade.getTaxaImposto() + "%");
        } else {
            System.out.println("Taxa recusada: use de 0 a 50.");
        }
    }

    // A justiça é escolhida por número, para ninguém errar a digitação do nome.
    private void definirJustica() {
        System.out.println("1 - Justa (impostos x 0,8)");
        System.out.println("2 - Moderada (impostos x 1,0)");
        System.out.println("3 - Severa (impostos x 1,3)");
        System.out.print("Escolha o nivel: ");
        String nivel;
        switch (lerOpcao()) {
            case 1: nivel = "Justa"; break;
            case 2: nivel = "Moderada"; break;
            case 3: nivel = "Severa"; break;
            default: nivel = "";      // o Governo vai recusar
        }
        if (governo.definirJustica(cidade, nivel)) {
            System.out.println("Nivel de justica: " + cidade.getNivelJustica());
        } else {
            System.out.println("Nivel invalido.");
        }
    }

    private void exibirSobre() {
        System.out.println();
        System.out.println("Bolonha e Milano e um jogo de estrategia economica");
        System.out.println("na Italia medieval. Cada jogador governa uma cidade-Estado.");
    }
}
