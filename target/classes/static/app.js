// ─── CONFIG ───────────────────────────────────────────────
const API_BASE = "";

function safeString(value) {
  return value == null ? "" : String(value);
}

async function readResponseBody(response) {
  const contentType = response.headers.get("content-type") || "";
  try {
    if (contentType.includes("application/json")) {
      return await response.json();
    }
    const text = await response.text();
    if (!text) return {};
    try {
      return JSON.parse(text);
    } catch {
      return { message: text };
    }
  } catch {
    return {};
  }
}

function friendlyApiMessage(path, status, data = {}) {
  const error = safeString(data.error).toLowerCase();
  const message = safeString(data.message).trim();
  const fields = data.fields && typeof data.fields === "object" ? Object.values(data.fields).filter(Boolean) : [];
  const firstFieldMessage = fields.length > 0 ? safeString(fields[0]) : "";
  const lowerMessage = message.toLowerCase();
  const normalizedPath = safeString(path);

  if (error === "validation_failed") return firstFieldMessage || message || "Revisa los campos marcados.";
  if (error === "invalid_parameter") return message || "El valor del parámetro no es válido.";

  if (status === 401 || status === 403) {
    if (normalizedPath.startsWith("/auth/sign-in")) return "Usuario y/o contraseña incorrectos.";
    if (normalizedPath.startsWith("/auth/sign-up")) {
      if (lowerMessage.includes("already") || lowerMessage.includes("registered")) return "El correo ya está registrado.";
      return "No se pudo crear la cuenta. Verifica los datos.";
    }
    return "Tu sesión expiró. Vuelve a iniciar sesión.";
  }

  if (status === 404) return message || "No se encontró el registro.";
  if (status === 409) return message || "Ya existe un registro con esos datos.";

  if (status >= 400 && status < 500) {
    if (lowerMessage.includes("invalid input value for enum") || lowerMessage.includes("enum")) {
      return "Uno de los valores seleccionados no es válido.";
    }
    if (lowerMessage.includes("duplicate key")) return "Ya existe un registro con esos datos.";
    if (lowerMessage.includes("foreign key")) return "Selecciona un registro relacionado válido.";
    if (lowerMessage.includes("null value in column") || lowerMessage.includes("not-null")) return "Completa los campos obligatorios.";
    if (normalizedPath.startsWith("/auth/sign-in")) return "Usuario y/o contraseña incorrectos.";
    if (normalizedPath.startsWith("/auth/sign-up")) return "No se pudo crear la cuenta. Verifica los datos.";
    return message || "Revisa los datos e intenta nuevamente.";
  }

  if (status >= 500) return "Ocurrió un error inesperado. Intenta nuevamente.";
  return message || `Error ${status}`;
}

