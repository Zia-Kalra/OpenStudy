<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<header class="teacher-navbar">
    <button type="button" class="menu-toggle" id="sidebarToggle" aria-label="Toggle navigation" aria-controls="teacherSidebar" aria-expanded="false">☰</button>
    <div class="navbar-title">
        <h1>OpenStudy</h1>
        <p>Teacher workspace</p>
    </div>
    <div class="navbar-user">
        <c:if test="${currentTeacher.demoIdentity}">
            <span class="demo-pill">Demo identity · Member 1 pending</span>
        </c:if>
        <div class="user-meta">
            <strong><c:out value="${currentTeacher.displayName}"/></strong>
            <span><c:out value="${currentTeacher.role}"/></span>
        </div>
        <div class="avatar" aria-hidden="true"><c:out value="${currentTeacher.initials}"/></div>
    </div>
</header>
