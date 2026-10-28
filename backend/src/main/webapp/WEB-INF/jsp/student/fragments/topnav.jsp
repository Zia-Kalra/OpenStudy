<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="os" tagdir="/WEB-INF/tags" %>
<%-- Student top navigation. Pages set the request attribute "activePage" to highlight a link. --%>
<c:set var="base" value="${fn:escapeXml(pageContext.request.contextPath)}"/>
<%-- Some pages (e.g. analytics) don't add the user to the model, so fall back to the session user. --%>
<c:set var="navName" value="${not empty currentUserName ? currentUserName
        : (not empty sessionScope.loggedInUser.fullName ? sessionScope.loggedInUser.fullName : 'Student')}"/>
<c:set var="navEmail" value="${not empty currentUserEmail ? currentUserEmail : sessionScope.loggedInUser.email}"/>
<div class="os-nav-wrap">
    <header class="os-nav">
        <os:logo href="${base}/student/dashboard"/>

        <nav class="os-nav__links" id="osNavLinks" aria-label="Student navigation">
            <a class="${activePage == 'dashboard' ? 'is-active' : ''}" href="${base}/student/dashboard" ${activePage == 'dashboard' ? 'aria-current="page"' : ''}><os:icon name="home"/>Dashboard</a>
            <a class="${activePage == 'quizzes' ? 'is-active' : ''}" href="${base}/student/quizzes" ${activePage == 'quizzes' ? 'aria-current="page"' : ''}><os:icon name="quiz"/>Quizzes</a>
            <a class="${activePage == 'assignments' ? 'is-active' : ''}" href="${base}/student/assignments" ${activePage == 'assignments' ? 'aria-current="page"' : ''}><os:icon name="assignment"/>Assignments</a>
            <a class="${activePage == 'results' ? 'is-active' : ''}" href="${base}/student/results" ${activePage == 'results' ? 'aria-current="page"' : ''}><os:icon name="results"/>Results</a>
            <a class="${activePage == 'performance' ? 'is-active' : ''}" href="${base}/student/performance" ${activePage == 'performance' ? 'aria-current="page"' : ''}><os:icon name="user"/>Performance</a>
            <a href="${base}/logout"><os:icon name="logout"/>Logout</a>
        </nav>

        <form class="os-search" role="search" action="${base}/student/dashboard" method="get" data-os-search>
            <os:icon name="search"/>
            <label class="sr-only" for="osSearch">Search quizzes and assignments</label>
            <input id="osSearch" type="search" name="q" placeholder="Search quizzes, assignments..." autocomplete="off">
        </form>

        <div class="os-user">
            <button type="button" class="os-user__button" data-os-menu-button aria-expanded="false" aria-controls="osUserMenu" aria-haspopup="true">
                <span class="os-avatar" aria-hidden="true"><c:out value="${fn:toUpperCase(fn:substring(navName, 0, 1))}"/></span>
                <span class="os-user__name"><c:out value="${navName}"/></span>
                <os:icon name="chevron-down"/>
                <span class="sr-only">Account menu</span>
            </button>
            <div class="os-menu" id="osUserMenu" hidden>
                <div class="os-menu__head">
                    <strong><c:out value="${navName}"/></strong>
                    <c:if test="${not empty navEmail}"><span><c:out value="${navEmail}"/></span></c:if>
                </div>
                <a href="${base}/student/profile">My profile</a>
                <a href="${base}/student/analytics">Detailed analytics</a>
                <a href="${base}/logout">Log out</a>
            </div>
        </div>

        <button type="button" class="os-nav__toggle" data-os-nav-toggle aria-controls="osNavLinks" aria-expanded="false" aria-label="Open navigation">
            <os:icon name="menu" cls="icon-menu"/><os:icon name="close" cls="icon-close"/>
        </button>
    </header>
</div>
<script src="${base}/js/openstudy.js" defer></script>
