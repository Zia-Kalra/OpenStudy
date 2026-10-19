(function () {
    var toggle = document.getElementById("sidebarToggle");
    var sidebar = document.getElementById("teacherSidebar");
    var close = document.getElementById("sidebarClose");
    if (!toggle || !sidebar || !close) {
        return;
    }
    var mobile = window.matchMedia("(max-width: 860px)");
    function setOpen(open, restoreFocus) {
        var visible = !mobile.matches || open;
        sidebar.classList.toggle("open", mobile.matches && open);
        sidebar.inert = !visible;
        sidebar.setAttribute("aria-hidden", String(!visible));
        toggle.setAttribute("aria-expanded", String(mobile.matches && open));
        if (mobile.matches && open) {
            close.focus();
        } else if (restoreFocus && mobile.matches) {
            toggle.focus();
        }
    }
    toggle.addEventListener("click", function () {
        setOpen(!sidebar.classList.contains("open"), true);
    });
    close.addEventListener("click", function () { setOpen(false, true); });
    document.addEventListener("click", function (event) {
        if (mobile.matches && sidebar.classList.contains("open")
                && !sidebar.contains(event.target) && !toggle.contains(event.target)) {
            setOpen(false, true);
        }
    });
    document.addEventListener("keydown", function (event) {
        if (!mobile.matches || !sidebar.classList.contains("open")) {
            return;
        }
        if (event.key === "Escape") {
            event.preventDefault();
            setOpen(false, true);
        } else if (event.key === "Tab") {
            var controls = sidebar.querySelectorAll("button, a[href]");
            var first = controls[0];
            var last = controls[controls.length - 1];
            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault();
                first.focus();
            }
        }
    });
    mobile.addEventListener("change", function () {
        setOpen(false, sidebar.contains(document.activeElement));
        if (!mobile.matches && document.activeElement === close) {
            sidebar.querySelector("a[href]").focus();
        }
    });
    setOpen(false, false);
})();
