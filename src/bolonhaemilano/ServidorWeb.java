package bolonhaemilano;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * SERVIDOR WEB: expõe as regras reais do jogo via HTTP + JSON e serve a interface
 * visual (pasta web/) usando apenas a biblioteca padrão do JDK 17.
 */
public class ServidorWeb {

    private static final String[] CIDADES_PADRAO = { "Bolonha", "Milano", "Florença", "Veneza" };
    private static final String[] TITULOS_EXIBICAO = {
        "Cavaleiro", "Barão", "Conde", "Marquês", "Duque", "Rei/Rainha"
    };
    private static final int MAX_HISTORICO = 60;

    private final Object lock = new Object();
    private final Mercado mercado = new Mercado();
    private final Agricultura agricultura = new Agricultura();
    private final Governo governo = new Governo();
    private final Construcoes construcoes = new Construcoes();
    private final Nobreza nobreza = new Nobreza();

    private Partida partida;
    private boolean terminada;
    private long proximoIdEvento = 1;
    private final List<EventoHistorico> historico = new ArrayList<>();
    private ResumoAno ultimoResumoAno;
    private Path webDir;
    private HttpServer servidor;
    private ExecutorService executor;
    private int portaAtiva;

    public void iniciar(int portaPreferida, boolean abrirNavegador) throws IOException {
        this.webDir = localizarDiretorioWeb();
        this.servidor = criarServidor(portaPreferida);
        this.portaAtiva = servidor.getAddress().getPort();

        registrarRotas(servidor);
        this.executor = Executors.newFixedThreadPool(4);
        servidor.setExecutor(this.executor);
        servidor.start();

        String url = "http://localhost:" + this.portaAtiva;
        System.out.println("==================================================");
        System.out.println("  BOLONHA E MILANO — Interface Visual");
        System.out.println("  Servidor rodando em: " + url);
        System.out.println("  Pasta estática: " + webDir);
        System.out.println("  Pressione Ctrl+C para encerrar.");
        System.out.println("==================================================");

        if (abrirNavegador) {
            abrirNoNavegador(url);
        }
    }

    public int getPortaAtiva() {
        return portaAtiva;
    }

