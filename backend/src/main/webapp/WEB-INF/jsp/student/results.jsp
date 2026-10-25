<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Results</title>
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
            <a class="active" href="${pageContext.request.contextPath}/student/results"><span class="side-icon">▤</span> Results</a>
            <a href="${pageContext.request.contextPath}/student/performance"><span class="side-icon">◎</span> Performance</a>
            <a href="${pageContext.request.contextPath}/student/profile"><span class="side-icon">☺</span> Profile</a>
            <a href="${pageContext.request.contextPath}/logout"><span class="side-icon">↩</span> Logout</a>
        </nav>
    </aside>

    <main class="student-main">
        <header class="student-topbar">
            <button class="icon-button sidebar-toggle" id="studentSidebarToggle" aria-label="Toggle navigation" data-action="toggle-sidebar">☰</button>
            <div class="searchbox">
                <span>⌕</span>
                <input type="text" value="Search results..." aria-label="Search results" />
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
                <h1>My Results</h1>
                <p>View your quiz and assignment results</p>
            </section>
            <div class="panel">
                <table class="student-table">
                    <thead>
                    <tr>
                        <th>Title</th>
                        <th>Type</th>
                        <th>Marks</th>
                        <th>Status</th>
                        <th>Date</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="result" items="${results}">
                        <tr>
                            <td><c:out value="${result.title}" /></td>
                            <td><c:out value="${result.type}" /></td>
                            <td><c:out value="${result.score}" /></td>
                            <td><span class="pill ${result.status == 'Passed' ? 'green' : (result.status == 'Failed' ? 'red' : 'gray')}"><c:out value="${result.status}" /></span></td>
                            <td><c:out value="${result.date}" /></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty results}">
                        <tr><td colspan="5" class="empty-state">No evaluated or submitted assignments yet.</td></tr>
                    </c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </main>
</div>
<script src="${pageContext.request.contextPath}/js/student-dashboard.js"></script>
</body>
</html>
