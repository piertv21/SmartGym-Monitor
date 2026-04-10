const areaFilter = document.getElementById("history-area-filter");
const machineFilter = document.getElementById("history-machine-filter");
const dateFromInput = document.getElementById("history-date-from");
const dateToInput = document.getElementById("history-date-to");
const rangeButtons = document.querySelectorAll("button[data-history-range]");
const applyFiltersButton = document.getElementById("history-apply-filters");
const feedbackElement = document.getElementById("history-feedback");
const sessionsBody = document.getElementById("history-sessions-body");
const paginationLabel = document.getElementById("history-pagination-label");
const prevPageButton = document.getElementById("history-page-prev");
const nextPageButton = document.getElementById("history-page-next");
const exportCsvButton = document.getElementById("history-export-csv");
const chartCanvas = document.getElementById("history-attendance-chart");
const dateInputs = [dateFromInput, dateToInput].filter(Boolean);

const ROWS_PER_PAGE = 5;
const HISTORY_LINE_COLOR = "#39d98a";
const HISTORY_TIMEZONE = "Europe/Rome";

const historyDateTimeFormatter = new Intl.DateTimeFormat("it-IT", {
  timeZone: HISTORY_TIMEZONE,
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
  hour12: false,
});

const historyState = {
  areas: [],
  machines: [],
  selectedAreaId: "",
  selectedMachineId: "",
  rangePreset: "day",
  fromDate: "",
  toDate: "",
  granularity: "daily",
  sessions: [],
  currentPage: 1,
  loading: false,
};

let attendanceChart = null;

