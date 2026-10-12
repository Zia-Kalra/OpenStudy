package com.openstudy.openstudy.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.openstudy.openstudy.config.TeacherAnalyticsAccessConfiguration;
import com.openstudy.openstudy.dto.TeacherIdentityDTO;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsQuery;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsScope;
import com.openstudy.openstudy.provider.analytics.*;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.server.ResponseStatusException;

class TeacherAnalyticsAccessTest {
    @Test
    void defaultProfileFailsClosedWithoutAuthenticatedAdapter() {
        new ApplicationContextRunner().withPropertyValues("spring.profiles.active=production").withUserConfiguration(TeacherAnalyticsAccessConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(TeacherAnalyticsAccessProvider.class);
                    assertThatThrownBy(() -> context.getBean(TeacherAnalyticsAccessProvider.class).resolve(() -> "teacher"))
                            .isInstanceOf(ResponseStatusException.class)
                            .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(503);
                });
    }

    @Test
    void futureAuthenticatedAdapterReplacesFallbackWithoutChangingControllers() {
        var scope = new MockTeacherAnalyticsAccessProvider().resolve(null);
        new ApplicationContextRunner().withPropertyValues("spring.profiles.active=production").withUserConfiguration(TeacherAnalyticsAccessConfiguration.class)
                .withBean(TeacherAnalyticsAccessProvider.class, () -> principal -> scope)
                .run(context -> assertThat(context.getBean(TeacherAnalyticsAccessProvider.class).resolve(() -> "teacher")).isSameAs(scope));
    }

    @Test
    void mockPrincipalCannotSelectAnotherTeacherAndNonTeacherScopesAreRejected() {
        var provider = new MockTeacherAnalyticsAccessProvider();
        assertThat(provider.resolve(null).teacher().isDemoIdentity()).isTrue();
        assertThatThrownBy(() -> provider.resolve(() -> "student-1")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> new TeacherAnalyticsScope(new TeacherIdentityDTO("S1", "Student", "", "Student", false), Set.of("CS-2A"), Set.of("DS")))
                .isInstanceOf(IllegalArgumentException.class);
        var demo = provider.resolve(null);
        var unknownTeacher = new TeacherAnalyticsScope(new TeacherIdentityDTO("OTHER", "Other", "", "Teacher", true), demo.classIds(), demo.subjectIds());
        assertThat(new MockTeacherAnalyticsDataProvider().load(unknownTeacher).students()).isEmpty();
    }

    @Test
    void mockSnapshotIsImmutableAndFiltersAreBounded() {
        var snapshot = new MockTeacherAnalyticsDataProvider().load(new MockTeacherAnalyticsAccessProvider().resolve(null));
        assertThatThrownBy(() -> snapshot.students().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.students().getFirst().subjectIds().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new TeacherAnalyticsQuery(null, null, "x".repeat(101), null, null, null)).isInstanceOf(ResponseStatusException.class);
    }
}