    public void parar() {
        if (servidor != null) {
            servidor.stop(0);
            servidor = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private HttpServer criarServidor(int portaPreferida) throws IOException {
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        if (portaPreferida == 0) {
            return HttpServer.create(new InetSocketAddress(loopback, 0), 0);
        }
        IOException ultimoErro = null;
        for (int tentativa = 0; tentativa <= 10; tentativa++) {
            int porta = portaPreferida + tentativa;
            try {
                return HttpServer.create(new InetSocketAddress(loopback, porta), 0);
            } catch (IOException e) {
                ultimoErro = e;
            }
        }
        throw ultimoErro != null ? ultimoErro : new IOException("Não foi possível iniciar o servidor HTTP.");
    }

    private Path localizarDiretorioWeb() {
        Path local = Paths.get("web").toAbsolutePath().normalize();
        if (Files.isDirectory(local)) {
            return local;
        }
        try {
            Path codeBase = Paths.get(ServidorWeb.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path parent = codeBase.getParent();
            if (parent != null) {
                Path candidato = parent.resolve("web").toAbsolutePath().normalize();
                if (Files.isDirectory(candidato)) {
                    return candidato;
                }
            }
        } catch (Exception ignored) {
        }
        return local;
    }

    private void abrirNoNavegador(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            System.out.println("Não foi possível abrir o navegador automaticamente. Acesse: " + url);
        }
    }

    private void registrarRotas(HttpServer servidor) {
        servidor.createContext("/api/estado", ex -> tratarRequisicao(ex, "GET", this::handleEstado));
        servidor.createContext("/api/novo-jogo", ex -> tratarRequisicao(ex, "POST", this::handleNovoJogo));
        servidor.createContext("/api/mercado/comprar-graos", ex -> tratarRequisicao(ex, "POST", this::handleComprarGraos));
        servidor.createContext("/api/mercado/vender-graos", ex -> tratarRequisicao(ex, "POST", this::handleVenderGraos));
        servidor.createContext("/api/mercado/comprar-terras", ex -> tratarRequisicao(ex, "POST", this::handleComprarTerras));
        servidor.createContext("/api/mercado/vender-terras", ex -> tratarRequisicao(ex, "POST", this::handleVenderTerras));
        servidor.createContext("/api/agricultura/colher", ex -> tratarRequisicao(ex, "POST", this::handleColher));
        servidor.createContext("/api/agricultura/distribuir", ex -> tratarRequisicao(ex, "POST", this::handleDistribuir));
        servidor.createContext("/api/governo/arrecadar", ex -> tratarRequisicao(ex, "POST", this::handleArrecadar));
        servidor.createContext("/api/governo/taxa", ex -> tratarRequisicao(ex, "POST", this::handleTaxa));
        servidor.createContext("/api/governo/justica", ex -> tratarRequisicao(ex, "POST", this::handleJustica));
        servidor.createContext("/api/construcoes/recrutar", ex -> tratarRequisicao(ex, "POST", this::handleRecrutar));
        servidor.createContext("/api/construcoes/construir", ex -> tratarRequisicao(ex, "POST", this::handleConstruir));
        servidor.createContext("/api/turno/encerrar", ex -> tratarRequisicao(ex, "POST", this::handleEncerrarTurno));
        servidor.createContext("/", new StaticFileHandler());
    }

    @FunctionalInterface
    private interface ApiHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private void tratarRequisicao(HttpExchange exchange, String metodoEsperado, ApiHandler handler) throws IOException {
        try {
            if (!metodoEsperado.equalsIgnoreCase(exchange.getRequestMethod())) {
                enviarJson(exchange, 405, jsonErro("Método não permitido. Use " + metodoEsperado + "."));
                return;
            }
            handler.handle(exchange);
        } catch (RequisicaoInvalidaException e) {
            enviarJson(exchange, 400, jsonErro(e.getMessage()));
        } catch (Exception e) {
            enviarJson(exchange, 500, jsonErro("Erro interno do servidor: " + safeText(e.getMessage())));
        }
    }

    // ================= HANDLERS DE API =================

    private void handleEstado(HttpExchange exchange) throws IOException {
        synchronized (lock) {
            enviarJson(exchange, 200, construirEstadoJson());
        }
    }

    private void handleNovoJogo(HttpExchange exchange) throws IOException {
        Map<String, Object> body = lerObjetoJson(exchange);
        Object rawJogadores = body.get("jogadores");
        if (!(rawJogadores instanceof List<?> lista) || lista.isEmpty() || lista.size() > Partida.MAXIMO_JOGADORES) {
            throw new RequisicaoInvalidaException("Informe de 1 a 4 jogadores em 'jogadores'.");
        }

        Partida novaPartida = new Partida();
        for (int i = 0; i < lista.size(); i++) {
            Object item = lista.get(i);
            if (!(item instanceof Map<?, ?> mapa)) {
                throw new RequisicaoInvalidaException("Cada jogador deve ser um objeto JSON com nome e cidade.");
            }
            String nomeJogador = asTrimmedString(mapa.get("nome"));
            if (nomeJogador.isEmpty()) {
                nomeJogador = "Jogador " + (i + 1);
            }
            String nomeCidade = asTrimmedString(mapa.get("cidade"));
            if (nomeCidade.isEmpty()) {
                nomeCidade = CIDADES_PADRAO[i];
            }
            novaPartida.adicionarCidade(new Cidade(nomeCidade, new Jogador(nomeJogador)));
        }

        synchronized (lock) {
            this.partida = novaPartida;
            this.terminada = false;
            this.ultimoResumoAno = null;
            this.historico.clear();
            this.proximoIdEvento = 1;

            Cidade inicial = novaPartida.getCidadeAtual();
            String msg = "Nova partida iniciada no ano " + novaPartida.getAnoAtual()
                    + ". Começa " + inicial.getDono().getNome() + " (" + inicial.getNome() + ").";
            registrarEvento("destaque", msg);
            enviarJson(exchange, 200, jsonRespostaAcao(true, msg));
        }
    }

    private void handleComprarGraos(HttpExchange exchange) throws IOException {
        int qtd = exigirInteiro(lerObjetoJson(exchange), "quantidade");
        synchronized (lock) {
            Cidade atual = exigirJogoEmAndamento();
            boolean ok = mercado.comprarGraos(atual, qtd);
            String msg = ok
                    ? atual.getNome() + " comprou " + qtd + " grãos por " + (qtd * 2) + " florins."
                    : "Compra de grãos recusada (quantidade inválida ou florins insuficientes).";
            registrarEvento(ok ? "sucesso" : "erro", msg);
            enviarJson(exchange, 200, jsonRespostaAcao(ok, msg));
        }
    }

    private void handleVenderGraos(HttpExchange exchange) throws IOException {
        int qtd = exigirInteiro(lerObjetoJson(exchange), "quantidade");
        synchronized (lock) {
            Cidade atual = exigirJogoEmAndamento();
            boolean ok = mercado.venderGraos(atual, qtd);
            String msg = ok
                    ? atual.getNome() + " vendeu " + qtd + " grãos por " + (qtd * 2) + " florins."
                    : "Venda de grãos recusada (quantidade inválida ou grãos insuficientes).";
            registrarEvento(ok ? "sucesso" : "erro", msg);
            enviarJson(exchange, 200, jsonRespostaAcao(ok, msg));
        }
    }

    private void handleComprarTerras(HttpExchange exchange) throws IOException {
        int qtd = exigirInteiro(lerObjetoJson(exchange), "quantidade");
        synchronized (lock) {
            Cidade atual = exigirJogoEmAndamento();
            boolean ok = mercado.comprarTerras(atual, qtd);
            String msg = ok
                    ? atual.getNome() + " adquiriu " + qtd + " ha de terras por " + qtd + " florins."
                    : "Compra de terras recusada (quantidade inválida ou florins insuficientes).";
            registrarEvento(ok ? "sucesso" : "erro", msg);
            enviarJson(exchange, 200, jsonRespostaAcao(ok, msg));
        }
    }

    private void handleVenderTerras(HttpExchange exchange) throws IOException {
        int qtd = exigirInteiro(lerObjetoJson(exchange), "quantidade");
        synchronized (lock) {
            Cidade atual = exigirJogoEmAndamento();
            boolean ok = mercado.venderTerras(atual, qtd);
            String msg = ok
                    ? atual.getNome() + " vendeu " + qtd + " ha de terras por " + qtd + " florins."
                    : "Venda de terras recusada (quantidade inválida ou terras insuficientes).";
            registrarEvento(ok ? "sucesso" : "erro", msg);
            enviarJson(exchange, 200, jsonRespostaAcao(ok, msg));
        }
    }

    private void handleColher(HttpExchange exchange) throws IOException {
        synchronized (lock) {
            Cidade atual = exigirJogoEmAndamento();
            int graosAntes = atual.getGraos();
            int hectares = agricultura.processarColheita(atual);
            boolean ok = hectares != -1;
            String msg;
            if (ok) {
                int saldoGraos = atual.getGraos() - graosAntes;
                msg = atual.getNome() + " colheu " + hectares + " hectares (estoque de grãos: "
                        + atual.getGraos() + ", saldo +" + saldoGraos + ").";
            } else {
                msg = "A colheita deste ano já foi realizada em " + atual.getNome() + ".";
            }
            registrarEvento(ok ? "sucesso" : "erro", msg);
            enviarJson(exchange, 200, jsonRespostaAcao(ok, msg));
        }
    }

    private void handleDistribuir(HttpExchange exchange) throws IOException {
        int percentual = exigirInteiro(lerObjetoJson(exchange), "percentual");
        synchronized (lock) {
            Cidade atual = exigirJogoEmAndamento();
            int servosAntes = atual.getServos();
            int graosAntes = atual.getGraos();
            boolean ok = agricultura.distribuirAlimentos(atual, percentual);
            int deltaServos = atual.getServos() - servosAntes;
            int graosGastos = graosAntes - atual.getGraos();
            String msg;
            if (ok) {
                String variacao = deltaServos >= 0 ? "+" + deltaServos : String.valueOf(deltaServos);
                msg = atual.getNome() + " distribuiu " + percentual + "% dos grãos (" + graosGastos
                        + " grãos). Servos: " + servosAntes + " → " + atual.getServos() + " (" + variacao + ").";
            } else {
                msg = "Distribuição recusada: escolha um percentual entre 20% e 80%.";
            }
            registrarEvento(ok ? "sucesso" : "erro", msg);

            StringBuilder extra = new StringBuilder();
            extra.append("\"servosAntes\":").append(servosAntes)
                 .append(",\"servosDepois\":").append(atual.getServos())
                 .append(",\"deltaServos\":").append(deltaServos)
                 .append(",\"graosGastos\":").append(graosGastos);
            enviarJson(exchange, 200, jsonRespostaAcao(ok, msg, extra.toString()));
        }
    }

    private void handleArrecadar(HttpExchange exchange) throws IOException {
        synchronized (lock) {
            Cidade atual = exigirJogoEmAndamento();
            int arrecadado = governo.arrecadarImpostos(atual);
            boolean ok = arrecadado != -1;
            String msg = ok
                    ? atual.getNome() + " arrecadou " + arrecadado + " florins em impostos."
                    : "Os impostos deste ano já foram arrecadados em " + atual.getNome() + ".";
            registrarEvento(ok ? "sucesso" : "erro", msg);
            enviarJson(exchange, 200, jsonRespostaAcao(ok, msg));
        }
    }

    private void handleTaxa(HttpExchange exchange) throws IOException {
        int taxa = exigirInteiro(lerObjetoJson(exchange), "taxa");
        synchronized (lock) {
            Cidade atual = exigirJogoEmAndamento();
            boolean ok = governo.definirTaxaImposto(atual, taxa);
            String msg = ok
                    ? "Taxa de imposto de " + atual.getNome() + " ajustada para " + taxa + "%."
                    : "Taxa de imposto recusada: informe um valor entre 0% e 50%.";
            registrarEvento(ok ? "sucesso" : "erro", msg);
            enviarJson(exchange, 200, jsonRespostaAcao(ok, msg));
        }
    }

    private void handleJustica(HttpExchange exchange) throws IOException {
        String nivel = exigirTexto(lerObjetoJson(exchange), "nivel");
        synchronized (lock) {
            Cidade atual = exigirJogoEmAndamento();
            boolean ok = governo.definirJustica(atual, nivel);
            String msg = ok
                    ? "Nível de justiça de " + atual.getNome() + " definido como " + nivel + "."
                    : "Nível de justiça inválido. Use Justa, Moderada ou Severa.";
            registrarEvento(ok ? "sucesso" : "erro", msg);
            enviarJson(exchange, 200, jsonRespostaAcao(ok, msg));
        }
    }

    private void handleRecrutar(HttpExchange exchange) throws IOException {
        int qtd = exigirInteiro(lerObjetoJson(exchange), "quantidade");
        synchronized (lock) {
            Cidade atual = exigirJogoEmAndamento();
            boolean ok = construcoes.recrutarSoldados(atual, qtd);
            String msg = ok
                    ? atual.getNome() + " recrutou " + qtd + " soldados (" + (qtd * construcoes.getPrecoSoldado()) + " florins e " + qtd + " servos)."
                    : "Recrutamento recusado (quantidade inválida, florins ou servos insuficientes).";
            registrarEvento(ok ? "sucesso" : "erro", msg);
            enviarJson(exchange, 200, jsonRespostaAcao(ok, msg));
        }
    }

    private void handleConstruir(HttpExchange exchange) throws IOException {
        int qtd = exigirInteiro(lerObjetoJson(exchange), "quantidade");
        synchronized (lock) {
            Cidade atual = exigirJogoEmAndamento();
            boolean ok = construcoes.construir(atual, qtd);
            String msg = ok
                    ? atual.getNome() + " ergueu " + qtd + " construção(ões) por " + (qtd * construcoes.getPrecoConstrucao()) + " florins."
                    : "Obra recusada (quantidade inválida ou florins insuficientes).";
            registrarEvento(ok ? "sucesso" : "erro", msg);
            enviarJson(exchange, 200, jsonRespostaAcao(ok, msg));
        }
    }

    private void handleEncerrarTurno(HttpExchange exchange) throws IOException {
        synchronized (lock) {
            Cidade encerrando = exigirJogoEmAndamento();
            int anoQueTerminou = partida.getAnoAtual();
            registrarEvento("info", "Turno de " + encerrando.getDono().getNome() + " (" + encerrando.getNome() + ") encerrado.");

            boolean fimDoAno = partida.passarTurno();
            String msg;

            if (fimDoAno) {
                List<Cidade> todas = partida.getCidades();
                Map<Cidade, String> titulosAnteriores = new LinkedHashMap<>();
                for (Cidade c : todas) {
                    titulosAnteriores.put(c, tituloBonito(c.getDono()));
                }

                List<Cidade> promovidas = partida.encerrarAno(nobreza);
                List<PromocaoInfo> promocoesInfo = new ArrayList<>();
                for (Cidade p : promovidas) {
                    String anterior = titulosAnteriores.get(p);
                    String novo = tituloBonito(p.getDono());
                    int prestigio = nobreza.calcularPrestigio(p);
                    promocoesInfo.add(new PromocaoInfo(p.getNome(), p.getDono().getNome(), anterior, novo, prestigio));
                }
                ultimoResumoAno = new ResumoAno(anoQueTerminou, promocoesInfo);

                registrarEvento("destaque", "Fim do ano " + anoQueTerminou + ".");
                for (PromocaoInfo info : promocoesInfo) {
                    registrarEvento("destaque", info.jogador + " (" + info.cidade + ") subiu de "
                            + info.tituloAnterior + " para " + info.novoTitulo + "!");
                }

                if (partida.terminou()) {
                    terminada = true;
                    Cidade vencedora = partida.determinarVencedor(nobreza);
                    msg = "Fim de jogo! " + vencedora.getDono().getNome() + " (" + tituloBonito(vencedora.getDono())
                            + "), de " + vencedora.getNome() + ", venceu a partida!";
                    registrarEvento("destaque", msg);
                } else {
                    Cidade proxima = partida.getCidadeAtual();
                    msg = "Ano " + anoQueTerminou + " encerrado! Começa " + partida.getAnoAtual()
                            + " — vez de " + proxima.getDono().getNome() + " (" + proxima.getNome() + ").";
                }
            } else {
                Cidade proxima = partida.getCidadeAtual();
                msg = "Agora é a vez de " + proxima.getDono().getNome() + " (" + proxima.getNome() + ").";
            }

            String extra = "\"fimDoAno\":" + fimDoAno + ",\"terminada\":" + terminada;
            enviarJson(exchange, 200, jsonRespostaAcao(true, msg, extra));
        }
    }

    // ================= AUXILIARES DE DOMÍNIO =================

    private Cidade exigirJogoEmAndamento() {
        if (partida == null) {
            throw new RequisicaoInvalidaException("Nenhum jogo em andamento. Inicie uma nova partida.");
        }
        if (terminada) {
            throw new RequisicaoInvalidaException("A partida já terminou. Inicie uma nova partida para continuar jogando.");
        }
        return partida.getCidadeAtual();
    }

    private void registrarEvento(String tipo, String mensagem) {
        int ano = partida != null ? Math.min(partida.getAnoAtual(), partida.getAnoFinal()) : 1400;
        historico.add(0, new EventoHistorico(proximoIdEvento++, ano, tipo, mensagem));
        if (historico.size() > MAX_HISTORICO) {
            historico.remove(historico.size() - 1);
        }
    }

    private static String tituloBonito(Jogador jogador) {
        int nivel = jogador.getNivelTitulo();
        if (nivel >= 0 && nivel < TITULOS_EXIBICAO.length) {
            return TITULOS_EXIBICAO[nivel];
        }
        return jogador.getTitulo();
    }

    private static String nomeTituloPorNivel(int nivel) {
        if (nivel >= 0 && nivel < TITULOS_EXIBICAO.length) {
            return TITULOS_EXIBICAO[nivel];
        }
        return "";
    }

    // ================= SERIALIZAÇÃO JSON =================

    private String jsonRespostaAcao(boolean ok, String mensagem) {
        return jsonRespostaAcao(ok, mensagem, null);
    }

    private String jsonRespostaAcao(boolean ok, String mensagem, String camposExtras) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"ok\":").append(ok);
        sb.append(",\"mensagem\":").append(jsonString(mensagem));
        if (camposExtras != null && !camposExtras.isEmpty()) {
            sb.append(",").append(camposExtras);
        }
        sb.append(",\"estado\":").append(construirEstadoJson());
        sb.append("}");
        return sb.toString();
    }

