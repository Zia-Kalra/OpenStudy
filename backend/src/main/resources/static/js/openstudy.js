/* OpenStudy shell behaviour: collapsible top nav, user menu, in-page search and toasts.
   Loaded by the nav fragments, so every page that shows the nav gets it exactly once. */
(function () {
  if (window.OpenStudy) {
    return;
  }
  var OpenStudy = (window.OpenStudy = {});

  var toast;
  OpenStudy.toast = function (message) {
    if (!toast) {
      toast = document.createElement("div");
      toast.className = "os-toast";
      toast.setAttribute("role", "status");
      document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.classList.add("is-visible");
    window.clearTimeout(OpenStudy.toast.timer);
    OpenStudy.toast.timer = window.setTimeout(function () {
      toast.classList.remove("is-visible");
    }, 2000);
  };

  function initNav() {
    var nav = document.querySelector(".os-nav");
    var toggle = nav && nav.querySelector("[data-os-nav-toggle]");
    if (!toggle) {
      return;
    }
    function setOpen(open) {
      nav.classList.toggle("is-open", open);
      toggle.setAttribute("aria-expanded", String(open));
      toggle.setAttribute("aria-label", open ? "Close navigation" : "Open navigation");
    }
    toggle.addEventListener("click", function () {
      setOpen(!nav.classList.contains("is-open"));
    });
    document.addEventListener("keydown", function (event) {
      if (event.key === "Escape" && nav.classList.contains("is-open")) {
        setOpen(false);
        toggle.focus();
      }
    });
    document.addEventListener("click", function (event) {
      if (nav.classList.contains("is-open") && !nav.contains(event.target)) {
        setOpen(false);
      }
    });
    window.matchMedia("(min-width: 1251px)").addEventListener("change", function (query) {
      if (query.matches) {
        setOpen(false);
      }
    });
  }

  function initUserMenu() {
    var button = document.querySelector("[data-os-menu-button]");
    var menu = button && document.getElementById(button.getAttribute("aria-controls"));
    if (!menu) {
      return;
    }
    function setOpen(open, restoreFocus) {
      menu.hidden = !open;
      button.setAttribute("aria-expanded", String(open));
      if (open) {
        menu.querySelector("a").focus();
      } else if (restoreFocus) {
        button.focus();
      }
    }
    button.addEventListener("click", function () {
      setOpen(menu.hidden, false);
    });
    document.addEventListener("click", function (event) {
      if (!menu.hidden && !menu.contains(event.target) && !button.contains(event.target)) {
        setOpen(false, false);
      }
    });
    document.addEventListener("keydown", function (event) {
      if (event.key === "Escape" && !menu.hidden) {
        setOpen(false, true);
      }
    });
    menu.addEventListener("focusout", function (event) {
      if (!menu.hidden && event.relatedTarget && !menu.contains(event.relatedTarget)
          && !button.contains(event.relatedTarget)) {
        setOpen(false, false);
      }
    });
  }

  // Rows marked data-os-searchable are filtered live. Pages without such rows submit
  // the search to the dashboard, which lists both quizzes and assignments.
  function initSearch() {
    var form = document.querySelector("[data-os-search]");
    var input = form && form.querySelector("input");
    var rows = Array.prototype.slice.call(document.querySelectorAll("[data-os-searchable]"));
    if (!input || rows.length === 0) {
      return;
    }
    var groups = [];
    rows.forEach(function (row) {
      if (groups.indexOf(row.parentElement) === -1) {
        groups.push(row.parentElement);
      }
    });

    function emptyNoteFor(group) {
      if (!group.osEmptyNote) {
        var note = document.createElement("p");
        note.className = "os-empty";
        note.hidden = true;
        var anchor = group.tagName === "TBODY" ? group.closest("table") : group;
        anchor.insertAdjacentElement("afterend", note);
        group.osEmptyNote = note;
      }
      return group.osEmptyNote;
    }

    function apply(query) {
      var needle = query.trim().toLowerCase();
      groups.forEach(function (group) {
        var visible = 0;
        Array.prototype.forEach.call(group.children, function (row) {
          if (!row.hasAttribute("data-os-searchable")) {
            return;
          }
          var match = !needle || row.textContent.toLowerCase().indexOf(needle) !== -1;
          row.hidden = !match;
          if (match) {
            visible += 1;
          }
        });
        var note = emptyNoteFor(group);
        note.textContent = 'Nothing matches "' + query.trim() + '".';
        note.hidden = visible > 0;
      });
    }

    form.addEventListener("submit", function (event) {
      event.preventDefault();
      apply(input.value);
    });
    input.addEventListener("input", function () {
      apply(input.value);
    });
    var initial = new URLSearchParams(window.location.search).get("q");
    if (initial) {
      input.value = initial;
      apply(initial);
    }
  }

  function init() {
    initNav();
    initUserMenu();
    initSearch();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
