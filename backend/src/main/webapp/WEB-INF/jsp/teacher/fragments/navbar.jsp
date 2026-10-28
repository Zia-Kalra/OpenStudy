<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="os" tagdir="/WEB-INF/tags" %>
<%-- Teacher top navigation. Needs model attributes "nav" (TeacherNavLinksDTO), "currentTeacher" and "activePage". --%>
<c:set var="base" value="${fn:escapeXml(pageContext.request.contextPath)}"/>
<div class="os-nav-wrap">
    <header class="os-nav os-nav--teacher">
        <os:logo href="${base}${fn:escapeXml(nav.dashboard)}"/>

        <nav class="os-nav__links" id="osNavLinks" aria-label="Teacher navigation">
            <a class="${activePage == 'dashboard' ? 'is-active' : ''}" href="${base}${fn:escapeXml(nav.dashboard)}" ${activePage == 'dashboard' ? 'aria-current="page"' : ''}><os:icon name="home"/>Dashboard</a>
            <a class="${activePage == 'question-bank' ? 'is-active' : ''}" href="${base}${fn:escapeXml(nav.questionBank)}" ${activePage == 'question-bank' ? 'aria-current="page"' : ''}><os:icon name="book"/>Question Bank</a>
            <a class="${activePage == 'quizzes' ? 'is-active' : ''}" href="${base}${fn:escapeXml(nav.quizzes)}" ${activePage == 'quizzes' ? 'aria-current="page"' : ''}><os:icon name="quiz"/>Quizzes</a>
            <a class="${activePage == 'assignments' ? 'is-active' : ''}" href="${base}${fn:escapeXml(nav.assignments)}" ${activePage == 'assignments' ? 'aria-current="page"' : ''}><os:icon name="assignment"/>Assignments</a>
            <a class="${activePage == 'submissions' ? 'is-active' : ''}" href="${base}${fn:escapeXml(nav.submissions)}" ${activePage == 'submissions' ? 'aria-current="page"' : ''}><os:icon name="inbox"/>Submissions</a>
            <a class="${activePage == 'analytics' ? 'is-active' : ''}" href="${base}${fn:escapeXml(nav.analytics)}" ${activePage == 'analytics' ? 'aria-current="page"' : ''}><os:icon name="analytics"/>Analytics</a>
        </nav>

        <c:if test="${currentTeacher.demoIdentity}">
            <span class="os-demo-pill" title="Member 1 authentication is pending">Demo identity</span>
        </c:if>

        <div class="os-user">
            <button type="button" class="os-user__button" data-os-menu-button aria-expanded="false" aria-controls="osUserMenu" aria-haspopup="true">
                <span class="os-avatar" aria-hidden="true"><c:out value="${currentTeacher.initials}"/></span>
                <span class="os-user__name"><c:out value="${currentTeacher.displayName}"/></span>
                <os:icon name="chevron-down"/>
                <span class="sr-only">Account menu</span>
            </button>
            <div class="os-menu" id="osUserMenu" hidden>
                <div class="os-menu__head">
                    <strong><c:out value="${currentTeacher.displayName}"/></strong>
                    <span><c:out value="${currentTeacher.role}"/></span>
                </div>
                <a href="${base}${fn:escapeXml(nav.profile)}">Profile</a>
                <a href="${base}${fn:escapeXml(nav.logout)}">Log out</a>
            </div>
        </div>

        <button type="button" class="os-nav__toggle" data-os-nav-toggle aria-controls="osNavLinks" aria-expanded="false" aria-label="Open navigation">
            <os:icon name="menu" cls="icon-menu"/><os:icon name="close" cls="icon-close"/>
        </button>
    </header>
</div>
<script src="${base}/js/openstudy.js" defer></script>
