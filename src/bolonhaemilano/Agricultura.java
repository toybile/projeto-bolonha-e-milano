package bolonhaemilano;

/*
 * AGRICULTURA: colheita e distribuição de comida para os servos.
 * Recebe a cidade como parâmetro (não guarda a cidade).
 */
public class Agricultura {

    private static final int HECTARES_POR_SERVO = 5;      // cada servo cultiva até 5 ha
    private static final int RENDIMENTO_POR_HECTARE = 2;  // cada ha cultivado produz 2 grãos
    private static final int SERVOS_POR_GRAO = 5;         // 1 grão alimenta 5 servos
    private static final int CRESCIMENTO_PERCENTUAL = 5;  // comida suficiente: +5% de servos
    private static final int PERCENTUAL_MINIMO = 20;
    private static final int PERCENTUAL_MAXIMO = 80;

    /*
     * COLHEITA (uma vez por ano).
     * Devolve quantos hectares foram cultivados, ou -1 se já colheu neste ano.
     *
     * Exemplo com a cidade inicial (10000 ha, 200 servos, 500 grãos):
     *   hectares = menor(10000, 200 x 5) = 1000
     *   sementes = 1000 / 2 = 500   -> grãos: 500 - 500 = 0
     *   produção = 1000 x 2 = 2000  -> grãos: 0 + 2000 = 2000
     */
    public int processarColheita(Cidade cidade) {
        if (cidade.isColheitaFeita()) {
            return -1;
        }

        // Só dá para cultivar a terra que existe E que os servos conseguem trabalhar.
        int hectares = Math.min(cidade.getTerras(), cidade.getServos() * HECTARES_POR_SERVO);

        // Para plantar, gasta-se meio grão de semente por hectare.
        int sementes = hectares / 2;

        // Se não há sementes para tudo, cultiva só o que as sementes permitem.
        if (sementes > cidade.getGraos()) {
            sementes = cidade.getGraos();
            hectares = sementes * 2;
        }

        cidade.removerGraos(sementes);
        int producao = hectares * RENDIMENTO_POR_HECTARE;
        cidade.adicionarGraos(producao);

        cidade.marcarColheitaFeita();
        return hectares;
    }

    /*
     * DISTRIBUIR ALIMENTOS: entrega uma porcentagem do estoque de grãos.
     *   - percentual fora de 20 a 80: recusa (false);
     *   - comida suficiente: servos crescem 5%;
     *   - comida insuficiente: morrem servos na medida do que faltou.
     */
    public boolean distribuirAlimentos(Cidade cidade, int percentual) {
        if (percentual < PERCENTUAL_MINIMO || percentual > PERCENTUAL_MAXIMO) {
            return false;
        }

        int alimento = cidade.getGraos() * percentual / 100;
        cidade.removerGraos(alimento);

        // Quantos grãos os servos precisam: 1 grão para cada 5 servos.
        int necessidade = cidade.getServos() / SERVOS_POR_GRAO;

        if (alimento >= necessidade) {
            int crescimento = cidade.getServos() * CRESCIMENTO_PERCENTUAL / 100;
            cidade.adicionarServos(crescimento);
        } else {
            int deficit = necessidade - alimento;
            // Math.min garante que não se tira mais servos do que existem.
            cidade.removerServos(Math.min(deficit, cidade.getServos()));
        }
        return true;
    }
}
