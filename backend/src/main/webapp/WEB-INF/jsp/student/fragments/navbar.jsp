<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<header class="student-topbar">
    <button class="icon-button sidebar-toggle" id="studentSidebarToggle" aria-label="Toggle navigation" data-action="toggle-sidebar" aria-controls="studentSidebar" aria-expanded="false">☰</button>
    <div class="searchbox">
        <span aria-hidden="true">⌕</span>
        <input type="search" id="studentPageSearch" placeholder="Search OpenStudy" aria-label="Search OpenStudy">
    </div>
    <div class="topbar-actions">
        <button class="icon-button" type="button" aria-label="Flag current item" data-action="toggle-flag">⚑</button>
        <div class="user-chip">
            <div class="avatar-circle"><c:out value="${currentUserInitial}"/></div>
            <div class="user-name"><c:out value="${currentUserName}"/></div>
        </div>
    </div>
</header>
