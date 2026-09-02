package com.chatbot;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;

@Configuration
@EnableScheduling
public class Scheduler {

    private final ConfluenceService confluenceService;
    private final EmbeddingService embeddingService;
    private final Map<String, List<Double>> vectorStore = new HashMap<>();
    private final Map<String, String> pageIdToTitle = new HashMap<>(); // Cache for page titles
    private final List<String> confluencePageIds;

    public Scheduler(
        ConfluenceService confluenceService,
        EmbeddingService embeddingService
    ) {
        this.confluenceService = confluenceService;
        this.embeddingService = embeddingService;
        this.confluencePageIds = new ArrayList<>();

        // Initialize with page IDs from environment variables
        Dotenv dotenv = Dotenv.load();
        String pageIdsEnv = dotenv.get("CONFLUENCE_PAGE_IDS");
        if (pageIdsEnv != null && !pageIdsEnv.trim().isEmpty()) {
            String[] pageIds = pageIdsEnv.split(",");
            for (String pageId : pageIds) {
                String trimmedId = pageId.trim();
                if (!trimmedId.isEmpty()) {
                    this.confluencePageIds.add(trimmedId);
                }
            }
        } else {
            // Default page ID if none configured
            this.confluencePageIds.add("6258689");
        }

        System.out.println(
            "Scheduler initialized with " +
            this.confluencePageIds.size() +
            " page IDs: " +
            this.confluencePageIds
        );
    }

    @Scheduled(fixedRate = 1000 * 60 * 10) // Runs every 10 minutes
    public void refreshData() throws IOException {
        System.out.println(
            "Scheduler: Starting data refresh for " +
            confluencePageIds.size() +
            " Confluence pages..."
        );

        try {
            // Clear existing data before adding new
            vectorStore.clear();
            pageIdToTitle.clear();

            int totalParagraphs = 0;
            int processedPages = 0;

            // Process each configured page
            for (String pageId : confluencePageIds) {
                System.out.println("Scheduler: Processing page ID: " + pageId);

                try {
                    // First, get page title
                    String pageTitle = confluenceService.fetchPageTitle(pageId);
                    if (pageTitle != null && !pageTitle.trim().isEmpty()) {
                        pageIdToTitle.put(pageId, pageTitle);
                        System.out.println(
                            "Scheduler: Page " + pageId + " title: " + pageTitle
                        );
                    } else {
                        pageIdToTitle.put(pageId, "Page " + pageId);
                        System.out.println(
                            "Scheduler: Could not fetch title for page " +
                            pageId
                        );
                    }

                    // Then get page content
                    String content = confluenceService.fetchDocument(pageId);

                    if (content == null || content.trim().isEmpty()) {
                        System.err.println(
                            "Scheduler: Fetched content is empty for page " +
                            pageId +
                            " (" +
                            pageIdToTitle.get(pageId) +
                            "). Skipping."
                        );
                        continue;
                    }

                    // Process content into embeddings
                    int pageEmbeddings = processPageContent(pageId, content);

                    System.out.println(
                        "Scheduler: Generated " +
                        pageEmbeddings +
                        " embeddings for page " +
                        pageId +
                        " (" +
                        pageIdToTitle.get(pageId) +
                        ")"
                    );

                    totalParagraphs += pageEmbeddings;
                    processedPages++;
                } catch (Exception e) {
                    System.err.println(
                        "Scheduler: Error processing page " +
                        pageId +
                        ": " +
                        e.getMessage()
                    );
                    e.printStackTrace();
                    // Continue processing other pages even if one fails
                }
            }

            System.out.println(
                "Scheduler: Data refresh complete. Successfully processed " +
                processedPages +
                "/" +
                confluencePageIds.size() +
                " pages. " +
                "Stored " +
                totalParagraphs +
                " document embeddings total."
            );
        } catch (Exception e) {
            System.err.println(
                "Scheduler: Critical error during data refresh: " +
                e.getMessage()
            );
            e.printStackTrace();
            throw e;
        }
    }

    private int processPageContent(String pageId, String content)
        throws IOException {
        int embeddingCount = 0;
        String pageTitle = pageIdToTitle.get(pageId);

        // Split content into meaningful chunks
        List<String> chunks = splitIntoChunks(content);

        System.out.println(
            "Scheduler: Processing " +
            chunks.size() +
            " chunks for page " +
            pageId
        );

        for (String chunk : chunks) {
            String cleanChunk = cleanHtmlTags(chunk);

            // Skip very short or meaningless chunks
            if (cleanChunk.length() < 20) {
                continue;
            }

            try {
                List<Double> embedding = embeddingService.getEmbedding(
                    cleanChunk
                );

                if (embedding != null && !embedding.isEmpty()) {
                    // Create a contextual key with page title and content
                    String contextKey =
                        "[" +
                        pageTitle +
                        " - Page " +
                        pageId +
                        "] " +
                        cleanChunk;
                    vectorStore.put(contextKey, embedding);
                    embeddingCount++;
                } else {
                    System.err.println(
                        "Scheduler: Could not generate embedding for chunk from page " +
                        pageId +
                        ": " +
                        cleanChunk.substring(
                            0,
                            Math.min(cleanChunk.length(), 100)
                        ) +
                        "..."
                    );
                }
            } catch (Exception e) {
                System.err.println(
                    "Scheduler: Error generating embedding for chunk in page " +
                    pageId +
                    ": " +
                    e.getMessage()
                );
            }
        }

        return embeddingCount;
    }

