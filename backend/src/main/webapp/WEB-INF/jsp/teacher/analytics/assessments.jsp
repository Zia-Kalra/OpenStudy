<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<section class="ta-panel" aria-labelledby="assessmentHeading">
    <div class="ta-panel-heading"><div><p class="ta-kicker">COMPARE THE WORK</p><h2 id="assessmentHeading">Assessment Analytics</h2><p>Search published assignments and available quizzes you created, then select one to inspect recorded student results.</p></div><span class="ta-label"><c:out value="${fn:length(analytics.assessments)}"/> assessments in view</span></div>
    <c:if test="${not empty analytics.subjects}">
        <c:url value="/teacher/analytics/assessments" var="assessmentAction"/>
        <form class="ta-search-form" method="get" action="${fn:escapeXml(assessmentAction)}">
            <input type="hidden" name="subjectId" value="${fn:escapeXml(analytics.query.subjectId)}">
            <c:if test="${not empty analytics.classes}"><input type="hidden" name="classId" value="${fn:escapeXml(analytics.query.classId)}"></c:if>
            <label class="ta-search">Assessment title<input type="search" name="assessmentSearch" maxlength="100" placeholder="Assignment or quiz title" value="${fn:escapeXml(analytics.query.assessmentSearch)}"></label>
            <button type="submit" class="ta-button">Find assessments</button>
            <c:url value="/teacher/analytics/assessments" var="clearAssessments"><c:param name="subjectId" value="${analytics.query.subjectId}"/><c:if test="${not empty analytics.classes}"><c:param name="classId" value="${analytics.query.classId}"/></c:if></c:url>
            <a class="ta-reset" href="${fn:escapeXml(clearAssessments)}">Clear title search</a>
        </form>
    </c:if>
    <div class="ta-comparison-strip"><div><span>Quiz average</span><strong><c:out value="${analytics.overview.getQuizDisplay()}"/></strong></div><div><span>Assignment average</span><strong><c:out value="${analytics.overview.getAssignmentDisplay()}"/></strong></div><div><span>Submission rate (known roster)</span><strong><c:out value="${analytics.overview.getSubmissionDisplay()}"/></strong></div></div>
    <div class="ta-table-scroll"><table class="ta-table"><caption>Assessments in your authorized subject scope</caption><thead><tr><th scope="col">Assessment</th><th scope="col">Subject</th><th scope="col">Type</th><th scope="col">Date</th><th scope="col">Average</th><th scope="col">Submitted</th><th scope="col">Graded</th><th scope="col">Submission rate</th></tr></thead><tbody>
        <c:forEach var="assessment" items="${analytics.assessments}">
            <c:url value="/teacher/analytics/assessments/${assessment.id}" var="assessmentUrl"><c:param name="subjectId" value="${analytics.query.subjectId}"/><c:param name="classId" value="${analytics.query.classId}"/><c:param name="assessmentSearch" value="${analytics.query.assessmentSearch}"/></c:url>
            <tr class="${analytics.selectedAssessment.summary.id == assessment.id ? 'ta-selected-row' : ''}"><th scope="row"><a class="ta-text-link" href="${fn:escapeXml(assessmentUrl)}"><c:out value="${assessment.title}"/></a></th><td><c:out value="${assessment.subject}"/></td><td><span class="ta-type ta-${fn:escapeXml(assessment.type)}"><c:out value="${assessment.type}"/></span></td><td><c:out value="${assessment.date}"/></td><td><b><c:out value="${assessment.getAverageDisplay()}"/></b></td><td><c:out value="${assessment.submitted}"/></td><td><c:out value="${assessment.graded}"/></td><td><b><c:out value="${assessment.getSubmissionDisplay()}"/></b><span class="ta-cell-subtitle">Expected: <c:out value="${assessment.getExpectedDisplay()}"/></span></td></tr>
        </c:forEach>
    </tbody></table></div>
    <c:if test="${empty analytics.assessments}"><p class="ta-empty">No published assignments or available quizzes match this title and subject scope.</p></c:if>