function escapeHtml(value) {
  return String(value ?? "-")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function setFeedback(message) {
  if (!feedbackElement) {
    return;
  }
  feedbackElement.textContent = message;
}

function toDateString(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function applyRangePreset(preset) {
  const today = new Date();
  const to = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  let from = new Date(to);

  if (preset === "week") {
    from.setDate(from.getDate() - 6);
  } else if (preset === "month") {
    from.setDate(from.getDate() - 29);
  } else if (preset === "day") {
    // Ultimo giorno: finestra di 24h su base daily (ieri -> oggi).
    from.setDate(from.getDate() - 1);
  }

  historyState.rangePreset = preset;
  historyState.granularity = preset === "month" ? "monthly" : "daily";
  if (preset !== "custom") {
    historyState.fromDate = toDateString(from);
    historyState.toDate = toDateString(to);
  }

  if (dateFromInput && dateToInput) {
    const disabled = preset !== "custom";
    dateFromInput.disabled = disabled;
    dateToInput.disabled = disabled;
    dateInputs.forEach((input) => {
      input.classList.toggle("history-date-input-disabled", disabled);
      input.setAttribute("aria-disabled", String(disabled));
    });
    dateFromInput.value = historyState.fromDate;
    dateToInput.value = historyState.toDate;
  }

  rangeButtons.forEach((button) => {
    const isActive = button.getAttribute("data-history-range") === preset;
    button.classList.toggle("active", isActive);
  });
}

function formatTime(value) {
  if (!value) {
    return "-";
  }

  const rawValue = String(value).trim();
  const hasExplicitTimezone = /([zZ]|[+-]\d{2}:?\d{2})$/.test(rawValue);
  const utcNormalizedValue = hasExplicitTimezone ? rawValue : `${rawValue}Z`;

  const date = new Date(utcNormalizedValue);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  return historyDateTimeFormatter.format(date);
}

function formatDuration(secondsValue) {
  const seconds = Number(secondsValue);
  if (!Number.isFinite(seconds) || seconds <= 0) {
    return "0 min";
  }
  const minutes = Math.round(seconds / 60);
  return `${minutes} min`;
}

function ensureChart() {
  if (!chartCanvas || typeof Chart === "undefined") {
    return null;
  }

  if (attendanceChart) {
    return attendanceChart;
  }

  const ctx = chartCanvas.getContext("2d");
  attendanceChart = new Chart(ctx, {
    type: "line",
    data: {
      labels: [],
      datasets: [
        {
          label: "Utenti in palestra",
          data: [],
          borderColor: HISTORY_LINE_COLOR,
          backgroundColor: "rgba(57, 217, 138, 0.15)",
          borderWidth: 3,
          fill: true,
          tension: 0.35,
          pointRadius: 3,
          pointHoverRadius: 4,
        },
      ],
    },
    options: {
      maintainAspectRatio: false,
      interaction: {
        mode: "nearest",
        intersect: false,
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          enabled: true,
          displayColors: false,
          callbacks: {
            label(context) {
              const value = Number(context.parsed?.y ?? 0);
              return `Valore: ${Number.isFinite(value) ? value : 0}`;
            },
          },
        },
      },
      scales: {
        x: {
          grid: { color: "rgba(255, 255, 255, 0.06)" },
          ticks: { color: "#9aa3b2" },
          title: {
            display: true,
            text: "Periodo",
            color: "#9aa3b2",
          },
        },
        y: {
          beginAtZero: true,
          grid: { color: "rgba(255, 255, 255, 0.06)" },
          ticks: { color: "#9aa3b2" },
          title: {
            display: true,
            text: "Utenti presenti",
            color: "#9aa3b2",
          },
        },
      },
    },
  });

  return attendanceChart;
}

function renderAttendanceChart(attendancePayload) {
  const chart = ensureChart();
  if (!chart) {
    return;
  }

  const points = Array.isArray(attendancePayload?.series)
    ? attendancePayload.series
    : [];

  chart.data.labels = points.map((point) => String(point?.period ?? "-"));
  chart.data.datasets[0].data = points.map((point) => {
    const rawValue = Number(point?.currentCount);
    return Number.isFinite(rawValue) ? rawValue : 0;
  });
  chart.data.datasets[0].pointRadius = points.length <= 1 ? 4 : 0;
  chart.data.datasets[0].pointHoverRadius = points.length <= 1 ? 5 : 4;
  chart.update();
}

function renderMachineOptions() {
  if (!machineFilter) {
    return;
  }

  const filteredMachines = historyState.selectedAreaId
    ? historyState.machines.filter(
        (machine) =>
          String(machine.areaId || "") === historyState.selectedAreaId,
      )
    : historyState.machines;

  machineFilter.innerHTML = [
    '<option value="">Tutte le macchine</option>',
    ...filteredMachines.map(
      (machine) =>
        `<option value="${escapeHtml(machine.machineId)}">${escapeHtml(machine.machineId)}</option>`,
    ),
  ].join("");

  const stillAvailable = filteredMachines.some(
    (machine) => machine.machineId === historyState.selectedMachineId,
  );
  if (!stillAvailable) {
    historyState.selectedMachineId = "";
  }
  machineFilter.value = historyState.selectedMachineId;
}

function renderAreaOptions() {
  if (!areaFilter) {
    return;
  }

  areaFilter.innerHTML = [
    '<option value="">Tutte le aree</option>',
    ...historyState.areas.map(
      (area) =>
        `<option value="${escapeHtml(area.id)}">${escapeHtml(area.name)}</option>`,
    ),
  ].join("");

  areaFilter.value = historyState.selectedAreaId;
}

function renderSessionsTable() {
  if (!sessionsBody || !paginationLabel || !prevPageButton || !nextPageButton) {
    return;
  }

  const totalRows = historyState.sessions.length;
  const totalPages = Math.max(1, Math.ceil(totalRows / ROWS_PER_PAGE));
  historyState.currentPage = Math.min(historyState.currentPage, totalPages);
  historyState.currentPage = Math.max(1, historyState.currentPage);

  const startIndex = (historyState.currentPage - 1) * ROWS_PER_PAGE;
  const pageRows = historyState.sessions.slice(
    startIndex,
    startIndex + ROWS_PER_PAGE,
  );

  if (pageRows.length === 0) {
    sessionsBody.innerHTML =
      '<tr><td colspan="6" class="text-secondary">Nessuna sessione trovata per i filtri selezionati.</td></tr>';
    paginationLabel.textContent = "Nessuna sessione disponibile.";
    prevPageButton.disabled = true;
    nextPageButton.disabled = true;
    return;
  }

  sessionsBody.innerHTML = pageRows
    .map(
      (row) => `
      <tr>
        <td>${escapeHtml(row.machineId)}</td>
        <td>${escapeHtml(row.areaId)}</td>
        <td>${escapeHtml(formatTime(row.startTime))}</td>
        <td>${escapeHtml(formatTime(row.endTime))}</td>
        <td>${escapeHtml(formatDuration(row.durationSeconds))}</td>
        <td>${escapeHtml(row.badgeId)}</td>
      </tr>
    `,
    )
    .join("");

  const shownStart = startIndex + 1;
  const shownEnd = startIndex + pageRows.length;
  paginationLabel.textContent = `Mostrate ${shownStart}-${shownEnd} di ${totalRows} sessioni`;

  prevPageButton.disabled = historyState.currentPage <= 1;
  nextPageButton.disabled = historyState.currentPage >= totalPages;
}

async function loadHistoryFilters() {
  try {
    const response = await fetch("/api/history/filters", {
      method: "GET",
      headers: { Accept: "application/json" },
    });

    if (!response.ok) {
      if (response.status === 401) {
        setFeedback("Sessione scaduta. Esegui di nuovo il login.");
        return false;
      }
      setFeedback(`Errore caricamento filtri (${response.status}).`);
      return false;
    }

    const payload = await response.json();
    historyState.areas = Array.isArray(payload.areas) ? payload.areas : [];
    historyState.machines = Array.isArray(payload.machines)
      ? payload.machines
      : [];

    renderAreaOptions();
    renderMachineOptions();
    return true;
  } catch (error) {
    setFeedback(`Errore di rete: ${error}`);
    return false;
  }
}

function buildHistoryQuery() {
  const params = new URLSearchParams();
  params.set("from", historyState.fromDate);
  params.set("to", historyState.toDate);
  params.set("granularity", historyState.granularity);
  if (historyState.selectedAreaId) {
    params.set("areaId", historyState.selectedAreaId);
  }
  if (historyState.selectedMachineId) {
    params.set("machineId", historyState.selectedMachineId);
  }
  return params.toString();
}

async function loadHistoryData() {
  if (historyState.loading) {
    return;
  }

  historyState.loading = true;
  setFeedback("Caricamento storico in corso...");

  try {
    const response = await fetch(`/api/history?${buildHistoryQuery()}`, {
      method: "GET",
      headers: { Accept: "application/json" },
    });

    if (!response.ok) {
      if (response.status === 401) {
        setFeedback("Sessione scaduta. Esegui di nuovo il login.");
        return;
      }
      setFeedback(`Errore caricamento storico (${response.status}).`);
      return;
    }

    const payload = await response.json();
    renderAttendanceChart(payload.attendanceSeries || {});
    const chart = ensureChart();
    if (chart) {
      chart.data.datasets[0].label = "Utenti in palestra";
      chart.options.scales.y.title.text = "Utenti presenti";
      chart.options.plugins.tooltip.callbacks.label = (context) => {
        const value = Number(context.parsed?.y ?? 0);
        return `Valore: ${Number.isFinite(value) ? value : 0}`;
      };
      chart.update();
    }
    historyState.sessions = Array.isArray(payload.sessions)
      ? payload.sessions
      : [];
    historyState.currentPage = 1;
    renderSessionsTable();
    setFeedback("");
  } catch (error) {
    setFeedback(`Errore di rete: ${error}`);
  } finally {
    historyState.loading = false;
  }
}

function exportSessionsCsv() {
  if (
    !Array.isArray(historyState.sessions) ||
    historyState.sessions.length === 0
  ) {
    return;
  }

  const lines = ["machineId,areaId,startTime,endTime,durationSeconds,badgeId"];
  historyState.sessions.forEach((row) => {
    const values = [
      row.machineId,
      row.areaId,
      row.startTime,
      row.endTime,
      row.durationSeconds,
      row.badgeId,
    ].map((value) => `"${String(value ?? "").replaceAll('"', '""')}"`);
    lines.push(values.join(","));
  });

  const csvContent = lines.join("\n");
  const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `history_${historyState.fromDate}_${historyState.toDate}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}

function syncFiltersFromUi() {
  historyState.selectedAreaId = areaFilter?.value || "";
  historyState.selectedMachineId = machineFilter?.value || "";
  historyState.fromDate = dateFromInput?.value || historyState.fromDate;
  historyState.toDate = dateToInput?.value || historyState.toDate;
}

if (areaFilter) {
  areaFilter.addEventListener("change", async () => {
    syncFiltersFromUi();
    renderMachineOptions();
    await loadHistoryData();
  });
}

if (machineFilter) {
  machineFilter.addEventListener("change", async () => {
    syncFiltersFromUi();
    await loadHistoryData();
  });
}

if (dateFromInput) {
  dateFromInput.addEventListener("change", async () => {
    syncFiltersFromUi();
    await loadHistoryData();
  });
}

if (dateToInput) {
  dateToInput.addEventListener("change", async () => {
    syncFiltersFromUi();
    await loadHistoryData();
  });
}

rangeButtons.forEach((button) => {
  button.addEventListener("click", async () => {
    const preset = button.getAttribute("data-history-range") || "day";
    applyRangePreset(preset);
    syncFiltersFromUi();
    await loadHistoryData();
  });
});

if (applyFiltersButton) {
  applyFiltersButton.addEventListener("click", async () => {
    syncFiltersFromUi();
    await loadHistoryData();
  });
}

if (prevPageButton) {
  prevPageButton.addEventListener("click", () => {
    historyState.currentPage -= 1;
    renderSessionsTable();
  });
}

if (nextPageButton) {
  nextPageButton.addEventListener("click", () => {
    historyState.currentPage += 1;
    renderSessionsTable();
  });
}

if (exportCsvButton) {
  exportCsvButton.addEventListener("click", exportSessionsCsv);
}

(async function initHistoryPage() {
  applyRangePreset("day");
  syncFiltersFromUi();
  const loadedFilters = await loadHistoryFilters();
  if (loadedFilters) {
    await loadHistoryData();
  }
})();
