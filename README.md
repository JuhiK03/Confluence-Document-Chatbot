# Confluence Chatbot

A modern, AI-powered chatbot that provides intelligent responses based on your Confluence documents, with support for casual conversation. Built with Java (SparkJava) backend and React frontend.

## 🌟 Features

- **Intelligent Document Search**: AI-powered responses based on Confluence document embeddings
- **Casual Conversation**: Handles greetings, time queries, and general chat
- **Modern Dark UI**: Beautiful, responsive React frontend with dark theme
- **Real-time Chat**: Continuous conversation flow with typing indicators
- **Multi-page Support**: Process multiple Confluence pages simultaneously
- **REST API**: Clean REST endpoints for web integration
- **Error Handling**: Graceful error handling and connection status indicators

## 🏗️ Architecture

- **Backend**: Java with SparkJava framework
- **Frontend**: React with modern UI components
- **AI**: OpenAI GPT-4 for responses and text embeddings
- **Data Source**: Confluence Cloud/Server via REST API
- **Scheduling**: Automatic document refresh every 10 minutes

## 📋 Prerequisites

### Backend Requirements
- Java 8 or higher
- Maven 3.6+
- OpenAI API key
- Confluence account with API access

### Frontend Requirements
- Node.js 16+ and npm
- Modern web browser

## 🚀 Setup Instructions

### 1. Backend Setup

#### Clone and Navigate
```bash
cd confluence-chatbot-one-page-executable/confluence-chatbot
```

#### Environment Configuration
Create a `.env` file in the `confluence-chatbot` directory:

```env
# OpenAI Configuration
OPENAI_API_KEY=your_openai_api_key_here

# Confluence Configuration
CONFLUENCE_URL=https://your-domain.atlassian.net
CONFLUENCE_EMAIL=your-email@company.com
CONFLUENCE_TOKEN=your_confluence_api_token

# Optional: Multiple Confluence pages (comma-separated page IDs)
CONFLUENCE_PAGE_IDS=6258689,1234567,8901234

# Optional: Server configuration
SERVER_PORT=8080
```

