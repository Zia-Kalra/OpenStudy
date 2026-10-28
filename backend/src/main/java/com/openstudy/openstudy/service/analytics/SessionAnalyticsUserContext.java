package com.openstudy.openstudy.service.analytics;

import com.openstudy.openstudy.config.AnalyticsProperties;
import com.openstudy.openstudy.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Reads the signed-in {@link User} from the HTTP session. Until the login module stores one, an
 * explicitly enabled, server-configured development identity can be used instead.
 */
@Component
@Profile("!mock")
public class SessionAnalyticsUserContext implements AnalyticsUserContext {

    private static final Logger log = LoggerFactory.getLogger(SessionAnalyticsUserContext.class);

    private final AnalyticsProperties props;

    public SessionAnalyticsUserContext(AnalyticsProperties props) {
        this.props = props;
        if (props.getDevIdentity().isEnabled()) {
            log.warn("openstudy.analytics.dev-identity is ENABLED: analytics APIs ignore login and act as a fixed "
                    + "student/teacher. Never enable this in production.");
        }
    }

    @Override
    public long requireUserId(HttpServletRequest request, Role required) {
        HttpSession session = request.getSession(false);
        Object attr = session == null ? null : session.getAttribute(props.getSessionAttribute());
        if (attr instanceof User user && user.getUserId() != null) {
            if (user.getAccountStatus() != null && !"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
                throw new AnalyticsAccessException(HttpStatus.FORBIDDEN, "This account is not active.");
            }
            String role = user.getRole() == null ? "" : user.getRole().toUpperCase(Locale.ROOT);
            if (!role.equals(required.name())) {
                throw new AnalyticsAccessException(HttpStatus.FORBIDDEN, "This page is only available to " + required.name().toLowerCase(Locale.ROOT) + "s.");
            }
            return user.getUserId();
        }
        var dev = props.getDevIdentity();
        if (dev.isEnabled()) {
            long id = required == Role.STUDENT ? dev.getStudentId() : dev.getTeacherId();
            if (id > 0) {
                return id;
            }
        }
        throw new AnalyticsAccessException(HttpStatus.UNAUTHORIZED, "Please sign in to view analytics.");
    }
}
