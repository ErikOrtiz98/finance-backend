// ─── CONFIG ───────────────────────────────────────────────
const API_BASE = "";

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

      // Si es 401 y tenemos refresh token, intentar renovar
      if (response.status === 401 && api._refresh()) {
        const refreshed = await api._tryRefresh();
        if (refreshed) {
          headers["Authorization"] = `Bearer ${api._token()}`;
          const retryResponse = await fetch(`${API_BASE}${path}`, { ...options, headers });
          if (!retryResponse.ok) throw new Error(`HTTP ${retryResponse.status}`);
          if (retryResponse.status === 204) return null;
          return retryResponse.json();
        } else {
          logout();
          return null;
        }
      }

      if (response.status === 204) return null;
      
      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || `Error ${response.status}`);
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
  const data = await api.post("/auth/sign-in", { email, password });
  if (data.accessToken) {
    localStorage.setItem("fin_token", data.accessToken);
    if (data.refreshToken) localStorage.setItem("fin_refresh", data.refreshToken);
  }
  return data;
}

async function signUp(email, password, displayName) {
  const data = await api.post("/auth/sign-up", { email, password, displayName });
  if (data.accessToken) {
    localStorage.setItem("fin_token", data.accessToken);
    if (data.refreshToken) localStorage.setItem("fin_refresh", data.refreshToken);
  }
  return data;
}

function logout() {
  const refreshToken = localStorage.getItem("fin_refresh");
  if (refreshToken) {
    api.post("/auth/sign-out", { refreshToken }).catch(() => {});
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
  summary: null,
  categoryStats: [],
  upcoming: null,
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
  debts: "Deudas y préstamos",
  analytics: "Estadísticas",
  categories: "Categorías",
  profile: "Mi perfil",
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
      case "analytics": await loadAnalytics(); break;
      case "categories": await loadCategories(); break;
      case "profile": await loadProfile(); break;
    }
  } catch (e) {
    console.error("Error loading section:", e);
    showToast(e.message || "Error al cargar datos", "error");
  }
}

// ─── DASHBOARD ─────────────────────────────────────────────
async function loadDashboard() {
  setLoading(true);
  try {
    const [summary, upcoming, catStats] = await Promise.all([
      api.get("/stats/summary"),
      api.get("/stats/upcoming"),
      api.get("/stats/categories"),
    ]);
    state.summary = summary;
    state.upcoming = upcoming;
    state.categoryStats = catStats || [];

    renderKPIs();
    renderUpcoming("upcoming-list", state.upcoming?.items || []);
    renderCategoryBars("category-bars", state.categoryStats);
  } catch (error) {
    console.error("Dashboard error:", error);
  } finally {
    setLoading(false);
  }
}