// ─── API CLIENT ───────────────────────────────────────────
const api = {
  _token: () => localStorage.getItem("fin_token"),
  _refresh: () => localStorage.getItem("fin_refresh"),

  async _fetch(path, options = {}) {
    const headers = { 
      "Content-Type": "application/json",
      ...options.headers 
    };
    const token = api._token();
    if (token) headers["Authorization"] = `Bearer ${token}`;

    try {
      const response = await fetch(`${API_BASE}${path}`, { 
        ...options, 
        headers,
        mode: 'cors'
      });

      if (response.status === 401 && api._refresh()) {
        const refreshed = await api._tryRefresh();
        if (refreshed) {
          headers["Authorization"] = `Bearer ${api._token()}`;
          const retryResponse = await fetch(`${API_BASE}${path}`, { ...options, headers });
          if (!retryResponse.ok) {
            const retryData = await readResponseBody(retryResponse);
            throw new Error(friendlyApiMessage(path, retryResponse.status, retryData));
          }
          if (retryResponse.status === 204) return null;
          return readResponseBody(retryResponse);
        } else {
          logout();
          return null;
        }
      }

      if (response.status === 204) return null;
      
      const data = await readResponseBody(response);
      if (!response.ok) {
        throw new Error(friendlyApiMessage(path, response.status, data));
      }
      return data;
    } catch (error) {
      console.error(`API Error ${path}:`, error);
      throw error;
    }
  },

  async _tryRefresh() {
    try {
      const response = await fetch(`${API_BASE}/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: api._refresh() }),
      });
      if (!response.ok) return false;
      const data = await response.json();
      if (data.accessToken) {
        localStorage.setItem("fin_token", data.accessToken);
        if (data.refreshToken) localStorage.setItem("fin_refresh", data.refreshToken);
      } else if (data.session && data.session.accessToken) {
        localStorage.setItem("fin_token", data.session.accessToken);
        if (data.session.refreshToken) localStorage.setItem("fin_refresh", data.session.refreshToken);
      }
      return true;
    } catch (error) {
      console.error("Refresh error:", error);
      return false;
    }
  },

  get: (path) => api._fetch(path),
  post: (path, body) => api._fetch(path, { method: "POST", body: JSON.stringify(body) }),
  patch: (path, body) => api._fetch(path, { method: "PATCH", body: JSON.stringify(body) }),
  delete: (path) => api._fetch(path, { method: "DELETE" }),
};

// ─── AUTH ──────────────────────────────────────────────────
async function signIn(email, password) {
  const response = await fetch(`${API_BASE}/auth/sign-in`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password })
  });
  
  if (!response.ok) {
    const error = await readResponseBody(response);
    throw new Error(friendlyApiMessage("/auth/sign-in", response.status, error));
  }
  
  const data = await readResponseBody(response);
  
  if (data.accessToken) {
    localStorage.setItem("fin_token", data.accessToken);
    if (data.refreshToken) localStorage.setItem("fin_refresh", data.refreshToken);
  } else if (data.session && data.session.accessToken) {
    localStorage.setItem("fin_token", data.session.accessToken);
    if (data.session.refreshToken) localStorage.setItem("fin_refresh", data.session.refreshToken);
  }
  
  return data;
}

async function signUp(email, password, displayName) {
  const response = await fetch(`${API_BASE}/auth/sign-up`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password, displayName })
  });
  
  if (!response.ok) {
    const error = await readResponseBody(response);
    throw new Error(friendlyApiMessage("/auth/sign-up", response.status, error));
  }
  
  const data = await readResponseBody(response);
  
  if (data.accessToken) {
    localStorage.setItem("fin_token", data.accessToken);
    if (data.refreshToken) localStorage.setItem("fin_refresh", data.refreshToken);
  } else if (data.session && data.session.accessToken) {
    localStorage.setItem("fin_token", data.session.accessToken);
    if (data.session.refreshToken) localStorage.setItem("fin_refresh", data.session.refreshToken);
  }
  
  return data;
}

function logout() {
  const refreshToken = localStorage.getItem("fin_refresh");
  if (refreshToken) {
    fetch(`${API_BASE}/auth/sign-out`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken })
    }).catch(() => {});
  }
  localStorage.removeItem("fin_token");
  localStorage.removeItem("fin_refresh");
  showAuth();
}

// ─── STATE ─────────────────────────────────────────────────
let state = {
  user: null,
  accounts: [],
  transactions: [],
  recurring: [],
  debts: [],
  categories: [],
  installments: [],
  goals: [],
  budgets: [],
  reports: [],
  summary: null,
  categoryStats: [],
  upcoming: null,
  debtRatio: null,
  biweeklySchedule: null,
  activePeriod: "biweekly",
  activeSection: "dashboard",
};

let editCtx = { type: null, id: null };

// ─── UTILS ─────────────────────────────────────────────────
function fmt(amount, currency = "MXN") {
  return new Intl.NumberFormat("es-MX", { style: "currency", currency, minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(amount || 0));
}

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function relativeDate(isoStr) {
  if (!isoStr) return "—";
  const d = new Date(isoStr);
  const now = new Date();
  const diff = Math.floor((d - now) / 86400000);
  if (diff === 0) return "hoy";
  if (diff === 1) return "mañana";
  if (diff === -1) return "ayer";
  if (diff > 0 && diff < 7) return `en ${diff} días`;
  return d.toLocaleDateString("es-MX", { day: "numeric", month: "short" });
}

function el(id) { return document.getElementById(id); }

function showToast(msg, type = "info") {
  const container = el("toast-container");
  if (!container) return;
  const toast = document.createElement("div");
  toast.className = `toast ${type}`;
  toast.textContent = msg;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3500);
}

function setLoading(show) {
  const loader = el("global-loader");
  if (loader) loader.classList.toggle("hidden", !show);
}

// ─── NAVIGATION ────────────────────────────────────────────
const SECTION_TITLES = {
  dashboard: "Dashboard",
  transactions: "Transacciones",
  accounts: "Cuentas y Tarjetas",
  recurring: "Pagos recurrentes",
  debts: "Deudas / Préstamos",
  installments: "Partialidades",
  goals: "Metas Financieras",
  budgets: "Presupuestos",
  reports: "Reportes Mensuales",
  analytics: "Estadísticas",
  categories: "Categorías",
  profile: "Mi perfil",
  biweekly: "Organización Quincenal",
};

function navigateTo(section) {
  state.activeSection = section;

  document.querySelectorAll(".nav-item").forEach(btn =>
    btn.classList.toggle("active", btn.dataset.section === section)
  );
  document.querySelectorAll(".page-section").forEach(sectionEl => {
    sectionEl.classList.toggle("hidden", sectionEl.id !== `section-${section}`);
  });

  const titleEl = el("page-title");
  if (titleEl) titleEl.textContent = SECTION_TITLES[section] || section;

  const sidebar = document.querySelector(".sidebar");
  if (sidebar && window.innerWidth <= 768) sidebar.classList.remove("open");

  loadSection(section);
}

async function loadSection(section) {
  try {
    switch (section) {
      case "dashboard": await loadDashboard(); break;
      case "transactions": await loadTransactions(); break;
      case "accounts": await loadAccounts(); break;
      case "recurring": await loadRecurring(); break;
      case "debts": await loadDebts(); break;
      case "installments": await loadInstallments(); break;
      case "goals": await loadGoals(); break;
      case "budgets": await loadBudgets(); break;
      case "reports": await loadReports(); break;
      case "analytics": await loadAnalytics(); break;
      case "categories": await loadCategories(); break;
      case "profile": await loadProfile(); break;
      case "biweekly": await loadBiweeklySchedule(); break;
    }
  } catch (e) {
    console.error("Error loading section:", e);
    showToast(e.message || "Error al cargar datos", "error");
  }
}

// ─── DASHBOARD (con gráfica de sobreendeudamiento) ─────────
async function loadDashboard() {
  setLoading(true);
  try {
    const range = state.activePeriod;
    const [summary, upcoming, catStats, debtRatio] = await Promise.all([
      api.get(`/stats/summary?range=${range}`),
      api.get("/stats/upcoming"),
      api.get("/stats/categories"),
      api.get("/stats/debt-ratio"),
    ]);
    state.summary = summary;
    state.upcoming = upcoming;
    state.categoryStats = catStats || [];
    state.debtRatio = debtRatio;

    renderKPIs();
    renderUpcoming("upcoming-list", state.upcoming?.next7Days || []);
    renderCategoryBars("category-bars", state.categoryStats);
    renderDebtRatioGauge("debt-ratio-gauge", state.debtRatio);
  } catch (error) {
    console.error("Dashboard error:", error);
  } finally {
    setLoading(false);
  }
}

function renderDebtRatioGauge(containerId, debtRatio) {
  const c = el(containerId);
  if (!c) return;
  if (!debtRatio) {
    c.innerHTML = `<div class="empty-state"><div class="empty-state-icon">📊</div>Cargando indicador...</div>`;
    return;
  }
  
  const ratio = Number(debtRatio.debtToIncomeRatio) || 0;
  const riskLevel = debtRatio.riskLevel || "medio";
  let barColor = "#34d399";
  if (riskLevel === "alto") barColor = "#fbbf24";
  if (riskLevel === "crítico") barColor = "#f87171";
  
  c.innerHTML = `
    <div class="debt-gauge">
      <div class="gauge-header">
        <span class="gauge-label">📊 Índice de Endeudamiento</span>
        <span class="gauge-value ${riskLevel}">${ratio}%</span>
      </div>
      <div class="gauge-track">
        <div class="gauge-fill" style="width: ${Math.min(ratio, 100)}%; background: ${barColor}"></div>
      </div>
      <div class="gauge-stats">
        <div class="gauge-stat"><span>Ingresos:</span> <strong>${fmt(debtRatio.totalIncome, debtRatio.currency)}</strong></div>
        <div class="gauge-stat"><span>Pagos de deudas:</span> <strong>${fmt(debtRatio.totalDebtPayments, debtRatio.currency)}</strong></div>
      </div>
      <div class="gauge-recommendation ${riskLevel}">
        💡 ${debtRatio.recommendation || "Mantén un control saludable de tus finanzas."}
      </div>
    </div>
  `;
}

function renderKPIs() {
  const s = state.summary || {};
  const cur = state.user?.currency || "MXN";
  
  const kpiIncome = el("kpi-income");
  const kpiObligations = el("kpi-obligations");
  const kpiBalance = el("kpi-balance");
  const kpiDebt = el("kpi-debt");
  const kpiNote = el("kpi-income-note");
  
  if (kpiIncome) kpiIncome.textContent = fmt(s.income || 0, cur);
  
  const totalObligations = (s.expenses || 0);
  
  if (kpiObligations) kpiObligations.textContent = fmt(totalObligations, cur);
  if (kpiBalance) kpiBalance.textContent = fmt(s.availableBalance || 0, cur);
  if (kpiDebt) kpiDebt.textContent = fmt(s.debtPayments || 0, cur);
  if (kpiNote && state.user) kpiNote.textContent = `Periodo ${state.activePeriod === "biweekly" ? "Quincenal" : "Mensual"}`;
}

function renderUpcoming(containerId, items) {
  const c = el(containerId);
  if (!c) return;
  if (!items || items.length === 0) {
    c.innerHTML = `<div class="empty-state"><div class="empty-state-icon">📅</div>Sin vencimientos próximos</div>`;
    return;
  }
  const cur = state.user?.currency || "MXN";
  c.innerHTML = items.slice(0, 6).map(item => `
    <div class="upcoming-item">
      <div>
        <div class="item-name">${item.name}</div>
        <div class="item-due">${relativeDate(item.dueDate)}</div>
      </div>
      <div class="item-amount">${fmt(item.amount, cur)}</div>
    </div>
  `).join("");
}

function renderCategoryBars(containerId, items) {
  const c = el(containerId);
  if (!c) return;
  if (!items || items.length === 0) {
    c.innerHTML = `<div class="empty-state"><div class="empty-state-icon">📊</div>Sin datos de categorías</div>`;
    return;
  }
  const cur = state.user?.currency || "MXN";
  const max = Math.max(...items.map(i => Number(i.amount || 0)));
  c.innerHTML = items.map(item => {
    const val = Number(item.amount || 0);
    const pct = max > 0 ? Math.round((val / max) * 100) : 0;
    return `
      <div class="bar-row">
        <div class="bar-meta">
          <span>${item.categoryName}</span>
          <span>${fmt(val, cur)} (${item.percentage}%)</span>
        </div>
        <div class="bar-track"><div class="bar-fill" style="width:${pct}%"></div></div>
      </div>
    `;
  }).join("");
}

// ─── ORGANIZACIÓN QUINCENAL ───────────────────────────────
async function loadBiweeklySchedule() {
  setLoading(true);
  try {
    const schedule = await api.get("/stats/biweekly-schedule");
    state.biweeklySchedule = schedule || [];
    renderBiweeklySchedule();
  } catch (error) {
    console.error("Error loading biweekly schedule:", error);
    showToast("Error al cargar organización quincenal", "error");
  } finally {
    setLoading(false);
  }
}

function renderBiweeklySchedule() {
  const c = el("biweekly-list");
  if (!c) return;
  const cur = state.user?.currency || "MXN";
  
  if (!state.biweeklySchedule || state.biweeklySchedule.length === 0) {
    c.innerHTML = `<div class="empty-state"><div class="empty-state-icon">📅</div>No hay pagos programados para este mes</div>`;
    return;
  }
  
  c.innerHTML = state.biweeklySchedule.map(period => `
    <div class="biweekly-card ${period.remainingAfterPayments < 0 ? 'negative' : ''}">
      <div class="biweekly-header">
        <h4>${period.periodName}</h4>
        <span class="biweekly-dates">${new Date(period.startDate).toLocaleDateString()} - ${new Date(period.endDate).toLocaleDateString()}</span>
      </div>
      <div class="biweekly-summary">
        <div class="summary-item">
          <span>💰 Ingreso disponible:</span>
          <strong>${fmt(period.availableIncome, cur)}</strong>
        </div>
        <div class="summary-item">
          <span>📋 Total pagos:</span>
          <strong class="${period.totalAmount > period.availableIncome ? 'text-danger' : ''}">${fmt(period.totalAmount, cur)}</strong>
        </div>
        <div class="summary-item">
          <span>⚖️ Restante:</span>
          <strong class="${period.remainingAfterPayments < 0 ? 'text-danger' : 'text-success'}">${fmt(period.remainingAfterPayments, cur)}</strong>
        </div>
      </div>
      <div class="biweekly-payments">
        <div class="payments-header">Pagos programados:</div>
        <div class="payments-list">
          ${period.payments.map(p => `
            <div class="payment-row">
              <div class="payment-name">${p.name}</div>
              <div class="payment-date">${relativeDate(p.dueDate)}</div>
              <div class="payment-amount">${fmt(p.amount, cur)}</div>
              ${p.remainingBalance ? `<div class="payment-remaining">Saldo restante: ${fmt(p.remainingBalance, cur)}</div>` : ''}
            </div>
          `).join("")}
        </div>
      </div>
    </div>
  `).join("");
}

// ─── TRANSACTIONS ───────────────────────────────────────────
async function loadTransactions() {
  setLoading(true);
  try {
    const [transactions, categories, accounts] = await Promise.all([
      api.get("/transactions"),
      api.get("/categories"),
      api.get("/accounts"),
    ]);
    state.transactions = transactions || [];
    state.categories = categories || [];
    state.accounts = accounts || [];
    renderTransactions();
    populateCategorySelect("tx-category", state.categories);
    populateAccountSelect("tx-account", state.accounts);
  } finally {
    setLoading(false);
  }
}

function renderTransactions() {
  const c = el("transactions-list");
  if (!c) return;
  const cur = state.user?.currency || "MXN";
  if (!state.transactions.length) {
    c.innerHTML = `<div class="empty-state"><div class="empty-state-icon">💸</div>Sin transacciones aún</div>`;
    return;
  }
  
  const sorted = [...state.transactions].sort((a, b) => 
    new Date(b.transactionDate) - new Date(a.transactionDate)
  );
  
  c.innerHTML = sorted.map(tx => {
    const isExpense = tx.type === "expense";
    const isPaymentToCredit = tx.type === "payment";
    const cat = state.categories.find(c => c.id === tx.categoryId);
    const acc = state.accounts.find(a => a.id === tx.accountId);
    const isCreditAccount = acc?.type === "credit";
    
    let amountClass = "expense";
    let amountPrefix = "-";
    if (tx.type === "income") {
      amountClass = "income";
      amountPrefix = "+";
    } else if (isPaymentToCredit && isCreditAccount) {
      amountClass = "income";
      amountPrefix = "↓";
    }
    
    return `
      <div class="data-row">
        <div class="data-row-icon">${cat?.icon || (isExpense ? "💸" : "💰")}</div>
        <div class="data-row-info">
          <div class="data-row-name">${tx.description || tx.name}</div>
          <div class="data-row-meta">${cat?.name || "—"} · ${acc?.name || "—"} · ${relativeDate(tx.transactionDate)}</div>
        </div>
        <div class="data-row-amount ${amountClass}">
          ${amountPrefix}${fmt(tx.amount, cur)}
        </div>
        <div class="data-row-actions">
          <button class="btn-edit-sm" data-action="edit-tx" data-id="${tx.id}">Editar</button>
          <button class="btn-danger-sm" data-action="del-tx" data-id="${tx.id}">Eliminar</button>
        </div>
      </div>
    `;
  }).join("");
}

// ─── ACCOUNTS ──────────────────────────────────────────────
async function loadAccounts() {
  setLoading(true);
  try {
    state.accounts = await api.get("/accounts") || [];
    renderAccounts();
  } finally {
    setLoading(false);
  }
}

function renderAccounts() {
  const c = el("accounts-list");
  if (!c) return;
  const cur = state.user?.currency || "MXN";
  if (!state.accounts.length) {
    c.innerHTML = `<div class="empty-state"><div class="empty-state-icon">🏦</div>No hay cuentas registradas</div>`;
    return;
  }
  
  c.innerHTML = state.accounts.map(acc => {
    const typeClass = acc.type === "credit" ? "credit" : "";
    const typeLabel = {
      checking: "Débito", credit: "Tarjeta de crédito",
      savings: "Ahorro", loan: "Préstamo", cash: "Efectivo"
    }[acc.type] || acc.type;
    const creditLine = acc.creditLimit ? `
      <div class="account-limit">Límite: ${fmt(acc.creditLimit, acc.currency || cur)} · Disponible: ${fmt((acc.creditLimit || 0) - (acc.balance || 0), acc.currency || cur)}</div>
    ` : "";
    return `
      <div class="account-card ${typeClass}">
        <div class="account-name">${acc.name}</div>
        <div class="account-type">${typeLabel}${acc.institution ? ` · ${acc.institution}` : ""}</div>
        <div class="account-balance">${fmt(acc.balance, acc.currency || cur)}</div>
        ${creditLine}
        <div class="account-actions">
          <button class="btn-edit-sm" data-action="edit-acc" data-id="${acc.id}">Editar</button>
          <button class="btn-danger-sm" data-action="del-acc" data-id="${acc.id}">Eliminar</button>
        </div>
      </div>
    `;
  }).join("");
}

// ─── RECURRING (con fecha fin y tipo ingreso) ─────────────
async function loadRecurring() {
  setLoading(true);
  try {
    const [recurring, categories, accounts] = await Promise.all([
      api.get("/recurring-payments"),
      api.get("/categories"),
      api.get("/accounts"),
    ]);
    state.recurring = recurring || [];
    state.categories = categories || [];
    state.accounts = accounts || [];
    renderRecurring();
    populateCategorySelect("rec-category", state.categories);
    populateAccountSelect("rec-account", state.accounts);
  } finally {
    setLoading(false);
  }
}

function renderRecurring() {
  const c = el("recurring-list");
  if (!c) return;
  const cur = state.user?.currency || "MXN";
  const freqMap = { weekly: "Semanal", biweekly: "Quincenal", monthly: "Mensual", quarterly: "Trimestral", yearly: "Anual", custom: "Personalizado" };
  
  if (!state.recurring.length) {
    c.innerHTML = `<div class="empty-state"><div class="empty-state-icon">🔄</div>Sin pagos recurrentes</div>`;
    return;
  }
  
  c.innerHTML = state.recurring.map(r => {
    const isIncome = r.paymentType === "income";
    return `
      <div class="data-row">
        <div class="data-row-icon">${isIncome ? "💰" : "🔄"}</div>
        <div class="data-row-info">
          <div class="data-row-name">${r.name}</div>
          <div class="data-row-meta">
            ${freqMap[r.frequency] || r.frequency} · ${isIncome ? "Ingreso" : "Gasto"} · 
            próximo: ${relativeDate(r.nextDueDate)}${r.endDate ? ` · hasta: ${new Date(r.endDate).toLocaleDateString()}` : ""}
          </div>
        </div>
        <div class="data-row-amount ${isIncome ? "income" : "expense"}">
          ${isIncome ? "+" : "-"}${fmt(r.amount, cur)}
        </div>
        <div class="data-row-actions">
          <button class="btn-edit-sm" data-action="edit-rec" data-id="${r.id}">Editar</button>
          <button class="btn-danger-sm" data-action="del-rec" data-id="${r.id}">Eliminar</button>
        </div>
      </div>
    `;
  }).join("");
}

// ─── DEBTS ─────────────────────────────────────────────────
async function loadDebts() {
  setLoading(true);
  try {
    state.debts = await api.get("/debts") || [];
    renderDebts();
  } finally {
    setLoading(false);
  }
}

function renderDebts() {
  const c = el("debts-list");
  if (!c) return;
  const cur = state.user?.currency || "MXN";
  
  if (!state.debts.length) {
    c.innerHTML = `<div class="empty-state"><div class="empty-state-icon">📋</div>Sin deudas registradas</div>`;
    return;
  }
  
  c.innerHTML = state.debts.map(d => {
    const remaining = d.remainingBalance || d.principalBalance || 0;
    const total = d.principalBalance || remaining;
    const pct = total > 0 ? Math.round(((total - remaining) / total) * 100) : 0;
    return `
      <div class="data-row" style="flex-direction:column;align-items:stretch;gap:.75rem">
        <div style="display:flex;align-items:center;gap:1rem">
          <div class="data-row-icon">📋</div>
          <div class="data-row-info">
            <div class="data-row-name">${d.name}</div>
            <div class="data-row-meta">Próximo pago: ${relativeDate(d.nextDueDate)} · Pago: ${fmt(d.installment || d.minimumPayment || 0, cur)}/${d.frequency || "mensual"}</div>
          </div>
          <div class="data-row-amount expense">${fmt(remaining, cur)}</div>
          <div class="data-row-actions">
            <button class="btn-edit-sm" data-action="edit-debt" data-id="${d.id}">Editar</button>
            <button class="btn-danger-sm" data-action="del-debt" data-id="${d.id}">Eliminar</button>
          </div>
        </div>
        <div style="display:grid;gap:.25rem">
          <div class="bar-track"><div class="bar-fill" style="width:${pct}%;background:linear-gradient(90deg,var(--green),var(--blue))"></div></div>
        </div>
      </div>
    `;
  }).join("");
}

// ─── INSTALLMENTS ─────────────────────────────────────────────
async function loadInstallments() {
  setLoading(true);
  try {
    const [installments, debts, accounts] = await Promise.all([
      api.get("/installments"),
      api.get("/debts"),
      api.get("/accounts"),
    ]);
    state.installments = installments || [];
    state.debts = debts || [];
    state.accounts = accounts || [];
    renderInstallments();
    populateDebtSelect("inst-debt", state.debts);
    populateAccountSelectForInstallments("inst-account", state.accounts);
  } catch (error) {
    console.error("Error loading installments:", error);
    showToast("Error al cargar partialidades", "error");
  } finally {
    setLoading(false);
  }
}

function populateAccountSelectForInstallments(selectId, accounts) {
  const select = el(selectId);
  if (!select) return;
  const creditAccounts = accounts.filter(a => a.type === "credit");
  const cur = state.user?.currency || "MXN";
  select.innerHTML = `<option value="">Seleccionar tarjeta de crédito (opcional)</option>` +
    creditAccounts.map(a => `<option value="${a.id}">${a.name} - Saldo: ${fmt(a.balance, cur)}</option>`).join("");
}

function renderInstallments() {
  const c = el("installments-list");
  if (!c) return;
  const cur = state.user?.currency || "MXN";
  
  if (!state.installments || state.installments.length === 0) {
    c.innerHTML = `<div class="empty-state"><div class="empty-state-icon">📋</div>Sin partialidades registradas</div>`;
    return;
  }
  
  const sorted = [...state.installments].sort((a, b) => new Date(a.dueDate) - new Date(b.dueDate));
  
  c.innerHTML = sorted.map(inst => {
    const isPaid = inst.paid;
    const debt = state.debts.find(d => d.id === inst.debtId);
    const account = state.accounts.find(a => a.id === inst.accountId);
    return `
      <div class="data-row" style="${isPaid ? 'opacity:0.7' : ''}">
        <div class="data-row-icon">${isPaid ? "✅" : "📅"}</div>
        <div class="data-row-info">
          <div class="data-row-name">
            Partialidad #${inst.number} - ${debt?.name || "Deuda"}
            ${account ? `<span class="badge badge-blue">${account.name}</span>` : ""}
          </div>
          <div class="data-row-meta">
            Vence: ${relativeDate(inst.dueDate)} · 
            ${isPaid ? `Pagada el ${inst.paidAt ? new Date(inst.paidAt).toLocaleDateString() : ''}` : "Pendiente"}
            ${inst.originalPurchaseAmount ? ` · Compra original: ${fmt(inst.originalPurchaseAmount, cur)}` : ""}
          </div>
        </div>
        <div class="data-row-amount ${isPaid ? "income" : "expense"}">
          ${fmt(inst.amount, cur)}
        </div>
        <div class="data-row-actions">
          ${!isPaid ? `<button class="btn-success-sm" data-action="pay-inst" data-id="${inst.id}">Pagar</button>` : ''}
          <button class="btn-edit-sm" data-action="edit-inst" data-id="${inst.id}">Editar</button>
          <button class="btn-danger-sm" data-action="del-inst" data-id="${inst.id}">Eliminar</button>
        </div>
      </div>
    `;
  }).join("");
}

function populateDebtSelect(selectId, debts) {
  const select = el(selectId);
  if (!select) return;
  const cur = state.user?.currency || "MXN";
  select.innerHTML = `<option value="">Seleccionar deuda</option>` +
    debts.map(d => `<option value="${d.id}">${d.name} - Saldo: ${fmt(d.remainingBalance || d.principalBalance || 0, cur)}</option>`).join("");
}

// ─── ANALYTICS ─────────────────────────────────────────────
async function loadAnalytics() {
  setLoading(true);
  try {
    const [summary, upcoming, catStats] = await Promise.all([
      api.get("/stats/summary"),
      api.get("/stats/upcoming"),
      api.get("/stats/categories"),
    ]);
    state.summary = summary || {};
    state.upcoming = upcoming;
    state.categoryStats = catStats || [];
    
    renderAnalyticsSummary();
    renderCategoryBars("analytics-categories", state.categoryStats);
    renderUpcoming("analytics-upcoming", state.upcoming?.next7Days || []);
  } finally {
    setLoading(false);
  }
}

function renderAnalyticsSummary() {
  const s = state.summary || {};
  const cur = state.user?.currency || "MXN";
  const rows = [
    ["Ingresos", fmt(s.income || 0, cur)],
    ["Gastos", fmt(s.expenses || 0, cur)],
    ["Pagos fijos", fmt(s.fixedPayments || 0, cur)],
    ["Pagos de deudas", fmt(s.debtPayments || 0, cur)],
    ["Balance disponible", fmt(s.availableBalance || 0, cur)],
  ];
  const container = el("analytics-summary");
  if (container) {
    container.innerHTML = rows.map(([label, val]) => `
      <div class="analytics-row"><span>${label}</span><strong>${val}</strong></div>
    `).join("");
  }
}

// ─── CATEGORIES ────────────────────────────────────────────
async function loadCategories() {
  setLoading(true);
  try {
    state.categories = await api.get("/categories") || [];
    renderCategories();
  } finally {
    setLoading(false);
  }
}

function renderCategories() {
  const c = el("categories-list");
  if (!c) return;
  if (!state.categories.length) {
    c.innerHTML = `<div class="empty-state"><div class="empty-state-icon">🏷️</div>Sin categorías</div>`;
    return;
  }
  
  c.innerHTML = state.categories.map(cat => `
    <div class="category-pill">
      <div class="cat-dot" style="background:${cat.color || '#c9a84c'}"></div>
      <div class="cat-info">
        <div class="cat-name">${cat.icon ? cat.icon + " " : ""}${cat.name}</div>
        <div class="cat-type">${cat.type || "gasto"}</div>
      </div>
      <div class="cat-actions">
        <button class="btn-danger-sm" data-action="del-cat" data-id="${cat.id}">✕</button>
      </div>
    </div>
  `).join("");
}

// ─── PROFILE ───────────────────────────────────────────────
async function loadProfile() {
  setLoading(true);
  try {
    const user = await api.get("/me");
    state.user = user;
    fillProfile(user);
  } finally {
    setLoading(false);
  }
}

function fillProfile(user) {
  if (!user) return;
  const income = el("profile-income");
  const currency = el("profile-currency");
  const period = el("profile-period");
  
  if (income) income.value = user.monthlyIncome || "";
  if (currency) currency.value = user.currency || "MXN";
  if (period) period.value = user.payCycle === "monthly" ? "monthly" : "biweekly";
  
  toggleProfilePeriodFields(user.payCycle || "biweekly");
  
  if (user.payDays && user.payDays.length) {
    if (user.payCycle === "biweekly") {
      const payday1 = el("profile-payday1");
      const payday2 = el("profile-payday2");
      if (payday1) payday1.value = user.payDays[0] || "";
      if (payday2) payday2.value = user.payDays[1] || "";
    } else {
      const paydayMonthly = el("profile-payday-monthly");
      if (paydayMonthly) paydayMonthly.value = user.payDays[0] || "";
    }
  }
}

function toggleProfilePeriodFields(period) {
  const isBiweekly = period === "biweekly";
  const quincenalFields = el("profile-quincenal-fields");
  const quincenalFields2 = el("profile-quincenal-fields2");
  const monthlyFields = el("profile-monthly-fields");
  
  if (quincenalFields) quincenalFields.classList.toggle("hidden", !isBiweekly);
  if (quincenalFields2) quincenalFields2.classList.toggle("hidden", !isBiweekly);
  if (monthlyFields) monthlyFields.classList.toggle("hidden", isBiweekly);
}

// ─── HELPERS ───────────────────────────────────────────────
function populateCategorySelect(selectId, categories) {
  const select = el(selectId);
  if (!select) return;
  select.innerHTML = `<option value="">Sin categoría</option>` +
    categories.map(c => `<option value="${c.id}">${c.icon || ""} ${c.name}</option>`).join("");
}

function populateAccountSelect(selectId, accounts) {
  const select = el(selectId);
  if (!select) return;
  select.innerHTML = `<option value="">Seleccionar cuenta</option>` +
    accounts.map(a => `<option value="${a.id}">${a.name}</option>`).join("");
  
  const transferSelect = el("tx-transfer-account");
  if (transferSelect) {
    transferSelect.innerHTML = `<option value="">Seleccionar cuenta destino</option>` +
      accounts.map(a => `<option value="${a.id}">${a.name}</option>`).join("");
  }
}

function toggleCreditFields() {
  const type = el("acc-type")?.value;
  syncCreditOnlyFields(document, type);
}

function showInlineForm(formId, btnId) {
  const form = el(formId);
  const btn = el(btnId);
  if (form) form.classList.toggle("hidden");
  if (btn) btn.textContent = form?.classList.contains("hidden") ? "+ Nuevo" : "✕ Cancelar";
}

function hideForm(formId, btnId, label) {
  const form = el(formId);
  const btn = el(btnId);
  if (form) form.classList.add("hidden");
  if (btn) btn.textContent = label || "+ Nuevo";
}

// ─── MODAL ─────────────────────────────────────────────────
function openModal(title, bodyHtml, onSave) {
  const modalTitle = el("modal-title");
  const modalBody = el("modal-body");
  const modal = el("edit-modal");
  const saveBtn = el("modal-save");
  
  if (modalTitle) modalTitle.textContent = title;
  if (modalBody) modalBody.innerHTML = bodyHtml;
  if (modal) modal.classList.remove("hidden");
  if (saveBtn) saveBtn._handler = onSave;
}

function closeModal() {
  const modal = el("edit-modal");
  const saveBtn = el("modal-save");
  if (modal) modal.classList.add("hidden");
  if (saveBtn) saveBtn._handler = null;
  editCtx = { type: null, id: null };
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (ch) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;",
  }[ch]));
}

function toDateInputValue(value) {
  if (!value) return "";
  const text = String(value);
  if (/^\d{4}-\d{2}-\d{2}$/.test(text)) return text;
  return text.slice(0, 10);
}

function buildSelectOptions(items, placeholder, selectedValue, getValue, getLabel) {
  const selected = String(selectedValue ?? "");
  const options = (items || []).map(item => {
    const value = String(getValue(item) ?? "");
    const label = getLabel(item);
    return `<option value="${escapeHtml(value)}"${selected === value ? " selected" : ""}>${escapeHtml(label)}</option>`;
  }).join("");
  return `<option value="">${escapeHtml(placeholder)}</option>${options}`;
}

// ─── WIRING ────────────────────────────────────────────────
function wireNav() {
  document.querySelectorAll(".nav-item[data-section]").forEach(btn => {
    btn.addEventListener("click", () => navigateTo(btn.dataset.section));
  });

  const logoutBtn = el("btn-logout");
  if (logoutBtn) logoutBtn.addEventListener("click", () => {
    if (confirm("¿Cerrar sesión?")) logout();
  });

  const hamburger = el("hamburger");
  if (hamburger) hamburger.addEventListener("click", () => {
    document.querySelector(".sidebar")?.classList.toggle("open");
  });

  const periodSelect = el("period-select");
  if (periodSelect) periodSelect.addEventListener("change", (e) => {
    state.activePeriod = e.target.value;
    loadSection(state.activeSection);
  });

  const modalClose = el("modal-close");
  const modalCancel = el("modal-cancel");
  const modalOverlay = el("edit-modal");
  const modalSave = el("modal-save");
  
  if (modalClose) modalClose.addEventListener("click", closeModal);
  if (modalCancel) modalCancel.addEventListener("click", closeModal);
  if (modalOverlay) modalOverlay.addEventListener("click", (e) => { if (e.target === modalOverlay) closeModal(); });
  if (modalSave) modalSave.addEventListener("click", () => {
    const handler = modalSave._handler;
    if (handler) handler();
  });
}

// ─── FORMS ─────────────────────────────────────────────────
function wireTxForm() {
  const addBtn = el("btn-add-transaction");
  const cancelBtn = el("btn-cancel-transaction");
  const saveBtn = el("btn-save-transaction");
  const dateInput = el("tx-date");
  const typeSelect = el("tx-type");
  const transferGroup = el("transfer-account-group");
  const validTypes = ["expense", "income", "transfer", "payment", "adjustment"];
  
  if (typeSelect && transferGroup) {
    typeSelect.addEventListener("change", () => {
      transferGroup.classList.toggle("hidden", typeSelect.value !== "transfer");
    });
  }
  
  if (addBtn) {
    const newAddBtn = addBtn.cloneNode(true);
    addBtn.parentNode.replaceChild(newAddBtn, addBtn);
    
    newAddBtn.addEventListener("click", () => {
      showInlineForm("transaction-form-wrap", "btn-add-transaction");
      if (transferGroup) transferGroup.classList.add("hidden");
      if (typeSelect) typeSelect.value = "expense";
    });
  }
  
  if (cancelBtn) {
    const newCancelBtn = cancelBtn.cloneNode(true);
    cancelBtn.parentNode.replaceChild(newCancelBtn, cancelBtn);
    
    newCancelBtn.addEventListener("click", () => {
      hideForm("transaction-form-wrap", "btn-add-transaction", "+ Nueva");
      document.querySelectorAll("#transaction-form-wrap input, #transaction-form-wrap select").forEach(i => i.value = "");
      if (dateInput) dateInput.value = todayIso();
      if (transferGroup) transferGroup.classList.add("hidden");
      if (typeSelect) typeSelect.value = "expense";
    });
  }
  
  if (dateInput) dateInput.value = todayIso();
  
  if (saveBtn) {
    const newSaveBtn = saveBtn.cloneNode(true);
    saveBtn.parentNode.replaceChild(newSaveBtn, saveBtn);
    
    newSaveBtn.addEventListener("click", async () => {
      if (newSaveBtn.dataset.saving === "true") return;
      newSaveBtn.dataset.saving = "true";
      
      try {
        let type = el("tx-type")?.value;
        
        if (!validTypes.includes(type)) {
          console.warn(`Tipo de transacción inválido: "${type}", cambiando a "expense"`);
          type = "expense";
        }
        
        const accountId = el("tx-account")?.value;
        const amount = Number(el("tx-amount")?.value || 0);
        const transactionDate = el("tx-date")?.value;
        const description = el("tx-name")?.value.trim();
        const categoryId = el("tx-category")?.value || null;
        const notes = el("tx-note")?.value.trim() || null;
        const transferAccountId = el("tx-transfer-account")?.value;
        
        if (!accountId) {
          showToast("Selecciona una cuenta", "error");
          return;
        }
        
        if (!amount || amount <= 0) {
          showToast("Ingresa un monto válido", "error");
          return;
        }
        
        if (!transactionDate) {
          showToast("Ingresa una fecha", "error");
          return;
        }
        
        if (!description) {
          showToast("Ingresa una descripción", "error");
          return;
        }
        
        if (type === "transfer") {
          if (!transferAccountId) {
            showToast("Selecciona la cuenta destino", "error");
            return;
          }
          if (transferAccountId === accountId) {
            showToast("La cuenta origen y destino no pueden ser iguales", "error");
            return;
          }
        }
        
        const body = {
          accountId: accountId,
          transferAccountId: type === "transfer" ? transferAccountId : null,
          categoryId: categoryId,
          debtId: null,
          type: type,
          description: description,
          amount: amount,
          currency: state.user?.currency || "MXN",
          transactionDate: transactionDate,
          notes: notes
        };
        
        console.log("Enviando transacción:", body);
        await api.post("/transactions", body);
        showToast("Transacción guardada", "success");
        
        hideForm("transaction-form-wrap", "btn-add-transaction", "+ Nueva");
        document.querySelectorAll("#transaction-form-wrap input, #transaction-form-wrap select").forEach(i => i.value = "");
        if (dateInput) dateInput.value = todayIso();
        if (transferGroup) transferGroup.classList.add("hidden");
        if (typeSelect) typeSelect.value = "expense";
        
        await loadTransactions();
        
      } catch (e) {
        console.error("Error al guardar transacción:", e);
        showToast(e.message || "Error al guardar la transacción", "error");
      } finally {
        newSaveBtn.dataset.saving = "false";
      }
    });
  }
}

function wireAccForm() {
  const addBtn = el("btn-add-account");
  const cancelBtn = el("btn-cancel-account");
  const saveBtn = el("btn-save-account");
  const msiBtn = el("btn-msi-purchase");
  const typeSelect = el("acc-type");
  const institutionSelect = el("acc-institution");
  const otherGroup = el("acc-other-institution-group");
  const otherInput = el("acc-other-institution");
  
  if (institutionSelect && otherGroup) {
    institutionSelect.addEventListener("change", () => {
      otherGroup.classList.toggle("hidden", institutionSelect.value !== "Otra");
      if (otherInput) otherInput.value = "";
    });
  }
  
  if (addBtn) {
    const newAddBtn = addBtn.cloneNode(true);
    addBtn.parentNode.replaceChild(newAddBtn, addBtn);
    newAddBtn.addEventListener("click", () => showInlineForm("account-form-wrap", "btn-add-account"));
  }
  
  if (cancelBtn) {
    const newCancelBtn = cancelBtn.cloneNode(true);
    cancelBtn.parentNode.replaceChild(newCancelBtn, cancelBtn);
    newCancelBtn.addEventListener("click", () => hideForm("account-form-wrap", "btn-add-account", "+ Nueva cuenta"));
  }
  
  if (typeSelect) typeSelect.addEventListener("change", toggleCreditFields);
  
  // Botón de compra a meses
  if (msiBtn) {
    const newMsiBtn = msiBtn.cloneNode(true);
    msiBtn.parentNode.replaceChild(newMsiBtn, msiBtn);
    newMsiBtn.addEventListener("click", () => {
      const creditCards = state.accounts.filter(a => a.type === "credit");
      if (creditCards.length === 0) {
        showToast("No tienes tarjetas de crédito registradas", "error");
        return;
      }
      showCreditCardPurchaseForm();
    });
  }
  
  if (saveBtn) {
    const newSaveBtn = saveBtn.cloneNode(true);
    saveBtn.parentNode.replaceChild(newSaveBtn, saveBtn);
    
    newSaveBtn.addEventListener("click", async () => {
      if (newSaveBtn.dataset.saving === "true") return;
      newSaveBtn.dataset.saving = "true";
      
      try {
        let institution = institutionSelect?.value || "";
        if (institution === "Otra") {
          institution = otherInput?.value.trim() || "";
          if (!institution) {
            showToast("Escribe el nombre de la institución", "error");
            return;
          }
        }
        
        const body = {
          type: el("acc-type")?.value,
          name: el("acc-name")?.value.trim(),
          institution: institution,
          currency: el("acc-currency")?.value,
          balance: Number(el("acc-balance")?.value || 0),
          creditLimit: el("acc-type")?.value === "credit" ? Number(el("acc-limit")?.value || 0) : null,
          closingDay: el("acc-type")?.value === "credit" ? Number(el("acc-cut-day")?.value || 0) : null,
          dueDay: el("acc-type")?.value === "credit" ? Number(el("acc-due-day")?.value || 0) : null,
          active: true,
        };
        if (!body.name) {
          showToast("Ingresa el nombre de la cuenta", "error");
          return;
        }
        await api.post("/accounts", body);
        showToast("Cuenta creada", "success");
        hideForm("account-form-wrap", "btn-add-account", "+ Nueva cuenta");
        if (institutionSelect) institutionSelect.value = "";
        if (otherGroup) otherGroup.classList.add("hidden");
        if (otherInput) otherInput.value = "";
        await loadAccounts();
      } catch (e) {
        showToast(e.message, "error");
      } finally {
        newSaveBtn.dataset.saving = "false";
      }
    });
  }
}

function wireRecurringForm() {
  const addBtn = el("btn-add-recurring");
  const cancelBtn = el("btn-cancel-recurring");
  const saveBtn = el("btn-save-recurring");
  const nextDue = el("rec-next-due");
  const paymentTypeSelect = el("rec-payment-type");
  const endDateGroup = el("rec-end-date-group");
  
  if (paymentTypeSelect && endDateGroup) {
    paymentTypeSelect.addEventListener("change", () => {
      endDateGroup.classList.toggle("hidden", paymentTypeSelect.value === "expense");
    });
  }
  
  if (addBtn) {
    const newAddBtn = addBtn.cloneNode(true);
    addBtn.parentNode.replaceChild(newAddBtn, addBtn);
    newAddBtn.addEventListener("click", () => showInlineForm("recurring-form-wrap", "btn-add-recurring"));
  }
  
  if (cancelBtn) {
    const newCancelBtn = cancelBtn.cloneNode(true);
    cancelBtn.parentNode.replaceChild(newCancelBtn, cancelBtn);
    newCancelBtn.addEventListener("click", () => hideForm("recurring-form-wrap", "btn-add-recurring", "+ Nuevo"));
  }
  
  if (nextDue) nextDue.value = todayIso();
  
  if (saveBtn) {
    const newSaveBtn = saveBtn.cloneNode(true);
    saveBtn.parentNode.replaceChild(newSaveBtn, saveBtn);
    
    newSaveBtn.addEventListener("click", async () => {
      if (newSaveBtn.dataset.saving === "true") return;
      newSaveBtn.dataset.saving = "true";
      
      try {
        const frequency = el("rec-frequency")?.value?.toLowerCase();
        const paymentType = el("rec-payment-type")?.value || "expense";
        const endDate = el("rec-end-date")?.value || null;
        
        const body = {
          name: el("rec-name")?.value.trim(),
          amount: Number(el("rec-amount")?.value || 0),
          currency: state.user?.currency || "MXN",
          frequency: frequency,
          nextDueDate: el("rec-next-due")?.value,
          endDate: endDate,
          categoryId: el("rec-category")?.value || null,
          paymentType: paymentType
        };
        
        if (!body.name || !body.amount) {
          showToast("Completa nombre y monto", "error");
          return;
        }
        
        await api.post("/recurring-payments", body);
        showToast("Pago recurrente guardado", "success");
        hideForm("recurring-form-wrap", "btn-add-recurring", "+ Nuevo");
        await loadRecurring();
      } catch (e) {
        showToast(e.message, "error");
      } finally {
        newSaveBtn.dataset.saving = "false";
      }
    });
  }
}

function wireDebtForm() {
  const addBtn = el("btn-add-debt");
  const cancelBtn = el("btn-cancel-debt");
  const saveBtn = el("btn-save-debt");
  const dueDate = el("debt-due-date");
  
  if (addBtn) {
    const newAddBtn = addBtn.cloneNode(true);
    addBtn.parentNode.replaceChild(newAddBtn, addBtn);
    newAddBtn.addEventListener("click", () => showInlineForm("debt-form-wrap", "btn-add-debt"));
  }
  
  if (cancelBtn) {
    const newCancelBtn = cancelBtn.cloneNode(true);
    cancelBtn.parentNode.replaceChild(newCancelBtn, cancelBtn);
    newCancelBtn.addEventListener("click", () => hideForm("debt-form-wrap", "btn-add-debt", "+ Nueva deuda"));
  }
  
  if (dueDate) dueDate.value = todayIso();
  
  if (saveBtn) {
    const newSaveBtn = saveBtn.cloneNode(true);
    saveBtn.parentNode.replaceChild(newSaveBtn, saveBtn);
    
    newSaveBtn.addEventListener("click", async () => {
      if (newSaveBtn.dataset.saving === "true") return;
      newSaveBtn.dataset.saving = "true";
      
      try {
        const body = {
          name: el("debt-name")?.value.trim(),
          principalBalance: Number(el("debt-remaining")?.value || 0),
          installment: Number(el("debt-min-payment")?.value || 0),
          frequency: "monthly",
          nextDueDate: el("debt-due-date")?.value,
          notes: el("debt-name")?.value,
        };
        if (!body.name) {
          showToast("Ingresa el nombre de la deuda", "error");
          return;
        }
        await api.post("/debts", body);
        showToast("Deuda registrada", "success");
        hideForm("debt-form-wrap", "btn-add-debt", "+ Nueva deuda");
        await loadDebts();
      } catch (e) {
        showToast(e.message, "error");
      } finally {
        newSaveBtn.dataset.saving = "false";
      }
    });
  }
}

function wireInstallmentForm() {
  const addBtn = el("btn-add-installment");
  const cancelBtn = el("btn-cancel-installment");
  const saveBtn = el("btn-save-installment");
  const dueDate = el("inst-due-date");
  const typeSelect = el("inst-type");
  const debtGroup = el("inst-debt-group");
  const accountGroup = el("inst-account-group");
  const numberGroup = el("inst-number-group");
  const amountGroup = el("inst-amount-group");
  const totalGroup = el("inst-total-group");
  const monthsGroup = el("inst-months-group");
  const interestGroup = el("inst-interest-group");
  const paidGroup = el("inst-paid-group");
  
  if (typeSelect && debtGroup && accountGroup) {
    typeSelect.addEventListener("change", () => {
      const isCreditCard = typeSelect.value === "credit_card";
      if (debtGroup) debtGroup.classList.toggle("hidden", isCreditCard);
      if (accountGroup) accountGroup.classList.toggle("hidden", !isCreditCard);
      if (numberGroup) numberGroup.classList.toggle("hidden", isCreditCard);
      if (amountGroup) amountGroup.classList.toggle("hidden", isCreditCard);
      if (totalGroup) totalGroup.classList.toggle("hidden", !isCreditCard);
      if (monthsGroup) monthsGroup.classList.toggle("hidden", !isCreditCard);
      if (interestGroup) interestGroup.classList.toggle("hidden", !isCreditCard);
      if (paidGroup) paidGroup.classList.toggle("hidden", isCreditCard);
    });
  }
  
  if (addBtn) {
    const newAddBtn = addBtn.cloneNode(true);
    addBtn.parentNode.replaceChild(newAddBtn, addBtn);
    newAddBtn.addEventListener("click", () => showInlineForm("installment-form-wrap", "btn-add-installment"));
  }
  
  if (cancelBtn) {
    const newCancelBtn = cancelBtn.cloneNode(true);
    cancelBtn.parentNode.replaceChild(newCancelBtn, cancelBtn);
    newCancelBtn.addEventListener("click", () => hideForm("installment-form-wrap", "btn-add-installment", "+ Nueva partialidad"));
  }
  
  if (dueDate) dueDate.value = todayIso();
  
  if (saveBtn) {
    const newSaveBtn = saveBtn.cloneNode(true);
    saveBtn.parentNode.replaceChild(newSaveBtn, saveBtn);
    
    newSaveBtn.addEventListener("click", async () => {
      if (newSaveBtn.dataset.saving === "true") return;
      newSaveBtn.dataset.saving = "true";
      
      try {
        const type = typeSelect?.value;
        let body;
        
        if (type === "credit_card") {
          const totalAmount = Number(el("inst-total-amount")?.value || 0);
          const months = parseInt(el("inst-months")?.value, 10);
          if (!months || months <= 0) {
            showToast("Ingresa el número de meses", "error");
            return;
          }
          const monthlyAmount = totalAmount / months;
          const interestRate = Number(el("inst-interest-rate")?.value || 0);
          const accountId = el("inst-account")?.value;
          
          if (!accountId) {
            showToast("Selecciona una tarjeta de crédito", "error");
            return;
          }
          
          body = {
            accountId: accountId,
            number: 1,
            amount: monthlyAmount,
            dueDate: dueDate?.value,
            paid: false,
            originalPurchaseAmount: totalAmount,
            interestRate: interestRate
          };
          
          await api.post("/installments/credit-card", body);
          showToast(`Compra a ${months} meses registrada`, "success");
        } else {
          body = {
            debtId: el("inst-debt")?.value,
            number: parseInt(el("inst-number")?.value, 10),
            amount: Number(el("inst-amount")?.value || 0),
            dueDate: dueDate?.value,
            paid: el("inst-paid")?.value === "true"
          };
          
          if (!body.debtId || !body.number || !body.amount) {
            showToast("Completa todos los campos", "error");
            return;
          }
          
          await api.post("/installments", body);
          showToast("Partialidad guardada", "success");
        }
        
        hideForm("installment-form-wrap", "btn-add-installment", "+ Nueva partialidad");
        document.querySelectorAll("#installment-form-wrap input, #installment-form-wrap select").forEach(i => i.value = "");
        if (dueDate) dueDate.value = todayIso();
        await loadInstallments();
        await loadDebts();
      } catch (e) {
        showToast(e.message, "error");
      } finally {
        newSaveBtn.dataset.saving = "false";
      }
    });
  }
}

function wireCategoryForm() {
  const addBtn = el("btn-add-category");
  const cancelBtn = el("btn-cancel-category");
  const saveBtn = el("btn-save-category");
  
  if (addBtn) {
    const newAddBtn = addBtn.cloneNode(true);
    addBtn.parentNode.replaceChild(newAddBtn, addBtn);
    newAddBtn.addEventListener("click", () => showInlineForm("category-form-wrap", "btn-add-category"));
  }
  
  if (cancelBtn) {
    const newCancelBtn = cancelBtn.cloneNode(true);
    cancelBtn.parentNode.replaceChild(newCancelBtn, cancelBtn);
    newCancelBtn.addEventListener("click", () => hideForm("category-form-wrap", "btn-add-category", "+ Nueva"));
  }
  
  if (saveBtn) {
    const newSaveBtn = saveBtn.cloneNode(true);
    saveBtn.parentNode.replaceChild(newSaveBtn, saveBtn);
    
    newSaveBtn.addEventListener("click", async () => {
      if (newSaveBtn.dataset.saving === "true") return;
      newSaveBtn.dataset.saving = "true";
      
      try {
        const body = {
          name: el("cat-name")?.value.trim(),
          type: el("cat-type")?.value,
          color: el("cat-color")?.value,
          icon: el("cat-icon")?.value.trim(),
        };
        if (!body.name) {
          showToast("Ingresa el nombre de la categoría", "error");
          return;
        }
        await api.post("/categories", body);
        showToast("Categoría creada", "success");
        hideForm("category-form-wrap", "btn-add-category", "+ Nueva");
        await loadCategories();
      } catch (e) {
        showToast(e.message, "error");
      } finally {
        newSaveBtn.dataset.saving = "false";
      }
    });
  }
}

function wireProfileForm() {
  const periodSelect = el("profile-period");
  const saveBtn = el("btn-save-profile");
  const exportBtn = el("btn-export-backup");
  const importInput = el("import-backup");
  
  if (periodSelect) periodSelect.addEventListener("change", () => toggleProfilePeriodFields(periodSelect.value));
  
  if (saveBtn) {
    const newSaveBtn = saveBtn.cloneNode(true);
    saveBtn.parentNode.replaceChild(newSaveBtn, saveBtn);
    
    newSaveBtn.addEventListener("click", async () => {
      if (newSaveBtn.dataset.saving === "true") return;
      newSaveBtn.dataset.saving = "true";
      
      try {
        const period = el("profile-period")?.value;
        let payDays = [];
        if (period === "biweekly") {
          const d1 = parseInt(el("profile-payday1")?.value, 10);
          const d2 = parseInt(el("profile-payday2")?.value, 10);
          if (!isNaN(d1)) payDays.push(d1);
          if (!isNaN(d2)) payDays.push(d2);
        } else {
          const d = parseInt(el("profile-payday-monthly")?.value, 10);
          if (!isNaN(d)) payDays.push(d);
        }
        
        const monthlyIncomeValue = parseFloat(el("profile-income")?.value || 0);
        
        const body = {
          displayName: state.user?.displayName || "",
          currency: el("profile-currency")?.value,
          payCycle: period,
          payDays: payDays,
          monthlyIncome: isNaN(monthlyIncomeValue) ? 0 : monthlyIncomeValue
        };
        
        await api.patch("/me", body);
        state.user = { ...(state.user || {}), ...body };
        showToast("Perfil actualizado", "success");
      } catch (e) {
        showToast(e.message, "error");
      } finally {
        newSaveBtn.dataset.saving = "false";
      }
    });
  }
  
  if (exportBtn) {
    const newExportBtn = exportBtn.cloneNode(true);
    exportBtn.parentNode.replaceChild(newExportBtn, exportBtn);
    
    newExportBtn.addEventListener("click", async () => {
      if (newExportBtn.dataset.saving === "true") return;
      newExportBtn.dataset.saving = "true";
      
      try {
        const data = await api.get("/backup/export");
        if (data.downloadUrl) {
          window.open(data.downloadUrl, '_blank');
        } else {
          showToast("No se pudo generar el respaldo", "error");
        }
        showToast("Respaldo exportado", "success");
      } catch (e) {
        showToast(e.message, "error");
      } finally {
        newExportBtn.dataset.saving = "false";
      }
    });
  }
  
  if (importInput) {
    const newImportInput = importInput.cloneNode(true);
    importInput.parentNode.replaceChild(newImportInput, importInput);
    
    newImportInput.addEventListener("change", async (e) => {
      const file = e.target.files?.[0];
      if (!file) return;
      try {
        const text = await file.text();
        const parsed = JSON.parse(text);
        await api.post("/backup/import", parsed);
        showToast("Respaldo importado exitosamente", "success");
        await loadSection(state.activeSection);
      } catch (err) {
        showToast("Error al importar el respaldo", "error");
      } finally {
        newImportInput.value = "";
      }
    });
  }
}

// ─── GOAL FORM ────────────────────────────────────────────────
function wireGoalForm() {
  const addBtn = el("btn-add-goal");
  const cancelBtn = el("btn-cancel-goal");
  const saveBtn = el("btn-save-goal");
  
  if (addBtn) {
    const newAddBtn = addBtn.cloneNode(true);
    addBtn.parentNode.replaceChild(newAddBtn, addBtn);
    newAddBtn.addEventListener("click", () => {
      const form = el("goal-form-wrap");
      if (form) {
        form.classList.toggle("hidden");
        newAddBtn.textContent = form.classList.contains("hidden") ? "+ Nueva meta" : "✕ Cancelar";
      }
    });
  }
  
  if (cancelBtn) {
    const newCancelBtn = cancelBtn.cloneNode(true);
    cancelBtn.parentNode.replaceChild(newCancelBtn, cancelBtn);
    newCancelBtn.addEventListener("click", () => {
      const form = el("goal-form-wrap");
      if (form) form.classList.add("hidden");
      const add = el("btn-add-goal");
      if (add) add.textContent = "+ Nueva meta";
      document.querySelectorAll("#goal-form-wrap input, #goal-form-wrap select").forEach(i => i.value = "");
    });
  }
  
  if (saveBtn) {
    const newSaveBtn = saveBtn.cloneNode(true);
    saveBtn.parentNode.replaceChild(newSaveBtn, saveBtn);
    
    newSaveBtn.addEventListener("click", async () => {
      if (newSaveBtn.dataset.saving === "true") return;
      newSaveBtn.dataset.saving = "true";
      
      try {
        const body = {
          name: el("goal-name")?.value.trim(),
          targetAmount: Number(el("goal-target")?.value || 0),
          currentProgress: Number(el("goal-progress")?.value || 0),
          targetDate: el("goal-date")?.value || null,
          status: el("goal-status")?.value || "active"
        };
        
        if (!body.name || !body.targetAmount) {
          showToast("Completa nombre y monto objetivo", "error");
          return;
        }
        
        if (body.currentProgress > body.targetAmount) {
          showToast("El progreso no puede exceder la meta", "error");
          return;
        }
        
        await api.post("/financial-goals", body);
        showToast("Meta guardada", "success");
        
        const form = el("goal-form-wrap");
        if (form) form.classList.add("hidden");
        const add = el("btn-add-goal");
        if (add) add.textContent = "+ Nueva meta";
        
        document.querySelectorAll("#goal-form-wrap input, #goal-form-wrap select").forEach(i => i.value = "");
        await loadGoals();
      } catch (e) {
        showToast(e.message, "error");
      } finally {
        newSaveBtn.dataset.saving = "false";
      }
    });
  }
}

// ─── BUDGET FORM ──────────────────────────────────────────────
function wireBudgetForm() {
  const addBtn = el("btn-add-budget");
  const cancelBtn = el("btn-cancel-budget");
  const saveBtn = el("btn-save-budget");
  
  if (addBtn) {
    const newAddBtn = addBtn.cloneNode(true);
    addBtn.parentNode.replaceChild(newAddBtn, addBtn);
    newAddBtn.addEventListener("click", () => {
      const form = el("budget-form-wrap");
      if (form) {
        form.classList.toggle("hidden");
        newAddBtn.textContent = form.classList.contains("hidden") ? "+ Nuevo presupuesto" : "✕ Cancelar";
      }
      populateCategorySelect("budget-category", state.categories);
    });
  }
  
  if (cancelBtn) {
    const newCancelBtn = cancelBtn.cloneNode(true);
    cancelBtn.parentNode.replaceChild(newCancelBtn, cancelBtn);
    newCancelBtn.addEventListener("click", () => {
      const form = el("budget-form-wrap");
      if (form) form.classList.add("hidden");
      const add = el("btn-add-budget");
      if (add) add.textContent = "+ Nuevo presupuesto";
    });
  }
  
  if (saveBtn) {
    const newSaveBtn = saveBtn.cloneNode(true);
    saveBtn.parentNode.replaceChild(newSaveBtn, saveBtn);
    
    newSaveBtn.addEventListener("click", async () => {
      if (newSaveBtn.dataset.saving === "true") return;
      newSaveBtn.dataset.saving = "true";
      
      try {
        const alertValue = Number(el("budget-alert")?.value || 80);
        const body = {
          categoryId: el("budget-category")?.value,
          period: "monthly",
          periodStart: el("budget-start")?.value,
          periodEnd: el("budget-end")?.value,
          amountLimit: Number(el("budget-limit")?.value || 0),
          alertThreshold: alertValue / 100
        };
        if (!body.categoryId || !body.amountLimit) {
          showToast("Completa categoría y monto límite", "error");
          return;
        }
        await api.post("/budgets", body);
        showToast("Presupuesto guardado", "success");
        
        const form = el("budget-form-wrap");
        if (form) form.classList.add("hidden");
        const add = el("btn-add-budget");
        if (add) add.textContent = "+ Nuevo presupuesto";
        
        document.querySelectorAll("#budget-form-wrap input, #budget-form-wrap select").forEach(i => i.value = "");
        await loadBudgets();
      } catch (e) {
        showToast(e.message, "error");
      } finally {
        newSaveBtn.dataset.saving = "false";
      }
    });
  }
}

// ─── BUDGETS ────────────────────────────────────────────────
async function loadBudgets() {
  setLoading(true);
  try {
    const [budgets, categories] = await Promise.all([
      api.get("/budgets"),
      api.get("/categories"),
    ]);
    state.budgets = budgets || [];
    state.categories = categories || [];
    renderBudgets();
    populateCategorySelect("budget-category", state.categories);
  } catch (error) {
    console.error("Error loading budgets:", error);
  } finally {
    setLoading(false);
  }
}

function renderBudgets() {
  const c = el("budgets-list");
  if (!c) return;
  const cur = state.user?.currency || "MXN";
  if (!state.budgets || state.budgets.length === 0) {
    c.innerHTML = `<div class="empty-state"><div class="empty-state-icon">💰</div>Sin presupuestos activos</div>`;
    return;
  }
  c.innerHTML = state.budgets.map(b => {
    const percent = b.usagePercentage || 0;
    let fillClass = "normal";
    if (percent >= 90) fillClass = "danger";
    else if (percent >= 75) fillClass = "warning";
    return `
      <div class="budget-item ${b.isAlert ? 'alert' : ''}">
        <div class="budget-header">
          <span class="budget-category">${b.categoryName}</span>
          <span class="budget-amount">${fmt(b.spentAmount, cur)} / ${fmt(b.amountLimit, cur)}</span>
        </div>
        <div class="budget-bar"><div class="budget-fill ${fillClass}" style="width:${percent}%"></div></div>
        <div style="display:flex;justify-content:space-between;margin-top:0.5rem">
          <span style="font-size:0.75rem">${percent}% usado</span>
          ${b.isAlert ? '<span style="color:var(--red);font-size:0.75rem">⚠️ Alerta de presupuesto</span>' : ''}
        </div>
        <div class="account-actions" style="margin-top:1rem">
          <button class="btn-edit-sm" data-action="edit-budget" data-id="${b.id}">Editar</button>
          <button class="btn-danger-sm" data-action="del-budget" data-id="${b.id}">Eliminar</button>
        </div>
      </div>
    `;
  }).join("");
}

// ─── FINANCIAL GOALS ─────────────────────────────────────────
async function loadGoals() {
  setLoading(true);
  try {
    const goals = await api.get("/financial-goals");
    state.goals = goals || [];
    renderGoals();
  } catch (error) {
    console.error("Error loading goals:", error);
  } finally {
    setLoading(false);
  }
}

function renderGoals() {
  const c = el("goals-list");
  if (!c) return;
  const cur = state.user?.currency || "MXN";
  if (!state.goals || state.goals.length === 0) {
    c.innerHTML = `<div class="empty-state"><div class="empty-state-icon">🎯</div>Sin metas registradas</div>`;
    return;
  }
  c.innerHTML = state.goals.map(goal => {
    const progressPercent = goal.progressPercentage || 0;
    const isAchieved = goal.status === 'achieved' || goal.currentProgress >= goal.targetAmount;
    return `
      <div class="goal-card">
        <div class="goal-name">${goal.name}</div>
        <div class="goal-amount">${fmt(goal.currentProgress, cur)} / ${fmt(goal.targetAmount, cur)}</div>
        <div class="goal-progress-bar"><div class="goal-progress-fill" style="width:${Math.min(progressPercent, 100)}%"></div></div>
        <div class="goal-date">
          ${goal.targetDate ? `Meta: ${new Date(goal.targetDate).toLocaleDateString()}` : 'Sin fecha límite'}
        </div>
        <div><span class="goal-status status-${goal.status === 'active' ? 'active' : 'paused'}">${goal.status === 'active' ? 'Activa' : 'Pausada'}</span></div>
        <div class="account-actions" style="margin-top:1rem; gap:0.5rem; flex-wrap:wrap">
          ${!isAchieved && goal.status === 'active' ? `<button class="btn-success-sm" data-action="add-progress-goal" data-id="${goal.id}">+ Agregar progreso</button>` : ''}
          <button class="btn-edit-sm" data-action="edit-goal" data-id="${goal.id}">Editar</button>
          <button class="btn-danger-sm" data-action="del-goal" data-id="${goal.id}">Eliminar</button>
        </div>
        ${isAchieved ? '<div style="margin-top:0.5rem;color:var(--green);font-size:0.8rem">🎉 Meta alcanzada</div>' : ''}
      </div>
    `;
  }).join("");
}

function buildAddProgressModal(goal) {
  const cur = state.user?.currency || "MXN";
  openModal(`Agregar progreso a "${goal.name}"`, `
    <div class="form-grid">
      <div class="field-group field-full">
        <label class="field-label">Monto a agregar</label>
        <input id="progress-amount" class="field-input" type="number" step="0.01" placeholder="0.00" />
        <small class="text-muted" style="margin-top:0.5rem">Progreso actual: ${fmt(goal.currentProgress, cur)}<br>
        Meta: ${fmt(goal.targetAmount, cur)}</small>
      </div>
    </div>
  `, async () => {
    const amount = Number(el("progress-amount")?.value || 0);
    if (amount <= 0) {
      showToast("Ingresa un monto válido", "error");
      return;
    }
    
    const newProgress = goal.currentProgress + amount;
    if (newProgress > goal.targetAmount) {
      showToast(`El progreso no puede exceder la meta de ${fmt(goal.targetAmount, cur)}`, "error");
      return;
    }
    
    await api.patch(`/financial-goals/${goal.id}`, {
      name: goal.name,
      targetAmount: goal.targetAmount,
      currentProgress: newProgress,
      targetDate: goal.targetDate,
      status: newProgress >= goal.targetAmount ? "achieved" : goal.status
    });
    
    showToast(`¡Progreso actualizado! Ahora tienes ${fmt(newProgress, cur)}`, "success");
    closeModal();
    await loadGoals();
  });
}

// ─── REPORTS ────────────────────────────────────────────────
async function loadReports() {
  setLoading(true);
  try {
    const year = el("report-year")?.value || new Date().getFullYear();
    const reports = await api.get(`/reports/monthly?year=${year}`);
    state.reports = reports || [];
    renderReports();
  } catch (error) {
    console.error("Error loading reports:", error);
  } finally {
    setLoading(false);
  }
}

function renderReports() {
  const c = el("reports-list");
  if (!c) return;
  const cur = state.user?.currency || "MXN";
  if (!state.reports || state.reports.length === 0) {
    c.innerHTML = `<div class="empty-state"><div class="empty-state-icon">📊</div>Sin datos para mostrar</div>`;
    return;
  }
  c.innerHTML = state.reports.map(report => `
    <div class="report-card">
      <div class="report-month">${report.yearMonth}</div>
      <div class="report-stats">
        <div class="report-stat report-stat-income">
          <div class="report-stat-label">Ingresos</div>
          <div class="report-stat-value">${fmt(report.totalIncome, cur)}</div>
        </div>
        <div class="report-stat report-stat-expense">
          <div class="report-stat-label">Gastos</div>
          <div class="report-stat-value">${fmt(report.totalExpenses, cur)}</div>
        </div>
        <div class="report-stat report-stat-savings">
          <div class="report-stat-label">Ahorro</div>
          <div class="report-stat-value">${fmt(report.totalSavings, cur)}</div>
        </div>
      </div>
      ${report.topExpenses && report.topExpenses.length > 0 ? `
        <div style="margin-top:1rem">
          <div style="font-size:0.75rem;color:var(--text-muted);margin-bottom:0.5rem">Top gastos</div>
          <div class="bars">
            ${report.topExpenses.slice(0, 3).map(cat => `
              <div class="bar-row">
                <div class="bar-meta"><span>${cat.categoryName}</span><span>${fmt(cat.amount, cur)} (${cat.percentage}%)</span></div>
                <div class="bar-track"><div class="bar-fill" style="width:${cat.percentage}%"></div></div>
              </div>
            `).join("")}
          </div>
        </div>
      ` : ''}
    </div>
  `).join("");
}

// ─── MODAL BUILDERS ─────────────────────────────────────────
function buildEditInstallmentModal(inst) {
  const cur = state.user?.currency || "MXN";
  const debtOptions = buildSelectOptions(
    state.debts,
    "Seleccionar deuda",
    inst.debtId,
    (d) => d.id,
    (d) => `${d.name} - ${fmt(d.principalBalance || 0, cur)}`
  );

  openModal("Editar parcialidad", `
    <div class="form-grid">
      <div class="field-group field-full">
        <label class="field-label">Deuda</label>
        <select id="m-inst-debt" class="field-input">${debtOptions}</select>
      </div>
      <div class="field-group">
        <label class="field-label">Número</label>
        <input id="m-inst-number" class="field-input" type="number" min="1" value="${escapeHtml(inst.number ?? "")}" />
      </div>
      <div class="field-group">
        <label class="field-label">Monto</label>
        <input id="m-inst-amount" class="field-input" type="number" step="0.01" value="${escapeHtml(inst.amount ?? "")}" />
      </div>
      <div class="field-group">
        <label class="field-label">Fecha de vencimiento</label>
        <input id="m-inst-due-date" class="field-input" type="date" value="${escapeHtml(toDateInputValue(inst.dueDate))}" />
      </div>
      <div class="field-group">
        <label class="field-label">Pagada</label>
        <select id="m-inst-paid" class="field-input">
          <option value="false" ${!inst.paid ? "selected" : ""}>No</option>
          <option value="true" ${inst.paid ? "selected" : ""}>Sí</option>
        </select>
      </div>
    </div>
  `, async () => {
    const debtId = el("m-inst-debt")?.value;
    const number = parseInt(el("m-inst-number")?.value, 10);
    const amount = Number(el("m-inst-amount")?.value || 0);
    const dueDate = el("m-inst-due-date")?.value;

    if (!debtId) {
      showToast("Selecciona una deuda", "error");
      return;
    }
    if (!number || !amount || !dueDate) {
      showToast("Completa número, monto y fecha", "error");
      return;
    }

    await api.patch(`/installments/${inst.id}`, {
      debtId,
      number,
      amount,
      dueDate,
      paid: el("m-inst-paid")?.value === "true",
      paymentMovementId: inst.paymentMovementId || null,
    });
    showToast("Parcialidad actualizada", "success");
    closeModal();
    await loadInstallments();
    await loadDebts();
  });
}

function buildEditTransactionModal(tx) {
  const cur = state.user?.currency || "MXN";
  const typeLabels = {
    expense: "Gasto",
    income: "Ingreso",
    transfer: "Transferencia",
    payment: "Pago",
    adjustment: "Ajuste",
  };
  const typeOptions = Object.keys(typeLabels).map(type => `
    <option value="${type}" ${tx.type === type ? "selected" : ""}>${typeLabels[type]}</option>
  `).join("");
  const categoryOptions = buildSelectOptions(
    state.categories,
    "Sin categoría",
    tx.categoryId,
    (c) => c.id,
    (c) => `${c.icon || ""} ${c.name}`.trim()
  );
  const accountOptions = buildSelectOptions(
    state.accounts,
    "Seleccionar cuenta",
    tx.accountId,
    (a) => a.id,
    (a) => a.name
  );
  const transferAccountOptions = buildSelectOptions(
    state.accounts,
    "Seleccionar cuenta destino",
    tx.transferAccountId,
    (a) => a.id,
    (a) => a.name
  );

  openModal("Editar transacción", `
    <div class="form-grid">
      <div class="field-group">
        <label class="field-label">Concepto</label>
        <input id="m-tx-name" class="field-input" type="text" value="${escapeHtml(tx.description || tx.name || "")}" />
      </div>
      <div class="field-group">
        <label class="field-label">Monto</label>
        <input id="m-tx-amount" class="field-input" type="number" step="0.01" value="${escapeHtml(tx.amount ?? 0)}" />
      </div>
      <div class="field-group">
        <label class="field-label">Tipo</label>
        <select id="m-tx-type" class="field-input">${typeOptions}</select>
      </div>
      <div class="field-group">
        <label class="field-label">Categoría</label>
        <select id="m-tx-category" class="field-input">${categoryOptions}</select>
      </div>
      <div class="field-group">
        <label class="field-label">Cuenta</label>
        <select id="m-tx-account" class="field-input">${accountOptions}</select>
      </div>
      <div id="m-transfer-group" class="field-group hidden">
        <label class="field-label">Cuenta destino</label>
        <select id="m-tx-transfer-account" class="field-input">${transferAccountOptions}</select>
      </div>
      <div class="field-group">
        <label class="field-label">Fecha</label>
        <input id="m-tx-date" class="field-input" type="date" value="${escapeHtml(toDateInputValue(tx.transactionDate))}" />
      </div>
      <div class="field-group field-full">
        <label class="field-label">Nota</label>
        <input id="m-tx-note" class="field-input" type="text" value="${escapeHtml(tx.notes || "")}" />
      </div>
    </div>
  `, async () => {
    const type = el("m-tx-type")?.value;
    const accountId = el("m-tx-account")?.value;
    const transferAccountId = el("m-tx-transfer-account")?.value;
    const amount = Number(el("m-tx-amount")?.value || 0);
    const transactionDate = el("m-tx-date")?.value;
    const description = el("m-tx-name")?.value.trim();
    const categoryId = el("m-tx-category")?.value || null;
    const notes = el("m-tx-note")?.value.trim() || null;

    if (!accountId) {
      showToast("Selecciona una cuenta", "error");
      return;
    }
    if (!amount || !transactionDate || !description) {
      showToast("Completa concepto, monto y fecha", "error");
      return;
    }
    if (type === "transfer") {
      if (!transferAccountId) {
        showToast("Selecciona la cuenta destino", "error");
        return;
      }
      if (transferAccountId === accountId) {
        showToast("La cuenta origen y destino no pueden ser iguales", "error");
        return;
      }
    }

    await api.patch(`/transactions/${tx.id}`, {
      accountId,
      transferAccountId: type === "transfer" ? transferAccountId : null,
      categoryId,
      debtId: null,
      type,
      description,
      amount,
      currency: tx.currency || cur,
      transactionDate,
      notes,
    });
    showToast("Transacción actualizada", "success");
    closeModal();
    await loadTransactions();
  });

  const typeSelect = el("m-tx-type");
  const transferGroup = el("m-transfer-group");
  const syncTransfer = () => {
    if (transferGroup && typeSelect) {
      transferGroup.classList.toggle("hidden", typeSelect.value !== "transfer");
    }
  };
  if (typeSelect) typeSelect.addEventListener("change", syncTransfer);
  syncTransfer();
}

function buildEditAccountModal(acc) {
  const cur = state.user?.currency || "MXN";
  const modal = el("edit-modal");
  const type = acc.type || "debit";

  openModal("Editar cuenta", `
    <div class="form-grid">
      <div class="field-group field-full">
        <label class="field-label">Institución</label>
        <input id="m-acc-institution" class="field-input" type="text" value="${escapeHtml(acc.institution || "")}" />
      </div>
      <div class="field-group">
        <label class="field-label">Nombre</label>
        <input id="m-acc-name" class="field-input" type="text" value="${escapeHtml(acc.name || "")}" />
      </div>
      <div class="field-group">
        <label class="field-label">Tipo</label>
        <select id="m-acc-type" class="field-input">
          <option value="debit" ${type === "debit" ? "selected" : ""}>Débito / Cheques</option>
          <option value="credit" ${type === "credit" ? "selected" : ""}>Tarjeta de crédito</option>
          <option value="savings" ${type === "savings" ? "selected" : ""}>Ahorro</option>
          <option value="loan" ${type === "loan" ? "selected" : ""}>Préstamo</option>
          <option value="cash" ${type === "cash" ? "selected" : ""}>Efectivo</option>
          <option value="investment" ${type === "investment" ? "selected" : ""}>Inversión</option>
        </select>
      </div>
      <div class="field-group">
        <label class="field-label">Moneda</label>
        <select id="m-acc-currency" class="field-input">
          <option value="MXN" ${String(acc.currency || cur) === "MXN" ? "selected" : ""}>MXN</option>
          <option value="USD" ${String(acc.currency || cur) === "USD" ? "selected" : ""}>USD</option>
        </select>
      </div>
      <div class="field-group">
        <label class="field-label">Saldo</label>
        <input id="m-acc-balance" class="field-input" type="number" step="0.01" value="${escapeHtml(acc.balance ?? 0)}" />
      </div>
      <div class="field-group">
        <label class="field-label">Activa</label>
        <select id="m-acc-active" class="field-input">
          <option value="true" ${acc.active !== false ? "selected" : ""}>Sí</option>
          <option value="false" ${acc.active === false ? "selected" : ""}>No</option>
        </select>
      </div>
      <div class="field-group credit-only hidden">
        <label class="field-label">Límite de crédito</label>
        <input id="m-acc-limit" class="field-input" type="number" step="0.01" value="${escapeHtml(acc.creditLimit ?? "")}" />
      </div>
      <div class="field-group credit-only hidden">
        <label class="field-label">Día de corte</label>
        <input id="m-acc-cut-day" class="field-input" type="number" min="1" max="31" value="${escapeHtml(acc.closingDay ?? "")}" />
      </div>
      <div class="field-group credit-only hidden">
        <label class="field-label">Día de pago</label>
        <input id="m-acc-due-day" class="field-input" type="number" min="1" max="31" value="${escapeHtml(acc.dueDay ?? "")}" />
      </div>
    </div>
  `, async () => {
    const accountType = el("m-acc-type")?.value;
    const body = {
      type: accountType,
      name: el("m-acc-name")?.value.trim(),
      institution: el("m-acc-institution")?.value.trim() || "",
      currency: el("m-acc-currency")?.value || cur,
      balance: Number(el("m-acc-balance")?.value || 0),
      creditLimit: accountType === "credit" ? Number(el("m-acc-limit")?.value || 0) : null,
      closingDay: accountType === "credit" ? Number(el("m-acc-cut-day")?.value || 0) : null,
      dueDay: accountType === "credit" ? Number(el("m-acc-due-day")?.value || 0) : null,
      active: el("m-acc-active")?.value === "true",
    };
    if (!body.name) {
      showToast("Ingresa el nombre de la cuenta", "error");
      return;
    }
    await api.patch(`/accounts/${acc.id}`, body);
    showToast("Cuenta actualizada", "success");
    closeModal();
    await loadAccounts();
  });

  const typeSelect = el("m-acc-type");
  if (typeSelect) {
    const sync = () => syncCreditOnlyFields(modal, typeSelect.value);
    typeSelect.addEventListener("change", sync);
    sync();
  }
}

function buildEditRecurringModal(rec) {
  const cur = state.user?.currency || "MXN";
  const categoryOptions = buildSelectOptions(
    state.categories,
    "Sin categoría",
    rec.categoryId,
    (c) => c.id,
    (c) => `${c.icon || ""} ${c.name}`.trim()
  );
  const frequencyOptions = [
    ["weekly", "Semanal"],
    ["biweekly", "Quincenal"],
    ["monthly", "Mensual"],
    ["quarterly", "Trimestral"],
    ["yearly", "Anual"],
    ["custom", "Personalizado"],
  ].map(([value, label]) => `<option value="${value}" ${String(rec.frequency || "").toLowerCase() === value ? "selected" : ""}>${label}</option>`).join("");
  const paymentTypeOptions = [
    ["expense", "Gasto"],
    ["income", "Ingreso"],
  ].map(([value, label]) => `<option value="${value}" ${rec.paymentType === value ? "selected" : ""}>${label}</option>`).join("");

  openModal("Editar pago recurrente", `
    <div class="form-grid">
      <div class="field-group">
        <label class="field-label">Nombre</label>
        <input id="m-rec-name" class="field-input" type="text" value="${escapeHtml(rec.name || "")}" />
      </div>
      <div class="field-group">
        <label class="field-label">Monto</label>
        <input id="m-rec-amount" class="field-input" type="number" step="0.01" value="${escapeHtml(rec.amount ?? 0)}" />
      </div>
      <div class="field-group">
        <label class="field-label">Moneda</label>
        <select id="m-rec-currency" class="field-input">
          <option value="MXN" ${String(rec.currency || cur) === "MXN" ? "selected" : ""}>MXN</option>
          <option value="USD" ${String(rec.currency || cur) === "USD" ? "selected" : ""}>USD</option>
        </select>
      </div>
      <div class="field-group">
        <label class="field-label">Tipo</label>
        <select id="m-rec-payment-type" class="field-input">${paymentTypeOptions}</select>
      </div>
      <div class="field-group">
        <label class="field-label">Frecuencia</label>
        <select id="m-rec-frequency" class="field-input">${frequencyOptions}</select>
      </div>
      <div class="field-group">
        <label class="field-label">Próximo vencimiento</label>
        <input id="m-rec-next-due" class="field-input" type="date" value="${escapeHtml(toDateInputValue(rec.nextDueDate))}" />
      </div>
      <div id="m-rec-end-date-group" class="field-group">
        <label class="field-label">Fecha final (opcional)</label>
        <input id="m-rec-end-date" class="field-input" type="date" value="${escapeHtml(toDateInputValue(rec.endDate))}" />
      </div>
      <div class="field-group field-full">
        <label class="field-label">Categoría</label>
        <select id="m-rec-category" class="field-input">${categoryOptions}</select>
      </div>
    </div>
  `, async () => {
    const body = {
      name: el("m-rec-name")?.value.trim(),
      amount: Number(el("m-rec-amount")?.value || 0),
      currency: el("m-rec-currency")?.value || cur,
      paymentType: el("m-rec-payment-type")?.value || "expense",
      frequency: el("m-rec-frequency")?.value,
      nextDueDate: el("m-rec-next-due")?.value,
      endDate: el("m-rec-end-date")?.value || null,
      categoryId: el("m-rec-category")?.value || null,
    };
    if (!body.name || !body.amount || !body.frequency || !body.nextDueDate) {
      showToast("Completa nombre, monto, frecuencia y fecha", "error");
      return;
    }
    await api.patch(`/recurring-payments/${rec.id}`, body);
    showToast("Pago recurrente actualizado", "success");
    closeModal();
    await loadRecurring();
  });
  
  const typeSelect = el("m-rec-payment-type");
  const endDateGroup = document.getElementById("m-rec-end-date-group");
  if (typeSelect && endDateGroup) {
    typeSelect.addEventListener("change", () => {
      endDateGroup.classList.toggle("hidden", typeSelect.value === "expense");
    });
  }
}

function buildEditDebtModal(debt) {
  const frequencyOptions = [
    ["weekly", "Semanal"],
    ["biweekly", "Quincenal"],
    ["monthly", "Mensual"],
    ["quarterly", "Trimestral"],
    ["yearly", "Anual"],
    ["custom", "Personalizado"],
  ].map(([value, label]) => `<option value="${value}" ${String(debt.frequency || "").toLowerCase() === value ? "selected" : ""}>${label}</option>`).join("");

  openModal("Editar deuda", `
    <div class="form-grid">
      <div class="field-group field-full">
        <label class="field-label">Nombre</label>
        <input id="m-debt-name" class="field-input" type="text" value="${escapeHtml(debt.name || "")}" />
      </div>
      <div class="field-group">
        <label class="field-label">Saldo total</label>
        <input id="m-debt-principal" class="field-input" type="number" step="0.01" value="${escapeHtml(debt.principalBalance ?? 0)}" />
      </div>
      <div class="field-group">
        <label class="field-label">Pago mínimo</label>
        <input id="m-debt-installment" class="field-input" type="number" step="0.01" value="${escapeHtml(debt.installment ?? "")}" />
      </div>
      <div class="field-group">
        <label class="field-label">Frecuencia</label>
        <select id="m-debt-frequency" class="field-input">${frequencyOptions}</select>
      </div>
      <div class="field-group">
        <label class="field-label">Próximo pago</label>
        <input id="m-debt-due-date" class="field-input" type="date" value="${escapeHtml(toDateInputValue(debt.nextDueDate))}" />
      </div>
      <div class="field-group field-full">
        <label class="field-label">Notas</label>
        <input id="m-debt-notes" class="field-input" type="text" value="${escapeHtml(debt.notes || "")}" />
      </div>
    </div>
  `, async () => {
    const body = {
      name: el("m-debt-name")?.value.trim(),
      principalBalance: Number(el("m-debt-principal")?.value || 0),
      installment: Number(el("m-debt-installment")?.value || 0),
      frequency: el("m-debt-frequency")?.value || "monthly",
      nextDueDate: el("m-debt-due-date")?.value || null,
      notes: el("m-debt-notes")?.value.trim() || null,
    };
    if (!body.name || !body.principalBalance || !body.frequency) {
      showToast("Completa nombre, saldo y frecuencia", "error");
      return;
    }
    await api.patch(`/debts/${debt.id}`, body);
    showToast("Deuda actualizada", "success");
    closeModal();
    await loadDebts();
    await loadInstallments();
  });
}

function buildEditGoalModal(goal) {
  openModal("Editar meta", `
    <div class="form-grid">
      <div class="field-group field-full">
        <label class="field-label">Nombre</label>
        <input id="m-goal-name" class="field-input" type="text" value="${escapeHtml(goal.name || "")}" />
      </div>
      <div class="field-group">
        <label class="field-label">Monto objetivo</label>
        <input id="m-goal-target" class="field-input" type="number" step="0.01" value="${escapeHtml(goal.targetAmount ?? 0)}" />
      </div>
      <div class="field-group">
        <label class="field-label">Progreso actual</label>
        <input id="m-goal-progress" class="field-input" type="number" step="0.01" value="${escapeHtml(goal.currentProgress ?? 0)}" />
      </div>
      <div class="field-group">
        <label class="field-label">Fecha objetivo</label>
        <input id="m-goal-date" class="field-input" type="date" value="${escapeHtml(toDateInputValue(goal.targetDate))}" />
      </div>
      <div class="field-group">
        <label class="field-label">Estado</label>
        <select id="m-goal-status" class="field-input">
          <option value="active" ${goal.status === "active" ? "selected" : ""}>Activa</option>
          <option value="paused" ${goal.status === "paused" ? "selected" : ""}>Pausada</option>
          <option value="achieved" ${goal.status === "achieved" ? "selected" : ""}>Alcanzada</option>
          <option value="cancelled" ${goal.status === "cancelled" ? "selected" : ""}>Cancelada</option>
        </select>
      </div>
    </div>
  `, async () => {
    const body = {
      name: el("m-goal-name")?.value.trim(),
      targetAmount: Number(el("m-goal-target")?.value || 0),
      currentProgress: Number(el("m-goal-progress")?.value || 0),
      targetDate: el("m-goal-date")?.value || null,
      status: el("m-goal-status")?.value || "active",
    };
    if (!body.name || !body.targetAmount) {
      showToast("Completa nombre y monto objetivo", "error");
      return;
    }
    await api.patch(`/financial-goals/${goal.id}`, body);
    showToast("Meta actualizada", "success");
    closeModal();
    await loadGoals();
  });
}

function buildEditBudgetModal(budget) {
  const cur = state.user?.currency || "MXN";
  const alertPercentage = budget.alertThreshold == null
    ? 80
    : Math.round(Number(budget.alertThreshold) * 100);
  const categoryOptions = buildSelectOptions(
    state.categories,
    "Seleccionar categoría",
    budget.categoryId,
    (c) => c.id,
    (c) => `${c.icon || ""} ${c.name}`.trim()
  );

  openModal("Editar presupuesto", `
    <div class="form-grid">
      <div class="field-group field-full">
        <label class="field-label">Categoría</label>
        <select id="m-budget-category" class="field-input">${categoryOptions}</select>
      </div>
      <div class="field-group">
        <label class="field-label">Periodo</label>
        <select id="m-budget-period" class="field-input">
          <option value="monthly" ${budget.period === "monthly" ? "selected" : ""}>Mensual</option>
          <option value="biweekly" ${budget.period === "biweekly" ? "selected" : ""}>Quincenal</option>
        </select>
      </div>
      <div class="field-group">
        <label class="field-label">Monto límite</label>
        <input id="m-budget-limit" class="field-input" type="number" step="0.01" value="${escapeHtml(budget.amountLimit ?? 0)}" />
      </div>
      <div class="field-group">
        <label class="field-label">Alerta (%)</label>
        <input id="m-budget-alert" class="field-input" type="number" min="0" max="100" step="1" value="${escapeHtml(alertPercentage)}" />
      </div>
      <div class="field-group">
        <label class="field-label">Periodo inicio</label>
        <input id="m-budget-start" class="field-input" type="date" value="${escapeHtml(toDateInputValue(budget.periodStart))}" />
      </div>
      <div class="field-group">
        <label class="field-label">Periodo fin</label>
        <input id="m-budget-end" class="field-input" type="date" value="${escapeHtml(toDateInputValue(budget.periodEnd))}" />
      </div>
    </div>
  `, async () => {
    const alertValue = Number(el("m-budget-alert")?.value || 80);
    const body = {
      categoryId: el("m-budget-category")?.value,
      period: el("m-budget-period")?.value || "monthly",
      periodStart: el("m-budget-start")?.value,
      periodEnd: el("m-budget-end")?.value,
      amountLimit: Number(el("m-budget-limit")?.value || 0),
      alertThreshold: alertValue / 100,
    };
    if (!body.categoryId || !body.amountLimit || !body.periodStart || !body.periodEnd) {
      showToast("Completa categoría, monto y rango de fechas", "error");
      return;
    }
    await api.patch(`/budgets/${budget.id}`, body);
    showToast("Presupuesto actualizado", "success");
    closeModal();
    await loadBudgets();
  });
}

// ─── AUTH VIEWS ────────────────────────────────────────────
function showAuth() {
  const authView = el("auth-view");
  const appView = el("app-view");
  if (authView) authView.classList.remove("hidden");
  if (appView) appView.classList.add("hidden");
  
  const loginEmail = el("login-email");
  const loginPassword = el("login-password");
  if (loginEmail) loginEmail.value = "";
  if (loginPassword) loginPassword.value = "";
}

function showApp() {
  const authView = el("auth-view");
  const appView = el("app-view");
  if (authView) authView.classList.add("hidden");
  if (appView) appView.classList.remove("hidden");
}

function wireAuth() {
  const goRegister = el("go-register");
  const goLogin = el("go-login");
  const loginBtn = el("btn-login");
  const registerBtn = el("btn-register");
  const viewLogin = el("view-login");
  const viewRegister = el("view-register");
  
  if (goRegister) goRegister.addEventListener("click", () => {
    if (viewLogin) viewLogin.classList.add("hidden");
    if (viewRegister) viewRegister.classList.remove("hidden");
  });
  
  if (goLogin) goLogin.addEventListener("click", () => {
    if (viewRegister) viewRegister.classList.add("hidden");
    if (viewLogin) viewLogin.classList.remove("hidden");
  });
  
  if (loginBtn) {
    const newLoginBtn = loginBtn.cloneNode(true);
    loginBtn.parentNode.replaceChild(newLoginBtn, loginBtn);
    
    newLoginBtn.addEventListener("click", async () => {
      if (newLoginBtn.dataset.saving === "true") return;
      newLoginBtn.dataset.saving = "true";
      
      const email = el("login-email")?.value.trim();
      const password = el("login-password")?.value;
      const errorEl = el("login-error");
      
      if (errorEl) errorEl.classList.add("hidden");
      if (!email || !password) {
        if (errorEl) {
          errorEl.textContent = "Completa correo y contraseña";
          errorEl.classList.remove("hidden");
        }
        newLoginBtn.dataset.saving = "false";
        return;
      }
      try {
        setLoading(true);
        await signIn(email, password);
        await loadAppAfterLogin();
      } catch (err) {
        console.error("Login error:", err);
        if (errorEl) {
          errorEl.textContent = err.message || "Credenciales incorrectas";
          errorEl.classList.remove("hidden");
        }
      } finally {
        setLoading(false);
        newLoginBtn.dataset.saving = "false";
      }
    });
  }
  
  if (registerBtn) {
    const newRegisterBtn = registerBtn.cloneNode(true);
    registerBtn.parentNode.replaceChild(newRegisterBtn, registerBtn);
    
    newRegisterBtn.addEventListener("click", async () => {
      if (newRegisterBtn.dataset.saving === "true") return;
      newRegisterBtn.dataset.saving = "true";
      
      const email = el("reg-email")?.value.trim();
      const displayName = el("reg-username")?.value.trim();
      const password = el("reg-password")?.value;
      const errorEl = el("reg-error");
      
      if (errorEl) errorEl.classList.add("hidden");
      if (!displayName || !email || !password) {
        if (errorEl) {
          errorEl.textContent = "Completa todos los campos";
          errorEl.classList.remove("hidden");
        }
        newRegisterBtn.dataset.saving = "false";
        return;
      }
      if (password.length < 8) {
        if (errorEl) {
          errorEl.textContent = "La contraseña debe tener al menos 8 caracteres";
          errorEl.classList.remove("hidden");
        }
        newRegisterBtn.dataset.saving = "false";
        return;
      }
      try {
        setLoading(true);
        await signUp(email, password, displayName);
        await loadAppAfterLogin();
      } catch (err) {
        console.error("Register error:", err);
        if (errorEl) {
          errorEl.textContent = err.message || "Error al crear cuenta";
          errorEl.classList.remove("hidden");
        }
      } finally {
        setLoading(false);
        newRegisterBtn.dataset.saving = "false";
      }
    });
  }
  
  ["login-email", "login-password"].forEach(id => {
    const input = el(id);
    if (input) input.addEventListener("keydown", e => { if (e.key === "Enter") loginBtn?.click(); });
  });
  ["reg-username", "reg-email", "reg-password"].forEach(id => {
    const input = el(id);
    if (input) input.addEventListener("keydown", e => { if (e.key === "Enter") registerBtn?.click(); });
  });
}

// ─── LOAD APP AFTER LOGIN ──────────────────────────────────
async function loadAppAfterLogin() {
  try {
    const token = localStorage.getItem("fin_token");
    if (!token) {
      throw new Error("No hay token guardado");
    }
    
    const user = await api.get("/me");
    state.user = user;
    
    showApp();
    
    const avatar = el("user-avatar");
    if (avatar) {
      avatar.textContent = (user.displayName || user.email || "U")[0].toUpperCase();
    }
    
    wireNav();
    wireTxForm();
    wireAccForm();
    wireRecurringForm();
    wireDebtForm();
    wireInstallmentForm();
    wireCategoryForm();
    wireProfileForm();
    wireGoalForm();
    wireBudgetForm();
    
    navigateTo("dashboard");
    
    console.log("App cargada correctamente");
  } catch (error) {
    console.error("Error cargando app:", error);
    showToast("Error al cargar la aplicación: " + error.message, "error");
    logout();
  }
}

// ─── GLOBAL EVENT DELEGATION ───────────────────────────────
document.addEventListener("click", async (e) => {
  const btn = e.target.closest("[data-action]");
  if (!btn) return;
  const { action, id } = btn.dataset;
  
  try {
    if (action === "del-tx") {
      if (!confirm("¿Eliminar esta transacción?")) return;
      await api.delete(`/transactions/${id}`);
      showToast("Transacción eliminada", "success");
      await loadTransactions();
    }
    if (action === "del-acc") {
      if (!confirm("¿Eliminar esta cuenta?")) return;
      await api.delete(`/accounts/${id}`);
      showToast("Cuenta eliminada", "success");
      await loadAccounts();
    }
    if (action === "del-rec") {
      if (!confirm("¿Eliminar este pago recurrente?")) return;
      await api.delete(`/recurring-payments/${id}`);
      showToast("Pago recurrente eliminado", "success");
      await loadRecurring();
    }
    if (action === "del-debt") {
      if (!confirm("¿Eliminar esta deuda?")) return;
      await api.delete(`/debts/${id}`);
      showToast("Deuda eliminada", "success");
      await loadDebts();
    }
    if (action === "del-cat") {
      if (!confirm("¿Eliminar esta categoría?")) return;
      await api.delete(`/categories/${id}`);
      showToast("Categoría eliminada", "success");
      await loadCategories();
    }
    if (action === "edit-tx") {
      const tx = state.transactions.find(item => item.id == id);
      if (tx) buildEditTransactionModal(tx);
    }
    if (action === "edit-acc") {
      const acc = state.accounts.find(item => item.id == id);
      if (acc) buildEditAccountModal(acc);
    }
    if (action === "edit-rec") {
      const rec = state.recurring.find(item => item.id == id);
      if (rec) buildEditRecurringModal(rec);
    }
    if (action === "edit-debt") {
      const debt = state.debts.find(item => item.id == id);
      if (debt) buildEditDebtModal(debt);
    }
    if (action === "edit-goal") {
      const goal = state.goals.find(item => item.id == id);
      if (goal) buildEditGoalModal(goal);
    }
    if (action === "del-goal") {
      if (!confirm("¿Eliminar esta meta?")) return;
      await api.delete(`/financial-goals/${id}`);
      showToast("Meta eliminada", "success");
      await loadGoals();
    }
    if (action === "edit-budget") {
      const budget = state.budgets.find(item => item.id == id);
      if (budget) buildEditBudgetModal(budget);
    }
    if (action === "del-budget") {
      if (!confirm("¿Eliminar este presupuesto?")) return;
      await api.delete(`/budgets/${id}`);
      showToast("Presupuesto eliminado", "success");
      await loadBudgets();
    }
    if (action === "pay-inst") {
      if (!confirm("¿Marcar esta partialidad como pagada?")) return;
      try {
        const installment = state.installments.find(i => i.id == id);
        if (!installment) throw new Error("No se encontró la partialidad");
        
        const debt = state.debts.find(d => d.id === installment.debtId);
        if (!debt) throw new Error("No se encontró la deuda asociada");
        
        const currentRemaining = debt.remainingBalance || debt.principalBalance || 0;
        const newRemaining = currentRemaining - installment.amount;
        
        if (newRemaining < 0) {
          showToast("El monto de la partialidad excede el saldo restante", "error");
          return;
        }
        
        await api.post(`/installments/${id}/pay`, {});
        
        await api.patch(`/debts/${debt.id}`, {
          remainingBalance: newRemaining,
          principalBalance: debt.principalBalance,
          installment: debt.installment,
          frequency: debt.frequency,
          nextDueDate: debt.nextDueDate,
          notes: debt.notes
        });
        
        showToast(`Partialidad pagada. Saldo restante: ${fmt(newRemaining, state.user?.currency || "MXN")}`, "success");
        await loadInstallments();
        await loadDebts();
      } catch (err) {
        showToast(err.message, "error");
      }
    }
    if (action === "del-inst") {
      if (!confirm("¿Eliminar esta partialidad?")) return;
      await api.delete(`/installments/${id}`);
      showToast("Partialidad eliminada", "success");
      await loadInstallments();
    }
    if (action === "edit-inst") {
      const inst = state.installments.find(i => i.id == id);
      if (inst) buildEditInstallmentModal(inst);
    }
    if (action === "add-progress-goal") {
      const goal = state.goals.find(g => g.id == id);
      if (goal) buildAddProgressModal(goal);
    }
  } catch (err) {
    showToast(err.message, "error");
  }
});

function syncCreditOnlyFields(scope, type) {
  const root = scope || document;
  root.querySelectorAll(".credit-only").forEach(field => {
    field.classList.toggle("hidden", type !== "credit");
  });
}

// ─── PWA INSTALL ─────────────────────────────────────────────
let deferredPrompt;

window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredPrompt = e;
  showInstallButton();
});

function showInstallButton() {
  const installBtn = document.getElementById('btn-install-app');
  if (installBtn) {
    installBtn.classList.remove('hidden');
    installBtn.addEventListener('click', async () => {
      if (deferredPrompt) {
        deferredPrompt.prompt();
        const { outcome } = await deferredPrompt.userChoice;
        if (outcome === 'accepted') {
          console.log('Usuario aceptó instalar la app');
        }
        deferredPrompt = null;
        installBtn.classList.add('hidden');
      }
    });
  }
}

// Función para compras a meses
async function showCreditCardPurchaseForm() {
  // Asegurar que las cuentas están cargadas
  if (!state.accounts || state.accounts.length === 0) {
    showToast("Cargando cuentas, intenta nuevamente", "info");
    await loadAccounts();  // Recargar cuentas
  }
  
  const creditCards = state.accounts.filter(a => a.type === "credit");
  
  console.log("=== DEBUG CREDIT CARDS ===");
  console.log("Todas las cuentas:", state.accounts);
  console.log("Tarjetas de crédito filtradas:", creditCards);
  
  if (creditCards.length === 0) {
    showToast("No tienes tarjetas de crédito registradas. Crea una primero.", "error");
    return;
  }
  
  const modalBody = `
    <div class="form-grid">
      <div class="field-group field-full">
        <label class="field-label">Tarjeta de crédito</label>
        <select id="purchase-account" class="field-input">
          ${creditCards.map(a => 
            `<option value="${a.id}">${a.name} - Límite: ${fmt(a.creditLimit)} - Saldo: ${fmt(a.balance)} - Tipo: ${a.type}</option>`
          ).join("")}
        </select>
      </div>
      <div class="field-group field-full">
        <label class="field-label">Nombre de la compra</label>
        <input id="purchase-name" class="field-input" type="text" placeholder="Ej: iPhone 15, Lavadora, Viaje..." />
      </div>
      <div class="field-group">
        <label class="field-label">Monto total</label>
        <input id="purchase-total" class="field-input" type="number" step="0.01" placeholder="0.00" />
      </div>
      <div class="field-group">
        <label class="field-label">Número de meses</label>
        <select id="purchase-months" class="field-input">
          <option value="3">3 meses</option>
          <option value="6">6 meses</option>
          <option value="9">9 meses</option>
          <option value="12">12 meses</option>
          <option value="18">18 meses</option>
          <option value="24">24 meses</option>
        </select>
      </div>
      <div class="field-group">
        <label class="field-label">Tasa de interés (%)</label>
        <input id="purchase-interest" class="field-input" type="number" step="0.01" placeholder="0 (sin intereses)" value="0" />
      </div>
      <div class="field-group">
        <label class="field-label">Primer pago</label>
        <input id="purchase-first-due" class="field-input" type="date" value="${todayIso()}" />
      </div>
      <div class="field-group">
        <label class="field-label">Categoría</label>
        <select id="purchase-category" class="field-input">
          <option value="">Seleccionar categoría</option>
          ${state.categories.map(c => `<option value="${c.id}">${c.icon || ""} ${c.name}</option>`).join("")}
        </select>
      </div>
    </div>
  `;
  
  openModal("Comprar a meses", modalBody, async () => {
    const accountId = el("purchase-account")?.value;
    const selectedOption = el("purchase-account")?.options[el("purchase-account").selectedIndex];
    const accountName = selectedOption?.text || "";
    
    console.log("Cuenta seleccionada ID:", accountId);
    console.log("Cuenta seleccionada nombre:", accountName);
    
    const selectedCard = creditCards.find(c => c.id === accountId);
    
    if (!selectedCard) {
      showToast("Selecciona una tarjeta de crédito válida", "error");
      return;
    }
    
    console.log("Tarjeta seleccionada:", selectedCard);
    console.log("Tipo de tarjeta:", selectedCard.type);
    
    if (selectedCard.type !== "credit") {
      showToast(`La cuenta "${selectedCard.name}" no es una tarjeta de crédito (tipo: ${selectedCard.type})`, "error");
      return;
    }
    
    const name = el("purchase-name")?.value.trim();
    const totalAmount = Number(el("purchase-total")?.value || 0);
    const months = parseInt(el("purchase-months")?.value, 10);
    const interestRate = Number(el("purchase-interest")?.value || 0);
    const firstDueDate = el("purchase-first-due")?.value;
    const categoryId = el("purchase-category")?.value || null;
    
    if (!name || !totalAmount || !months || !firstDueDate) {
      showToast("Completa todos los campos", "error");
      return;
    }
    
    const body = {
      accountId: accountId,
      name: name,
      totalAmount: totalAmount,
      months: months,
      interestRate: interestRate,
      firstDueDate: firstDueDate,
      categoryId: categoryId
    };
    
    console.log("Enviando body:", body);
    
    try {
      await api.post("/installments/credit-card-purchase", body);
      showToast(`Compra a ${months} meses registrada exitosamente`, "success");
      closeModal();
      await loadInstallments();
      await loadAccounts();
      await loadDebts();
    } catch (e) {
      console.error("Error al registrar compra:", e);
      showToast(e.message, "error");
    }
  });
}

// ─── INIT ──────────────────────────────────────────────────
async function init() {
  wireAuth();
  
  const token = localStorage.getItem("fin_token");
  
  if (token) {
    try {
      setLoading(true);
      const user = await api.get("/me");
      state.user = user;
      showApp();
      
      const avatar = el("user-avatar");
      if (avatar) {
        avatar.textContent = (user.displayName || user.email || "U")[0].toUpperCase();
      }
      
      wireNav();
      wireTxForm();
      wireAccForm();
      wireRecurringForm();
      wireDebtForm();
      wireInstallmentForm();
      wireCategoryForm();
      wireProfileForm();
      wireGoalForm();
      wireBudgetForm();
      navigateTo("dashboard");
      
    } catch (error) {
      console.error("Sesión inválida:", error);
      localStorage.removeItem("fin_token");
      localStorage.removeItem("fin_refresh");
      showAuth();
    } finally {
      setLoading(false);
    }
  } else {
    showAuth();
  }
}

// Iniciar la aplicación
init();