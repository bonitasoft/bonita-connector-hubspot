package com.bonitasoft.connectors.hubspot;

import lombok.extern.slf4j.Slf4j;

/**
 * [BETA] Creates a new contact in HubSpot CRM.
 * Handles 409 conflict (email already exists) gracefully.
 * API: POST /crm/v3/objects/contacts
 */
@Slf4j
public class CreateContactConnector extends AbstractHubSpotConnector {

    static final String INPUT_AUTH_MODE = "authMode";
    static final String INPUT_PRIVATE_APP_TOKEN = "privateAppToken";
    static final String INPUT_CLIENT_ID = "clientId";
    static final String INPUT_CLIENT_SECRET = "clientSecret";
    static final String INPUT_REFRESH_TOKEN = "refreshToken";
    static final String INPUT_BASE_PATH = "basePath";
    static final String INPUT_CONNECT_TIMEOUT = "connectTimeout";
    static final String INPUT_READ_TIMEOUT = "readTimeout";
    static final String INPUT_EMAIL = "email";
    static final String INPUT_FIRST_NAME = "firstName";
    static final String INPUT_LAST_NAME = "lastName";
    static final String INPUT_PHONE = "phone";
    static final String INPUT_COMPANY = "company";
    static final String INPUT_JOB_TITLE = "jobTitle";
    static final String INPUT_LIFECYCLE_STAGE = "lifecycleStage";
    static final String INPUT_CONTACT_OWNER_ID = "contactOwnerId";
    static final String INPUT_CUSTOM_PROPERTIES = "customProperties";

    static final String OUTPUT_CONTACT_ID = "contactId";
    static final String OUTPUT_EMAIL = "email";
    static final String OUTPUT_CREATED_AT = "createdAt";
    static final String OUTPUT_UPDATED_AT = "updatedAt";
    static final String OUTPUT_IS_NEW = "isNew";
    static final String OUTPUT_CONTACT_URL = "contactUrl";

    @Override
    protected HubSpotConfiguration buildConfiguration() {
        return HubSpotConfiguration.builder()
                .authMode(readStringInput(INPUT_AUTH_MODE, "PRIVATE_APP"))
                .privateAppToken(readStringInput(INPUT_PRIVATE_APP_TOKEN))
                .clientId(readStringInput(INPUT_CLIENT_ID))
                .clientSecret(readStringInput(INPUT_CLIENT_SECRET))
                .refreshToken(readStringInput(INPUT_REFRESH_TOKEN))
                .basePath(readStringInput(INPUT_BASE_PATH, "https://api.hubapi.com"))
                .connectTimeout(readIntegerInput(INPUT_CONNECT_TIMEOUT, 30000))
                .readTimeout(readIntegerInput(INPUT_READ_TIMEOUT, 60000))
                .email(readStringInput(INPUT_EMAIL))
                .firstName(readStringInput(INPUT_FIRST_NAME))
                .lastName(readStringInput(INPUT_LAST_NAME))
                .phone(readStringInput(INPUT_PHONE))
                .company(readStringInput(INPUT_COMPANY))
                .jobTitle(readStringInput(INPUT_JOB_TITLE))
                .lifecycleStage(readStringInput(INPUT_LIFECYCLE_STAGE))
                .contactOwnerId(readStringInput(INPUT_CONTACT_OWNER_ID))
                .customProperties(readStringInput(INPUT_CUSTOM_PROPERTIES))
                .build();
    }

    @Override
    protected void validateConfiguration(HubSpotConfiguration config) {
        super.validateConfiguration(config);
        if (config.getEmail() == null || config.getEmail().isBlank()) {
            throw new IllegalArgumentException("email is mandatory");
        }
    }

    @Override
    protected void doExecute() throws HubSpotException {
        log.info("Executing Create Contact connector");

        HubSpotClient.CreateContactResult result = client.createContact(
                configuration.getEmail(),
                configuration.getFirstName(),
                configuration.getLastName(),
                configuration.getPhone(),
                configuration.getCompany(),
                configuration.getJobTitle(),
                configuration.getLifecycleStage(),
                configuration.getContactOwnerId(),
                configuration.getCustomProperties()
        );

        setOutputParameter(OUTPUT_CONTACT_ID, result.contactId());
        setOutputParameter(OUTPUT_EMAIL, result.email());
        setOutputParameter(OUTPUT_CREATED_AT, result.createdAt());
        setOutputParameter(OUTPUT_UPDATED_AT, result.updatedAt());
        setOutputParameter(OUTPUT_IS_NEW, result.isNew());
        setOutputParameter(OUTPUT_CONTACT_URL, result.contactUrl());

        if (!result.isNew()) {
            log.info("Contact already exists — contactId={}", result.contactId());
        } else {
            log.info("Create Contact connector executed successfully — contactId={}", result.contactId());
        }
    }
}
