package com.x.order.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/** Reads a business's Telegram alert switches and hands alerts to the business service. */
@Component
public class TelegramAlertClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramAlertClient.class);
    private final RestClient client;

    public TelegramAlertClient(
            RestClient.Builder builder,
            @Value("${services.business.base-url:http://127.0.0.1:8086}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // The business service waits for Telegram before it answers.
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.client = builder.clone().requestFactory(requestFactory).baseUrl(baseUrl).build();
    }

    /** The business's alert switches, or a disconnected default when they cannot be read. */
    public AlertSettings settings(Long businessId) {
        try {
            JsonNode response = client.get()
                    .uri("/api/v1/businesses/{id}/telegram", businessId)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode data = response == null ? null : response.path("data");
            if (data == null || data.isMissingNode()) {
                return AlertSettings.DISCONNECTED;
            }
            return new AlertSettings(
                    data.path("connected").asBoolean(false),
                    data.path("orderAlerts").asBoolean(false),
                    data.path("lowStockAlerts").asBoolean(false),
                    data.path("lowStockThreshold").asInt(5));
        } catch (RestClientException exception) {
            log.warn("Telegram alert settings for business {} could not be read: {}", businessId, exception.getMessage());
            return AlertSettings.DISCONNECTED;
        }
    }

    public void send(Long businessId, String type, String text) {
        try {
            client.post()
                    .uri("/internal/businesses/{id}/telegram/notify", businessId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("type", type, "text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            log.warn("Telegram alert for business {} was not sent: {}", businessId, exception.getMessage());
        }
    }

    public record AlertSettings(boolean connected, boolean orderAlerts, boolean lowStockAlerts, int lowStockThreshold) {
        static final AlertSettings DISCONNECTED = new AlertSettings(false, false, false, 0);
    }
}
