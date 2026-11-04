<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="base" value="${fn:escapeXml(pageContext.request.contextPath)}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Assignment Result · OpenStudy</title>
    <link rel="stylesheet" href="${base}/css/openstudy.css">
    <link rel="stylesheet" href="${base}/css/student-assignment-result.css">
</head>
<body data-context-path="${base}" data-submission-id="${submissionId}">
<jsp:include page="fragments/topnav.jsp"/>
<main class="os-page">
    <section class="os-hero os-hero--compact">
        <div>
            <p class="os-hero__eyebrow">Assignment feedback</p>
            <h1 id="resultTitle">Loading result…</h1>
            <p id="resultMeta" class="os-hero__sub">Loading your evaluated answers.</p>
        </div>
        <a class="os-btn os-btn--ghost" href="${base}/student/results">Back to results</a>
    </section>
    <div id="resultMessage" class="os-alert os-alert--info" role="status" aria-live="polite">Loading evaluation…</div>
    <section id="resultSummary" class="student-result-summary" hidden>
        <article class="os-panel"><span>Total marks</span><strong id="resultMarks"></strong></article>
        <article class="os-panel"><span>Result</span><strong id="resultStatus"></strong></article>
        <article class="os-panel"><span>Evaluation</span><strong id="resultType"></strong></article>
    </section>
    <section id="resultAnswers" class="student-result-answers" aria-label="Evaluated answers"></section>
</main>
<script src="${base}/js/student-assignment-result.js" defer></script>
</body>
</html>
