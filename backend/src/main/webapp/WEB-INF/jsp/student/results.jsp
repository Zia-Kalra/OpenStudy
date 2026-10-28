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
    <title>Results · OpenStudy</title>
    <link rel="stylesheet" href="${base}/css/openstudy.css">
</head>
<body>
<jsp:include page="fragments/topnav.jsp"/>

<main class="os-page">
    <section class="os-hero os-hero--compact">
        <div>
            <h1>My Results</h1>
            <p class="os-hero__sub">View your quiz and assignment results</p>
        </div>
    </section>

    <section class="os-panel os-panel--padded tone-mint" aria-labelledby="resultsTitle">
        <div class="os-panel__head">
            <span class="os-panel__icon"><os:icon name="results"/></span>
            <h2 id="resultsTitle">Graded Work</h2>
        </div>
        <div class="os-table-wrap">
            <table class="os-table">
                <thead>
                <tr>
                    <th scope="col">Title</th>
                    <th scope="col">Type</th>
                    <th scope="col">Marks</th>
                    <th scope="col">Status</th>
                    <th scope="col">Date</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="result" items="${results}">
                    <tr data-os-searchable>
                        <td>
                            <span class="os-table__title ${result.type == 'Quiz' ? 'tone-purple' : 'tone-peach'}">
                                <span class="os-tile" aria-hidden="true"><c:out value="${fn:substring(result.type, 0, 1)}"/></span>
                                <c:out value="${result.title}"/>
                            </span>
                        </td>
                        <td><c:out value="${result.type}"/></td>
                        <td class="os-score"><c:out value="${result.score}"/></td>
                        <td><os:pill status="${result.status}"/></td>
                        <td><c:out value="${result.date}"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
        <c:if test="${empty results}">
            <div class="os-empty"><strong>No results yet</strong>Your marks appear here once work is graded.</div>
        </c:if>
    </section>
</main>
<script src="${base}/js/student-dashboard.js"></script>
</body>
</html>
