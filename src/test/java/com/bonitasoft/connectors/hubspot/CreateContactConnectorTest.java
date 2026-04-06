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
class CreateContactConnectorTest {

    @Mock
    private HubSpotClient mockClient;

    private CreateContactConnector connector;

    @BeforeEach
    void setUp() {
        connector = new CreateContactConnector();
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("authMode", "PRIVATE_APP");
        inputs.put("privateAppToken", "pat-test-token-123");
        inputs.put("email", "john@acme.com");
        inputs.put("firstName", "John");
        inputs.put("lastName", "Doe");
        return inputs;
    }

    private void injectMockClient() throws Exception {
        var clientField = AbstractHubSpotConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }

    @Test
    void shouldCreateContactSuccessfully() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.createContact(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HubSpotClient.CreateContactResult(
                        "67890", "john@acme.com", "2026-04-06T10:00:00.000Z",
                        "2026-04-06T10:00:00.000Z", true,
                        "https://app.hubspot.com/contacts/contact/67890"));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(true);
        assertThat(connector.getOutputs().get("contactId")).isEqualTo("67890");
        assertThat(connector.getOutputs().get("email")).isEqualTo("john@acme.com");
        assertThat(connector.getOutputs().get("isNew")).isEqualTo(true);
    }

    @Test
    void shouldHandleExistingContact() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.createContact(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HubSpotClient.CreateContactResult(
                        "11111", "john@acme.com", null, null, false,
                        "https://app.hubspot.com/contacts/contact/11111"));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(true);
        assertThat(connector.getOutputs().get("contactId")).isEqualTo("11111");
        assertThat(connector.getOutputs().get("isNew")).isEqualTo(false);
    }

    @Test
    void shouldFailWhenEmailMissing() {
        var inputs = validInputs();
        inputs.remove("email");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldSetErrorOutputsOnFailure() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.createContact(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new HubSpotException("API error", 500, true));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(false);
    }
}
