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
        System.out.println("2 - Sobre o jogo");
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
                System.out.println("Novo jogo: em construcao (Etapa 2).");
                break;
            case 2:
                exibirSobre();
                break;
            case 0:
                emExecucao = false;
                break;
            default:
                System.out.println("Opcao invalida. Tente de novo.");
        }
    }

    private void exibirSobre() {
        System.out.println();
        System.out.println("Bolonha e Milano e um jogo de estrategia economica");
        System.out.println("na Italia medieval. Cada jogador governa uma cidade-Estado.");
    }
}
