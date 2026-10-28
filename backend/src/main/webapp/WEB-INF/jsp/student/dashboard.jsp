<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Student Dashboard</title>
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
            <a class="active" href="${pageContext.request.contextPath}/student/dashboard"><span class="side-icon">⌂</span> Dashboard</a>
            <a href="${pageContext.request.contextPath}/student/quizzes"><span class="side-icon">▣</span> Quizzes</a>
            <a href="${pageContext.request.contextPath}/student/assignments"><span class="side-icon">✎</span> Assignments</a>
            <a href="${pageContext.request.contextPath}/student/results"><span class="side-icon">▤</span> Results</a>
            <a href="${pageContext.request.contextPath}/student/performance"><span class="side-icon">◎</span> Performance</a>
            <a href="${pageContext.request.contextPath}/student/profile"><span class="side-icon">☺</span> Profile</a>
            <a href="${pageContext.request.contextPath}/logout"><span class="side-icon">↩</span> Logout</a>
        </nav>
    </aside>

    <main class="student-main">
        <header class="student-topbar">
            <button class="icon-button sidebar-toggle" id="studentSidebarToggle" aria-label="Toggle navigation" data-action="toggle-sidebar">☰</button>
            <div class="topbar-actions">
                <button class="icon-button" aria-label="Flag item" data-action="toggle-flag">⚑</button>
                <div class="user-chip">
                    <div class="avatar-circle">${currentUserInitial}</div>
                    <div>
                        <div class="user-name">${currentUserName}</div>
                    </div>
                </div>
            </div>
        </header>

        <div class="content-shell">
            <section class="page-header">
                <h1>Welcome Back, ${currentUserName}!</h1>
                <p>Here's your overview</p>
                <c:if test="${not empty newAccountId}">
                    <p class="profile-message" role="status">Your OpenStudy account was created. Your generated account ID is <strong><c:out value="${newAccountId}" /></strong>.</p>
                </c:if>
            </section>

            <section class="stats-grid" aria-label="Student statistics">
                <c:forEach var="stat" items="${stats}">
                    <article class="stat-card">
                        <span class="value-badge value-${stat.accent}"></span>
                        <span class="label">${stat.label}</span>
                        <div class="metric">${stat.value}</div>
                    </article>
                </c:forEach>
            </section>

            <div class="two-panel">
                <section class="panel">
                    <div class="panel-header">
                        <h3>Recent Quizzes</h3>
                        <a href="${pageContext.request.contextPath}/student/quizzes" class="text-link">View All</a>
                    </div>
                    <ul class="listing">
                        <c:forEach var="quiz" items="${recentQuizzes}">
                            <li>
                                <div class="item-icon icon-blue">Q</div>
                                <div class="meta">
                                    <strong>${quiz.title}</strong>
                                    <div class="meta-details">
                                        <span>${quiz.meta}</span>
                                        <span>${quiz.time}</span>
                                    </div>
                                </div>
                                <span class="pill blue">${quiz.status}</span>
                            </li>
                        </c:forEach>
                    </ul>
                </section>

                <section class="panel">
                    <div class="panel-header">
                        <h3>Recent Assignments</h3>
                        <a href="${pageContext.request.contextPath}/student/assignments" class="text-link">View All</a>
                    </div>
                    <ul class="listing">
                        <c:forEach var="assignment" items="${recentAssignments}">
                            <li>
                                <div class="item-icon icon-purple">A</div>
                                <div class="meta">
                                    <strong>${assignment.title}</strong>
                                    <div class="meta-details">
                                        <span>${assignment.meta}</span>
                                        <span>${assignment.time}</span>
                                    </div>
                                </div>
                                <span class="pill purple">${assignment.status}</span>
                            </li>
                        </c:forEach>
                        <c:if test="${empty recentAssignments}">
                            <li class="empty-state">No assignments due in the next or past month.</li>
                        </c:if>
                    </ul>
                </section>
            </div>
        </div>
    </main>
</div>
<script src="${pageContext.request.contextPath}/js/student-dashboard.js"></script>
</body>
</html>
