<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Submit Quiz | OpenStudy</title>
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/student-dashboard.css">
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/student-quiz.css">
</head>
<body>
<div class="student-wrapper">
    <jsp:include page="fragments/sidebar.jsp"/>
    <main class="student-main">
        <jsp:include page="fragments/navbar.jsp"/>
        <div class="content-shell quiz-workspace" id="quizSubmitPage"
             data-attempt-id="${attemptId}" data-api="${fn:escapeXml(pageContext.request.contextPath)}/api/student">
            <header class="quiz-header"><div><h1>Submit quiz?</h1><p id="submitQuizTitle">Loading attempt…</p></div></header>
            <section class="quiz-surface" aria-labelledby="submitHeading">
                <h2 id="submitHeading">Final check</h2>
                <p id="submitCounts" class="quiz-summary-list"></p>
                <p id="submitWarning" class="quiz-confirm-warning" role="alert" hidden></p>
                <p id="submitStatus" role="status" aria-live="polite"></p>
                <div class="quiz-page-footer">
                    <a id="continueAttempt" class="quiz-button secondary" href="#">Continue attempt</a>
                    <button id="submitQuiz" class="quiz-button" type="button">Submit quiz</button>
                </div>
            </section>
        </div>
    </main>
</div>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/student-dashboard.js"></script>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/student-quiz.js"></script>
</body>
</html>
