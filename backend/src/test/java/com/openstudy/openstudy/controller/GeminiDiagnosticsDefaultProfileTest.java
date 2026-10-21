package com.openstudy.openstudy.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mock")
class GeminiDiagnosticsDefaultProfileTest {
    @Autowired private MockMvc mvc;
    @Test
    void diagnosticsAreAbsentFromTheExistingApplication() throws Exception {
        mvc.perform(get("/api/dev/gemini/status")).andExpect(status().isNotFound());
    }
}
