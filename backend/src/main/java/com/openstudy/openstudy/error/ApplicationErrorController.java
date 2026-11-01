package com.openstudy.openstudy.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webmvc.autoconfigure.error.AbstractErrorController;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class ApplicationErrorController extends AbstractErrorController {
    private static final Logger log = LoggerFactory.getLogger(ApplicationErrorController.class);
    private final ErrorViewSupport views;

    public ApplicationErrorController(ErrorAttributes errorAttributes, ErrorViewSupport views) {
        super(errorAttributes);
        this.views = views;
    }

    @RequestMapping
    public Object error(HttpServletRequest request, HttpServletResponse response) {
        int status = getStatus(request).value();
        String referenceId = ErrorPages.newReferenceId();
        Throwable cause = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        ErrorPageContent content = cause != null ? ErrorPages.from(cause, status) : ErrorPages.ofStatus(status);
        if (content.status() >= 500) {
            log.error("Container error status={} code={} ref={} path={}", content.status(), content.code(), referenceId,
                    request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        } else {
            log.warn("Container error status={} code={} ref={} path={}", content.status(), content.code(), referenceId,
                    request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        }
        GlobalExceptionHandler.applyHeaders(response);
        if (GlobalExceptionHandler.wantsJson(request) || prefersJson(request)) {
            return ResponseEntity.status(content.status())
                    .header("Cache-Control", "no-store")
                    .header("X-Content-Type-Options", "nosniff")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiErrorResponse.of(content, referenceId));
        }
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        return views.page(content, referenceId, path == null ? "" : path.toString());
    }

    private boolean prefersJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)
                && !accept.contains(MediaType.TEXT_HTML_VALUE);
    }
}
