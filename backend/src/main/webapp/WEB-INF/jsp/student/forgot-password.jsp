<%@ page contentType="text/html;charset=UTF-8" language="java" %> <%@ taglib
prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>OpenStudy | Reset Password</title>
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
          <p>Get back into your learning workspace.</p>
        </div>
        <div class="book-illustration" aria-hidden="true"></div>
      </aside>
      <main class="login-form-panel">
        <div class="login-form">
          <h2>Forgot password?</h2>
          <div class="subtitle">
            Enter your account email to request a secure reset link.
          </div>
          <c:if test="${not empty statusMessage}">
            <div class="profile-message" role="status">
              <c:out value="${statusMessage}" />
            </div>
          </c:if>
          <c:if test="${not empty errorMessage}">
            <div class="form-alert" role="alert">
              <c:out value="${errorMessage}" />
            </div>
          </c:if>
          <form
            method="post"
            action="${pageContext.request.contextPath}/forgot-password"
          >
            <label class="input-wrap">
              <span class="field-icon" aria-hidden="true">✉</span>
              <input
                type="email"
                name="email"
                placeholder="Email address"
                autocomplete="email"
                required
              />
            </label>
            <button type="submit" class="login-btn">Send Reset Link</button>
          </form>
          <div class="account-link">
            <a href="${pageContext.request.contextPath}/login">Back to login</a>
          </div>
        </div>
      </main>
    </div>
  </body>
</html>
