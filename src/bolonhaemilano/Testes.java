package bolonhaemilano;

/*
 * TESTES: confere automaticamente as regras do jogo.
 *
 * Tem o seu próprio main (é um segundo programa, fora do jogo):
 *     java -cp bin bolonhaemilano.Testes
 *
 * Cada teste segue o mesmo esqueleto:
 *     PREPARAR  -> cria a situação (ex.: uma cidade nova)
 *     EXECUTAR  -> chama o método que está sendo testado
 *     VERIFICAR -> confere o resultado com verificar(descricao, condicao)
 *
 * Tudo é static: ninguém precisa criar um objeto Testes.
 * Sem JUnit: é uma classe Java comum, compilada com o mesmo javac.
 */
public class Testes {

    private static int aprovados = 0;
    private static int reprovados = 0;

    public static void main(String[] args) {
        System.out.println("===== TESTES BOLONHA E MILANO =====");

        testarCidade();
        testarMercado();
        testarAgricultura();
        testarGoverno();
        testarConstrucoes();
        testarJogadorENobreza();
        testarPartida();

        System.out.println();
        System.out.println("Aprovados: " + aprovados + " | Reprovados: " + reprovados);

        // Se algum teste falhou, o programa termina com código de erro 1.
        if (reprovados > 0) {
            System.exit(1);
        }
    }

    // Se a condição é verdadeira, o teste passou; senão, falhou.
    private static void verificar(String descricao, boolean condicao) {
        if (condicao) {
            aprovados++;
            System.out.println("[OK] " + descricao);
        } else {
            reprovados++;
            System.out.println("[FALHA] " + descricao);
        }
    }

    // Atalho para PREPARAR: uma cidade nova, sempre com os valores iniciais.
    private static Cidade novaCidade() {
        return new Cidade("Bolonha", new Jogador("Teste"));
    }

    // ---------------------------------------------------------------
    private static void testarCidade() {
        System.out.println();
        System.out.println("-- Cidade --");

        Cidade cidade = novaCidade();
        verificar("cidade nasce com 1000 florins", cidade.getFlorins() == 1000);
        verificar("cidade nasce no ano 1400", cidade.getAnoAtual() == 1400);

        // caso de erro: tirar mais do que tem
        boolean removeu = cidade.removerFlorins(5000);
        verificar("removerFlorins acima do saldo devolve false", !removeu);
        verificar("... e nao altera o saldo", cidade.getFlorins() == 1000);

        cidade.marcarColheitaFeita();
        cidade.avancarAno();
        verificar("avancarAno soma 1 ao ano", cidade.getAnoAtual() == 1401);
        verificar("avancarAno libera a colheita de novo", !cidade.isColheitaFeita());
    }

    // ---------------------------------------------------------------
    private static void testarMercado() {
        System.out.println();
        System.out.println("-- Mercado --");
        Mercado mercado = new Mercado();

        Cidade cidade = novaCidade();
        boolean comprou = mercado.comprarGraos(cidade, 100);
        verificar("comprar 100 graos da certo", comprou);
        verificar("... e custa 200 florins", cidade.getFlorins() == 800);
        verificar("... e soma 100 graos", cidade.getGraos() == 600);

        Cidade outra = novaCidade();
        verificar("comprar quantidade negativa e recusado", !mercado.comprarGraos(outra, -5));
        verificar("comprar sem florins suficientes e recusado", !mercado.comprarGraos(outra, 1000));
    }

    // ---------------------------------------------------------------
    private static void testarAgricultura() {
        System.out.println();
        System.out.println("-- Agricultura --");
        Agricultura agricultura = new Agricultura();

        Cidade cidade = novaCidade();
        int hectares = agricultura.processarColheita(cidade);
        verificar("primeira colheita do ano cultiva 1000 hectares", hectares == 1000);
        verificar("... e deixa 2000 graos (500 - 500 sementes + 2000)", cidade.getGraos() == 2000);
        verificar("segunda colheita no mesmo ano devolve -1", agricultura.processarColheita(cidade) == -1);

        verificar("distribuir 10% e recusado (minimo 20%)", !agricultura.distribuirAlimentos(novaCidade(), 10));
    }

