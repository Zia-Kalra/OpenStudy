<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<section aria-labelledby="rHeading">
    <div class="ta-section-heading"><div><p class="ta-kicker">GO BEYOND AVERAGES</p><h2 id="rHeading">Advanced R Analytics</h2></div><span class="ta-label">R engine not connected</span></div>
    <p class="ta-r-intro"><c:choose><c:when test="${analytics.demo}">Explore illustrative saved outputs for statistical comparisons and models. These examples are not calculated from the current dashboard results.</c:when><c:otherwise>Descriptive statistics use actual graded results in your selected subject scope. R statistical tests and saved R reports are unavailable because no R engine or report store is connected.</c:otherwise></c:choose></p>
    <c:if test="${analytics.demo}">
    <div class="ta-method-grid">
        <article><span>01 / DESCRIBE</span><h3>Descriptive statistics</h3><p>Mean, median, spread, quartiles, and distribution charts.</p></article>
        <article><span>02 / COMPARE PAIRS</span><h3>t-tests</h3><p>Paired or independent comparisons with assumptions and effect sizes.</p></article>
        <article><span>03 / COMPARE GROUPS</span><h3>ANOVA</h3><p>Compare assigned classes or subjects, with post-hoc interpretation.</p></article>
        <article><span>04 / MODEL</span><h3>Regression</h3><p>Explore relationships and progress with diagnostics and fitted charts.</p></article>
    </div>
    </c:if>
</section>
<c:if test="${not analytics.demo}">
    <section class="ta-panel" aria-labelledby="liveStatisticsHeading">
        <div class="ta-panel-heading"><div><p class="ta-kicker">RECORDED GRADES</p><h3 id="liveStatisticsHeading">Live descriptive summary</h3><p>Calculated by the application from normalized final grades; this is not an R output or a saved report.</p></div></div>
        <c:choose><c:when test="${analytics.statistics.count > 0}">
            <div class="ta-report-metrics">
                <div><span>Graded observations</span><strong><c:out value="${analytics.statistics.count}"/></strong></div>
                <div><span>Mean</span><strong><c:out value="${analytics.statistics.getMeanDisplay()}"/></strong></div>
                <div><span>Median</span><strong><c:out value="${analytics.statistics.getMedianDisplay()}"/></strong></div>
                <div><span>Sample standard deviation</span><strong><c:out value="${analytics.statistics.getDeviationDisplay()}"/></strong></div>
            </div>
            <p>Lower / upper quartile: <c:out value="${analytics.statistics.getQuartileDisplay()}"/>. Standard deviation requires at least two grades. Missing and ungraded work are excluded.</p>
        </c:when><c:otherwise><div class="ta-empty"><h3>No graded data for this scope</h3><p>Statistics will appear when enrolled students have final grades for published assessments. No sample values are substituted.</p></div></c:otherwise></c:choose>
    </section>
</c:if>
<c:if test="${analytics.demo or not empty analytics.reports}">
<section class="ta-panel" aria-labelledby="reportsHeading">
    <div class="ta-panel-heading"><div><h3 id="reportsHeading">Saved reports</h3><p>Only reports fully contained within the selected authorized scope are shown.</p></div><c:if test="${analytics.demo}"><span class="ta-demo">ILLUSTRATIVE OUTPUTS</span></c:if></div>
    <c:url value="/teacher/analytics/reports" var="reportAction"/>
    <form class="ta-report-filter" method="get" action="${fn:escapeXml(reportAction)}">
        <input type="hidden" name="classId" value="${fn:escapeXml(analytics.query.classId)}"><input type="hidden" name="subjectId" value="${fn:escapeXml(analytics.query.subjectId)}">
        <label>Analysis method<select name="method"><option value="ALL">All methods</option><option value="DESCRIPTIVE" ${analytics.query.method == 'DESCRIPTIVE' ? 'selected' : ''}>Descriptive statistics</option><option value="T_TEST" ${analytics.query.method == 'T_TEST' ? 'selected' : ''}>t-test</option><option value="ANOVA" ${analytics.query.method == 'ANOVA' ? 'selected' : ''}>ANOVA</option><option value="REGRESSION" ${analytics.query.method == 'REGRESSION' ? 'selected' : ''}>Regression</option></select></label><button type="submit" class="ta-button">Find reports</button>
    </form>
    <div class="ta-report-list"><c:forEach var="report" items="${analytics.reports}">
        <c:url value="/teacher/analytics/reports/${report.id}" var="reportUrl"><c:param name="classId" value="${analytics.query.classId}"/><c:param name="subjectId" value="${analytics.query.subjectId}"/><c:param name="method" value="${analytics.query.method}"/></c:url>
        <a class="ta-report-row ${analytics.selectedReport.id == report.id ? 'is-selected' : ''}" href="${fn:escapeXml(reportUrl)}"><span class="ta-report-icon" aria-hidden="true">R</span><span><strong><c:out value="${report.title}"/></strong><small><c:out value="${report.methodLabel}"/> · <c:out value="${report.createdAt}"/></small></span><span class="ta-report-open">View sample →</span></a>
    </c:forEach></div>
    <c:if test="${empty analytics.reports}"><div class="ta-empty"><h3>No saved reports match this scope</h3><p><c:choose><c:when test="${analytics.demo}">Try all assigned subjects or a broader class selection. Cross-class reports require access to every included class.</c:when><c:otherwise>No R-report table or R engine is available in this integration. t-tests, ANOVA, and regression outputs remain empty.</c:otherwise></c:choose></p></div></c:if>
</section>
</c:if>
<c:if test="${not empty analytics.selectedReport}"><c:set var="report" value="${analytics.selectedReport}"/>
    <section class="ta-panel ta-report-preview" aria-labelledby="reportHeading"><div class="ta-panel-heading"><div><p class="ta-kicker">SAVED REPORT PREVIEW</p><h3 id="reportHeading"><c:out value="${report.title}"/></h3><p><c:out value="${report.methodLabel}"/> · Saved <c:out value="${report.createdAt}"/></p></div><span class="ta-label">Sample output</span></div>
        <div class="ta-report-metrics"><c:forEach var="metric" items="${report.metrics}"><div><span><c:out value="${metric.label}"/></span><strong><c:out value="${metric.value}"/></strong></div></c:forEach></div>
        <div class="ta-detail-columns"><div><h4>Illustrative chart</h4><c:forEach var="bar" items="${report.chart}"><div class="ta-student-subject"><div class="ta-bar-label"><span><c:out value="${bar.label}"/></span><strong><c:out value="${bar.percent}"/>%</strong></div><div class="ta-track"><span style="width:${bar.percent}%"></span></div></div></c:forEach></div><div class="ta-report-note"><h4>Interpretation</h4><p><c:out value="${report.summary}"/></p><p>Review sample selection, missing data, and model assumptions before acting on any future statistical output.</p></div></div>
    </section>
</c:if>
