package com.openstudy.openstudy.error;

import com.openstudy.openstudy.service.ai.AiServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Order(0)
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ErrorViewSupport views;

    public GlobalExceptionHandler(ErrorViewSupport views) {
        this.views = views;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Object handleStatus(ResponseStatusException ex, HttpServletRequest request, HttpServletResponse response) {
        return render(ErrorPages.from(ex, ex.getStatusCode().value()), request, response, ex);
    }

    @ExceptionHandler(AiServiceException.class)
    public Object handleAi(AiServiceException ex, HttpServletRequest request, HttpServletResponse response) {
        return render(ErrorPages.fromAi(ex), request, response, ex);
    }

    @ExceptionHandler(DataAccessException.class)
    public Object handleDatabase(DataAccessException ex, HttpServletRequest request, HttpServletResponse response) {
        return render(ErrorPages.from(ex, 503), request, response, ex);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class,
            IllegalArgumentException.class
    })
    public Object handleBadRequest(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        return render(ErrorPages.ofStatus(400), request, response, ex);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest request,
            HttpServletResponse response) {
        return render(ErrorPages.ofStatus(405), request, response, ex);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public Object handleMissing(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        return render(ErrorPages.ofStatus(404), request, response, ex);
    }

    @ExceptionHandler(Exception.class)
    public Object handleUnhandled(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        return render(ErrorPages.ofStatus(500), request, response, ex);
    }

    private Object render(ErrorPageContent content, HttpServletRequest request, HttpServletResponse response,
            Exception ex) {
        String referenceId = ErrorPages.newReferenceId();
        if (content.status() >= 500) {
            log.error("Request failed status={} code={} ref={} path={}", content.status(), content.code(), referenceId,
                    request.getRequestURI());
            log.debug("Technical failure ref={}", referenceId, ex);
        } else {
            log.warn("Request failed status={} code={} ref={} path={}", content.status(), content.code(), referenceId,
                    request.getRequestURI());
        }
        applyHeaders(response);
        if (wantsJson(request)) {
            return ResponseEntity.status(content.status())
                    .header("Cache-Control", "no-store")
                    .header("X-Content-Type-Options", "nosniff")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiErrorResponse.of(content, referenceId));
        }
        return views.page(content, referenceId, request.getRequestURI());
    }

    static void applyHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    static boolean wantsJson(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)
                && !accept.contains(MediaType.TEXT_HTML_VALUE);
    }
}
