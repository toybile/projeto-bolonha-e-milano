package bolonhaemilano;

import java.util.Scanner;

/*
 * MENU: o laço principal do jogo.
 *
 * Mostra as opções, lê o que o jogador digitou e decide o que fazer.
 * Repete isso enquanto o atributo emExecucao for true.
 */
public class Menu {

    // Lê o que o jogador digita no teclado (System.in).
    private Scanner scanner;

    // Enquanto for true, o menu continua aparecendo. A opção 0 muda para false.
    private boolean emExecucao;

    // O jogo em andamento. Enquanto ninguém escolher "Novo jogo", ficam null.
    private Jogador jogador;
    private Cidade cidade;

    public Menu() {
        scanner = new Scanner(System.in);
        emExecucao = true;
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
        System.out.println("3 - Sobre o jogo");
        System.out.println("0 - Sair");
        System.out.print("Escolha uma opcao: ");
    }

    // Lê uma linha e tenta transformar em número.
    // Se o jogador digitar algo que não é número, devolve -1 (opção impossível),
    // assim o programa não trava com erro.
    private int lerOpcao() {
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
            case 1:
                iniciarNovoJogo();
                break;
            case 2:
                exibirStatus();
                break;
            case 3:
                exibirSobre();
                break;
            case 0:
                emExecucao = false;
                break;
            default:
                System.out.println("Opcao invalida. Tente de novo.");
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

    private void exibirSobre() {
        System.out.println();
        System.out.println("Bolonha e Milano e um jogo de estrategia economica");
        System.out.println("na Italia medieval. Cada jogador governa uma cidade-Estado.");
    }
}
