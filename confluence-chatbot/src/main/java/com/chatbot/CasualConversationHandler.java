package com.chatbot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.regex.Pattern;

public class CasualConversationHandler {

    private static final Random random = new Random();

    // Greeting patterns
    private static final Pattern GREETING_PATTERN = Pattern.compile(
        "(?i)\\b(hi|hello|hey|good morning|good afternoon|good evening|greetings|howdy)\\b"
    );

    // Time patterns
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "(?i)\\b(what time|current time|time is it|what's the time|whats the time)\\b"
    );

    // Weather patterns
    private static final Pattern WEATHER_PATTERN = Pattern.compile(
        "(?i)\\b(weather|temperature|forecast|hot|cold|rain|sunny|cloudy)\\b"
    );

    // How are you patterns
    private static final Pattern HOW_ARE_YOU_PATTERN = Pattern.compile(
        "(?i)\\b(how are you|how do you do|how's it going|hows it going|how are things)\\b"
    );

    // Thank you patterns
    private static final Pattern THANK_YOU_PATTERN = Pattern.compile(
        "(?i)\\b(thank you|thanks|thank u|thx|appreciate)\\b"
    );

    // Goodbye patterns
    private static final Pattern GOODBYE_PATTERN = Pattern.compile(
        "(?i)\\b(bye|goodbye|see you|farewell|take care|catch you later)\\b"
    );

    // Name patterns
    private static final Pattern NAME_PATTERN = Pattern.compile(
        "(?i)\\b(what's your name|whats your name|who are you|your name)\\b"
    );

    // Help patterns
    private static final Pattern HELP_PATTERN = Pattern.compile(
        "(?i)\\b(help|assist|support|what can you do|how can you help)\\b"
    );

    // Greeting responses
    private static final String[] GREETING_RESPONSES = {
        "Hello!",
        "Hi there!",
        "Hey!",
        "Good to see you!",
        "Hello! Nice to meet you.",
    };

    // How are you responses
    private static final String[] HOW_ARE_YOU_RESPONSES = {
        "I'm doing well, thank you for asking!",
        "I'm great, thanks!",
        "I'm doing fantastic!",
        "I'm well, thanks for asking!",
    };

    // Thank you responses
    private static final String[] THANK_YOU_RESPONSES = {
        "You're welcome!",
        "My pleasure!",
        "You're very welcome!",
        "Happy to help!",
    };

    // Goodbye responses
    private static final String[] GOODBYE_RESPONSES = {
        "Goodbye! Have a great day!",
        "See you later!",
        "Take care!",
        "Farewell!",
    };

    // Name responses
    private static final String[] NAME_RESPONSES = {
        "I'm AIO Tests Assistant!",
        "You can call me AIO Assistant.",
        "I'm the AIO Tests chatbot.",
        "I'm AIO Assistant.",
    };

    // Help responses
    private static final String[] HELP_RESPONSES = {
        "I can help you with questions about your Confluence documents, provide general information, and have casual conversations. Just ask me anything!",
        "I'm here to assist with information from your Confluence pages, answer general questions, and chat with you. What would you like to know?",
        "I can search through your Confluence content to answer questions, provide the current time, and have friendly conversations. How can I help?",
        "I'm designed to help with Confluence-related questions and general inquiries. Feel free to ask me about anything!",
    };

    public boolean canHandle(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        String trimmedMessage = message.trim();

        return (
            GREETING_PATTERN.matcher(trimmedMessage).find() ||
            TIME_PATTERN.matcher(trimmedMessage).find() ||
            WEATHER_PATTERN.matcher(trimmedMessage).find() ||
            HOW_ARE_YOU_PATTERN.matcher(trimmedMessage).find() ||
            THANK_YOU_PATTERN.matcher(trimmedMessage).find() ||
            GOODBYE_PATTERN.matcher(trimmedMessage).find() ||
            NAME_PATTERN.matcher(trimmedMessage).find() ||
            HELP_PATTERN.matcher(trimmedMessage).find()
        );
    }

    public String handleMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "I'm sorry, I didn't understand that. Could you please rephrase your question?";
        }

        String trimmedMessage = message.trim();

        // Check patterns in order of priority
        if (GREETING_PATTERN.matcher(trimmedMessage).find()) {
            return getRandomResponse(GREETING_RESPONSES);
        }

        if (TIME_PATTERN.matcher(trimmedMessage).find()) {
            return getCurrentTime();
        }

        if (WEATHER_PATTERN.matcher(trimmedMessage).find()) {
            return getWeatherResponse();
        }

        if (HOW_ARE_YOU_PATTERN.matcher(trimmedMessage).find()) {
            return getRandomResponse(HOW_ARE_YOU_RESPONSES);
        }

        if (THANK_YOU_PATTERN.matcher(trimmedMessage).find()) {
            return getRandomResponse(THANK_YOU_RESPONSES);
        }

        if (GOODBYE_PATTERN.matcher(trimmedMessage).find()) {
            return getRandomResponse(GOODBYE_RESPONSES);
        }

        if (NAME_PATTERN.matcher(trimmedMessage).find()) {
            return getRandomResponse(NAME_RESPONSES);
        }

        if (HELP_PATTERN.matcher(trimmedMessage).find()) {
            return getRandomResponse(HELP_RESPONSES);
        }

        // This shouldn't happen if canHandle() worked correctly
        return "I'm sorry, I didn't understand that. Could you please rephrase your question?";
    }

    private String getRandomResponse(String[] responses) {
        return responses[random.nextInt(responses.length)];
    }

    private String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
            "EEEE, MMMM d, yyyy 'at' h:mm a"
        );
        return "The current time is " + now.format(formatter) + ".";
    }

    private String getWeatherResponse() {
        return "I don't have access to real-time weather data, but I'd be happy to help you with questions about your Confluence documents instead! For weather information, I recommend checking your local weather service or app.";
    }
}
