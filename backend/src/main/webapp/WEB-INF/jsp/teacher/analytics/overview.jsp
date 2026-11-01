<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<section aria-labelledby="overviewHeading">
    <div class="ta-section-heading"><div><p class="ta-kicker">THE BIG PICTURE</p><h2 id="overviewHeading">Class Overview</h2></div><span>Normalized scores · 0–100%</span></div>
    <div class="ta-metrics">
        <article><span>Recorded students</span><strong><c:out value="${analytics.overview.totalStudents}"/></strong><p>Enrollments and recorded assessment participants</p></article>
        <article class="ta-featured-metric"><span>Class average</span><strong><c:out value="${analytics.overview.getAverageDisplay()}"/></strong><p><c:out value="${analytics.overview.gradedCount}"/> graded results · equal assessment weight</p></article>
        <article><span>Highest / lowest result</span><strong class="ta-range"><c:out value="${analytics.overview.getHighestDisplay()}"/> <span>/</span> <c:out value="${analytics.overview.getLowestDisplay()}"/></strong><p>Individual assessment scores</p></article>
        <article><span>Submission rate</span><strong><c:out value="${analytics.overview.getSubmissionDisplay()}"/></strong><p>Submitted and graded / expected work</p></article>
    </div>
</section>
<jsp:include page="insights.jsp"/>
<div class="ta-two-column">
    <section class="ta-panel" aria-labelledby="trendHeading">
        <div class="ta-panel-heading"><div><h3 id="trendHeading">Quiz vs assignment trends</h3><p>Weekly means of graded results. Missing work is not scored as zero.</p></div><span class="ta-label">${analytics.demo ? '4-week demo period' : 'Recorded assessment dates'}</span></div>
        <c:if test="${analytics.overview.gradedCount == 0}"><p class="ta-empty">No graded results are available for the selected student scope yet.</p></c:if>
        <c:if test="${analytics.overview.gradedCount > 0}">
        <div class="ta-legend"><span class="ta-quiz-key">Quiz <strong><c:out value="${analytics.overview.getQuizDisplay()}"/></strong></span><span class="ta-assignment-key">Assignment <strong><c:out value="${analytics.overview.getAssignmentDisplay()}"/></strong></span></div>
        <div class="ta-trend-chart" role="group" aria-label="Weekly quiz and assignment averages; exact values are listed below each pair of bars">
            <c:forEach var="point" items="${analytics.overview.trends}">
                <div class="ta-trend-group"><div class="ta-bar-pair"><div class="ta-quiz-bar" style="height:${point.getQuizHeight()}%"></div><div class="ta-assignment-bar" style="height:${point.getAssignmentHeight()}%"></div></div><strong><c:out value="${point.label}"/></strong><span><c:out value="${point.getQuizDisplay()}"/> / <c:out value="${point.getAssignmentDisplay()}"/></span></div>
            </c:forEach>
        </div>
        </c:if>
    </section>
    <section class="ta-panel" aria-labelledby="distributionHeading">
        <div class="ta-panel-heading"><div><h3 id="distributionHeading">Student score distribution</h3><p>Students grouped by their average graded score.</p></div></div>
        <c:if test="${analytics.overview.totalStudents == 0}"><p class="ta-empty">A score distribution requires recorded students in your selected subjects.</p></c:if>
        <c:if test="${analytics.overview.totalStudents > 0}">
        <div class="ta-distribution">
            <c:forEach var="band" items="${analytics.overview.distribution}"><div><div class="ta-bar-label"><span><c:out value="${band.label}"/></span><strong><c:out value="${band.count}"/> students</strong></div><div class="ta-track"><span style="width:${band.percent}%"></span></div></div></c:forEach>
        </div>
        </c:if>
    </section>
</div>
<section class="ta-panel">
    <div class="ta-panel-heading"><div><h3>Compare subjects</h3><p>A consistent percentage scale across quizzes and assignments.</p></div><c:url value="/teacher/analytics/assessments" var="compareUrl"><c:param name="classId" value="${analytics.query.classId}"/><c:param name="subjectId" value="${analytics.query.subjectId}"/></c:url><a class="ta-text-link" href="${fn:escapeXml(compareUrl)}">Explore assessments →</a></div>
    <div class="ta-subject-grid"><c:forEach var="subject" items="${analytics.subjectComparison}"><article><h4><c:out value="${subject.label}"/></h4><strong><c:out value="${subject.getAverageDisplay()}"/></strong><div class="ta-track"><span style="width:${subject.getAverage()}%"></span></div><p>Quiz <b><c:out value="${subject.getQuizDisplay()}"/></b> · Assignment <b><c:out value="${subject.getAssignmentDisplay()}"/></b></p><span><c:out value="${subject.graded}"/> graded results</span></article></c:forEach></div>
    <c:if test="${empty analytics.subjectComparison}"><p class="ta-empty">No subject results are available yet.</p></c:if>
</section>
