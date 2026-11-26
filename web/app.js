(() => {
  "use strict";

  const CIDADES_PADRAO = ["Bolonha", "Milano", "Florença", "Veneza"];
  const CORES_HERALDICAS = [
    { nome: "Bolonha", primario: "#8f2424", secundario: "#d4a843", fundo: "#f6e3dc" },
    { nome: "Milano", primario: "#1f4d7a", secundario: "#d4a843", fundo: "#e0ebf5" },
    { nome: "Florença", primario: "#9e6b08", secundario: "#f4ecdc", fundo: "#f7ecd5" },
    { nome: "Veneza", primario: "#2e5c3a", secundario: "#d4a843", fundo: "#dfeee3" }
  ];

  const TITULOS = [
    { nome: "Cavaleiro", prestigio: 0 },
    { nome: "Barão", prestigio: 400 },
    { nome: "Conde", prestigio: 550 },
    { nome: "Marquês", prestigio: 700 },
    { nome: "Duque", prestigio: 850 },
    { nome: "Rei/Rainha", prestigio: 1000 }
  ];

  const state = {
    mode: "java",
    localEngine: null,
    playerCount: 2,
    game: null,
    activeTab: "mercado",
    previousStats: null,
    lastRenderedCityKey: null,
    lastShownYearEnd: null,
    lastShownVictoryKey: null,
    busy: false
  };

  // ================= ELEMENTOS DOM =================

  const setupScreen = document.getElementById("setup-screen");
  const gameScreen = document.getElementById("game-screen");
  const playerCountControl = document.getElementById("player-count-control");
  const playerFieldsContainer = document.getElementById("player-fields-container");
  const newGameForm = document.getElementById("new-game-form");

  const headerCrest = document.getElementById("header-crest");
  const topbarTurnLabel = document.getElementById("topbar-turn-label");
  const yearBadge = document.getElementById("year-badge");
  const yearProgressFill = document.getElementById("year-progress-fill");
  const btnEndTurn = document.getElementById("btn-end-turn");

  const activeCityShield = document.getElementById("active-city-shield");
  const activeCityName = document.getElementById("active-city-name");
  const activePlayerTitle = document.getElementById("active-player-title");
  const activePlayerName = document.getElementById("active-player-name");
  const activeGovSummary = document.getElementById("active-government-summary");
  const chipColheita = document.getElementById("chip-colheita");
  const chipImpostos = document.getElementById("chip-impostos");
  const cityPanoramaSvg = document.getElementById("city-panorama-svg");

  const activePrestigeValue = document.getElementById("active-prestige-value");
  const activeNextTitleHint = document.getElementById("active-next-title-hint");
  const prestigeBreakdown = document.getElementById("prestige-breakdown");
  const nobleStepper = document.getElementById("noble-stepper");

  const standingsCount = document.getElementById("standings-count");
  const standingsList = document.getElementById("standings-list");
  const chronicleList = document.getElementById("chronicle-list");
  const toastContainer = document.getElementById("toast-container");

  // Inputs & controles de ações
  const inputGraosQtd = document.getElementById("input-graos-qtd");
  const inputTerrasQtd = document.getElementById("input-terras-qtd");
  const inputSoldadosQtd = document.getElementById("input-soldados-qtd");
  const inputConstrucoesQtd = document.getElementById("input-construcoes-qtd");
  const sliderAlimentos = document.getElementById("slider-alimentos");
  const valSliderAlimentos = document.getElementById("val-slider-alimentos");
  const badgePctAlimentos = document.getElementById("badge-pct-alimentos");
  const sliderTaxa = document.getElementById("slider-taxa");
  const valSliderTaxa = document.getElementById("val-slider-taxa");
  const badgeJusticaAtual = document.getElementById("badge-justica-atual");

  const modalYearEnd = document.getElementById("modal-year-end");
  const modalVictory = document.getElementById("modal-victory");
  const modalAbout = document.getElementById("modal-about");

  // ================= UTILITÁRIOS =================

  function fmt(num) {
    return Number(num || 0).toLocaleString("pt-BR");
  }

  function escapeHtml(str) {
    return String(str ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function corDaCidade(indice) {
    return CORES_HERALDICAS[indice % CORES_HERALDICAS.length];
  }

  function criarEscudoSvg(corPrimaria, corSecundaria, tamanho) {
    return `
      <svg viewBox="0 0 48 54" width="${tamanho}" height="${Math.round(tamanho * 1.12)}">
        <path d="M24 3 L44 10 V27 C44 39 24 50 24 50 C24 50 4 39 4 27 V10 Z"
              fill="${corPrimaria}" stroke="${corSecundaria}" stroke-width="3"/>
        <path d="M24 5 V48 M6 24 H42" stroke="#fbf6eb" stroke-width="2.5" opacity="0.88"/>
        <circle cx="24" cy="24" r="5" fill="${corSecundaria}" stroke="#241a12" stroke-width="1.2"/>
      </svg>
    `;
  }

  function showToast(mensagem, tipo = "info") {
    const el = document.createElement("div");
    el.className = `toast ${tipo}`;
    el.textContent = mensagem;
    toastContainer.appendChild(el);
    setTimeout(() => {
      el.remove();
    }, 4200);
  }

  async function api(url, method = "GET", body = null) {
    if (state.mode === "static" && state.localEngine) {
      return state.localEngine.handleRequest(url, method, body);
    }

    const relativeUrl = String(url).replace(/^\//, "");
    const options = { method, headers: {} };
    if (body !== null) {
      options.headers["Content-Type"] = "application/json; charset=utf-8";
      options.body = JSON.stringify(body);
    }
    const response = await fetch(relativeUrl, options);
    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.erro || data.mensagem || "Erro ao comunicar com o servidor.");
    }
    return data;
  }

  // ================= SETUP SCREEN =================

  function renderPlayerInputs() {
    const antigos = Array.from(playerFieldsContainer.querySelectorAll(".player-row-card")).map((row) => ({
      nome: row.querySelector(".inp-player-name")?.value ?? "",
      cidade: row.querySelector(".inp-city-name")?.value ?? ""
    }));

    playerFieldsContainer.innerHTML = "";
    for (let i = 0; i < state.playerCount; i++) {
      const cor = corDaCidade(i);
      const nomeVal = antigos[i]?.nome || `Jogador ${i + 1}`;
      const cidadeVal = antigos[i]?.cidade || CIDADES_PADRAO[i];
      const row = document.createElement("div");
      row.className = "player-row-card";
      row.innerHTML = `
        <span class="player-crest-dot" style="background:${cor.primario}" title="Brasão ${i + 1}"></span>
        <div class="input-group">
          <label for="player-name-${i}">Governante ${i + 1}</label>
          <input id="player-name-${i}" class="inp-player-name" type="text" maxlength="28" value="${escapeHtml(nomeVal)}" placeholder="Jogador ${i + 1}">
        </div>
        <div class="input-group">
          <label for="city-name-${i}">Cidade-Estado</label>
          <input id="city-name-${i}" class="inp-city-name" type="text" maxlength="28" value="${escapeHtml(cidadeVal)}" placeholder="${CIDADES_PADRAO[i]}">
        </div>
      `;
      playerFieldsContainer.appendChild(row);
    }
  }

  playerCountControl.addEventListener("click", (ev) => {
    const btn = ev.target.closest(".seg-btn");
    if (!btn) return;
    state.playerCount = Number(btn.dataset.count);
    playerCountControl.querySelectorAll(".seg-btn").forEach((b) => b.classList.toggle("active", b === btn));
    renderPlayerInputs();
  });

  newGameForm.addEventListener("submit", async (ev) => {
    ev.preventDefault();
    const rows = Array.from(playerFieldsContainer.querySelectorAll(".player-row-card"));
    const jogadores = rows.map((row, idx) => ({
      nome: row.querySelector(".inp-player-name").value.trim() || `Jogador ${idx + 1}`,
      cidade: row.querySelector(".inp-city-name").value.trim() || CIDADES_PADRAO[idx]
    }));

    try {
      state.busy = true;
      state.previousStats = null;
      state.lastRenderedCityKey = null;
      state.lastShownYearEnd = null;
      state.lastShownVictoryKey = null;
      const res = await api("/api/novo-jogo", "POST", { jogadores });
      state.game = res.estado;
      showToast(res.mensagem, "destaque");
      render();
    } catch (err) {
      showToast(err.message, "erro");
    } finally {
      state.busy = false;
    }
  });

  // ================= TABS =================

  document.querySelector(".tabs-bar").addEventListener("click", (ev) => {
    const btn = ev.target.closest(".tab-btn");
    if (!btn) return;
    state.activeTab = btn.dataset.tab;
    document.querySelectorAll(".tab-btn").forEach((b) => {
      const active = b === btn;
      b.classList.toggle("active", active);
      b.setAttribute("aria-selected", active ? "true" : "false");
    });
    document.querySelectorAll(".tab-panel").forEach((panel) => {
      panel.classList.toggle("active", panel.id === `tab-${state.activeTab}`);
    });
  });

  // ================= AÇÕES DE JOGO =================

  async function executarAcao(url, body = null, onSuccess = null) {
    if (state.busy) return;
    const cidadeAntes = state.game?.cidadeAtual ? { ...state.game.cidadeAtual } : null;
    try {
      state.busy = true;
      const res = await api(url, "POST", body);
      if (res.estado) {
        if (cidadeAntes && res.estado.cidadeAtual && cidadeAntes.indice === res.estado.cidadeAtual.indice) {
          state.previousStats = cidadeAntes;
        } else {
          state.previousStats = null;
        }
        state.game = res.estado;
      }
      showToast(res.mensagem, res.ok ? (res.fimDoAno || res.terminada ? "destaque" : "sucesso") : "erro");
      if (res.ok && onSuccess) {
        onSuccess(res);
      }
      render();
    } catch (err) {
      showToast(err.message, "erro");
    } finally {
      state.busy = false;
    }
  }

  function lerInteiroInput(inputEl) {
    const n = parseInt(inputEl.value, 10);
    return Number.isNaN(n) ? 0 : n;
  }

  document.addEventListener("click", (ev) => {
    const chip = ev.target.closest("[data-set-input]");
    if (chip) {
      const target = document.getElementById(chip.dataset.setInput);
      if (target) {
        target.value = chip.dataset.val;
        atualizarPreviews();
      }
    }
  });

  // Mercado
  document.getElementById("btn-max-comprar-graos").addEventListener("click", () => {
    const c = state.game?.cidadeAtual;
    if (!c) return;
    inputGraosQtd.value = Math.max(1, Math.floor(c.florins / 2));
    atualizarPreviews();
  });
  document.getElementById("btn-max-vender-graos").addEventListener("click", () => {
    const c = state.game?.cidadeAtual;
    if (!c) return;
    inputGraosQtd.value = Math.max(1, c.graos);
    atualizarPreviews();
  });
  document.getElementById("btn-max-comprar-terras").addEventListener("click", () => {
    const c = state.game?.cidadeAtual;
    if (!c) return;
    inputTerrasQtd.value = Math.max(1, c.florins);
    atualizarPreviews();
  });
  document.getElementById("btn-max-vender-terras").addEventListener("click", () => {
    const c = state.game?.cidadeAtual;
    if (!c) return;
    inputTerrasQtd.value = Math.max(1, c.terras);
    atualizarPreviews();
  });

  document.getElementById("btn-comprar-graos").addEventListener("click", () => {
    executarAcao("/api/mercado/comprar-graos", { quantidade: lerInteiroInput(inputGraosQtd) });
  });
  document.getElementById("btn-vender-graos").addEventListener("click", () => {
    executarAcao("/api/mercado/vender-graos", { quantidade: lerInteiroInput(inputGraosQtd) });
  });
  document.getElementById("btn-comprar-terras").addEventListener("click", () => {
    executarAcao("/api/mercado/comprar-terras", { quantidade: lerInteiroInput(inputTerrasQtd) });
  });
  document.getElementById("btn-vender-terras").addEventListener("click", () => {
    executarAcao("/api/mercado/vender-terras", { quantidade: lerInteiroInput(inputTerrasQtd) });
  });

  // Agricultura
  document.getElementById("btn-colher").addEventListener("click", () => {
    executarAcao("/api/agricultura/colher", {});
  });
  document.getElementById("btn-distribuir").addEventListener("click", () => {
    executarAcao("/api/agricultura/distribuir", { percentual: Number(sliderAlimentos.value) });
  });

  // Governo
  document.getElementById("btn-arrecadar").addEventListener("click", () => {
    executarAcao("/api/governo/arrecadar", {});
  });
  document.getElementById("btn-definir-taxa").addEventListener("click", () => {
    executarAcao("/api/governo/taxa", { taxa: Number(sliderTaxa.value) });
  });
  document.getElementById("justice-cards").addEventListener("click", (ev) => {
    const card = ev.target.closest(".justice-card");
    if (!card) return;
    executarAcao("/api/governo/justica", { nivel: card.dataset.nivel });
  });

  // Construções & Defesa
  document.getElementById("btn-max-recrutar").addEventListener("click", () => {
    const c = state.game?.cidadeAtual;
    if (!c) return;
    inputSoldadosQtd.value = Math.max(1, Math.min(Math.floor(c.florins / 5), c.servos));
    atualizarPreviews();
  });
  document.getElementById("btn-max-construir").addEventListener("click", () => {
    const c = state.game?.cidadeAtual;
    if (!c) return;
    inputConstrucoesQtd.value = Math.max(1, Math.floor(c.florins / 20));
    atualizarPreviews();
  });

  document.getElementById("btn-recrutar").addEventListener("click", () => {
    executarAcao("/api/construcoes/recrutar", { quantidade: lerInteiroInput(inputSoldadosQtd) });
  });
  document.getElementById("btn-construir").addEventListener("click", () => {
    executarAcao("/api/construcoes/construir", { quantidade: lerInteiroInput(inputConstrucoesQtd) });
  });

  // Turno
  btnEndTurn.addEventListener("click", () => {
    executarAcao("/api/turno/encerrar", {}, (res) => {
      if (res.fimDoAno && !res.terminada && state.game?.ultimoResumoAno) {
        abrirModalFimDeAno(state.game.ultimoResumoAno, state.game);
      }
    });
  });

  // Live previews ao editar inputs/sliders
  [inputGraosQtd, inputTerrasQtd, inputSoldadosQtd, inputConstrucoesQtd].forEach((inp) => {
    inp.addEventListener("input", atualizarPreviews);
  });
  sliderAlimentos.addEventListener("input", () => {
    valSliderAlimentos.textContent = `${sliderAlimentos.value}%`;
    badgePctAlimentos.textContent = `${sliderAlimentos.value}% dos grãos`;
    atualizarPreviews();
  });
  sliderTaxa.addEventListener("input", () => {
    valSliderTaxa.textContent = `${sliderTaxa.value}%`;
    atualizarPreviews();
  });

  // Modais e botões de navegação
  document.getElementById("btn-new-game-top").addEventListener("click", voltarAoSetup);
  document.getElementById("btn-play-again").addEventListener("click", () => {
    modalVictory.classList.add("hidden");
    voltarAoSetup();
  });
  document.getElementById("btn-close-victory").addEventListener("click", () => {
    modalVictory.classList.add("hidden");
  });
  document.getElementById("btn-close-year-end").addEventListener("click", () => {
    modalYearEnd.classList.add("hidden");
  });

  document.getElementById("btn-open-about-setup").addEventListener("click", () => modalAbout.classList.remove("hidden"));
  document.getElementById("btn-open-about-game").addEventListener("click", () => modalAbout.classList.remove("hidden"));
  document.getElementById("btn-close-about").addEventListener("click", () => modalAbout.classList.add("hidden"));
  document.getElementById("btn-ok-about").addEventListener("click", () => modalAbout.classList.add("hidden"));

  function voltarAoSetup() {
    modalYearEnd.classList.add("hidden");
    modalVictory.classList.add("hidden");
    gameScreen.classList.add("hidden");
    setupScreen.classList.remove("hidden");
  }

  // ================= RENDERIZAÇÃO =================

  function render() {
    const g = state.game;
    if (!g || !g.possuiPartida) {
      setupScreen.classList.remove("hidden");
      gameScreen.classList.add("hidden");
      return;
    }

    setupScreen.classList.add("hidden");
    gameScreen.classList.remove("hidden");

    const atual = g.cidadeAtual;
    const cor = corDaCidade(atual.indice);
    const cityKey = `${g.anoAtual}-${atual.indice}`;
    if (state.lastRenderedCityKey !== cityKey) {
      state.lastRenderedCityKey = cityKey;
      resetActionInputs();
    }

    // Header
    headerCrest.innerHTML = criarEscudoSvg(cor.primario, cor.secundario, 32);
    if (g.terminada) {
      topbarTurnLabel.textContent = `Partida encerrada em ${g.anoAtual} · Vencedor: ${g.vencedor?.jogador || ""}`;
      btnEndTurn.disabled = true;
      btnEndTurn.textContent = "Partida Encerrada";
    } else {
      topbarTurnLabel.textContent = `Ano ${g.anoAtual} · Vez de ${atual.jogador} (${atual.nome})`;
      btnEndTurn.disabled = false;
      btnEndTurn.textContent = g.ultimoJogadorDoAno
        ? `Encerrar Turno e Fechar Ano ${g.anoAtual}`
        : "Encerrar Turno → Próximo Jogador";
    }

    const progressoDecada = Math.max(0, Math.min(100, ((g.anoAtual - 1400) / (g.anoFinal - 1400)) * 100));
    yearBadge.textContent = g.terminada ? `Ano ${g.anoAtual} (Encerrada)` : `Ano ${g.anoAtual} / ${g.anoFinal}`;
    yearProgressFill.style.width = `${progressoDecada}%`;

    // Hero da Cidade
    activeCityShield.innerHTML = criarEscudoSvg(cor.primario, cor.secundario, 48);
    activeCityName.textContent = atual.nome;
    activePlayerTitle.textContent = atual.titulo;
    activePlayerName.textContent = atual.jogador;
    activeGovSummary.textContent = `Imposto ${atual.taxaImposto}% · Justiça ${atual.nivelJustica}`;

    chipColheita.textContent = atual.colheitaFeita ? "Colheita: Feita" : "Colheita: Pendente";
    chipColheita.classList.toggle("done", atual.colheitaFeita);
    chipImpostos.textContent = atual.impostosArrecadados ? "Impostos: Arrecadados" : "Impostos: Pendentes";
    chipImpostos.classList.toggle("done", atual.impostosArrecadados);

    renderPanoramaSvg(atual, cor);
    renderPrestigio(atual);
    renderKpis(atual);

    // Sincronizar controles de governo e botões anuais
    sliderTaxa.value = atual.taxaImposto;
    valSliderTaxa.textContent = `${atual.taxaImposto}%`;
    badgeJusticaAtual.textContent = atual.nivelJustica;
    document.querySelectorAll(".justice-card").forEach((card) => {
      card.classList.toggle("active", card.dataset.nivel === atual.nivelJustica);
    });

    const btnColher = document.getElementById("btn-colher");
    btnColher.disabled = g.terminada || atual.colheitaFeita;
    btnColher.textContent = atual.colheitaFeita ? "Safra já colhida neste ano" : "Colher Safra do Ano";

    const btnArrecadar = document.getElementById("btn-arrecadar");
    btnArrecadar.disabled = g.terminada || atual.impostosArrecadados;
    btnArrecadar.textContent = atual.impostosArrecadados ? "Impostos já arrecadados neste ano" : "Arrecadar Impostos";

    atualizarPreviews();
    renderStandings(g);
    renderChronicle(g);

    if (g.terminada && g.vencedor) {
      const vKey = `${g.anoAtual}-${g.vencedor.nome}-${g.vencedor.prestigio}`;
      if (state.lastShownVictoryKey !== vKey) {
        state.lastShownVictoryKey = vKey;
        abrirModalVitoria(g);
      }
    }
  }

  function renderPrestigio(c) {
    activePrestigeValue.textContent = fmt(c.prestigio);
    if (c.proximoTituloPrestigio === -1) {
      activeNextTitleHint.textContent = "Título máximo alcançado (Rei/Rainha)!";
    } else {
      const falta = Math.max(0, c.proximoTituloPrestigio - c.prestigio);
      activeNextTitleHint.textContent = falta === 0
        ? `Pronto para subir a ${c.proximoTituloNome} (${fmt(c.proximoTituloPrestigio)}) no fim do ano!`
        : `Próximo: ${c.proximoTituloNome} (${fmt(c.proximoTituloPrestigio)} — faltam ${fmt(falta)})`;
    }

    const d = c.decomposicaoPrestigio;
    prestigeBreakdown.innerHTML = `
      <span>Obras: <strong>+${fmt(d.construcoes)}</strong></span>
      <span>Soldados: <strong>+${fmt(d.soldados)}</strong></span>
      <span>Servos: <strong>+${fmt(d.servos)}</strong></span>
      <span>Terras: <strong>+${fmt(d.terras)}</strong></span>
    `;

    nobleStepper.innerHTML = TITULOS.map((t, idx) => {
      let cls = "noble-step";
      if (idx < c.nivelTitulo) cls += " achieved";
      else if (idx === c.nivelTitulo) cls += " current";
      else if (idx === c.nivelTitulo + 1 && c.prestigio >= t.prestigio) cls += " ready-next";
      return `<div class="${cls}"><strong>${t.nome}</strong><span>${fmt(t.prestigio)}</span></div>`;
    }).join("");
  }

  function renderKpis(c) {
    const campos = ["florins", "terras", "graos", "servos", "soldados", "construcoes"];
    for (const campo of campos) {
      document.getElementById(`kpi-${campo}`).textContent = fmt(c[campo]);
      const deltaEl = document.getElementById(`delta-${campo}`);
      if (state.previousStats && typeof state.previousStats[campo] === "number") {
        const diff = c[campo] - state.previousStats[campo];
        if (diff > 0) {
          deltaEl.textContent = `+${fmt(diff)}`;
          deltaEl.className = "kpi-delta pos";
        } else if (diff < 0) {
          deltaEl.textContent = `${fmt(diff)}`;
          deltaEl.className = "kpi-delta neg";
        } else {
          deltaEl.textContent = "";
          deltaEl.className = "kpi-delta";
        }
      } else {
        deltaEl.textContent = "";
        deltaEl.className = "kpi-delta";
      }
    }
  }

  function renderPanoramaSvg(c, cor) {
    const numObrasVisiveis = Math.min(18, Math.max(2, 2 + Math.floor(c.construcoes / 2)));
    const numTorres = Math.min(6, Math.max(1, 1 + c.nivelTitulo));
    const numGuardas = Math.min(8, Math.ceil(c.soldados / 15));
    const camposDourados = c.colheitaFeita ? "#d8b44c" : "#6c8b4e";
    const ceuCor = c.impostosArrecadados ? "#f3dec0" : "#e7dec8";

    let edificiosSvg = "";
    for (let i = 0; i < numObrasVisiveis; i++) {
      const x = 140 + i * 32 + (i % 2) * 4;
      const h = 34 + ((i * 19) % 44);
      const y = 132 - h;
      const w = 22 + (i % 3) * 4;
      const ehTorre = i % 3 === 0 && i / 3 < numTorres;
      const fill = i % 2 === 0 ? "#d9c6a5" : "#ccb48c";
      edificiosSvg += `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="${fill}" stroke="#5c4b3b" stroke-width="1.2"/>`;
      if (ehTorre) {
        edificiosSvg += `
          <polygon points="${x - 2},${y} ${x + w / 2},${y - 16} ${x + w + 2},${y}" fill="${cor.primario}" stroke="#5c4b3b" stroke-width="1.2"/>
          <line x1="${x + w / 2}" y1="${y - 16}" x2="${x + w / 2}" y2="${y - 26}" stroke="#241a12" stroke-width="1.4"/>
          <polygon points="${x + w / 2},${y - 26} ${x + w / 2 + 12},${y - 22} ${x + w / 2},${y - 18}" fill="${cor.secundario}"/>
        `;
      } else {
        edificiosSvg += `<polygon points="${x - 1},${y} ${x + w / 2},${y - 8} ${x + w + 1},${y}" fill="#a45c44" stroke="#5c4b3b" stroke-width="1"/>`;
      }
      edificiosSvg += `<rect x="${x + 6}" y="${y + 10}" width="4" height="7" rx="2" fill="#4a3728"/>`;
    }

    // Duomo central proporcional ao título/obras
    const cupulaEscala = 1 + Math.min(0.45, c.construcoes * 0.015);
    const duomoSvg = `
      <g transform="translate(410, 132) scale(${cupulaEscala}) translate(-410, -132)">
        <rect x="378" y="84" width="64" height="48" fill="#e6d7bd" stroke="#5c4b3b" stroke-width="1.5"/>
        <path d="M382 84 C382 52, 438 52, 438 84 Z" fill="${cor.primario}" stroke="#5c4b3b" stroke-width="1.5"/>
        <line x1="410" y1="45" x2="410" y2="58" stroke="#d4a843" stroke-width="2.5"/>
        <circle cx="410" cy="44" r="3" fill="#d4a843"/>
      </g>
    `;

    // Guardas na muralha
    let guardasSvg = "";
    for (let g = 0; g < numGuardas; g++) {
      const gx = 158 + g * 68;
      guardasSvg += `
        <g transform="translate(${gx}, 122)">
          <rect x="0" y="4" width="6" height="10" rx="2" fill="${cor.primario}"/>
          <circle cx="3" cy="2" r="2.5" fill="#e7c6a5"/>
          <line x1="7" y1="-4" x2="7" y2="14" stroke="#3b2f26" stroke-width="1.3"/>
        </g>
      `;
    }

    // Ameias alinhadas exatamente da borda esquerda (125) até a direita (715) da muralha (largura 590)
    let ameiasSvg = "";
    for (let mx = 125; mx <= 701; mx += 24) {
      ameiasSvg += `<rect x="${mx}" y="125" width="14" height="5" fill="#b9a07a" stroke="#5c4b3b" stroke-width="1"/>`;
    }

    cityPanoramaSvg.innerHTML = `
      <svg viewBox="0 0 820 170" preserveAspectRatio="xMidYMid slice">
        <defs>
          <linearGradient id="skyGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="${ceuCor}"/>
            <stop offset="100%" stop-color="#f7efe0"/>
          </linearGradient>
        </defs>
        <rect width="820" height="170" fill="url(#skyGrad)"/>
        <circle cx="710" cy="40" r="20" fill="#e5b84b" opacity="0.65"/>
        <!-- Colinas da Toscana/Lombardia -->
        <path d="M0 132 Q120 96 260 124 T560 118 T820 126 V170 H0 Z" fill="#b7c094" opacity="0.7"/>
        <path d="M0 142 Q190 116 390 138 T820 134 V170 H0 Z" fill="${camposDourados}"/>
        <!-- Campos cultivados -->
        <path d="M18 156 H135 M28 163 H155 M650 152 H795 M670 161 H805" stroke="#4f6b37" stroke-width="1.5" opacity="0.55"/>
        <!-- Cidade -->
        ${edificiosSvg}
        ${duomoSvg}
        <!-- Muralha medieval -->
        <rect x="125" y="130" width="590" height="14" fill="#b9a07a" stroke="#5c4b3b" stroke-width="1.3"/>
        ${ameiasSvg}
        ${guardasSvg}
      </svg>
    `;

    const captionEl = document.getElementById("panorama-caption");
    if (captionEl) {
      captionEl.textContent = `${c.nome} · ${fmt(c.terras)} ha · ${fmt(c.construcoes)} obras · ${fmt(c.soldados)} soldados`;
    }
  }

  function resetActionInputs() {
    inputGraosQtd.value = "100";
    inputTerrasQtd.value = "500";
    inputSoldadosQtd.value = "10";
    inputConstrucoesQtd.value = "5";
    sliderAlimentos.value = "40";
    valSliderAlimentos.textContent = "40%";
    badgePctAlimentos.textContent = "40% dos grãos";
  }

  function atualizarPreviews() {
    const c = state.game?.cidadeAtual;
    if (!c) return;

    // Preview Grãos
    const qGraos = lerInteiroInput(inputGraosQtd);
    const prevGraos = document.getElementById("preview-mercado-graos");
    if (qGraos <= 0) {
      prevGraos.innerHTML = `<span class="warn-text">Informe uma quantidade maior que zero.</span>`;
    } else {
      const valor = qGraos * 2;
      const podeComprar = c.florins >= valor;
      const podeVender = c.graos >= qGraos;
      prevGraos.innerHTML = `
        <div>Compra: custa <strong>${fmt(valor)} florins</strong> <span class="${podeComprar ? "ok-text" : "warn-text"}">(${podeComprar ? "saldo suficiente" : "florins insuficientes"})</span></div>
        <div>Venda: rende <strong>${fmt(valor)} florins</strong> <span class="${podeVender ? "ok-text" : "warn-text"}">(${podeVender ? "estoque suficiente" : "grãos insuficientes"})</span></div>
      `;
    }

    // Preview Terras
    const qTerras = lerInteiroInput(inputTerrasQtd);
    const prevTerras = document.getElementById("preview-mercado-terras");
    if (qTerras <= 0) {
      prevTerras.innerHTML = `<span class="warn-text">Informe uma quantidade maior que zero.</span>`;
    } else {
      const podeComprar = c.florins >= qTerras;
      const podeVender = c.terras >= qTerras;
      const ganhoPrestigio = Math.floor((c.terras + qTerras) / 100) - Math.floor(c.terras / 100);
      prevTerras.innerHTML = `
        <div>Compra: custa <strong>${fmt(qTerras)} florins</strong> (+${fmt(ganhoPrestigio)} prestígio) <span class="${podeComprar ? "ok-text" : "warn-text"}">(${podeComprar ? "OK" : "insuficiente"})</span></div>
        <div>Venda: rende <strong>${fmt(qTerras)} florins</strong> <span class="${podeVender ? "ok-text" : "warn-text"}">(${podeVender ? "OK" : "insuficiente"})</span></div>
      `;
    }

    // Preview Colheita
    const prevColheita = document.getElementById("preview-colheita");
    let hectares = Math.min(c.terras, c.servos * 5);
    let sementes = Math.floor(hectares / 2);
    if (sementes > c.graos) {
      sementes = c.graos;
      hectares = sementes * 2;
    }
    const producao = hectares * 2;
    const saldoGraos = producao - sementes;
    prevColheita.innerHTML = c.colheitaFeita
      ? `<span class="ok-text">Colheita deste ano já foi realizada. Nova safra liberada em ${c.anoAtual + 1}.</span>`
      : `
        <div>Área cultivável agora: <strong>${fmt(hectares)} ha</strong> (gasta ${fmt(sementes)} grãos de semente)</div>
        <div>Produção bruta: <strong>${fmt(producao)} grãos</strong> (ganho líquido: <span class="ok-text">+${fmt(saldoGraos)} grãos</span>)</div>
      `;

    // Preview Alimentos
    const pct = Number(sliderAlimentos.value);
    const alimento = Math.floor((c.graos * pct) / 100);
    const necessidade = Math.floor(c.servos / 5);
    const prevAlimentos = document.getElementById("preview-alimentos");
    if (alimento >= necessidade) {
      const crescimento = Math.floor((c.servos * 5) / 100);
      prevAlimentos.innerHTML = `
        <div>Entregando <strong>${fmt(alimento)} grãos</strong> (necessidade: ${fmt(necessidade)} grãos)</div>
        <div class="ok-text">Comida suficiente! Crescimento esperado: +${fmt(crescimento)} servos (+${fmt(crescimento)} prestígio)</div>
      `;
    } else {
      const perdas = Math.min(necessidade - alimento, c.servos);
      prevAlimentos.innerHTML = `
        <div>Entregando <strong>${fmt(alimento)} grãos</strong> (necessidade: ${fmt(necessidade)} grãos)</div>
        <div class="warn-text">Déficit alimentar! Morreriam ${fmt(perdas)} servos.</div>
      `;
    }

    // Preview Impostos
    const mult = c.nivelJustica === "Justa" ? 0.8 : c.nivelJustica === "Severa" ? 1.3 : 1.0;
    const taxaPreview = Number(sliderTaxa.value);
    const baseAtual = Math.floor((c.servos * c.taxaImposto) / 100);
    const arrecadadoAtual = Math.floor(baseAtual * mult);
    const arrecadadoSimulado = Math.floor(Math.floor((c.servos * taxaPreview) / 100) * mult);
    document.getElementById("preview-impostos").innerHTML = c.impostosArrecadados
      ? `<span class="ok-text">Impostos de ${c.anoAtual} já arrecadados. Com taxa ${taxaPreview}% e justiça ${c.nivelJustica}, no próximo ano renderia ${fmt(arrecadadoSimulado)} florins.</span>`
      : `
        <div>Com as regras atuais (${c.taxaImposto}% · ${c.nivelJustica}): arrecada <strong>${fmt(arrecadadoAtual)} florins</strong></div>
        ${taxaPreview !== c.taxaImposto ? `<div>Se aplicar taxa de ${taxaPreview}%: arrecadaria <strong>${fmt(arrecadadoSimulado)} florins</strong></div>` : ""}
      `;

    // Preview Soldados
    const qSoldados = lerInteiroInput(inputSoldadosQtd);
    const prevSoldados = document.getElementById("preview-soldados");
    if (qSoldados <= 0) {
      prevSoldados.innerHTML = `<span class="warn-text">Informe uma quantidade maior que zero.</span>`;
    } else {
      const custo = qSoldados * 5;
      const ok = c.florins >= custo && c.servos >= qSoldados;
      prevSoldados.innerHTML = `
        <div>Custo: <strong>${fmt(custo)} florins</strong> e <strong>${fmt(qSoldados)} servos</strong></div>
        <div class="${ok ? "ok-text" : "warn-text"}">${ok ? `Pronto para recrutar (prestígio líquido +${fmt(qSoldados)})` : "Florins ou servos insuficientes"}</div>
      `;
    }

    // Preview Construções
    const qObras = lerInteiroInput(inputConstrucoesQtd);
    const prevObras = document.getElementById("preview-construcoes");
    if (qObras <= 0) {
      prevObras.innerHTML = `<span class="warn-text">Informe uma quantidade maior que zero.</span>`;
    } else {
      const custo = qObras * 20;
      const ok = c.florins >= custo;
      prevObras.innerHTML = `
        <div>Custo: <strong>${fmt(custo)} florins</strong> · Ganho: <strong>+${fmt(qObras * 10)} prestígio</strong></div>
        <div class="${ok ? "ok-text" : "warn-text"}">${ok ? "Recursos suficientes para construir" : "Florins insuficientes"}</div>
      `;
    }
  }

  function renderStandings(g) {
    standingsCount.textContent = `${g.cidades.length} ${g.cidades.length === 1 ? "cidade" : "cidades"}`;
    const ordenadas = [...g.cidades].sort((a, b) => {
      if (b.nivelTitulo !== a.nivelTitulo) return b.nivelTitulo - a.nivelTitulo;
      return b.prestigio - a.prestigio;
    });

    standingsList.innerHTML = ordenadas.map((c, pos) => {
      const cor = corDaCidade(c.indice);
      const pct = c.proximoTituloPrestigio === -1
        ? 100
        : Math.min(100, Math.round((c.prestigio / c.proximoTituloPrestigio) * 100));
      const meta = c.proximoTituloPrestigio === -1
        ? "Já alcançou Rei/Rainha"
        : `Meta ${c.proximoTituloNome}: ${fmt(c.proximoTituloPrestigio)}`;
      return `
        <div class="standing-item ${c.ehAtual && !g.terminada ? "current" : ""}">
          <div class="standing-top">
            <span class="standing-city">
              <span class="standing-dot" style="background:${cor.primario}"></span>
              ${pos + 1}º · ${escapeHtml(c.nome)}
            </span>
            <span class="standing-prestige">${fmt(c.prestigio)} pts</span>
          </div>
          <div class="standing-meta">
            ${escapeHtml(c.jogador)} · <strong>${escapeHtml(c.titulo)}</strong>
            ${c.ehAtual && !g.terminada ? ' · <span class="ok-text">Na vez</span>' : ""}
          </div>
          <div class="standing-bar"><div class="standing-bar-fill" style="width:${pct}%"></div></div>
          <div class="standing-goal">${meta} · ${fmt(c.florins)} florins</div>
        </div>
      `;
    }).join("");
  }

  function renderChronicle(g) {
    if (!g.historico || g.historico.length === 0) {
      chronicleList.innerHTML = `<li class="chronicle-item">Nenhum evento registrado ainda.</li>`;
      return;
    }
    chronicleList.innerHTML = g.historico.map((ev) => `
      <li class="chronicle-item ${escapeHtml(ev.tipo)}">
        <span class="chronicle-year">${ev.ano}</span>
        <span>${escapeHtml(ev.mensagem)}</span>
      </li>
    `).join("");
  }

  function abrirModalFimDeAno(resumo, g) {
    const key = `${resumo.anoEncerrado}-${resumo.promocoes.length}-${g.anoAtual}`;
    if (state.lastShownYearEnd === key) return;
    state.lastShownYearEnd = key;

    document.getElementById("year-end-title").textContent = `Fim do Ano ${resumo.anoEncerrado}`;
    document.getElementById("year-end-subtitle").textContent = `Todas as cidades avançaram para o ano ${g.anoAtual}.`;

    const promoBox = document.getElementById("year-end-promotions");
    if (resumo.promocoes.length === 0) {
      promoBox.innerHTML = `<div class="promo-item">Nenhuma promoção de título neste ano.</div>`;
    } else {
      promoBox.innerHTML = resumo.promocoes.map((p) => `
        <div class="promo-item">
          <strong>${escapeHtml(p.jogador)}</strong> (${escapeHtml(p.cidade)}) subiu de
          <em>${escapeHtml(p.tituloAnterior)}</em> para <strong>${escapeHtml(p.novoTitulo)}</strong>
          com ${fmt(p.prestigio)} de prestígio!
        </div>
      `).join("");
    }

    document.getElementById("year-end-standings").innerHTML = construirTabelaResumo(g.cidades);
    modalYearEnd.classList.remove("hidden");
  }

  function abrirModalVitoria(g) {
    const v = g.vencedor;
    document.getElementById("victory-title").textContent = `Vitória de ${v.nome}!`;
    document.getElementById("victory-banner").innerHTML = `
      <strong>${escapeHtml(v.jogador)}</strong> (${escapeHtml(v.titulo)}), governante de
      <strong>${escapeHtml(v.nome)}</strong>, venceu a partida com <strong>${fmt(v.prestigio)}</strong> de prestígio!
    `;
    document.getElementById("victory-table").innerHTML = construirTabelaResumo(g.cidades);
    modalYearEnd.classList.add("hidden");
    modalVictory.classList.remove("hidden");
  }

  function construirTabelaResumo(cidades) {
    const ordenadas = [...cidades].sort((a, b) => {
      if (b.nivelTitulo !== a.nivelTitulo) return b.nivelTitulo - a.nivelTitulo;
      return b.prestigio - a.prestigio;
    });
    return `
      <table class="summary-table">
        <thead>
          <tr>
            <th>Cidade</th>
            <th>Governante</th>
            <th>Título</th>
            <th>Prestígio</th>
            <th>Florins</th>
          </tr>
        </thead>
        <tbody>
          ${ordenadas.map((c) => `
            <tr>
              <td><strong>${escapeHtml(c.nome)}</strong></td>
              <td>${escapeHtml(c.jogador)}</td>
              <td>${escapeHtml(c.titulo)}</td>
              <td><strong>${fmt(c.prestigio)}</strong></td>
              <td>${fmt(c.florins)}</td>
            </tr>
          `).join("")}
        </tbody>
      </table>
    `;
  }

  // ================= INICIALIZAÇÃO =================

  function atualizarBadgeMotor() {
    const badge = document.getElementById("engine-mode-badge");
    if (!badge) return;
    badge.textContent = state.mode === "static" ? "Modo Navegador (estático)" : "Motor Java (local)";
  }

  function deveForcarModoEstatico() {
    const params = new URLSearchParams(window.location.search);
    if (params.get("mode") === "static") return true;
    if (window.location.protocol === "file:") return true;
    if (window.location.hostname.endsWith("github.io")) return true;
    return false;
  }

  function ativarModoEstatico() {
    state.mode = "static";
    if (!state.localEngine && window.BolonhaLocalEngine) {
      state.localEngine = window.BolonhaLocalEngine.createEngine();
    }
    atualizarBadgeMotor();
    return state.localEngine.getState();
  }

  async function init() {
    renderPlayerInputs();
    try {
      if (deveForcarModoEstatico()) {
        state.game = ativarModoEstatico();
        render();
        return;
      }

      const response = await fetch("api/estado");
      if (response.ok) {
        const data = await response.json();
        if (data && typeof data.emAndamento === "boolean") {
          state.mode = "java";
          atualizarBadgeMotor();
          state.game = data;
          render();
          return;
        }
      }
      state.game = ativarModoEstatico();
      render();
    } catch (err) {
      if (window.BolonhaLocalEngine) {
        state.game = ativarModoEstatico();
        render();
      } else {
        showToast("Não foi possível carregar o estado inicial: " + err.message, "erro");
      }
    }
  }

  init();
})();
