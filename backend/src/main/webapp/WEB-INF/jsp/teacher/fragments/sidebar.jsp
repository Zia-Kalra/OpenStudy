<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<aside class="teacher-sidebar" id="teacherSidebar" tabindex="-1">
    <button type="button" class="sidebar-close" id="sidebarClose" aria-label="Close navigation">×</button>
    <div class="brand">
        <span class="brand-mark">OS</span>
        <div>
            <strong>OpenStudy</strong>
            <span>Teacher portal</span>
        </div>
    </div>
    <nav class="sidebar-nav" aria-label="Teacher navigation">
        <a class="${activePage == 'dashboard' ? 'active' : ''}" href="${fn:escapeXml(pageContext.request.contextPath)}${fn:escapeXml(nav.dashboard)}">
            <span class="nav-icon">⌂</span> Dashboard
        </a>
        <a class="${activePage == 'question-bank' ? 'active' : ''}" href="${fn:escapeXml(pageContext.request.contextPath)}${fn:escapeXml(nav.questionBank)}">
            <span class="nav-icon">☰</span> Question Bank
            <span class="owner-chip">M2</span>
        </a>
        <a class="${activePage == 'quizzes' ? 'active' : ''}" href="${fn:escapeXml(pageContext.request.contextPath)}${fn:escapeXml(nav.quizzes)}">
            <span class="nav-icon">▣</span> Quizzes
            <span class="owner-chip">M2</span>
        </a>
        <a class="${activePage == 'assignments' ? 'active' : ''}" href="${fn:escapeXml(pageContext.request.contextPath)}${fn:escapeXml(nav.assignments)}">
            <span class="nav-icon">✎</span> Assignments
            <span class="owner-chip">M3</span>
        </a>
        <a class="${activePage == 'submissions' ? 'active' : ''}" href="${fn:escapeXml(pageContext.request.contextPath)}${fn:escapeXml(nav.submissions)}">
            <span class="nav-icon">▤</span> Submissions
            <span class="owner-chip">M3</span>
        </a>
        <a class="${activePage == 'analytics' ? 'active' : ''}" href="${fn:escapeXml(pageContext.request.contextPath)}${fn:escapeXml(nav.analytics)}">
            <span class="nav-icon">◎</span> Teacher Analytics
            <span class="owner-chip">M5</span>
        </a>
        <a class="${activePage == 'profile' ? 'active' : ''}" href="${fn:escapeXml(pageContext.request.contextPath)}${fn:escapeXml(nav.profile)}">
            <span class="nav-icon">☺</span> Profile
            <span class="owner-chip">M1</span>
        </a>
        <a class="${activePage == 'logout' ? 'active' : ''} logout-link" href="${fn:escapeXml(pageContext.request.contextPath)}${fn:escapeXml(nav.logout)}">
            <span class="nav-icon">→</span> Logout
            <span class="owner-chip">M1</span>
        </a>
    </nav>
</aside>
