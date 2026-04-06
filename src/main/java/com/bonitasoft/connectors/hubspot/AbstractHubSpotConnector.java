package com.bonitasoft.connectors.hubspot;

import lombok.extern.slf4j.Slf4j;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

import java.util.Map;

/**
 * Abstract base connector for HubSpot CRM operations.
 * Handles lifecycle: validate -> connect -> execute -> disconnect.
 * Subclasses implement buildConfiguration() and doExecute().
 */
@Slf4j
public abstract class AbstractHubSpotConnector extends AbstractConnector {

    // Output parameter constants
    protected static final String OUTPUT_SUCCESS = "success";
    protected static final String OUTPUT_ERROR_MESSAGE = "errorMessage";

    protected HubSpotConfiguration configuration;
    protected HubSpotClient client;

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        try {
            this.configuration = buildConfiguration();
            validateConfiguration(this.configuration);
        } catch (IllegalArgumentException e) {
            throw new ConnectorValidationException(this, e.getMessage());
        }
    }

    @Override
    public void connect() throws ConnectorException {
        try {
            this.client = new HubSpotClient(this.configuration);
            log.info("HubSpot connector connected successfully");
        } catch (HubSpotException e) {
            throw new ConnectorException("Failed to connect: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() throws ConnectorException {
        this.client = null;
    }

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        try {
            doExecute();
            setOutputParameter(OUTPUT_SUCCESS, true);
        } catch (HubSpotException e) {
            log.error("HubSpot connector execution failed: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in HubSpot connector: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, "Unexpected error: " + e.getMessage());
        }
    }

    protected abstract void doExecute() throws HubSpotException;

    protected abstract HubSpotConfiguration buildConfiguration();

    /**
     * Validates the configuration. Checks auth mode requirements.
     * Subclasses call super and add operation-specific validation.
     */
    protected void validateConfiguration(HubSpotConfiguration config) {
        String authMode = config.getAuthMode();
        if ("PRIVATE_APP".equals(authMode)) {
            String token = resolvePrivateAppToken(config);
            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException(
                        "privateAppToken is mandatory when authMode=PRIVATE_APP. "
                                + "Set it directly, via JVM property hubspot.privateAppToken, "
                                + "or env var HUBSPOT_PRIVATE_APP_TOKEN");
            }
        } else if ("OAUTH2".equals(authMode)) {
            String cid = resolveValue(config.getClientId(), "hubspot.clientId", "HUBSPOT_CLIENT_ID");
            String csec = resolveValue(config.getClientSecret(), "hubspot.clientSecret", "HUBSPOT_CLIENT_SECRET");
            String rt = resolveValue(config.getRefreshToken(), "hubspot.refreshToken", "HUBSPOT_REFRESH_TOKEN");
            if (cid == null || cid.isBlank()) {
                throw new IllegalArgumentException("clientId is mandatory when authMode=OAUTH2");
            }
            if (csec == null || csec.isBlank()) {
                throw new IllegalArgumentException("clientSecret is mandatory when authMode=OAUTH2");
            }
            if (rt == null || rt.isBlank()) {
                throw new IllegalArgumentException("refreshToken is mandatory when authMode=OAUTH2");
            }
        } else if (authMode != null && !authMode.isBlank()) {
            throw new IllegalArgumentException("Invalid authMode: " + authMode + ". Must be PRIVATE_APP or OAUTH2");
        }
    }

    /** Resolves private app token from config, JVM prop, or env var. */
    protected String resolvePrivateAppToken(HubSpotConfiguration config) {
        return resolveValue(config.getPrivateAppToken(), "hubspot.privateAppToken", "HUBSPOT_PRIVATE_APP_TOKEN");
    }

    /** Resolves a value from direct input, JVM system property, or environment variable. */
    protected String resolveValue(String directValue, String sysProp, String envVar) {
        if (directValue != null && !directValue.isBlank()) {
            return directValue;
        }
        String sysValue = System.getProperty(sysProp);
        if (sysValue != null && !sysValue.isBlank()) {
            return sysValue;
        }
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return null;
    }

    /** Helper: read a String input, returning null if not set. */
    protected String readStringInput(String name) {
        Object value = getInputParameter(name);
        return value != null ? value.toString() : null;
    }

    /** Helper: read a String input with a default value. */
    protected String readStringInput(String name, String defaultValue) {
        String value = readStringInput(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    /** Helper: read a Boolean input with a default value. */
    protected Boolean readBooleanInput(String name, boolean defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? (Boolean) value : defaultValue;
    }

    /** Helper: read an Integer input with a default value. */
    protected Integer readIntegerInput(String name, int defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? ((Number) value).intValue() : defaultValue;
    }

    /**
     * Exposes output parameters for testing. Package-visible.
     */
    Map<String, Object> getOutputs() {
        return getOutputParameters();
    }
}
