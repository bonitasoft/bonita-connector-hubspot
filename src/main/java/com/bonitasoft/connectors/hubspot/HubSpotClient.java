package com.bonitasoft.connectors.hubspot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP client facade for HubSpot CRM API v3.
 * Uses java.net.http.HttpClient with Bearer token or OAuth2 authentication.
 * All methods use the RetryPolicy for automatic exponential backoff on 429/5xx.
 */
@Slf4j
public class HubSpotClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HubSpotConfiguration configuration;
    private final HttpClient httpClient;
    private final RetryPolicy retryPolicy;
    private String accessToken;

    public HubSpotClient(HubSpotConfiguration configuration) throws HubSpotException {
        this.configuration = configuration;
        this.retryPolicy = new RetryPolicy(configuration.getMaxRetries());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(configuration.getConnectTimeout()))
                .build();
        resolveAccessToken();
        log.debug("HubSpotClient initialized with authMode={}", configuration.getAuthMode());
    }

    // Visible for testing
    HubSpotClient(HubSpotConfiguration configuration, HttpClient httpClient, RetryPolicy retryPolicy, String accessToken) {
        this.configuration = configuration;
        this.httpClient = httpClient;
        this.retryPolicy = retryPolicy;
        this.accessToken = accessToken;
    }

    private void resolveAccessToken() throws HubSpotException {
        if ("OAUTH2".equals(configuration.getAuthMode())) {
            refreshOAuth2Token();
        } else {
            // Private App Token - resolve from config/sysprop/env
            this.accessToken = resolveValue(
                    configuration.getPrivateAppToken(),
                    "hubspot.privateAppToken",
                    "HUBSPOT_PRIVATE_APP_TOKEN"
            );
            if (this.accessToken == null || this.accessToken.isBlank()) {
                throw new HubSpotException("Private App Token not found in configuration, system property, or environment variable");
            }
        }
    }

    private void refreshOAuth2Token() throws HubSpotException {
        try {
            String clientId = resolveValue(configuration.getClientId(), "hubspot.clientId", "HUBSPOT_CLIENT_ID");
            String clientSecret = resolveValue(configuration.getClientSecret(), "hubspot.clientSecret", "HUBSPOT_CLIENT_SECRET");
            String refreshToken = resolveValue(configuration.getRefreshToken(), "hubspot.refreshToken", "HUBSPOT_REFRESH_TOKEN");

            String body = "grant_type=refresh_token"
                    + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                    + "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(configuration.getBasePath() + "/oauth/v1/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofMillis(configuration.getReadTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new HubSpotException(
                        "OAuth2 token refresh failed (HTTP " + response.statusCode() + "): " + response.body(),
                        response.statusCode(), false);
            }

            JsonNode json = MAPPER.readTree(response.body());
            this.accessToken = json.get("access_token").asText();
            log.debug("OAuth2 access token refreshed successfully");
        } catch (HubSpotException e) {
            throw e;
        } catch (Exception e) {
            throw new HubSpotException("OAuth2 token refresh failed: " + e.getMessage(), e);
        }
    }

    private String resolveValue(String directValue, String sysProp, String envVar) {
        if (directValue != null && !directValue.isBlank()) return directValue;
        String sysValue = System.getProperty(sysProp);
        if (sysValue != null && !sysValue.isBlank()) return sysValue;
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isBlank()) return envValue;
        return null;
    }

    // === Deal Operations ===

    public CreateDealResult createDeal(String dealName, String dealStage, String pipelineId,
                                       String amount, String closeDate, String dealOwnerId,
                                       String dealType, String description,
                                       String customProperties) throws HubSpotException {
        return retryPolicy.execute(() -> {
            ObjectNode properties = MAPPER.createObjectNode();
            properties.put("dealname", dealName);
            properties.put("dealstage", dealStage);
            if (pipelineId != null && !pipelineId.isBlank()) properties.put("pipeline", pipelineId);
            if (amount != null && !amount.isBlank()) properties.put("amount", amount);
            if (closeDate != null && !closeDate.isBlank()) properties.put("closedate", closeDate);
            if (dealOwnerId != null && !dealOwnerId.isBlank()) properties.put("hubspot_owner_id", dealOwnerId);
            if (dealType != null && !dealType.isBlank()) properties.put("dealtype", dealType);
            if (description != null && !description.isBlank()) properties.put("description", description);
            mergeCustomProperties(properties, customProperties);

            ObjectNode body = MAPPER.createObjectNode();
            body.set("properties", properties);

            JsonNode response = doPost("/crm/v3/objects/deals", body);
            String id = response.get("id").asText();
            JsonNode props = response.get("properties");

            return new CreateDealResult(
                    id,
                    getTextOrNull(props, "dealname"),
                    getTextOrNull(props, "dealstage"),
                    getTextOrNull(props, "createdate"),
                    getTextOrNull(props, "hs_lastmodifieddate"),
                    buildDealUrl(id)
            );
        });
    }

    public UpdateDealResult updateDeal(String dealId, String dealStage, String amount,
                                       String closeDate, String dealOwnerId,
                                       String customProperties) throws HubSpotException {
        return retryPolicy.execute(() -> {
            ObjectNode properties = MAPPER.createObjectNode();
            if (dealStage != null && !dealStage.isBlank()) properties.put("dealstage", dealStage);
            if (amount != null && !amount.isBlank()) properties.put("amount", amount);
            if (closeDate != null && !closeDate.isBlank()) properties.put("closedate", closeDate);
            if (dealOwnerId != null && !dealOwnerId.isBlank()) properties.put("hubspot_owner_id", dealOwnerId);
            mergeCustomProperties(properties, customProperties);

            ObjectNode body = MAPPER.createObjectNode();
            body.set("properties", properties);

            JsonNode response = doPatch("/crm/v3/objects/deals/" + dealId, body);
            JsonNode props = response.get("properties");

            return new UpdateDealResult(
                    response.get("id").asText(),
                    getTextOrNull(props, "dealstage"),
                    getTextOrNull(props, "hs_lastmodifieddate")
            );
        });
    }

    public GetDealResult getDeal(String dealId, String propertiesList) throws HubSpotException {
        return retryPolicy.execute(() -> {
            String path = "/crm/v3/objects/deals/" + dealId;
            if (propertiesList != null && !propertiesList.isBlank()) {
                path += "?properties=" + URLEncoder.encode(propertiesList.replaceAll("\\s+", ""), StandardCharsets.UTF_8);
            }

            JsonNode response = doGet(path);
            JsonNode props = response.get("properties");

            // Serialize all properties as JSON string
            String allPropertiesJson = props != null ? MAPPER.writeValueAsString(props) : "{}";

            return new GetDealResult(
                    response.get("id").asText(),
                    getTextOrNull(props, "dealname"),
                    getTextOrNull(props, "dealstage"),
                    getTextOrNull(props, "pipeline"),
                    getTextOrNull(props, "amount"),
                    getTextOrNull(props, "closedate"),
                    getTextOrNull(props, "hubspot_owner_id"),
                    getTextOrNull(props, "createdate"),
                    getTextOrNull(props, "hs_lastmodifieddate"),
                    allPropertiesJson
            );
        });
    }

    // === Contact Operations ===

    public CreateContactResult createContact(String email, String firstName, String lastName,
                                              String phone, String company, String jobTitle,
                                              String lifecycleStage, String contactOwnerId,
                                              String customProperties) throws HubSpotException {
        return retryPolicy.execute(() -> {
            ObjectNode properties = MAPPER.createObjectNode();
            properties.put("email", email);
            if (firstName != null && !firstName.isBlank()) properties.put("firstname", firstName);
            if (lastName != null && !lastName.isBlank()) properties.put("lastname", lastName);
            if (phone != null && !phone.isBlank()) properties.put("phone", phone);
            if (company != null && !company.isBlank()) properties.put("company", company);
            if (jobTitle != null && !jobTitle.isBlank()) properties.put("jobtitle", jobTitle);
            if (lifecycleStage != null && !lifecycleStage.isBlank()) properties.put("lifecyclestage", lifecycleStage);
            if (contactOwnerId != null && !contactOwnerId.isBlank()) properties.put("hubspot_owner_id", contactOwnerId);
            mergeCustomProperties(properties, customProperties);

            ObjectNode body = MAPPER.createObjectNode();
            body.set("properties", properties);

            try {
                JsonNode response = doPost("/crm/v3/objects/contacts", body);
                String id = response.get("id").asText();
                JsonNode props = response.get("properties");

                return new CreateContactResult(
                        id,
                        getTextOrNull(props, "email"),
                        getTextOrNull(props, "createdate"),
                        getTextOrNull(props, "hs_lastmodifieddate"),
                        true,
                        buildContactUrl(id)
                );
            } catch (HubSpotException e) {
                // Handle 409 Conflict - contact already exists
                if (e.getStatusCode() == 409) {
                    String existingId = extractExistingContactId(e.getMessage());
                    return new CreateContactResult(
                            existingId,
                            email,
                            null,
                            null,
                            false,
                            existingId != null ? buildContactUrl(existingId) : null
                    );
                }
                throw e;
            }
        });
    }

    public UpdateContactResult updateContact(String contactId, String email, String firstName,
                                              String lastName, String phone, String company,
                                              String jobTitle, String lifecycleStage,
                                              String contactOwnerId,
                                              String customProperties) throws HubSpotException {
        return retryPolicy.execute(() -> {
            ObjectNode properties = MAPPER.createObjectNode();
            if (email != null && !email.isBlank()) properties.put("email", email);
            if (firstName != null && !firstName.isBlank()) properties.put("firstname", firstName);
            if (lastName != null && !lastName.isBlank()) properties.put("lastname", lastName);
            if (phone != null && !phone.isBlank()) properties.put("phone", phone);
            if (company != null && !company.isBlank()) properties.put("company", company);
            if (jobTitle != null && !jobTitle.isBlank()) properties.put("jobtitle", jobTitle);
            if (lifecycleStage != null && !lifecycleStage.isBlank()) properties.put("lifecyclestage", lifecycleStage);
            if (contactOwnerId != null && !contactOwnerId.isBlank()) properties.put("hubspot_owner_id", contactOwnerId);
            mergeCustomProperties(properties, customProperties);

            ObjectNode body = MAPPER.createObjectNode();
            body.set("properties", properties);

            JsonNode response = doPatch("/crm/v3/objects/contacts/" + contactId, body);
            JsonNode props = response.get("properties");

            return new UpdateContactResult(
                    response.get("id").asText(),
                    getTextOrNull(props, "hs_lastmodifieddate")
            );
        });
    }

    public SearchContactsResult searchContacts(String searchFilters, String propertiesList,
                                                int limit, String after, String sortBy,
                                                String sortDirection) throws HubSpotException {
        return retryPolicy.execute(() -> {
            ObjectNode body = MAPPER.createObjectNode();

            // Parse searchFilters - user provides array of filters, we wrap in filterGroups
            JsonNode filtersNode = MAPPER.readTree(searchFilters);
            if (filtersNode.isArray()) {
                // Simple array of filters -> single filter group
                ArrayNode filterGroups = MAPPER.createArrayNode();
                ObjectNode group = MAPPER.createObjectNode();
                group.set("filters", filtersNode);
                filterGroups.add(group);
                body.set("filterGroups", filterGroups);
            } else if (filtersNode.isObject() && filtersNode.has("filterGroups")) {
                // Full filterGroups structure provided
                body.set("filterGroups", filtersNode.get("filterGroups"));
            }

            body.put("limit", limit);

            if (propertiesList != null && !propertiesList.isBlank()) {
                ArrayNode propsArray = MAPPER.createArrayNode();
                for (String prop : propertiesList.split(",")) {
                    propsArray.add(prop.trim());
                }
                body.set("properties", propsArray);
            }

            if (after != null && !after.isBlank()) {
                body.put("after", after);
            }

            if (sortBy != null && !sortBy.isBlank()) {
                ArrayNode sorts = MAPPER.createArrayNode();
                ObjectNode sort = MAPPER.createObjectNode();
                sort.put("propertyName", sortBy);
                sort.put("direction", sortDirection != null ? sortDirection : "ASCENDING");
                sorts.add(sort);
                body.set("sorts", sorts);
            }

            JsonNode response = doPost("/crm/v3/objects/contacts/search", body);

            int total = response.has("total") ? response.get("total").asInt() : 0;
            boolean hasMore = response.has("paging") && response.get("paging").has("next");
            String nextAfter = hasMore ? response.get("paging").get("next").get("after").asText() : "";

            JsonNode results = response.get("results");
            String contactsJson = results != null ? MAPPER.writeValueAsString(results) : "[]";

            String firstId = "";
            String firstEmail = "";
            if (results != null && results.isArray() && !results.isEmpty()) {
                JsonNode first = results.get(0);
                firstId = first.has("id") ? first.get("id").asText() : "";
                if (first.has("properties") && first.get("properties").has("email")) {
                    firstEmail = first.get("properties").get("email").asText();
                }
            }

            return new SearchContactsResult(
                    contactsJson,
                    total,
                    hasMore,
                    nextAfter,
                    firstId,
                    firstEmail
            );
        });
    }

    // === HTTP Methods ===

    private JsonNode doGet(String path) throws HubSpotException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(configuration.getBasePath() + path))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .GET()
                    .timeout(Duration.ofMillis(configuration.getReadTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response);
        } catch (HubSpotException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new HubSpotException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private JsonNode doPost(String path, ObjectNode body) throws HubSpotException {
        try {
            String jsonBody = MAPPER.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(configuration.getBasePath() + path))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofMillis(configuration.getReadTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response);
        } catch (HubSpotException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new HubSpotException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private JsonNode doPatch(String path, ObjectNode body) throws HubSpotException {
        try {
            String jsonBody = MAPPER.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(configuration.getBasePath() + path))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofMillis(configuration.getReadTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response);
        } catch (HubSpotException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new HubSpotException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private JsonNode handleResponse(HttpResponse<String> response) throws HubSpotException {
        int statusCode = response.statusCode();
        String responseBody = response.body();

        if (statusCode >= 200 && statusCode < 300) {
            try {
                return MAPPER.readTree(responseBody);
            } catch (JsonProcessingException e) {
                throw new HubSpotException("Failed to parse response: " + e.getMessage(), e);
            }
        }

        // Error handling
        boolean retryable = RetryPolicy.isRetryableStatusCode(statusCode);
        String errorMessage = buildErrorMessage(statusCode, responseBody);

        throw new HubSpotException(errorMessage, statusCode, retryable);
    }

    private String buildErrorMessage(int statusCode, String responseBody) {
        try {
            JsonNode error = MAPPER.readTree(responseBody);
            String message = getTextOrNull(error, "message");
            String category = getTextOrNull(error, "category");
            if (message != null) {
                return String.format("HubSpot API error %d (%s): %s", statusCode,
                        category != null ? category : "UNKNOWN", message);
            }
        } catch (JsonProcessingException ignored) {
            // Fall through to generic message
        }
        return "HubSpot API error " + statusCode + ": " + responseBody;
    }

    private void mergeCustomProperties(ObjectNode properties, String customPropertiesJson) throws HubSpotException {
        if (customPropertiesJson == null || customPropertiesJson.isBlank()) return;
        try {
            JsonNode custom = MAPPER.readTree(customPropertiesJson);
            if (custom.isObject()) {
                custom.fields().forEachRemaining(entry ->
                        properties.put(entry.getKey(), entry.getValue().asText()));
            }
        } catch (JsonProcessingException e) {
            throw new HubSpotException("Invalid customProperties JSON: " + e.getMessage(), e);
        }
    }

    private String getTextOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return null;
        return node.get(field).asText();
    }

    private String extractExistingContactId(String errorMessage) {
        // HubSpot 409 responses include the existing contact ID in the message
        // Pattern: "Contact already exists. Existing ID: 12345"
        if (errorMessage != null && errorMessage.contains("Existing ID:")) {
            String[] parts = errorMessage.split("Existing ID:");
            if (parts.length > 1) {
                return parts[1].trim().replaceAll("[^0-9]", "");
            }
        }
        // Try to extract from standard HubSpot error format
        if (errorMessage != null) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b(\\d{5,})\\b").matcher(errorMessage);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private String buildDealUrl(String dealId) {
        return "https://app.hubspot.com/contacts/deal/" + dealId;
    }

    private String buildContactUrl(String contactId) {
        return "https://app.hubspot.com/contacts/contact/" + contactId;
    }

    // === Result records ===

    public record CreateDealResult(String dealId, String dealName, String dealStage,
                                   String createdAt, String updatedAt, String dealUrl) {}

    public record UpdateDealResult(String dealId, String dealStage, String updatedAt) {}

    public record GetDealResult(String dealId, String dealName, String dealStage, String pipelineId,
                                String amount, String closeDate, String dealOwnerId,
                                String createdAt, String updatedAt, String allProperties) {}

    public record CreateContactResult(String contactId, String email, String createdAt,
                                      String updatedAt, boolean isNew, String contactUrl) {}

    public record UpdateContactResult(String contactId, String updatedAt) {}

    public record SearchContactsResult(String contacts, int totalResults, boolean hasMore,
                                       String nextAfter, String firstContactId,
                                       String firstContactEmail) {}
}
