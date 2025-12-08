# Fleet Navigator - API Documentation

**Version:** 0.1.0-SNAPSHOT
**Base URL:** `http://localhost:8080`

---

## Chat Endpoints

### POST /api/chat/send
Send a message to a chat and get AI response.

**Request Body:**
```json
{
  "chatId": 1,          // Optional - creates new chat if null
  "message": "Hello!",  // Required
  "model": "llama3.2:3b", // Optional - uses chat's model if null
  "systemPrompt": "You are a helpful assistant", // Optional
  "stream": false       // Optional - streaming not yet implemented
}
```

**Response:**
```json
{
  "chatId": 1,
  "response": "Hello! How can I help you today?",
  "tokens": 25,
  "model": "llama3.2:3b"
}
```

---

### POST /api/chat/new
Create a new chat.

**Request Body:**
```json
{
  "title": "My Chat",      // Optional - defaults to "New Chat"
  "model": "llama3.2:3b"   // Optional - defaults to configured default
}
```

**Response:**
```json
{
  "id": 1,
  "title": "My Chat",
  "model": "llama3.2:3b",
  "createdAt": "2025-10-31T13:30:00",
  "updatedAt": "2025-10-31T13:30:00",
  "messages": [],
  "totalTokens": 0
}
```

---

### GET /api/chat/history/{chatId}
Get complete chat history with all messages.

**Response:**
```json
{
  "id": 1,
  "title": "My Chat",
  "model": "llama3.2:3b",
  "createdAt": "2025-10-31T13:30:00",
  "updatedAt": "2025-10-31T13:35:00",
  "messages": [
    {
      "id": 1,
      "role": "USER",
      "content": "Hello!",
      "tokens": 5,
      "createdAt": "2025-10-31T13:30:00"
    },
    {
      "id": 2,
      "role": "ASSISTANT",
      "content": "Hello! How can I help?",
      "tokens": 25,
      "createdAt": "2025-10-31T13:30:05"
    }
  ],
  "totalTokens": 30
}
```

---

### GET /api/chat/all
Get all chats (sorted by most recent).

**Response:**
```json
[
  {
    "id": 1,
    "title": "Chat 1",
    "model": "llama3.2:3b",
    "createdAt": "2025-10-31T13:30:00",
    "updatedAt": "2025-10-31T13:35:00",
    "messages": [...],
    "totalTokens": 150
  },
  {
    "id": 2,
    "title": "Chat 2",
    "model": "codellama:70b",
    "createdAt": "2025-10-31T12:00:00",
    "updatedAt": "2025-10-31T12:10:00",
    "messages": [...],
    "totalTokens": 500
  }
]
```

---

### DELETE /api/chat/{chatId}
Delete a chat and all its messages.

**Response:** `204 No Content`

---

## Model Endpoints

### GET /api/models
Get all available Ollama models.

**Response:**
```json
[
  {
    "name": "llama3.2:3b",
    "size": "2.0 GB",
    "modifiedAt": "2025-10-30T10:00:00",
    "contextWindow": null
  },
  {
    "name": "codellama:70b",
    "size": "38.0 GB",
    "modifiedAt": "2025-10-29T15:30:00",
    "contextWindow": null
  }
]
```

---

## Statistics Endpoints

### GET /api/stats/global
Get global usage statistics.

**Response:**
```json
{
  "totalTokens": 15000,
  "totalMessages": 250,
  "chatCount": 10
}
```

---

### GET /api/stats/chat/{chatId}
Get statistics for a specific chat.

**Response:**
```json
{
  "totalTokens": 500,
  "totalMessages": 20,
  "chatCount": 1
}
```

---

## System Endpoints

### GET /api/system/status
Get system monitoring information.

**Response:**
```json
{
  "cpuUsage": 1.5,
  "totalMemory": 2147483648,
  "freeMemory": 1073741824,
  "usedMemory": 1073741824,
  "ollamaAvailable": true,
  "ollamaVersion": "Unknown"
}
```

---

## Error Responses

All endpoints may return these error codes:

- **400 Bad Request** - Invalid request body
- **404 Not Found** - Chat/resource not found
- **500 Internal Server Error** - Ollama connection issues or server errors

**Error Response Example:**
```json
{
  "timestamp": "2025-10-31T13:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to connect to Ollama",
  "path": "/api/chat/send"
}
```

---

## CORS Configuration

The API allows requests from:
- `http://localhost:3000` (React default)
- `http://localhost:5173` (Vite default)
- `http://localhost:8081` (Alternative port)

Allowed methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`

---

## Database

Fleet Navigator uses H2 in-memory database for development.

**H2 Console:** `http://localhost:8080/h2-console`

**Connection Details:**
- JDBC URL: `jdbc:h2:mem:fleetnavdb`
- Username: `sa`
- Password: (empty)

---

## Testing with curl

### Create a new chat:
```bash
curl -X POST http://localhost:8080/api/chat/new \
  -H "Content-Type: application/json" \
  -d '{"title": "Test Chat", "model": "llama3.2:3b"}'
```

### Send a message:
```bash
curl -X POST http://localhost:8080/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{
    "chatId": 1,
    "message": "What is Java?",
    "systemPrompt": "You are a Java expert"
  }'
```

### Get all models:
```bash
curl http://localhost:8080/api/models
```

### Get global stats:
```bash
curl http://localhost:8080/api/stats/global
```

---

## Notes

- Token counts are estimated (4 characters ≈ 1 token)
- Streaming is not yet implemented
- WebSocket support coming in Phase 2
- Context file upload coming in Phase 3
