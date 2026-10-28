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
    <title>Profile · OpenStudy</title>
    <link rel="stylesheet" href="${base}/css/openstudy.css">
</head>
<body>
<jsp:include page="fragments/topnav.jsp"/>

<main class="os-page">
    <section class="os-hero os-hero--compact">
        <div>
            <h1>My Profile</h1>
            <p class="os-hero__sub">View and manage your personal information</p>
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
            <span class="os-avatar" aria-hidden="true"><c:out value="${currentUserInitial}"/></span>
            <div>
                <h2><c:out value="${currentUserName}"/></h2>
                <p><c:out value="${currentUserEmail}"/></p>
            </div>
            <button type="button" class="os-btn os-btn--ghost" data-action="edit-profile" aria-expanded="false" aria-controls="profileEditForm">
                <os:icon name="edit"/><span data-label>Edit profile</span>
            </button>
        </div>

        <div class="os-fields">
            <div class="os-field"><span>Account ID (system-generated)</span><strong><c:out value="${empty currentUser.userId ? 'Not set' : currentUser.userId}"/></strong></div>
            <div class="os-field"><span>Student / College ID</span><strong class="${empty currentUser.studentId ? 'is-empty' : ''}"><c:out value="${empty currentUser.studentId ? 'Not set' : currentUser.studentId}"/></strong></div>
            <div class="os-field"><span>Course</span><strong class="${empty currentUser.course ? 'is-empty' : ''}"><c:out value="${empty currentUser.course ? 'Not set' : currentUser.course}"/></strong></div>
            <div class="os-field"><span>Semester</span><strong class="${empty currentUser.semester ? 'is-empty' : ''}"><c:out value="${empty currentUser.semester ? 'Not set' : currentUser.semester}"/></strong></div>
            <div class="os-field"><span>Account Type</span><strong>Student</strong></div>
            <div class="os-field"><span>Phone Number</span><strong class="${empty currentUser.phoneNumber ? 'is-empty' : ''}"><c:out value="${empty currentUser.phoneNumber ? 'Not set' : currentUser.phoneNumber}"/></strong></div>
            <div class="os-field"><span>Institution</span><strong class="${empty currentUser.institution ? 'is-empty' : ''}"><c:out value="${empty currentUser.institution ? 'Not set' : currentUser.institution}"/></strong></div>
            <div class="os-field"><span>Department</span><strong class="${empty currentUser.department ? 'is-empty' : ''}"><c:out value="${empty currentUser.department ? 'Not set' : currentUser.department}"/></strong></div>
            <div class="os-field"><span>Year</span><strong class="${empty currentUser.year ? 'is-empty' : ''}"><c:out value="${empty currentUser.year ? 'Not set' : currentUser.year}"/></strong></div>
        </div>

        <form id="profileEditForm" class="os-profile-edit" method="post" action="${base}/student/profile" hidden>
            <h3>Edit your details</h3>
            <div class="os-fields">
                <label class="os-field"><span>Full name</span><input name="fullName" maxlength="150" required value="<c:out value='${currentUser.fullName}'/>"></label>
                <label class="os-field"><span>Email</span><input type="email" name="email" maxlength="254" required value="<c:out value='${currentUser.email}'/>"></label>
                <label class="os-field"><span>Student / College ID</span><input name="studentId" maxlength="50" value="<c:out value='${currentUser.studentId}'/>"></label>
                <label class="os-field"><span>Course</span><input name="course" maxlength="100" value="<c:out value='${currentUser.course}'/>"></label>
                <label class="os-field"><span>Semester</span><input name="semester" maxlength="30" value="<c:out value='${currentUser.semester}'/>"></label>
                <label class="os-field"><span>Phone number</span><input name="phoneNumber" maxlength="40" value="<c:out value='${currentUser.phoneNumber}'/>"></label>
                <label class="os-field"><span>Institution</span><input name="institution" maxlength="150" value="<c:out value='${currentUser.institution}'/>"></label>
                <label class="os-field"><span>Department</span><input name="department" maxlength="100" value="<c:out value='${currentUser.department}'/>"></label>
                <label class="os-field"><span>Year</span><input name="year" maxlength="30" value="<c:out value='${currentUser.year}'/>"></label>
            </div>
            <div class="os-form-actions">
                <button type="submit" class="os-btn os-btn--primary">Save changes</button>
                <button type="button" class="os-btn os-btn--quiet" data-action="cancel-profile">Cancel</button>
            </div>
        </form>
    </section>
</main>
<script src="${base}/js/student-dashboard.js"></script>
</body>
</html>
