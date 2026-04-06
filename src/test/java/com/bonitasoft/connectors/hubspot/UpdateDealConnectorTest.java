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
class UpdateDealConnectorTest {

    @Mock
    private HubSpotClient mockClient;

    private UpdateDealConnector connector;

    @BeforeEach
    void setUp() {
        connector = new UpdateDealConnector();
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("authMode", "PRIVATE_APP");
        inputs.put("privateAppToken", "pat-test-token-123");
        inputs.put("dealId", "12345");
        inputs.put("dealStage", "closedwon");
        return inputs;
    }

    private void injectMockClient() throws Exception {
        var clientField = AbstractHubSpotConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }

    @Test
    void shouldUpdateDealSuccessfully() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.updateDeal(any(), any(), any(), any(), any(), any()))
                .thenReturn(new HubSpotClient.UpdateDealResult("12345", "closedwon", "2026-04-06T12:00:00.000Z"));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(true);
        assertThat(connector.getOutputs().get("dealId")).isEqualTo("12345");
        assertThat(connector.getOutputs().get("dealStage")).isEqualTo("closedwon");
    }

    @Test
    void shouldFailWhenDealIdMissing() {
        var inputs = validInputs();
        inputs.remove("dealId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.updateDeal(any(), any(), any(), any(), any(), any()))
                .thenThrow(new HubSpotException("Deal not found", 404, false));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(false);
        assertThat((String) connector.getOutputs().get("errorMessage")).contains("Deal not found");
    }
}
