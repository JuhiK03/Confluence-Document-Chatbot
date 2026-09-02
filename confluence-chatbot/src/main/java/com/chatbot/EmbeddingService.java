package com.chatbot;
import okhttp3.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.*;
import java.io.IOException;

public class EmbeddingService {
    private final OkHttpClient client; // Make client an instance variable
    private final ObjectMapper mapper; // Make mapper an instance variable
    private final String openAiApiKey; // Store the API key passed in constructor

    // Constructor to receive the API key from App.java
    public EmbeddingService(String openAiApiKey) {
        this.openAiApiKey = openAiApiKey;
        this.client = new OkHttpClient(); // Initialize client once
        this.mapper = new ObjectMapper(); // Initialize mapper once

        if (this.openAiApiKey == null || this.openAiApiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("OpenAI API key must not be null or empty for EmbeddingService.");
        }
    }

    public List<Double> getEmbedding(String text) throws IOException {
        // Use the instance variables
        String requestBody = mapper.writeValueAsString(Map.of(
            "input", text,
            "model", "text-embedding-3-small"
        ));

        Request request = new Request.Builder()
            .url("https://api.openai.com/v1/embeddings")
            // Use the stored API key
            .addHeader("Authorization", "Bearer " + openAiApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                System.err.println("OpenAI Embedding API Error: " + response.code() + " - " + errorBody + " for text: " + text.substring(0, Math.min(text.length(), 100)) + "...");
                // Decide if you want to throw an exception or return an empty list on API error
                throw new IOException("Failed to get embedding from OpenAI: " + response.code());
            }
            JsonNode root = mapper.readTree(response.body().string());
            // Added null check for the path just in case
            JsonNode embeddingNode = root.at("/data/0/embedding");
            if (embeddingNode.isArray()) {
                 return mapper.convertValue(embeddingNode, new TypeReference<List<Double>>() {});
            } else {
                System.err.println("Embedding response format unexpected or missing embedding: " + root.toString());
                return Collections.emptyList(); // Return empty list if embedding not found/malformed
            }
        } catch (Exception e) {
            System.err.println("Error during embedding API call for text: '" + text.substring(0, Math.min(text.length(), 100)) + "...' - " + e.getMessage());
            return Collections.emptyList(); // Return empty list on any other error
        }
    }
}