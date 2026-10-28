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
    <title>Forgot password · OpenStudy</title>
    <link rel="stylesheet" href="${base}/css/openstudy.css">
</head>
<body>
<div class="os-auth">
    <div class="os-auth__card">
        <jsp:include page="fragments/auth-brand.jsp"/>

        <main class="os-auth__form">
            <h2>Forgot password?</h2>
            <p class="os-auth__sub">Enter your account email to request a secure reset link.</p>

            <form method="post" action="${base}/forgot-password">
                <c:if test="${not empty statusMessage}">
                    <div class="os-alert os-alert--success" role="status"><os:icon name="check-circle"/><p><c:out value="${statusMessage}"/></p></div>
                </c:if>
                <c:if test="${not empty errorMessage}">
                    <div class="os-alert os-alert--error" role="alert"><p><c:out value="${errorMessage}"/></p></div>
                </c:if>

                <label class="os-input">
                    <os:icon name="mail"/>
                    <span class="sr-only">Email address</span>
                    <input type="email" name="email" placeholder="Email address" autocomplete="email" required>
                </label>

                <button type="submit" class="os-btn os-btn--primary">Send reset link</button>
            </form>

            <p class="os-auth__foot"><a href="${base}/login">Back to login</a></p>
        </main>
    </div>
</div>
</body>
</html>
