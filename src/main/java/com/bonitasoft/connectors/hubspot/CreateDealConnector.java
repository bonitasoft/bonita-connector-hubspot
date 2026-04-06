package com.bonitasoft.connectors.hubspot;

import lombok.extern.slf4j.Slf4j;

/**
 * [BETA] Creates a new deal in a HubSpot sales pipeline.
 * API: POST /crm/v3/objects/deals
 */
@Slf4j
public class CreateDealConnector extends AbstractHubSpotConnector {

    // === Input parameter name constants ===
    static final String INPUT_AUTH_MODE = "authMode";
    static final String INPUT_PRIVATE_APP_TOKEN = "privateAppToken";
    static final String INPUT_CLIENT_ID = "clientId";
    static final String INPUT_CLIENT_SECRET = "clientSecret";
    static final String INPUT_REFRESH_TOKEN = "refreshToken";
    static final String INPUT_BASE_PATH = "basePath";
    static final String INPUT_CONNECT_TIMEOUT = "connectTimeout";
    static final String INPUT_READ_TIMEOUT = "readTimeout";
    static final String INPUT_DEAL_NAME = "dealName";
    static final String INPUT_DEAL_STAGE = "dealStage";
    static final String INPUT_PIPELINE_ID = "pipelineId";
    static final String INPUT_AMOUNT = "amount";
    static final String INPUT_CLOSE_DATE = "closeDate";
    static final String INPUT_DEAL_OWNER_ID = "dealOwnerId";
    static final String INPUT_DEAL_TYPE = "dealType";
    static final String INPUT_DESCRIPTION = "description";
    static final String INPUT_CUSTOM_PROPERTIES = "customProperties";

    // === Output parameter name constants ===
    static final String OUTPUT_DEAL_ID = "dealId";
    static final String OUTPUT_DEAL_NAME = "dealName";
    static final String OUTPUT_DEAL_STAGE = "dealStage";
    static final String OUTPUT_CREATED_AT = "createdAt";
    static final String OUTPUT_UPDATED_AT = "updatedAt";
    static final String OUTPUT_DEAL_URL = "dealUrl";

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
                .dealName(readStringInput(INPUT_DEAL_NAME))
                .dealStage(readStringInput(INPUT_DEAL_STAGE))
                .pipelineId(readStringInput(INPUT_PIPELINE_ID))
                .amount(readStringInput(INPUT_AMOUNT))
                .closeDate(readStringInput(INPUT_CLOSE_DATE))
                .dealOwnerId(readStringInput(INPUT_DEAL_OWNER_ID))
                .dealType(readStringInput(INPUT_DEAL_TYPE))
                .description(readStringInput(INPUT_DESCRIPTION))
                .customProperties(readStringInput(INPUT_CUSTOM_PROPERTIES))
                .build();
    }

    @Override
    protected void validateConfiguration(HubSpotConfiguration config) {
        super.validateConfiguration(config);
        if (config.getDealName() == null || config.getDealName().isBlank()) {
            throw new IllegalArgumentException("dealName is mandatory");
        }
        if (config.getDealStage() == null || config.getDealStage().isBlank()) {
            throw new IllegalArgumentException("dealStage is mandatory");
        }
    }

    @Override
    protected void doExecute() throws HubSpotException {
        log.info("Executing Create Deal connector");

        HubSpotClient.CreateDealResult result = client.createDeal(
                configuration.getDealName(),
                configuration.getDealStage(),
                configuration.getPipelineId(),
                configuration.getAmount(),
                configuration.getCloseDate(),
                configuration.getDealOwnerId(),
                configuration.getDealType(),
                configuration.getDescription(),
                configuration.getCustomProperties()
        );

        setOutputParameter(OUTPUT_DEAL_ID, result.dealId());
        setOutputParameter(OUTPUT_DEAL_NAME, result.dealName());
        setOutputParameter(OUTPUT_DEAL_STAGE, result.dealStage());
        setOutputParameter(OUTPUT_CREATED_AT, result.createdAt());
        setOutputParameter(OUTPUT_UPDATED_AT, result.updatedAt());
        setOutputParameter(OUTPUT_DEAL_URL, result.dealUrl());

        log.info("Create Deal connector executed successfully — dealId={}", result.dealId());
    }
}
