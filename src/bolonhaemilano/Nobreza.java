package bolonhaemilano;

/*
 * NOBREZA: calcula o prestígio e decide QUANDO o governante sobe de título.
 * Recebe a cidade como parâmetro (não guarda a cidade).
 *
 *   prestígio = construções x 10 + soldados x 2 + servos x 1 + terras / 100
 *   (florins guardados NÃO contam: para subir é preciso investir)
 */
public class Nobreza {

    private static final int PESO_CONSTRUCAO = 10;
    private static final int PESO_SOLDADO = 2;
    private static final int PESO_SERVO = 1;
    private static final int HECTARES_POR_PONTO = 100;

    // Prestígio mínimo de cada título, na mesma ordem do vetor TITULOS do Jogador:
    // Cavaleiro 0, Barao 400, Conde 550, Marques 700, Duque 850, Rei/Rainha 1000.
    private static final int[] PRESTIGIO_MINIMO = { 0, 400, 550, 700, 850, 1000 };

    public int calcularPrestigio(Cidade cidade) {
        return cidade.getConstrucoes() * PESO_CONSTRUCAO
             + cidade.getSoldados() * PESO_SOLDADO
             + cidade.getServos() * PESO_SERVO
             + cidade.getTerras() / HECTARES_POR_PONTO;
    }

    // Quanto de prestígio o jogador precisa para o PRÓXIMO título.
    // Devolve -1 se ele já é Rei/Rainha (não há próximo título).
    public int prestigioParaProximoTitulo(Jogador jogador) {
        if (jogador.ehMonarca()) {
            return -1;
        }
        return PRESTIGIO_MINIMO[jogador.getNivelTitulo() + 1];
    }

    // Promove o dono da cidade se o prestígio alcançar o próximo título.
    // No máximo UM título por chamada (uma chamada por ano, feita pela Partida).
    public boolean verificarPromocao(Cidade cidade) {
        Jogador dono = cidade.getDono();
        int necessario = prestigioParaProximoTitulo(dono);
        if (necessario == -1) {
            return false;
        }
        if (calcularPrestigio(cidade) < necessario) {
            return false;
        }
        return dono.promover();
    }
}
