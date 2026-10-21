package com.openstudy.openstudy.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.openstudy.openstudy.config.GeminiProperties;
import com.openstudy.openstudy.service.ai.AiErrorCode;
import com.openstudy.openstudy.service.ai.AiServiceException;
import com.openstudy.openstudy.service.ai.GeminiAIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GeminiDiagnosticsControllerTest {
    private GeminiProperties properties;
    private GeminiAIService service;
    private MockMvc mvc;
    private static final String SECRET = "SYNTHETIC_PRIVATE_KEY_MARKER";

    @BeforeEach
    void setup() {
        properties = new GeminiProperties();
        properties.setApiKey(SECRET);
        service = mock(GeminiAIService.class);
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("geminiAIService", service);
        mvc = MockMvcBuilders.standaloneSetup(new GeminiDiagnosticsController(beans.getBeanProvider(GeminiAIService.class), properties)).build();
    }

    @Test
    void statusChecksConfigurationWithoutCallingGeminiOrReturningTheKey() throws Exception {
        mvc.perform(get("/api/dev/gemini/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CONFIGURED_NOT_VERIFIED"))
                .andExpect(jsonPath("$.model").value("gemini-3.8-flash"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(SECRET))))
                .andExpect(header().string("Cache-Control", "no-store"));
        verifyNoInteractions(service);
    }

    @Test
    void postReturnsOnlySafeVerificationStatusAndThrottlesRepeatedProbes() throws Exception {
        mvc.perform(post("/api/dev/gemini/test").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("VERIFIED"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(SECRET))));
        mvc.perform(post("/api/dev/gemini/test").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("LOCAL_RATE_LIMITED"));
        verify(service, times(1)).verifyConnection();
    }

    @Test
    void missingOrPlaceholderKeyPreventsCalls() throws Exception {
        properties.setApiKey("YOUR_API_KEY_HERE");
        mvc.perform(post("/api/dev/gemini/test").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("MISSING_API_KEY"));
        verifyNoInteractions(service);
    }

    @Test
    void upstreamMessagesCausesAndKeysAreNeverReturned() throws Exception {
        doThrow(new AiServiceException(AiErrorCode.INVALID_API_KEY, SECRET, new RuntimeException(SECRET))).when(service).verifyConnection();
        mvc.perform(post("/api/dev/gemini/test").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("INVALID_API_KEY"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(SECRET))));
    }

    @Test
    void rateLimitAndTimeoutHaveUsefulStatusCodes() throws Exception {
        doThrow(new AiServiceException(AiErrorCode.TIMEOUT, SECRET)).when(service).verifyConnection();
        mvc.perform(post("/api/dev/gemini/test").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isGatewayTimeout()).andExpect(jsonPath("$.code").value("TIMEOUT"));
    }

    @Test
    void deniesBrowserOriginsRemoteClientsAndSpoofedForwardedHeaders() throws Exception {
        mvc.perform(post("/api/dev/gemini/test").contentType(MediaType.APPLICATION_JSON).content("{}").header("Origin", "https://example.invalid"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/dev/gemini/status").header("Sec-Fetch-Site", "same-origin"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/dev/gemini/status").header("X-Forwarded-For", "127.0.0.1").with(request -> {
            request.setRemoteAddr("203.0.113.1"); return request;
        })).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void acceptsNoFormPostsAndNoGetVerificationCalls() throws Exception {
        mvc.perform(post("/api/dev/gemini/test").contentType(MediaType.APPLICATION_FORM_URLENCODED).content("prompt=test"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/api/dev/gemini/test")).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }

    @Test
    void mockProviderCannotClaimLiveVerification() throws Exception {
        var empty = new DefaultListableBeanFactory();
        var localMvc = MockMvcBuilders.standaloneSetup(new GeminiDiagnosticsController(empty.getBeanProvider(GeminiAIService.class), properties)).build();
        localMvc.perform(post("/api/dev/gemini/test").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("PROVIDER_NOT_ENABLED"));
    }
}
