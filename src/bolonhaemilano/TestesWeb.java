package bolonhaemilano;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * TESTES WEB: bateria de testes de integração para o ServidorWeb e toda a API HTTP/JSON.
 *
 * Usa apenas java.net.http.HttpClient (JDK 17), sem dependências externas:
 *     java -cp bin bolonhaemilano.TestesWeb
 */
public class TestesWeb {

    private static int aprovados = 0;
    private static int reprovados = 0;
    private static HttpClient client;
    private static String baseUrl;

    public static void main(String[] args) throws Exception {
        System.out.println("===== TESTES DA API WEB - BOLONHA E MILANO =====");

        ServidorWeb servidor = new ServidorWeb();
        servidor.iniciar(0, false);
        baseUrl = "http://127.0.0.1:" + servidor.getPortaAtiva();
        client = HttpClient.newHttpClient();

        try {
            testarArquivosEstaticos();
            testarProtocoloEValidacoes();
            testarMercado();
            testarAgricultura();
            testarGoverno();
            testarConstrucoes();
            testarTurnosEPromocaoAnual();
            testarVitoriaPorMonarca();
            testarVitoriaPorAno1410();
        } finally {
            servidor.parar();
        }

        System.out.println();
        System.out.println("Aprovados: " + aprovados + " | Reprovados: " + reprovados);
        if (reprovados > 0) {
            System.exit(1);
        }
    }

    private static void verificar(String descricao, boolean condicao) {
        if (condicao) {
            aprovados++;
            System.out.println("[OK] " + descricao);
        } else {
            reprovados++;
            System.out.println("[FALHA] " + descricao);
        }
    }

    private static HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> post(String path, String json) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void iniciarPartidaPadrao2Jogadores() throws IOException, InterruptedException {
        HttpResponse<String> r = post("/api/novo-jogo",
                "{\"jogadores\":[{\"nome\":\"Ana\",\"cidade\":\"Bolonha\"},{\"nome\":\"\",\"cidade\":\"\"}]}");
        if (r.statusCode() != 200 || !contem(r.body(), "\"ok\":true")) {
            throw new IllegalStateException("Falha ao iniciar partida padrão: " + r.body());
        }
    }

    private static boolean contem(String texto, String trecho) {
        return texto != null && texto.contains(trecho);
    }

