package itmo.blps.bitrix.jca.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class BitrixRestClient {

    private final String restBaseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BitrixRestClient(String restBaseUrl) {
        String base = restBaseUrl == null ? "" : restBaseUrl.trim();
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        this.restBaseUrl = base;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public JsonNode call(String method, Map<String, Object> params) throws IOException, InterruptedException {
        String url = restBaseUrl + method + ".json";
        String body = objectMapper.writeValueAsString(params != null ? params : Map.of());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Bitrix REST HTTP " + response.statusCode() + ": " + response.body());
        }
        JsonNode root = objectMapper.readTree(response.body());
        if (root.has("error")) {
            throw new IOException("Bitrix REST error: " + root.get("error").asText()
                    + " — " + (root.has("error_description") ? root.get("error_description").asText() : ""));
        }
        return root;
    }

    public JsonNode callGet(String method, Map<String, String> queryParams) throws IOException, InterruptedException {
        StringBuilder url = new StringBuilder(restBaseUrl).append(method).append(".json");
        if (queryParams != null && !queryParams.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (Map.Entry<String, String> e : queryParams.entrySet()) {
                if (!first) {
                    url.append("&");
                }
                url.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                        .append("=")
                        .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
                first = false;
            }
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Bitrix REST HTTP " + response.statusCode() + ": " + response.body());
        }
        JsonNode root = objectMapper.readTree(response.body());
        if (root.has("error")) {
            throw new IOException("Bitrix REST error: " + root.get("error").asText());
        }
        return root;
    }

    public static Map<String, Object> fields(Map<String, Object> entries) {
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("fields", entries);
        return wrapper;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
