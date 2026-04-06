package com.bonitasoft.connectors.hubspot;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class CreateDealConnectorTest {

    @Mock
    private HubSpotClient mockClient;

    private CreateDealConnector connector;

    @BeforeEach
    void setUp() {
        connector = new CreateDealConnector();
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("authMode", "PRIVATE_APP");
        inputs.put("privateAppToken", "pat-test-token-123");
        inputs.put("dealName", "ACME Corp - Enterprise License");
        inputs.put("dealStage", "appointmentscheduled");
        return inputs;
    }

    private void injectMockClient() throws Exception {
        var clientField = AbstractHubSpotConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }

    @Test
    void shouldCreateDealSuccessfully() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.createDeal(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HubSpotClient.CreateDealResult(
                        "12345", "ACME Corp - Enterprise License", "appointmentscheduled",
                        "2026-04-06T10:00:00.000Z", "2026-04-06T10:00:00.000Z",
                        "https://app.hubspot.com/contacts/deal/12345"));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(true);
        assertThat(connector.getOutputs().get("dealId")).isEqualTo("12345");
        assertThat(connector.getOutputs().get("dealName")).isEqualTo("ACME Corp - Enterprise License");
        assertThat(connector.getOutputs().get("dealStage")).isEqualTo("appointmentscheduled");
        assertThat(connector.getOutputs().get("dealUrl")).isEqualTo("https://app.hubspot.com/contacts/deal/12345");
    }

    @Test
    void shouldFailWhenDealNameMissing() {
        var inputs = validInputs();
        inputs.remove("dealName");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldFailWhenDealStageMissing() {
        var inputs = validInputs();
        inputs.remove("dealStage");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldFailWhenPrivateAppTokenMissing() {
        var inputs = validInputs();
        inputs.remove("privateAppToken");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.createDeal(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new HubSpotException("HubSpot API error 400: INVALID_OPTION", 400, false));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(false);
        assertThat((String) connector.getOutputs().get("errorMessage")).contains("INVALID_OPTION");
    }

    @Test
    void shouldApplyDefaultsForOptionalInputs() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();

        var configField = AbstractHubSpotConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        var config = (HubSpotConfiguration) configField.get(connector);

        assertThat(config.getAuthMode()).isEqualTo("PRIVATE_APP");
        assertThat(config.getBasePath()).isEqualTo("https://api.hubapi.com");
        assertThat(config.getConnectTimeout()).isEqualTo(30000);
        assertThat(config.getReadTimeout()).isEqualTo(60000);
    }

    @Test
    void shouldPassAllOptionalParameters() throws Exception {
        var inputs = validInputs();
        inputs.put("pipelineId", "sales-pipeline");
        inputs.put("amount", "50000");
        inputs.put("closeDate", "2026-06-30T00:00:00.000Z");
        inputs.put("dealOwnerId", "98765");
        inputs.put("dealType", "newbusiness");
        inputs.put("description", "Enterprise license deal");
        inputs.put("customProperties", "{\"hs_priority\":\"high\"}");

        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.createDeal(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HubSpotClient.CreateDealResult("12345", "ACME Corp - Enterprise License",
                        "appointmentscheduled", "2026-04-06T10:00:00.000Z", "2026-04-06T10:00:00.000Z",
                        "https://app.hubspot.com/contacts/deal/12345"));

        connector.executeBusinessLogic();

        verify(mockClient).createDeal(
                eq("ACME Corp - Enterprise License"),
                eq("appointmentscheduled"),
                eq("sales-pipeline"),
                eq("50000"),
                eq("2026-06-30T00:00:00.000Z"),
                eq("98765"),
                eq("newbusiness"),
                eq("Enterprise license deal"),
                eq("{\"hs_priority\":\"high\"}")
        );
    }
}
