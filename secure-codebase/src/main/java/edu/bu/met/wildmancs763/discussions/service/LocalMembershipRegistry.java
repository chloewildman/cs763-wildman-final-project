package edu.bu.met.wildmancs763.discussions.service;

import edu.bu.met.wildmancs763.discussions.domain.ScopeType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Locale;
import java.util.Set;

@Component
public class LocalMembershipRegistry {

    private static final Map<ScopeType, Map<String, Set<String>>> MEMBERSHIPS = Map.of(
            ScopeType.PROJECT, Map.of(
                    "project-42", Set.of("alice", "bob"),
                    "project-1", Set.of("alice", "bob"),
                    "project-99", Set.of("alice")),
            ScopeType.COURSE, Map.of(
                    "course-9", Set.of("alice", "bob", "professor1")),
            ScopeType.LAB, Map.of(
                    "lab-1", Set.of("alice", "bob", "professor1")));

    public boolean isMember(ScopeType scopeType, String scopeId, String subject) {
        return MEMBERSHIPS.getOrDefault(scopeType, Map.of())
                .getOrDefault(scopeId.toLowerCase(Locale.ROOT), Set.of())
                .contains(subject);
    }
}
