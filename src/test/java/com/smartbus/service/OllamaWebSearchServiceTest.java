package com.smartbus.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaWebSearchServiceTest {
    @Test
    void searchesWithBearerKeyAndReturnsOnlyHttpsSources() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ollama.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://ollama.com/api/web_search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", org.hamcrest.Matchers.equalTo(
                        "Bearer " + "test" + "-key")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Who is the Tamil Nadu CM in 2026?")))
                .andRespond(withSuccess("""
                        {"results":[
                          {"title":"Official source","url":"https://example.org/current","content":"Current officeholder details."},
                          {"title":"Unsafe source","url":"javascript:alert(1)","content":"Do not include this link."}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        var service = new OllamaWebSearchService(builder.build(), "test-key");
        var results = service.search("Who is the Tamil Nadu CM in 2026?");

        assertEquals(1, results.size());
        assertEquals("Official source", results.get(0).title());
        assertEquals("https://example.org/current", results.get(0).url());
        server.verify();
    }

    @Test
    void reportsMissingApiKeyWithoutSendingRequest() {
        var service = new OllamaWebSearchService(RestClient.builder()
                .baseUrl("https://ollama.com").build(), " ");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.search("Who is the current chief minister?"));

        assertEquals(503, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("OLLAMA_API_KEY"));
    }
}