#### Getting Confluence API Token
1. Go to [Atlassian Account Settings](https://id.atlassian.com/manage-profile/security/api-tokens)
2. Click "Create API token"
3. Copy the generated token to your `.env` file

#### Getting Confluence Page IDs
- Open your Confluence page in browser
- Page ID is in the URL: `https://your-domain.atlassian.net/wiki/spaces/SPACE/pages/PAGE_ID/Page-Title`
- For multiple pages, list them comma-separated in `CONFLUENCE_PAGE_IDS`

#### Build and Run Backend
```bash
# Install dependencies and compile
mvn clean package

# Run in API mode (recommended)
java -jar target/confluence-chatbot-1.0.jar --api

# Or run in CLI mode (legacy)
mvn exec:java
```

The backend API will start on `http://localhost:8080`

### 2. Frontend Setup

#### Navigate to Frontend Directory
```bash
cd ../frontend
```

#### Install Dependencies
```bash
npm install
```

#### Start Development Server
```bash
npm start
```

The frontend will start on `http://localhost:3000` and automatically proxy API requests to the backend.

## 🎯 Usage

### Web Interface
1. Open `http://localhost:3000` in your browser
2. Start chatting! Try:
   - "Hello" (casual conversation)
   - "What time is it?" (time query)
   - "What is the deployment process?" (Confluence-based question)
   - "How do I configure the system?" (document search)

### API Endpoints

#### Chat Endpoint
```http
POST /chat
Content-Type: application/json

{
  "message": "Your question here",
  "sessionId": "optional-session-id"
}
```

Response:
```json
{
  "message": "Bot response",
  "timestamp": "2024-01-01T12:00:00",
  "success": true,
  "sessionId": "session-id"
}
```

#### Health Check
```http
GET /health
```

#### API Information
```http
GET /api/info
```

## ⚙️ Configuration

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `OPENAI_API_KEY` | OpenAI API key for GPT and embeddings | Yes | - |
| `CONFLUENCE_URL` | Base URL of your Confluence instance | Yes | - |
| `CONFLUENCE_EMAIL` | Email for Confluence authentication | Yes | - |
| `CONFLUENCE_TOKEN` | Confluence API token | Yes | - |
| `CONFLUENCE_PAGE_IDS` | Comma-separated list of page IDs to process | No | `6258689` |
| `SERVER_PORT` | Port for the backend server | No | `8080` |

### Adding Multiple Pages
To process multiple Confluence pages, add their IDs to the `CONFLUENCE_PAGE_IDS` environment variable:

```env
CONFLUENCE_PAGE_IDS=6258689,1234567,8901234,5678901
```

The system will fetch and process all specified pages, creating embeddings that can answer questions from any of the documents.

## 🔧 Development

### Backend Development
```bash
cd confluence-chatbot

# Run tests
mvn test

# Package application
mvn package

# Run with specific profile
mvn exec:java -Dexec.args="--api" -Dspring.profiles.active=dev
```

### Frontend Development
```bash
cd frontend

# Start development server with hot reload
npm start

# Build for production
npm run build

# Run tests
npm test
```

### Adding New Casual Conversation Patterns
Edit `CasualConversationHandler.java` to add new patterns and responses:

```java
// Add new pattern
private static final Pattern NEW_PATTERN = Pattern.compile(
    "(?i)\\b(new|pattern|keywords)\\b"
);

// Add new responses
private static final String[] NEW_RESPONSES = {
    "Response 1",
    "Response 2"
};
```

## 📦 Production Deployment

### Backend (JAR)
```bash
cd confluence-chatbot
mvn clean package
java -jar target/confluence-chatbot-1.0.jar --api
```

### Frontend (Static Files)
```bash
cd frontend
npm run build
# Deploy the 'build' folder to your web server
```

### Docker Support
You can containerize both applications:

```dockerfile
# Backend Dockerfile example
FROM openjdk:8-jre-slim
COPY target/confluence-chatbot-1.0.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar", "--api"]
```

## 🐛 Troubleshooting

### Common Issues

#### Backend Won't Start
- Check `.env` file exists and has correct values
- Verify OpenAI API key is valid
- Ensure Confluence credentials are correct
- Check if port 8080 is available

#### Frontend Can't Connect to Backend
- Ensure backend is running on port 8080
- Check proxy configuration in `package.json`
- Verify CORS headers are enabled (automatically handled)

#### No Confluence Data
- Verify page IDs are correct
- Check Confluence API permissions
- Review logs for API errors
- Ensure pages are accessible with provided credentials

#### Embeddings Not Generated
- Verify OpenAI API key has embedding permissions
- Check API rate limits
- Review network connectivity
- Monitor OpenAI API usage

### Debug Logging
The application logs important events to console. Monitor the output for:
- Confluence API calls
- Embedding generation
- Scheduler activities
- API requests

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📄 License

This project is proprietary to AIO Tests.

## 📞 Support

For technical support or questions:
- Check the troubleshooting section above
- Review application logs
- Verify configuration settings
- Contact the development team

---

## 🔍 API Reference

### Chat Request Format
```typescript
interface ChatRequest {
  message: string;      // User's message
  sessionId?: string;   // Optional session identifier
}
```

### Chat Response Format
```typescript
interface ChatResponse {
  message: string;      // Bot's response
  timestamp: string;    // ISO timestamp
  success: boolean;     // Request success status
  sessionId: string;    // Session identifier
  error?: string;       // Error message if success is false
}
```

### Error Handling
The API returns appropriate HTTP status codes:
- `200`: Success
- `400`: Bad request (invalid JSON, missing message)
- `500`: Internal server error

Error responses include detailed error messages in the response body.
