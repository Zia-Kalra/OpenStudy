<%@ page contentType="text/html;charset=UTF-8" language="java" %> <%@ taglib
prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>OpenStudy | Create Account</title>
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
          <p>Create your <c:out value="${accountRoleLabel}" /> account and get started with OpenStudy.</p>
        </div>
        <div class="book-illustration" aria-hidden="true"></div>
      </aside>
      <main class="login-form-panel">
        <div class="login-form">
          <h2>Create <c:out value="${accountRoleLabel}" /> account</h2>
          <div class="subtitle">
            Your account ID is generated when your account is saved.
          </div>
          <c:if test="${not empty errorMessage}">
            <div class="form-alert" role="alert">
              <c:out value="${errorMessage}" />
            </div>
          </c:if>
          <form
            method="post"
            action="${pageContext.request.contextPath}/signup"
          >
            <input type="hidden" name="role" value="<c:out value='${accountRole}' />" />
            <label class="input-wrap">
              <span class="field-icon" aria-hidden="true">●</span>
              <input
                type="text"
                name="fullName"
                maxlength="150"
                placeholder="Full name"
                autocomplete="name"
                required
              />
            </label>
            <label class="input-wrap">
              <span class="field-icon" aria-hidden="true">✉</span>
              <input
                type="email"
                name="email"
                maxlength="254"
                placeholder="Email address"
                autocomplete="email"
                required
              />
            </label>
            <label class="input-wrap">
              <span class="field-icon" aria-hidden="true">🔒</span>
              <input
                type="password"
                name="password"
                minlength="8"
                maxlength="72"
                placeholder="Password (8 or more characters)"
                autocomplete="new-password"
                required
              />
            </label>
            <label class="input-wrap">
              <span class="field-icon" aria-hidden="true">🔒</span>
              <input
                type="password"
                name="confirmPassword"
                minlength="8"
                maxlength="72"
                placeholder="Confirm password"
                autocomplete="new-password"
                required
              />
            </label>
            <button type="submit" class="login-btn">
              Create <c:out value="${accountRoleLabel}" /> Account
            </button>
          </form>
          <div class="account-link">
            Already registered?
            <a href="${pageContext.request.contextPath}/login">Log in</a>
          </div>
        </div>
      </main>
    </div>
  </body>
</html>
