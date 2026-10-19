<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Performance</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/student-dashboard.css">
</head>
<body>
<div class="student-wrapper">
    <aside class="student-sidebar" id="studentSidebar">
        <div class="brand-block">
            <div class="brand-icon">OS</div>
            <div class="brand-meta">
                <strong>OpenStudy</strong>
            </div>
        </div>
        <nav class="student-side-nav" aria-label="Student navigation">
            <a href="${pageContext.request.contextPath}/student/dashboard"><span class="side-icon">⌂</span> Dashboard</a>
            <a href="${pageContext.request.contextPath}/student/quizzes"><span class="side-icon">▣</span> Quizzes</a>
            <a href="${pageContext.request.contextPath}/student/assignments"><span class="side-icon">✎</span> Assignments</a>
            <a href="${pageContext.request.contextPath}/student/results"><span class="side-icon">▤</span> Results</a>
            <a class="active" href="${pageContext.request.contextPath}/student/performance"><span class="side-icon">◎</span> Performance</a>
            <a href="${pageContext.request.contextPath}/student/profile"><span class="side-icon">☺</span> Profile</a>
            <a href="${pageContext.request.contextPath}/logout"><span class="side-icon">↩</span> Logout</a>
        </nav>
    </aside>

    <main class="student-main">
        <header class="student-topbar">
            <button class="icon-button sidebar-toggle" id="studentSidebarToggle" aria-label="Toggle navigation" data-action="toggle-sidebar">☰</button>
            <div class="searchbox">
                <span>⌕</span>
                <input type="text" value="Search performance..." aria-label="Search performance" />
            </div>
            <div class="topbar-actions">
                <button class="icon-button" aria-label="Flag item" data-action="toggle-flag">⚑</button>
                <div class="user-chip">
                    <div class="avatar-circle">${currentUserInitial}</div>
                    <div class="user-name">${currentUserName}</div>
                </div>
            </div>
        </header>

        <div class="content-shell">
            <section class="page-header">
                <h1>My Performance</h1>
                <p>Track your progress across quizzes and assignments</p>
            </section>

            <div class="performance-layout">
                <div class="donut-wrap">
                    <div class="donut"><span class="donut-value">76.4%</span></div>
                    <div class="legend">
                        <div class="legend-item">
                            <span class="legend-badge"><span class="legend-swatch" style="background:#3b82f6"></span> Quizzes</span>
                            <strong>72%</strong>
                        </div>
                        <div class="legend-item">
                            <span class="legend-badge"><span class="legend-swatch" style="background:#a78bfa"></span> Assignments</span>
                            <strong>80%</strong>
                        </div>
                        <div class="legend-item">
                            <span class="legend-badge"><span class="legend-swatch" style="background:#cbd5e1"></span> Average</span>
                            <strong>76.4%</strong>
                        </div>
                    </div>
                </div>

                <div class="trend-panel">
                    <div class="panel-header">
                        <h3>Performance Trend</h3>
                    </div>
                    <div class="trend-graph">
                        <div class="trend-line"></div>
                    </div>
                    <div class="axis-labels">
                        <span>Aug</span>
                        <span>Sep</span>
                        <span>Oct</span>
                        <span>Nov</span>
                    </div>
                </div>
            </div>
        </div>
    </main>
</div>
<script src="${pageContext.request.contextPath}/js/student-dashboard.js"></script>
</body>
</html>
