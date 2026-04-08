(function () {
  const STORAGE_KEY = "sg-theme";

  function applyUI(theme) {
    document.querySelectorAll("[data-theme-toggle]").forEach((btn) => {
      const icon = btn.querySelector("i");
      if (!icon) return;
      if (theme === "dark") {
        icon.className = "bi bi-sun-fill";
        btn.setAttribute("aria-label", "Passa alla modalità chiara");
        btn.setAttribute("title", "Light mode");
      } else {
        icon.className = "bi bi-moon-stars-fill";
        btn.setAttribute("aria-label", "Passa alla modalità scura");
        btn.setAttribute("title", "Dark mode");
      }
    });
  }

  function setTheme(theme) {
    document.documentElement.setAttribute("data-bs-theme", theme);
    localStorage.setItem(STORAGE_KEY, theme);
    applyUI(theme);
  }

  const saved = localStorage.getItem(STORAGE_KEY) || "dark";
  setTheme(saved);

  document.addEventListener("DOMContentLoaded", function () {
    applyUI(document.documentElement.getAttribute("data-bs-theme") || "dark");

    document.querySelectorAll("[data-theme-toggle]").forEach((btn) => {
      btn.addEventListener("click", function () {
        const current = document.documentElement.getAttribute("data-bs-theme");
        setTheme(current === "dark" ? "light" : "dark");
      });
    });
  });
})();
