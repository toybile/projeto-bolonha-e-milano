package bolonhaemilano;

/*
 * CONSTRUCOES: recrutar soldados e construir obras.
 * Recebe a cidade como parâmetro (não guarda a cidade).
 */
public class Construcoes {

    private static final int PRECO_SOLDADO = 5;       // florins por soldado
    private static final int SERVOS_POR_SOLDADO = 1;  // cada soldado sai de 1 servo
    private static final int PRECO_CONSTRUCAO = 20;   // florins por construção

    /*
     * RECRUTAR: custa florins E servos.
     * Regra importante: confere os DOIS antes de tirar qualquer um.
     * Se faltar um deles, nada é debitado (nem pela metade).
     */
    public boolean recrutarSoldados(Cidade cidade, int quantidade) {
        if (quantidade <= 0) {
            return false;
        }
        int custo = quantidade * PRECO_SOLDADO;
        int servosNecessarios = quantidade * SERVOS_POR_SOLDADO;

        if (cidade.getFlorins() < custo || cidade.getServos() < servosNecessarios) {
            return false;
        }

        cidade.removerFlorins(custo);
        cidade.removerServos(servosNecessarios);
        cidade.adicionarSoldados(quantidade);
        return true;
    }

    // CONSTRUIR: custa só florins.
    public boolean construir(Cidade cidade, int quantidade) {
        if (quantidade <= 0) {
            return false;
        }
        if (!cidade.removerFlorins(quantidade * PRECO_CONSTRUCAO)) {
            return false;
        }
        cidade.adicionarConstrucoes(quantidade);
        return true;
    }

    public int getPrecoSoldado() {
        return PRECO_SOLDADO;
    }

    public int getPrecoConstrucao() {
        return PRECO_CONSTRUCAO;
    }
}
