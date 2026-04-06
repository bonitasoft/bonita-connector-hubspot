package com.bonitasoft.connectors.hubspot;

import lombok.extern.slf4j.Slf4j;

/**
 * [BETA] Searches for contacts matching filter criteria in HubSpot CRM.
 * API: POST /crm/v3/objects/contacts/search
 */
@Slf4j
public class SearchContactsConnector extends AbstractHubSpotConnector {

    static final String INPUT_AUTH_MODE = "authMode";
    static final String INPUT_PRIVATE_APP_TOKEN = "privateAppToken";
    static final String INPUT_CLIENT_ID = "clientId";
    static final String INPUT_CLIENT_SECRET = "clientSecret";
    static final String INPUT_REFRESH_TOKEN = "refreshToken";
    static final String INPUT_BASE_PATH = "basePath";
    static final String INPUT_CONNECT_TIMEOUT = "connectTimeout";
    static final String INPUT_READ_TIMEOUT = "readTimeout";
    static final String INPUT_SEARCH_FILTERS = "searchFilters";
    static final String INPUT_PROPERTIES = "properties";
    static final String INPUT_LIMIT = "limit";
    static final String INPUT_AFTER = "after";
    static final String INPUT_SORT_BY = "sortBy";
    static final String INPUT_SORT_DIRECTION = "sortDirection";

    static final String OUTPUT_CONTACTS = "contacts";
    static final String OUTPUT_TOTAL_RESULTS = "totalResults";
    static final String OUTPUT_HAS_MORE = "hasMore";
    static final String OUTPUT_NEXT_AFTER = "nextAfter";
    static final String OUTPUT_FIRST_CONTACT_ID = "firstContactId";
    static final String OUTPUT_FIRST_CONTACT_EMAIL = "firstContactEmail";

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
                .searchFilters(readStringInput(INPUT_SEARCH_FILTERS))
                .properties(readStringInput(INPUT_PROPERTIES))
                .limit(readIntegerInput(INPUT_LIMIT, 10))
                .after(readStringInput(INPUT_AFTER))
                .sortBy(readStringInput(INPUT_SORT_BY))
                .sortDirection(readStringInput(INPUT_SORT_DIRECTION, "ASCENDING"))
                .build();
    }

    @Override
    protected void validateConfiguration(HubSpotConfiguration config) {
        super.validateConfiguration(config);
        if (config.getSearchFilters() == null || config.getSearchFilters().isBlank()) {
            throw new IllegalArgumentException("searchFilters is mandatory");
        }
    }

    @Override
    protected void doExecute() throws HubSpotException {
        log.info("Executing Search Contacts connector");

        HubSpotClient.SearchContactsResult result = client.searchContacts(
                configuration.getSearchFilters(),
                configuration.getProperties(),
                configuration.getLimit(),
                configuration.getAfter(),
                configuration.getSortBy(),
                configuration.getSortDirection()
        );

        setOutputParameter(OUTPUT_CONTACTS, result.contacts());
        setOutputParameter(OUTPUT_TOTAL_RESULTS, result.totalResults());
        setOutputParameter(OUTPUT_HAS_MORE, result.hasMore());
        setOutputParameter(OUTPUT_NEXT_AFTER, result.nextAfter());
        setOutputParameter(OUTPUT_FIRST_CONTACT_ID, result.firstContactId());
        setOutputParameter(OUTPUT_FIRST_CONTACT_EMAIL, result.firstContactEmail());

        log.info("Search Contacts connector executed successfully — totalResults={}", result.totalResults());
    }
}
