package com.chatbot;

import static spark.Spark.*;

import com.chatbot.models.ChatRequest;
import com.chatbot.models.ChatResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RestApiController {

    private static final Logger logger = Logger.getLogger(
        RestApiController.class.getName()
    );
    private final ChatbotService chatbotService;
    private final CasualConversationHandler casualHandler;
    private final Gson gson;

    public RestApiController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
        this.casualHandler = new CasualConversationHandler();
        this.gson = new Gson();
    }

    public void setupRoutes() {
        // Enable CORS for all routes
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header(
                "Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS"
            );
            response.header(
                "Access-Control-Allow-Headers",
                "Content-Type, Authorization, X-Requested-With"
            );
            response.type("application/json");
        });

        // Handle preflight requests
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers(
                "Access-Control-Request-Headers"
            );
            if (accessControlRequestHeaders != null) {
                response.header(
                    "Access-Control-Allow-Headers",
                    accessControlRequestHeaders
                );
            }
            String accessControlRequestMethod = request.headers(
                "Access-Control-Request-Method"
            );
            if (accessControlRequestMethod != null) {
                response.header(
                    "Access-Control-Allow-Methods",
                    accessControlRequestMethod
                );
            }
            return "OK";
        });

        // Health check endpoint
        get("/health", (request, response) -> {
            response.status(200);
            return gson.toJson(
                new HealthResponse("OK", "Confluence Chatbot API is running")
            );
        });

        // Main chat endpoint
        post("/chat", (request, response) -> {
            try {
                String requestBody = request.body();
                if (requestBody == null || requestBody.trim().isEmpty()) {
                    response.status(400);
                    return gson.toJson(
                        ChatResponse.error("Request body is empty", null)
                    );
                }

                ChatRequest chatRequest;
                try {
                    chatRequest = gson.fromJson(requestBody, ChatRequest.class);
                } catch (JsonSyntaxException e) {
                    logger.log(
                        Level.WARNING,
                        "Invalid JSON in request: " + e.getMessage()
                    );
                    response.status(400);
                    return gson.toJson(
                        ChatResponse.error("Invalid JSON format", null)
                    );
                }

                if (
                    chatRequest.getMessage() == null ||
                    chatRequest.getMessage().trim().isEmpty()
                ) {
                    response.status(400);
                    return gson.toJson(
                        ChatResponse.error(
                            "Message is required",
                            chatRequest.getSessionId()
                        )
                    );
                }

                String userMessage = chatRequest.getMessage().trim();
                String sessionId = chatRequest.getSessionId();

                logger.info(
                    "Received chat request - Session: " +
                    sessionId +
                    ", Message: " +
                    userMessage
                );

                String botResponse;

                // Check if this is a casual conversation first
                if (casualHandler.canHandle(userMessage)) {
                    botResponse = casualHandler.handleMessage(userMessage);
                    logger.info("Handled as casual conversation");
                } else {
                    // Use the Confluence-based chatbot service
                    try {
                        botResponse = chatbotService.askQuestion(userMessage);
                        logger.info("Handled as Confluence question");
                    } catch (IOException e) {
                        logger.log(
                            Level.SEVERE,
                            "Error processing Confluence question: " +
                            e.getMessage(),
                            e
                        );
                        response.status(500);
                        return gson.toJson(
                            ChatResponse.error(
                                "Sorry, I encountered an error processing your question. Please try again.",
                                sessionId
                            )
                        );
                    } catch (Exception e) {
                        logger.log(
                            Level.SEVERE,
                            "Unexpected error processing question: " +
                            e.getMessage(),
                            e
                        );
                        response.status(500);
                        return gson.toJson(
                            ChatResponse.error(
                                "Sorry, I encountered an unexpected error. Please try again.",
                                sessionId
                            )
                        );
                    }
                }

                ChatResponse chatResponse = new ChatResponse(
                    botResponse,
                    sessionId
                );
                response.status(200);
                return gson.toJson(chatResponse);
            } catch (Exception e) {
                logger.log(
                    Level.SEVERE,
                    "Unexpected error in chat endpoint: " + e.getMessage(),
                    e
                );
                response.status(500);
                return gson.toJson(
                    ChatResponse.error("Internal server error", null)
                );
            }
        });

        // Get API info
        get("/api/info", (request, response) -> {
            response.status(200);
            return gson.toJson(
                new ApiInfo(
                    "AIO Tests Confluence Chatbot API",
                    "1.0",
                    "Chat API for Confluence document queries"
                )
            );
        });

        // Diagnostic endpoint for testing embeddings and server status
        get("/api/diagnostics", (request, response) -> {
            try {
                Map<String, Object> diagnostics = new HashMap<>();

                // Get embedding statistics
                Map<String, List<Double>> embeddings = chatbotService
                    .getScheduler()
                    .getDocumentEmbeddings();
                Map<String, String> pageTitles = chatbotService
                    .getScheduler()
                    .getPageTitles();

                diagnostics.put("embeddingCount", embeddings.size());
                diagnostics.put("pageCount", pageTitles.size());
                diagnostics.put(
                    "configuredPages",
                    chatbotService.getScheduler().getPageIds()
                );
                diagnostics.put("pageTitles", pageTitles);

                // Sample some embedding keys to verify content
                List<String> sampleKeys = embeddings
                    .keySet()
                    .stream()
                    .limit(5)
                    .map(key ->
                        key.length() > 150 ? key.substring(0, 150) + "..." : key
                    )
                    .collect(java.util.stream.Collectors.toList());
                diagnostics.put("sampleEmbeddingKeys", sampleKeys);

                diagnostics.put("serverStatus", "running");
                diagnostics.put(
                    "timestamp",
                    java.time.LocalDateTime.now().toString()
                );

                response.status(200);
                return gson.toJson(diagnostics);
            } catch (Exception e) {
                logger.log(
                    Level.SEVERE,
                    "Error in diagnostics endpoint: " + e.getMessage(),
                    e
                );
                response.status(500);
                return gson.toJson(
                    ChatResponse.error(
                        "Error generating diagnostics: " + e.getMessage(),
                        null
                    )
                );
            }
        });

        // Exception handling
        exception(Exception.class, (exception, request, response) -> {
            logger.log(
                Level.SEVERE,
                "Unhandled exception: " + exception.getMessage(),
                exception
            );
            response.status(500);
            response.body(
                gson.toJson(ChatResponse.error("Internal server error", null))
            );
        });

        // 404 handler
        notFound((request, response) -> {
            response.status(404);
            response.type("application/json");
            return gson.toJson(
                new ErrorResponse(
                    "Not Found",
                    "The requested endpoint was not found"
                )
            );
        });
    }

    public void start(int port) {
        port(port);
        setupRoutes();
        logger.info("REST API server started on port " + port);

        // Add graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    logger.info("Shutting down REST API server...");
                    stop();
                })
            );
    }

    // Helper classes for responses
    private static class HealthResponse {

        private final String status;
        private final String message;

        public HealthResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

    private static class ApiInfo {

        private final String name;
        private final String version;
        private final String description;

        public ApiInfo(String name, String version, String description) {
            this.name = name;
            this.version = version;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getDescription() {
            return description;
        }
    }

    private static class ErrorResponse {

        private final String error;
        private final String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }
    }
}
