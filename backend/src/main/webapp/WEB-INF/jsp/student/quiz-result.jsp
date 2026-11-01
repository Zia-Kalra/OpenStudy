<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quiz Result | OpenStudy</title>
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/student-dashboard.css">
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/student-quiz.css">
</head>
<body>
<div class="student-wrapper">
    <jsp:include page="fragments/sidebar.jsp"/>
    <main class="student-main">
        <jsp:include page="fragments/navbar.jsp"/>
        <div class="content-shell quiz-workspace">
            <header class="quiz-header">
                <div><p>QUIZ RESULT</p><h1><c:out value="${result.quizTitle}"/></h1><p><c:out value="${result.subjectName}"/></p></div>
            </header>
            <section class="quiz-surface">
                <div class="quiz-result-score">
                    <div class="quiz-score-item"><span>Score</span><strong><c:out value="${result.score}"/> / <c:out value="${result.maximumMarks}"/></strong></div>
                    <div class="quiz-score-item"><span>Percentage</span><strong><c:out value="${result.percentage}"/>%</strong></div>
                    <div class="quiz-score-item"><span>Submitted</span><strong><c:out value="${result.submittedAt}"/></strong></div>
                    <div class="quiz-score-item"><span>Status</span><strong><c:out value="${result.status}"/></strong></div>
                </div>
                <h2>Question review</h2>
                <c:forEach var="question" items="${result.questions}" varStatus="loop">
                    <article class="quiz-result-question ${question.outcome}">
                        <h3>Question <c:out value="${loop.count}"/> · <c:out value="${question.outcome}"/></h3>
                        <p><c:out value="${question.question_text}"/></p>
                        <p>Your answer: <c:out value="${question.selected_answer}" default="Unanswered"/></p>
                        <p>Correct answer: <c:out value="${question.correct_answer}"/></p>
                        <p>Marks: <c:out value="${question.awarded_points}" default="0"/> / <c:out value="${question.points}"/></p>
                    </article>
                </c:forEach>
                <div class="quiz-page-footer"><a class="quiz-button" href="${fn:escapeXml(pageContext.request.contextPath)}/student/quizzes">Back to available quizzes</a></div>
            </section>
        </div>
    </main>
</div>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/student-dashboard.js"></script>
</body>
</html>
