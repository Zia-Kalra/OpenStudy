<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="status" required="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Status pill whose colour follows the status word, so every page labels a status the same way. --%>
<c:choose>
    <c:when test="${status == 'Ongoing' || status == 'In Progress'}"><c:set var="tone" value="purple"/></c:when>
    <c:when test="${status == 'Pending' || status == 'Due Soon'}"><c:set var="tone" value="peach"/></c:when>
    <c:when test="${status == 'Upcoming'}"><c:set var="tone" value="lavender"/></c:when>
    <c:when test="${status == 'Passed' || status == 'Completed' || status == 'Submitted' || status == 'Graded'}"><c:set var="tone" value="mint"/></c:when>
    <c:when test="${status == 'Failed' || status == 'Overdue'}"><c:set var="tone" value="rose"/></c:when>
    <c:otherwise><c:set var="tone" value="neutral"/></c:otherwise>
</c:choose>
<span class="os-pill os-pill--${tone}"><c:out value="${status}"/></span>
