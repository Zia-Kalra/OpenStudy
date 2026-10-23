<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
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
<div class="student-wrapper">
    <aside class="student-sidebar" id="studentSidebar">
        <div class="brand-block"><div class="brand-icon">OS</div><div class="brand-meta"><strong>OpenStudy</strong></div></div>
        <nav class="student-side-nav" aria-label="Student navigation">
            <a href="${pageContext.request.contextPath}/student/dashboard"><span class="side-icon">⌂</span> Dashboard</a>
            <a href="${pageContext.request.contextPath}/student/quizzes"><span class="side-icon">▣</span> Quizzes</a>
            <a class="active" href="${pageContext.request.contextPath}/student/assignments"><span class="side-icon">✎</span> Assignments</a>
            <a href="${pageContext.request.contextPath}/student/results"><span class="side-icon">▤</span> Results</a>
            <a href="${pageContext.request.contextPath}/student/performance"><span class="side-icon">◎</span> Performance</a>
            <a href="${pageContext.request.contextPath}/student/profile"><span class="side-icon">☻</span> Profile</a>
            <a href="${pageContext.request.contextPath}/logout"><span class="side-icon">↩</span> Logout</a>
        </nav>
    </aside>
    <main class="student-main student-assignment-main">
        <header class="student-topbar">
            <button class="icon-button sidebar-toggle" id="studentSidebarToggle" aria-label="Toggle navigation" data-action="toggle-sidebar">☰</button>
            <div class="searchbox"><span>⌕</span><input type="text" placeholder="Search assignments..." aria-label="Search assignments"></div>
            <div class="topbar-actions"><div class="user-chip"><div class="avatar-circle"><c:out value="${currentUserInitial}"/></div><div class="user-name"><c:out value="${currentUserName}"/></div></div></div>
        </header>
        <div class="content-shell">
            <section class="page-header" id="studentAssignmentsHeader"><h1>Pending Assignments</h1><p>Published assignments that still need your submission.</p></section>
            <div id="studentAssignmentNotice" class="student-assignment-notice" role="status" aria-live="polite"></div>
            <section class="panel student-assignment-panel" id="studentAssignmentListPanel">
                <div class="panel-header"><div><h3>My assignments</h3><p>Complete each attempt before its due date.</p></div><span id="pendingAssignmentCount" class="assignment-count">Loading…</span></div>
                <div class="student-assignment-table-wrap">
                    <table class="student-assignment-table">
                        <thead><tr><th>No.</th><th>Assignment topic</th><th>Subject</th><th>Creation date</th><th>Due date</th><th>Marks</th><th>Activity</th></tr></thead>
                        <tbody id="pendingAssignmentRows"><tr><td colspan="7" class="student-assignment-empty">Loading published assignments…</td></tr></tbody>
                    </table>
                </div>
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
        <div class="student-submit-summary"><section><h3>Questions attempted <span id="attemptedCount">0</span></h3><p id="attemptedQuestionNumbers">None</p></section><section><h3>Unattempted <span id="unattemptedCount">0</span></h3><p id="unattemptedQuestionNumbers">None</p></section><section><h3>Total marks</h3><p id="submitTotalMarks">0</p></section><section><h3>Attachments</h3><p id="submitAttachmentCount">0 files</p></section></div>
        <p class="student-submit-confirm-copy">Are you sure you want to submit this assignment? You cannot edit your answers after submission.</p>
        <div class="student-dialog-actions"><button type="button" id="cancelSubmitAttempt" class="secondary-button">Go back</button><button type="button" id="confirmSubmitAttempt" class="primary-button">Yes, submit</button></div>
    </div>
</dialog>
<dialog id="submissionResultDialog" class="student-confirm-dialog">
    <div class="student-confirm-dialog-card">
        <p class="dialog-eyebrow">ASSIGNMENT SUBMITTED</p>
        <h2 id="submissionResultTitle">Time is up</h2>
        <p id="submissionResultMessage">Your saved answers have been submitted automatically.</p>
        <div class="student-submit-summary"><section><h3>Questions attempted <span id="resultAttemptedCount">0</span></h3><p id="resultAttemptedNumbers">None</p></section><section><h3>Unattempted <span id="resultUnattemptedCount">0</span></h3><p id="resultUnattemptedNumbers">None</p></section><section><h3>Total marks</h3><p id="resultTotalMarks">0</p></section><section><h3>Attachments</h3><p id="resultAttachmentCount">0 files</p></section></div>
        <div class="student-dialog-actions"><button type="button" id="closeSubmissionResult" class="primary-button">Close</button></div>
    </div>
</dialog>
<script src="${pageContext.request.contextPath}/js/student-dashboard.js"></script>
<script src="${pageContext.request.contextPath}/assignments/student-assignments.js" defer></script>
</body>
</html>
