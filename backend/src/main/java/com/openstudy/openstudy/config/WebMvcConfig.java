package com.openstudy.openstudy.config;

import com.openstudy.openstudy.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor())
                .addPathPatterns("/student/**", "/teacher/**")
                .excludePathPatterns(
                        "/student",
                        "/student/login",
                        "/login",
                        "/logout",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/error"
                );
    }

    private static class AuthInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            HttpSession session = request.getSession(false);
            User user = session == null ? null : (User) session.getAttribute("loggedInUser");
            if (user == null) {
                response.sendRedirect(request.getContextPath() + "/login?error=unauthenticated");
                return false;
            }

            String path = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }

            if (path.startsWith("/student/" ) && !"student".equalsIgnoreCase(user.getRole())) {
                response.sendRedirect(contextPath + "/teacher/dashboard");
                return false;
            }

            if (path.startsWith("/teacher/") && !"teacher".equalsIgnoreCase(user.getRole())) {
                response.sendRedirect(contextPath + "/student/dashboard");
                return false;
            }

            return true;
        }
    }
}
