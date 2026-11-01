package com.smartbus.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import com.smartbus.dto.NearbyAssistantResponse;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

class OllamaChatServiceTest {
    @Test
    void sendsOnlyQuestionAndRoleToLoopbackModel() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://127.0.0.1:11434");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://127.0.0.1:11434/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Explain gravity")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("USER")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("untrusted input")))
                .andRespond(withSuccess("""
                        {"message":{"role":"assistant","content":"Gravity attracts objects with mass."}}
                        """, MediaType.APPLICATION_JSON));

        OllamaChatService service = new OllamaChatService(builder.build(), "test-model");

        assertEquals("Gravity attracts objects with mass.", service.answer("Explain gravity", "USER"));
        server.verify();
    }

    @Test
    void rejectsNonLoopbackModelUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> new com.smartbus.config.OllamaClientConfiguration()
                        .ollamaRestClient(RestClient.builder(), "http://example.com:11434"));
    }

    @Test
    void includesUntrustedWebResultsForCurrentAnswers() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://127.0.0.1:11434");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://127.0.0.1:11434/api/chat"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Current web search results follow")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tamil Nadu government")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("https://example.org/tn")))
                .andRespond(withSuccess("""
                        {"message":{"role":"assistant","content":"See the current source [1]."}}
                        """, MediaType.APPLICATION_JSON));
        OllamaChatService service = new OllamaChatService(builder.build(), "test-model");
        var source = new OllamaWebSearchService.SearchResult(
                "Tamil Nadu government", "https://example.org/tn", "Chief Minister information.");

        assertEquals("See the current source [1].",
                service.answer("Who is the chief minister?", "GUEST", java.util.List.of(source)));
        server.verify();
    }

    @Test
    void answersWithoutLiveSearchAndClearlyLabelsTheUnverifiedResult() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://127.0.0.1:11434");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://127.0.0.1:11434/api/chat"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CM of tamil nadu")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Live web search was unavailable")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("potentially outdated")))
                .andRespond(withSuccess("""
                        {"message":{"role":"assistant","content":"I cannot verify the current officeholder."}}
                        """, MediaType.APPLICATION_JSON));
        OllamaChatService service = new OllamaChatService(builder.build(), "test-model");

        String answer = service.answerWithoutLiveSearch("CM of tamil nadu", "GUEST");

        assertTrue(answer.startsWith("I couldn't verify the latest information because live search is unavailable."));
        assertTrue(answer.contains("I cannot verify the current officeholder."));
        assertTrue(answer.endsWith("Please check a current source for time-sensitive facts."));
        server.verify();
    }

    @Test
    void reportsUnavailableModelAsServiceUnavailable() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://127.0.0.1:11434");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://127.0.0.1:11434/api/chat"))
                .andRespond(withServerError());
        OllamaChatService service = new OllamaChatService(builder.build(), "test-model");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.answer("Explain gravity", "GUEST"));

        assertEquals(503, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("test-model"));
        server.verify();
    }
}
