<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="os" tagdir="/WEB-INF/tags" %>
<c:set var="base" value="${fn:escapeXml(pageContext.request.contextPath)}"/>
<c:set var="tones" value="${['purple', 'peach', 'mint', 'lavender']}"/>
<c:set var="actionIcons" value="${['quiz', 'assignment', 'inbox', 'analytics']}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Teacher Dashboard · OpenStudy</title>
    <link rel="stylesheet" href="${base}/css/openstudy.css">
</head>
<body>
<jsp:include page="fragments/navbar.jsp"/>

<main class="os-page">
    <section class="os-hero os-hero--banner">
        <div>
            <p class="os-hero__eyebrow"><c:out value="${dashboard.currentDate}"/></p>
            <h1><c:out value="${dashboard.greeting}"/></h1>
            <p class="os-hero__sub"><c:out value="${dashboard.welcomeMessage}"/></p>
        </div>
        <p class="os-motto" aria-hidden="true">Small steps, big progress <os:icon name="sparkle"/></p>
    </section>

    <c:if test="${dashboard.demoData}">
        <div class="os-alert os-alert--warn" role="status">
            <p>Demo / mock data is shown for development. These figures are not loaded from MySQL.</p>
        </div>
    </c:if>

    <section class="os-stats" aria-label="Class statistics">
        <a class="os-stat tone-purple" href="${base}${fn:escapeXml(nav.quizzes)}">
            <span class="os-stat__icon"><os:icon name="quiz"/></span>
            <span class="os-stat__label">Total Quizzes</span>
            <span class="os-stat__value"><c:out value="${dashboard.stats.totalQuizzes}"/></span>
            <c:if test="${dashboard.stats.noQuizzes}"><span class="os-stat__note">No quizzes yet. Member 2 will supply live counts.</span></c:if>
        </a>
        <a class="os-stat tone-peach" href="${base}${fn:escapeXml(nav.assignments)}">
            <span class="os-stat__icon"><os:icon name="assignment"/></span>
            <span class="os-stat__label">Total Assignments</span>
            <span class="os-stat__value"><c:out value="${dashboard.stats.totalAssignments}"/></span>
            <c:if test="${dashboard.stats.noAssignments}"><span class="os-stat__note">No assignments yet. Member 3 will supply live counts.</span></c:if>
        </a>
        <a class="os-stat tone-mint" href="${base}${fn:escapeXml(nav.analytics)}">
            <span class="os-stat__icon"><os:icon name="users"/></span>
            <span class="os-stat__label">Total Students</span>
            <span class="os-stat__value"><c:out value="${dashboard.stats.totalStudents}"/></span>
        </a>
        <a class="os-stat tone-lavender" href="${base}${fn:escapeXml(nav.submissions)}">
            <span class="os-stat__icon"><os:icon name="inbox"/></span>
            <span class="os-stat__label">Pending Reviews</span>
            <span class="os-stat__value"><c:out value="${dashboard.stats.pendingAssignmentReviews}"/></span>
            <c:if test="${dashboard.stats.noPendingReviews}"><span class="os-stat__note">You're all caught up.</span></c:if>
        </a>
    </section>

    <div class="os-grid-2 os-grid-2--even">
        <section class="os-panel tone-purple" aria-labelledby="activityTitle">
            <div class="os-panel__head">
                <span class="os-panel__icon"><os:icon name="activity"/></span>
                <h2 id="activityTitle">Recent Activity</h2>
            </div>
            <c:choose>
                <c:when test="${dashboard.emptyActivity}">
                    <div class="os-empty"><strong>No recent activity</strong>When quizzes and assignments are published, activity will appear here.</div>
                </c:when>
                <c:otherwise>
                    <ul class="os-list">
                        <c:forEach var="item" items="${dashboard.recentActivity}" varStatus="loop">
                            <li class="os-row tone-${tones[loop.index % 4]}">
                                <span class="os-tile" aria-hidden="true"><c:out value="${fn:toUpperCase(fn:substring(item.type, 0, 1))}"/></span>
                                <div class="os-row__body">
                                    <strong class="os-row__title"><c:out value="${item.title}"/></strong>
                                    <p class="os-row__desc"><c:out value="${item.description}"/></p>
                                </div>
                                <div class="os-row__aside">
                                    <time class="os-row__meta"><c:out value="${item.occurredAtDisplay}"/></time>
                                </div>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </section>

        <section class="os-panel tone-peach" aria-labelledby="deadlinesTitle">
            <div class="os-panel__head">
                <span class="os-panel__icon"><os:icon name="calendar"/></span>
                <h2 id="deadlinesTitle">Upcoming Deadlines</h2>
            </div>
            <c:choose>
                <c:when test="${dashboard.emptyDeadlines}">
                    <div class="os-empty"><strong>No upcoming deadlines</strong>Quiz and assignment due dates from Members 2 and 3 will be listed here.</div>
                </c:when>
                <c:otherwise>
                    <ul class="os-list">
                        <c:forEach var="deadline" items="${dashboard.upcomingDeadlines}" varStatus="loop">
                            <li class="os-row tone-${tones[loop.index % 4]}">
                                <span class="os-tile" aria-hidden="true"><c:out value="${fn:toUpperCase(fn:substring(deadline.itemType, 0, 1))}"/></span>
                                <div class="os-row__body">
                                    <strong class="os-row__title"><c:out value="${deadline.title}"/></strong>
                                    <div class="os-row__meta"><span><c:out value="${deadline.courseLabel}"/></span><span><c:out value="${deadline.dueAtDisplay}"/></span></div>
                                </div>
                                <span class="os-chip"><c:out value="${deadline.itemType}"/></span>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </section>
    </div>

    <section class="os-panel tone-lavender" aria-labelledby="quickActionsTitle">
        <div class="os-panel__head">
                <span class="os-panel__icon"><os:icon name="sparkle"/></span>
            <div>
                <h2 id="quickActionsTitle">Quick Actions</h2>
                <p>These shortcuts are integration-ready. They do not create quizzes or assignments yet.</p>
            </div>
        </div>
        <div class="os-actions">
            <c:forEach var="action" items="${dashboard.quickActions}" varStatus="loop">
                <a class="os-action tone-${tones[loop.index % 4]}" href="${base}${fn:escapeXml(action.href)}">
                    <span class="os-panel__icon"><os:icon name="${actionIcons[loop.index % 4]}"/></span>
                    <strong><c:out value="${action.label}"/></strong>
                    <p><c:out value="${action.description}"/></p>
                    <span class="os-action__meta">
                        <span class="os-chip"><c:out value="${action.owner}"/></span>
                        <c:if test="${action.placeholder}"><span class="os-chip">Placeholder link</span></c:if>
                    </span>
                </a>
            </c:forEach>
        </div>
    </section>
</main>
<jsp:include page="fragments/footer.jsp"/>
</body>
</html>
