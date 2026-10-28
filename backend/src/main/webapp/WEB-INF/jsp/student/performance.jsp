<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="os" tagdir="/WEB-INF/tags" %>
<c:set var="base" value="${fn:escapeXml(pageContext.request.contextPath)}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Performance · OpenStudy</title>
    <link rel="stylesheet" href="${base}/css/openstudy.css">
</head>
<body>
<jsp:include page="fragments/topnav.jsp"/>

<main class="os-page">
    <section class="os-hero os-hero--compact">
        <div>
            <h1>My Performance</h1>
            <p class="os-hero__sub">Track your progress across quizzes and assignments</p>
        </div>
        <div class="os-hero__actions">
            <a class="os-btn os-btn--primary" href="${base}/student/analytics"><os:icon name="analytics"/>Detailed analytics</a>
        </div>
    </section>

    <div class="os-perf">
        <section class="os-panel os-panel--padded tone-purple" aria-labelledby="overallTitle">
            <div class="os-panel__head">
                <span class="os-panel__icon"><os:icon name="star"/></span>
                <h2 id="overallTitle">Overall Score</h2>
            </div>
            <div class="os-donut-wrap">
                <div class="os-donut" style="--p: 76.4%" role="img" aria-label="Average score 76.4 percent">
                    <span class="os-donut__value">76.4%<small>average</small></span>
                </div>
                <ul class="os-legend">
                    <li class="tone-purple"><span class="swatch"></span>Quizzes<strong>72%</strong></li>
                    <li class="tone-lavender"><span class="swatch"></span>Assignments<strong>80%</strong></li>
                    <li class="tone-mint"><span class="swatch"></span>Completed tasks<strong>5</strong></li>
                </ul>
            </div>
        </section>

        <section class="os-panel os-panel--padded tone-lavender" aria-labelledby="trendTitle">
            <div class="os-panel__head">
                <span class="os-panel__icon"><os:icon name="trend"/></span>
                <h2 id="trendTitle">Performance Trend</h2>
            </div>
            <div class="os-trend-legend"><span><i></i>Quizzes</span><span><i class="peach"></i>Assignments</span></div>
            <svg class="os-trend" viewBox="0 0 640 260" role="img" aria-label="Scores rose from 64% in August to 81% in November for quizzes, and from 70% to 84% for assignments">
                <defs>
                    <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0" stop-color="#6d4fd8" stop-opacity="0.07"/>
                        <stop offset="1" stop-color="#6d4fd8" stop-opacity="0"/>
                    </linearGradient>
                </defs>
                <g class="grid">
                    <line x1="40" y1="20" x2="630" y2="20"/><line x1="40" y1="80" x2="630" y2="80"/>
                    <line x1="40" y1="140" x2="630" y2="140"/><line x1="40" y1="200" x2="630" y2="200"/>
                </g>
                <g class="axis" text-anchor="end">
                    <text x="30" y="24">100</text><text x="30" y="84">80</text><text x="30" y="144">60</text><text x="30" y="204">40</text>
                </g>
                <g class="axis" text-anchor="middle">
                    <text x="80" y="245">Aug</text><text x="260" y="245">Sep</text><text x="440" y="245">Oct</text><text x="600" y="245">Nov</text>
                </g>
                <path d="M80 128 C170 110 190 104 260 98 S380 84 440 77 S560 70 600 77 L600 200 L80 200 Z" fill="url(#trendFill)"/>
                <path class="line" d="M80 128 C170 110 190 104 260 98 S380 84 440 77 S560 70 600 77"/>
                <path class="line line--peach" d="M80 110 C170 100 200 95 260 89 S380 80 440 68 S560 64 600 68"/>
                <circle class="dot" cx="80" cy="128" r="5"/><circle class="dot" cx="260" cy="98" r="5"/>
                <circle class="dot" cx="440" cy="77" r="5"/><circle class="dot" cx="600" cy="77" r="5"/>
            </svg>
        </section>
    </div>
</main>
<script src="${base}/js/student-dashboard.js"></script>
</body>
</html>
