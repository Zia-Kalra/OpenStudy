package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.config.GeminiProperties;
import com.openstudy.openstudy.service.ai.AiErrorCode;
import com.openstudy.openstudy.service.ai.AiServiceException;
import com.openstudy.openstudy.service.ai.GeminiAIService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Opt-in local diagnostics only. No arbitrary prompt, generated content, credentials, or DB access. */
@RestController
@Profile("gemini-local & !prod & !production")
@ConditionalOnProperty(name = "openstudy.ai.gemini.diagnostics-enabled", havingValue = "true")
@RequestMapping("/api/dev/gemini")
public class GeminiDiagnosticsController {
    private final ObjectProvider<GeminiAIService> serviceProvider;
    private final GeminiProperties properties;
    private final AtomicLong lastProbe = new AtomicLong();
    private static final long COOLDOWN_NANOS = 10_000_000_000L;

    public GeminiDiagnosticsController(ObjectProvider<GeminiAIService> serviceProvider, GeminiProperties properties) {
        this.serviceProvider = serviceProvider;
        this.properties = properties;
    }

    @GetMapping("/status")
    public ResponseEntity<DiagnosticResponse> status(HttpServletRequest request) {
        if (!localRequest(request)) { return forbidden(); }
        if (!properties.hasApiKey()) { return missingKey(); }
        if (serviceProvider.getIfAvailable() == null) { return inactive(); }
        return response(HttpStatus.OK, true, "CONFIGURED_NOT_VERIFIED",
                "Local Gemini configuration is ready. POST /api/dev/gemini/test to verify with Google.");
    }

    @PostMapping(value = "/test", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DiagnosticResponse> test(HttpServletRequest request) {
        if (!localRequest(request)) { return forbidden(); }
        if (!properties.hasApiKey()) { return missingKey(); }
        var service = serviceProvider.getIfAvailable();
        if (service == null) { return inactive(); }
        long now = System.nanoTime();
        long previous = lastProbe.get();
        if ((previous != 0 && now - previous < COOLDOWN_NANOS) || !lastProbe.compareAndSet(previous, now)) {
            return response(HttpStatus.TOO_MANY_REQUESTS, false, "LOCAL_RATE_LIMITED", "Wait 10 seconds between local verification requests.");
        }
        try {
            service.verifyConnection();
            return response(HttpStatus.OK, true, "VERIFIED", "Gemini API verification succeeded.");
        } catch (AiServiceException ex) {
            // Use only known messages; never serialize the exception, upstream body, or generated text.
            return switch (ex.getErrorCode()) {
                case MISSING_API_KEY -> missingKey();
                case INVALID_API_KEY -> response(HttpStatus.BAD_GATEWAY, false, AiErrorCode.INVALID_API_KEY.name(),
                        "Google rejected the configured Gemini API key. Check the key and its API restrictions locally.");
                case RATE_LIMITED -> response(HttpStatus.TOO_MANY_REQUESTS, false, AiErrorCode.RATE_LIMITED.name(),
                        "Gemini quota or rate limit reached. Check your Google project quota.");
                case TIMEOUT -> response(HttpStatus.GATEWAY_TIMEOUT, false, AiErrorCode.TIMEOUT.name(), "Gemini request timed out.");
                default -> response(HttpStatus.BAD_GATEWAY, false, ex.getErrorCode().name(), "Gemini verification failed. Check connectivity, model availability, or try again later.");
            };
        } catch (RuntimeException ex) {
            return response(HttpStatus.INTERNAL_SERVER_ERROR, false, "VERIFICATION_FAILED", "Gemini verification could not be completed.");
        }
    }

    private boolean localRequest(HttpServletRequest request) {
        // Ignore X-Forwarded-For and reject browser-originated requests, including cross-site POSTs.
        if (request.getHeader("Origin") != null || request.getHeader("Sec-Fetch-Site") != null) { return false; }
        String address = request.getRemoteAddr();
        if (!"127.0.0.1".equals(address) && !"::1".equals(address) && !"0:0:0:0:0:0:0:1".equals(address)) { return false; }
        try { return InetAddress.getByName(address).isLoopbackAddress(); } catch (Exception ex) { return false; }
    }

    private ResponseEntity<DiagnosticResponse> forbidden() {
        return response(HttpStatus.FORBIDDEN, false, "LOCAL_ONLY", "Gemini diagnostics accept only local command-line requests.");
    }
    private ResponseEntity<DiagnosticResponse> missingKey() {
        return response(HttpStatus.SERVICE_UNAVAILABLE, false, AiErrorCode.MISSING_API_KEY.name(),
                "GEMINI_API_KEY is missing or still a placeholder. Store it in backend/.env and restart with the local launcher.");
    }
    private ResponseEntity<DiagnosticResponse> inactive() {
        return response(HttpStatus.SERVICE_UNAVAILABLE, false, "PROVIDER_NOT_ENABLED", "The live Gemini provider is not enabled.");
    }
    private ResponseEntity<DiagnosticResponse> response(HttpStatus status, boolean success, String code, String message) {
        return ResponseEntity.status(status).header("Cache-Control", "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(new DiagnosticResponse(success, code, message, properties.getModel()));
    }
    public record DiagnosticResponse(boolean success, String code, String message, String model) { }
}
