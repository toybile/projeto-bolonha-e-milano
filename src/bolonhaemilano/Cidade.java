package bolonhaemilano;

/*
 * CIDADE: a cidade-Estado governada por um jogador.
 *
 * Guarda os dados da cidade. A Cidade não tem regras de jogo: ela só
 * guarda e altera os números. As regras ficam nas classes de serviço,
 * que recebem a cidade como parâmetro.
 *
 * Padrão seguro de recursos:
 *   adicionarX(qtd)  -> soma
 *   removerX(qtd)    -> só tira se tiver o suficiente; devolve true/false.
 *                       Assim NENHUM recurso fica negativo.
 */
public class Cidade {

    // ---------- VALORES INICIAIS (toda cidade começa igual) ----------
    private static final int ANO_INICIAL = 1400;
    private static final int FLORINS_INICIAIS = 1000;
    private static final int TERRAS_INICIAIS = 10000;   // hectares
    private static final int GRAOS_INICIAIS = 500;

    // ---------- ATRIBUTOS ----------
    private String nome;
    private int anoAtual;
    private Jogador dono;          // associação: a cidade conhece quem a governa

    private int florins;
    private int terras;
    private int graos;

    public Cidade(String nome, Jogador dono) {
        this.nome = nome;
        this.dono = dono;
        this.anoAtual = ANO_INICIAL;
        this.florins = FLORINS_INICIAIS;
        this.terras = TERRAS_INICIAIS;
        this.graos = GRAOS_INICIAIS;
    }

    // ---------- IDENTIFICAÇÃO E ANO ----------

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

    // ---------- FLORINS (dinheiro) ----------

    public int getFlorins() {
        return florins;
    }

    public void adicionarFlorins(int quantidade) {
        florins = florins + quantidade;
    }

    public boolean removerFlorins(int quantidade) {
        if (quantidade > florins) {
            return false;            // não tem dinheiro suficiente: não mexe em nada
        }
        florins = florins - quantidade;
        return true;
    }

    // ---------- TERRAS (hectares) ----------

    public int getTerras() {
        return terras;
    }

    public void adicionarTerras(int quantidade) {
        terras = terras + quantidade;
    }

    public boolean removerTerras(int quantidade) {
        if (quantidade > terras) {
            return false;
        }
        terras = terras - quantidade;
        return true;
    }

    // ---------- GRÃOS ----------

    public int getGraos() {
        return graos;
    }

    public void adicionarGraos(int quantidade) {
        graos = graos + quantidade;
    }

    public boolean removerGraos(int quantidade) {
        if (quantidade > graos) {
            return false;
        }
        graos = graos - quantidade;
        return true;
    }

    // ---------- STATUS ----------

    public void exibirStatus() {
        System.out.println();
        System.out.println("----------- STATUS DA CIDADE -----------");
        System.out.println("Cidade: " + nome);
        System.out.println("Governante: " + dono.getNome() + " (" + dono.getTitulo() + ")");
        System.out.println("Ano: " + anoAtual);
        System.out.println("Florins: " + florins);
        System.out.println("Terras: " + terras + " hectares");
        System.out.println("Graos em estoque: " + graos);
    }
}
