package com.openstudy.openstudy.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.openstudy.openstudy.service.ai.GeminiAIService;

@SpringBootTest(properties = "openstudy.ai.gemini.api-key=SYNTHETIC_PROFILE_KEY")
@AutoConfigureMockMvc
@ActiveProfiles({"mock", "gemini-local"})
class GeminiDiagnosticsLocalProfileTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private GeminiAIService gemini;

    @Test
    void localProfileEnablesSanitizedStatusWithoutHttpCalls() throws Exception {
        mvc.perform(get("/api/dev/gemini/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CONFIGURED_NOT_VERIFIED"));
        org.mockito.Mockito.verifyNoInteractions(gemini);
    }
}
