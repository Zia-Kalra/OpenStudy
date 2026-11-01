<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${errorContent.heading}"/> · OpenStudy</title>
    <link rel="stylesheet" href="${fn:escapeXml(pageContext.request.contextPath)}/css/teacher-dashboard.css">
</head>
<body class="teacher-shell">
<div class="layout">
    <jsp:include page="fragments/sidebar.jsp"/>
    <div class="layout-main">
        <jsp:include page="fragments/navbar.jsp"/>
        <main class="content">
            <section class="panel error-panel" role="alert">
                <p class="eyebrow">Error <c:out value="${errorContent.status}"/> · <c:out value="${errorContent.code}"/></p>
                <h2><c:out value="${errorContent.heading}"/></h2>
                <p><c:out value="${errorContent.explanation}"/></p>
                <c:if test="${errorContent.suggestSignIn}">
                    <p>Teacher login is owned by Member 1 and is not available in this workspace yet.</p>
                </c:if>
                <p class="error-ref">Reference <c:out value="${errorReferenceId}"/></p>
                <div class="error-actions">
                    <a class="primary-link" href="${fn:escapeXml(pageContext.request.contextPath)}${fn:escapeXml(nav.dashboard)}">Back to dashboard</a>
                    <c:if test="${errorContent.retryable && fn:startsWith(errorPath, '/') && !fn:startsWith(errorPath, '//')}">
                        <a class="primary-link error-retry" href="${fn:escapeXml(pageContext.request.contextPath)}${fn:escapeXml(errorPath)}">Try again</a>
                    </c:if>
                </div>
            </section>
        </main>
        <jsp:include page="fragments/footer.jsp"/>
    </div>
</div>
<script src="${fn:escapeXml(pageContext.request.contextPath)}/js/teacher-dashboard.js"></script>
</body>
</html>
