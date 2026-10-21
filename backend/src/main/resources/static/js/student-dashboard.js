document.addEventListener("DOMContentLoaded", function () {
  const sidebar = document.getElementById("studentSidebar");
  const toggleButtons = document.querySelectorAll(
    '[data-action="toggle-sidebar"]',
  );
  const toast = document.createElement("div");
  toast.className = "student-toast";
  document.body.appendChild(toast);

  function showToast(message) {
    toast.textContent = message;
    toast.classList.add("show");
    window.clearTimeout(showToast.timer);
    showToast.timer = window.setTimeout(function () {
      toast.classList.remove("show");
    }, 1800);
  }

  toggleButtons.forEach(function (button) {
    button.addEventListener("click", function () {
      if (!sidebar) {
        return;
      }
      sidebar.classList.toggle("open");
    });
  });

  if (sidebar) {
    document.addEventListener("click", function (event) {
      const clickedInside =
        sidebar.contains(event.target) ||
        Array.from(toggleButtons).some(function (button) {
          return button.contains(event.target);
        });
      if (!clickedInside && sidebar.classList.contains("open")) {
        sidebar.classList.remove("open");
      }
    });
  }

  document
    .querySelectorAll('[data-action="show-details"]')
    .forEach(function (button) {
      button.addEventListener("click", function () {
        const item = button.getAttribute("data-item") || "this item";
        showToast("Showing details for " + item);
      });
    });

  document
    .querySelectorAll('[data-action="toggle-flag"]')
    .forEach(function (button) {
      button.addEventListener("click", function () {
        const flagged = button.classList.toggle("is-flagged");
        button.setAttribute("aria-pressed", String(flagged));
        showToast(flagged ? "Item flagged" : "Flag removed");
      });
    });

  document
    .querySelectorAll('[data-action="edit-profile"]')
    .forEach(function (button) {
      button.addEventListener("click", function () {
        const form = document.getElementById(
          button.getAttribute("aria-controls"),
        );
        if (!form) {
          return;
        }
        const opening = form.hidden;
        form.hidden = !opening;
        button.setAttribute("aria-expanded", String(opening));
        button.textContent = opening ? "Close Editor" : "Edit Profile";
        if (opening) {
          form.querySelector("input")?.focus();
        }
      });
    });

  document
    .querySelectorAll('[data-action="cancel-profile"]')
    .forEach(function (button) {
      button.addEventListener("click", function () {
        const form = button.closest("form");
        const editButton = document.querySelector(
          '[data-action="edit-profile"]',
        );
        if (form) {
          form.reset();
          form.hidden = true;
        }
        if (editButton) {
          editButton.setAttribute("aria-expanded", "false");
          editButton.textContent = "Edit Profile";
        }
      });
    });

  document.querySelectorAll(".role-btn").forEach(function (button) {
    button.addEventListener("click", function () {
      const group = button.closest(".role-picker");
      if (!group) {
        return;
      }
      group.querySelectorAll(".role-btn").forEach(function (item) {
        item.classList.remove("active");
      });
      button.classList.add("active");
    });
  });
});
