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
    <title>Assignments · OpenStudy</title>
    <link rel="stylesheet" href="${base}/css/openstudy.css">
</head>
<body>
<jsp:include page="fragments/topnav.jsp"/>

<main class="os-page">
    <section class="os-hero os-hero--compact">
        <div>
            <h1>Available Assignments</h1>
            <p class="os-hero__sub">Submit your solutions before the deadline</p>
        </div>
    </section>

    <section class="os-panel tone-peach" aria-labelledby="assignmentListTitle">
        <div class="os-panel__head">
            <span class="os-panel__icon"><os:icon name="assignment"/></span>
            <div>
                <h2 id="assignmentListTitle">All Assignments</h2>
                <p><c:out value="${fn:length(assignments)}"/> assignments assigned to you</p>
            </div>
        </div>
        <ul class="os-list">
            <c:forEach var="assignment" items="${assignments}" varStatus="loop">
                <li class="os-row tone-${tones[loop.index % 4]}" data-os-searchable>
                    <span class="os-tile" aria-hidden="true">A</span>
                    <div class="os-row__body">
                        <strong class="os-row__title"><c:out value="${assignment.title}"/></strong>
                        <div class="os-row__meta">
                            <span><c:out value="${assignment.details}"/></span>
                            <span><c:out value="${assignment.marks}"/></span>
                            <span><c:out value="${assignment.due}"/></span>
                        </div>
                    </div>
                    <div class="os-row__aside">
                        <os:pill status="${assignment.status}"/>
                        <button type="button" class="os-btn os-btn--ghost os-btn--sm" data-action="show-details" data-item="${fn:escapeXml(assignment.title)}">View details</button>
                    </div>
                </li>
            </c:forEach>
        </ul>
        <c:if test="${empty assignments}">
            <div class="os-empty"><strong>No assignments available</strong>Check back when your teacher publishes one.</div>
        </c:if>
    </section>
</main>
<script src="${base}/js/student-dashboard.js"></script>
</body>
</html>