    private String jsonErro(String erro) {
        return "{\"ok\":false,\"erro\":" + jsonString(erro) + ",\"mensagem\":" + jsonString(erro) + "}";
    }

    private String construirEstadoJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean emAndamento = partida != null && !terminada;
        sb.append("\"emAndamento\":").append(emAndamento);
        sb.append(",\"possuiPartida\":").append(partida != null);
        sb.append(",\"terminada\":").append(terminada);

        if (partida == null) {
            sb.append(",\"anoAtual\":1400");
            sb.append(",\"anoFinal\":1410");
            sb.append(",\"indiceAtual\":0");
            sb.append(",\"ultimoJogadorDoAno\":false");
            sb.append(",\"cidadeAtual\":null");
            sb.append(",\"cidades\":[]");
            sb.append(",\"vencedor\":null");
        } else {
            List<Cidade> cidades = partida.getCidades();
            Cidade atual = partida.getCidadeAtual();
            int indiceAtual = 0;
            for (int i = 0; i < cidades.size(); i++) {
                if (cidades.get(i) == atual) {
                    indiceAtual = i;
                    break;
                }
            }
            int anoExibido = terminada
                    ? Math.min(partida.getAnoAtual(), partida.getAnoFinal())
                    : partida.getAnoAtual();
            boolean ultimoDoAno = !cidades.isEmpty() && indiceAtual == cidades.size() - 1;

            sb.append(",\"anoAtual\":").append(anoExibido);
            sb.append(",\"anoFinal\":").append(partida.getAnoFinal());
            sb.append(",\"indiceAtual\":").append(indiceAtual);
            sb.append(",\"ultimoJogadorDoAno\":").append(ultimoDoAno);
            sb.append(",\"cidadeAtual\":").append(cidadeParaJson(atual, indiceAtual, true));

            sb.append(",\"cidades\":[");
            for (int i = 0; i < cidades.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(cidadeParaJson(cidades.get(i), i, i == indiceAtual));
            }
            sb.append("]");

            if (terminada) {
                Cidade vencedora = partida.determinarVencedor(nobreza);
                int idxVencedora = cidades.indexOf(vencedora);
                sb.append(",\"vencedor\":").append(cidadeParaJson(vencedora, idxVencedora, false));
            } else {
                sb.append(",\"vencedor\":null");
            }
        }

