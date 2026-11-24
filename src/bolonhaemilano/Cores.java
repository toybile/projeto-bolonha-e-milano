package bolonhaemilano;

/*
 * CORES (melhoria extra): pinta o texto do terminal.
 *
 * O terminal entende "códigos ANSI": sequências que começam com o caractere
 * de escape (\033) e mudam a cor do que vem depois. RESET volta ao normal.
 *
 * Uso:  System.out.println(Cores.sucesso("Compra feita."));
 *
 * Tudo é static: ninguém precisa criar um objeto Cores (igual Math.max).
 */
public class Cores {

    private static final String RESET = "\033[0m";
    private static final String VERDE = "\033[32m";
    private static final String VERMELHO = "\033[31m";
    private static final String AMARELO = "\033[1;33m";   // 1; = negrito
    private static final String CIANO = "\033[36m";

    // Deu certo (compra feita, colheita feita...).
    public static String sucesso(String texto) {
        return VERDE + texto + RESET;
    }

    // Foi recusado ou é inválido.
    public static String erro(String texto) {
        return VERMELHO + texto + RESET;
    }

    // Títulos e anúncios importantes (fim do ano, promoções, vencedor).
    public static String destaque(String texto) {
        return AMARELO + texto + RESET;
    }

    // Cabeçalhos de menu.
    public static String titulo(String texto) {
        return CIANO + texto + RESET;
    }
}
