<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<section class="ta-panel ta-insight" aria-labelledby="aiInsightHeading">
    <div class="ta-panel-heading"><div><p class="ta-kicker">CALCULATED FACTS / GROQ INSIGHTS</p><h3 id="aiInsightHeading">${section == 'students' ? 'AI Student Summary' : 'AI Class Insights'}</h3><p>Groq selects relevant recorded facts and an eligible next step. All displayed numbers are calculated by OpenStudy.</p></div></div>
    <c:choose>
        <c:when test="${analytics.demo}"><p class="ta-empty">AI summaries require real Aiven results; demo data is never sent to Groq.</p></c:when>
        <c:when test="${analytics.overview.totalStudents == 0 or empty analytics.assessments}"><p class="ta-empty">No recorded assessment data is available for an AI summary in this scope.</p></c:when>
        <c:otherwise>
            <c:choose><c:when test="${section == 'students'}"><c:url value="/teacher/analytics/students/${analytics.selectedStudent.summary.student.id}/insights" var="insightAction"/></c:when><c:otherwise><c:url value="/teacher/analytics/insights" var="insightAction"/></c:otherwise></c:choose>
            <form method="post" action="${fn:escapeXml(insightAction)}">
                <input type="hidden" name="insightToken" value="${fn:escapeXml(analyticsInsightToken)}">
                <input type="hidden" name="subjectId" value="${fn:escapeXml(analytics.query.subjectId)}">
                <input type="hidden" name="classId" value="${fn:escapeXml(analytics.query.classId)}">
                <input type="hidden" name="search" value="${fn:escapeXml(analytics.query.search)}">
                <input type="hidden" name="band" value="${fn:escapeXml(analytics.query.band)}">
                <input type="hidden" name="sort" value="${fn:escapeXml(analytics.query.sort)}">
                <button type="submit" class="ta-button">Generate AI summary</button>
            </form>
            <p>Only anonymous calculated facts are sent. Names, answers and teacher feedback stay in OpenStudy.</p>
        </c:otherwise>
    </c:choose>
    <c:if test="${not empty aiInsight}">
        <c:if test="${not empty aiInsight.message}"><p class="ta-notice" role="status"><c:out value="${aiInsight.message}"/></p></c:if>
        <c:if test="${not empty aiInsight.observations}"><ul><c:forEach var="observation" items="${aiInsight.observations}"><li><c:out value="${observation}"/></li></c:forEach></ul><p><strong>Suggested next step:</strong> <c:out value="${aiInsight.recommendation}"/></p></c:if>
    </c:if>
</section>
