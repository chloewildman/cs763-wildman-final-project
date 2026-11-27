/* 
Architectural Header

Language stack: Java/Spring Boot
AI contribution: 100%

*/

package edu.bu.met.wildmancs763.discussions.security;

import org.springframework.security.core.Authentication;

import java.util.List;

public final class CallerIdentity {

    private CallerIdentity() {
    }

    public static String subject(Authentication authentication) {
        return authentication.getName();
    }

    public static boolean hasRole(Authentication authentication, String role) {
        String authority = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    public static boolean isAdministrator(Authentication authentication) {
        return hasRole(authentication, "ADMIN");
    }

    public static boolean isModerator(Authentication authentication) {
        return isAdministrator(authentication)
                || hasRole(authentication, "FACULTY_INSTRUCTOR");
    }

    public static List<String> roles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(granted -> granted.getAuthority().substring("ROLE_".length()).toLowerCase())
                .sorted()
                .toList();
    }
}
