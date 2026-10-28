<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="cls" required="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Line icons (Lucide-style, 24px grid). Usage: <os:icon name="home"/>; size follows font-size. --%>
<svg class="icon ${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">
<c:choose>
<c:when test="${name == 'home'}"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><path d="M9 22V12h6v10"/></c:when>
<c:when test="${name == 'quiz'}"><rect width="8" height="4" x="8" y="2" rx="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><path d="M12 11h4"/><path d="M12 16h4"/><path d="M8 11h.01"/><path d="M8 16h.01"/></c:when>
<c:when test="${name == 'assignment'}"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="M10 9H8"/><path d="M16 13H8"/><path d="M16 17H8"/></c:when>
<c:when test="${name == 'results'}"><path d="M18 20V10"/><path d="M12 20V4"/><path d="M6 20v-6"/></c:when>
<c:when test="${name == 'user'}"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></c:when>
<c:when test="${name == 'users'}"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></c:when>
<c:when test="${name == 'logout'}"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="m16 17 5-5-5-5"/><path d="M21 12H9"/></c:when>
<c:when test="${name == 'search'}"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></c:when>
<c:when test="${name == 'chevron-down'}"><path d="m6 9 6 6 6-6"/></c:when>
<c:when test="${name == 'chevron-right'}"><path d="m9 18 6-6-6-6"/></c:when>
<c:when test="${name == 'arrow-right'}"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></c:when>
<c:when test="${name == 'check-circle'}"><circle cx="12" cy="12" r="10"/><path d="m9 12 2 2 4-4"/></c:when>
<c:when test="${name == 'star'}"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01z"/></c:when>
<c:when test="${name == 'menu'}"><path d="M4 6h16"/><path d="M4 12h16"/><path d="M4 18h16"/></c:when>
<c:when test="${name == 'close'}"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></c:when>
<c:when test="${name == 'book'}"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></c:when>
<c:when test="${name == 'inbox'}"><path d="M22 12h-6l-2 3h-4l-2-3H2"/><path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"/></c:when>
<c:when test="${name == 'analytics'}"><path d="M21.21 15.89A10 10 0 1 1 8 2.83"/><path d="M22 12A10 10 0 0 0 12 2v10z"/></c:when>
<c:when test="${name == 'trend'}"><path d="m22 7-8.5 8.5-5-5L2 17"/><path d="M16 7h6v6"/></c:when>
<c:when test="${name == 'calendar'}"><rect width="18" height="18" x="3" y="4" rx="2"/><path d="M16 2v4"/><path d="M8 2v4"/><path d="M3 10h18"/></c:when>
<c:when test="${name == 'clock'}"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></c:when>
<c:when test="${name == 'mail'}"><rect width="20" height="16" x="2" y="4" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></c:when>
<c:when test="${name == 'lock'}"><rect width="18" height="11" x="3" y="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></c:when>
<c:when test="${name == 'edit'}"><path d="M17 3a2.85 2.85 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></c:when>
<c:when test="${name == 'plus'}"><path d="M5 12h14"/><path d="M12 5v14"/></c:when>
<c:when test="${name == 'activity'}"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></c:when>
<c:when test="${name == 'sparkle'}"><path d="M12 3c.5 4.5 2.5 6.5 9 9-6.5 2.5-8.5 4.5-9 9-.5-4.5-2.5-6.5-9-9 6.5-2.5 8.5-4.5 9-9z"/></c:when>
<c:otherwise><circle cx="12" cy="12" r="9"/></c:otherwise>
</c:choose>
</svg>
