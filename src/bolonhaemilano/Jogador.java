package bolonhaemilano;

/*
 * JOGADOR: quem governa uma cidade.
 * Guarda o nome e o título de nobreza (todo mundo começa como Cavaleiro).
 */
public class Jogador {

    private String nome;
    private String titulo;

    public Jogador(String nome) {
        this.nome = nome;
        this.titulo = "Cavaleiro";
    }

    public String getNome() {
        return nome;
    }

    public String getTitulo() {
        return titulo;
    }
}
