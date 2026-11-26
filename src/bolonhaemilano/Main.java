package bolonhaemilano;

import java.io.IOException;

/*
 * MAIN: o ponto de entrada do jogo.
 *
 * Por padrão inicia a interface visual no navegador (via ServidorWeb).
 * Também mantém o modo clássico de terminal quando executado com --cli ou --terminal.
 */
public class Main {

    private static final int PORTA_PADRAO = 8080;

    public static void main(String[] args) {
        boolean modoCli = false;
        boolean abrirNavegador = true;
        int porta = PORTA_PADRAO;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--cli".equalsIgnoreCase(arg) || "--terminal".equalsIgnoreCase(arg)) {
                modoCli = true;
            } else if ("--no-browser".equalsIgnoreCase(arg)) {
                abrirNavegador = false;
            } else if ("--port".equalsIgnoreCase(arg) && i + 1 < args.length) {
                try {
                    porta = Integer.parseInt(args[++i]);
                } catch (NumberFormatException e) {
                    System.err.println("Porta inválida. Usando a porta padrão " + PORTA_PADRAO + ".");
                    porta = PORTA_PADRAO;
                }
            }
        }

        if (modoCli) {
            Menu menu = new Menu();
            menu.executar();
            return;
        }

        try {
            ServidorWeb servidor = new ServidorWeb();
            servidor.iniciar(porta, abrirNavegador);
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor web: " + e.getMessage());
            System.exit(1);
        }
    }
}
