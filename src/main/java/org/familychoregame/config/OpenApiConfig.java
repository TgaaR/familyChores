package org.familychoregame.config;

import java.util.List;
import java.util.Map;
import java.util.Set;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;

@Configuration
public class OpenApiConfig {
    private static final Set<String> PUBLIC_OPERATIONS = Set.of("session", "loginMembers", "login", "setupFamily");
    private static final Map<String, String> SUMMARIES = Map.ofEntries(
            Map.entry("session", "Check login status and refresh the CSRF cookie"),
            Map.entry("loginMembers", "List members available for login"),
            Map.entry("login", "Log in with a member ID and PIN"),
            Map.entry("logout", "Log out"),
            Map.entry("bootstrap", "Get the family, catalogs, balances, and weekly boards"),
            Map.entry("setupFamily", "Create a family and its initial members"),
            Map.entry("week", "Get a weekly board"),
            Map.entry("addMember", "Add a family member"),
            Map.entry("archiveMember", "Archive a family member"),
            Map.entry("createAssignment", "Assign a chore"),
            Map.entry("deleteAssignment", "Delete a chore assignment"),
            Map.entry("setCompletion", "Update chore completion"),
            Map.entry("redeemRight", "Redeem a reward using earned points"),
            Map.entry("clearAllData", "Permanently erase all application data"));

    @Bean
    OpenAPI familyChoresOpenApi() {
        return new OpenAPI().info(new Info().title("Family Chores API").version("v1")
                .description("Browse an endpoint, select Try it out, fill in its request, and select Execute. "
                        + "For authenticated calls, log in through the main app in another tab on the same host, "
                        + "or execute GET /api/v1/login-members followed by POST /api/v1/login here. "
                        + "After login or logout, execute GET /api/v1/session to refresh the CSRF cookie. "
                        + "Swagger sends session cookies and CSRF headers automatically. "
                        + "On a fresh installation, create a family through the main app or POST /api/v1/families first."))
                .components(new Components().addSecuritySchemes("memberSession", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("JSESSIONID")
                        .description("Obtained by logging in; the browser sends it automatically. "
                                + "Do not enter a cookie manually in Authorize.")));
    }

    @Bean
    OperationCustomizer familyChoresOperations() {
        return (operation, handler) -> {
            var name = handler.getMethod().getName();
            operation.setSummary(SUMMARIES.getOrDefault(name, name));
            boolean parentOnly = handler.hasMethodAnnotation(PreAuthorize.class);
            operation.setTags(List.of(name.equals("clearAllData") ? "Administration"
                    : handler.getBeanType().getSimpleName().equals("AuthController") ? "Login" : "Family game"));
            operation.setSecurity(PUBLIC_OPERATIONS.contains(name) ? List.of()
                    : List.of(new SecurityRequirement().addList("memberSession")));
            if (parentOnly) operation.setDescription("Parent login required.");
            if (name.equals("clearAllData")) {
                operation.setDescription("Parent login required. Permanently clears all eight application tables, "
                        + "including family members, PIN hashes, catalogs, assignments, and rewards. "
                        + "Preserves schema_migrations. Logs the caller out and requires fresh family setup.");
            }
            if (name.equals("week")) operation.setDescription("weekStart must be a Sunday in YYYY-MM-DD format.");
            return operation;
        };
    }
}