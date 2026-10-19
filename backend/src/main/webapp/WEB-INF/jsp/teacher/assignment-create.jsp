<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html><html lang="en"><head>
<meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Create Assignment · OpenStudy</title>
<link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/teacher-dashboard.css">
<link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/assignments/assignments.css">
</head><body class="teacher-shell" data-context-path="${fn:escapeXml(pageContext.request.contextPath)}" data-assignment-id="${assignmentId}">
<div class="layout"><jsp:include page="fragments/sidebar.jsp"/><div class="layout-main">
<main class="content teacher-create-content">
  <header class="teacher-assignment-heading"><div><p class="eyebrow">ASSIGNMENTS / CREATE</p><h2>Create New Assignment</h2><p>Provide basic details, then add questions.</p></div><a class="light-link" href="${fn:escapeXml(pageContext.request.contextPath)}/teacher/assignments">Back to Assignments</a></header>
  <div id="createNotice" class="assignment-notice" role="status" aria-live="polite"></div>
  <section id="detailsStep" class="panel create-panel"><h3>Assignment details</h3><p class="panel-subtitle">Set the due date and timed attempt separately.</p>
    <form id="assignmentDetailsForm">
      <label>Title *<input id="assignmentTitle" maxlength="200" required></label>
      <label>Subject *<select id="assignmentSubject" required><option value="">Choose a subject</option><option value="__add_subject__">＋ Add a subject…</option></select></label>
      <label class="create-description">Description *<textarea id="assignmentDescription" maxlength="10000" required></textarea></label>
      <label>Total marks *<input id="totalMarks" type="number" min="0.01" step="0.01" required placeholder="Enter maximum marks"></label>
      <label>Due date *<input id="assignmentDue" type="datetime-local" required></label>
      <label>Instructions (optional)<textarea id="assignmentInstructions" maxlength="5000"></textarea></label>
      <fieldset class="attempt-duration"><legend>Attempt duration *</legend><label>Hours<input id="attemptHours" type="number" min="0" max="720" value="1" required></label><label>Minutes<input id="attemptMinutes" type="number" min="0" max="59" value="0" required></label></fieldset>
      <div class="create-actions"><button class="primary-link" type="submit">Next: Add Questions <span aria-hidden="true">→</span></button></div>
    </form>
  </section>
  <section id="questionsStep" class="create-questions is-hidden">
    <div class="panel"><div class="create-step-heading"><div><h3>Add Assignment Questions</h3><p class="panel-subtitle">Question marks must add up to the total marks you entered.</p></div><span class="total-pill">Question marks <strong id="questionTotal">0</strong> / <span id="assignmentMax">0</span></span></div>
      <form id="questionForm" class="question-form">
        <label>Question *<textarea id="questionText" maxlength="5000" required></textarea></label>
        <div class="form-grid"><label>Question type<select id="questionType"><option value="LONG_TEXT">Long answer</option><option value="SHORT_TEXT">Short answer</option></select></label><label>Marks *<input id="questionPoints" type="number" min="0.01" step="0.01" required></label></div>
        <label>Model answer *<textarea id="modelAnswer" maxlength="10000" required></textarea></label><label>Rubric (optional)<textarea id="questionRubric" maxlength="10000"></textarea></label>
        <div class="create-actions"><button type="submit" class="primary-link">Add Question</button></div>
      </form>
    </div>
    <div class="panel"><h3>Questions</h3><ol id="draftQuestions" class="draft-question-list"></ol><p id="noQuestions" class="panel-subtitle">No questions added yet.</p></div>
    <details class="panel ai-question-panel"><summary>Generate question suggestions with AI</summary><p class="panel-subtitle">Review every suggestion before adding it to the assignment.</p>
      <form id="suggestionForm" class="form-grid"><label>Topic<input id="suggestionTopic" required maxlength="300"></label><label>Number of questions<input id="suggestionCount" type="number" min="1" max="20" value="3"></label><label>Difficulty<select id="suggestionDifficulty"><option>Easy</option><option selected>Medium</option><option>Hard</option></select></label><label>Marks each<input id="suggestionPoints" type="number" min="0.01" step="0.01" value="5"></label><button class="light" type="submit">Generate suggestions</button></form><div id="suggestions"></div>
    </details>
    <div class="create-actions create-footer-actions"><button id="backToDetails" class="light" type="button">Back to details</button><button id="publishAssignment" class="primary-link" type="button">Publish Assignment</button></div>
  </section>
</main><jsp:include page="fragments/footer.jsp"/></div></div>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/teacher-dashboard.js"></script><script src="${fn:escapeXml(pageContext.request.contextPath)}/assignments/teacher-assignment-create.js" defer></script>
</body></html>
