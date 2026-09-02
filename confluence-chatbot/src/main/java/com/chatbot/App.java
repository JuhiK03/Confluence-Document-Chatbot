package com.chatbot;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.Scanner;

public class App {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.load();
        String openAiApiKey = dotenv.get("OPENAI_API_KEY");
        String confluenceApiToken = dotenv.get("CONFLUENCE_TOKEN"); // Assuming this is correct from your .env
        String confluenceBaseUrl = dotenv.get("CONFLUENCE_URL");
        String confluenceEmail = dotenv.get("CONFLUENCE_EMAIL");

        // --- IMPORTANT: Add validation and error handling ---
        if (openAiApiKey == null || openAiApiKey.isEmpty()) {
            System.err.println(
                "Error: OPENAI_API_KEY not found in .env or environment variables. Please check your .env file and key name."
            );
            System.exit(1);
        }
        if (confluenceApiToken == null || confluenceApiToken.isEmpty()) {
            System.err.println(
                "Error: CONFLUENCE_TOKEN not found in .env or environment variables."
            ); // Corrected var name
            System.exit(1);
        }
        if (confluenceBaseUrl == null || confluenceBaseUrl.isEmpty()) {
            System.err.println(
                "Error: CONFLUENCE_URL not found in .env or environment variables."
            ); // Corrected var name
            System.exit(1);
        }
        if (confluenceEmail == null || confluenceEmail.isEmpty()) {
            // <-- ADD THIS VALIDATION
            System.err.println(
                "Error: CONFLUENCE_EMAIL not found in .env or environment variables. Required for Confluence Basic Auth."
            );
            System.exit(1);
        }

        // Initialize EmbeddingService (it needs the OpenAI API key)
        EmbeddingService embeddingService = new EmbeddingService(openAiApiKey); // Pass the API key to EmbeddingService

        // Initialize ConfluenceService (it needs base URL and API token)
        ConfluenceService confluenceService = new ConfluenceService(
            confluenceBaseUrl,
            confluenceApiToken,
            confluenceEmail
        );

        // Initialize Scheduler, passing the necessary services
        Scheduler scheduler = new Scheduler(
            confluenceService,
            embeddingService
        );
        scheduler.refreshData(); // Initial load of Confluence data and embeddings

        // Initialize ChatbotService, passing both scheduler and embeddingService
        ChatbotService chatbot = new ChatbotService(
            scheduler,
            embeddingService
        ); // <--- CORRECTED LINE

        // Check if running in API mode or CLI mode
        boolean startApiServer = args.length > 0 && args[0].equals("--api");

        if (startApiServer) {
            // Start REST API server
            int port = 8080; // Default port
            String portEnv = dotenv.get("SERVER_PORT");
            if (portEnv != null && !portEnv.isEmpty()) {
                try {
                    port = Integer.parseInt(portEnv);
                } catch (NumberFormatException e) {
                    System.err.println(
                        "Warning: Invalid SERVER_PORT value. Using default port 8080."
                    );
                }
            }

            System.out.println(
                "Starting AIO Tests Confluence Chatbot API server on port " +
                port
            );
            System.out.println("Access the API at: http://localhost:" + port);
            System.out.println(
                "Health check endpoint: http://localhost:" + port + "/health"
            );
            System.out.println(
                "Chat endpoint: http://localhost:" + port + "/chat"
            );

            RestApiController apiController = new RestApiController(chatbot);
            apiController.start(port);

            // Keep the main thread alive
            System.out.println("API server is running. Press Ctrl+C to stop.");
            System.out.println(
                "You can now open http://localhost:3000 in your browser to use the web interface."
            );
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                System.out.println("Server interrupted. Shutting down...");
            }
        } else {
            // Run CLI mode (backward compatibility)
            Scanner scanner = new Scanner(System.in);
            System.out.println(
                "Chatbot initialized. Type your questions (type 'exit' to quit)."
            );
            System.out.println(
                "To start the API server instead, run with --api flag."
            );
            while (true) {
                System.out.print("You: ");
                String q = scanner.nextLine();
                if (q.equalsIgnoreCase("exit")) {
                    System.out.println("Exiting chatbot. Goodbye!");
                    break;
                }
                String answer = chatbot.askQuestion(q);
                System.out.println("Bot: " + answer);
            }
            scanner.close(); // Close the scanner when done
        }
    }
}
