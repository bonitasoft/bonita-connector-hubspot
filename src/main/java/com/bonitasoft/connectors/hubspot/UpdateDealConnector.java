package com.bonitasoft.connectors.hubspot;

import lombok.extern.slf4j.Slf4j;

/**
 * [BETA] Updates properties of an existing HubSpot deal.
 * API: PATCH /crm/v3/objects/deals/{dealId}
 */
@Slf4j
public class UpdateDealConnector extends AbstractHubSpotConnector {

    static final String INPUT_AUTH_MODE = "authMode";
    static final String INPUT_PRIVATE_APP_TOKEN = "privateAppToken";
    static final String INPUT_CLIENT_ID = "clientId";
    static final String INPUT_CLIENT_SECRET = "clientSecret";
    static final String INPUT_REFRESH_TOKEN = "refreshToken";
    static final String INPUT_BASE_PATH = "basePath";
    static final String INPUT_CONNECT_TIMEOUT = "connectTimeout";
    static final String INPUT_READ_TIMEOUT = "readTimeout";
    static final String INPUT_DEAL_ID = "dealId";
    static final String INPUT_DEAL_STAGE = "dealStage";
    static final String INPUT_AMOUNT = "amount";
    static final String INPUT_CLOSE_DATE = "closeDate";
    static final String INPUT_DEAL_OWNER_ID = "dealOwnerId";
    static final String INPUT_CUSTOM_PROPERTIES = "customProperties";

    static final String OUTPUT_DEAL_ID = "dealId";
    static final String OUTPUT_DEAL_STAGE = "dealStage";
    static final String OUTPUT_UPDATED_AT = "updatedAt";

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
                .dealStage(readStringInput(INPUT_DEAL_STAGE))
                .amount(readStringInput(INPUT_AMOUNT))
                .closeDate(readStringInput(INPUT_CLOSE_DATE))
                .dealOwnerId(readStringInput(INPUT_DEAL_OWNER_ID))
                .customProperties(readStringInput(INPUT_CUSTOM_PROPERTIES))
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
        log.info("Executing Update Deal connector — dealId={}", configuration.getDealId());

        HubSpotClient.UpdateDealResult result = client.updateDeal(
                configuration.getDealId(),
                configuration.getDealStage(),
                configuration.getAmount(),
                configuration.getCloseDate(),
                configuration.getDealOwnerId(),
                configuration.getCustomProperties()
        );

        setOutputParameter(OUTPUT_DEAL_ID, result.dealId());
        setOutputParameter(OUTPUT_DEAL_STAGE, result.dealStage());
        setOutputParameter(OUTPUT_UPDATED_AT, result.updatedAt());

        log.info("Update Deal connector executed successfully");
    }
}
