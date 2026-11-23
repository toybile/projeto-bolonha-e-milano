package bolonhaemilano;

import java.util.ArrayList;
import java.util.List;

/*
 * PARTIDA: transforma o jogo em rodadas.
 *
 * Guarda de 1 a 4 cidades (uma por jogador) numa List, controla de quem é
 * a vez, fecha o ano quando o último jogador encerra o turno, sabe quando o
 * jogo terminou e quem venceu.
 */
public class Partida {

    public static final int MAXIMO_JOGADORES = 4;
    private static final int ANO_FINAL = 1410;

    // ArrayList: uma lista que cresce conforme adicionamos cidades.
    private List<Cidade> cidades = new ArrayList<>();

    // Posição na lista da cidade que está jogando agora (0 = primeira).
    private int indiceAtual = 0;

    // Coloca a cidade na partida. Recusa a quinta (máximo 4 jogadores).
    public boolean adicionarCidade(Cidade cidade) {
        if (cidades.size() >= MAXIMO_JOGADORES) {
            return false;
        }
        cidades.add(cidade);
        return true;
    }

    public List<Cidade> getCidades() {
        return cidades;
    }

    // A cidade do jogador da vez.
    public Cidade getCidadeAtual() {
        return cidades.get(indiceAtual);
    }

    // Todas as cidades estão sempre no mesmo ano; usamos a primeira.
    public int getAnoAtual() {
        return cidades.get(0).getAnoAtual();
    }

    public int getAnoFinal() {
        return ANO_FINAL;
    }

    // Passa a vez para o próximo jogador.
    // Devolve true quando o último jogador encerrou: o ano acabou.
    public boolean passarTurno() {
        indiceAtual++;
        if (indiceAtual >= cidades.size()) {
            indiceAtual = 0;     // volta para o primeiro jogador
            return true;         // o ano acabou
        }
        return false;
    }

    // Fecha o ano: cada cidade avança um ano e pode subir um título.
    // Devolve a lista das cidades cujo governante foi promovido.
    public List<Cidade> encerrarAno(Nobreza nobreza) {
        List<Cidade> promovidas = new ArrayList<>();
        for (Cidade cidade : cidades) {
            cidade.avancarAno();
            if (nobreza.verificarPromocao(cidade)) {
                promovidas.add(cidade);
            }
        }
        return promovidas;
    }

    // O jogo termina se alguém virou Rei/Rainha ou se 1410 já passou.
    public boolean terminou() {
        for (Cidade cidade : cidades) {
            if (cidade.getDono().ehMonarca()) {
                return true;
            }
        }
        return getAnoAtual() > ANO_FINAL;
    }

    // Vence o título mais alto; em caso de empate, o maior prestígio.
    public Cidade determinarVencedor(Nobreza nobreza) {
        Cidade vencedora = cidades.get(0);
        for (Cidade cidade : cidades) {
            int nivel = cidade.getDono().getNivelTitulo();
            int nivelVencedora = vencedora.getDono().getNivelTitulo();

            if (nivel > nivelVencedora) {
                vencedora = cidade;
            } else if (nivel == nivelVencedora
                    && nobreza.calcularPrestigio(cidade) > nobreza.calcularPrestigio(vencedora)) {
                vencedora = cidade;
            }
        }
        return vencedora;
    }
}