</section>
<c:if test="${not empty analytics.selectedAssessment}">
    <c:set var="selected" value="${analytics.selectedAssessment}"/>
    <section class="ta-panel" aria-labelledby="selectedAssessmentHeading">
        <div class="ta-panel-heading"><div><p class="ta-kicker">RECORDED STUDENT RESULTS</p><h3 id="selectedAssessmentHeading"><c:out value="${selected.summary.title}"/></h3><p><c:out value="${selected.summary.subject}"/> · Maximum <c:out value="${selected.maximumScore}"/> marks</p></div><span class="ta-label"><c:out value="${selected.summary.submitted}"/> submitted · <c:out value="${selected.summary.graded}"/> graded</span></div>
        <div class="ta-table-scroll"><table class="ta-table"><caption>Student results for the selected assessment</caption><thead><tr><th scope="col">Student</th><th scope="col">Status</th><th scope="col">Final marks</th><th scope="col">Percentage</th><th scope="col">Feedback</th></tr></thead><tbody>
            <c:forEach var="result" items="${selected.results}">
                <c:url value="/teacher/analytics/students/${result.studentId}" var="resultStudentUrl"><c:param name="subjectId" value="${analytics.query.subjectId}"/></c:url>
                <tr><th scope="row"><a class="ta-text-link" href="${fn:escapeXml(resultStudentUrl)}"><c:out value="${result.name}"/></a></th><td><c:out value="${result.status}"/></td><td><c:out value="${result.getScoreDisplay()}"/></td><td><c:out value="${result.getPercentageDisplay()}"/></td><td class="ta-feedback-cell"><c:out value="${result.feedback}"/></td></tr>
            </c:forEach>
        </tbody></table></div>
        <c:if test="${empty selected.results}"><p class="ta-empty">No enrollments or completed submissions are recorded for this assessment. No student results are inferred.</p></c:if>
        <p>Only valid final grades enter averages. Submitted work awaiting grading has no final score. Missing work is shown only for known eligible enrollments.</p>
    </section>
</c:if>
<div class="ta-two-column">
    <section class="ta-panel"><div class="ta-panel-heading"><div><h3>Subject comparison</h3><p>Normalized final grades for assessments matching the selected title and subject.</p></div></div><c:forEach var="subject" items="${analytics.subjectComparison}"><div class="ta-student-subject"><div class="ta-bar-label"><strong><c:out value="${subject.label}"/></strong><b><c:out value="${subject.getAverageDisplay()}"/></b></div><div class="ta-track"><span style="width:${subject.getAverage()}%"></span></div><p>Quiz <c:out value="${subject.getQuizDisplay()}"/> · Assignment <c:out value="${subject.getAssignmentDisplay()}"/></p></div></c:forEach><c:if test="${empty analytics.subjectComparison}"><p class="ta-empty">No graded comparison is available.</p></c:if></section>
    <section class="ta-panel ta-insight"><p class="ta-kicker">READ THE NUMBERS IN CONTEXT</p><h3>Recorded work and enrollment coverage</h3><p>Submissions remain visible even when enrollment links are absent. A submission does not establish a complete class roster; expected-work counts and rates stay unavailable where coverage is unknown.</p><p>AI suggestions and missing work are never counted as final grades.</p></section>
</div>
<section class="ta-panel" aria-labelledby="difficultyHeading"><div class="ta-panel-heading"><div><h3 id="difficultyHeading">Question difficulty</h3><p>Final awarded points from the latest completed quiz attempt; ungraded responses are excluded.</p></div></div>
    <c:choose><c:when test="${not empty analytics.questions}"><div class="ta-table-scroll"><table class="ta-table"><caption>Quiz question response rates</caption><thead><tr><th scope="col">Assessment</th><th scope="col">Question</th><th scope="col">Difficulty</th><th scope="col">Correct / graded responses</th><th scope="col">Correct response rate</th></tr></thead><tbody><c:forEach var="question" items="${analytics.questions}"><tr><th scope="row"><c:out value="${question.assessment}"/></th><td><c:out value="${question.label}"/></td><td><c:out value="${question.difficulty}"/></td><td><c:out value="${question.correct}"/> / <c:out value="${question.answered}"/></td><td><c:out value="${question.getRateDisplay()}"/></td></tr></c:forEach></tbody></table></div></c:when><c:otherwise><p class="ta-empty">No graded quiz-question results are available for this scope.</p></c:otherwise></c:choose>
</section>
