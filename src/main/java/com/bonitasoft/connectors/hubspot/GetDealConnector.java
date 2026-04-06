package com.bonitasoft.connectors.hubspot;

import lombok.extern.slf4j.Slf4j;

/**
 * [BETA] Retrieves current state of a HubSpot deal.
 * API: GET /crm/v3/objects/deals/{dealId}
 */
@Slf4j
public class GetDealConnector extends AbstractHubSpotConnector {

    static final String INPUT_AUTH_MODE = "authMode";
    static final String INPUT_PRIVATE_APP_TOKEN = "privateAppToken";
    static final String INPUT_CLIENT_ID = "clientId";
    static final String INPUT_CLIENT_SECRET = "clientSecret";
    static final String INPUT_REFRESH_TOKEN = "refreshToken";
    static final String INPUT_BASE_PATH = "basePath";
    static final String INPUT_CONNECT_TIMEOUT = "connectTimeout";
    static final String INPUT_READ_TIMEOUT = "readTimeout";
    static final String INPUT_DEAL_ID = "dealId";
    static final String INPUT_PROPERTIES = "properties";

    static final String OUTPUT_DEAL_ID = "dealId";
    static final String OUTPUT_DEAL_NAME = "dealName";
    static final String OUTPUT_DEAL_STAGE = "dealStage";
    static final String OUTPUT_PIPELINE_ID = "pipelineId";
    static final String OUTPUT_AMOUNT = "amount";
    static final String OUTPUT_CLOSE_DATE = "closeDate";
    static final String OUTPUT_DEAL_OWNER_ID = "dealOwnerId";
    static final String OUTPUT_CREATED_AT = "createdAt";
    static final String OUTPUT_UPDATED_AT = "updatedAt";
    static final String OUTPUT_ALL_PROPERTIES = "allProperties";

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
                .dealId(readStringInput(INPUT_DEAL_ID))
                .properties(readStringInput(INPUT_PROPERTIES))
                .build();
    }

    @Override
    protected void validateConfiguration(HubSpotConfiguration config) {
        super.validateConfiguration(config);
        if (config.getDealId() == null || config.getDealId().isBlank()) {
            throw new IllegalArgumentException("dealId is mandatory");
        }
    }

    @Override
    protected void doExecute() throws HubSpotException {
        log.info("Executing Get Deal connector — dealId={}", configuration.getDealId());

        HubSpotClient.GetDealResult result = client.getDeal(
                configuration.getDealId(),
                configuration.getProperties()
        );

        setOutputParameter(OUTPUT_DEAL_ID, result.dealId());
        setOutputParameter(OUTPUT_DEAL_NAME, result.dealName());
        setOutputParameter(OUTPUT_DEAL_STAGE, result.dealStage());
        setOutputParameter(OUTPUT_PIPELINE_ID, result.pipelineId());
        setOutputParameter(OUTPUT_AMOUNT, result.amount());
        setOutputParameter(OUTPUT_CLOSE_DATE, result.closeDate());
        setOutputParameter(OUTPUT_DEAL_OWNER_ID, result.dealOwnerId());
        setOutputParameter(OUTPUT_CREATED_AT, result.createdAt());
        setOutputParameter(OUTPUT_UPDATED_AT, result.updatedAt());
        setOutputParameter(OUTPUT_ALL_PROPERTIES, result.allProperties());

        log.info("Get Deal connector executed successfully");
    }
}
