document.addEventListener("DOMContentLoaded", function () {
  function showToast(message) {
    if (window.OpenStudy) {
      window.OpenStudy.toast(message);
    }
  }

  document
    .querySelectorAll('[data-action="show-details"]')
    .forEach(function (button) {
      button.addEventListener("click", function () {
        const item = button.getAttribute("data-item") || "this item";
        showToast("Details for " + item + " are coming soon");
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
        button.querySelector("[data-label]").textContent = opening
          ? "Close editor"
          : "Edit profile";
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
          editButton.querySelector("[data-label]").textContent = "Edit profile";
          editButton.focus();
        }
      });
    });
});
