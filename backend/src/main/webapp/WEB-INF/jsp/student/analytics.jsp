<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Performance · OpenStudy</title>
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/openstudy.css">
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/analytics.css">
</head>
<body class="analytics-page" data-context="${fn:escapeXml(pageContext.request.contextPath)}">
<jsp:include page="fragments/topnav.jsp"/>
<main class="os-page">
    <div class="page-head os-hero os-hero--compact">
        <div>
            <h1>Detailed Analytics</h1>
            <p class="os-hero__sub">Track your progress across quizzes and assignments</p>
        </div>
        <div class="filter-bar">
            <label>Period
                <select id="rangeSelect">
                    <option value="3">Last 3 months</option>
                    <option value="6" selected>Last 6 months</option>
                    <option value="12">Last 12 months</option>
                    <option value="0">All time</option>
                </select>
            </label>
            <label>Subject
                <select id="subjectSelect"><option value="">All subjects</option></select>
            </label>
        </div>
    </div>

    <div id="pageNotice" aria-live="polite"></div>

    <section class="kpi-grid" id="kpis" aria-label="Performance summary"></section>

    <div class="tabs" id="tabs" role="tablist" aria-label="Performance views">
        <button class="tab" role="tab" id="tab-overview" data-tab="overview" aria-controls="panel-overview" aria-selected="true">Overview</button>
        <button class="tab" role="tab" id="tab-detailed" data-tab="detailed" aria-controls="panel-detailed" aria-selected="false" tabindex="-1">Detailed analysis</button>
        <button class="tab" role="tab" id="tab-subjects" data-tab="subjects" aria-controls="panel-subjects" aria-selected="false" tabindex="-1">Subject-wise</button>
        <button class="tab" role="tab" id="tab-quizzes" data-tab="quizzes" aria-controls="panel-quizzes" aria-selected="false" tabindex="-1">Quizzes</button>
        <button class="tab" role="tab" id="tab-assignments" data-tab="assignments" aria-controls="panel-assignments" aria-selected="false" tabindex="-1">Assignments</button>
        <button class="tab" role="tab" id="tab-insights" data-tab="insights" aria-controls="panel-insights" aria-selected="false" tabindex="-1">Insights</button>
    </div>

    <section class="tab-panel" id="panel-overview" role="tabpanel" aria-labelledby="tab-overview">
        <div class="grid-2">
            <div class="card">
                <h3>Score trend</h3>
                <p class="sub">Monthly average percentage for quizzes and assignments.</p>
                <div class="chart-box"><canvas id="trendChart" role="img" aria-label="Line chart of monthly average scores"></canvas></div>
            </div>
            <div class="card">
                <h3>Subject-wise performance</h3>
                <p class="sub">Average percentage in each subject.</p>
                <div class="chart-box"><canvas id="subjectBarChart" role="img" aria-label="Bar chart of subject averages"></canvas></div>
            </div>
        </div>
    </section>

    <section class="tab-panel" id="panel-detailed" role="tabpanel" aria-labelledby="tab-detailed" hidden>
        <div class="grid-2">
            <div class="card">
                <h3>Score distribution</h3>
                <p class="sub">How your results are spread across grade bands.</p>
                <div class="chart-box"><canvas id="bandChart" role="img" aria-label="Doughnut chart of score bands"></canvas></div>
            </div>
            <div class="card">
                <h3>Performance trend (monthly)</h3>
                <p class="sub">Overall monthly average.</p>
                <div class="chart-box"><canvas id="monthlyChart" role="img" aria-label="Line chart of overall monthly average"></canvas></div>
            </div>
        </div>
        <div class="card">
            <h3>Trend analysis (R)</h3>
            <div id="trendAnalysis"></div>
        </div>
    </section>

    <section class="tab-panel" id="panel-subjects" role="tabpanel" aria-labelledby="tab-subjects" hidden>
        <div id="subjectCards" class="subject-cards"></div>
    </section>

    <section class="tab-panel" id="panel-quizzes" role="tabpanel" aria-labelledby="tab-quizzes" hidden>
        <div class="grid-2">
            <div class="card"><h3>Quiz performance</h3><p class="sub">Your most recent quiz results.</p><div id="quizTable"></div></div>
            <div class="card"><h3>Score comparison</h3><p class="sub">Percentage per quiz (latest 8).</p>
                <div class="chart-box"><canvas id="quizChart" role="img" aria-label="Bar chart of quiz scores"></canvas></div></div>
        </div>
    </section>

    <section class="tab-panel" id="panel-assignments" role="tabpanel" aria-labelledby="tab-assignments" hidden>
        <div class="grid-2">
            <div class="card"><h3>Assignment performance</h3><p class="sub">Graded assignments only.</p><div id="assignmentTable"></div></div>
            <div class="card"><h3>Assignment score distribution</h3><p class="sub">Number of graded assignments per score range.</p>
                <div class="chart-box"><canvas id="assignmentChart" role="img" aria-label="Bar chart of assignment score ranges"></canvas></div></div>
        </div>
    </section>

    <section class="tab-panel" id="panel-insights" role="tabpanel" aria-labelledby="tab-insights" hidden>
        <div class="grid-2">
            <div class="card"><h3>Key insights</h3><p class="sub">Generated by fixed rules from your own results.</p><div id="insightList"></div></div>
            <div class="card"><h3>Recommendations</h3><p class="sub">Suggestions based on the insights.</p><div id="recommendationList"></div></div>
        </div>
    </section>

    <p class="sub" id="averagingRule"></p>
</main>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/vendor/chart.umd.js"></script>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/analytics-common.js"></script>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/student-analytics.js"></script>
</body>
</html>
