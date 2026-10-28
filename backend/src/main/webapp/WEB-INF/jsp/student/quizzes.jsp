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
    <title>Quizzes · OpenStudy</title>
    <link rel="stylesheet" href="${base}/css/openstudy.css">
</head>
<body>
<jsp:include page="fragments/topnav.jsp"/>

<main class="os-page">
    <section class="os-hero os-hero--compact">
        <div>
            <h1>Available Quizzes</h1>
            <p class="os-hero__sub">Attempt quizzes to test your understanding</p>
        </div>
    </section>

    <section class="os-panel tone-purple" aria-labelledby="quizListTitle">
        <div class="os-panel__head">
            <span class="os-panel__icon"><os:icon name="quiz"/></span>
            <div>
                <h2 id="quizListTitle">All Quizzes</h2>
                <p><c:out value="${fn:length(quizzes)}"/> quizzes assigned to you</p>
            </div>
        </div>
        <ul class="os-list">
            <c:forEach var="quiz" items="${quizzes}" varStatus="loop">
                <li class="os-row tone-${tones[loop.index % 4]}" data-os-searchable>
                    <span class="os-tile" aria-hidden="true">Q</span>
                    <div class="os-row__body">
                        <strong class="os-row__title"><c:out value="${quiz.title}"/></strong>
                        <div class="os-row__meta">
                            <span><c:out value="${quiz.questions}"/></span>
                            <span><c:out value="${quiz.marks}"/></span>
                            <c:if test="${not empty quiz.difficulty}"><span><c:out value="${quiz.difficulty}"/></span></c:if>
                            <span><c:out value="${quiz.due}"/></span>
                        </div>
                    </div>
                    <div class="os-row__aside">
                        <os:pill status="${quiz.status}"/>
                        <button type="button" class="os-btn os-btn--ghost os-btn--sm" data-action="show-details" data-item="${fn:escapeXml(quiz.title)}">View details</button>
                    </div>
                </li>
            </c:forEach>
        </ul>
        <c:if test="${empty quizzes}">
            <div class="os-empty"><strong>No quizzes available</strong>Check back when your teacher publishes one.</div>
        </c:if>
    </section>
</main>
<script src="${base}/js/student-dashboard.js"></script>
</body>
</html>
