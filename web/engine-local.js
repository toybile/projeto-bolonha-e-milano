(function (root, factory) {
  if (typeof module === "object" && module.exports) {
    module.exports = factory();
  } else {
    root.BolonhaLocalEngine = factory();
  }
})(typeof globalThis !== "undefined" ? globalThis : window, function () {
  "use strict";

  const STORAGE_KEY_PADRAO = "bolonha_milano_estado_v1";
  const MAXIMO_JOGADORES = 4;
  const ANO_INICIAL = 1400;
  const ANO_FINAL = 1410;
  const MAX_HISTORICO = 60;

  const CIDADES_PADRAO = ["Bolonha", "Milano", "Florença", "Veneza"];
  const TITULOS_ORIGINAIS = [
    "Cavaleiro", "Barao", "Conde", "Marques", "Duque", "Rei/Rainha"
  ];
  const TITULOS_EXIBICAO = [
    "Cavaleiro", "Barão", "Conde", "Marquês", "Duque", "Rei/Rainha"
  ];
  const PRESTIGIO_MINIMO = [0, 400, 550, 700, 850, 1000];

  const PRECO_GRAO = 2;
  const PRECO_TERRA = 1;
  const PRECO_SOLDADO = 5;
  const SERVOS_POR_SOLDADO = 1;
  const PRECO_CONSTRUCAO = 20;

  function criarErroHttp(status, mensagem) {
    const err = new Error(mensagem);
    err.status = status;
    return err;
  }

  function clonar(obj) {
    return JSON.parse(JSON.stringify(obj));
  }

  function tituloBonito(nivel) {
    return TITULOS_EXIBICAO[nivel] || TITULOS_ORIGINAIS[nivel] || "Cavaleiro";
  }

  function calcularPrestigio(c) {
    return (
      c.construcoes * 10 +
      c.soldados * 2 +
      c.servos +
      Math.floor(c.terras / 100)
    );
  }

  function criarCidade(nomeCidade, nomeJogador) {
    return {
      nome: nomeCidade,
      anoAtual: ANO_INICIAL,
      jogador: nomeJogador,
      nivelTitulo: 0,
      florins: 1000,
      terras: 10000,
      graos: 500,
      servos: 200,
      taxaImposto: 10,
      nivelJustica: "Moderada",
      soldados: 0,
      construcoes: 0,
      colheitaFeita: false,
      impostosArrecadados: false
    };
  }

  function determinarVencedor(cidades) {
    let vencedora = cidades[0];
    for (const c of cidades) {
      if (c.nivelTitulo > vencedora.nivelTitulo) {
        vencedora = c;
      } else if (
        c.nivelTitulo === vencedora.nivelTitulo &&
        calcularPrestigio(c) > calcularPrestigio(vencedora)
      ) {
        vencedora = c;
      }
    }
    return vencedora;
  }

  function exigirInteiro(body, campo) {
    if (!body || typeof body !== "object" || body[campo] === undefined || body[campo] === null) {
      throw criarErroHttp(400, "Campo '" + campo + "' é obrigatório.");
    }
    const raw = body[campo];
    if (typeof raw === "number") {
      if (!Number.isFinite(raw) || !Number.isInteger(raw) || raw < -2147483648 || raw > 2147483647) {
        throw criarErroHttp(400, "Campo '" + campo + "' deve ser um número inteiro.");
      }
      return raw;
    }
    if (typeof raw === "string") {
      const trimmed = raw.trim();
      if (!/^-?\d+$/.test(trimmed)) {
        throw criarErroHttp(400, "Campo '" + campo + "' deve ser um número inteiro.");
      }
      const n = Number(trimmed);
      if (n < -2147483648 || n > 2147483647) {
        throw criarErroHttp(400, "Campo '" + campo + "' deve ser um número inteiro.");
      }
      return n;
    }
    throw criarErroHttp(400, "Campo '" + campo + "' deve ser um número inteiro.");
  }

  function exigirTexto(body, campo) {
    if (!body || typeof body !== "object" || body[campo] === undefined || body[campo] === null) {
      throw criarErroHttp(400, "Campo '" + campo + "' é obrigatório.");
    }
    if (typeof body[campo] !== "string") {
      throw criarErroHttp(400, "Campo '" + campo + "' deve ser texto.");
    }
    return body[campo].trim();
  }

  function createEngine(options) {
    const opts = options || {};
    const storageKey = opts.storageKey === undefined ? STORAGE_KEY_PADRAO : opts.storageKey;
    const storage = opts.storage !== undefined
      ? opts.storage
      : (typeof localStorage !== "undefined" ? localStorage : null);

    let data = carregarEstado() || estadoVazio();

    function estadoVazio() {
      return {
        cidades: null,
        indiceAtual: 0,
        terminada: false,
        proximoIdEvento: 1,
        historico: [],
        ultimoResumoAno: null
      };
    }

    function carregarEstado() {
      if (!storage || !storageKey) return null;
      try {
        const raw = storage.getItem(storageKey);
        if (!raw) return null;
        const parsed = JSON.parse(raw);
        if (!parsed || typeof parsed !== "object") return null;
        return {
          cidades: Array.isArray(parsed.cidades) ? parsed.cidades : null,
          indiceAtual: Number.isInteger(parsed.indiceAtual) ? parsed.indiceAtual : 0,
          terminada: Boolean(parsed.terminada),
          proximoIdEvento: Number(parsed.proximoIdEvento) || 1,
          historico: Array.isArray(parsed.historico) ? parsed.historico : [],
          ultimoResumoAno: parsed.ultimoResumoAno || null
        };
      } catch (e) {
        return null;
      }
    }

    function salvarEstado() {
      if (!storage || !storageKey) return;
      try {
        storage.setItem(storageKey, JSON.stringify(data));
      } catch (e) {
        // Ignora indisponibilidade de storage (ex.: modo privado restrito)
      }
    }

    function registrarEvento(tipo, mensagem) {
      const ano = data.cidades && data.cidades.length > 0
        ? Math.min(data.cidades[0].anoAtual, ANO_FINAL)
        : ANO_INICIAL;
      data.historico.unshift({
        id: data.proximoIdEvento++,
        ano,
        tipo,
        mensagem
      });
      if (data.historico.length > MAX_HISTORICO) {
        data.historico.length = MAX_HISTORICO;
      }
    }

    function exigirJogoEmAndamento() {
      if (!data.cidades || data.cidades.length === 0) {
        throw criarErroHttp(400, "Nenhum jogo em andamento. Inicie uma nova partida.");
      }
      if (data.terminada) {
        throw criarErroHttp(400, "A partida já terminou. Inicie uma nova partida para continuar jogando.");
      }
      return data.cidades[data.indiceAtual];
    }

    function cidadeParaJson(c, indice, ehAtual) {
      const prestigio = calcularPrestigio(c);
      const ehMonarca = c.nivelTitulo >= TITULOS_ORIGINAIS.length - 1;
      const proximoPrestigio = ehMonarca ? -1 : PRESTIGIO_MINIMO[c.nivelTitulo + 1];
      const proximoTitulo = ehMonarca ? null : tituloBonito(c.nivelTitulo + 1);

      return {
        indice,
        ehAtual,
        nome: c.nome,
        jogador: c.jogador,
        titulo: tituloBonito(c.nivelTitulo),
        tituloOriginal: TITULOS_ORIGINAIS[c.nivelTitulo] || "Cavaleiro",
        nivelTitulo: c.nivelTitulo,
        ehMonarca,
        anoAtual: c.anoAtual,
        florins: c.florins,
        terras: c.terras,
        graos: c.graos,
        servos: c.servos,
        taxaImposto: c.taxaImposto,
        nivelJustica: c.nivelJustica,
        soldados: c.soldados,
        construcoes: c.construcoes,
        colheitaFeita: c.colheitaFeita,
        impostosArrecadados: c.impostosArrecadados,
        prestigio,
        proximoTituloPrestigio: proximoPrestigio,
        proximoTituloNome: proximoTitulo,
        decomposicaoPrestigio: {
          construcoes: c.construcoes * 10,
          soldados: c.soldados * 2,
          servos: c.servos,
          terras: Math.floor(c.terras / 100)
        }
      };
    }

    function construirEstado() {
      const possuiPartida = Array.isArray(data.cidades) && data.cidades.length > 0;
      const emAndamento = possuiPartida && !data.terminada;

      if (!possuiPartida) {
        return {
          emAndamento: false,
          possuiPartida: false,
          terminada: false,
          anoAtual: ANO_INICIAL,
          anoFinal: ANO_FINAL,
          indiceAtual: 0,
          ultimoJogadorDoAno: false,
          cidadeAtual: null,
          cidades: [],
          vencedor: null,
          ultimoResumoAno: clonar(data.ultimoResumoAno),
          historico: clonar(data.historico),
          precos: {
            grao: PRECO_GRAO,
            terra: PRECO_TERRA,
            soldado: PRECO_SOLDADO,
            servosPorSoldado: SERVOS_POR_SOLDADO,
            construcao: PRECO_CONSTRUCAO
          }
        };
      }

      const atual = data.cidades[data.indiceAtual];
      const anoExibido = data.terminada
        ? Math.min(data.cidades[0].anoAtual, ANO_FINAL)
        : data.cidades[0].anoAtual;
      const ultimoDoAno = data.indiceAtual === data.cidades.length - 1;
      const cidadesJson = data.cidades.map((c, idx) => cidadeParaJson(c, idx, idx === data.indiceAtual));
      let vencedorJson = null;
      if (data.terminada) {
        const venc = determinarVencedor(data.cidades);
        const idxVenc = data.cidades.indexOf(venc);
        vencedorJson = cidadeParaJson(venc, idxVenc, false);
      }

      return {
        emAndamento,
        possuiPartida: true,
        terminada: data.terminada,
        anoAtual: anoExibido,
        anoFinal: ANO_FINAL,
        indiceAtual: data.indiceAtual,
        ultimoJogadorDoAno: ultimoDoAno,
        cidadeAtual: cidadeParaJson(atual, data.indiceAtual, true),
        cidades: cidadesJson,
        vencedor: vencedorJson,
        ultimoResumoAno: clonar(data.ultimoResumoAno),
        historico: clonar(data.historico),
        precos: {
          grao: PRECO_GRAO,
          terra: PRECO_TERRA,
          soldado: PRECO_SOLDADO,
          servosPorSoldado: SERVOS_POR_SOLDADO,
          construcao: PRECO_CONSTRUCAO
        }
      };
    }

    function respostaAcao(ok, mensagem, extras) {
      return Object.assign(
        { ok, mensagem },
        extras || {},
        { estado: construirEstado() }
      );
    }

    function normalizarRota(url) {
      let clean = String(url || "").split("?")[0];
      if (!clean.startsWith("/")) {
        clean = "/" + clean;
      }
      return clean;
    }

    function handleRequest(url, method, body) {
      const rota = normalizarRota(url);
      const metodo = String(method || "GET").toUpperCase();

      if (rota === "/api/estado") {
        if (metodo !== "GET") throw criarErroHttp(405, "Método não permitido. Use GET.");
        return construirEstado();
      }

      if (metodo !== "POST") {
        throw criarErroHttp(405, "Método não permitido. Use POST.");
      }

      if (rota === "/api/novo-jogo") {
        const lista = body && body.jogadores;
        if (!Array.isArray(lista) || lista.length === 0 || lista.length > MAXIMO_JOGADORES) {
          throw criarErroHttp(400, "Informe de 1 a 4 jogadores em 'jogadores'.");
        }
        const novasCidades = lista.map((item, i) => {
          if (!item || typeof item !== "object") {
            throw criarErroHttp(400, "Cada jogador deve ser um objeto JSON com nome e cidade.");
          }
          const nomeJogador = String(item.nome ?? "").trim() || ("Jogador " + (i + 1));
          const nomeCidade = String(item.cidade ?? "").trim() || CIDADES_PADRAO[i];
          return criarCidade(nomeCidade, nomeJogador);
        });

        data = estadoVazio();
        data.cidades = novasCidades;
        const inicial = data.cidades[0];
        const msg = "Nova partida iniciada no ano " + inicial.anoAtual
          + ". Começa " + inicial.jogador + " (" + inicial.nome + ").";
        registrarEvento("destaque", msg);
        salvarEstado();
        return respostaAcao(true, msg);
      }

      if (rota === "/api/mercado/comprar-graos") {
        const qtd = exigirInteiro(body, "quantidade");
        const atual = exigirJogoEmAndamento();
        const custo = qtd * PRECO_GRAO;
        const ok = qtd > 0 && atual.florins >= custo;
        if (ok) {
          atual.florins -= custo;
          atual.graos += qtd;
        }
        const msg = ok
          ? atual.nome + " comprou " + qtd + " grãos por " + custo + " florins."
          : "Compra de grãos recusada (quantidade inválida ou florins insuficientes).";
        registrarEvento(ok ? "sucesso" : "erro", msg);
        salvarEstado();
        return respostaAcao(ok, msg);
      }

      if (rota === "/api/mercado/vender-graos") {
        const qtd = exigirInteiro(body, "quantidade");
        const atual = exigirJogoEmAndamento();
        const ok = qtd > 0 && atual.graos >= qtd;
        if (ok) {
          atual.graos -= qtd;
          atual.florins += qtd * PRECO_GRAO;
        }
        const msg = ok
          ? atual.nome + " vendeu " + qtd + " grãos por " + (qtd * PRECO_GRAO) + " florins."
          : "Venda de grãos recusada (quantidade inválida ou grãos insuficientes).";
        registrarEvento(ok ? "sucesso" : "erro", msg);
        salvarEstado();
        return respostaAcao(ok, msg);
      }

      if (rota === "/api/mercado/comprar-terras") {
        const qtd = exigirInteiro(body, "quantidade");
        const atual = exigirJogoEmAndamento();
        const custo = qtd * PRECO_TERRA;
        const ok = qtd > 0 && atual.florins >= custo;
        if (ok) {
          atual.florins -= custo;
          atual.terras += qtd;
        }
        const msg = ok
          ? atual.nome + " adquiriu " + qtd + " ha de terras por " + qtd + " florins."
          : "Compra de terras recusada (quantidade inválida ou florins insuficientes).";
        registrarEvento(ok ? "sucesso" : "erro", msg);
        salvarEstado();
        return respostaAcao(ok, msg);
      }

      if (rota === "/api/mercado/vender-terras") {
        const qtd = exigirInteiro(body, "quantidade");
        const atual = exigirJogoEmAndamento();
        const ok = qtd > 0 && atual.terras >= qtd;
        if (ok) {
          atual.terras -= qtd;
          atual.florins += qtd * PRECO_TERRA;
        }
        const msg = ok
          ? atual.nome + " vendeu " + qtd + " ha de terras por " + qtd + " florins."
          : "Venda de terras recusada (quantidade inválida ou terras insuficientes).";
        registrarEvento(ok ? "sucesso" : "erro", msg);
        salvarEstado();
        return respostaAcao(ok, msg);
      }

      if (rota === "/api/agricultura/colher") {
        const atual = exigirJogoEmAndamento();
        if (atual.colheitaFeita) {
          const msg = "A colheita deste ano já foi realizada em " + atual.nome + ".";
          registrarEvento("erro", msg);
          salvarEstado();
          return respostaAcao(false, msg);
        }
        const graosAntes = atual.graos;
        let hectares = Math.min(atual.terras, atual.servos * 5);
        let sementes = Math.floor(hectares / 2);
        if (sementes > atual.graos) {
          sementes = atual.graos;
          hectares = sementes * 2;
        }
        atual.graos -= sementes;
        atual.graos += hectares * 2;
        atual.colheitaFeita = true;
        const saldoGraos = atual.graos - graosAntes;
        const msg = atual.nome + " colheu " + hectares + " hectares (estoque de grãos: "
          + atual.graos + ", saldo +" + saldoGraos + ").";
        registrarEvento("sucesso", msg);
        salvarEstado();
        return respostaAcao(true, msg);
      }

      if (rota === "/api/agricultura/distribuir") {
        const percentual = exigirInteiro(body, "percentual");
        const atual = exigirJogoEmAndamento();
        const servosAntes = atual.servos;
        const graosAntes = atual.graos;
        let ok = false;
        if (percentual >= 20 && percentual <= 80) {
          ok = true;
          const alimento = Math.floor((atual.graos * percentual) / 100);
          atual.graos -= alimento;
          const necessidade = Math.floor(atual.servos / 5);
          if (alimento >= necessidade) {
            const crescimento = Math.floor((atual.servos * 5) / 100);
            atual.servos += crescimento;
          } else {
            const deficit = necessidade - alimento;
            atual.servos -= Math.min(deficit, atual.servos);
          }
        }
        const deltaServos = atual.servos - servosAntes;
        const graosGastos = graosAntes - atual.graos;
        let msg;
        if (ok) {
          const variacao = deltaServos >= 0 ? "+" + deltaServos : String(deltaServos);
          msg = atual.nome + " distribuiu " + percentual + "% dos grãos (" + graosGastos
            + " grãos). Servos: " + servosAntes + " → " + atual.servos + " (" + variacao + ").";
        } else {
          msg = "Distribuição recusada: escolha um percentual entre 20% e 80%.";
        }
        registrarEvento(ok ? "sucesso" : "erro", msg);
        salvarEstado();
        return respostaAcao(ok, msg, {
          servosAntes,
          servosDepois: atual.servos,
          deltaServos,
          graosGastos
        });
      }

      if (rota === "/api/governo/arrecadar") {
        const atual = exigirJogoEmAndamento();
        if (atual.impostosArrecadados) {
          const msg = "Os impostos deste ano já foram arrecadados em " + atual.nome + ".";
          registrarEvento("erro", msg);
          salvarEstado();
          return respostaAcao(false, msg);
        }
        const base = Math.floor((atual.servos * atual.taxaImposto) / 100);
        const mult = atual.nivelJustica === "Justa" ? 0.8 : (atual.nivelJustica === "Severa" ? 1.3 : 1.0);
        const arrecadado = Math.floor(base * mult);
        atual.florins += arrecadado;
        atual.impostosArrecadados = true;
        const msg = atual.nome + " arrecadou " + arrecadado + " florins em impostos.";
        registrarEvento("sucesso", msg);
        salvarEstado();
        return respostaAcao(true, msg);
      }

      if (rota === "/api/governo/taxa") {
        const taxa = exigirInteiro(body, "taxa");
        const atual = exigirJogoEmAndamento();
        const ok = taxa >= 0 && taxa <= 50;
        if (ok) {
          atual.taxaImposto = taxa;
        }
        const msg = ok
          ? "Taxa de imposto de " + atual.nome + " ajustada para " + taxa + "%."
          : "Taxa de imposto recusada: informe um valor entre 0% e 50%.";
        registrarEvento(ok ? "sucesso" : "erro", msg);
        salvarEstado();
        return respostaAcao(ok, msg);
      }

      if (rota === "/api/governo/justica") {
        const nivel = exigirTexto(body, "nivel");
        const atual = exigirJogoEmAndamento();
        const ok = nivel === "Justa" || nivel === "Moderada" || nivel === "Severa";
        if (ok) {
          atual.nivelJustica = nivel;
        }
        const msg = ok
          ? "Nível de justiça de " + atual.nome + " definido como " + nivel + "."
          : "Nível de justiça inválido. Use Justa, Moderada ou Severa.";
        registrarEvento(ok ? "sucesso" : "erro", msg);
        salvarEstado();
        return respostaAcao(ok, msg);
      }

      if (rota === "/api/construcoes/recrutar") {
        const qtd = exigirInteiro(body, "quantidade");
        const atual = exigirJogoEmAndamento();
        const custo = qtd * PRECO_SOLDADO;
        const servosNecessarios = qtd * SERVOS_POR_SOLDADO;
        const ok = qtd > 0 && atual.florins >= custo && atual.servos >= servosNecessarios;
        if (ok) {
          atual.florins -= custo;
          atual.servos -= servosNecessarios;
          atual.soldados += qtd;
        }
        const msg = ok
          ? atual.nome + " recrutou " + qtd + " soldados (" + custo + " florins e " + qtd + " servos)."
          : "Recrutamento recusado (quantidade inválida, florins ou servos insuficientes).";
        registrarEvento(ok ? "sucesso" : "erro", msg);
        salvarEstado();
        return respostaAcao(ok, msg);
      }

      if (rota === "/api/construcoes/construir") {
        const qtd = exigirInteiro(body, "quantidade");
        const atual = exigirJogoEmAndamento();
        const custo = qtd * PRECO_CONSTRUCAO;
        const ok = qtd > 0 && atual.florins >= custo;
        if (ok) {
          atual.florins -= custo;
          atual.construcoes += qtd;
        }
        const msg = ok
          ? atual.nome + " ergueu " + qtd + " construção(ões) por " + custo + " florins."
          : "Obra recusada (quantidade inválida ou florins insuficientes).";
        registrarEvento(ok ? "sucesso" : "erro", msg);
        salvarEstado();
        return respostaAcao(ok, msg);
      }

      if (rota === "/api/turno/encerrar") {
        const encerrando = exigirJogoEmAndamento();
        const anoQueTerminou = encerrando.anoAtual;
        registrarEvento("info", "Turno de " + encerrando.jogador + " (" + encerrando.nome + ") encerrado.");

        data.indiceAtual += 1;
        let fimDoAno = false;
        if (data.indiceAtual >= data.cidades.length) {
          data.indiceAtual = 0;
          fimDoAno = true;
        }

        let msg;
        if (fimDoAno) {
          const promocoesInfo = [];
          for (const c of data.cidades) {
            const tituloAntes = tituloBonito(c.nivelTitulo);
            c.anoAtual += 1;
            c.colheitaFeita = false;
            c.impostosArrecadados = false;
            if (c.nivelTitulo < TITULOS_ORIGINAIS.length - 1) {
              const prestigio = calcularPrestigio(c);
              const necessario = PRESTIGIO_MINIMO[c.nivelTitulo + 1];
              if (prestigio >= necessario) {
                c.nivelTitulo += 1;
                promocoesInfo.push({
                  cidade: c.nome,
                  jogador: c.jogador,
                  tituloAnterior: tituloAntes,
                  novoTitulo: tituloBonito(c.nivelTitulo),
                  prestigio
                });
              }
            }
          }

          data.ultimoResumoAno = {
            anoEncerrado: anoQueTerminou,
            promocoes: promocoesInfo
          };

          registrarEvento("destaque", "Fim do ano " + anoQueTerminou + ".");
          for (const p of promocoesInfo) {
            registrarEvento("destaque", p.jogador + " (" + p.cidade + ") subiu de "
              + p.tituloAnterior + " para " + p.novoTitulo + "!");
          }

          const alguemMonarca = data.cidades.some((c) => c.nivelTitulo >= TITULOS_ORIGINAIS.length - 1);
          const passouAnoFinal = data.cidades[0].anoAtual > ANO_FINAL;
          if (alguemMonarca || passouAnoFinal) {
            data.terminada = true;
            const venc = determinarVencedor(data.cidades);
            msg = "Fim de jogo! " + venc.jogador + " (" + tituloBonito(venc.nivelTitulo)
              + "), de " + venc.nome + ", venceu a partida!";
            registrarEvento("destaque", msg);
          } else {
            const proxima = data.cidades[data.indiceAtual];
            msg = "Ano " + anoQueTerminou + " encerrado! Começa " + proxima.anoAtual
              + " — vez de " + proxima.jogador + " (" + proxima.nome + ").";
          }
        } else {
          const proxima = data.cidades[data.indiceAtual];
          msg = "Agora é a vez de " + proxima.jogador + " (" + proxima.nome + ").";
        }

        salvarEstado();
        return respostaAcao(true, msg, { fimDoAno, terminada: data.terminada });
      }

      throw criarErroHttp(404, "Rota de API desconhecida: " + rota);
    }

    return {
      handleRequest,
      getState: construirEstado
    };
  }

  return {
    createEngine,
    calcularPrestigio,
    TITULOS_EXIBICAO,
    PRESTIGIO_MINIMO
  };
});
