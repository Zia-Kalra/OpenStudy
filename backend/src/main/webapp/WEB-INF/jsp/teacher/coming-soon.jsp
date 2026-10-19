<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${moduleName}"/> · OpenStudy</title>
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/teacher-dashboard.css">
</head>
<body class="teacher-shell">
<div class="layout">
    <jsp:include page="fragments/sidebar.jsp"/>
    <div class="layout-main">
        <jsp:include page="fragments/navbar.jsp"/>
        <main class="content">
            <section class="panel placeholder-panel">
                <p class="eyebrow">Integration placeholder</p>
                <h2><c:out value="${moduleName}"/></h2>
                <p>This route is reserved for <strong><c:out value="${moduleOwner}"/></strong>. Member 5 has not implemented this feature.</p>
                <p>When <c:out value="${moduleOwner}"/> publishes their module, update <code>openstudy.modules</code> so the sidebar points at the real path instead of this page.</p>
                <p class="module-key">Module key: <c:out value="${moduleKey}"/></p>
                <a class="primary-link" href="${fn:escapeXml(pageContext.request.contextPath)}${fn:escapeXml(nav.dashboard)}">Back to dashboard</a>
            </section>
        </main>
        <jsp:include page="fragments/footer.jsp"/>
    </div>
</div>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/teacher-dashboard.js"></script>
</body>
</html>
