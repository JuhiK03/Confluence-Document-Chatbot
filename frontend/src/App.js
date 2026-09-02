import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { format } from 'date-fns';
import './App.css';

const App = () => {
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [sessionId] = useState(() => `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`);
  const [isConnected, setIsConnected] = useState(true);
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  // Auto-scroll to bottom when new messages arrive
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Focus input on component mount
  useEffect(() => {
    inputRef.current?.focus();

    // Add welcome message
    setMessages([
      {
        id: 'welcome',
        text: "Hello! I'm your Confluence Bot. I can help you with questions about your Confluence documents, or we can have a casual conversation. How can I help you today?",
        sender: 'bot',
        timestamp: new Date(),
      }
    ]);

    // Check server connection
    checkServerConnection();
  }, []);

  const checkServerConnection = async () => {
    try {
      await axios.get('/health');
      setIsConnected(true);
    } catch (error) {
      setIsConnected(false);
      console.error('Server connection failed:', error);
    }
  };

  const sendMessage = async () => {
    if (!inputMessage.trim() || isLoading) return;

    const userMessage = {
      id: `user_${Date.now()}`,
      text: inputMessage.trim(),
      sender: 'user',
      timestamp: new Date(),
    };

    setMessages(prev => [...prev, userMessage]);
    setInputMessage('');
    setIsLoading(true);

    try {
      const response = await axios.post('/chat', {
        message: userMessage.text,
        sessionId: sessionId
      });

      const botMessage = {
        id: `bot_${Date.now()}`,
        text: response.data.message,
        sender: 'bot',
        timestamp: new Date(),
      };

      setMessages(prev => [...prev, botMessage]);
      setIsConnected(true);
    } catch (error) {
      console.error('Error sending message:', error);
      setIsConnected(false);

      const errorMessage = {
        id: `error_${Date.now()}`,
        text: "Sorry, I'm having trouble connecting to the server. Please check your connection and try again.",
        sender: 'bot',
        timestamp: new Date(),
        isError: true,
      };

      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
      inputRef.current?.focus();
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const startNewChat = () => {
    setMessages([
      {
        id: 'welcome_new',
        text: "Hello! I'm your Confluence Bot. How can I help you today?",
        sender: 'bot',
        timestamp: new Date(),
      }
    ]);
  };

  const formatTimestamp = (timestamp) => {
    return format(timestamp, 'HH:mm');
  };

  const renderMessage = (message) => {
    const isUser = message.sender === 'user';
    const isError = message.isError;

    return (
      <div
        key={message.id}
        className={`message ${isUser ? 'user-message' : 'bot-message'} ${isError ? 'error-message' : ''}`}
      >
        <div className="message-content">
          <div className="message-text">
            {message.text}
          </div>
          <div className="message-timestamp">
            {formatTimestamp(message.timestamp)}
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-content">
          <h1 className="company-name">XXXX</h1>
          <h2 className="app-title">Confluence Chatbot</h2>
          <div className="connection-status">
            <div className={`status-indicator ${isConnected ? 'connected' : 'disconnected'}`}>
              <span className="status-dot"></span>
              {isConnected ? 'Connected' : 'Disconnected'}
            </div>
          </div>
        </div>
        <button
          className="new-chat-btn"
          onClick={startNewChat}
          title="Start New Chat"
        >
          New Chat
        </button>
      </header>

      <main className="chat-container">
        <div className="messages-container">
          {messages.map(renderMessage)}
          {isLoading && (
            <div className="message bot-message">
              <div className="message-content">
                <div className="typing-indicator">
                  <div className="typing-dots">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                  <div className="typing-text">Confluence Bot is typing...</div>
                </div>
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>

        <div className="input-container">
          <div className="input-wrapper">
            <textarea
              ref={inputRef}
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              onKeyPress={handleKeyPress}
              placeholder="Type your message here... (Press Enter to send, Shift+Enter for new line)"
              className="message-input"
              rows="1"
              disabled={isLoading || !isConnected}
            />
            <button
              onClick={sendMessage}
              disabled={!inputMessage.trim() || isLoading || !isConnected}
              className="send-button"
              title="Send Message"
            >
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path
                  d="M2 21L23 12L2 3V10L17 12L2 14V21Z"
                  fill="currentColor"
                />
              </svg>
            </button>
          </div>
          {!isConnected && (
            <div className="connection-warning">
              Unable to connect to server. Please check if the backend is running.
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

export default App;
