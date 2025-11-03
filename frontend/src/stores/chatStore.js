import { defineStore } from 'pinia'
import { ref, computed, reactive, watch } from 'vue'
import api from '../services/api'
import { useSettingsStore } from './settingsStore'

const PROMPT_STORAGE_KEY = 'fleet-navigator-last-prompt'

export const useChatStore = defineStore('chat', () => {
  // Load last used system prompt from localStorage
  const loadLastPrompt = () => {
    try {
      const stored = localStorage.getItem(PROMPT_STORAGE_KEY)
      if (stored) {
        const { content, title } = JSON.parse(stored)
        console.log('✅ Loaded last system prompt:', title)
        return { content, title }
      }
    } catch (e) {
      console.error('Failed to load last prompt', e)
    }
    return { content: '', title: null }
  }

  const lastPrompt = loadLastPrompt()

  // State
  const chats = ref([])
  const currentChat = ref(null)
  const messages = ref([])
  const isLoading = ref(false)
  const error = ref(null)
  const models = ref([])
  const selectedModel = ref('phi:latest') // Microsoft Phi - small and efficient default
  const systemPrompt = ref(lastPrompt.content)
  const systemPromptTitle = ref(lastPrompt.title)
  const streamingEnabled = ref(true)  // Default: Streaming aktiviert
  const currentRequestId = ref(null)  // Track current request for cancellation

  // Watch system prompt changes and save to localStorage
  watch([systemPrompt, systemPromptTitle], ([newPrompt, newTitle]) => {
    if (newPrompt) {
      try {
        localStorage.setItem(PROMPT_STORAGE_KEY, JSON.stringify({
          content: newPrompt,
          title: newTitle
        }))
        console.log('💾 Saved system prompt:', newTitle || '(custom)')
      } catch (e) {
        console.error('Failed to save prompt', e)
      }
    }
  })

  // Global stats
  const globalStats = ref({
    totalTokens: 0,
    totalMessages: 0,
    chatCount: 0
  })

  // System status
  const systemStatus = ref({
    cpuUsage: 0,
    totalMemory: 0,
    freeMemory: 0,
    usedMemory: 0,
    ollamaAvailable: false,
    ollamaVersion: 'Unknown'
  })

  // Computed
  const currentChatTokens = computed(() => {
    if (!messages.value.length) return 0
    return messages.value.reduce((sum, msg) => sum + (msg.tokens || 0), 0)
  })

  const memoryUsagePercent = computed(() => {
    if (!systemStatus.value.totalMemory) return 0
    return Math.round((systemStatus.value.usedMemory / systemStatus.value.totalMemory) * 100)
  })

  // Actions
  async function loadChats() {
    try {
      chats.value = await api.getAllChats()
    } catch (err) {
      error.value = 'Failed to load chats'
      console.error(err)
    }
  }

  async function loadModels() {
    try {
      models.value = await api.getAvailableModels()

      // Load default model from backend
      const defaultModelResponse = await api.getDefaultModel()
      if (defaultModelResponse && defaultModelResponse.model) {
        selectedModel.value = defaultModelResponse.model
      }
    } catch (err) {
      error.value = 'Failed to load models'
      console.error(err)
    }
  }

  async function createNewChat(title = 'New Chat') {
    try {
      isLoading.value = true
      const chat = await api.createNewChat({
        title,
        model: selectedModel.value
      })
      chats.value.unshift(chat)
      currentChat.value = chat
      messages.value = []
      return chat
    } catch (err) {
      error.value = 'Failed to create chat'
      console.error(err)
    } finally {
      isLoading.value = false
    }
  }

  async function sendMessage(messageData) {
    // Handle both string (legacy) and object with files
    const messageText = typeof messageData === 'string' ? messageData : messageData.text
    const files = typeof messageData === 'object' ? messageData.files || [] : []

    if (!messageText.trim()) return

    try {
      isLoading.value = true
      error.value = null

      // Process uploaded files
      const images = []
      let documentContext = ''

      for (const file of files) {
        if (file.type === 'image' && file.base64Content) {
          images.push(file.base64Content)
        } else if (file.type === 'pdf' || file.type === 'text') {
          if (file.textContent) {
            documentContext += `\n\n=== ${file.name} ===\n${file.textContent}`
          }
        }
      }

      // Vision-Chaining Logic
      const settingsStore = useSettingsStore()
      let visionChainEnabled = false
      let visionModel = null

      if (images.length > 0) {
        const isCurrentModelVision = settingsStore.isVisionModel(selectedModel.value)
        const chainEnabled = settingsStore.getSetting('visionChainEnabled')

        if (!isCurrentModelVision && chainEnabled) {
          // Vision-Chaining aktivieren
          visionChainEnabled = true
          visionModel = settingsStore.getSetting('preferredVisionModel')
          console.log(`🔗 Vision-Chaining aktiviert: ${visionModel} → ${selectedModel.value}`)
        } else if (!isCurrentModelVision && settingsStore.getSetting('autoSelectVisionModel')) {
          // Legacy: Wechsel zu Vision Model (ohne Chaining)
          const preferredVision = settingsStore.getSetting('preferredVisionModel')
          console.log(`🖼️ Bild erkannt! Wechsle zu Vision Model: ${preferredVision}`)
          selectedModel.value = preferredVision
        }
      }

      // Add user message optimistically
      const userMessage = {
        role: 'USER',
        content: messageText,
        createdAt: new Date().toISOString()
      }
      messages.value.push(userMessage)

      // Check if streaming is enabled
      if (streamingEnabled.value) {
        // Use streaming endpoint
        await sendMessageStreaming(messageText, images, documentContext, visionChainEnabled, visionModel)
      } else {
        // Build request
        const settingsStore = useSettingsStore()

        const request = {
          chatId: currentChat.value?.id,
          message: messageText,
          model: selectedModel.value,
          systemPrompt: systemPrompt.value,
          stream: false,
          // Add generation parameters from settings
          maxTokens: settingsStore.settings.maxTokens,
          temperature: settingsStore.settings.temperature,
          topP: settingsStore.settings.topP,
          topK: settingsStore.settings.topK,
          repeatPenalty: settingsStore.settings.repeatPenalty
        }

        // Add images if present
        if (images.length > 0) {
          request.images = images
        }

        // Add document context if present
        if (documentContext.trim()) {
          request.documentContext = documentContext.trim()
        }

        // Use regular non-streaming endpoint
        const response = await api.sendMessage(request)

        // Store request ID for potential cancellation
        currentRequestId.value = response.requestId

        // Update current chat ID if new chat was created
        if (!currentChat.value) {
          currentChat.value = { id: response.chatId }
          await loadChats()
        }

        // Add assistant response
        const assistantMessage = {
          role: 'ASSISTANT',
          content: response.response,
          tokens: response.tokens,
          createdAt: new Date().toISOString()
        }
        messages.value.push(assistantMessage)

        // Reload global stats
        await loadGlobalStats()

        // Clear request ID when done
        currentRequestId.value = null
      }

      return true
    } catch (err) {
      // Check if it was cancelled
      if (err.message && err.message.includes('cancelled')) {
        console.log('Request was cancelled by user')
        // Don't remove the user message, keep it in history
      } else {
        error.value = 'Failed to send message'
        console.error(err)
        // Remove optimistic user message on error
        messages.value.pop()
      }
    } finally {
      isLoading.value = false
      currentRequestId.value = null
    }
  }

  async function sendMessageStreaming(messageText, images = [], documentContext = '', visionChainEnabled = false, visionModel = null) {
    return new Promise((resolve, reject) => {
      // Construct SSE endpoint URL
      const baseURL = '/api/chat/send-stream'

      const settingsStore = useSettingsStore()

      // DEBUG: Log settings before sending
      console.log('🔍 Settings Store:', settingsStore.settings)
      console.log('🔍 maxTokens:', settingsStore.settings.maxTokens)

      // Create EventSource with POST body (using fetch to send body, then EventSource for reading)
      const requestBody = {
        chatId: currentChat.value?.id,
        message: messageText,
        model: selectedModel.value,
        systemPrompt: systemPrompt.value,
        stream: true,
        // Add generation parameters from settings
        maxTokens: settingsStore.settings.maxTokens,
        temperature: settingsStore.settings.temperature,
        topP: settingsStore.settings.topP,
        topK: settingsStore.settings.topK,
        repeatPenalty: settingsStore.settings.repeatPenalty
      }

      // DEBUG: Log request body
      console.log('📤 Request Body:', JSON.stringify(requestBody, null, 2))

      // Add images if present
      if (images.length > 0) {
        requestBody.images = images
      }

      // Add document context if present
      if (documentContext.trim()) {
        requestBody.documentContext = documentContext.trim()
      }

      // Add Vision-Chaining parameters
      if (visionChainEnabled && visionModel) {
        requestBody.visionChainEnabled = true
        requestBody.visionModel = visionModel

        // Erzwinge deutsche Ausgabe im System-Prompt
        const deutschPrompt = 'Du antwortest IMMER auf Deutsch.'
        requestBody.systemPrompt = requestBody.systemPrompt
          ? deutschPrompt + '\n\n' + requestBody.systemPrompt
          : deutschPrompt

        console.log('📤 Vision-Chaining:', visionModel, '→', selectedModel.value, '(Deutsch erzwungen)')
      }

      // We need to use fetch for POST with body, then read SSE
      fetch(baseURL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody)
      }).then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }
        console.log('Streaming started, response status:', response.status)
        const reader = response.body.getReader()
        const decoder = new TextDecoder()

        // Add placeholder for streaming message (reactive!)
        const streamingMessage = reactive({
          role: 'ASSISTANT',
          content: '',
          tokens: 0,
          createdAt: new Date().toISOString(),
          isStreaming: true
        })
        messages.value.push(streamingMessage)

        let buffer = ''

        function processChunk({ done, value }) {
          if (done) {
            console.log('Streaming completed')
            streamingMessage.isStreaming = false
            isLoading.value = false
            loadGlobalStats()
            resolve()
            return
          }

          // Decode chunk
          buffer += decoder.decode(value, { stream: true })

          // Split by newlines (SSE format)
          const lines = buffer.split('\n')
          buffer = lines.pop() || '' // Keep incomplete line in buffer

          for (const line of lines) {
            console.log('[SSE] Processing line:', line)
            if (line.startsWith('data:')) {
              const data = line.substring(5)  // DON'T trim - spaces are important!
              console.log('[SSE] Data:', data)

              try {
                const parsed = JSON.parse(data)
                console.log('[SSE] Parsed JSON:', parsed)

                // Handle different event types based on content
                if (parsed.chatId) {
                  // Start event
                  console.log('[SSE] Start event - chatId:', parsed.chatId)
                  if (!currentChat.value) {
                    currentChat.value = { id: parsed.chatId }
                    loadChats()
                  }
                  currentRequestId.value = parsed.requestId
                } else if (parsed.tokens !== undefined) {
                  // Done event
                  console.log('[SSE] Done event - tokens:', parsed.tokens)
                  streamingMessage.tokens = parsed.tokens
                  streamingMessage.isStreaming = false
                  currentRequestId.value = null
                } else if (parsed.error) {
                  // Error event
                  console.error('Streaming error:', parsed.error)
                  error.value = parsed.error
                  reject(new Error(parsed.error))
                }
              } catch (e) {
                // Plain text chunk
                console.log('[SSE] Plain text chunk:', data)
                streamingMessage.content += data
              }
            } else if (line.startsWith('event:')) {
              console.log('[SSE] Event type:', line)
              // SSE event name (we can ignore for now)
            }
          }

          // Read next chunk
          reader.read().then(processChunk).catch(err => {
            console.error('Streaming error:', err)
            streamingMessage.isStreaming = false
            isLoading.value = false
            reject(err)
          })
        }

        // Start reading
        reader.read().then(processChunk).catch(err => {
          console.error('Failed to start streaming:', err)
          reject(err)
        })
      }).catch(err => {
        console.error('Failed to initiate streaming:', err)
        reject(err)
      })
    })
  }

  async function loadChatHistory(chatId) {
    try {
      isLoading.value = true
      const chat = await api.getChatHistory(chatId)
      currentChat.value = chat
      messages.value = chat.messages || []
    } catch (err) {
      error.value = 'Failed to load chat history'
      console.error(err)
    } finally {
      isLoading.value = false
    }
  }

  async function renameChat(chatId, newTitle) {
    try {
      const updatedChat = await api.renameChat(chatId, newTitle)
      // Update chat in list
      const index = chats.value.findIndex(c => c.id === chatId)
      if (index !== -1) {
        chats.value[index] = { ...chats.value[index], title: newTitle }
      }
      // Update current chat if it's the one being renamed
      if (currentChat.value?.id === chatId) {
        currentChat.value.title = newTitle
      }
      return updatedChat
    } catch (err) {
      error.value = 'Failed to rename chat'
      console.error(err)
      throw err
    }
  }

  async function deleteChat(chatId) {
    try {
      await api.deleteChat(chatId)
      chats.value = chats.value.filter(c => c.id !== chatId)
      if (currentChat.value?.id === chatId) {
        currentChat.value = null
        messages.value = []
      }
    } catch (err) {
      error.value = 'Failed to delete chat'
      console.error(err)
    }
  }

  async function loadGlobalStats() {
    try {
      globalStats.value = await api.getGlobalStats()
    } catch (err) {
      console.error('Failed to load global stats', err)
    }
  }

  async function loadSystemStatus() {
    try {
      systemStatus.value = await api.getSystemStatus()
    } catch (err) {
      console.error('Failed to load system status', err)
    }
  }

  function setSelectedModel(model) {
    selectedModel.value = model
  }

  function setSystemPrompt(prompt, title = null) {
    systemPrompt.value = prompt
    systemPromptTitle.value = title
  }

  function toggleStreaming() {
    streamingEnabled.value = !streamingEnabled.value
  }

  function startNewChat() {
    currentChat.value = null
    messages.value = []
  }

  async function abortCurrentRequest() {
    if (!currentRequestId.value) {
      console.warn('No active request to abort')
      return false
    }

    try {
      console.log('Aborting request:', currentRequestId.value)
      await api.abortRequest(currentRequestId.value)
      isLoading.value = false
      currentRequestId.value = null
      return true
    } catch (err) {
      console.error('Failed to abort request', err)
      return false
    }
  }

  return {
    // State
    chats,
    currentChat,
    messages,
    isLoading,
    error,
    models,
    selectedModel,
    systemPrompt,
    systemPromptTitle,
    streamingEnabled,
    globalStats,
    systemStatus,
    currentRequestId,

    // Computed
    currentChatTokens,
    memoryUsagePercent,

    // Actions
    loadChats,
    loadModels,
    createNewChat,
    sendMessage,
    loadChatHistory,
    renameChat,
    deleteChat,
    loadGlobalStats,
    loadSystemStatus,
    setSelectedModel,
    setSystemPrompt,
    toggleStreaming,
    startNewChat,
    abortCurrentRequest
  }
})
