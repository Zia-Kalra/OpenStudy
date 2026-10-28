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
    <title>Create account · OpenStudy</title>
    <link rel="stylesheet" href="${base}/css/openstudy.css">
</head>
<body>
<div class="os-auth">
    <div class="os-auth__card">
        <jsp:include page="fragments/auth-brand.jsp"/>

        <main class="os-auth__form">
            <h2>Create <c:out value="${accountRoleLabel}"/> account</h2>
            <p class="os-auth__sub">Your account ID is generated when your account is saved.</p>

            <form method="post" action="${base}/signup">
                <input type="hidden" name="role" value="<c:out value='${accountRole}'/>">

                <c:if test="${not empty errorMessage}">
                    <div class="os-alert os-alert--error" role="alert"><p><c:out value="${errorMessage}"/></p></div>
                </c:if>

                <label class="os-input">
                    <os:icon name="user"/>
                    <span class="sr-only">Full name</span>
                    <input type="text" name="fullName" maxlength="150" placeholder="Full name" autocomplete="name" required>
                </label>
                <label class="os-input">
                    <os:icon name="mail"/>
                    <span class="sr-only">Email address</span>
                    <input type="email" name="email" maxlength="254" placeholder="Email address" autocomplete="email" required>
                </label>
                <label class="os-input">
                    <os:icon name="lock"/>
                    <span class="sr-only">Password</span>
                    <input type="password" name="password" minlength="8" maxlength="72" placeholder="Password (8 or more characters)" autocomplete="new-password" required>
                </label>
                <label class="os-input">
                    <os:icon name="lock"/>
                    <span class="sr-only">Confirm password</span>
                    <input type="password" name="confirmPassword" minlength="8" maxlength="72" placeholder="Confirm password" autocomplete="new-password" required>
                </label>

                <button type="submit" class="os-btn os-btn--primary">Create <c:out value="${accountRoleLabel}"/> account</button>
            </form>

            <p class="os-auth__foot">Already registered? <a href="${base}/login">Log in</a></p>
        </main>
    </div>
</div>
</body>
</html>
