<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="os" tagdir="/WEB-INF/tags" %>
<c:set var="base" value="${fn:escapeXml(pageContext.request.contextPath)}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Profile · OpenStudy</title>
    <link rel="stylesheet" href="${base}/css/openstudy.css">
</head>
<body>
<jsp:include page="fragments/navbar.jsp"/>

<main class="os-page">
    <section class="os-hero os-hero--compact">
        <div>
            <h1>My Profile</h1>
            <p class="os-hero__sub">Your teacher account details</p>
        </div>
    </section>

    <c:if test="${not empty profileMessage}">
        <div class="os-alert os-alert--success" role="status"><os:icon name="check-circle"/><p><c:out value="${profileMessage}"/></p></div>
    </c:if>
    <c:if test="${not empty profileError}">
        <div class="os-alert os-alert--error" role="alert"><p><c:out value="${profileError}"/></p></div>
    </c:if>

    <section class="os-panel os-panel--padded" aria-label="Profile details">
        <div class="os-profile-head">
            <span class="os-avatar" aria-hidden="true"><c:out value="${currentTeacher.initials}"/></span>
            <div>
                <h2><c:out value="${profileUser.fullName}"/></h2>
                <p><c:out value="${profileUser.email}"/></p>
            </div>
            <button type="button" class="os-btn os-btn--ghost" data-action="edit-profile" aria-expanded="false" aria-controls="profileEditForm">
                <os:icon name="edit"/><span data-label>Edit profile</span>
            </button>
        </div>

        <div class="os-fields">
            <div class="os-field"><span>Account ID (system-generated)</span><strong><c:out value="${empty profileUser.userId ? 'Not set' : profileUser.userId}"/></strong></div>
            <div class="os-field"><span>Account type</span><strong>Teacher</strong></div>
            <div class="os-field"><span>Status</span><strong><c:out value="${empty profileUser.accountStatus ? 'ACTIVE' : profileUser.accountStatus}"/></strong></div>
            <div class="os-field"><span>Member since</span><strong class="${empty memberSince ? 'is-empty' : ''}"><c:out value="${empty memberSince ? 'Not available' : memberSince}"/></strong></div>
            <div class="os-field"><span>Subjects</span>
                <strong class="${empty subjects ? 'is-empty' : ''}"><c:choose>
                    <c:when test="${empty subjects}">None yet</c:when>
                    <c:otherwise><c:forEach var="subject" items="${subjects}" varStatus="loop"><c:out value="${subject}"/><c:if test="${!loop.last}">, </c:if></c:forEach></c:otherwise>
                </c:choose></strong>
            </div>
        </div>

        <form id="profileEditForm" class="os-profile-edit" method="post" action="${base}/teacher/profile" hidden>
            <h3>Edit your details</h3>
            <div class="os-fields">
                <label class="os-field"><span>Full name</span><input name="fullName" maxlength="150" required value="<c:out value='${profileUser.fullName}'/>"></label>
                <label class="os-field"><span>Email</span><input type="email" name="email" maxlength="254" required value="<c:out value='${profileUser.email}'/>"></label>
            </div>
            <div class="os-form-actions">
                <button type="submit" class="os-btn os-btn--primary">Save changes</button>
                <button type="button" class="os-btn os-btn--quiet" data-action="cancel-profile">Cancel</button>
            </div>
        </form>
    </section>
</main>
<jsp:include page="fragments/footer.jsp"/>
<script src="${base}/js/student-dashboard.js"></script>
</body>
</html>
