const statusesBody = document.getElementById("statuses-body");
const statusesFeedback = document.getElementById("statuses-feedback");
const refreshButton = document.getElementById("refresh-statuses");

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

function renderStatuses(statuses) {
  if (!Array.isArray(statuses) || statuses.length === 0) {
    statusesBody.innerHTML = "";
    statusesFeedback.textContent = "Nessuno stato disponibile al momento.";
    return;
  }

  statusesBody.innerHTML = statuses
    .map(
      (status) => `
	  <tr>
		<td>${escapeHtml(status.deviceId)}</td>
		<td>${escapeHtml(status.deviceType)}</td>
		<td>${statusBadge(status.online)}</td>
		<td>${escapeHtml(status.statusDetail)}</td>
		<td>${escapeHtml(status.timeStamp)}</td>
	  </tr>
	`,
    )
    .join("");

  statusesFeedback.textContent = `Ultimo aggiornamento: ${new Date().toLocaleTimeString()} (${statuses.length} dispositivi)`;
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

loadStatuses();
setInterval(loadStatuses, 10000);
