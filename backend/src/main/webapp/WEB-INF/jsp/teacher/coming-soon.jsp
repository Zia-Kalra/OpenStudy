<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="os" tagdir="/WEB-INF/tags" %>
<c:set var="base" value="${fn:escapeXml(pageContext.request.contextPath)}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:out value="${moduleName}"/> · OpenStudy</title>
    <link rel="stylesheet" href="${base}/css/openstudy.css">
</head>
<body>
<jsp:include page="fragments/navbar.jsp"/>

<main class="os-page">
    <section class="os-hero os-hero--compact">
        <div>
            <p class="os-hero__eyebrow">Coming soon</p>
            <h1><c:out value="${moduleName}"/></h1>
            <p class="os-hero__sub">This module is still being built.</p>
        </div>
    </section>

    <section class="os-panel os-panel--padded">
        <div class="os-panel__head">
            <h2>Integration placeholder</h2>
        </div>
        <p>This route is reserved for <strong><c:out value="${moduleOwner}"/></strong>. Member 5 has not implemented this feature.</p>
        <p style="margin-top: 8px">When <c:out value="${moduleOwner}"/> publishes their module, update <code>openstudy.modules</code> so the navigation points at the real path instead of this page.</p>
        <p style="margin: 14px 0 20px"><span class="os-chip">Module key: <c:out value="${moduleKey}"/></span></p>
        <a class="os-btn os-btn--primary" href="${base}${fn:escapeXml(nav.dashboard)}"><os:icon name="home"/>Back to dashboard</a>
    </section>
</main>
<jsp:include page="fragments/footer.jsp"/>
</body>
</html>
