# Quick Start Guide - Confluence Chatbot

Get your chatbot running in 5 minutes!

## 🚀 Prerequisites

- Java 8+ installed
- Node.js 16+ installed  
- Maven 3.6+ installed
- OpenAI API key
- Confluence account with API access

## ⚡ Quick Setup

### 1. Configure Environment
```bash
cd confluence-chatbot
cp .env.example .env
```

Edit `.env` with your credentials:
```env
OPENAI_API_KEY=sk-your-openai-api-key
CONFLUENCE_URL=https://your-domain.atlassian.net
CONFLUENCE_EMAIL=your-email@company.com
CONFLUENCE_TOKEN=your-confluence-api-token
CONFLUENCE_PAGE_IDS=6258689,1234567  # Your page IDs
```

### 2. Start Services

**Windows:**
```cmd
start-chatbot.bat
```

**Mac/Linux:**
```bash
./start-chatbot.sh
```

**Manual Start:**

Backend:
```bash
cd confluence-chatbot
# First build if needed
mvn clean package
# Then run
java -jar target/confluence-chatbot-1.0.jar --api
```

Frontend (new terminal):
```bash
cd frontend
npm install
npm start
```

### 3. Access Your Chatbot

- **Web UI:** http://localhost:3000
- **API Test:** Open `api-test.html` in browser
- **Health Check:** http://localhost:8080/health

## 🎯 Test It

Try these messages:
- "Hello" (casual chat)
- "What time is it?" (time query)
- "What is our deployment process?" (Confluence search)

## 🔧 Getting Your Confluence Setup

### API Token
1. Go to https://id.atlassian.com/manage-profile/security/api-tokens
2. Click "Create API token"
3. Copy token to `.env`

### Page IDs
1. Open Confluence page in browser
2. Look at URL: `https://domain.atlassian.net/wiki/spaces/SPACE/pages/PAGE_ID/Title`
3. Copy `PAGE_ID` numbers to `.env`

## ❌ Troubleshooting

**Backend won't start:**
- Check `.env` file exists and has correct values
- Verify Java installed: `java -version`
- Build the jar first: `mvn clean package`
- Make sure port 8080 is available

**Frontend won't connect:**
- Ensure backend is running on port 8080
- Check browser console for errors

**No Confluence data:**
- Verify page IDs are correct
- Check Confluence credentials
- Ensure pages are accessible

## 📖 Need More Help?

- Full documentation: `README.md`
- API testing: Open `api-test.html`
- Check logs in terminal windows

## 🧪 Quick Test

To test just the backend API:
```bash
# Run this script for quick testing
test-chatbot.bat   # Windows
./test-chatbot.sh  # Mac/Linux
```

Or test manually:
1. Visit: http://localhost:8080/health
2. Open `api-test.html` in your browser
3. Try the diagnostics: http://localhost:8080/api/diagnostics

That's it! Your AI chatbot should now be running and ready to answer questions from your Confluence documents! 🎉