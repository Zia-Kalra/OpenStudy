<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="href" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- OpenStudy wordmark: sparkle mark + name. Renders a link when href is given. --%>
<c:set var="mark">
    <svg class="os-logo__mark" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M12 2c.6 5.4 3.6 8.4 10 10-6.4 1.6-9.4 4.6-10 10-.6-5.4-3.6-8.4-10-10 6.4-1.6 9.4-4.6 10-10z" fill="#6d4fd8"/></svg>
    <span class="os-logo__text">OpenStudy</span>
</c:set>
<c:choose>
    <c:when test="${not empty href}"><a class="os-logo" href="${href}" aria-label="OpenStudy home">${mark}</a></c:when>
    <c:otherwise><span class="os-logo">${mark}</span></c:otherwise>
</c:choose>
