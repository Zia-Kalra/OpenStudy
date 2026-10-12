package com.openstudy.openstudy.dto;

/**
 * Teacher identity for the portal. Phase 1 uses a labeled demo identity
 * until Member 1 provides session-based authentication.
 */
public class TeacherIdentityDTO {

    private final String teacherId;
    private final String displayName;
    private final String email;
    private final String role;
    private final boolean demoIdentity;

    public TeacherIdentityDTO(
            String teacherId,
            String displayName,
            String email,
            String role,
            boolean demoIdentity) {
        this.teacherId = teacherId;
        this.displayName = displayName;
        this.email = email;
        this.role = role;
        this.demoIdentity = demoIdentity;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public boolean isDemoIdentity() {
        return demoIdentity;
    }

    public String getInitials() {
        if (displayName == null || displayName.isBlank()) {
            return "T";
        }
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