function renderKPIs() {
  const s = state.summary || {};
  const cur = state.user?.currency || "MXN";
  
  const kpiIncome = el("kpi-income");
  const kpiObligations = el("kpi-obligations");
  const kpiBalance = el("kpi-balance");
  const kpiDebt = el("kpi-debt");
  const kpiNote = el("kpi-income-note");
  
  if (kpiIncome) kpiIncome.textContent = fmt(s.totalIncome || 0, cur);
  if (kpiObligations) kpiObligations.textContent = fmt(s.totalObligations || 0, cur);
  if (kpiBalance) kpiBalance.textContent = fmt(s.netCashflow || 0, cur);
  if (kpiDebt) kpiDebt.textContent = fmt(s.totalDebt || 0, cur);
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
        <div class="item-name">${item.name || item.description || item.title}</div>
        <div class="item-due">${relativeDate(item.dueDate || item.nextDueDate || item.date)}</div>
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
  const max = Math.max(...items.map(i => Number(i.total || 0)));
  c.innerHTML = items.map(item => {
    const val = Number(item.total || 0);
    const pct = max > 0 ? Math.round((val / max) * 100) : 0;
    return `
      <div class="bar-row">
        <div class="bar-meta">
          <span>${item.categoryName || item.category || item.name}</span>
          <span>${fmt(val, cur)}</span>
        </div>
        <div class="bar-track"><div class="bar-fill" style="width:${pct}%"></div></div>
      </div>
    `;
  }).join("");
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
    new Date(b.transactionDate || b.date) - new Date(a.transactionDate || a.date)
  );
  
  c.innerHTML = sorted.map(tx => {
    const isExpense = tx.type === "expense" || tx.type === "withdrawal";
    const cat = state.categories.find(c => c.id === tx.categoryId);
    const acc = state.accounts.find(a => a.id === tx.accountId);
    return `
      <div class="data-row">
        <div class="data-row-icon">${cat?.icon || (isExpense ? "💸" : "💰")}</div>
        <div class="data-row-info">
          <div class="data-row-name">${tx.description || tx.name}</div>
          <div class="data-row-meta">${cat?.name || "—"} · ${acc?.name || "—"} · ${relativeDate(tx.transactionDate || tx.date)}</div>
        </div>
        <div class="data-row-amount ${isExpense ? "expense" : "income"}">
          ${isExpense ? "-" : "+"}${fmt(tx.amount, cur)}
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
        <div class="account-type">${typeLabel}</div>
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

// ─── RECURRING ─────────────────────────────────────────────
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
  const freqMap = { WEEKLY: "Semanal", BIWEEKLY: "Quincenal", MONTHLY: "Mensual", YEARLY: "Anual" };
  
  if (!state.recurring.length) {
    c.innerHTML = `<div class="empty-state"><div class="empty-state-icon">🔄</div>Sin pagos recurrentes</div>`;
    return;
  }
  
  c.innerHTML = state.recurring.map(r => `
    <div class="data-row">
      <div class="data-row-icon">🔄</div>
      <div class="data-row-info">
        <div class="data-row-name">${r.name}</div>
        <div class="data-row-meta">${freqMap[r.frequency] || r.frequency} · próximo: ${relativeDate(r.nextDueDate)}</div>
      </div>
      <div class="data-row-amount expense">${fmt(r.amount, cur)}</div>
      <div class="data-row-actions">
        <button class="btn-edit-sm" data-action="edit-rec" data-id="${r.id}">Editar</button>
        <button class="btn-danger-sm" data-action="del-rec" data-id="${r.id}">Eliminar</button>
      </div>
    </div>
  `).join("");
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
    renderUpcoming("analytics-upcoming", state.upcoming?.items || []);
  } finally {
    setLoading(false);
  }
}

function renderAnalyticsSummary() {
  const s = state.summary || {};
  const cur = state.user?.currency || "MXN";
  const rows = [
    ["Ingreso del periodo", fmt(s.totalIncome || 0, cur)],
    ["Gastos del periodo", fmt(s.totalExpenses || 0, cur)],
    ["Obligaciones próximas", fmt(s.totalObligations || 0, cur)],
    ["Flujo neto", fmt(s.netCashflow || 0, cur)],
    ["Deuda total", fmt(s.totalDebt || 0, cur)],
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
  select.innerHTML = `<option value="">Sin cuenta</option>` +
    accounts.map(a => `<option value="${a.id}">${a.name}</option>`).join("");
}

function toggleCreditFields() {
  const type = el("acc-type")?.value;
  document.querySelectorAll(".credit-only").forEach(field =>
    field.classList.toggle("hidden", type !== "credit")
  );
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
  if (modal) modal.classList.add("hidden");
  editCtx = { type: null, id: null };
}

// ─── EVENT HANDLERS ────────────────────────────────────────
async function handleDelete(action, id, loadFunction, entityName) {
  if (!confirm(`¿Eliminar ${entityName}?`)) return;
  try {
    await api.delete(`/${action}/${id}`);
    showToast(`${entityName} eliminado`, "success");
    await loadFunction();
  } catch (err) {
    showToast(err.message, "error");
  }
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
  
  if (addBtn) addBtn.addEventListener("click", () => showInlineForm("transaction-form-wrap", "btn-add-transaction"));
  if (cancelBtn) cancelBtn.addEventListener("click", () => hideForm("transaction-form-wrap", "btn-add-transaction", "+ Nueva"));
  if (dateInput) dateInput.value = todayIso();
  
  if (saveBtn) saveBtn.addEventListener("click", async () => {
    try {
      const body = {
        description: el("tx-name")?.value.trim(),
        amount: Number(el("tx-amount")?.value || 0),
        type: el("tx-type")?.value,
        categoryId: el("tx-category")?.value || null,
        accountId: el("tx-account")?.value || null,
        transactionDate: el("tx-date")?.value,
        notes: el("tx-note")?.value.trim(),
        currency: state.user?.currency || "MXN",
      };
      if (!body.description || !body.amount) {
        showToast("Completa concepto y monto", "error");
        return;
      }
      await api.post("/transactions", body);
      showToast("Transacción guardada", "success");
      hideForm("transaction-form-wrap", "btn-add-transaction", "+ Nueva");
      document.querySelectorAll("#transaction-form-wrap input, #transaction-form-wrap select").forEach(i => i.value = "");
      if (dateInput) dateInput.value = todayIso();
      await loadTransactions();
    } catch (e) {
      showToast(e.message, "error");
    }
  });
}

function wireAccForm() {
  const addBtn = el("btn-add-account");
  const cancelBtn = el("btn-cancel-account");
  const saveBtn = el("btn-save-account");
  const typeSelect = el("acc-type");
  
  if (addBtn) addBtn.addEventListener("click", () => showInlineForm("account-form-wrap", "btn-add-account"));
  if (cancelBtn) cancelBtn.addEventListener("click", () => hideForm("account-form-wrap", "btn-add-account", "+ Nueva cuenta"));
  if (typeSelect) typeSelect.addEventListener("change", toggleCreditFields);
  
  if (saveBtn) saveBtn.addEventListener("click", async () => {
    try {
      const body = {
        name: el("acc-name")?.value.trim(),
        type: el("acc-type")?.value,
        balance: Number(el("acc-balance")?.value || 0),
        currency: el("acc-currency")?.value,
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
      await loadAccounts();
    } catch (e) {
      showToast(e.message, "error");
    }
  });
}

function wireRecurringForm() {
  const addBtn = el("btn-add-recurring");
  const cancelBtn = el("btn-cancel-recurring");
  const saveBtn = el("btn-save-recurring");
  const nextDue = el("rec-next-due");
  
  if (addBtn) addBtn.addEventListener("click", () => showInlineForm("recurring-form-wrap", "btn-add-recurring"));
  if (cancelBtn) cancelBtn.addEventListener("click", () => hideForm("recurring-form-wrap", "btn-add-recurring", "+ Nuevo"));
  if (nextDue) nextDue.value = todayIso();
  
  if (saveBtn) saveBtn.addEventListener("click", async () => {
    try {
      const body = {
        name: el("rec-name")?.value.trim(),
        amount: Number(el("rec-amount")?.value || 0),
        frequency: el("rec-frequency")?.value,
        nextDueDate: el("rec-next-due")?.value,
        categoryId: el("rec-category")?.value || null,
        accountId: el("rec-account")?.value || null,
        currency: state.user?.currency || "MXN",
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
    }
  });
}

function wireDebtForm() {
  const addBtn = el("btn-add-debt");
  const cancelBtn = el("btn-cancel-debt");
  const saveBtn = el("btn-save-debt");
  const dueDate = el("debt-due-date");
  
  if (addBtn) addBtn.addEventListener("click", () => showInlineForm("debt-form-wrap", "btn-add-debt"));
  if (cancelBtn) cancelBtn.addEventListener("click", () => hideForm("debt-form-wrap", "btn-add-debt", "+ Nueva deuda"));
  if (dueDate) dueDate.value = todayIso();
  
  if (saveBtn) saveBtn.addEventListener("click", async () => {
    try {
      const body = {
        name: el("debt-name")?.value.trim(),
        principalBalance: Number(el("debt-remaining")?.value || 0),
        installment: Number(el("debt-min-payment")?.value || 0),
        nextDueDate: el("debt-due-date")?.value,
        frequency: "monthly",
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
    }
  });
}

function wireCategoryForm() {
  const addBtn = el("btn-add-category");
  const cancelBtn = el("btn-cancel-category");
  const saveBtn = el("btn-save-category");
  
  if (addBtn) addBtn.addEventListener("click", () => showInlineForm("category-form-wrap", "btn-add-category"));
  if (cancelBtn) cancelBtn.addEventListener("click", () => hideForm("category-form-wrap", "btn-add-category", "+ Nueva"));
  
  if (saveBtn) saveBtn.addEventListener("click", async () => {
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
    }
  });
}

function wireProfileForm() {
  const periodSelect = el("profile-period");
  const saveBtn = el("btn-save-profile");
  const exportBtn = el("btn-export-backup");
  const importInput = el("import-backup");
  
  if (periodSelect) periodSelect.addEventListener("change", () => toggleProfilePeriodFields(periodSelect.value));
  
  if (saveBtn) saveBtn.addEventListener("click", async () => {
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
      
      const body = {
        monthlyIncome: Number(el("profile-income")?.value || 0),
        currency: el("profile-currency")?.value,
        payCycle: period,
        payDays: payDays,
      };
      await api.patch("/me", body);
      state.user = { ...(state.user || {}), ...body };
      showToast("Perfil actualizado", "success");
    } catch (e) {
      showToast(e.message, "error");
    }
  });
  
  if (exportBtn) exportBtn.addEventListener("click", async () => {
    try {
      const data = await api.get("/backup/export");
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = Object.assign(document.createElement("a"), { href: url, download: `respaldo-${todayIso()}.json` });
      a.click();
      URL.revokeObjectURL(url);
      showToast("Respaldo exportado", "success");
    } catch (e) {
      showToast(e.message, "error");
    }
  });
  
  if (importInput) importInput.addEventListener("change", async (e) => {
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
      importInput.value = "";
    }
  });
}

// ─── AUTH VIEWS ────────────────────────────────────────────
function showAuth() {
  const authView = el("auth-view");
  const appView = el("app-view");
  if (authView) authView.classList.remove("hidden");
  if (appView) appView.classList.add("hidden");
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
  
  if (loginBtn) loginBtn.addEventListener("click", async () => {
    const email = el("login-email")?.value.trim();
    const password = el("login-password")?.value;
    const errorEl = el("login-error");
    
    if (errorEl) errorEl.classList.add("hidden");
    if (!email || !password) {
      if (errorEl) {
        errorEl.textContent = "Completa correo y contraseña";
        errorEl.classList.remove("hidden");
      }
      return;
    }
    try {
      setLoading(true);
      await signIn(email, password);
      await bootApp();
    } catch (err) {
      if (errorEl) {
        errorEl.textContent = err.message || "Credenciales incorrectas";
        errorEl.classList.remove("hidden");
      }
    } finally {
      setLoading(false);
    }
  });
  
  if (registerBtn) registerBtn.addEventListener("click", async () => {
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
      return;
    }
    if (password.length < 8) {
      if (errorEl) {
        errorEl.textContent = "La contraseña debe tener al menos 8 caracteres";
        errorEl.classList.remove("hidden");
      }
      return;
    }
    try {
      setLoading(true);
      await signUp(email, password, displayName);
      await bootApp();
    } catch (err) {
      if (errorEl) {
        errorEl.textContent = err.message || "Error al crear cuenta";
        errorEl.classList.remove("hidden");
      }
    } finally {
      setLoading(false);
    }
  });
  
  // Enter key support
  ["login-email", "login-password"].forEach(id => {
    const input = el(id);
    if (input) input.addEventListener("keydown", e => { if (e.key === "Enter") loginBtn?.click(); });
  });
  ["reg-username", "reg-email", "reg-password"].forEach(id => {
    const input = el(id);
    if (input) input.addEventListener("keydown", e => { if (e.key === "Enter") registerBtn?.click(); });
  });
}

// ─── GLOBAL EVENT DELEGATION ───────────────────────────────
document.addEventListener("click", async (e) => {
  const btn = e.target.closest("[data-action]");
  if (!btn) return;
  const { action, id } = btn.dataset;
  
  try {
    if (action === "del-tx") await handleDelete("transactions", id, loadTransactions, "Transacción");
    if (action === "del-acc") await handleDelete("accounts", id, loadAccounts, "Cuenta");
    if (action === "del-rec") await handleDelete("recurring-payments", id, loadRecurring, "Pago recurrente");
    if (action === "del-debt") await handleDelete("debts", id, loadDebts, "Deuda");
    if (action === "del-cat") await handleDelete("categories", id, loadCategories, "Categoría");
  } catch (err) {
    showToast(err.message, "error");
  }
});

// ─── BOOT ──────────────────────────────────────────────────
async function bootApp() {
  try {
    const user = await api.get("/me");
    state.user = user;
    const avatar = el("user-avatar");
    if (avatar) avatar.textContent = (user.displayName || user.email || "U")[0].toUpperCase();
    showApp();
    wireNav();
    wireTxForm();
    wireAccForm();
    wireRecurringForm();
    wireDebtForm();
    wireCategoryForm();
    wireProfileForm();
    navigateTo("dashboard");
    
    if ("serviceWorker" in navigator) {
      navigator.serviceWorker.register("./sw.js").catch(() => console.log("SW registration failed"));
    }
  } catch (error) {
    console.error("Boot error:", error);
    logout();
  }
}

async function init() {
  wireAuth();
  if (api._token()) {
    try {
      setLoading(true);
      await bootApp();
    } catch {
      showAuth();
    } finally {
      setLoading(false);
    }
  } else {
    showAuth();
  }
}

init();