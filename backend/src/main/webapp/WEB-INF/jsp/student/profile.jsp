<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Profile</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/student-dashboard.css">
</head>
<body>
<div class="student-wrapper">
    <aside class="student-sidebar" id="studentSidebar">
        <div class="brand-block">
            <div class="brand-icon">OS</div>
            <div class="brand-meta">
                <strong>OpenStudy</strong>
            </div>
        </div>
        <nav class="student-side-nav" aria-label="Student navigation">
            <a href="${pageContext.request.contextPath}/student/dashboard"><span class="side-icon">⌂</span> Dashboard</a>
            <a href="${pageContext.request.contextPath}/student/quizzes"><span class="side-icon">▣</span> Quizzes</a>
            <a href="${pageContext.request.contextPath}/student/assignments"><span class="side-icon">✎</span> Assignments</a>
            <a href="${pageContext.request.contextPath}/student/results"><span class="side-icon">▤</span> Results</a>
            <a href="${pageContext.request.contextPath}/student/performance"><span class="side-icon">◎</span> Performance</a>
            <a class="active" href="${pageContext.request.contextPath}/student/profile"><span class="side-icon">☺</span> Profile</a>
            <a href="${pageContext.request.contextPath}/logout"><span class="side-icon">↩</span> Logout</a>
        </nav>
    </aside>

    <main class="student-main">
        <header class="student-topbar">
            <button class="icon-button sidebar-toggle" id="studentSidebarToggle" aria-label="Toggle navigation" data-action="toggle-sidebar">☰</button>
            <div class="searchbox">
                <span>⌕</span>
                <input type="text" value="Search..." aria-label="Search" />
            </div>
            <div class="topbar-actions">
                <button class="icon-button" aria-label="Flag item" data-action="toggle-flag">⚑</button>
                <div class="user-chip">
                    <div class="avatar-circle">${currentUserInitial}</div>
                    <div class="user-name">${currentUserName}</div>
                </div>
            </div>
        </header>

        <div class="content-shell">
            <section class="page-header">
                <h1>My Profile</h1>
                <p>View and manage your personal information</p>
            </section>

            <div class="profile-card">
                <div class="profile-head">
                    <div class="profile-avatar">${currentUserInitial}</div>
                    <div>
                        <div class="profile-name"><c:out value="${currentUserName}" /></div>
                        <span class="profile-email"><c:out value="${currentUserEmail}" /></span>
                    </div>
                    <div class="profile-actions">
                        <button type="button" class="profile-button" data-action="edit-profile" aria-expanded="false" aria-controls="profileEditForm">Edit Profile</button>
                    </div>
                </div>

                <c:if test="${not empty profileMessage}">
                    <p class="profile-message" role="status"><c:out value="${profileMessage}" /></p>
                </c:if>
                <c:if test="${not empty profileError}">
                    <p class="profile-error" role="alert"><c:out value="${profileError}" /></p>
                </c:if>

                <div class="profile-grid">
                    <div class="field"><span>Account ID (system-generated)</span><strong><c:out value="${currentUser.userId}" /></strong></div>
                    <div class="field"><span>Student / College ID</span><strong><c:out value="${empty currentUser.studentId ? 'Not set' : currentUser.studentId}" /></strong></div>
                    <div class="field"><span>Course</span><strong><c:out value="${empty currentUser.course ? 'Not set' : currentUser.course}" /></strong></div>
                    <div class="field"><span>Semester</span><strong><c:out value="${empty currentUser.semester ? 'Not set' : currentUser.semester}" /></strong></div>
                    <div class="field"><span>Account Type</span><strong>Student</strong></div>
                    <div class="field"><span>Phone Number</span><strong><c:out value="${empty currentUser.phoneNumber ? 'Not set' : currentUser.phoneNumber}" /></strong></div>
                    <div class="field"><span>Institution</span><strong><c:out value="${empty currentUser.institution ? 'Not set' : currentUser.institution}" /></strong></div>
                    <div class="field"><span>Department</span><strong><c:out value="${empty currentUser.department ? 'Not set' : currentUser.department}" /></strong></div>
                    <div class="field"><span>Year</span><strong><c:out value="${empty currentUser.year ? 'Not set' : currentUser.year}" /></strong></div>
                </div>

                <form id="profileEditForm" class="profile-edit-form" method="post" action="${pageContext.request.contextPath}/student/profile" hidden>
                    <div class="profile-grid">
                        <label class="field"><span>Full name</span><input name="fullName" maxlength="150" required value="<c:out value='${currentUser.fullName}' />"></label>
                        <label class="field"><span>Email</span><input type="email" name="email" maxlength="254" required value="<c:out value='${currentUser.email}' />"></label>
                        <label class="field"><span>Student / College ID</span><input name="studentId" maxlength="50" value="<c:out value='${currentUser.studentId}' />"></label>
                        <label class="field"><span>Course</span><input name="course" maxlength="100" value="<c:out value='${currentUser.course}' />"></label>
                        <label class="field"><span>Semester</span><input name="semester" maxlength="30" value="<c:out value='${currentUser.semester}' />"></label>
                        <label class="field"><span>Phone number</span><input name="phoneNumber" maxlength="40" value="<c:out value='${currentUser.phoneNumber}' />"></label>
                        <label class="field"><span>Institution</span><input name="institution" maxlength="150" value="<c:out value='${currentUser.institution}' />"></label>
                        <label class="field"><span>Department</span><input name="department" maxlength="100" value="<c:out value='${currentUser.department}' />"></label>
                        <label class="field"><span>Year</span><input name="year" maxlength="30" value="<c:out value='${currentUser.year}' />"></label>
                    </div>
                    <div class="profile-form-actions">
                        <button type="submit" class="profile-button">Save Changes</button>
                        <button type="button" class="profile-cancel" data-action="cancel-profile">Cancel</button>
                    </div>
                </form>
            </div>
        </div>
    </main>
</div>
<script src="${pageContext.request.contextPath}/js/student-dashboard.js"></script>
</body>
</html>
