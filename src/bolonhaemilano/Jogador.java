package bolonhaemilano;

/*
 * JOGADOR: quem governa uma cidade.
 *
 * Guarda o nome e o NÍVEL do título de nobreza. O nível é a posição no
 * vetor TITULOS: 0 = Cavaleiro, 1 = Barao, ..., 5 = Rei/Rainha.
 *
 * Quem sabe a ORDEM dos títulos é o Jogador.
 * Quem decide QUANDO promover é a Nobreza.
 */
public class Jogador {

    // Os seis títulos, do mais baixo para o mais alto. "static": é um só
    // vetor para a classe inteira, não um por jogador.
    private static final String[] TITULOS = {
        "Cavaleiro", "Barao", "Conde", "Marques", "Duque", "Rei/Rainha"
    };

    private String nome;
    private int nivelTitulo;   // posição no vetor TITULOS

    public Jogador(String nome) {
        this.nome = nome;
        this.nivelTitulo = 0;  // todo mundo começa como Cavaleiro
    }

    public String getNome() {
        return nome;
    }

    // O nome do título é o texto que está na posição nivelTitulo do vetor.
    public String getTitulo() {
        return TITULOS[nivelTitulo];
    }

    public int getNivelTitulo() {
        return nivelTitulo;
    }

    // Sobe UM título. Se já é Rei/Rainha, não tem para onde subir: devolve false.
    public boolean promover() {
        if (ehMonarca()) {
            return false;
        }
        nivelTitulo = nivelTitulo + 1;
        return true;
    }

    // true quando o jogador está no último título do vetor (Rei/Rainha).
    public boolean ehMonarca() {
        return nivelTitulo == TITULOS.length - 1;
    }
}
