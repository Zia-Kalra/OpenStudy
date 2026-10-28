<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="os" tagdir="/WEB-INF/tags" %>
<c:set var="base" value="${fn:escapeXml(pageContext.request.contextPath)}"/>
<c:set var="tones" value="${['purple', 'peach', 'mint', 'lavender']}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard · OpenStudy</title>
    <link rel="stylesheet" href="${base}/css/openstudy.css">
</head>
<body>
<jsp:include page="fragments/topnav.jsp"/>

<main class="os-page">
    <section class="os-hero os-hero--banner">
        <div>
            <p class="os-hero__eyebrow">Good to see you,</p>
            <h1><c:out value="${currentUserName}"/>!</h1>
            <p class="os-hero__sub">Here's your overview</p>
        </div>
        <p class="os-motto" aria-hidden="true">Small steps, big progress <os:icon name="sparkle"/></p>
    </section>

    <c:if test="${not empty newAccountId}">
        <div class="os-alert os-alert--success" role="status">
            <os:icon name="check-circle"/>
            <p>Your OpenStudy account was created. Your account ID is <strong><c:out value="${newAccountId}"/></strong>.</p>
        </div>
    </c:if>

    <section class="os-stats" aria-label="Your statistics">
        <c:forEach var="stat" items="${stats}">
            <a class="os-stat tone-${stat.accent}" href="${base}${stat.href}">
                <span class="os-stat__icon"><os:icon name="${stat.icon}"/></span>
                <span class="os-stat__label"><c:out value="${stat.label}"/></span>
                <span class="os-stat__value"><c:out value="${stat.value}"/></span>
            </a>
        </c:forEach>
    </section>

    <div class="os-grid-2">
        <section class="os-panel tone-purple" aria-labelledby="recentQuizzesTitle">
            <div class="os-panel__head">
                <span class="os-panel__icon"><os:icon name="quiz"/></span>
                <h2 id="recentQuizzesTitle">Recent Quizzes</h2>
                <a class="os-panel__link" href="${base}/student/quizzes">View All<os:icon name="chevron-right"/></a>
            </div>
            <ul class="os-list">
                <c:forEach var="quiz" items="${recentQuizzes}" varStatus="loop">
                    <li class="os-row tone-${tones[loop.index % 4]}" data-os-searchable>
                        <span class="os-tile" aria-hidden="true">Q</span>
                        <div class="os-row__body">
                            <strong class="os-row__title"><c:out value="${quiz.title}"/></strong>
                            <div class="os-row__meta"><span><c:out value="${quiz.meta}"/></span><span><c:out value="${quiz.time}"/></span></div>
                        </div>
                        <os:pill status="${quiz.status}"/>
                    </li>
                </c:forEach>
            </ul>
            <c:if test="${empty recentQuizzes}">
                <div class="os-empty"><strong>No quizzes yet</strong>New quizzes from your teachers will show up here.</div>
            </c:if>
        </section>

        <section class="os-panel tone-peach" aria-labelledby="recentAssignmentsTitle">
            <div class="os-panel__head">
                <span class="os-panel__icon"><os:icon name="assignment"/></span>
                <h2 id="recentAssignmentsTitle">Recent Assignments</h2>
                <a class="os-panel__link" href="${base}/student/assignments">View All<os:icon name="chevron-right"/></a>
            </div>
            <ul class="os-list">
                <c:forEach var="assignment" items="${recentAssignments}" varStatus="loop">
                    <li class="os-row tone-${tones[loop.index % 4]}" data-os-searchable>
                        <span class="os-tile" aria-hidden="true">A</span>
                        <div class="os-row__body">
                            <strong class="os-row__title"><c:out value="${assignment.title}"/></strong>
                            <div class="os-row__meta"><span><c:out value="${assignment.meta}"/></span><span><c:out value="${assignment.time}"/></span></div>
                        </div>
                        <os:pill status="${assignment.status}"/>
                    </li>
                </c:forEach>
            </ul>
            <c:if test="${empty recentAssignments}">
                <div class="os-empty"><strong>No assignments yet</strong>Assignments from your teachers will show up here.</div>
            </c:if>
        </section>
    </div>
</main>
<script src="${base}/js/student-dashboard.js"></script>
</body>
</html>
