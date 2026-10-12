package com.openstudy.openstudy.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class TeacherModulePropertiesTest {
    @Test
    void rejectsExternalAndExecutableNavigationTargets() {
        var properties = new TeacherModuleProperties();
        for (String path : new String[] { null, "javascript:alert(1)", "https://example.invalid", "//example.invalid", "/\\example.invalid", "/teacher\n" }) {
            assertThatThrownBy(() -> properties.setQuizzesPath(path)).isInstanceOf(IllegalArgumentException.class);
        }
        properties.setQuizzesPath("/teacher/quizzes?view=recent&filter=active");
        assertThat(properties.getQuizzesPath()).isEqualTo("/teacher/quizzes?view=recent&filter=active");
    }
}
