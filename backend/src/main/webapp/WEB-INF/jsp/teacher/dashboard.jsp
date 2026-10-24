<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Teacher Dashboard · OpenStudy</title>
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/teacher-dashboard.css">
</head>
<body class="teacher-shell">
<div class="layout">
    <jsp:include page="fragments/sidebar.jsp"/>
    <div class="layout-main">
        <jsp:include page="fragments/navbar.jsp"/>
        <main class="content">
            <c:if test="${dashboard.demoData}">
                <div class="banner" role="status">
                    Demo / mock data is shown for development. These figures are not loaded from MySQL.
                </div>
            </c:if>

            <section class="welcome-panel">
                <div>
                    <p class="eyebrow"><c:out value="${dashboard.currentDate}"/></p>
                    <h2><c:out value="${dashboard.greeting}"/></h2>
                    <h3><c:out value="${dashboard.heading}"/></h3>
                    <p class="welcome-copy"><c:out value="${dashboard.welcomeMessage}"/></p>
                </div>
            </section>

            <section class="stats-grid" aria-label="Class statistics">
                <article class="stat-card">
                    <span class="stat-label">Total Quizzes</span>
                    <strong><c:out value="${dashboard.stats.totalQuizzes}"/></strong>
                    <c:if test="${dashboard.stats.noQuizzes}">
                        <p class="empty-inline">No quizzes yet. Member 2 will supply live counts.</p>
                    </c:if>
                </article>
                <article class="stat-card">
                    <span class="stat-label">Total Assignments</span>
                    <strong><c:out value="${dashboard.stats.totalAssignments}"/></strong>
                    <c:if test="${dashboard.stats.noAssignments}">
                        <p class="empty-inline">No assignments yet. Member 3 will supply live counts.</p>
                    </c:if>
                </article>
                <article class="stat-card">
                    <span class="stat-label">Total Students</span>
                    <strong><c:out value="${dashboard.stats.totalStudents}"/></strong>
                </article>
                <article class="stat-card accent">
                    <span class="stat-label">Pending Assignment Reviews</span>
                    <strong><c:out value="${dashboard.stats.pendingAssignmentReviews}"/></strong>
                    <c:if test="${dashboard.stats.noPendingReviews}">
                        <p class="empty-inline">No pending reviews.</p>
                    </c:if>
                </article>
            </section>

            <div class="two-column">
                <section class="panel">
                    <div class="panel-header">
                        <h3>Recent Activity</h3>
                    </div>
                    <c:choose>
                        <c:when test="${dashboard.emptyActivity}">
                            <div class="empty-state">
                                <h4>No recent activity</h4>
                                <p>When quizzes and assignments are published, teacher activity will appear here.</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <ul class="activity-list">
                                <c:forEach var="item" items="${dashboard.recentActivity}">
                                    <li>
                                        <span class="activity-type"><c:out value="${item.type}"/></span>
                                        <div>
                                            <strong><c:out value="${item.title}"/></strong>
                                            <p><c:out value="${item.description}"/></p>
                                            <time><c:out value="${item.occurredAtDisplay}"/></time>
                                        </div>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:otherwise>
                    </c:choose>
                </section>

                <section class="panel">
                    <div class="panel-header">
                        <h3>Upcoming Deadlines</h3>
                    </div>
                    <c:choose>
                        <c:when test="${dashboard.emptyDeadlines}">
                            <div class="empty-state">
                                <h4>No upcoming deadlines</h4>
                                <p>Quiz and assignment due dates from Members 2 and 3 will be listed here.</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <ul class="deadline-list">
                                <c:forEach var="deadline" items="${dashboard.upcomingDeadlines}">
                                    <li>
                                        <span class="deadline-type"><c:out value="${deadline.itemType}"/></span>
                                        <div>
                                            <strong><c:out value="${deadline.title}"/></strong>
                                            <p><c:out value="${deadline.courseLabel}"/></p>
                                            <time><c:out value="${deadline.dueAtDisplay}"/></time>
                                        </div>
                                    </li>
                                </c:forEach>
                            </ul>
                        </c:otherwise>
                    </c:choose>
                </section>
            </div>

            <section class="panel">
                <div class="panel-header">
                    <h3>Quick Actions</h3>
                    <p>Use these shortcuts to open available tools and modules still being integrated.</p>
                </div>
                <div class="actions-grid">
                    <c:forEach var="action" items="${dashboard.quickActions}">
                        <a class="action-card" href="${fn:escapeXml(pageContext.request.contextPath)}${fn:escapeXml(action.href)}">
                            <strong><c:out value="${action.label}"/></strong>
                            <p><c:out value="${action.description}"/></p>
                            <span class="owner-chip"><c:out value="${action.owner}"/></span>
                            <c:if test="${action.placeholder}">
                                <span class="placeholder-note">Placeholder link</span>
                            </c:if>
                        </a>
                    </c:forEach>
                </div>
            </section>
        </main>
        <jsp:include page="fragments/footer.jsp"/>
    </div>
</div>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/teacher-dashboard.js"></script>
</body>
</html>
