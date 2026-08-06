package com.minecompanion.safety;

import com.minecompanion.chat.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Enforces role-based authorization on intents.
 *
 * Role restrictions are configured in application.yml under
 * safety-policy.role-restrictions as a map of intent name → allowed roles.
 * Intents not present in the map are accessible to all roles.
 *
 * Example config:
 *   safety-policy:
 *     role-restrictions:
 *       EQUIPMENT_HELP: [MAINTENANCE, SUPERVISOR, SAFETY_OFFICER]
 *
 * Adding a new restriction requires only a config change — no code change.
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "safety-policy")
public class RoleAuthorizationPolicy implements SafetyPolicy {

    private Map<String, List<String>> roleRestrictions = Map.of();

    public void setRoleRestrictions(Map<String, List<String>> roleRestrictions) {
        this.roleRestrictions = roleRestrictions;
    }

    @Override
    public int priority() { return 20; }

    @Override
    public PolicyVerdict evaluate(PolicyContext context) {
        String intentName = context.getIntent().name();
        List<String> allowedRoles = roleRestrictions.get(intentName);

        // Intent not restricted — all roles permitted
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return PolicyVerdict.pass(name());
        }

        UserRole userRole = context.getRole();
        boolean permitted = allowedRoles.stream()
                .anyMatch(r -> r.equalsIgnoreCase(userRole.name()));

        if (!permitted) {
            log.warn("[RoleAuthorizationPolicy] Access denied | intent={} role={}",
                    intentName, userRole);
            return PolicyVerdict.block(
                    name(),
                    "Role " + userRole + " is not authorized for intent " + intentName,
                    "This information is available to maintenance engineers and supervisors. " +
                    "Please contact your supervisor or maintenance team for assistance."
            );
        }

        return PolicyVerdict.pass(name());
    }
}
