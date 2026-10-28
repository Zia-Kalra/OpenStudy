/* OpenStudy analytics - shared helpers (Member 4). Exposes window.OSA.
   Everything rendered from server data goes through textContent / DOM nodes, never innerHTML. */
(function () {
    "use strict";
    var ctx = document.body.getAttribute("data-context") || "";
    // Keys are chart roles kept for compatibility; values follow the openstudy.css palette.
    var COLORS = { blue: "#6d4fd8", green: "#4fa36b", purple: "#a596e8", orange: "#e0a04a", red: "#c74343", grey: "#c4c4cc" };
    if (window.Chart) {
        Chart.defaults.font.family = '"Work Sans", system-ui, sans-serif';
        Chart.defaults.color = "#6e6a82";
        Chart.defaults.borderColor = "#ece8f6";
    }
    var charts = {};

    function qs(params) {
        var p = [];
        Object.keys(params || {}).forEach(function (k) {
            var v = params[k];
            if (v !== null && v !== undefined && v !== "") { p.push(encodeURIComponent(k) + "=" + encodeURIComponent(v)); }
        });
        return p.length ? "?" + p.join("&") : "";
    }

    /** GET /api/analytics<path>; resolves with the envelope's data or rejects with Error(message). */
    function api(path, params) {
        return fetch(ctx + "/api/analytics" + path + qs(params), { headers: { Accept: "application/json" }, credentials: "same-origin" })
            .then(function (res) {
                return res.json().catch(function () { return {}; }).then(function (body) {
                    if (!res.ok || body.success === false) {
                        var err = new Error(body.message || "Request failed (" + res.status + ").");
                        err.status = res.status;
                        throw err;
                    }
                    return body;
                });
            });
    }

    function el(tag, attrs) {
        var node = document.createElement(tag);
        if (attrs) {
            Object.keys(attrs).forEach(function (k) {
                if (k === "class") { node.className = attrs[k]; }
                else if (k === "text") { node.textContent = attrs[k]; }
                else { node.setAttribute(k, attrs[k]); }
            });
        }
        function add(c) {
            if (c === null || c === undefined || c === false) { return; }
            if (Array.isArray(c)) { c.forEach(add); return; }
            node.appendChild(typeof c === "string" || typeof c === "number" ? document.createTextNode(String(c)) : c);
        }
        for (var i = 2; i < arguments.length; i++) { add(arguments[i]); }
        return node;
    }
    function clear(node) { while (node && node.firstChild) { node.removeChild(node.firstChild); } return node; }
    function byId(id) { return document.getElementById(id); }

    function pct(v, d) { return v === null || v === undefined ? "\u2014" : Number(v).toFixed(d === undefined ? 1 : d) + "%"; }
    function num(v, d) { return v === null || v === undefined ? "\u2014" : Number(v).toFixed(d === undefined ? 1 : d); }
    function whole(v) { return v === null || v === undefined ? "\u2014" : String(v); }
    function date(iso) {
        if (!iso) { return "\u2014"; }
        var d = new Date(iso);
        return isNaN(d) ? "\u2014" : d.toLocaleDateString(undefined, { day: "2-digit", month: "short", year: "numeric" });
    }
    function monthLabel(ym) {
        var p = String(ym).split("-");
        var d = new Date(Number(p[0]), Number(p[1]) - 1, 1);
        return isNaN(d) ? ym : d.toLocaleDateString(undefined, { month: "short", year: "2-digit" });
    }
    function isoDate(d) {
        return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0");
    }

    function chart(canvasId, config) {
        var canvas = byId(canvasId);
        if (!canvas) { return null; }
        if (charts[canvasId]) { charts[canvasId].destroy(); delete charts[canvasId]; }
        if (typeof Chart === "undefined") { return null; }
        config.options = Object.assign({ responsive: true, maintainAspectRatio: false }, config.options || {});
        charts[canvasId] = new Chart(canvas.getContext("2d"), config);
        return charts[canvasId];
    }
    function destroyChart(canvasId) { if (charts[canvasId]) { charts[canvasId].destroy(); delete charts[canvasId]; } }

    /** Replaces a card's chart with a friendly message when there is nothing to plot. */
    function emptyChart(canvasId, message) {
        destroyChart(canvasId);
        var canvas = byId(canvasId);
        if (!canvas) { return; }
        var box = canvas.parentNode;
        var existing = box.querySelector(".empty-box");
        if (existing) { box.removeChild(existing); }
        canvas.style.display = message ? "none" : "";
        if (message) { box.appendChild(el("div", { class: "empty-box", text: message })); }
    }
    function readyChart(canvasId) { emptyChart(canvasId, null); }

    function notice(container, kind, message) {
        clear(container);
        if (message) { container.appendChild(el("div", { class: "notice " + (kind || ""), role: "status", text: message })); }
    }

    /** Shows the standard explanation when an R-backed section is not available. */
    function rProblem(r) {
        if (!r || r.status === "OK") { return null; }
        return r.message || "This statistical analysis is not available.";
    }

    function badge(text, kind) { return el("span", { class: "badge " + (kind || "muted"), text: text }); }

    function table(headers, rows) {
        var thead = el("thead", null, el("tr", null, headers.map(function (h) { return el("th", { scope: "col", text: h }); })));
        var tbody = el("tbody");
        rows.forEach(function (cells) {
            tbody.appendChild(el("tr", null, cells.map(function (c) {
                return el("td", null, c instanceof Node ? c : document.createTextNode(c === null || c === undefined ? "\u2014" : String(c)));
            })));
        });
        return el("div", { class: "table-wrap" }, el("table", { class: "data" }, thead, tbody));
    }

    function tabs(root) {
        var buttons = Array.prototype.slice.call(root.querySelectorAll("[role=tab]"));
        function select(btn) {
            buttons.forEach(function (b) {
                var on = b === btn;
                b.setAttribute("aria-selected", String(on));
                b.tabIndex = on ? 0 : -1;
                var panel = byId(b.getAttribute("aria-controls"));
                if (panel) { panel.hidden = !on; }
            });
            if (typeof root.onTab === "function") { root.onTab(btn.getAttribute("data-tab")); }
            window.dispatchEvent(new Event("resize"));   // lets Chart.js size charts that were hidden
        }
        buttons.forEach(function (b, i) {
            b.addEventListener("click", function () { select(b); });
            b.addEventListener("keydown", function (e) {
                var next = e.key === "ArrowRight" ? i + 1 : e.key === "ArrowLeft" ? i - 1 : null;
                if (next === null) { return; }
                e.preventDefault();
                var target = buttons[(next + buttons.length) % buttons.length];
                target.focus(); select(target);
            });
        });
        return { select: select };
    }

    function showError(container, err) {
        var msg = err && err.status === 401 ? "Please sign in to view analytics."
                : err && err.status === 403 ? (err.message || "You do not have access to this data.")
                : (err && err.message) || "Analytics could not be loaded.";
        notice(container, "error", msg);
    }

    window.OSA = { COLORS: COLORS, api: api, el: el, clear: clear, byId: byId, pct: pct, num: num, whole: whole,
        date: date, monthLabel: monthLabel, isoDate: isoDate, chart: chart, emptyChart: emptyChart, readyChart: readyChart,
        notice: notice, rProblem: rProblem, badge: badge, table: table, tabs: tabs, showError: showError };
})();
