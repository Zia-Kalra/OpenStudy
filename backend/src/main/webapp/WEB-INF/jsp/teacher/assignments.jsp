<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en"><head>
  <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Assignments · OpenStudy</title>
  <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/teacher-dashboard.css">
  <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/assignments/assignments.css">
</head>
<body class="teacher-shell" data-context-path="${fn:escapeXml(pageContext.request.contextPath)}">
<div class="layout"><jsp:include page="fragments/sidebar.jsp"/><div class="layout-main">
  <main class="content teacher-assignments-content">
    <header class="teacher-assignment-heading"><div><p class="eyebrow">TEACHER WORKSPACE</p><h2>Assignments</h2><p>Assignments created by you.</p></div><a class="primary-link assignment-create-button" href="${fn:escapeXml(pageContext.request.contextPath)}/teacher/assignments/create">Create Assignment</a></header>
    <div id="assignmentListNotice" class="assignment-notice" role="status" aria-live="polite"></div>
    <section class="panel teacher-assignment-list-panel">
      <div class="assignment-list-heading"><div><h3>My assignments</h3><p class="panel-subtitle">View each assignment’s details and submission deadline.</p></div><span id="assignmentCount" class="total-pill">0 assignments</span></div>
      <div class="teacher-assignment-table-wrap"><table class="teacher-assignment-table">
        <thead><tr><th>Assignment title</th><th>Subject</th><th>Created on</th><th>Submission date</th><th>Action</th></tr></thead>
        <tbody id="teacherAssignmentRows"><tr><td colspan="5">Loading your assignments…</td></tr></tbody>
      </table></div>
    </section>
    <dialog id="assignmentViewDialog" class="assignment-view-dialog">
      <div class="dialog-heading"><div><p class="eyebrow">ASSIGNMENT DETAILS</p><h2 id="viewAssignmentTitle">Assignment</h2></div><button class="light" id="closeAssignmentView" type="button">Close</button></div>
      <div id="assignmentViewContent"></div>
    </dialog>
  </main>
  <jsp:include page="fragments/footer.jsp"/>
</div></div>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/teacher-dashboard.js"></script>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/assignments/teacher-assignment-list.js" defer></script>
</body></html>
