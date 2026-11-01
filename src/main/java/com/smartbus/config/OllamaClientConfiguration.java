package com.smartbus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

@Configuration
public class OllamaClientConfiguration {
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "::1");

    @Bean("ollamaRestClient")
    public RestClient ollamaRestClient(
            RestClient.Builder builder,
            @Value("${smartbus.assistant.ollama.base-url:http://127.0.0.1:11434}") String baseUrl) {
        validateLocalBaseUrl(baseUrl);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));
        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Bean("ollamaWebSearchRestClient")
    public RestClient ollamaWebSearchRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        return builder.clone()
                .baseUrl("https://ollama.com")
                .requestFactory(requestFactory)
                .build();
    }

    private void validateLocalBaseUrl(String baseUrl) {
        URI uri;
        try {
            uri = URI.create(baseUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Ollama base URL must be a valid loopback HTTP URL.", exception);
        }
        if (!"http".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || !LOOPBACK_HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Ollama must use a loopback HTTP URL so chatbot questions stay on this machine.");
        }
    }
}
