package pl.routecost.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfiguration {

    private static final String BEARER_SCHEME = "bearerAuth";

    private static final Map<String, String> TAGS = Map.of(
            "/api/auth", "Authentication",
            "/api/vehicles", "Vehicles",
            "/api/vehicles/{vehicleId}/fuel-entries", "Fuel",
            "/api/vehicles/{vehicleId}/fuel-statistics", "Fuel",
            "/api/vehicles/{vehicleId}/expenses", "Expenses",
            "/api/vehicles/{vehicleId}/expense-statistics", "Expenses",
            "/api/vehicles/{vehicleId}/reminders", "Reminders",
            "/api/admin", "Admin",
            "/api/dashboard", "Dashboard and cost reports",
            "/api/vehicles/{vehicleId}/cost-summary", "Dashboard and cost reports"
    );

    private static final Map<String, String> SUMMARIES = Map.ofEntries(
            Map.entry("POST /api/auth/register", "Register an account"),
            Map.entry("POST /api/auth/login", "Log in and issue an access token"),
            Map.entry("POST /api/auth/refresh", "Refresh the access token using the refresh cookie"),
            Map.entry("POST /api/auth/logout", "Revoke the refresh session"),
            Map.entry("GET /api/auth/me", "Get the current user profile"),
            Map.entry("GET /api/vehicles", "List the current user's vehicles"),
            Map.entry("GET /api/vehicles/{vehicleId}", "Get a vehicle"),
            Map.entry("POST /api/vehicles", "Create a vehicle"),
            Map.entry("PUT /api/vehicles/{vehicleId}", "Replace a vehicle"),
            Map.entry("DELETE /api/vehicles/{vehicleId}", "Delete a vehicle"),
            Map.entry("GET /api/vehicles/{vehicleId}/fuel-entries", "List fuel entries"),
            Map.entry("GET /api/vehicles/{vehicleId}/fuel-entries/{entryId}", "Get a fuel entry"),
            Map.entry("POST /api/vehicles/{vehicleId}/fuel-entries", "Record a fuel entry"),
            Map.entry("PUT /api/vehicles/{vehicleId}/fuel-entries/{entryId}", "Replace a fuel entry"),
            Map.entry("DELETE /api/vehicles/{vehicleId}/fuel-entries/{entryId}", "Delete a fuel entry"),
            Map.entry("GET /api/vehicles/{vehicleId}/fuel-statistics", "Get fuel consumption statistics"),
            Map.entry("GET /api/vehicles/{vehicleId}/expenses", "List expenses"),
            Map.entry("GET /api/vehicles/{vehicleId}/expenses/{expenseId}", "Get an expense"),
            Map.entry("POST /api/vehicles/{vehicleId}/expenses", "Record an expense"),
            Map.entry("PUT /api/vehicles/{vehicleId}/expenses/{expenseId}", "Replace an expense"),
            Map.entry("DELETE /api/vehicles/{vehicleId}/expenses/{expenseId}", "Delete an expense"),
            Map.entry("GET /api/vehicles/{vehicleId}/expense-statistics", "Get expense statistics in PLN"),
            Map.entry("GET /api/vehicles/{vehicleId}/reminders", "List vehicle reminders"),
            Map.entry("GET /api/vehicles/{vehicleId}/reminders/{reminderId}", "Get a reminder"),
            Map.entry("POST /api/vehicles/{vehicleId}/reminders", "Create a reminder"),
            Map.entry("PUT /api/vehicles/{vehicleId}/reminders/{reminderId}", "Replace a reminder"),
            Map.entry("PATCH /api/vehicles/{vehicleId}/reminders/{reminderId}/status", "Change reminder status"),
            Map.entry("POST /api/vehicles/{vehicleId}/reminders/{reminderId}/reactivate", "Reactivate a reminder"),
            Map.entry("DELETE /api/vehicles/{vehicleId}/reminders/{reminderId}", "Delete a reminder"),
            Map.entry("GET /api/admin/users", "Search and filter users"),
            Map.entry("PATCH /api/admin/users/{userId}/role", "Change a user's role"),
            Map.entry("PATCH /api/admin/users/{userId}/status", "Change a user's enabled status"),
            Map.entry("GET /api/admin/dashboard", "Get global application statistics"),
            Map.entry("GET /api/dashboard/summary", "Get the current user's dashboard summary"),
            Map.entry("GET /api/vehicles/{vehicleId}/cost-summary", "Get a vehicle cost summary")
    );

    @Bean
    OpenAPI routeCostOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RouteCost API")
                        .description("REST API do zarządzania pojazdami, tankowaniami, wydatkami i przypomnieniami. "
                                + "Publiczna rejestracja tworzy konto USER. Operacje chronione wymagają access tokenu JWT; "
                                + "refresh token pozostaje w ciasteczku HttpOnly.")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    @Bean
    OpenApiCustomizer routeCostOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) return;
            openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap().forEach((method, operation) -> {
                String key = method.name() + " " + path;
                String tag = tagFor(path);
                operation.setTags(List.of(tag));
                operation.setSummary(SUMMARIES.getOrDefault(key, tag + " operation"));
                operation.setDescription(descriptionFor(path, method.name()));

                boolean publicAuth = path.matches("/api/auth/(register|login|refresh|logout)");
                if (!publicAuth) operation.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));

                String successStatus = method.name().equals("DELETE") ? "204"
                        : method.name().equals("POST") && (path.endsWith("/register")
                        || !path.endsWith("/login") && !path.endsWith("/refresh")
                        && !path.endsWith("/logout") && !path.endsWith("/reactivate")) ? "201" : "200";
                ApiResponses responses = operation.getResponses();
                if (!"200".equals(successStatus)) {
                    ApiResponse successResponse = responses.remove("200");
                    if (successResponse == null) successResponse = response(
                            "204".equals(successStatus) ? "Resource deleted." : "Resource created.");
                    responses.addApiResponse(successStatus, successResponse);
                }

                if ((method.name().equals("POST") || method.name().equals("PUT") || method.name().equals("PATCH"))
                        && !path.endsWith("/refresh") && !path.endsWith("/logout")) {
                    responses.addApiResponse("400", response("Invalid request data."));
                }
                if (!publicAuth || path.endsWith("/login") || path.endsWith("/refresh")) {
                    responses.addApiResponse("401", response("Missing, invalid or expired access token or invalid credentials."));
                }
                if (path.startsWith("/api/admin/") || path.endsWith("/refresh") || path.endsWith("/logout")) {
                    responses.addApiResponse("403", response("The authenticated user is not allowed to perform this operation, or the required auth header is missing."));
                }
                if (path.matches(".*/\\{(vehicleId|entryId|expenseId|reminderId|userId)}.*")) {
                    responses.addApiResponse("404", response("The requested resource was not found or is not accessible."));
                }
                if (path.contains("/admin/users/") || path.endsWith("/register")
                        || path.endsWith("/status") || path.endsWith("/reactivate")) {
                    responses.addApiResponse("409", response("The requested operation conflicts with current application state."));
                }
            }));
        };
    }

    private static String tagFor(String path) {
        return TAGS.entrySet().stream()
                .filter(entry -> path.equals(entry.getKey()) || path.startsWith(entry.getKey() + "/"))
                .max(Map.Entry.comparingByKey((left, right) -> Integer.compare(left.length(), right.length())))
                .map(Map.Entry::getValue)
                .orElse("API");
    }

    private static ApiResponse response(String description) {
        return new ApiResponse().description(description);
    }

    private static String descriptionFor(String path, String method) {
        if (path.startsWith("/api/admin/")) return "Dostępne wyłącznie dla użytkownika z rolą ADMIN.";
        if (path.matches("/api/auth/(register|login|refresh|logout)")) {
            return "Operacja publiczna. Refresh i logout wymagają ciasteczka refresh oraz nagłówka X-RouteCost-Auth: 1.";
        }
        if (path.equals("/api/auth/me")) return "Wymaga Bearer access tokenu JWT.";
        if (path.contains("/fuel-statistics")) return "Średnie spalanie jest obliczane z odstępów pomiędzy kolejnymi pełnymi tankowaniami.";
        if (path.contains("/expense-statistics")) return "Wszystkie kwoty są wyrażone w PLN.";
        if (path.contains("/reminders")) return "Przypomnienia są przypisane do pojazdu i podlegają regułom przejść statusów.";
        if (path.contains("/fuel-entries") || path.contains("/expenses") || path.contains("/vehicles")) {
            return "Zasób jest dostępny tylko właścicielowi pojazdu. Parametry i pola żądania wynikają ze schematu.";
        }
        return method.equals("GET") ? "Zwraca dane przypisane do uwierzytelnionego użytkownika." : "Operacja wymaga uwierzytelnienia.";
    }
}
