package com.smartbus.service;

import tools.jackson.databind.JsonNode;
import com.smartbus.dto.NearbyAssistantResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OllamaWebSearchService {
    private static final int MAX_RESULTS = 5;
    private static final int MAX_CONTENT_LENGTH = 1200;

    private final RestClient restClient;
    private final String apiKey;

    public OllamaWebSearchService(
            @Qualifier("ollamaWebSearchRestClient") RestClient restClient,
            @Value("${smartbus.assistant.ollama.api-key:}") String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    public List<SearchResult> search(String query) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Live answers need an Ollama API key. Set the OLLAMA_API_KEY environment variable and restart SmartBus.");
        }

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/api/web_search")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("query", query, "max_results", MAX_RESULTS))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Live web search is unavailable. Neuronex cannot verify a current answer right now.",
                    exception);
        }

        List<SearchResult> results = new ArrayList<>();
        JsonNode nodes = response == null ? null : response.path("results");
        if (nodes != null && nodes.isArray()) {
            for (JsonNode node : nodes) {
                if (results.size() == MAX_RESULTS) {
                    break;
                }
                String title = boundedText(node.path("title").asText(""), 200);
                String url = node.path("url").asText("").trim();
                String content = boundedText(node.path("content").asText(""), MAX_CONTENT_LENGTH);
                if (!title.isBlank() && !content.isBlank() && isHttpsUrl(url)) {
                    results.add(new SearchResult(title, url, content));
                }
            }
        }

        if (results.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Live web search returned no usable sources. Please try rephrasing the question.");
        }
        return List.copyOf(results);
    }

    private String boundedText(String text, int maxLength) {
        String normalized = text == null ? "" : text.strip();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private boolean isHttpsUrl(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public record SearchResult(String title, String url, String content) {
        public NearbyAssistantResponse.Source source() {
            return new NearbyAssistantResponse.Source(title, url);
        }
    }
}
