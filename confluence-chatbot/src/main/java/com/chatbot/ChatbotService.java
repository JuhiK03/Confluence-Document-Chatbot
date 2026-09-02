package com.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import okhttp3.*;

public class ChatbotService {

    private final EmbeddingService embeddingService;
    private final Scheduler scheduler;
    private final String openAIApiKey;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> pageIdToTitle = new HashMap<>(); // Cache for page titles

    public ChatbotService(
        Scheduler scheduler,
        EmbeddingService embeddingService
    ) {
        this.scheduler = scheduler;
        this.embeddingService = embeddingService;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();

        Dotenv dotenv = Dotenv.load();
        this.openAIApiKey = dotenv.get("OPENAI_API_KEY");

        if (this.openAIApiKey == null || this.openAIApiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "Environment variable 'OPENAI_API_KEY' is not set or is empty in .env file."
            );
        }
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public String askQuestion(String question) throws IOException {
        List<Double> questionVector = embeddingService.getEmbedding(question);

        if (questionVector == null || questionVector.isEmpty()) {
            return "Sorry, I couldn't generate an embedding for your question.";
        }

        // Retrieve relevant context from embeddings
        Map<String, List<Double>> memory = scheduler.getDocumentEmbeddings();

        // Handle case where memory might be empty
        if (memory == null || memory.isEmpty()) {
            return "Sorry, I don't have any document embeddings to refer to. Please ensure documents are processed and try again.";
        }

        // Get multiple relevant contexts instead of just one
        List<RelevantContext> relevantContexts = findRelevantContexts(
            questionVector,
            memory,
            5
        );

        if (relevantContexts.isEmpty()) {
            return "I couldn't find relevant information in the Confluence documents to answer your question. Please try rephrasing your question or ask about a different topic.";
        }

        // Combine contexts from multiple sources
        String combinedContext = buildCombinedContext(relevantContexts);

        // Call OpenAI with the question and combined context
        return callOpenAI(question, combinedContext, relevantContexts);
    }

    private List<RelevantContext> findRelevantContexts(
        List<Double> questionVector,
        Map<String, List<Double>> memory,
        int maxResults
    ) {
        List<RelevantContext> contexts = new ArrayList<>();

        for (Map.Entry<String, List<Double>> entry : memory.entrySet()) {
            double similarity = cosineSimilarity(
                questionVector,
                entry.getValue()
            );
            if (similarity > 0.1) {
                // Only include contexts with meaningful similarity
                String content = entry.getKey();
                String pageId = extractPageId(content);
                String pageTitle = extractPageTitle(content);
                contexts.add(
                    new RelevantContext(content, similarity, pageId, pageTitle)
                );
            }
        }

        // Sort by similarity (highest first) and take top results
        return contexts
            .stream()
            .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
            .limit(maxResults)
            .collect(Collectors.toList());
    }

    private String buildCombinedContext(List<RelevantContext> contexts) {
        StringBuilder combinedContext = new StringBuilder();
        Set<String> addedSources = new HashSet<>(); // Track sources to avoid duplicates

        for (RelevantContext context : contexts) {
            String sourceInfo = context.pageTitle != null
                ? context.pageTitle
                : "Page " + context.pageId;
            if (!addedSources.contains(sourceInfo)) {
                combinedContext
                    .append("\n--- From ")
                    .append(sourceInfo)
                    .append(" ---\n");
                addedSources.add(sourceInfo);
            }

            String cleanContent = context.content
                .replaceAll("\\[Page \\d+\\]\\s*", "")
                .trim();
            combinedContext.append(cleanContent).append("\n");
        }

        return combinedContext.toString();
    }

    private String extractPageId(String content) {
        // Extract page ID from content that starts with "[Page 123456]"
        if (content.startsWith("[Page ")) {
            int endIndex = content.indexOf("]");
            if (endIndex > 0) {
                return content.substring(6, endIndex);
            }
        }
        return "Unknown";
    }

    private String extractPageTitle(String content) {
        // For now, we'll use page ID as title. This could be enhanced to fetch actual titles
        String pageId = extractPageId(content);
        return pageIdToTitle.getOrDefault(pageId, "Page " + pageId);
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (
            a == null ||
            b == null ||
            a.isEmpty() ||
            b.isEmpty() ||
            a.size() != b.size()
        ) {
            return 0.0;
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            normA += Math.pow(a.get(i), 2);
            normB += Math.pow(b.get(i), 2);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String callOpenAI(
        String userQuestion,
        String context,
        List<RelevantContext> relevantContexts
    ) throws IOException {
        if (this.openAIApiKey == null || this.openAIApiKey.isEmpty()) {
            throw new IllegalStateException(
                "OPENAI_API_KEY is not available for OpenAI API call."
            );
        }

        // Create source references for the response
        StringBuilder sourceReferences = new StringBuilder();
        Set<String> uniqueSources = new HashSet<>();

        for (RelevantContext ctx : relevantContexts) {
            String source = ctx.pageTitle != null
                ? ctx.pageTitle
                : "Page " + ctx.pageId;
            uniqueSources.add(source);
        }

        if (!uniqueSources.isEmpty()) {
            sourceReferences.append("\n\nSources: ");
            sourceReferences.append(String.join(", ", uniqueSources));
        }

        // Enhanced prompt for comprehensive answers
        String systemPrompt =
            "You are a helpful assistant specialized in answering questions based on Confluence documentation. " +
            "Provide comprehensive, complete answers that include ALL relevant steps, procedures, or information from the provided context. " +
            "If the question asks for steps or procedures, list ALL steps in order. " +
            "Do not provide partial answers or stop at the first step. " +
            "If information spans multiple sources, synthesize it into a coherent, complete response. " +
            "Always strive to give the most complete and helpful answer possible.";

        String userPrompt =
            "Context from Confluence documents:\n" +
            context +
            "\n\nUser Question: " +
            userQuestion +
            "\n\nProvide a comprehensive answer based on the provided context. " +
            "If the question asks for steps or procedures, provide ALL steps in complete detail. " +
            "If the context contains incomplete information, clearly state what information is available and what might be missing.";

        Map<String, Object> body = Map.of(
            "model",
            "gpt-4o",
            "messages",
            List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "temperature",
            0.3, // Lower temperature for more consistent, focused responses
            "max_tokens",
            1500 // Increased token limit for comprehensive answers
        );

        String jsonBody = objectMapper.writeValueAsString(body);
        RequestBody requestBody = RequestBody.create(
            jsonBody,
            MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer " + openAIApiKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null
                    ? response.body().string()
                    : "No response body";
                System.err.println(
                    "OpenAI Chat API Error: " +
                    response.code() +
                    " - " +
                    errorBody
                );
                throw new IOException(
                    "OpenAI Chat API request failed: " +
                    response.code() +
                    " - " +
                    errorBody
                );
            }

            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode contentNode = root.at("/choices/0/message/content");
            if (contentNode.isTextual()) {
                String aiResponse = contentNode.asText();
                // Add source references to the response
                return aiResponse + sourceReferences.toString();
            } else {
                System.err.println(
                    "OpenAI Chat API response did not contain expected content: " +
                    root.toString()
                );
                return "Sorry, I received an unexpected response from the AI.";
            }
        }
    }

    // Helper class to store relevant context with metadata
    private static class RelevantContext {

        final String content;
        final double similarity;
        final String pageId;
        final String pageTitle;

        RelevantContext(
            String content,
            double similarity,
            String pageId,
            String pageTitle
        ) {
            this.content = content;
            this.similarity = similarity;
            this.pageId = pageId;
            this.pageTitle = pageTitle;
        }
    }
}
