<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>OpenStudy | Choose New Password</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/student-dashboard.css">
</head>
<body class="login-shell">
<div class="login-card">
    <aside class="login-brand">
        <div>
            <div class="logo-lockup"><div class="logo-mark">OS</div><h1 class="logo-text">OpenStudy</h1></div>
            <p>Choose a new password for your account.</p>
        </div>
        <div class="book-illustration" aria-hidden="true"></div>
    </aside>
    <main class="login-form-panel">
        <div class="login-form">
            <h2>Choose a new password</h2>
            <c:if test="${not empty errorMessage}">
                <div class="form-alert" role="alert"><c:out value="${errorMessage}" /></div>
            </c:if>
            <c:choose>
                <c:when test="${validResetToken}">
                    <form method="post" action="${pageContext.request.contextPath}/reset-password">
                        <input type="hidden" name="token" value="<c:out value='${resetToken}' />">
                        <label class="input-wrap">
                            <span class="field-icon" aria-hidden="true">🔒</span>
                            <input type="password" name="password" minlength="12" maxlength="72" placeholder="New password" autocomplete="new-password" required>
                        </label>
                        <label class="input-wrap">
                            <span class="field-icon" aria-hidden="true">🔒</span>
                            <input type="password" name="confirmPassword" minlength="12" maxlength="72" placeholder="Confirm new password" autocomplete="new-password" required>
                        </label>
                        <button type="submit" class="login-btn">Update Password</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <p class="profile-error" role="alert">This reset link is invalid or has expired. Request a new one.</p>
                    <div class="account-link"><a href="${pageContext.request.contextPath}/forgot-password">Request another reset link</a></div>
                </c:otherwise>
            </c:choose>
            <div class="account-link"><a href="${pageContext.request.contextPath}/login">Back to login</a></div>
        </div>
    </main>
</div>
</body>
</html>