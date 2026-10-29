<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="base" value="${fn:escapeXml(pageContext.request.contextPath)}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Assignments · OpenStudy</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/student-dashboard.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/student-assignments.css">
</head>
<body data-context-path="${pageContext.request.contextPath}">
<jsp:include page="fragments/topnav.jsp"/>
<div class="student-wrapper">
    <main class="student-main student-assignment-main">
        <div class="content-shell">
            <div class="student-assignment-search">
                <div class="searchbox"><span>⌕</span><input id="studentAssignmentSearch" type="search" placeholder="Search by assignment or subject..." aria-label="Search assignments by assignment or subject"></div>
            </div>
            <section class="page-header" id="studentAssignmentsHeader"><h1 id="studentAssignmentsTitle">Pending Assignments</h1><p id="studentAssignmentsDescription">Published assignments that still need your submission.</p></section>
            <div id="studentAssignmentNotice" class="student-assignment-notice" role="status" aria-live="polite"></div>
            <section class="panel student-assignment-panel" id="studentAssignmentListPanel">
                <div class="panel-header"><div><h3>My assignments</h3></div><span id="pendingAssignmentCount" class="assignment-count">Loading…</span></div>
                <div class="student-assignment-tabs" role="tablist" aria-label="Assignment activity">
                    <button type="button" id="pendingAssignmentsTab" class="active" role="tab" aria-selected="true">Pending</button>
                    <button type="button" id="attemptedAssignmentsTab" role="tab" aria-selected="false">Attempted</button>
                </div>
                <div class="student-assignment-table-wrap">
                    <table class="student-assignment-table">
                        <thead><tr><th>No.</th><th>Assignment topic</th><th>Subject</th><th id="assignmentDateHeading">Creation date</th><th>Due date</th><th>Marks</th><th>Activity</th></tr></thead>
                        <tbody id="pendingAssignmentRows"><tr><td colspan="7" class="student-assignment-empty">Loading published assignments…</td></tr></tbody>
                    </table>
                </div>
            </section>
            <section id="studentSubmittedAttemptPanel" class="panel student-attempt-panel" hidden>
                <div class="student-attempt-heading"><div><h2 id="submittedAttemptTitle">Submitted attempt</h2><p id="submittedAttemptMeta"></p></div><button id="backToAttemptedList" class="secondary-button" type="button">Back to attempted assignments</button></div>
                <div id="studentSubmittedQuestions" class="student-attempt-questions"></div>
            </section>
            <section id="studentAttemptPanel" class="panel student-attempt-panel" hidden>
                <div class="student-attempt-heading"><div><h2 id="studentAttemptTitle">Assignment attempt</h2><p id="studentAttemptDue"></p></div><div class="student-attempt-timer"><span>Time remaining</span><strong id="studentAttemptTimer">--:--:--</strong></div></div>
                <div id="studentAttemptQuestions" class="student-attempt-questions"></div>
                <div class="student-attempt-actions"><span id="studentAnswerSaveStatus" class="student-answer-save-status">Answers save automatically.</span><button id="submitStudentAttempt" class="primary-button" type="button">Submit assignment</button></div>
            </section>
        </div>
    </main>
</div>
<dialog id="attemptConfirmDialog" class="student-confirm-dialog">
    <form method="dialog" class="student-confirm-dialog-card">
        <p class="dialog-eyebrow">READY TO BEGIN?</p>
        <h2 id="attemptConfirmTitle">Assignment</h2>
        <p>Review the attempt details before the timer starts.</p>
        <div class="student-attempt-summary"><div><span>Questions</span><strong id="attemptQuestionCount">0</strong></div><div><span>Time allowed</span><strong id="attemptTimeAllowed">—</strong></div></div>
        <div class="student-dialog-actions"><button type="submit" class="secondary-button" value="cancel">Cancel</button><button type="button" id="beginStudentAttempt" class="primary-button">Begin attempt</button></div>
    </form>
</dialog>
<dialog id="submitConfirmDialog" class="student-confirm-dialog">
    <div class="student-confirm-dialog-card">
        <p class="dialog-eyebrow">SUBMISSION REVIEW</p>
        <h2>Review your answers</h2>
        <div class="student-submit-summary"><section><h3>Questions attempted</h3><strong id="attemptedCount">0</strong></section><section><h3>Unattempted</h3><strong id="unattemptedCount">0</strong><p id="unattemptedQuestionNumbers">None</p></section><section><h3>Total marks</h3><p id="submitTotalMarks">0</p></section><section><h3>Attachments</h3><p id="submitAttachmentCount">0 files</p></section></div>
        <p class="student-submit-confirm-copy">Are you sure you want to submit this assignment? You cannot edit your answers after submission.</p>
        <div class="student-dialog-actions"><button type="button" id="cancelSubmitAttempt" class="secondary-button">Go back</button><button type="button" id="confirmSubmitAttempt" class="primary-button">Yes, submit</button></div>
    </div>
</dialog>
<dialog id="submissionResultDialog" class="student-confirm-dialog">
    <div class="student-confirm-dialog-card">
        <p class="dialog-eyebrow">ASSIGNMENT SUBMITTED</p>
        <h2 id="submissionResultTitle">Time is up</h2>
        <p id="submissionResultMessage">Your saved answers have been submitted automatically.</p>
        <div class="student-submit-summary"><section><h3>Questions attempted</h3><strong id="resultAttemptedCount">0</strong></section><section><h3>Unattempted</h3><strong id="resultUnattemptedCount">0</strong><p id="resultUnattemptedNumbers">None</p></section><section><h3>Total marks</h3><p id="resultTotalMarks">0</p></section><section><h3>Attachments</h3><p id="resultAttachmentCount">0 files</p></section></div>
        <div class="student-dialog-actions"><button type="button" id="closeSubmissionResult" class="primary-button">Close</button></div>
    </div>
</dialog>
<script src="${pageContext.request.contextPath}/js/student-dashboard.js"></script>
<script src="${pageContext.request.contextPath}/assignments/student-assignments.js" defer></script>
</body>
</html>
