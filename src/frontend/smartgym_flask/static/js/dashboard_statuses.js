const statusesBody = document.getElementById("statuses-body");
const statusesFeedback = document.getElementById("statuses-feedback");
const refreshButton = document.getElementById("refresh-statuses");
const maintenanceBody = document.getElementById("maintenance-body");
const maintenanceFeedback = document.getElementById("maintenance-feedback");
const pageLastUpdate = document.getElementById("page-last-update");

const kpiUsersInside = document.getElementById("kpi-users-inside");
const kpiUsersEntered = document.getElementById("kpi-users-entered");
const kpiUsersExited = document.getElementById("kpi-users-exited");
const kpiAverageSession = document.getElementById("kpi-average-session");

let internalMachines = [];
const DASHBOARD_REFRESH_MS = 5000;
let dashboardRefreshInFlight = false;

const ENDPOINTS = {
  analyticsDashboard: "/api/analytics/dashboard",
  machines: "/api/machines",
  maintenanceToggle: "/api/maintenance/toggle",
  statuses: "/api/statuses",
};

const MESSAGE_SESSION_EXPIRED = "Sessione scaduta. Esegui di nuovo il login.";

function setLastUpdateLabel(date = new Date()) {
  if (!pageLastUpdate) {
    return;
  }

  pageLastUpdate.textContent = `Ultimo aggiornamento: ${date.toLocaleTimeString()}`;
}

function statusBadge(online) {
  if (online === true) {
    return '<span class="badge badge--up">ONLINE</span>';
  }
  if (online === false) {
    return '<span class="badge badge--down">OFFLINE</span>';
  }
  return '<span class="badge">N/A</span>';
}

function escapeHtml(value) {
  return String(value ?? "-")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function setFeedback(element, message) {
  if (!element) {
    return;
  }
  element.textContent = message;
}

async function extractErrorMessage(response, fallbackMessage) {
  try {
    const payload = await response.json();
    if (payload && typeof payload.error === "string" && payload.error.trim()) {
      return payload.error;
    }
  } catch (_error) {
    // Keep fallback if response body is empty or not JSON.
  }
  return fallbackMessage;
}

function compareAlphabetically(left, right) {
  return String(left ?? "").localeCompare(String(right ?? ""), "it", {
    sensitivity: "base",
  });
}

function machineStatusBadge(status) {
  const normalizedStatus = String(status ?? "").toUpperCase();
  if (normalizedStatus === "MAINTENANCE") {
    return '<span class="badge bg-warning text-dark">IN MANUTENZIONE</span>';
  }
  if (normalizedStatus === "OCCUPIED") {
    return '<span class="badge bg-danger">IN USO</span>';
  }
  if (normalizedStatus === "FREE") {
    return '<span class="badge bg-success">FREE</span>';
  }
  return '<span class="badge bg-secondary">N/A</span>';
}

function normalizeMachinePayload(payload) {
  if (Array.isArray(payload)) {
    return payload;
  }
  if (payload && Array.isArray(payload.list)) {
    return payload.list.map((item) => (item && item.map ? item.map : item));
  }
  if (payload && Array.isArray(payload.machines)) {
    return payload.machines;
  }
  return [];
}

function renderMaintenanceRows() {
  if (!maintenanceBody) {
    return;
  }

  if (!Array.isArray(internalMachines) || internalMachines.length === 0) {
    maintenanceBody.innerHTML =
      '<tr><td colspan="4" class="text-secondary">Nessun macchinario disponibile.</td></tr>';
    return;
  }

  maintenanceBody.innerHTML = [...internalMachines]
    .sort((left, right) => {
      const leftId = left?.machineId ?? left?.id ?? "";
      const rightId = right?.machineId ?? right?.id ?? "";
      return compareAlphabetically(leftId, rightId);
    })
    .map((machine, index) => {
      const machineId = String(machine.machineId ?? machine.id ?? "");
      const status = String(machine.status ?? "FREE");
      const normalizedStatus = status.toUpperCase();
      const inMaintenance = normalizedStatus === "MAINTENANCE";
      const occupied = normalizedStatus === "OCCUPIED";
      const canToggleMaintenance =
        Boolean(machineId) && (inMaintenance || !occupied);
      const actionLabel = inMaintenance ? "Fine manutenzione" : "Manutenzione";
      const disabledHint =
        occupied && !inMaintenance ? "Macchinario in uso" : "";

      return `
      <tr>
        <td>${index + 1}</td>
        <td>${escapeHtml(machineId || "-")}</td>
        <td>${machineStatusBadge(status)}</td>
        <td>
          <button
            class="btn btn-sm btn-outline-warning btn-maintenance-action"
            type="button"
            data-maintenance-id="${escapeHtml(machineId)}"
            data-maintenance-active="${inMaintenance ? "false" : "true"}"
            aria-label="${escapeHtml(actionLabel)} ${escapeHtml(machineId)}"
            ${canToggleMaintenance ? "" : "disabled"}
            ${disabledHint ? `title="${escapeHtml(disabledHint)}"` : ""}
          >
            ${escapeHtml(disabledHint || actionLabel)}
          </button>
        </td>
      </tr>
    `;
    })
    .join("");
}

function renderKpis(attendance, session) {
  if (
    !kpiUsersInside ||
    !kpiUsersEntered ||
    !kpiUsersExited ||
    !kpiAverageSession
  ) {
    return;
  }

  const gymCount = Number(attendance?.gymCount ?? 0);
  const totalEntries = Number(attendance?.totalEntries ?? 0);
  const totalExits = Number(attendance?.totalExits ?? 0);
  const avgDuration = Number(session?.averageDurationMinutes ?? 0);

  kpiUsersInside.textContent = `${Number.isFinite(gymCount) ? gymCount : 0} utenti`;
  kpiUsersEntered.textContent = `${Number.isFinite(totalEntries) ? totalEntries : 0} utenti`;
  kpiUsersExited.textContent = `${Number.isFinite(totalExits) ? totalExits : 0} utenti`;
  kpiAverageSession.textContent = `${Number.isFinite(avgDuration) ? avgDuration.toFixed(1) : "0.0"} minuti`;
}

async function loadKpis() {
  try {
    const response = await fetch(ENDPOINTS.analyticsDashboard, {
      method: "GET",
      headers: { Accept: "application/json" },
    });

    if (!response.ok) {
      return false;
    }

    const payload = await response.json();
    renderKpis(payload.attendance ?? {}, payload.session ?? {});
    return true;
  } catch (error) {
    console.error("Error loading KPIs:", error);
    return false;
  }
}

async function loadMachines(showLoading = false) {
  if (!maintenanceBody) {
    return false;
  }

  try {
    if (showLoading) {
      setFeedback(maintenanceFeedback, "Caricamento macchinari...");
    }
    const response = await fetch(ENDPOINTS.machines, {
      method: "GET",
      headers: { Accept: "application/json" },
    });

    if (!response.ok) {
      if (response.status === 401) {
        setFeedback(maintenanceFeedback, MESSAGE_SESSION_EXPIRED);
        return false;
      }
      setFeedback(
        maintenanceFeedback,
        `Errore caricamento macchinari (${response.status}).`,
      );
      return false;
    }

    const payload = await response.json();
    internalMachines = normalizeMachinePayload(payload);
    renderMaintenanceRows();
    setFeedback(maintenanceFeedback, "");
    return true;
  } catch (error) {
    setFeedback(maintenanceFeedback, `Errore di rete: ${error}`);
    return false;
  }
}

async function toggleMaintenance(deviceId, active) {
  if (!deviceId || typeof active !== "boolean") {
    return;
  }

  try {
    const response = await fetch(ENDPOINTS.maintenanceToggle, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({ machineId: deviceId, active }),
    });

    if (!response.ok) {
      const errorMessage = await extractErrorMessage(
        response,
        `Errore aggiornamento manutenzione (${response.status}).`,
      );
      setFeedback(maintenanceFeedback, errorMessage);
      return;
    }

    await loadMachines();
  } catch (error) {
    setFeedback(maintenanceFeedback, `Errore di rete: ${error}`);
  }
}

