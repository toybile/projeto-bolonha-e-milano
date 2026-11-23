package bolonhaemilano;

/*
 * MERCADO: compra e venda de grãos e terras.
 *
 * O Mercado NÃO guarda a cidade como atributo: ele a recebe como parâmetro
 * em cada método (isso é uma DEPENDÊNCIA, e não uma associação).
 *
 * Todos os métodos devolvem boolean: true = deu certo, false = recusado.
 */
public class Mercado {

    private static final int PRECO_GRAO = 2;   // florins por grão
    private static final int PRECO_TERRA = 1;  // florins por hectare

    // COMPRAR: primeiro tenta pagar; só se conseguir, entrega os grãos.
    public boolean comprarGraos(Cidade cidade, int quantidade) {
        if (quantidade <= 0) {
            return false;
        }
        int custo = quantidade * PRECO_GRAO;
        if (!cidade.removerFlorins(custo)) {
            return false;                       // dinheiro insuficiente
        }
        cidade.adicionarGraos(quantidade);
        return true;
    }

    // VENDER: primeiro tenta tirar os grãos; só se conseguir, paga a cidade.
    public boolean venderGraos(Cidade cidade, int quantidade) {
        if (quantidade <= 0) {
            return false;
        }
        if (!cidade.removerGraos(quantidade)) {
            return false;                       // não tem tantos grãos
        }
        cidade.adicionarFlorins(quantidade * PRECO_GRAO);
        return true;
    }

    public boolean comprarTerras(Cidade cidade, int quantidade) {
        if (quantidade <= 0) {
            return false;
        }
        int custo = quantidade * PRECO_TERRA;
        if (!cidade.removerFlorins(custo)) {
            return false;
        }
        cidade.adicionarTerras(quantidade);
        return true;
    }

    public boolean venderTerras(Cidade cidade, int quantidade) {
        if (quantidade <= 0) {
            return false;
        }
        if (!cidade.removerTerras(quantidade)) {
            return false;
        }
        cidade.adicionarFlorins(quantidade * PRECO_TERRA);
        return true;
    }
}
