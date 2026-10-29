<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Quizzes | OpenStudy</title>
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/student-dashboard.css">
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/student-quiz.css">
</head>
<body>
<div class="student-wrapper">
    <jsp:include page="fragments/sidebar.jsp"/>
    <main class="student-main">
        <jsp:include page="fragments/navbar.jsp"/>
        <div class="content-shell quiz-workspace" id="availableQuizzesPage" data-api="${fn:escapeXml(pageContext.request.contextPath)}/api/student">
            <header class="quiz-header">
                <div><h1>My Quizzes</h1><p>Continue pending quizzes or review submitted attempts.</p></div>
                <div class="searchbox"><span aria-hidden="true">⌕</span><input type="search" id="studentPageSearch" placeholder="Search quizzes" aria-label="Search quizzes"></div>
            </header>
            <div class="quiz-tabs" role="tablist" aria-label="Quiz activity">
                <button type="button" id="pendingQuizzesTab" class="active" role="tab" aria-selected="true">Pending</button>
                <button type="button" id="attemptedQuizzesTab" role="tab" aria-selected="false">Attempted</button>
            </div>
            <div id="quizListStatus" class="quiz-state" role="status" aria-live="polite">Loading available quizzes…</div>
            <section id="quizList" class="quiz-list" aria-label="Available quizzes"></section>
        </div>
    </main>
</div>
<dialog id="quizStartDialog" class="quiz-start-dialog">
    <div class="quiz-start-dialog-card">
        <p class="quiz-dialog-eyebrow">READY TO BEGIN?</p>
        <h2 id="quizPreviewTitle">Quiz</h2>
        <p id="quizPreviewSubject"></p>
        <div class="quiz-preview-summary">
            <div><span>Questions</span><strong id="quizPreviewQuestions">0 questions</strong></div>
            <div><span>Total marks</span><strong id="quizPreviewMarks">0 marks</strong></div>
            <div><span>Time allowed</span><strong id="quizPreviewDuration">No time limit</strong></div>
        </div>
        <h3>Instructions</h3><p id="quizPreviewInstructions"></p>
        <p id="quizPreviewNotice" class="quiz-preview-notice"></p>
        <p id="quizPreviewStatus" class="quiz-preview-error" role="status" aria-live="polite"></p>
        <div class="quiz-preview-actions">
            <button type="button" id="cancelQuizPreview" class="quiz-button secondary">Cancel</button>
            <button type="button" id="beginQuizButton" class="quiz-button">Begin Quiz</button>
        </div>
    </div>
</dialog>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/student-dashboard.js"></script>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/student-quiz.js"></script>
</body>
</html>