function renderStatuses(statuses) {
  if (!Array.isArray(statuses) || statuses.length === 0) {
    statusesBody.innerHTML =
      '<tr><td colspan="5" class="text-secondary">Nessuno stato disponibile al momento.</td></tr>';
    setFeedback(statusesFeedback, "");
    return;
  }

  statusesBody.innerHTML = [...statuses]
    .sort((left, right) =>
      compareAlphabetically(left?.deviceId, right?.deviceId),
    )
    .map(
      (status, index) => `
      <tr>
        <td>${index + 1}</td>
        <td>${escapeHtml(status.deviceId)}</td>
        <td>${escapeHtml(status.deviceType)}</td>
        <td>${statusBadge(status.online)}</td>
        <td>${escapeHtml(status.timeStamp)}</td>
      </tr>
    `,
    )
    .join("");

  setFeedback(statusesFeedback, "");
}

async function loadStatuses(showLoading = false) {
  try {
    if (showLoading) {
      setFeedback(statusesFeedback, "Caricamento stati...");
    }
    const response = await fetch(ENDPOINTS.statuses, {
      method: "GET",
      headers: { Accept: "application/json" },
    });

    if (!response.ok) {
      if (response.status === 401) {
        setFeedback(statusesFeedback, MESSAGE_SESSION_EXPIRED);
        return false;
      }
      setFeedback(
        statusesFeedback,
        `Errore caricamento stati (${response.status}).`,
      );
      return false;
    }

    const payload = await response.json();
    renderStatuses(payload.statuses || []);
    return true;
  } catch (error) {
    setFeedback(statusesFeedback, `Errore di rete: ${error}`);
    return false;
  }
}

async function refreshDashboardData(showLoading = false) {
  if (dashboardRefreshInFlight) {
    return;
  }

  dashboardRefreshInFlight = true;
  try {
    const results = await Promise.all([
      loadStatuses(showLoading),
      loadKpis(),
      loadMachines(showLoading),
    ]);

    if (results.some(Boolean)) {
      setLastUpdateLabel();
    }
  } finally {
    dashboardRefreshInFlight = false;
  }
}

if (refreshButton) {
  refreshButton.addEventListener("click", () => refreshDashboardData(true));
}

if (maintenanceBody) {
  maintenanceBody.addEventListener("click", async (event) => {
    const target = event.target;
    if (!(target instanceof HTMLElement)) {
      return;
    }

    const button = target.closest("button[data-maintenance-id]");
    if (!button) {
      return;
    }

    const deviceId = button.getAttribute("data-maintenance-id");
    const active = button.getAttribute("data-maintenance-active") === "true";
    await toggleMaintenance(deviceId, active);
  });
}

renderMaintenanceRows();
refreshDashboardData(true);

setInterval(() => {
  refreshDashboardData(false);
}, DASHBOARD_REFRESH_MS);
