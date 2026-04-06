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
class SearchContactsConnectorTest {

    @Mock
    private HubSpotClient mockClient;

    private SearchContactsConnector connector;

    @BeforeEach
    void setUp() {
        connector = new SearchContactsConnector();
    }

    private Map<String, Object> validInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("authMode", "PRIVATE_APP");
        inputs.put("privateAppToken", "pat-test-token-123");
        inputs.put("searchFilters", "[{\"propertyName\":\"email\",\"operator\":\"EQ\",\"value\":\"john@acme.com\"}]");
        return inputs;
    }

    private void injectMockClient() throws Exception {
        var clientField = AbstractHubSpotConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }

    @Test
    void shouldSearchContactsSuccessfully() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.searchContacts(any(), any(), anyInt(), any(), any(), any()))
                .thenReturn(new HubSpotClient.SearchContactsResult(
                        "[{\"id\":\"67890\",\"properties\":{\"email\":\"john@acme.com\"}}]",
                        1, false, "", "67890", "john@acme.com"));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(true);
        assertThat(connector.getOutputs().get("totalResults")).isEqualTo(1);
        assertThat(connector.getOutputs().get("hasMore")).isEqualTo(false);
        assertThat(connector.getOutputs().get("firstContactId")).isEqualTo("67890");
        assertThat(connector.getOutputs().get("firstContactEmail")).isEqualTo("john@acme.com");
    }

    @Test
    void shouldHandleEmptyResults() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.searchContacts(any(), any(), anyInt(), any(), any(), any()))
                .thenReturn(new HubSpotClient.SearchContactsResult("[]", 0, false, "", "", ""));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("success")).isEqualTo(true);
        assertThat(connector.getOutputs().get("totalResults")).isEqualTo(0);
        assertThat(connector.getOutputs().get("firstContactId")).isEqualTo("");
    }

    @Test
    void shouldFailWhenSearchFiltersMissing() {
        var inputs = validInputs();
        inputs.remove("searchFilters");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldApplyDefaultLimit() throws Exception {
        connector.setInputParameters(validInputs());
        connector.validateInputParameters();

        var configField = AbstractHubSpotConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        var config = (HubSpotConfiguration) configField.get(connector);

        assertThat(config.getLimit()).isEqualTo(10);
        assertThat(config.getSortDirection()).isEqualTo("ASCENDING");
    }

    @Test
    void shouldHandlePagination() throws Exception {
        var inputs = validInputs();
        inputs.put("limit", 5);
        inputs.put("after", "abc123");
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        injectMockClient();

        when(mockClient.searchContacts(any(), any(), eq(5), eq("abc123"), any(), any()))
                .thenReturn(new HubSpotClient.SearchContactsResult(
                        "[{\"id\":\"1\"},{\"id\":\"2\"}]", 10, true, "def456", "1", "a@b.com"));

        connector.executeBusinessLogic();

        assertThat(connector.getOutputs().get("hasMore")).isEqualTo(true);
        assertThat(connector.getOutputs().get("nextAfter")).isEqualTo("def456");
    }
}
