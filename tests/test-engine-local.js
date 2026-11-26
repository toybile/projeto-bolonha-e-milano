"use strict";

const assert = require("assert");
const BolonhaLocalEngine = require("../web/engine-local.js");

let aprovados = 0;
let reprovados = 0;

function verificar(descricao, condicao) {
  if (condicao) {
    aprovados++;
    console.log(`[OK] ${descricao}`);
  } else {
    reprovados++;
    console.error(`[FALHA] ${descricao}`);
  }
}

function createMemoryStorage() {
  const map = new Map();
  return {
    getItem(k) {
      return map.has(k) ? map.get(k) : null;
    },
    setItem(k, v) {
      map.set(k, String(v));
    }
  };
}

function expectHttpError(fn, statusEsperado, descricao) {
  try {
    fn();
    verificar(descricao, false);
  } catch (err) {
    verificar(descricao, err && err.status === statusEsperado);
  }
}

console.log("===== TESTES DO MOTOR ESTATICO JS - BOLONHA E MILANO =====");

const storage = createMemoryStorage();
let engine = BolonhaLocalEngine.createEngine({ storage, storageKey: "teste_bolonha" });

// Estado inicial e validações
const inicial = engine.handleRequest("/api/estado", "GET");
verificar("estado inicial tem emAndamento:false", inicial.emAndamento === false && inicial.possuiPartida === false);
expectHttpError(() => engine.handleRequest("/api/mercado/comprar-graos", "POST", { quantidade: 10 }), 400, "acao antes de iniciar partida lanca erro 400");
expectHttpError(() => engine.handleRequest("/api/estado", "POST", {}), 405, "POST em /api/estado lanca erro 405");
expectHttpError(() => engine.handleRequest("/api/novo-jogo", "POST", { jogadores: [] }), 400, "novo-jogo com 0 jogadores lanca erro 400");
expectHttpError(() => engine.handleRequest("/api/novo-jogo", "POST", { jogadores: [{}, {}, {}, {}, {}] }), 400, "novo-jogo com 5 jogadores lanca erro 400");

// Novo jogo com padrões
const start = engine.handleRequest("/api/novo-jogo", "POST", {
  jogadores: [{ nome: "Ana", cidade: "Bolonha" }, { nome: "", cidade: "" }]
});
verificar("novo-jogo cria 2 cidades com nomes padrao",
  start.ok === true &&
  start.estado.cidades.length === 2 &&
  start.estado.cidades[0].nome === "Bolonha" &&
  start.estado.cidades[1].nome === "Milano" &&
  start.estado.cidades[1].jogador === "Jogador 2");

// Mercado
const cGraos = engine.handleRequest("/api/mercado/comprar-graos", "POST", { quantidade: 100 });
verificar("comprar 100 graos custa 200 florins (800 fl, 600 gr)",
  cGraos.ok && cGraos.estado.cidadeAtual.florins === 800 && cGraos.estado.cidadeAtual.graos === 600);

const vGraos = engine.handleRequest("/api/mercado/vender-graos", "POST", { quantidade: 50 });
verificar("vender 50 graos rende 100 florins (900 fl, 550 gr)",
  vGraos.ok && vGraos.estado.cidadeAtual.florins === 900 && vGraos.estado.cidadeAtual.graos === 550);

const recGraos = engine.handleRequest("/api/mercado/comprar-graos", "POST", { quantidade: 99999 });
verificar("compra de graos acima do saldo retorna ok:false sem debitar",
  recGraos.ok === false && recGraos.estado.cidadeAtual.florins === 900);

const cTerras = engine.handleRequest("/api/mercado/comprar-terras", "POST", { quantidade: 200 });
const vTerras = engine.handleRequest("/api/mercado/vender-terras", "POST", { quantidade: 100 });
verificar("comprar e vender terras atualizam terras e florins",
  cTerras.ok && vTerras.ok && vTerras.estado.cidadeAtual.terras === 10100 && vTerras.estado.cidadeAtual.florins === 800);

// Agricultura (colheita + crescimento + deficit)
engine.handleRequest("/api/novo-jogo", "POST", { jogadores: [{ nome: "Ana", cidade: "Bolonha" }] });
const col1 = engine.handleRequest("/api/agricultura/colher", "POST", {});
const col2 = engine.handleRequest("/api/agricultura/colher", "POST", {});
verificar("primeira colheita deixa 2000 graos e segunda e recusada",
  col1.ok && col1.estado.cidadeAtual.graos === 2000 && col2.ok === false);

const distBad = engine.handleRequest("/api/agricultura/distribuir", "POST", { percentual: 10 });
const distOk = engine.handleRequest("/api/agricultura/distribuir", "POST", { percentual: 40 });
verificar("distribuir 10% falha e 40% aumenta servos para 210",
  distBad.ok === false && distOk.ok && distOk.estado.cidadeAtual.servos === 210 && distOk.estado.cidadeAtual.graos === 1200);

