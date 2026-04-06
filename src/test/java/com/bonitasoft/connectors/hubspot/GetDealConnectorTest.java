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
class GetDealConnectorTest {

    @Mock
    private HubSpotClient mockClient;

    private GetDealConnector connector;

    @BeforeEach
    void setUp() {
        connector = new GetDealConnector();
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("authMode", "PRIVATE_APP");
        inputs.put("privateAppToken", "pat-test-token-123");
        inputs.put("dealId", "12345");
        return inputs;
    }

    private void injectMockClient() throws Exception {
        var clientField = AbstractHubSpotConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }

    @Test
    void shouldGetDealSuccessfully() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.getDeal(any(), any()))
                .thenReturn(new HubSpotClient.GetDealResult(
                        "12345", "ACME Deal", "contractsent", "default",
                        "50000", "2026-06-30T00:00:00.000Z", "98765",
                        "2026-04-01T10:00:00.000Z", "2026-04-06T12:00:00.000Z",
                        "{\"dealname\":\"ACME Deal\",\"dealstage\":\"contractsent\"}"));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(true);
        assertThat(connector.getOutputs().get("dealId")).isEqualTo("12345");
        assertThat(connector.getOutputs().get("dealName")).isEqualTo("ACME Deal");
        assertThat(connector.getOutputs().get("dealStage")).isEqualTo("contractsent");
        assertThat(connector.getOutputs().get("amount")).isEqualTo("50000");
        assertThat(connector.getOutputs().get("allProperties")).isNotNull();
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
    void shouldPassPropertiesListToClient() throws Exception {
        var inputs = validInputs();
        inputs.put("properties", "dealname,dealstage,amount");
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.getDeal(any(), any()))
                .thenReturn(new HubSpotClient.GetDealResult(
                        "12345", "ACME Deal", "contractsent", null,
                        "50000", null, null,
                        "2026-04-01T10:00:00.000Z", "2026-04-06T12:00:00.000Z", "{}"));

        connector.executeBusinessLogic();

        verify(mockClient).getDeal("12345", "dealname,dealstage,amount");
    }
}
