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
    <title>Choose a new password · OpenStudy</title>
    <link rel="stylesheet" href="${base}/css/openstudy.css">
</head>
<body>
<div class="os-auth">
    <div class="os-auth__card">
        <jsp:include page="fragments/auth-brand.jsp"/>

        <main class="os-auth__form">
            <h2>Choose a new password</h2>
            <p class="os-auth__sub">Use at least 8 characters.</p>

            <c:if test="${not empty errorMessage}">
                <div class="os-alert os-alert--error" role="alert"><p><c:out value="${errorMessage}"/></p></div>
            </c:if>
            <c:choose>
                <c:when test="${validResetToken}">
                    <form method="post" action="${base}/reset-password">
                        <input type="hidden" name="token" value="<c:out value='${resetToken}'/>">
                        <label class="os-input">
                            <os:icon name="lock"/>
                            <span class="sr-only">New password</span>
                            <input type="password" name="password" minlength="8" maxlength="72" placeholder="New password" autocomplete="new-password" required>
                        </label>
                        <label class="os-input">
                            <os:icon name="lock"/>
                            <span class="sr-only">Confirm new password</span>
                            <input type="password" name="confirmPassword" minlength="8" maxlength="72" placeholder="Confirm new password" autocomplete="new-password" required>
                        </label>
                        <button type="submit" class="os-btn os-btn--primary">Update password</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <div class="os-alert os-alert--error" role="alert"><p>This reset link is invalid or has expired. <a href="${base}/forgot-password">Request a new one</a>.</p></div>
                </c:otherwise>
            </c:choose>

            <p class="os-auth__foot"><a href="${base}/login">Back to login</a></p>
        </main>
    </div>
</div>
</body>
</html>
