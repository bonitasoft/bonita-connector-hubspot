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
class UpdateContactConnectorTest {

    @Mock
    private HubSpotClient mockClient;

    private UpdateContactConnector connector;

    @BeforeEach
    void setUp() {
        connector = new UpdateContactConnector();
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("authMode", "PRIVATE_APP");
        inputs.put("privateAppToken", "pat-test-token-123");
        inputs.put("contactId", "67890");
        inputs.put("lifecycleStage", "customer");
        return inputs;
    }

    private void injectMockClient() throws Exception {
        var clientField = AbstractHubSpotConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }

    @Test
    void shouldUpdateContactSuccessfully() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.updateContact(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HubSpotClient.UpdateContactResult("67890", "2026-04-06T12:00:00.000Z"));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(true);
        assertThat(connector.getOutputs().get("contactId")).isEqualTo("67890");
        assertThat(connector.getOutputs().get("updatedAt")).isEqualTo("2026-04-06T12:00:00.000Z");
    }

    @Test
    void shouldFailWhenContactIdMissing() {
        var inputs = validInputs();
        inputs.remove("contactId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.updateContact(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new HubSpotException("Contact not found", 404, false));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(false);
        assertThat((String) connector.getOutputs().get("errorMessage")).contains("Contact not found");
    }
}