        sb.append(",\"ultimoResumoAno\":").append(resumoAnoParaJson(ultimoResumoAno));

        sb.append(",\"historico\":[");
        for (int i = 0; i < historico.size(); i++) {
            if (i > 0) sb.append(",");
            EventoHistorico ev = historico.get(i);
            sb.append("{")
              .append("\"id\":").append(ev.id)
              .append(",\"ano\":").append(ev.ano)
              .append(",\"tipo\":").append(jsonString(ev.tipo))
              .append(",\"mensagem\":").append(jsonString(ev.mensagem))
              .append("}");
        }
        sb.append("]");

        sb.append(",\"precos\":{")
          .append("\"grao\":2")
          .append(",\"terra\":1")
          .append(",\"soldado\":5")
          .append(",\"servosPorSoldado\":1")
          .append(",\"construcao\":20")
          .append("}");

        sb.append("}");
        return sb.toString();
    }

    private String cidadeParaJson(Cidade c, int indice, boolean ehAtual) {
        Jogador dono = c.getDono();
        int prestigio = nobreza.calcularPrestigio(c);
        int proximoPrestigio = nobreza.prestigioParaProximoTitulo(dono);
        String proximoTitulo = dono.ehMonarca() ? "" : nomeTituloPorNivel(dono.getNivelTitulo() + 1);

        int ptsConstrucoes = c.getConstrucoes() * 10;
        int ptsSoldados = c.getSoldados() * 2;
        int ptsServos = c.getServos();
        int ptsTerras = c.getTerras() / 100;

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"indice\":").append(indice);
        sb.append(",\"ehAtual\":").append(ehAtual);
        sb.append(",\"nome\":").append(jsonString(c.getNome()));
        sb.append(",\"jogador\":").append(jsonString(dono.getNome()));
        sb.append(",\"titulo\":").append(jsonString(tituloBonito(dono)));
        sb.append(",\"tituloOriginal\":").append(jsonString(dono.getTitulo()));
        sb.append(",\"nivelTitulo\":").append(dono.getNivelTitulo());
        sb.append(",\"ehMonarca\":").append(dono.ehMonarca());
        sb.append(",\"anoAtual\":").append(c.getAnoAtual());
        sb.append(",\"florins\":").append(c.getFlorins());
        sb.append(",\"terras\":").append(c.getTerras());
        sb.append(",\"graos\":").append(c.getGraos());
        sb.append(",\"servos\":").append(c.getServos());
        sb.append(",\"taxaImposto\":").append(c.getTaxaImposto());
        sb.append(",\"nivelJustica\":").append(jsonString(c.getNivelJustica()));
        sb.append(",\"soldados\":").append(c.getSoldados());
        sb.append(",\"construcoes\":").append(c.getConstrucoes());
        sb.append(",\"colheitaFeita\":").append(c.isColheitaFeita());
        sb.append(",\"impostosArrecadados\":").append(c.isImpostosArrecadados());
        sb.append(",\"prestigio\":").append(prestigio);
        sb.append(",\"proximoTituloPrestigio\":").append(proximoPrestigio);
        sb.append(",\"proximoTituloNome\":").append(proximoPrestigio == -1 ? "null" : jsonString(proximoTitulo));
        sb.append(",\"decomposicaoPrestigio\":{")
          .append("\"construcoes\":").append(ptsConstrucoes)
          .append(",\"soldados\":").append(ptsSoldados)
          .append(",\"servos\":").append(ptsServos)
          .append(",\"terras\":").append(ptsTerras)
          .append("}");
        sb.append("}");
        return sb.toString();
    }

    private String resumoAnoParaJson(ResumoAno resumo) {
        if (resumo == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"anoEncerrado\":").append(resumo.anoEncerrado);
        sb.append(",\"promocoes\":[");
        for (int i = 0; i < resumo.promocoes.size(); i++) {
            if (i > 0) sb.append(",");
            PromocaoInfo p = resumo.promocoes.get(i);
            sb.append("{")
              .append("\"cidade\":").append(jsonString(p.cidade))
              .append(",\"jogador\":").append(jsonString(p.jogador))
              .append(",\"tituloAnterior\":").append(jsonString(p.tituloAnterior))
              .append(",\"novoTitulo\":").append(jsonString(p.novoTitulo))
              .append(",\"prestigio\":").append(p.prestigio)
              .append("}");
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String jsonString(String valor) {
        if (valor == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < valor.length(); i++) {
            char ch = valor.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    // ================= LEITURA E PARSER JSON =================

    private Map<String, Object> lerObjetoJson(HttpExchange exchange) throws IOException {
        String corpo = lerCorpoUtf8(exchange);
        if (corpo.trim().isEmpty()) {
            throw new RequisicaoInvalidaException("Corpo JSON obrigatório.");
        }
        Object parsed;
        try {
            parsed = new JsonParser(corpo).parse();
        } catch (IllegalArgumentException e) {
            throw new RequisicaoInvalidaException("JSON inválido: " + e.getMessage());
        }
        if (!(parsed instanceof Map<?, ?> rawMap)) {
            throw new RequisicaoInvalidaException("O corpo deve ser um objeto JSON.");
        }
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            resultado.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return resultado;
    }

    private String lerCorpoUtf8(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private int exigirInteiro(Map<String, Object> mapa, String chave) {
        if (!mapa.containsKey(chave) || mapa.get(chave) == null) {
            throw new RequisicaoInvalidaException("Campo '" + chave + "' é obrigatório.");
        }
        Object valor = mapa.get(chave);
        if (valor instanceof Number num) {
            double d = num.doubleValue();
            if (d != Math.rint(d) || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
                throw new RequisicaoInvalidaException("Campo '" + chave + "' deve ser um número inteiro.");
            }
            return (int) d;
        }
        if (valor instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException e) {
                throw new RequisicaoInvalidaException("Campo '" + chave + "' deve ser um número inteiro.");
            }
        }
        throw new RequisicaoInvalidaException("Campo '" + chave + "' deve ser um número inteiro.");
    }

    private String exigirTexto(Map<String, Object> mapa, String chave) {
        if (!mapa.containsKey(chave) || mapa.get(chave) == null) {
            throw new RequisicaoInvalidaException("Campo '" + chave + "' é obrigatório.");
        }
        Object valor = mapa.get(chave);
        if (!(valor instanceof String str)) {
            throw new RequisicaoInvalidaException("Campo '" + chave + "' deve ser texto.");
        }
        return str.trim();
    }

    private static String asTrimmedString(Object valor) {
        return valor == null ? "" : String.valueOf(valor).trim();
    }

    private static String safeText(String s) {
        return s == null ? "erro desconhecido" : s;
    }

    private void enviarJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ================= SERVIDOR DE ARQUIVOS ESTÁTICOS =================

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())
                    && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                enviarTexto(exchange, 405, "text/plain; charset=utf-8", "Método não permitido");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/")) {
                path = "/index.html";
            }

            String relativo = path.startsWith("/") ? path.substring(1) : path;
            Path destino = webDir.resolve(relativo).normalize();
            if (!destino.startsWith(webDir) || Files.isDirectory(destino) || !Files.exists(destino)) {
                enviarTexto(exchange, 404, "text/plain; charset=utf-8", "Arquivo não encontrado: " + path);
                return;
            }

            byte[] conteudo = Files.readAllBytes(destino);
            exchange.getResponseHeaders().set("Content-Type", contentType(destino.getFileName().toString()));
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(200, conteudo.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(conteudo);
                }
            }
        }

        private String contentType(String nomeArquivo) {
            String lower = nomeArquivo.toLowerCase();
            if (lower.endsWith(".html")) return "text/html; charset=utf-8";
            if (lower.endsWith(".css")) return "text/css; charset=utf-8";
            if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
            if (lower.endsWith(".svg")) return "image/svg+xml; charset=utf-8";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".json")) return "application/json; charset=utf-8";
            return "application/octet-stream";
        }

        private void enviarTexto(HttpExchange exchange, int status, String contentType, String texto) throws IOException {
            byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // ================= ESTRUTURAS INTERNAS =================

    private static class EventoHistorico {
        final long id;
        final int ano;
        final String tipo;
        final String mensagem;

        EventoHistorico(long id, int ano, String tipo, String mensagem) {
            this.id = id;
            this.ano = ano;
            this.tipo = tipo;
            this.mensagem = mensagem;
        }
    }

    private static class PromocaoInfo {
        final String cidade;
        final String jogador;
        final String tituloAnterior;
        final String novoTitulo;
        final int prestigio;

        PromocaoInfo(String cidade, String jogador, String tituloAnterior, String novoTitulo, int prestigio) {
            this.cidade = cidade;
            this.jogador = jogador;
            this.tituloAnterior = tituloAnterior;
            this.novoTitulo = novoTitulo;
            this.prestigio = prestigio;
        }
    }

    private static class ResumoAno {
        final int anoEncerrado;
        final List<PromocaoInfo> promocoes;

        ResumoAno(int anoEncerrado, List<PromocaoInfo> promocoes) {
            this.anoEncerrado = anoEncerrado;
            this.promocoes = promocoes;
        }
    }

    private static class RequisicaoInvalidaException extends RuntimeException {
        RequisicaoInvalidaException(String message) {
            super(message);
        }
    }

    // Parser JSON mínimo sem dependências externas
    private static class JsonParser {
        private final String src;
        private int pos;

        JsonParser(String src) {
            this.src = src;
        }

        Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (pos != src.length()) {
                throw new IllegalArgumentException("Conteúdo inesperado na posição " + pos);
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (pos >= src.length()) {
                throw new IllegalArgumentException("Fim inesperado do JSON");
            }
            char c = src.charAt(pos);
            switch (c) {
                case '{': return parseObject();
                case '[': return parseArray();
                case '"': return parseString();
                case 't': return parseLiteral("true", Boolean.TRUE);
                case 'f': return parseLiteral("false", Boolean.FALSE);
                case 'n': return parseLiteral("null", null);
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return parseNumber();
                    }
                    throw new IllegalArgumentException("Caractere inesperado '" + c + "' na posição " + pos);
            }
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (!peek('"')) {
                    throw new IllegalArgumentException("Esperada chave string na posição " + pos);
                }
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object val = parseValue();
                map.put(key, val);
                skipWhitespace();
                if (peek(',')) {
                    pos++;
                } else if (peek('}')) {
                    pos++;
                    return map;
                } else {
                    throw new IllegalArgumentException("Esperado ',' ou '}' na posição " + pos);
                }
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                pos++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(',')) {
                    pos++;
                } else if (peek(']')) {
                    pos++;
                    return list;
                } else {
                    throw new IllegalArgumentException("Esperado ',' ou ']' na posição " + pos);
                }
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (pos >= src.length()) {
                        throw new IllegalArgumentException("Escape incompleto");
                    }
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 > src.length()) {
                                throw new IllegalArgumentException("Escape unicode inválido");
                            }
                            String hex = src.substring(pos, pos + 4);
                            pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                            break;
                        default:
                            throw new IllegalArgumentException("Escape inválido \\" + esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("String não terminada");
        }

        private Object parseLiteral(String literal, Object value) {
            if (!src.startsWith(literal, pos)) {
                throw new IllegalArgumentException("Esperado '" + literal + "' na posição " + pos);
            }
            pos += literal.length();
            return value;
        }

        private Number parseNumber() {
            int start = pos;
            if (peek('-')) pos++;
            if (pos >= src.length() || !Character.isDigit(src.charAt(pos))) {
                throw new IllegalArgumentException("Número inválido na posição " + start);
            }
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            boolean isFloat = false;
            if (peek('.')) {
                isFloat = true;
                pos++;
                if (pos >= src.length() || !Character.isDigit(src.charAt(pos))) {
                    throw new IllegalArgumentException("Fração decimal inválida na posição " + start);
                }
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            }
            if (peek('e') || peek('E')) {
                isFloat = true;
                pos++;
                if (peek('+') || peek('-')) pos++;
                if (pos >= src.length() || !Character.isDigit(src.charAt(pos))) {
                    throw new IllegalArgumentException("Expoente inválido na posição " + start);
                }
                while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
            }
            String numStr = src.substring(start, pos);
            if (isFloat) {
                return Double.parseDouble(numStr);
            }
            long l = Long.parseLong(numStr);
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                return (int) l;
            }
            return l;
        }

        private void skipWhitespace() {
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        private void expect(char c) {
            if (pos >= src.length() || src.charAt(pos) != c) {
                throw new IllegalArgumentException("Esperado '" + c + "' na posição " + pos);
            }
            pos++;
        }

        private boolean peek(char c) {
            return pos < src.length() && src.charAt(pos) == c;
        }
    }
}
