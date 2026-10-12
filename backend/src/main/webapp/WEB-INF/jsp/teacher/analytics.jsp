<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Teacher Analytics · OpenStudy</title>
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/teacher-dashboard.css">
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/teacher-analytics.css">
</head>
<body class="teacher-shell">
<div class="layout">
    <jsp:include page="fragments/sidebar.jsp"/>
    <div class="layout-main">
        <jsp:include page="fragments/navbar.jsp"/>
        <main class="content teacher-analytics" id="analyticsContent">
            <header class="ta-hero">
                <div>
                    <p class="ta-eyebrow">CLASS INSIGHTS / TEACHER WORKSPACE</p>
                    <h1>Teacher Analytics</h1>
                    <p>Compare class performance, explore individual progress, and turn results into your next teaching decision.</p>
                </div>
                <div class="ta-hero-meta">
                    <c:if test="${analytics.demo}"><span class="ta-demo">DEMO WORKSPACE</span></c:if>
                    <strong><c:out value="${analytics.overview.totalStudents}"/> assigned students in view</strong>
                    <span><c:out value="${fn:length(analytics.classes)}"/> assigned classes · <c:out value="${fn:length(analytics.subjects)}"/> subjects</span>
                </div>
            </header>
            <c:if test="${analytics.demo}"><p class="ta-notice" role="status">You are exploring sample class results. These records and saved reports are demonstration data.</p></c:if>

            <c:url value="/teacher/analytics" var="overviewUrl"><c:param name="classId" value="${analytics.query.classId}"/><c:param name="subjectId" value="${analytics.query.subjectId}"/></c:url>
            <c:url value="/teacher/analytics/students" var="studentsUrl"><c:param name="classId" value="${analytics.query.classId}"/><c:param name="subjectId" value="${analytics.query.subjectId}"/></c:url>
            <c:url value="/teacher/analytics/assessments" var="assessmentsUrl"><c:param name="classId" value="${analytics.query.classId}"/><c:param name="subjectId" value="${analytics.query.subjectId}"/></c:url>
            <c:url value="/teacher/analytics/reports" var="reportsUrl"><c:param name="classId" value="${analytics.query.classId}"/><c:param name="subjectId" value="${analytics.query.subjectId}"/></c:url>
            <nav class="ta-tabs" aria-label="Teacher analytics sections">
                <a href="${fn:escapeXml(overviewUrl)}" class="${section == 'overview' ? 'is-active' : ''}" <c:if test="${section == 'overview'}">aria-current="page"</c:if>>Class Overview</a>
                <a href="${fn:escapeXml(studentsUrl)}" class="${section == 'students' ? 'is-active' : ''}" <c:if test="${section == 'students'}">aria-current="page"</c:if>>Student Explorer</a>
                <a href="${fn:escapeXml(assessmentsUrl)}" class="${section == 'assessments' ? 'is-active' : ''}" <c:if test="${section == 'assessments'}">aria-current="page"</c:if>>Assessment Analytics</a>
                <a href="${fn:escapeXml(reportsUrl)}" class="${section == 'reports' ? 'is-active' : ''}" <c:if test="${section == 'reports'}">aria-current="page"</c:if>>Advanced R Analytics</a>
            </nav>

            <c:set var="pagePath" value="/teacher/analytics"/>
            <c:if test="${section != 'overview'}"><c:set var="pagePath" value="/teacher/analytics/${section}"/></c:if>
            <c:url value="${pagePath}" var="filterAction"/>
            <form class="ta-filters" method="get" action="${fn:escapeXml(filterAction)}">
                <label>Class
                    <select name="classId">
                        <option value="all">All assigned classes</option>
                        <c:forEach var="cohort" items="${analytics.classes}"><option value="${fn:escapeXml(cohort.id)}" ${analytics.query.classId == cohort.id ? 'selected' : ''}><c:out value="${cohort.label}"/></option></c:forEach>
                    </select>
                </label>
                <label>Subject
                    <select name="subjectId">
                        <option value="all">All assigned subjects</option>
                        <c:forEach var="subject" items="${analytics.subjects}"><option value="${fn:escapeXml(subject.id)}" ${analytics.query.subjectId == subject.id ? 'selected' : ''}><c:out value="${subject.label}"/></option></c:forEach>
                    </select>
                </label>
                <c:if test="${section == 'students'}">
                    <input type="hidden" name="search" value="${fn:escapeXml(analytics.query.search)}">
                    <input type="hidden" name="band" value="${fn:escapeXml(analytics.query.band)}">
                    <input type="hidden" name="sort" value="${fn:escapeXml(analytics.query.sort)}">
                </c:if>
                <c:if test="${section == 'reports'}"><input type="hidden" name="method" value="${fn:escapeXml(analytics.query.method)}"></c:if>
                <button type="submit" class="ta-button">Apply filters</button>
                <a href="${fn:escapeXml(filterAction)}" class="ta-reset">Reset</a>
                <span class="ta-scope-note">Assigned classes only</span>
            </form>
            <jsp:include page="analytics/${section}.jsp"/>
        </main>
        <jsp:include page="fragments/footer.jsp"/>
    </div>
</div>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/teacher-dashboard.js"></script>
</body>
</html>