    private List<String> splitIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();

        // First, split by major HTML elements
        String[] majorSections = content.split(
            "(?i)(<h[1-6][^>]*>|<div[^>]*>|<section[^>]*>|<article[^>]*>)"
        );

        for (String section : majorSections) {
            // Further split by paragraphs
            String[] paragraphs = section.split(
                "(?i)(<p[^>]*>|<br[^>]*>|\\n\\s*\\n)"
            );

            for (String paragraph : paragraphs) {
                String cleaned = cleanHtmlTags(paragraph.trim());

                if (cleaned.length() > 20) {
                    // If paragraph is too long, split it into smaller chunks
                    if (cleaned.length() > 1000) {
                        chunks.addAll(splitLongText(cleaned, 800));
                    } else {
                        chunks.add(cleaned);
                    }
                }
            }
        }

        // If we didn't get good chunks, fall back to simple splitting
        if (chunks.isEmpty()) {
            String cleanContent = cleanHtmlTags(content);
            if (cleanContent.length() > 50) {
                chunks.addAll(splitLongText(cleanContent, 800));
            }
        }

        return chunks;
    }

    private List<String> splitLongText(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();

        // Split by sentences first
        String[] sentences = text.split("(?<=[.!?])\\s+");

        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() <= maxLength) {
                if (currentChunk.length() > 0) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
            } else {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }

                // If single sentence is too long, split it
                if (sentence.length() > maxLength) {
                    String[] words = sentence.split("\\s+");
                    StringBuilder sentenceChunk = new StringBuilder();

                    for (String word : words) {
                        if (
                            sentenceChunk.length() + word.length() <= maxLength
                        ) {
                            if (sentenceChunk.length() > 0) {
                                sentenceChunk.append(" ");
                            }
                            sentenceChunk.append(word);
                        } else {
                            if (sentenceChunk.length() > 0) {
                                chunks.add(sentenceChunk.toString());
                                sentenceChunk = new StringBuilder();
                            }
                            sentenceChunk.append(word);
                        }
                    }

                    if (sentenceChunk.length() > 0) {
                        currentChunk.append(sentenceChunk.toString());
                    }
                } else {
                    currentChunk.append(sentence);
                }
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    private String cleanHtmlTags(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }

        // Remove HTML tags but preserve content
        String cleaned = html.replaceAll("<[^>]+>", " ");

        // Clean up HTML entities
        cleaned = cleaned.replace("&nbsp;", " ");
        cleaned = cleaned.replace("&amp;", "&");
        cleaned = cleaned.replace("&lt;", "<");
        cleaned = cleaned.replace("&gt;", ">");
        cleaned = cleaned.replace("&quot;", "\"");
        cleaned = cleaned.replace("&apos;", "'");
        cleaned = cleaned.replace("&#39;", "'");

        // Replace multiple whitespace with single space
        cleaned = cleaned.replaceAll("\\s+", " ");

        return cleaned.trim();
    }

    // Method to get document embeddings (used by ChatbotService)
    public Map<String, List<Double>> getDocumentEmbeddings() {
        return new HashMap<>(vectorStore); // Return copy for thread safety
    }

    // Method to get page titles (used by ChatbotService)
    public Map<String, String> getPageTitles() {
        return new HashMap<>(pageIdToTitle); // Return copy for thread safety
    }

    // Method to add page ID
    public void addPageId(String pageId) {
        if (
            pageId != null &&
            !pageId.trim().isEmpty() &&
            !confluencePageIds.contains(pageId.trim())
        ) {
            confluencePageIds.add(pageId.trim());
            System.out.println("Scheduler: Added page ID: " + pageId);
        }
    }

    // Method to remove page ID
    public void removePageId(String pageId) {
        if (confluencePageIds.remove(pageId)) {
            pageIdToTitle.remove(pageId);
            System.out.println("Scheduler: Removed page ID: " + pageId);
        }
    }

    // Method to get current page IDs
    public List<String> getPageIds() {
        return new ArrayList<>(confluencePageIds);
    }

    // Method to get embedding statistics
    public Map<String, Integer> getEmbeddingStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalEmbeddings", vectorStore.size());
        stats.put("totalPages", pageIdToTitle.size());
        stats.put("configuredPages", confluencePageIds.size());
        return stats;
    }
}
