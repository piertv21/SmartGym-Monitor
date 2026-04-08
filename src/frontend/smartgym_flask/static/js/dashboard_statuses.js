const statusesBody = document.getElementById("statuses-body");
const statusesFeedback = document.getElementById("statuses-feedback");
const refreshButton = document.getElementById("refresh-statuses");
const maintenanceBody = document.getElementById("maintenance-body");
const maintenanceFeedback = document.getElementById("maintenance-feedback");

const kpiUsersInside = document.getElementById("kpi-users-inside");
const kpiUsersEntered = document.getElementById("kpi-users-entered");
const kpiUsersExited = document.getElementById("kpi-users-exited");
const kpiAverageSession = document.getElementById("kpi-average-session");

const maintenanceState = new Map([
  ["Treadmill 1", false],
  ["Treadmill 2", false],
  ["Bike 1", true],
  ["Ellittica 1", false],
  ["Rower 1", false],
  ["Chest Press 1", false],
  ["Rower 12", false],
  ["Rower 13", false],
  ["Rower 14", false],
  ["Rower 15", false],
  ["Rower 16", false],
]);

function statusBadge(online) {
  if (online === true) {
    return '<span class="badge text-bg-success">ONLINE</span>';
  }
  if (online === false) {
    return '<span class="badge text-bg-danger">OFFLINE</span>';
  }
  return '<span class="badge text-bg-secondary">N/A</span>';
}

function escapeHtml(value) {
  return String(value ?? "-")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function toggleMaintenance(deviceId) {
  if (!deviceId) {
    return;
  }

  const currentValue = maintenanceState.get(deviceId) === true;
  maintenanceState.set(deviceId, !currentValue);
  renderMaintenanceRows();
}

function renderMaintenanceRows() {
  if (!maintenanceBody) {
    return;
  }

  const rows = Array.from(maintenanceState.entries());
  if (rows.length === 0) {
    maintenanceBody.innerHTML =
      '<tr><td colspan="3" class="muted">Nessun macchinario disponibile.</td></tr>';
    return;
  }

  maintenanceBody.innerHTML = rows
    .map(([deviceId, inMaintenance]) => {
      const actionLabel = inMaintenance ? "Fine manutenzione" : "Manutenzione";
      const statusLabel = inMaintenance ? "In manutenzione" : "FREE";
      return `
      <tr>
        <td>${escapeHtml(deviceId)}</td>
        <td class="fw-semibold">${escapeHtml(statusLabel)}</td>
        <td>
          <button
            class="btn btn-outline-secondary btn-sm"
            type="button"
            data-maintenance-id="${escapeHtml(deviceId)}"
            aria-label="${escapeHtml(actionLabel)} ${escapeHtml(deviceId)}"
          >
            ${escapeHtml(actionLabel)}
          </button>
        </td>
      </tr>
    `;
    })
    .join("");
}

function updateKpis(statuses) {
  if (
    !kpiUsersInside ||
    !kpiUsersEntered ||
    !kpiUsersExited ||
    !kpiAverageSession
  ) {
    return;
  }

  const onlineDevices = statuses.filter(
    (status) => status.online === true,
  ).length;
  const totalDevices = statuses.length;
  const estimatedInside = Math.max(onlineDevices, 15);
  const estimatedEntered = Math.max(totalDevices * 2, 30);
  const estimatedExited = Math.max(estimatedEntered - estimatedInside, 15);

  kpiUsersInside.textContent = `${estimatedInside} utenti`;
  kpiUsersEntered.textContent = `${estimatedEntered} utenti`;
  kpiUsersExited.textContent = `${estimatedExited} utenti`;
  kpiAverageSession.textContent = "4.3 minuti";
}

function updateMaintenance(statuses) {
  if (!maintenanceBody || !maintenanceFeedback) {
    return;
  }

  const machines = statuses.filter((status) => {
    const deviceType = String(status.deviceType ?? "").toUpperCase();
    return deviceType.includes("MACHINE") || deviceType.includes("MACCH");
  });

  if (machines.length === 0) {
    // Nessun macchinario dall'API: mantieni i dati demo già presenti
    maintenanceFeedback.textContent =
      "Nessun macchinario rilevato dall'API. Dati demo visualizzati.";
    return;
  }

  for (const machine of machines) {
    const deviceId = String(machine.deviceId ?? "");
    if (!deviceId || maintenanceState.has(deviceId)) {
      continue;
    }

    maintenanceState.set(deviceId, false);
  }

  renderMaintenanceRows();
  maintenanceFeedback.textContent = `${machines.length} macchinari monitorati.`;
}

function renderStatuses(statuses) {
  if (!Array.isArray(statuses) || statuses.length === 0) {
    statusesBody.innerHTML =
      '<tr><td colspan="5" class="muted">Nessuno stato disponibile al momento.</td></tr>';
    statusesFeedback.textContent = "Nessuno stato disponibile al momento.";
    updateKpis([]);
    updateMaintenance([]);
    return;
  }

  statusesBody.innerHTML = statuses
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

  statusesFeedback.textContent = `Ultimo aggiornamento: ${new Date().toLocaleTimeString()} (${statuses.length} dispositivi)`;
  updateKpis(statuses);
  updateMaintenance(statuses);
}

async function loadStatuses() {
  try {
    statusesFeedback.textContent = "Caricamento stati...";
    const response = await fetch("/api/statuses", {
      method: "GET",
      headers: { Accept: "application/json" },
    });

    if (!response.ok) {
      if (response.status === 401) {
        statusesFeedback.textContent =
          "Sessione scaduta. Esegui di nuovo il login.";
        return;
      }
      statusesFeedback.textContent = `Errore caricamento stati (${response.status}).`;
      return;
    }

    const payload = await response.json();
    renderStatuses(payload.statuses || []);
  } catch (error) {
    statusesFeedback.textContent = `Errore di rete: ${error}`;
  }
}

if (refreshButton) {
  refreshButton.addEventListener("click", loadStatuses);
}

if (maintenanceBody) {
  maintenanceBody.addEventListener("click", (event) => {
    const target = event.target;
    if (!(target instanceof HTMLElement)) {
      return;
    }

    const button = target.closest("button[data-maintenance-id]");
    if (!button) {
      return;
    }

    const deviceId = button.getAttribute("data-maintenance-id");
    toggleMaintenance(deviceId);
  });
}

renderMaintenanceRows();
if (maintenanceFeedback) {
  maintenanceFeedback.textContent = `${maintenanceState.size} macchinari monitorati (demo).`;
}

loadStatuses();
setInterval(loadStatuses, 10000);
