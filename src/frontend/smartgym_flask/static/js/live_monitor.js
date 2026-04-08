const liveAreasContainer = document.getElementById("live-monitor-areas");
const liveFeedback = document.getElementById("live-feedback");
const liveLastUpdate = document.getElementById("live-last-update");
const refreshLiveMonitorButton = document.getElementById(
  "refresh-live-monitor",
);

const LIVE_REFRESH_MS = 5000;
let liveRefreshInFlight = false;

function escapeHtml(value) {
  return String(value ?? "-")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function setLiveFeedback(message) {
  if (!liveFeedback) {
    return;
  }
  liveFeedback.textContent = message;
}

function setLastUpdateLabel(date = new Date()) {
  if (!liveLastUpdate) {
    return;
  }
  liveLastUpdate.textContent = `Ultimo aggiornamento: ${date.toLocaleTimeString()}`;
}

function machineBoxClass(status) {
  const normalizedStatus = String(status ?? "").toUpperCase();
  if (normalizedStatus === "OCCUPIED") {
    return "bg-danger";
  }
  if (normalizedStatus === "MAINTENANCE") {
    return "bg-warning";
  }
  return "bg-success";
}

function machineLabel(machineId) {
  return String(machineId ?? "-")
    .replaceAll("-", " ")
    .replaceAll("_", " ");
}

function renderMachines(machines) {
  if (!Array.isArray(machines) || machines.length === 0) {
    return '<div class="card-body h-100 d-flex flex-column justify-content-center align-items-center text-center p-3"><span class="text-secondary small">Nessun macchinario in quest\'area</span></div>';
  }

  return `<div class="card-body d-flex flex-wrap align-content-center gap-4 justify-content-center py-4">${machines
    .map(
      (machine) => `
      <div class="equipment-item text-center">
        <div class="equipment-box ${machineBoxClass(machine.status)} border border-dark rounded-1 mb-1 shadow-sm"></div>
        <div class="equipment-label">${escapeHtml(machineLabel(machine.machineId))}</div>
      </div>
    `,
    )
    .join("")}</div>`;
}

function renderAreas(areas) {
  if (!liveAreasContainer) {
    return;
  }

  if (!Array.isArray(areas) || areas.length === 0) {
    liveAreasContainer.innerHTML = `
      <div class="col-12">
        <div class="card live-area-card">
          <div class="card-body text-secondary small">Nessuna area disponibile al momento.</div>
        </div>
      </div>
    `;
    return;
  }

  liveAreasContainer.innerHTML = areas
    .map((area) => {
      const areaName = String(area.name ?? area.areaId ?? "Area");
      const currentUsers = Number.isFinite(Number(area.currentUsers))
        ? Number(area.currentUsers)
        : 0;
      const capacity = Number.isFinite(Number(area.capacity))
        ? Number(area.capacity)
        : 0;
      const occupancyPercentRaw = Number(area.occupancyPercent);
      const occupancyPercent = Number.isFinite(occupancyPercentRaw)
        ? Math.max(0, Math.min(100, occupancyPercentRaw))
        : 0;

      return `
      <div class="col-12 col-md-6 col-xl d-flex flex-column mb-0">
        <h2 class="h6 text-center text-uppercase fw-normal mb-3">${escapeHtml(areaName)}</h2>
        <div class="card live-progress-card mb-3">
          <div class="card-body">
            <div class="live-progress-container p-3 d-flex align-items-center justify-content-between">
              <div class="progress-circle" style="--p: ${occupancyPercent}%">
                <span>${occupancyPercent.toFixed(1)}%</span>
              </div>
              <div class="text-end">
                <div class="fs-4 fw-bold lh-1">
                  ${currentUsers}<span class="fs-6 text-muted fw-normal">/${capacity}</span>
                </div>
                <div class="text-muted small" style="font-size: 0.65rem">Current Users</div>
              </div>
            </div>
          </div>
        </div>
        <div class="card live-area-card flex-grow-1">
          ${renderMachines(area.machines)}
        </div>
      </div>
    `;
    })
    .join("");
}

async function loadLiveMonitor(showLoading = false) {
  try {
    if (showLoading) {
      setLiveFeedback("Caricamento live monitor...");
    }

    const response = await fetch("/api/live-monitor", {
      method: "GET",
      headers: { Accept: "application/json" },
    });

    if (!response.ok) {
      if (response.status === 401) {
        setLiveFeedback("Sessione scaduta. Esegui di nuovo il login.");
        return false;
      }
      setLiveFeedback(`Errore caricamento live monitor (${response.status}).`);
      return false;
    }

    const payload = await response.json();
    renderAreas(payload.areas || []);
    setLastUpdateLabel();
    setLiveFeedback("");
    return true;
  } catch (error) {
    setLiveFeedback(`Errore di rete: ${error}`);
    return false;
  }
}

async function refreshLiveMonitor(showLoading = false) {
  if (liveRefreshInFlight) {
    return;
  }

  liveRefreshInFlight = true;
  try {
    await loadLiveMonitor(showLoading);
  } finally {
    liveRefreshInFlight = false;
  }
}

if (refreshLiveMonitorButton) {
  refreshLiveMonitorButton.addEventListener("click", () =>
    refreshLiveMonitor(true),
  );
}

refreshLiveMonitor(true);
setInterval(() => {
  refreshLiveMonitor(false);
}, LIVE_REFRESH_MS);
