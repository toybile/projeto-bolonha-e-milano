package bolonhaemilano;

/*
 * GOVERNO: impostos e nível de justiça.
 * Recebe a cidade como parâmetro (não guarda a cidade).
 */
public class Governo {

    private static final int TAXA_MAXIMA = 50;                 // %
    private static final double MULTIPLICADOR_JUSTA = 0.8;
    private static final double MULTIPLICADOR_MODERADA = 1.0;
    private static final double MULTIPLICADOR_SEVERA = 1.3;

    /*
     * ARRECADAR (uma vez por ano).
     * Devolve quantos florins entraram, ou -1 se já arrecadou neste ano.
     *
     *   base       = servos x taxa / 100
     *   arrecadado = base x multiplicador da justiça (sem casas decimais)
     * Ex.: 200 servos, 10%, Severa -> 200 x 10 / 100 = 20 -> 20 x 1,3 = 26
     */
    public int arrecadarImpostos(Cidade cidade) {
        if (cidade.isImpostosArrecadados()) {
            return -1;
        }
        int base = cidade.getServos() * cidade.getTaxaImposto() / 100;
        // (int) corta as casas decimais: 27,3 vira 27.
        int arrecadado = (int) (base * multiplicadorPara(cidade.getNivelJustica()));
        cidade.adicionarFlorins(arrecadado);
        cidade.marcarImpostosArrecadados();
        return arrecadado;
    }

    // Aceita taxas de 0 a 50.
    public boolean definirTaxaImposto(Cidade cidade, int taxa) {
        if (taxa < 0 || taxa > TAXA_MAXIMA) {
            return false;
        }
        cidade.setTaxaImposto(taxa);
        return true;
    }

    // Aceita só "Justa", "Moderada" ou "Severa".
    public boolean definirJustica(Cidade cidade, String nivel) {
        if (!nivel.equals("Justa") && !nivel.equals("Moderada") && !nivel.equals("Severa")) {
            return false;
        }
        cidade.setNivelJustica(nivel);
        return true;
    }

    // Transforma o nome do nível no número que multiplica o imposto.
    private double multiplicadorPara(String nivel) {
        if (nivel.equals("Justa")) {
            return MULTIPLICADOR_JUSTA;
        } else if (nivel.equals("Severa")) {
            return MULTIPLICADOR_SEVERA;
        } else {
            return MULTIPLICADOR_MODERADA;
        }
    }
}
