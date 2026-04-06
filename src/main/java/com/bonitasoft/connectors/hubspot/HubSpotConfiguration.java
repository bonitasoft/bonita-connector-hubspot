package com.bonitasoft.connectors.hubspot;

import lombok.Builder;
import lombok.Data;

/**
 * Immutable configuration for HubSpot connector operations.
 * Holds all connection, auth, and operation-specific parameters.
 */
@Data
@Builder
public class HubSpotConfiguration {

    // === Connection / Auth parameters (Project/Runtime scope) ===
    @Builder.Default
    private String authMode = "PRIVATE_APP";

    private String privateAppToken;
    private String clientId;
    private String clientSecret;
    private String refreshToken;

    @Builder.Default
    private String basePath = "https://api.hubapi.com";

    @Builder.Default
    private int connectTimeout = 30000;

    @Builder.Default
    private int readTimeout = 60000;

    // === Deal parameters ===
    private String dealName;
    private String dealStage;
    private String pipelineId;
    private String amount;
    private String closeDate;
    private String dealOwnerId;
    private String dealType;
    private String description;
    private String dealId;

    // === Contact parameters ===
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String company;
    private String jobTitle;
    private String lifecycleStage;
    private String contactOwnerId;
    private String contactId;

    // === Search parameters ===
    private String searchFilters;
    private String properties;
    @Builder.Default
    private int limit = 10;
    private String after;
    private String sortBy;
    @Builder.Default
    private String sortDirection = "ASCENDING";

    // === Custom properties (JSON) ===
    private String customProperties;

    // === Retry ===
    @Builder.Default
    private int maxRetries = 5;
}
