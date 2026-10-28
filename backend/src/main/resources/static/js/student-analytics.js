/* Student Performance page (Member 4). Data comes only from /api/analytics/student/**. */
(function () {
    "use strict";
    var O = window.OSA, C = O.COLORS, el = O.el;
    var state = { months: 6, subjectId: "" };
    var subjectsLoaded = false;

    function params(extra) {
        var p = { subjectId: state.subjectId || null };
        if (state.months > 0) {
            var d = new Date();
            d.setMonth(d.getMonth() - state.months);
            p.from = O.isoDate(d);
        }
        return Object.assign(p, extra || {});
    }

    function kpi(label, value, small, accent) {
        return el("article", { class: "kpi" + (accent ? " accent" : "") },
            el("span", { class: "kpi-label", text: label }), el("strong", { text: value }), small ? el("small", { text: small }) : null);
    }

    function renderKpis(ov) {
        var s = ov.summary, box = O.clear(O.byId("kpis"));
        box.appendChild(kpi("Average score", O.pct(s.average), "Mean of all completed results", true));
        box.appendChild(kpi("Quiz average", O.pct(s.quizAverage), s.quizCount + " quizzes completed"));
        box.appendChild(kpi("Assignment average", O.pct(s.assignmentAverage), s.assignmentCount + " assignments graded"));
        box.appendChild(kpi("Highest score", O.pct(s.max)));
        box.appendChild(kpi("Lowest score", O.pct(s.min)));
        box.appendChild(kpi("Class rank", ov.rank ? "#" + ov.rank.rank : "\u2014",
            ov.rank ? "of " + ov.rank.classSize + " students (top " + Math.max(1, Math.round(100 - ov.rank.percentile)) + "%)" : "Needs at least one result"));
        O.byId("averagingRule").textContent = ov.averagingRule || "";
    }

    function renderSubjectOptions(subjects) {
        var sel = O.byId("subjectSelect");
        if (subjectsLoaded) { return; }
        subjectsLoaded = true;
        subjects.forEach(function (s) { sel.appendChild(el("option", { value: String(s.subjectId), text: s.subjectName })); });
    }

    function renderTrend(trends) {
        var m = trends.monthly || [];
        if (!m.length) { O.emptyChart("trendChart", "No completed assessments in this period."); O.emptyChart("monthlyChart", "No completed assessments in this period."); return; }
        O.readyChart("trendChart"); O.readyChart("monthlyChart");
        var labels = m.map(function (x) { return O.monthLabel(x.month); });
        O.chart("trendChart", { type: "line", data: { labels: labels, datasets: [
            { label: "Quizzes", data: m.map(function (x) { return x.quizAverage; }), borderColor: C.blue, backgroundColor: C.blue, tension: .3, spanGaps: true },
            { label: "Assignments", data: m.map(function (x) { return x.assignmentAverage; }), borderColor: C.purple, backgroundColor: C.purple, tension: .3, spanGaps: true }] },
            options: { scales: { y: { min: 0, max: 100, title: { display: true, text: "Score (%)" } } } } });
        O.chart("monthlyChart", { type: "line", data: { labels: labels, datasets: [
            { label: "Overall average", data: m.map(function (x) { return x.average; }), borderColor: C.blue, backgroundColor: "rgba(59,130,246,.15)", fill: true, tension: .3 }] },
            options: { scales: { y: { min: 0, max: 100 } }, plugins: { legend: { display: false } } } });

        var box = O.clear(O.byId("trendAnalysis")), a = trends.analysis;
        var problem = O.rProblem(a);
        if (problem) { box.appendChild(el("div", { class: "notice", text: problem })); return; }
        var t = a.data.trend;
        if (t && t.status === "ok") {
            box.appendChild(el("p", { text: t.interpretation }));
            box.appendChild(el("div", { class: "kv" }, el("span", null, "Change per month: ", el("b", { text: O.num(t.slope_per_month) + " points" })),
                el("span", null, "p-value: ", el("b", { text: t.p_value < 0.001 ? "< 0.001" : O.num(t.p_value, 3) })), el("span", null, "R\u00b2: ", el("b", { text: O.num(t.r_squared, 3) }))));
            (t.limitations || []).forEach(function (l) { box.appendChild(el("p", { class: "sub", text: "Limitation: " + l })); });
        } else {
            box.appendChild(el("p", { class: "sub", text: (t && t.reason) || "A trend test needs at least 5 completed assessments." }));
        }
    }

    function renderSubjects(subjects) {
        var wrap = O.clear(O.byId("subjectCards"));
        if (!subjects.length) {
            O.emptyChart("subjectBarChart", "No completed assessments in this period.");
            wrap.appendChild(el("div", { class: "card empty-box", text: "No subject results yet." })); return;
        }
        O.readyChart("subjectBarChart");
        var palette = [C.blue, C.green, C.purple, C.orange, C.red, C.grey];
        O.chart("subjectBarChart", { type: "bar", data: { labels: subjects.map(function (s) { return s.subjectName; }),
            datasets: [{ label: "Average (%)", data: subjects.map(function (s) { return s.average; }), backgroundColor: subjects.map(function (s, i) { return palette[i % palette.length]; }), borderRadius: 6 }] },
            options: { indexAxis: "y", scales: { x: { min: 0, max: 100 } }, plugins: { legend: { display: false } } } });
        subjects.forEach(function (s) {
            wrap.appendChild(el("article", { class: "card subject-card" },
                el("h3", { text: s.subjectName }), el("div", { class: "big", text: O.pct(s.average) }),
                el("div", { class: "meta" }, el("span", { text: "Quizzes: " + O.pct(s.quizAverage) + " (" + s.quizCount + ")" }),
                    el("span", { text: "Assignments: " + O.pct(s.assignmentAverage) + " (" + s.assignmentCount + ")" })),
                el("div", { class: "progress", role: "img", "aria-label": "Average " + O.pct(s.average) }, el("span", { style: "width:" + Math.max(0, Math.min(100, s.average || 0)) + "%" }))));
        });
    }

    function renderBands(ov) {
        var bands = ov.bands || [], total = bands.reduce(function (a, b) { return a + b.count; }, 0);
        if (!total) { O.emptyChart("bandChart", "No completed assessments in this period."); return; }
        O.readyChart("bandChart");
        O.chart("bandChart", { type: "doughnut", data: { labels: bands.map(function (b) { return b.label + " (" + b.count + ")"; }),
            datasets: [{ data: bands.map(function (b) { return b.count; }), backgroundColor: [C.green, C.blue, C.orange, C.red] }] },
            options: { cutout: "62%", plugins: { legend: { position: "right" } } } });
    }

    function scoreCell(r) { return O.num(r.score, 1).replace(/\.0$/, "") + "/" + O.num(r.maxScore, 1).replace(/\.0$/, "") + " (" + O.num(r.percentage, 0) + "%)"; }

    function renderAssessments(rows) {
        var quizzes = rows.filter(function (r) { return r.assessmentType === "QUIZ"; });
        var asg = rows.filter(function (r) { return r.assessmentType === "ASSIGNMENT"; });
        var qBox = O.clear(O.byId("quizTable")), aBox = O.clear(O.byId("assignmentTable"));
        if (!quizzes.length) { qBox.appendChild(el("div", { class: "empty-box", text: "No completed quizzes in this period." })); O.emptyChart("quizChart", "No completed quizzes in this period."); }
        else {
            qBox.appendChild(O.table(["Quiz", "Subject", "Score", "Date"], quizzes.slice(0, 15).map(function (r) { return [r.title, r.subjectName, scoreCell(r), O.date(r.completedAt)]; })));
            O.readyChart("quizChart");
            var last = quizzes.slice(0, 8).reverse();
            O.chart("quizChart", { type: "bar", data: { labels: last.map(function (r) { return r.title; }), datasets: [{ label: "Score (%)", data: last.map(function (r) { return r.percentage; }),
                backgroundColor: last.map(function (r, i) { return [C.blue, C.green, C.purple, C.orange][i % 4]; }), borderRadius: 6 }] },
                options: { scales: { y: { min: 0, max: 100 } }, plugins: { legend: { display: false } } } });
        }
        if (!asg.length) { aBox.appendChild(el("div", { class: "empty-box", text: "No graded assignments in this period." })); O.emptyChart("assignmentChart", "No graded assignments in this period."); }
        else {
            aBox.appendChild(O.table(["Assignment", "Subject", "Score", "Date"], asg.slice(0, 15).map(function (r) { return [r.title, r.subjectName, scoreCell(r), O.date(r.completedAt)]; })));
            O.readyChart("assignmentChart");
            var labels = ["0-20", "21-40", "41-60", "61-80", "81-100"], counts = [0, 0, 0, 0, 0];
            asg.forEach(function (r) { counts[Math.min(4, Math.max(0, Math.ceil(r.percentage / 20) - 1))] += 1; });
            O.chart("assignmentChart", { type: "bar", data: { labels: labels, datasets: [{ label: "Assignments", data: counts, backgroundColor: [C.red, C.orange, C.blue, C.green, C.purple], borderRadius: 6 }] },
                options: { scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }, plugins: { legend: { display: false } } } });
        }
    }

    function renderInsights(res) {
        var list = O.clear(O.byId("insightList")), rec = O.clear(O.byId("recommendationList"));
        if (!res.sufficientData || !res.insights.length) {
            list.appendChild(el("div", { class: "empty-box", text: res.note || "No insights yet." }));
            rec.appendChild(el("div", { class: "empty-box", text: res.sufficientData ? "No recommendations right now." : "Complete a few more assessments to unlock recommendations." }));
            return;
        }
        var ul = el("ul", { class: "insight-list" });
        res.insights.forEach(function (i) { ul.appendChild(el("li", null, el("span", { class: "dot " + i.type, "aria-hidden": "true" }), el("span", { text: i.message }))); });
        list.appendChild(ul);
        if (!res.recommendations.length) { rec.appendChild(el("div", { class: "empty-box", text: "Nothing to improve right now. Keep it up!" })); return; }
        var ul2 = el("ul", { class: "insight-list" });
        res.recommendations.forEach(function (t) { ul2.appendChild(el("li", null, el("span", { class: "dot", "aria-hidden": "true" }), el("span", { text: t }))); });
        rec.appendChild(ul2);
    }

    function load() {
        var notice = O.byId("pageNotice");
        O.clear(notice);
        var p = params();
        var calls = [O.api("/student/overview", p), O.api("/student/subjects", params({ subjectId: null })), O.api("/student/trends", p),
                     O.api("/student/assessments", p), O.api("/student/insights", { from: p.from })];
        Promise.all(calls).then(function (r) {
            var ov = r[0].data;
            renderSubjectOptions(r[1].data);
            renderKpis(ov);
            if (ov.summary.count === 0) { O.notice(notice, "", "You have no completed quizzes or assignments in this period yet. Charts will appear after your first graded result."); }
            renderTrend(r[2].data); renderSubjects(r[1].data); renderBands(ov); renderAssessments(r[3].data); renderInsights(r[4].data);
        }).catch(function (e) { O.showError(notice, e); });
    }

    document.addEventListener("DOMContentLoaded", function () {
        O.tabs(O.byId("tabs"));
        O.byId("rangeSelect").addEventListener("change", function (e) { state.months = Number(e.target.value); load(); });
        O.byId("subjectSelect").addEventListener("change", function (e) { state.subjectId = e.target.value; load(); });
        load();
    });
})();
