package bolonhaemilano;

/*
 * CIDADE: a cidade-Estado governada por um jogador.
 *
 * Guarda o nome, o ano atual e o dono (um objeto Jogador).
 * "dono" é uma ASSOCIAÇÃO: a Cidade conhece o Jogador que a governa.
 */
public class Cidade {

    // Constante: todo jogo começa no ano 1400.
    private static final int ANO_INICIAL = 1400;

    private String nome;
    private int anoAtual;
    private Jogador dono;

    public Cidade(String nome, Jogador dono) {
        this.nome = nome;
        this.dono = dono;
        this.anoAtual = ANO_INICIAL;
    }

    public String getNome() {
        return nome;
    }

    public int getAnoAtual() {
        return anoAtual;
    }

    public Jogador getDono() {
        return dono;
    }

    // Passa para o ano seguinte (será usado a partir da Etapa 7).
    public void avancarAno() {
        anoAtual = anoAtual + 1;
    }

    // Mostra tudo o que a cidade tem.
    public void exibirStatus() {
        System.out.println();
        System.out.println("----------- STATUS DA CIDADE -----------");
        System.out.println("Cidade: " + nome);
        System.out.println("Governante: " + dono.getNome() + " (" + dono.getTitulo() + ")");
        System.out.println("Ano: " + anoAtual);
    }
}
