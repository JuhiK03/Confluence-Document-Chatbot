package com.chatbot;

import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import okhttp3.*;
import okhttp3.Credentials;

public class ConfluenceService {

    private final OkHttpClient client; // Make client an instance variable
    private final ObjectMapper mapper; // Make mapper an instance variable
    private final String confluenceBaseUrl;
    private final String apiToken;
    private final String confluenceEmail; // For Basic Auth

    // Constructor to receive parameters from App.java
    public ConfluenceService(
        String confluenceBaseUrl,
        String apiToken,
        String confluenceEmail
    ) {
        // Clean up base URL for consistency
        this.confluenceBaseUrl = confluenceBaseUrl.endsWith("/")
            ? confluenceBaseUrl.substring(0, confluenceBaseUrl.length() - 1)
            : confluenceBaseUrl;
        this.apiToken = apiToken;
        this.confluenceEmail = confluenceEmail;
        this.client = new OkHttpClient(); // Initialize client once
        this.mapper = new ObjectMapper(); // Initialize mapper once

        // Basic validation for constructor arguments
        if (
            this.confluenceBaseUrl == null ||
            this.confluenceBaseUrl.trim().isEmpty()
        ) {
            throw new IllegalArgumentException(
                "Confluence Base URL must not be null or empty."
            );
        }
        if (this.apiToken == null || this.apiToken.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Confluence API Token must not be null or empty."
            );
        }
        // If you are using Basic Auth, email is required
        if (
            this.confluenceEmail == null ||
            this.confluenceEmail.trim().isEmpty()
        ) {
            throw new IllegalArgumentException(
                "Confluence Email is required for Basic Authentication."
            );
        }
    }

    public String fetchDocument(String pageId) throws IOException {
        String url =
            confluenceBaseUrl +
            "/wiki/rest/api/content/" +
            pageId +
            "?expand=body.storage";

        Request request = new Request.Builder()
            .url(url)
            .addHeader(
                "Authorization",
                Credentials.basic(confluenceEmail, apiToken)
            )
            .addHeader("Accept", "application/json")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null
                    ? response.body().string()
                    : "No response body";
                System.err.println(
                    "Confluence API Error fetching page " +
                    pageId +
                    ": " +
                    response.code() +
                    " - " +
                    errorBody
                );
                throw new IOException(
                    "Unexpected response code " +
                    response.code() +
                    " for URL: " +
                    url +
                    ". Body: " +
                    errorBody
                );
            }
            String responseBody = response.body().string();
            JsonNode root = mapper.readTree(responseBody);

            JsonNode contentNode = root.at("/body/storage/value");
            if (contentNode.isTextual()) {
                return contentNode.asText();
            } else {
                System.err.println(
                    "Confluence response did not contain expected content format for page " +
                    pageId +
                    ": " +
                    root.toString()
                );
                return "";
            }
        } catch (Exception e) {
            System.err.println(
                "Error fetching document from Confluence for page ID " +
                pageId +
                ": " +
                e.getMessage()
            );
            throw new IOException("Error during Confluence document fetch.", e);
        }
    }

    public String fetchPageTitle(String pageId) throws IOException {
        String url =
            confluenceBaseUrl +
            "/wiki/rest/api/content/" +
            pageId +
            "?expand=title";

        Request request = new Request.Builder()
            .url(url)
            .addHeader(
                "Authorization",
                Credentials.basic(confluenceEmail, apiToken)
            )
            .addHeader("Accept", "application/json")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null
                    ? response.body().string()
                    : "No response body";
                System.err.println(
                    "Confluence API Error fetching page title " +
                    pageId +
                    ": " +
                    response.code() +
                    " - " +
                    errorBody
                );
                return null; // Return null instead of throwing exception for title fetch
            }
            String responseBody = response.body().string();
            JsonNode root = mapper.readTree(responseBody);

            JsonNode titleNode = root.at("/title");
            if (titleNode.isTextual()) {
                return titleNode.asText();
            } else {
                System.err.println(
                    "Confluence response did not contain title for page " +
                    pageId
                );
                return null;
            }
        } catch (Exception e) {
            System.err.println(
                "Error fetching page title from Confluence for page ID " +
                pageId +
                ": " +
                e.getMessage()
            );
            return null; // Return null instead of throwing exception
        }
    }
}
