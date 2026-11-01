<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<aside class="student-sidebar" id="studentSidebar">
    <div class="brand-block">
        <div class="brand-icon">OS</div>
        <div class="brand-meta"><strong>OpenStudy</strong></div>
    </div>
    <nav class="student-side-nav" aria-label="Student navigation">
        <a class="${activePage == 'dashboard' ? 'active' : ''}" href="${pageContext.request.contextPath}/student/dashboard"><span class="side-icon">⌂</span> Dashboard</a>
        <a class="${activePage == 'quizzes' ? 'active' : ''}" href="${pageContext.request.contextPath}/student/quizzes"><span class="side-icon">▣</span> Quizzes</a>
        <a class="${activePage == 'assignments' ? 'active' : ''}" href="${pageContext.request.contextPath}/student/assignments"><span class="side-icon">✎</span> Assignments</a>
        <a class="${activePage == 'results' ? 'active' : ''}" href="${pageContext.request.contextPath}/student/results"><span class="side-icon">▤</span> Results</a>
        <a class="${activePage == 'performance' ? 'active' : ''}" href="${pageContext.request.contextPath}/student/performance"><span class="side-icon">◎</span> Performance</a>
        <a class="${activePage == 'profile' ? 'active' : ''}" href="${pageContext.request.contextPath}/student/profile"><span class="side-icon">☺</span> Profile</a>
        <a href="${pageContext.request.contextPath}/logout"><span class="side-icon">↪</span> Logout</a>
    </nav>
</aside>