    private static int extrairInt(String json, String campo) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(campo) + "\":(-?\\d+)").matcher(json);
        if (!m.find()) {
            throw new IllegalStateException("Campo inteiro '" + campo + "' não encontrado em: " + json);
        }
        return Integer.parseInt(m.group(1));
    }

    // ---------------------------------------------------------------
    private static void testarArquivosEstaticos() throws Exception {
        System.out.println();
        System.out.println("-- Arquivos Estaticos --");

        HttpResponse<String> home = get("/");
        verificar("GET / devolve HTTP 200 com HTML",
                home.statusCode() == 200 && contem(home.body(), "Bolonha e Milano"));

        HttpResponse<String> css = get("/styles.css");
        verificar("GET /styles.css devolve HTTP 200",
                css.statusCode() == 200 && contem(css.body(), ".kpi-grid"));

        HttpResponse<String> js = get("/app.js");
        verificar("GET /app.js devolve HTTP 200",
                js.statusCode() == 200 && contem(js.body(), "/api/estado"));

        HttpResponse<String> inexistente = get("/nao-existe.txt");
        verificar("GET /nao-existe.txt devolve HTTP 404", inexistente.statusCode() == 404);

        HttpResponse<String> traversal = get("/../README.md");
        verificar("Path traversal /../README.md e bloqueado com 404", traversal.statusCode() == 404);
    }

    // ---------------------------------------------------------------
    private static void testarProtocoloEValidacoes() throws Exception {
        System.out.println();
        System.out.println("-- Protocolo e Validacoes --");

        HttpResponse<String> estadoInicial = get("/api/estado");
        verificar("GET /api/estado inicial devolve emAndamento:false",
                estadoInicial.statusCode() == 200 && contem(estadoInicial.body(), "\"emAndamento\":false"));

        HttpResponse<String> semJogo = post("/api/mercado/comprar-graos", "{\"quantidade\":10}");
        verificar("Acao antes de iniciar partida devolve HTTP 400", semJogo.statusCode() == 400);

        HttpResponse<String> metodoErrado = post("/api/estado", "{}");
        verificar("POST em /api/estado devolve HTTP 405", metodoErrado.statusCode() == 405);

        HttpResponse<String> getEmPost = get("/api/novo-jogo");
        verificar("GET em /api/novo-jogo devolve HTTP 405", getEmPost.statusCode() == 405);

        HttpResponse<String> jsonQuebrado = post("/api/novo-jogo", "{invalido");
        verificar("JSON malformado devolve HTTP 400", jsonQuebrado.statusCode() == 400);

        HttpResponse<String> zeroJogadores = post("/api/novo-jogo", "{\"jogadores\":[]}");
        verificar("Novo jogo com 0 jogadores devolve HTTP 400", zeroJogadores.statusCode() == 400);

        HttpResponse<String> cincoJogadores = post("/api/novo-jogo",
                "{\"jogadores\":[{\"nome\":\"1\"},{\"nome\":\"2\"},{\"nome\":\"3\"},{\"nome\":\"4\"},{\"nome\":\"5\"}]}");
        verificar("Novo jogo com 5 jogadores devolve HTTP 400", cincoJogadores.statusCode() == 400);

        iniciarPartidaPadrao2Jogadores();
        HttpResponse<String> defaults = get("/api/estado");
        verificar("Nomes vazios assumem padrao Jogador 2 e Milano",
                contem(defaults.body(), "\"jogador\":\"Jogador 2\"") && contem(defaults.body(), "\"nome\":\"Milano\""));

        HttpResponse<String> campoFaltando = post("/api/mercado/comprar-graos", "{}");
        verificar("Campo quantidade ausente devolve HTTP 400", campoFaltando.statusCode() == 400);

        HttpResponse<String> campoTexto = post("/api/mercado/comprar-graos", "{\"quantidade\":\"abc\"}");
        verificar("Campo quantidade nao numerico devolve HTTP 400", campoTexto.statusCode() == 400);
    }

    // ---------------------------------------------------------------
    private static void testarMercado() throws Exception {
        System.out.println();
        System.out.println("-- API Mercado --");
        iniciarPartidaPadrao2Jogadores();

        HttpResponse<String> comprouGraos = post("/api/mercado/comprar-graos", "{\"quantidade\":100}");
        verificar("comprar-graos 100 da certo (800 florins, 600 graos)",
                contem(comprouGraos.body(), "\"ok\":true")
                        && extrairInt(comprouGraos.body(), "florins") == 800
                        && extrairInt(comprouGraos.body(), "graos") == 600);

        HttpResponse<String> vendeuGraos = post("/api/mercado/vender-graos", "{\"quantidade\":50}");
        verificar("vender-graos 50 da certo (900 florins, 550 graos)",
                contem(vendeuGraos.body(), "\"ok\":true")
                        && extrairInt(vendeuGraos.body(), "florins") == 900
                        && extrairInt(vendeuGraos.body(), "graos") == 550);

        HttpResponse<String> comprouTerras = post("/api/mercado/comprar-terras", "{\"quantidade\":200}");
        verificar("comprar-terras 200 da certo (700 florins, 10200 terras)",
                contem(comprouTerras.body(), "\"ok\":true")
                        && extrairInt(comprouTerras.body(), "florins") == 700
                        && extrairInt(comprouTerras.body(), "terras") == 10200);

        HttpResponse<String> vendeuTerras = post("/api/mercado/vender-terras", "{\"quantidade\":100}");
        verificar("vender-terras 100 da certo (800 florins, 10100 terras)",
                contem(vendeuTerras.body(), "\"ok\":true")
                        && extrairInt(vendeuTerras.body(), "florins") == 800
                        && extrairInt(vendeuTerras.body(), "terras") == 10100);

        HttpResponse<String> recusaCompra = post("/api/mercado/comprar-graos", "{\"quantidade\":99999}");
        verificar("compra acima do saldo retorna ok:false sem alterar florins",
                recusaCompra.statusCode() == 200
                        && contem(recusaCompra.body(), "\"ok\":false")
                        && extrairInt(recusaCompra.body(), "florins") == 800);

        HttpResponse<String> recusaNegativa = post("/api/mercado/vender-terras", "{\"quantidade\":-10}");
        verificar("venda com quantidade negativa retorna ok:false",
                contem(recusaNegativa.body(), "\"ok\":false"));
    }

    // ---------------------------------------------------------------
    private static void testarAgricultura() throws Exception {
        System.out.println();
        System.out.println("-- API Agricultura --");
        iniciarPartidaPadrao2Jogadores();

        HttpResponse<String> colheita1 = post("/api/agricultura/colher", "{}");
        verificar("primeira colheita deixa 2000 graos e marca colheitaFeita:true",
                contem(colheita1.body(), "\"ok\":true")
                        && extrairInt(colheita1.body(), "graos") == 2000
                        && contem(colheita1.body(), "\"colheitaFeita\":true"));

        HttpResponse<String> colheita2 = post("/api/agricultura/colher", "{}");
        verificar("segunda colheita no mesmo ano retorna ok:false",
                contem(colheita2.body(), "\"ok\":false"));

        HttpResponse<String> distInvalidaBaixa = post("/api/agricultura/distribuir", "{\"percentual\":10}");
        verificar("distribuir 10% retorna ok:false", contem(distInvalidaBaixa.body(), "\"ok\":false"));

        HttpResponse<String> distInvalidaAlta = post("/api/agricultura/distribuir", "{\"percentual\":90}");
        verificar("distribuir 90% retorna ok:false", contem(distInvalidaAlta.body(), "\"ok\":false"));

        HttpResponse<String> distValida = post("/api/agricultura/distribuir", "{\"percentual\":40}");
        verificar("distribuir 40% de 2000 graos cresce servos para 210",
                contem(distValida.body(), "\"ok\":true")
                        && extrairInt(distValida.body(), "servos") == 210
                        && extrairInt(distValida.body(), "graos") == 1200);

        // Testa deficit alimentar quando ha poucos graos
        iniciarPartidaPadrao2Jogadores();
        post("/api/mercado/vender-graos", "{\"quantidade\":450}"); // sobra 50 graos
        HttpResponse<String> distDeficit = post("/api/agricultura/distribuir", "{\"percentual\":20}"); // 10 graos < 40 necessarios
        verificar("distribuir com deficit reduz servos (200 -> 170)",
                contem(distDeficit.body(), "\"ok\":true")
                        && extrairInt(distDeficit.body(), "servos") == 170);
    }

    // ---------------------------------------------------------------
    private static void testarGoverno() throws Exception {
        System.out.println();
        System.out.println("-- API Governo --");
        iniciarPartidaPadrao2Jogadores();

        HttpResponse<String> taxaInvalida = post("/api/governo/taxa", "{\"taxa\":60}");
        verificar("taxa 60% e recusada (ok:false)", contem(taxaInvalida.body(), "\"ok\":false"));

        HttpResponse<String> taxaValida = post("/api/governo/taxa", "{\"taxa\":20}");
        verificar("taxa 20% e aceita",
                contem(taxaValida.body(), "\"ok\":true") && extrairInt(taxaValida.body(), "taxaImposto") == 20);

        HttpResponse<String> justicaInvalida = post("/api/governo/justica", "{\"nivel\":\"Branda\"}");
        verificar("justica Branda e recusada (ok:false)", contem(justicaInvalida.body(), "\"ok\":false"));

        HttpResponse<String> justicaValida = post("/api/governo/justica", "{\"nivel\":\"Severa\"}");
        verificar("justica Severa e aceita",
                contem(justicaValida.body(), "\"ok\":true") && contem(justicaValida.body(), "\"nivelJustica\":\"Severa\""));

        // 200 servos * 20% = 40 * 1.3 = 52 -> 1052 florins
        HttpResponse<String> arrecadar1 = post("/api/governo/arrecadar", "{}");
        verificar("arrecadar com 20% e Severa soma 52 florins (1052)",
                contem(arrecadar1.body(), "\"ok\":true")
                        && extrairInt(arrecadar1.body(), "florins") == 1052
                        && contem(arrecadar1.body(), "\"impostosArrecadados\":true"));

        HttpResponse<String> arrecadar2 = post("/api/governo/arrecadar", "{}");
        verificar("segunda arrecadacao no mesmo ano retorna ok:false",
                contem(arrecadar2.body(), "\"ok\":false"));
    }

    // ---------------------------------------------------------------
    private static void testarConstrucoes() throws Exception {
        System.out.println();
        System.out.println("-- API Construcoes --");
        iniciarPartidaPadrao2Jogadores();

        HttpResponse<String> construir = post("/api/construcoes/construir", "{\"quantidade\":10}");
        verificar("construir 10 obras custa 200 florins e eleva prestigio para 400",
                contem(construir.body(), "\"ok\":true")
                        && extrairInt(construir.body(), "florins") == 800
                        && extrairInt(construir.body(), "construcoes") == 10
                        && extrairInt(construir.body(), "prestigio") == 400);

        HttpResponse<String> recrutar = post("/api/construcoes/recrutar", "{\"quantidade\":10}");
        verificar("recrutar 10 soldados custa 50 florins e 10 servos (saldo prestigio +10 = 410)",
                contem(recrutar.body(), "\"ok\":true")
                        && extrairInt(recrutar.body(), "florins") == 750
                        && extrairInt(recrutar.body(), "servos") == 190
                        && extrairInt(recrutar.body(), "soldados") == 10
                        && extrairInt(recrutar.body(), "prestigio") == 410);

        HttpResponse<String> recusaRecrutar = post("/api/construcoes/recrutar", "{\"quantidade\":200}");
        verificar("recrutar acima de florins/servos e recusado sem debitar",
                contem(recusaRecrutar.body(), "\"ok\":false")
                        && extrairInt(recusaRecrutar.body(), "florins") == 750
                        && extrairInt(recusaRecrutar.body(), "servos") == 190);
    }

    // ---------------------------------------------------------------
    private static void testarTurnosEPromocaoAnual() throws Exception {
        System.out.println();
        System.out.println("-- API Turnos e Fim do Ano --");
        iniciarPartidaPadrao2Jogadores();

        // 40 obras = +400 prestigio (total 700, suficiente para Marques, mas deve subir no maximo 1 titulo: Barao)
        post("/api/construcoes/construir", "{\"quantidade\":40}");
        post("/api/agricultura/colher", "{}");
        post("/api/governo/arrecadar", "{}");

        HttpResponse<String> turno1 = post("/api/turno/encerrar", "{}");
        verificar("primeiro turno passa para Milano sem fechar ano",
                contem(turno1.body(), "\"fimDoAno\":false")
                        && contem(turno1.body(), "\"nome\":\"Milano\"")
                        && contem(turno1.body(), "\"ultimoJogadorDoAno\":true"));

        HttpResponse<String> turno2 = post("/api/turno/encerrar", "{}");
        verificar("segundo turno fecha o ano 1400, avanca para 1401 e promove Bolonha para Barao",
                contem(turno2.body(), "\"fimDoAno\":true")
                        && extrairInt(turno2.body(), "anoAtual") == 1401
                        && contem(turno2.body(), "\"novoTitulo\":\"Barão\"")
                        && contem(turno2.body(), "\"colheitaFeita\":false")
                        && contem(turno2.body(), "\"impostosArrecadados\":false"));
    }

    // ---------------------------------------------------------------
    private static void testarVitoriaPorMonarca() throws Exception {
        System.out.println();
        System.out.println("-- API Vitoria por Rei/Rainha --");
        post("/api/novo-jogo", "{\"jogadores\":[{\"nome\":\"Maria\",\"cidade\":\"Bolonha\"}]}");

        // Compra 3000 terras (ou constroi 35 obras = 700 fl -> +350 prestigio = 650) e vende terras/usa impostos
        // Mais simples: vende 7000 terras (+7000 florins -> 8000 florins, 3000 terras -> prestigio base 230)
        // e constroi 80 obras (1600 florins -> +800 prestigio -> total 1030 >= 1000!)
        post("/api/mercado/vender-terras", "{\"quantidade\":7000}");
        post("/api/construcoes/construir", "{\"quantidade\":80}");

        // 5 anos para subir: Cavaleiro -> Barao -> Conde -> Marques -> Duque -> Rei/Rainha
        HttpResponse<String> ultimo = null;
        for (int i = 0; i < 5; i++) {
            ultimo = post("/api/turno/encerrar", "{}");
        }
        verificar("apos 5 anos com prestigio >= 1000 termina com vitoria de Rei/Rainha",
                ultimo != null
                        && contem(ultimo.body(), "\"terminada\":true")
                        && contem(ultimo.body(), "\"titulo\":\"Rei/Rainha\""));

        HttpResponse<String> aposFim = post("/api/construcoes/construir", "{\"quantidade\":1}");
        verificar("acao apos fim de jogo e bloqueada com HTTP 400", aposFim.statusCode() == 400);
    }

    // ---------------------------------------------------------------
    private static void testarVitoriaPorAno1410() throws Exception {
        System.out.println();
        System.out.println("-- API Vitoria no Fim de 1410 --");
        post("/api/novo-jogo", "{\"jogadores\":[{\"nome\":\"Carlos\",\"cidade\":\"Veneza\"}]}");

        HttpResponse<String> ultimo = null;
        for (int ano = 1400; ano <= 1410; ano++) {
            ultimo = post("/api/turno/encerrar", "{}");
        }
        verificar("apos encerrar 1410 a partida termina e exibe anoAtual:1410",
                ultimo != null
                        && contem(ultimo.body(), "\"terminada\":true")
                        && extrairInt(ultimo.body(), "anoAtual") == 1410
                        && contem(ultimo.body(), "\"vencedor\":{\"indice\":0"));
    }
}
