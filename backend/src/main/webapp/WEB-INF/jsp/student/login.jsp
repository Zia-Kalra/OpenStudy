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
    <title>Log in · OpenStudy</title>
    <link rel="stylesheet" href="${base}/css/openstudy.css">
</head>
<body>
<div class="os-auth">
    <div class="os-auth__card">
        <jsp:include page="fragments/auth-brand.jsp"/>

        <main class="os-auth__form">
            <h2>Welcome back</h2>
            <p class="os-auth__sub">Log in to your account</p>

            <form method="post" action="${base}/login">
                <div class="os-segmented" role="group" aria-label="I am a">
                    <button type="button" class="is-active" data-role="student" aria-pressed="true">Student</button>
                    <button type="button" data-role="teacher" aria-pressed="false">Teacher</button>
                </div>
                <input type="hidden" name="role" id="loginRole" value="student">

                <c:if test="${not empty errorMessage}">
                    <div class="os-alert os-alert--error" role="alert"><p><c:out value="${errorMessage}"/></p></div>
                </c:if>

                <label class="os-input">
                    <os:icon name="mail"/>
                    <span class="sr-only">Email address</span>
                    <input type="email" name="email" placeholder="Email address" autocomplete="username" required>
                </label>
                <label class="os-input">
                    <os:icon name="lock"/>
                    <span class="sr-only">Password</span>
                    <input type="password" name="password" placeholder="Password" autocomplete="current-password" required>
                </label>

                <div class="os-auth__row"><a href="${base}/forgot-password">Forgot password?</a></div>

                <button type="submit" class="os-btn os-btn--primary">Log in</button>
            </form>

            <p class="os-auth__foot">Don't have an account? <a id="signupLink" href="${base}/signup?role=student">Sign up</a></p>
        </main>
    </div>
</div>
<script>
    document.addEventListener("DOMContentLoaded", function () {
        var roleInput = document.getElementById("loginRole");
        var signupLink = document.getElementById("signupLink");
        var buttons = document.querySelectorAll("[data-role]");
        buttons.forEach(function (button) {
            button.addEventListener("click", function () {
                buttons.forEach(function (other) {
                    other.classList.toggle("is-active", other === button);
                    other.setAttribute("aria-pressed", String(other === button));
                });
                roleInput.value = button.dataset.role;
                signupLink.href = "${base}/signup?role=" + encodeURIComponent(button.dataset.role);
            });
        });
    });
</script>
</body>
</html>
