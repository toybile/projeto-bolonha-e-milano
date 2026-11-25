package bolonhaemilano;

import java.util.List;
import java.util.Scanner;

/*
 * MENU: o laço principal do jogo e a única classe que conversa com o jogador.
 *
 * O Menu só LÊ o que foi digitado, CHAMA a classe de regra certa e MOSTRA
 * o resultado. As regras do jogo ficam nas classes de serviço:
 *   Mercado, Agricultura, Governo, Construcoes e Nobreza.
 *
 * Menu principal:
 *   1 Novo jogo        2 Ver status       3 Mercado        4 Agricultura
 *   5 Governo          6 Construcoes      7 Encerrar turno 8 Classificacao
 *   9 Sobre o jogo     0 Sair
 *
 * Melhorias extras: a tela é limpa a cada volta do menu (limparTela), o
 * resultado fica na tela até o ENTER (pausar) e as mensagens são coloridas
 * pela classe Cores (verde = deu certo, vermelho = recusado).
 */
public class Menu {

    // Nomes sugeridos para as cidades, na ordem dos jogadores.
    private static final String[] CIDADES_PADRAO = { "Bolonha", "Milano", "Florenca", "Veneza" };

    private Scanner scanner;
    private boolean emExecucao;

    private Partida partida;   // o jogo em andamento (null = nenhum jogo)
    private Cidade cidade;     // a cidade do jogador DA VEZ

    // Classes de serviço: cada uma cuida de um grupo de regras.
    private Mercado mercado;
    private Agricultura agricultura;
    private Governo governo;
    private Construcoes construcoes;
    private Nobreza nobreza;

    public Menu() {
        scanner = new Scanner(System.in);
        emExecucao = true;
        mercado = new Mercado();
        agricultura = new Agricultura();
        governo = new Governo();
        construcoes = new Construcoes();
        nobreza = new Nobreza();
    }

    // ================= LAÇO PRINCIPAL =================

    public void executar() {
        while (emExecucao) {
            limparTela();
            exibirOpcoes();
            int opcao = lerOpcao();
            processarOpcao(opcao);
            // Os submenus (3 a 6) já pausam sozinhos; o resto pausa aqui,
            // para a mensagem não sumir quando a tela for limpa.
            if (emExecucao && !ehSubmenu(opcao)) {
                pausar();
            }
        }
        scanner.close();
        System.out.println("Ate a proxima!");
    }

    private void exibirOpcoes() {
        System.out.println();
        System.out.println(Cores.titulo("=========================================="));
        System.out.println(Cores.titulo("             BOLONHA E MILANO"));
        // No topo aparece sempre o ano e de quem é a vez.
        if (partida != null) {
            System.out.println(Cores.destaque("   Ano " + partida.getAnoAtual() + " | Vez de "
                    + cidade.getDono().getNome() + " (" + cidade.getNome() + ")"));
        }
        System.out.println(Cores.titulo("=========================================="));
        System.out.println("1 - Novo jogo");
        System.out.println("2 - Ver status da cidade");
        System.out.println("3 - Mercado");
        System.out.println("4 - Agricultura");
        System.out.println("5 - Governo");
        System.out.println("6 - Construcoes");
        System.out.println("7 - Encerrar turno");
        System.out.println("8 - Ver classificacao");
        System.out.println("9 - Sobre o jogo");
        System.out.println("0 - Sair");
        System.out.print("Escolha uma opcao: ");
    }

    private void processarOpcao(int opcao) {
        switch (opcao) {
            case 1: iniciarNovoJogo(); break;
            case 2: exibirStatus(); break;
            case 3: abrirMercado(); break;
            case 4: abrirAgricultura(); break;
            case 5: abrirGoverno(); break;
            case 6: abrirConstrucoes(); break;
            case 7: encerrarTurno(); break;
            case 8: exibirClassificacao(); break;
            case 9: exibirSobre(); break;
            case 0: emExecucao = false; break;
            default: System.out.println(Cores.erro("Opcao invalida. Tente de novo."));
        }
    }

