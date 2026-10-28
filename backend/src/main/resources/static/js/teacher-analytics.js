/* Teacher Class Analytics page (Member 4). Data comes only from /api/analytics/teacher/**. */
(function () {
    "use strict";
    var O = window.OSA, C = O.COLORS, el = O.el;

    function filters() {
        return { subjectId: O.byId("fSubject").value || null, type: O.byId("fType").value || null,
                 from: O.byId("fFrom").value || null, to: O.byId("fTo").value || null };
    }
    function kpi(label, value, small, accent) {
        return el("article", { class: "kpi" + (accent ? " accent" : "") }, el("span", { class: "kpi-label", text: label }),
            el("strong", { text: value }), small ? el("small", { text: small }) : null);
    }

    function renderKpis(ov) {
        var s = ov.summary, p = ov.participation, d = ov.descriptive, box = O.clear(O.byId("kpis"));
        var o = d && d.status === "OK" ? d.data.overall : null;
        var rNote = o ? null : "Needs R";
        box.appendChild(kpi("Class average", O.pct(s.average), s.count + " results", true));
        box.appendChild(kpi("Median", o ? O.pct(o.median) : "\u2014", rNote));
        box.appendChild(kpi("Standard deviation", o && o.sd !== undefined && o.sd !== null ? O.num(o.sd) + " pts" : "\u2014", rNote));
        box.appendChild(kpi("Minimum / maximum", O.pct(s.min, 0) + " / " + O.pct(s.max, 0)));
        box.appendChild(kpi("Quiz average", O.pct(s.quizAverage), s.quizCount + " results"));
        box.appendChild(kpi("Assignment average", O.pct(s.assignmentAverage), s.assignmentCount + " results"));
        box.appendChild(kpi("Students with results", O.whole(p.studentsWithResults), "of " + p.enrolledStudents + " enrolled"));
        box.appendChild(kpi("Participation", O.pct(p.rate, 0), "Enrolled students with a counted result"));
        O.byId("averagingRule").textContent = ov.averagingRule || "";
    }

    function renderBox(ov) {
        var d = ov.descriptive;
        var problem = O.rProblem(d);
        if (problem) { O.emptyChart("boxChart", problem); return; }
        var groups = (d.data.by_subject || []).filter(function (g) { return g.n >= 1 && g.boxplot; });
        if (!groups.length) { O.emptyChart("boxChart", "Not enough data for a box plot."); return; }
        O.readyChart("boxChart");
        var labels = groups.map(function (g) { return g.group + " (n=" + g.n + ")"; });
        O.chart("boxChart", { data: { labels: labels, datasets: [
            { type: "bar", label: "Q1 to Q3", data: groups.map(function (g) { return [g.boxplot.q1, g.boxplot.q3]; }), backgroundColor: "rgba(79,70,229,.25)", borderColor: C.purple, borderWidth: 1, borderSkipped: false },
            { type: "line", label: "Median", data: groups.map(function (g) { return g.boxplot.median; }), showLine: false, pointStyle: "rectRot", pointRadius: 7, backgroundColor: C.red, borderColor: C.red },
            { type: "line", label: "Maximum", data: groups.map(function (g) { return g.max; }), showLine: false, pointStyle: "triangle", pointRadius: 5, backgroundColor: C.green, borderColor: C.green },
            { type: "line", label: "Minimum", data: groups.map(function (g) { return g.min; }), showLine: false, pointStyle: "triangle", rotation: 180, pointRadius: 5, backgroundColor: C.orange, borderColor: C.orange }] },
            options: { scales: { y: { min: 0, max: 100, title: { display: true, text: "Score (%)" } } } } });
    }

    function renderCharts(trends, subjects, dist) {
        if (!trends.length) { O.emptyChart("trendChart", "No completed assessments match these filters."); }
        else {
            O.readyChart("trendChart");
            O.chart("trendChart", { type: "line", data: { labels: trends.map(function (m) { return O.monthLabel(m.month); }), datasets: [
                { label: "All", data: trends.map(function (m) { return m.average; }), borderColor: C.blue, backgroundColor: C.blue, tension: .3 },
                { label: "Quizzes", data: trends.map(function (m) { return m.quizAverage; }), borderColor: C.purple, backgroundColor: C.purple, tension: .3, spanGaps: true, borderDash: [5, 4] },
                { label: "Assignments", data: trends.map(function (m) { return m.assignmentAverage; }), borderColor: C.green, backgroundColor: C.green, tension: .3, spanGaps: true, borderDash: [5, 4] }] },
                options: { scales: { y: { min: 0, max: 100, title: { display: true, text: "Average (%)" } } } } });
        }
        if (!dist.total) { O.emptyChart("histChart", "No completed assessments match these filters."); }
        else {
            O.readyChart("histChart");
            O.chart("histChart", { type: "bar", data: { labels: dist.bins.map(function (b) { return b.label; }), datasets: [
                { label: "Results", data: dist.bins.map(function (b) { return b.count; }), backgroundColor: C.blue, borderRadius: 4, categoryPercentage: 1, barPercentage: .95 }] },
                options: { scales: { y: { beginAtZero: true, ticks: { precision: 0 } }, x: { title: { display: true, text: "Score range (%)" } } }, plugins: { legend: { display: false } } } });
        }
        if (!subjects.length) { O.emptyChart("subjectChart", "No completed assessments match these filters."); }
        else {
            O.readyChart("subjectChart");
            O.chart("subjectChart", { type: "bar", data: { labels: subjects.map(function (s) { return s.subjectName; }), datasets: [
                { label: "Quizzes", data: subjects.map(function (s) { return s.quizAverage; }), backgroundColor: C.blue, borderRadius: 4 },
                { label: "Assignments", data: subjects.map(function (s) { return s.assignmentAverage; }), backgroundColor: C.purple, borderRadius: 4 }] },
                options: { scales: { y: { min: 0, max: 100, title: { display: true, text: "Average (%)" } } } } });
        }
    }

    function renderParticipation(ov) {
        var box = O.clear(O.byId("participationTable"));
        if (!ov.assessments.length) { box.appendChild(el("div", { class: "empty-box", text: "No published quizzes or assignments in this scope." })); return; }
        box.appendChild(O.table(["Assessment", "Type", "Subject", "Enrolled", "Completed", "Participation", "Average"],
            ov.assessments.map(function (a) { return [a.title, a.assessmentType === "QUIZ" ? "Quiz" : "Assignment", a.subjectName, a.enrolled, a.completed, O.pct(a.rate, 0), O.pct(a.average)]; })));
    }

    function renderQuestions(qs) {
        var box = O.clear(O.byId("questionTable"));
        if (!qs.length) { box.appendChild(el("div", { class: "empty-box", text: "No quiz question data for these filters." })); return; }
        box.appendChild(O.table(["Quiz", "Q", "Question", "Attempts", "Correct", "Difficulty"], qs.map(function (q) {
            var kind = !q.reliable ? "muted" : q.correctRate < 40 ? "bad" : q.correctRate < 70 ? "warn" : "good";
            return [q.quizTitle, q.position, el("span", { text: q.question }), q.attempts, O.pct(q.correctRate, 0), O.badge(q.difficulty, kind)];
        })));
    }

    function renderInsights(res) {
        var list = O.clear(O.byId("insightList")), rec = O.clear(O.byId("recommendationList"));
        if (!res.sufficientData || !res.insights.length) { list.appendChild(el("div", { class: "empty-box", text: res.note || "No insights yet." })); return; }
        var ul = el("ul", { class: "insight-list" });
        res.insights.forEach(function (i) { ul.appendChild(el("li", null, el("span", { class: "dot " + i.type, "aria-hidden": "true" }), el("span", { text: i.message }))); });
        list.appendChild(ul);
        if (res.recommendations.length) {
            rec.appendChild(el("h3", { text: "Suggested actions", style: "margin-top:14px" }));
            var u2 = el("ul", { class: "insight-list" });
            res.recommendations.forEach(function (t) { u2.appendChild(el("li", null, el("span", { class: "dot", "aria-hidden": "true" }), el("span", { text: t }))); });
            rec.appendChild(u2);
        }
    }

    // ------------------------------------------------------------ statistical report

    function kv(pairs) {
        var box = el("div", { class: "kv" });
        pairs.forEach(function (p) { if (p[1] !== null && p[1] !== undefined && p[1] !== "\u2014") { box.appendChild(el("span", null, p[0] + ": ", el("b", { text: String(p[1]) }))); } });
        return box;
    }
    function pText(p) { return p === null || p === undefined ? "\u2014" : p < 0.001 ? "< 0.001" : O.num(p, 3); }

    function testCard(title, r, build) {
        var card = el("div", { class: "stat-test" });
        var h = el("h4", null, title);
        card.appendChild(h);
        if (!r || r.status !== "ok") {
            h.appendChild(O.badge("Not run", "muted"));
            card.appendChild(el("p", { class: "sub", text: (r && (r.reason || r.message)) || "Not enough data." }));
            return card;
        }
        build(card, h, r);
        if (r.limitations && r.limitations.length) {
            card.appendChild(el("ul", { class: "limits" }, r.limitations.filter(Boolean).map(function (l) { return el("li", { text: String(l) }); })));
        }
        return card;
    }

    function ciText(ci) { return ci ? O.num(ci.lower) + " to " + O.num(ci.upper) : null; }
    function sig(p, alpha) { return p !== null && p !== undefined && p < (alpha || 0.05); }

    function buildHypothesis(h, body) {
        body.appendChild(testCard("Mean vs benchmark (one-sample t-test)", h.one_sample_t_test, function (c, head, r) {
            head.appendChild(O.badge(sig(r.p_value) ? "Significant" : "Not significant", sig(r.p_value) ? "good" : "info"));
            c.appendChild(el("p", { class: "sub", text: r.question })); c.appendChild(el("p", { text: r.interpretation }));
            c.appendChild(kv([["Students", r.n], ["Mean", O.pct(r.mean)], ["95% CI", ciText(r.ci)], ["t", O.num(r.t, 2)], ["df", O.num(r.df, 1)], ["p", pText(r.p_value)]]));
        }));
        body.appendChild(testCard("Quiz vs assignment (paired t-test)", h.paired_t_test, function (c, head, r) {
            head.appendChild(O.badge(sig(r.p_value) ? "Significant" : "Not significant", sig(r.p_value) ? "good" : "info"));
            c.appendChild(el("p", { class: "sub", text: r.question })); c.appendChild(el("p", { text: r.interpretation }));
            c.appendChild(kv([["Students", r.n], ["Quiz mean", O.pct(r.mean_quiz)], ["Assignment mean", O.pct(r.mean_assignment)], ["95% CI of difference", ciText(r.ci)], ["p", pText(r.p_value)]]));
            if (r.fallback) { c.appendChild(el("p", { class: "sub", text: r.fallback.method + ": p = " + pText(r.fallback.p_value) })); }
        }));
        body.appendChild(testCard("Subjects compared (ANOVA)", h.anova, function (c, head, r) {
            head.appendChild(O.badge(sig(r.p_value) ? "Significant" : "Not significant", sig(r.p_value) ? "good" : "info"));
            if (r.assumptions && !r.assumptions.satisfied) { head.appendChild(O.badge("Assumptions doubtful", "warn")); }
            c.appendChild(el("p", { class: "sub", text: r.question })); c.appendChild(el("p", { text: r.interpretation }));
            c.appendChild(kv([["F", O.num(r.f, 2)], ["df", r.df_between + ", " + r.df_within], ["p", pText(r.p_value)], ["Eta\u00b2", O.num(r.eta_squared, 3)],
                ["Residual normality p", pText(r.assumptions && r.assumptions.residual_normality_shapiro_p)], ["Equal variance p", pText(r.assumptions && r.assumptions.equal_variance_fligner_p)]]));
            if (r.fallback) { c.appendChild(el("p", { class: "sub", text: r.fallback.method + ": p = " + pText(r.fallback.p_value) })); }
            if (r.post_hoc && r.post_hoc.length) { c.appendChild(O.table(["Comparison", "Difference", "Adjusted p"], r.post_hoc.map(function (x) { return [x.comparison, O.num(x.diff), pText(x.p_adj)]; }))); }
        }));
        body.appendChild(testCard("Quiz vs assignment correlation", h.correlation, function (c, head, r) {
            head.appendChild(O.badge("r = " + O.num(r.pearson_r, 2), sig(r.p_value) ? "good" : "info"));
            c.appendChild(el("p", { class: "sub", text: r.question })); c.appendChild(el("p", { text: r.interpretation }));
            c.appendChild(kv([["Students", r.n], ["Pearson r", O.num(r.pearson_r, 3)], ["95% CI", r.ci ? O.num(r.ci.lower, 2) + " to " + O.num(r.ci.upper, 2) : null], ["p", pText(r.p_value)], ["Spearman \u03c1", O.num(r.spearman_rho, 3)]]));
        }));
    }

    function coefTable(model) {
        return O.table(["Term", "Estimate", "Std. error", "t", "p"], model.coefficients.map(function (c) { return [c.term, O.num(c.estimate, 3), O.num(c.std_error, 3), O.num(c.t, 2), pText(c.p_value)]; }));
    }
    function buildRegression(rg, body) {
        [["Simple regression: assignment mean ~ quiz mean", rg.quiz_vs_assignment], ["Simple regression: score ~ time", rg.trend_simple], ["Multiple regression: score ~ time + type + subject", rg.trend_multiple]].forEach(function (pair) {
            body.appendChild(testCard(pair[0], pair[1], function (c, head, m) {
                head.appendChild(O.badge("R\u00b2 = " + O.num(m.r_squared, 3), "info"));
                c.appendChild(el("p", { class: "sub", text: m.formula + " (n = " + m.n + ")" })); c.appendChild(el("p", { text: m.interpretation }));
                c.appendChild(kv([["R\u00b2", O.num(m.r_squared, 3)], ["Adjusted R\u00b2", O.num(m.adj_r_squared, 3)], ["F", O.num(m.f_statistic, 2) + " (" + m.f_df1 + ", " + m.f_df2 + ")"], ["F-test p", pText(m.f_p_value)], ["Residual SE", O.num(m.residual_std_error, 2)]]));
                c.appendChild(coefTable(m));
                var ra = m.residual_analysis || {};
                c.appendChild(kv([["Residual normality (Shapiro p)", pText(ra.normality_shapiro_p)], ["Heteroscedasticity (Spearman p)", pText(ra.heteroscedasticity_spearman_p)], ["Influential points", ra.influential_points]]));
                if (m.vif) { c.appendChild(el("p", { class: "sub", text: "Multicollinearity (VIF): " + m.vif.map(function (v) { return v.term + " " + O.num(v.vif, 2); }).join(", ") })); }
            }));
        });
    }
    function buildTrend(tr, body) {
        body.appendChild(testCard("Trend over time", tr && tr.trend, function (c, head, t) {
            head.appendChild(O.badge(t.direction.replace(/_/g, " "), t.direction === "improving" ? "good" : t.direction === "declining" ? "bad" : "info"));
            c.appendChild(el("p", { text: t.interpretation })); c.appendChild(kv([["Change per month", O.num(t.slope_per_month) + " pts"], ["p", pText(t.p_value)], ["R\u00b2", O.num(t.r_squared, 3)]]));
        }));
    }
    function buildDescriptive(d, body) {
        var o = d.overall;
        body.appendChild(testCard("Descriptive statistics", d, function (c) {
            c.appendChild(el("p", { text: d.interpretation }));
            c.appendChild(kv([["n", o.n], ["Mean", O.pct(o.mean)], ["Median", O.pct(o.median)], ["Mode", o.mode ? o.mode.join(", ") : "none"], ["Variance", O.num(o.variance)], ["SD", O.num(o.sd)],
                ["Min", O.pct(o.min)], ["Q1", O.pct(o.q1)], ["Q3", O.pct(o.q3)], ["Max", O.pct(o.max)], ["IQR", O.num(o.iqr)], ["95% CI of mean", ciText(o.ci95)], ["Pass rate", O.pct(d.pass_rate, 0)]]));
            c.appendChild(O.table(["Score range", "Count", "Share"], d.distribution.map(function (b) { return [b.label, b.count, O.pct(b.percent)]; })));
        }));
    }

    function drawStatCharts(report) {
        var box = O.byId("reportCharts");
        var reg = report.regression && report.regression.status === "OK" ? report.regression.data.quiz_vs_assignment : null;
        if (!reg || reg.status !== "ok") { box.hidden = true; return; }
        box.hidden = false;
        O.chart("scatterChart", { type: "scatter", data: { datasets: [
            { label: "Students", data: reg.points, backgroundColor: C.blue, pointRadius: 5 },
            { label: "Fitted line", data: reg.fitted_line, type: "line", borderColor: C.red, backgroundColor: C.red, pointRadius: 0, borderWidth: 2 }] },
            options: { scales: { x: { min: 0, max: 100, title: { display: true, text: "Quiz average (%)" } }, y: { min: 0, max: 100, title: { display: true, text: "Assignment average (%)" } } } } });
        var pts = (reg.residual_analysis.plot || []).map(function (p) { return { x: p.fitted, y: p.residual }; });
        O.chart("residChart", { type: "scatter", data: { datasets: [{ label: "Residuals", data: pts, backgroundColor: C.purple, pointRadius: 5 }] },
            options: { scales: { x: { title: { display: true, text: "Fitted value" } }, y: { title: { display: true, text: "Residual" } } }, plugins: { legend: { display: false } } } });
    }

    function runReport() {
        var btn = O.byId("reportBtn"), status = O.byId("reportStatus"), body = O.clear(O.byId("reportBody"));
        btn.disabled = true; O.notice(status, "", "Running statistical analysis in R\u2026");
        O.api("/teacher/statistical-report", filters()).then(function (res) {
            var r = res.data; O.clear(status);
            var problems = ["descriptive", "trends", "hypothesis", "regression"].map(function (k) { return O.rProblem(r[k]); }).filter(Boolean);
            if (problems.length === 4) { O.notice(status, "warn", problems[0]); O.byId("reportCharts").hidden = true; return; }
            body.appendChild(el("p", { class: "sub", text: "Based on " + r.observations + " results from " + r.students + " students." }));
            if (r.descriptive.status === "OK") { buildDescriptive(r.descriptive.data, body); }
            if (r.trends.status === "OK") { buildTrend(r.trends.data, body); }
            if (r.hypothesis.status === "OK") { buildHypothesis(r.hypothesis.data, body); }
            if (r.regression.status === "OK") { buildRegression(r.regression.data, body); }
            if (problems.length) { O.notice(status, "warn", problems[0]); }
            drawStatCharts(r);
        }).catch(function (e) { O.showError(status, e); }).then(function () { btn.disabled = false; });
    }

    // ------------------------------------------------------------ loading

    var subjectsLoaded = false;
    function load() {
        var notice = O.byId("pageNotice"), f = filters();
        O.clear(notice); O.clear(O.byId("reportBody")); O.clear(O.byId("reportStatus")); O.byId("reportCharts").hidden = true;
        O.byId("applyBtn").disabled = true;
        var calls = [O.api("/teacher/class-overview", f), O.api("/teacher/subjects", f), O.api("/teacher/distribution", f), O.api("/teacher/trends", f),
                     O.api("/teacher/question-performance", { subjectId: f.subjectId, type: f.type }), O.api("/teacher/insights", f)];
        if (!subjectsLoaded) { calls.push(O.api("/teacher/filters")); }
        Promise.all(calls).then(function (r) {
            if (r[6]) {
                subjectsLoaded = true;
                r[6].data.subjects.forEach(function (s) { O.byId("fSubject").appendChild(el("option", { value: String(s.subjectId), text: s.subjectName })); });
            }
            var ov = r[0].data;
            renderKpis(ov);
            if (ov.summary.count === 0) { O.notice(notice, "", "No completed quizzes or assignments match these filters."); }
            else if (ov.descriptive.status !== "OK" && ov.descriptive.status !== "INSUFFICIENT_DATA") { O.notice(notice, "warn", ov.descriptive.message + " Median, standard deviation and the box plot are hidden; other figures come directly from the database."); }
            renderCharts(r[3].data, r[1].data, r[2].data); renderBox(ov); renderParticipation(ov); renderQuestions(r[4].data); renderInsights(r[5].data);
        }).catch(function (e) { O.showError(notice, e); }).then(function () { O.byId("applyBtn").disabled = false; });
    }

    document.addEventListener("DOMContentLoaded", function () {
        O.byId("filterForm").addEventListener("submit", function (e) { e.preventDefault(); load(); });
        O.byId("resetBtn").addEventListener("click", function () { O.byId("filterForm").reset(); load(); });
        O.byId("reportBtn").addEventListener("click", runReport);
        load();
    });
})();
