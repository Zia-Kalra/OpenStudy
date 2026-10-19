<%@ page contentType="text/html;charset=UTF-8" language="java" %> <%@ taglib
prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>OpenStudy | Login</title>
    <link
      rel="stylesheet"
      href="${pageContext.request.contextPath}/css/student-dashboard.css"
    />
  </head>
  <body class="login-shell">
    <div class="login-card">
      <aside class="login-brand">
        <div>
          <div class="logo-lockup">
            <div class="logo-mark">OS</div>
            <h1 class="logo-text">OpenStudy</h1>
          </div>
          <p>
            A smart platform for quizzes and assignments with AI-powered
            evaluation and analytics.
          </p>
        </div>
        <div class="book-illustration" aria-hidden="true"></div>
      </aside>

      <main class="login-form-panel">
        <div class="login-form">
          <h2>Welcome Back</h2>
          <div class="subtitle">Login to your account</div>

          <form method="post" action="${pageContext.request.contextPath}/login">
            <div class="role-picker" aria-label="Select role">
              <button type="button" class="role-btn active" data-role="student">
                Student
              </button>
              <button type="button" class="role-btn" data-role="teacher">
                Teacher
              </button>
            </div>
            <input type="hidden" name="role" id="login-role" value="student" />

            <c:if test="${not empty errorMessage}">
              <div class="form-alert" role="alert">
                <c:out value="${errorMessage}" />
              </div>
            </c:if>

            <label class="input-wrap">
              <span class="field-icon">✉</span>
              <input
                type="email"
                name="email"
                aria-label="Email address"
                placeholder="Email address"
                autocomplete="username"
                required
              />
            </label>

            <label class="input-wrap">
              <span class="field-icon">🔒</span>
              <input
                type="password"
                name="password"
                aria-label="Password"
                placeholder="Password"
                autocomplete="current-password"
                required
              />
            </label>

            <div class="login-actions">
              <span></span>
              <a href="${pageContext.request.contextPath}/forgot-password"
                >Forgot password?</a
              >
            </div>

            <button type="submit" class="login-btn">Login</button>

            <div class="account-link">
              Don't have an account?
              <a id="signup-link" href="${pageContext.request.contextPath}/signup?role=student">Sign up</a>
            </div>
          </form>

          <script>
            document.addEventListener("DOMContentLoaded", function () {
              const roleInput = document.getElementById("login-role");
              const signupLink = document.getElementById("signup-link");
              const buttons = document.querySelectorAll(".role-btn");
              function setRole(role) {
                roleInput.value = role;
                signupLink.href = "${pageContext.request.contextPath}/signup?role=" + encodeURIComponent(role);
              }
              buttons.forEach(function (button) {
                button.addEventListener("click", function () {
                  buttons.forEach(function (btn) {
                    btn.classList.remove("active");
                  });
                  button.classList.add("active");
                  setRole(button.dataset.role);
                });
              });
            });
          </script>
        </div>
      </main>
    </div>
  </body>
</html>