// Deficit alimentar
engine.handleRequest("/api/novo-jogo", "POST", { jogadores: [{ nome: "Ana", cidade: "Bolonha" }] });
engine.handleRequest("/api/mercado/vender-graos", "POST", { quantidade: 450 });
const distDeficit = engine.handleRequest("/api/agricultura/distribuir", "POST", { percentual: 20 });
verificar("distribuir com deficit reduz servos (200 -> 170)",
  distDeficit.ok && distDeficit.estado.cidadeAtual.servos === 170);

// Governo
engine.handleRequest("/api/novo-jogo", "POST", { jogadores: [{ nome: "Ana", cidade: "Bolonha" }] });
const tBad = engine.handleRequest("/api/governo/taxa", "POST", { taxa: 60 });
const tOk = engine.handleRequest("/api/governo/taxa", "POST", { taxa: 20 });
const jBad = engine.handleRequest("/api/governo/justica", "POST", { nivel: "Branda" });
const jOk = engine.handleRequest("/api/governo/justica", "POST", { nivel: "Severa" });
const imp1 = engine.handleRequest("/api/governo/arrecadar", "POST", {});
const imp2 = engine.handleRequest("/api/governo/arrecadar", "POST", {});
verificar("governo valida taxa/justica e arrecada 52 florins uma vez no ano",
  tBad.ok === false && tOk.ok && jBad.ok === false && jOk.ok &&
  imp1.ok && imp1.estado.cidadeAtual.florins === 1052 && imp2.ok === false);

// Construções + persistência em storage
engine.handleRequest("/api/novo-jogo", "POST", {
  jogadores: [{ nome: "Ana", cidade: "Bolonha" }, { nome: "Beto", cidade: "Milano" }]
});
const obras = engine.handleRequest("/api/construcoes/construir", "POST", { quantidade: 40 });
const soldOk = engine.handleRequest("/api/construcoes/recrutar", "POST", { quantidade: 10 });
const soldBad = engine.handleRequest("/api/construcoes/recrutar", "POST", { quantidade: 500 });
verificar("construir 40 obras, recrutar 10 soldados e recusar recrutamento atomico",
  obras.ok && soldOk.ok && soldBad.ok === false &&
  soldBad.estado.cidadeAtual.florins === 150 &&
  soldBad.estado.cidadeAtual.servos === 190 &&
  soldBad.estado.cidadeAtual.soldados === 10);

// Recriando engine a partir do mesmo storage (simulando F5 / reload)
const reloaded = BolonhaLocalEngine.createEngine({ storage, storageKey: "teste_bolonha" });
const reloadState = reloaded.handleRequest("/api/estado", "GET");
verificar("estado persiste apos reload em storage",
  reloadState.possuiPartida && reloadState.cidadeAtual.construcoes === 40 && reloadState.cidadeAtual.soldados === 10);

// Turnos e limite de 1 promoção por ano
const turno1 = reloaded.handleRequest("/api/turno/encerrar", "POST", {});
const turno2 = reloaded.handleRequest("/api/turno/encerrar", "POST", {});
verificar("fim de ano promove no maximo 1 titulo (Cavaleiro -> Barao) mesmo com prestigio >= 700",
  turno1.fimDoAno === false &&
  turno2.fimDoAno === true &&
  turno2.estado.anoAtual === 1401 &&
  turno2.estado.cidades[0].titulo === "Barão" &&
  turno2.estado.cidades[0].nivelTitulo === 1);

// Vitória por Rei/Rainha
reloaded.handleRequest("/api/novo-jogo", "POST", { jogadores: [{ nome: "Maria", cidade: "Bolonha" }] });
reloaded.handleRequest("/api/mercado/vender-terras", "POST", { quantidade: 7000 });
reloaded.handleRequest("/api/construcoes/construir", "POST", { quantidade: 80 });
let resMonarca = null;
for (let i = 0; i < 5; i++) {
  resMonarca = reloaded.handleRequest("/api/turno/encerrar", "POST", {});
}
verificar("vitoria por Rei/Rainha encerra partida apos 5 anos",
  resMonarca.terminada === true && resMonarca.estado.vencedor.titulo === "Rei/Rainha");
expectHttpError(() => reloaded.handleRequest("/api/construcoes/construir", "POST", { quantidade: 1 }), 400, "acao apos vitoria lanca erro 400");

// Vitória ao fim de 1410
reloaded.handleRequest("/api/novo-jogo", "POST", { jogadores: [{ nome: "Carlos", cidade: "Veneza" }] });
let res1410 = null;
for (let ano = 1400; ano <= 1410; ano++) {
  res1410 = reloaded.handleRequest("/api/turno/encerrar", "POST", {});
}
verificar("vitoria no fim de 1410 encerra partida com anoAtual:1410",
  res1410.terminada === true && res1410.estado.anoAtual === 1410 && res1410.estado.vencedor.nome === "Veneza");

assert.strictEqual(reprovados, 0);
console.log(`\nAprovados: ${aprovados} | Reprovados: ${reprovados}`);
