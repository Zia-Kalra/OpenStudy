<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en"><head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Submissions · OpenStudy</title>
  <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/teacher-dashboard.css">
  <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/assignments/assignments.css">
</head>
<body class="teacher-shell" data-context-path="${fn:escapeXml(pageContext.request.contextPath)}">
<div class="layout"><jsp:include page="fragments/sidebar.jsp"/><div class="layout-main">
  <main class="content teacher-assignments-content teacher-submissions-content">
    <header class="teacher-assignment-heading"><div><p class="eyebrow">TEACHER WORKSPACE</p><h2>Submissions</h2><p>Evaluate submissions and review student work.</p></div></header>
    <div id="evaluationNotice" class="assignment-notice" role="status" aria-live="polite"></div>
    <section class="panel evaluation-panel" id="evaluation">
        <div class="evaluation-toolbar"><div><h3 id="evaluationTitle">All Submissions</h3><p>Review student work manually or use AI suggestions, then confirm the marks.</p></div>
        <label class="assignment-filter-label">Assignment<select id="assignmentFilter"><option value="all">All assignments</option></select></label>
      </div>
      <div class="evaluation-tabs" role="tablist" aria-label="Filter submissions">
        <button type="button" class="active" data-filter="all" role="tab" aria-selected="true">All Submissions</button>
        <button type="button" data-filter="pending" role="tab" aria-selected="false">Pending</button>
        <button type="button" data-filter="ai-evaluated" role="tab" aria-selected="false">AI Evaluated</button>
        <button type="button" data-filter="manually-evaluated" role="tab" aria-selected="false">Manually Evaluated</button>
      </div>
      <div class="evaluation-table-wrap"><table class="evaluation-table">
        <thead><tr><th>Student</th><th>Assignment</th><th>Submitted On</th><th>Status</th><th>Actions</th></tr></thead>
        <tbody id="submissionRows"><tr><td colspan="5">Loading submissions…</td></tr></tbody>
      </table></div>
    </section>
    <dialog id="evaluationDialog" class="evaluation-dialog"><div class="dialog-heading"><div><p class="eyebrow">SUBMISSION REVIEW</p><h2 id="reviewHeading">Evaluation</h2></div><button type="button" class="light" id="closeReview">Close</button></div><div id="reviewContent"></div></dialog>
  </main>
  <jsp:include page="fragments/footer.jsp"/>
</div></div>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/teacher-dashboard.js"></script>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/assignments/teacher-assignment-evaluation.js" defer></script>
</body></html>