    // ---------------------------------------------------------------
    private static void testarGoverno() {
        System.out.println();
        System.out.println("-- Governo --");
        Governo governo = new Governo();

        Cidade cidade = novaCidade();
        verificar("taxa de 60% e recusada (maximo 50%)", !governo.definirTaxaImposto(cidade, 60));
        verificar("justica \"Branda\" e recusada", !governo.definirJustica(cidade, "Branda"));

        governo.definirJustica(cidade, "Severa");
        verificar("200 servos, 10%, Severa: arrecada 26 florins", governo.arrecadarImpostos(cidade) == 26);
        verificar("segunda arrecadacao no mesmo ano devolve -1", governo.arrecadarImpostos(cidade) == -1);
    }

    // ---------------------------------------------------------------
    private static void testarConstrucoes() {
        System.out.println();
        System.out.println("-- Construcoes --");
        Construcoes construcoes = new Construcoes();

        // preparar: florins bastam (500 de 1000), servos nao (so 50)
        Cidade cidade = novaCidade();
        cidade.removerServos(150);
        // executar
        boolean recrutou = construcoes.recrutarSoldados(cidade, 100);
        // verificar
        verificar("recrutar 100 soldados com so 50 servos e recusado", !recrutou);
        verificar("... e NAO debita florins pela metade", cidade.getFlorins() == 1000);

        Cidade outra = novaCidade();
        verificar("construir 10 da certo", construcoes.construir(outra, 10));
        verificar("... e custa 200 florins", outra.getFlorins() == 800);
    }

    // ---------------------------------------------------------------
    private static void testarJogadorENobreza() {
        System.out.println();
        System.out.println("-- Jogador e Nobreza --");
        Nobreza nobreza = new Nobreza();

        Cidade cidade = novaCidade();
        verificar("jogador comeca como Cavaleiro", cidade.getDono().getTitulo().equals("Cavaleiro"));
        verificar("prestigio inicial e 300 (200 servos + 10000/100 terras)",
                nobreza.calcularPrestigio(cidade) == 300);
        verificar("com prestigio 300 nao sobe de titulo", !nobreza.verificarPromocao(cidade));

        // 50 construções = +500 de prestígio (total 800: daria até Marques)
        cidade.adicionarConstrucoes(50);
        nobreza.verificarPromocao(cidade);
        verificar("com 50 construcoes sobe para Barao", cidade.getDono().getTitulo().equals("Barao"));
        verificar("sobe no maximo um titulo por chamada", cidade.getDono().getNivelTitulo() == 1);

        Jogador rei = new Jogador("Rei");
        for (int i = 0; i < 5; i++) {
            rei.promover();       // Cavaleiro -> ... -> Rei/Rainha
        }
        verificar("promover para alem de Rei/Rainha e recusado", !rei.promover());
    }

    // ---------------------------------------------------------------
    private static void testarPartida() {
        System.out.println();
        System.out.println("-- Partida --");
        Nobreza nobreza = new Nobreza();

        Partida partida = new Partida();
        Cidade bolonha = new Cidade("Bolonha", new Jogador("Ana"));
        Cidade milano = new Cidade("Milano", new Jogador("Beto"));
        partida.adicionarCidade(bolonha);
        partida.adicionarCidade(milano);

        verificar("primeiro turno e de Bolonha", partida.getCidadeAtual() == bolonha);
        verificar("passar o turno de Bolonha NAO fecha o ano", !partida.passarTurno());
        verificar("agora e a vez de Milano", partida.getCidadeAtual() == milano);
        verificar("passar o turno de Milano fecha o ano", partida.passarTurno());

        milano.adicionarConstrucoes(20);   // Milano passa de 400 e vira Barao
        partida.encerrarAno(nobreza);
        verificar("encerrarAno avanca todas as cidades",
                bolonha.getAnoAtual() == 1401 && milano.getAnoAtual() == 1401);
        verificar("partida ainda nao terminou em 1401", !partida.terminou());
        verificar("vencedor parcial e Milano (titulo mais alto)", partida.determinarVencedor(nobreza) == milano);

        Partida cheia = new Partida();
        for (int i = 0; i < 4; i++) {
            cheia.adicionarCidade(novaCidade());
        }
        verificar("quinta cidade e recusada (maximo 4)", !cheia.adicionarCidade(novaCidade()));
    }
}
