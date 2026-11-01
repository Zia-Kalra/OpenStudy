<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quiz Attempt | OpenStudy</title>
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/student-dashboard.css">
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/student-quiz.css">
</head>
<body>
<div class="student-wrapper">
    <jsp:include page="fragments/sidebar.jsp"/>
    <main class="student-main">
        <jsp:include page="fragments/navbar.jsp"/>
        <div class="content-shell quiz-workspace" id="quizAttemptPage"
             data-attempt-id="${attemptId}" data-api="${fn:escapeXml(pageContext.request.contextPath)}/api/student">
            <header class="quiz-header">
                <div><h1 id="attemptTitle">Loading quiz…</h1><p id="attemptMetadata"></p></div>
                <div id="quizTimer" class="quiz-timer" role="timer" aria-live="off" hidden></div>
            </header>
            <div id="attemptStatus" class="quiz-save-status" role="status" aria-live="polite"></div>
            <div class="quiz-attempt-layout">
                <section class="quiz-surface quiz-question" aria-label="Current quiz question">
                    <div class="quiz-attempt-top">
                        <strong id="questionCounter"></strong>
                        <span id="questionPoints"></span>
                    </div>
                    <h2 id="questionText"></h2>
                    <div id="answerOptions" class="quiz-options" role="radiogroup" aria-labelledby="questionText"></div>
                    <div class="quiz-question-controls">
                        <button type="button" id="previousQuestion" class="quiz-button secondary">Previous</button>
                        <button type="button" id="markForReview" class="quiz-review-toggle" aria-pressed="false">Mark for review</button>
                        <button type="button" id="nextQuestion" class="quiz-button">Next</button>
                    </div>
                    <div class="quiz-page-footer"><a id="confirmSubmit" class="quiz-button" href="#">Review and submit</a></div>
                </section>
                <aside class="quiz-surface" aria-label="Question navigation">
                    <h2>Questions</h2>
                    <div class="quiz-summary-list"><span id="answeredCount">Answered: 0</span><span id="unansweredCount">Unanswered: 0</span></div>
                    <nav id="questionNavigator" class="quiz-number-grid" aria-label="Choose a question"></nav>
                    <div class="quiz-legend"><span>Green: answered</span><span>Purple underline: review</span><span>Outline: current</span></div>
                </aside>
            </div>
        </div>
    </main>
</div>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/student-dashboard.js"></script>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/student-quiz.js"></script>
</body>
</html>