    // ================= TELA (melhoria extra) =================

    // Limpa o terminal: \033[H põe o cursor no topo e \033[2J apaga a tela.
    private void limparTela() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    // Segura o resultado na tela até o jogador apertar ENTER.
    private void pausar() {
        System.out.print("\nPressione ENTER para continuar...");
        scanner.nextLine();
    }

    // Opções 3 a 6 abrem um submenu, que pausa sozinho (se há jogo em andamento).
    private boolean ehSubmenu(int opcao) {
        return partida != null && opcao >= 3 && opcao <= 6;
    }

    // ================= LEITURA DO TECLADO =================

    // Lê uma linha e converte para número. Se não for número, devolve -1.
    // (Etapa 8: antes esse try/catch estava copiado em lerOpcao e lerQuantidade.)
    private int lerInteiro() {
        String linha = scanner.nextLine();
        try {
            return Integer.parseInt(linha.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int lerOpcao() {
        return lerInteiro();
    }

    private int lerQuantidade() {
        System.out.print("Quantidade: ");
        return lerInteiro();
    }

    // Se não há jogo, avisa e devolve false. Cada submenu começa com:
    //     if (!exigirJogoEmAndamento()) { return; }
    // (Etapa 8: antes esse if estava repetido em 7 métodos.)
    private boolean exigirJogoEmAndamento() {
        if (partida == null) {
            System.out.println(Cores.erro("Nenhum jogo em andamento. Escolha 1 para comecar."));
            return false;
        }
        return true;
    }

    // ================= 1 - NOVO JOGO =================

    private void iniciarNovoJogo() {
        System.out.print("Quantos jogadores (1 a 4)? ");
        int quantidade = lerInteiro();
        if (quantidade < 1 || quantidade > Partida.MAXIMO_JOGADORES) {
            System.out.print("Número de jogadores inválido.\nPadrão: 2 jogadores.\n");
            quantidade = 2;
        }

        partida = new Partida();
        for (int i = 0; i < quantidade; i++) {
            System.out.print("Nome do jogador " + (i + 1) + ": ");
            String nomeJogador = scanner.nextLine();

            System.out.print("Nome da cidade de " + nomeJogador + " (Enter = " + CIDADES_PADRAO[i] + "): ");
            String nomeCidade = scanner.nextLine();
            if (nomeCidade.trim().isEmpty()) {
                nomeCidade = CIDADES_PADRAO[i];   // Enter vazio aceita a sugestão
            }

            partida.adicionarCidade(new Cidade(nomeCidade, new Jogador(nomeJogador)));
        }

        cidade = partida.getCidadeAtual();
        System.out.println();
        System.out.println(Cores.sucesso("Novo jogo iniciado! Ano " + partida.getAnoAtual() + ". A partida vai ate"));
        System.out.println("o fim de " + partida.getAnoFinal() + " ou ate alguem virar Rei/Rainha.");
        System.out.println(Cores.destaque("Comeca: " + descreverDaVez() + "."));
    }

    // ================= 2 - STATUS =================

    private void exibirStatus() {
        if (!exigirJogoEmAndamento()) {
            return;
        }
        cidade.exibirStatus();
    }

    // ================= 3 - MERCADO =================

    private void abrirMercado() {
        if (!exigirJogoEmAndamento()) {
            return;
        }
        boolean noSubmenu = true;
        while (noSubmenu) {
            limparTela();
            System.out.println(Cores.titulo("--- MERCADO (" + cidade.getNome() + ": " + cidade.getFlorins() + " florins) ---"));
            System.out.println("1 - Comprar graos (2 florins cada)");
            System.out.println("2 - Vender graos (2 florins cada)");
            System.out.println("3 - Comprar terras (1 florim por hectare)");
            System.out.println("4 - Vender terras (1 florim por hectare)");
            System.out.println("0 - Voltar");
            System.out.print("Escolha uma opcao: ");
            switch (lerOpcao()) {
                case 1: comprarGraos(); break;
                case 2: venderGraos(); break;
                case 3: comprarTerras(); break;
                case 4: venderTerras(); break;
                case 0: noSubmenu = false; break;
                default: System.out.println(Cores.erro("Opcao invalida."));
            }
            if (noSubmenu) {
                pausar();
            }
        }
    }

    // Cada operação só lê a quantidade e chama o Mercado (quem decide é ele).
    private void comprarGraos() {
        if (mercado.comprarGraos(cidade, lerQuantidade())) {
            System.out.println(Cores.sucesso("Compra feita. Graos: " + cidade.getGraos()));
        } else {
            System.out.println(Cores.erro("Compra recusada (quantidade invalida ou florins insuficientes)."));
        }
    }

    private void venderGraos() {
        if (mercado.venderGraos(cidade, lerQuantidade())) {
            System.out.println(Cores.sucesso("Venda feita. Florins: " + cidade.getFlorins()));
        } else {
            System.out.println(Cores.erro("Venda recusada (quantidade invalida ou graos insuficientes)."));
        }
    }

    private void comprarTerras() {
        if (mercado.comprarTerras(cidade, lerQuantidade())) {
            System.out.println(Cores.sucesso("Compra feita. Terras: " + cidade.getTerras() + " hectares"));
        } else {
            System.out.println(Cores.erro("Compra recusada (quantidade invalida ou florins insuficientes)."));
        }
    }

    private void venderTerras() {
        if (mercado.venderTerras(cidade, lerQuantidade())) {
            System.out.println(Cores.sucesso("Venda feita. Florins: " + cidade.getFlorins()));
        } else {
            System.out.println(Cores.erro("Venda recusada (quantidade invalida ou terras insuficientes)."));
        }
    }

    // ================= 4 - AGRICULTURA =================

    private void abrirAgricultura() {
        if (!exigirJogoEmAndamento()) {
            return;
        }
        boolean noSubmenu = true;
        while (noSubmenu) {
            limparTela();
            System.out.println(Cores.titulo("--- AGRICULTURA (" + cidade.getNome() + ": " + cidade.getGraos() + " graos) ---"));
            System.out.println("1 - Colher a safra (1 vez por ano)");
            System.out.println("2 - Distribuir alimentos (20% a 80% dos graos)");
            System.out.println("0 - Voltar");
            System.out.print("Escolha uma opcao: ");
            switch (lerOpcao()) {
                case 1: colher(); break;
                case 2: distribuirAlimentos(); break;
                case 0: noSubmenu = false; break;
                default: System.out.println(Cores.erro("Opcao invalida."));
            }
            if (noSubmenu) {
                pausar();
            }
        }
    }

    private void colher() {
        int hectares = agricultura.processarColheita(cidade);
        if (hectares == -1) {
            System.out.println(Cores.erro("A colheita deste ano ja foi feita."));
        } else {
            System.out.println(Cores.sucesso("Colheita feita: " + hectares + " hectares cultivados. Graos: " + cidade.getGraos()));
        }
    }

    private void distribuirAlimentos() {
        System.out.print("Percentual dos graos a distribuir (20 a 80): ");
        int percentual = lerInteiro();
        int servosAntes = cidade.getServos();
        if (agricultura.distribuirAlimentos(cidade, percentual)) {
            System.out.println(Cores.sucesso("Alimentos distribuidos. Servos: " + servosAntes + " -> " + cidade.getServos()));
        } else {
            System.out.println(Cores.erro("Percentual recusado: use de 20 a 80."));
        }
    }

    // ================= 5 - GOVERNO =================

    private void abrirGoverno() {
        if (!exigirJogoEmAndamento()) {
            return;
        }
        boolean noSubmenu = true;
        while (noSubmenu) {
            limparTela();
            System.out.println(Cores.titulo("--- GOVERNO (taxa " + cidade.getTaxaImposto() + "%, justica "
                    + cidade.getNivelJustica() + ") ---"));
            System.out.println("1 - Arrecadar impostos (1 vez por ano)");
            System.out.println("2 - Definir taxa de imposto (0 a 50%)");
            System.out.println("3 - Definir nivel de justica");
            System.out.println("0 - Voltar");
            System.out.print("Escolha uma opcao: ");
            switch (lerOpcao()) {
                case 1: arrecadarImpostos(); break;
                case 2: definirTaxaImposto(); break;
                case 3: definirJustica(); break;
                case 0: noSubmenu = false; break;
                default: System.out.println(Cores.erro("Opcao invalida."));
            }
            if (noSubmenu) {
                pausar();
            }
        }
    }

    private void arrecadarImpostos() {
        int arrecadado = governo.arrecadarImpostos(cidade);
        if (arrecadado == -1) {
            System.out.println(Cores.erro("Os impostos deste ano ja foram arrecadados."));
        } else {
            System.out.println(Cores.sucesso("Impostos arrecadados: " + arrecadado + " florins. Florins: " + cidade.getFlorins()));
        }
    }

    private void definirTaxaImposto() {
        System.out.print("Nova taxa (0 a 50): ");
        if (governo.definirTaxaImposto(cidade, lerInteiro())) {
            System.out.println(Cores.sucesso("Taxa definida: " + cidade.getTaxaImposto() + "%"));
        } else {
            System.out.println(Cores.erro("Taxa recusada: use de 0 a 50."));
        }
    }

    // A justiça é escolhida por número, para ninguém errar a digitação do nome.
    private void definirJustica() {
        System.out.println("1 - Justa (impostos x 0,8)");
        System.out.println("2 - Moderada (impostos x 1,0)");
        System.out.println("3 - Severa (impostos x 1,3)");
        System.out.print("Escolha o nivel: ");
        String nivel;
        switch (lerInteiro()) {
            case 1: nivel = "Justa"; break;
            case 2: nivel = "Moderada"; break;
            case 3: nivel = "Severa"; break;
            default: nivel = "";      // o Governo vai recusar
        }
        if (governo.definirJustica(cidade, nivel)) {
            System.out.println(Cores.sucesso("Nivel de justica: " + cidade.getNivelJustica()));
        } else {
            System.out.println(Cores.erro("Nivel invalido."));
        }
    }

    // ================= 6 - CONSTRUÇÕES =================

    private void abrirConstrucoes() {
        if (!exigirJogoEmAndamento()) {
            return;
        }
        boolean noSubmenu = true;
        while (noSubmenu) {
            limparTela();
            System.out.println(Cores.titulo("--- CONSTRUCOES (" + cidade.getFlorins() + " florins, "
                    + cidade.getServos() + " servos) ---"));
            System.out.println("1 - Recrutar soldados (" + construcoes.getPrecoSoldado() + " florins + 1 servo cada)");
            System.out.println("2 - Construir (" + construcoes.getPrecoConstrucao() + " florins cada)");
            System.out.println("0 - Voltar");
            System.out.print("Escolha uma opcao: ");
            switch (lerOpcao()) {
                case 1: recrutarSoldados(); break;
                case 2: construir(); break;
                case 0: noSubmenu = false; break;
                default: System.out.println(Cores.erro("Opcao invalida."));
            }
            if (noSubmenu) {
                pausar();
            }
        }
    }

    private void recrutarSoldados() {
        if (construcoes.recrutarSoldados(cidade, lerQuantidade())) {
            System.out.println(Cores.sucesso("Soldados: " + cidade.getSoldados() + ". Florins: " + cidade.getFlorins()));
        } else {
            System.out.println(Cores.erro("Recrutamento recusado (quantidade invalida, florins ou servos insuficientes)."));
        }
    }

    private void construir() {
        if (construcoes.construir(cidade, lerQuantidade())) {
            System.out.println(Cores.sucesso("Construcoes: " + cidade.getConstrucoes() + ". Florins: " + cidade.getFlorins()));
        } else {
            System.out.println(Cores.erro("Construcao recusada (quantidade invalida ou florins insuficientes)."));
        }
    }

    // ================= 7 - ENCERRAR TURNO =================

    /*
     * Passa a vez. Se quem encerrou foi o ÚLTIMO jogador, fecha o ano:
     * avança as cidades, anuncia promoções, mostra a classificação e
     * verifica se o jogo terminou.
     */
    private void encerrarTurno() {
        if (!exigirJogoEmAndamento()) {
            return;
        }
        System.out.println("Turno de " + cidade.getDono().getNome() + " encerrado.");

        // Guarda o ano ANTES, porque encerrarAno() já avança para o próximo.
        int anoQueTerminou = partida.getAnoAtual();
        boolean fimDoAno = partida.passarTurno();

        if (fimDoAno) {
            List<Cidade> promovidas = partida.encerrarAno(nobreza);

            System.out.println();
            System.out.println(Cores.destaque("========= FIM DO ANO " + anoQueTerminou + " ========="));
            for (Cidade promovida : promovidas) {
                System.out.println(Cores.destaque(promovida.getDono().getNome() + ", de " + promovida.getNome()
                        + ", agora e " + promovida.getDono().getTitulo() + "!"));
            }
            exibirClassificacao();

            if (partida.terminou()) {
                encerrarPartida();
                return;
            }
            System.out.println(Cores.destaque("Comeca o ano " + partida.getAnoAtual() + "."));
        }

        cidade = partida.getCidadeAtual();
        System.out.println(Cores.destaque("Agora e a vez de " + descreverDaVez() + "."));
    }

    // ================= 8 - CLASSIFICAÇÃO =================

    private void exibirClassificacao() {
        if (!exigirJogoEmAndamento()) {
            return;
        }
        System.out.println();
        System.out.println(Cores.titulo("----------- CLASSIFICACAO -----------"));
        for (Cidade c : partida.getCidades()) {
            Jogador dono = c.getDono();
            System.out.println(c.getNome() + " | " + dono.getNome() + " (" + dono.getTitulo()
                    + ") | prestigio " + nobreza.calcularPrestigio(c));

            int proximo = nobreza.prestigioParaProximoTitulo(dono);
            if (proximo == -1) {
                System.out.println("      (ja e Rei/Rainha)");
            } else {
                System.out.println("      (proximo titulo com " + proximo + ")");
            }
        }
    }

    // Mostra o vencedor e apaga a partida (partida = null).
    private void encerrarPartida() {
        Cidade vencedora = partida.determinarVencedor(nobreza);
        System.out.println();
        System.out.println(Cores.destaque("============ FIM DE JOGO ============"));
        System.out.println(Cores.destaque("Vencedor: " + vencedora.getDono().getNome() + " ("
                + vencedora.getDono().getTitulo() + "), de " + vencedora.getNome()
                + ", com prestigio " + nobreza.calcularPrestigio(vencedora) + "."));
        partida = null;
        cidade = null;
    }

    // ================= 9 - SOBRE =================

    private void exibirSobre() {
        System.out.println();
        System.out.println("Bolonha e Milano: estrategia economica na Italia medieval.");
        System.out.println("Cada jogador governa uma cidade-Estado e quer subir de titulo:");
        System.out.println("Cavaleiro, Barao, Conde, Marques, Duque e Rei/Rainha.");
        System.out.println("Prestigio = construcoes x 10 + soldados x 2 + servos + terras / 100.");
        System.out.println("Florins guardados nao contam. So se sobe um titulo por ano.");
        System.out.println("O jogo acaba quando alguem vira Rei/Rainha ou ao fim de 1410.");
    }

    // Texto do tipo "Ana (Cavaleiro), de Bolonha".
    private String descreverDaVez() {
        return cidade.getDono().getNome() + " (" + cidade.getDono().getTitulo() + "), de " + cidade.getNome();
    }
}
