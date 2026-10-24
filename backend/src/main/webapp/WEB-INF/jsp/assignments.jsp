<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Assignments · OpenStudy</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assignments/assignments.css">
</head>
<body class="assignment-page">
  <aside class="assignment-rail">
    <a class="brand" href="${pageContext.request.contextPath}/assignments/">OS</a>
    <nav><a href="#student">My assignments</a><a href="${pageContext.request.contextPath}/student/dashboard">Dashboard</a></nav>
    <small>OpenStudy</small>
  </aside>
  <main class="assignment-main"><jsp:include page="fragments/assignment-workspace.jsp" /></main>
  <script src="${pageContext.request.contextPath}/assignments/assignments.js" defer></script>
</body>
</html>
