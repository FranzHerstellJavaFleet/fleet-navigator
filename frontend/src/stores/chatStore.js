import { defineStore } from 'pinia'
import { ref, computed, reactive, watch } from 'vue'
import api from '../services/api'
import { useSettingsStore } from './settingsStore'

const PROMPT_STORAGE_KEY = 'fleet-navigator-last-prompt'
const MODEL_STORAGE_KEY = 'fleet-navigator-selected-model'
const CHAT_CACHE_KEY = 'fleet-navigator-chat-cache'

export const useChatStore = defineStore('chat', () => {
  // Helper functions for chat cache in localStorage
  const saveChatToCache = (chatId, messages) => {
    try {
      const cache = JSON.parse(localStorage.getItem(CHAT_CACHE_KEY) || '{}')
      cache[chatId] = messages
      localStorage.setItem(CHAT_CACHE_KEY, JSON.stringify(cache))
    } catch (e) {
      console.error('Failed to save chat to cache:', e)
    }
  }

  const getChatFromCache = (chatId) => {
    try {
      const cache = JSON.parse(localStorage.getItem(CHAT_CACHE_KEY) || '{}')
      return cache[chatId] || null
    } catch (e) {
      console.error('Failed to get chat from cache:', e)
      return null
    }
  }

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
    // Default: null (will load from API)
    return { content: null, title: null }
  }

  // Load last selected model from database (async loaded in mounted)
  const loadLastModel = () => {
    // Return null initially, will be loaded from DB asynchronously in loadModels()
    return null
  }

  const lastPrompt = loadLastPrompt()

  // State
  const chats = ref([])
  const currentChat = ref(null)
  const messages = ref([])
  const isLoading = ref(false)
  const error = ref(null)
  const models = ref([])
  const selectedModel = ref(loadLastModel()) // Load from localStorage
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

  // Watch selected model changes and save to localStorage
  watch(selectedModel, (newModel) => {
    if (newModel) {
      try {
        localStorage.setItem(MODEL_STORAGE_KEY, newModel)
        console.log('💾 Saved selected model:', newModel)
      } catch (e) {
        console.error('Failed to save selected model', e)
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

  // Helper function: Check if model is a custom model
  const isCustomModel = (modelName) => {
    if (!modelName) return false

    const knownOfficialModels = [
      'llama2', 'llama3', 'llama3.1', 'llama3.2', 'llama3.3',
      'mistral', 'mixtral',
      'phi', 'phi3',
      'gemma', 'gemma2',
      'codellama',
      'qwen', 'qwen2', 'qwen2.5', 'qwen3',
      'qwen-coder', 'qwen2.5-coder', 'qwen3-coder',
      'deepseek-coder', 'deepseek-coder-v2',
      'deepseek-r1',
      'llava',
      'moondream',
      'bge-m3',
      'nomic-embed-text',
      'mxbai-embed-large',
      'all-minilm'
    ]

    const baseName = modelName.split(':')[0]
    return !knownOfficialModels.includes(baseName)
  }

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

      // Load user's last selected model from database
      const savedModel = await api.getSelectedModel()
      if (savedModel) {
        selectedModel.value = savedModel
        console.log('✅ Loaded selected model from database:', savedModel)
      } else {
        // No model saved yet, use default
        const defaultModelResponse = await api.getDefaultModel()
        if (defaultModelResponse && defaultModelResponse.model) {
          selectedModel.value = defaultModelResponse.model
          console.log('📥 Using backend default model:', defaultModelResponse.model)
        }
      }

      // ALWAYS sync system prompt with database (localStorage = cache, DB = source of truth)
      try {
        console.log('🔄 Synchronizing system prompt with database...')
        const defaultPrompt = await api.getDefaultSystemPrompt()
        if (defaultPrompt && defaultPrompt.content) {
          systemPrompt.value = defaultPrompt.content
          systemPromptTitle.value = defaultPrompt.name

          // Sync localStorage with database
          localStorage.setItem(PROMPT_STORAGE_KEY, JSON.stringify({
            content: defaultPrompt.content,
            title: defaultPrompt.name
          }))

          console.log('✅ System prompt synchronized with DB:', defaultPrompt.name)
        }
      } catch (promptError) {
        console.warn('⚠️ Could not sync system prompt from DB, using localStorage:', promptError.message)
        // Continue with localStorage values
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

      // Initialize empty cache for new chat
      saveChatToCache(chat.id, [])
      console.log('[Cache-Sync] Initialized cache for new chat')

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
          systemPrompt: isCustomModel(selectedModel.value) ? '' : systemPrompt.value,
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
        systemPrompt: isCustomModel(selectedModel.value) ? '' : systemPrompt.value,
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

        // Erzwinge deutsche Ausgabe im System-Prompt (nur wenn kein Custom Model)
        if (!isCustomModel(selectedModel.value)) {
          const deutschPrompt = 'Du antwortest IMMER auf Deutsch.'
          requestBody.systemPrompt = requestBody.systemPrompt
            ? deutschPrompt + '\n\n' + requestBody.systemPrompt
            : deutschPrompt
        }

        console.log('📤 Vision-Chaining:', visionModel, '→', selectedModel.value, isCustomModel(selectedModel.value) ? '(Custom Model - kein Deutsch-Prompt)' : '(Deutsch erzwungen)')
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
          isStreaming: true,
          modelName: selectedModel.value
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
                  // Done event - replace reactive object with plain object to trigger re-render
                  const finalMessage = {
                    role: streamingMessage.role,
                    content: streamingMessage.content,
                    tokens: parsed.tokens,
                    createdAt: streamingMessage.createdAt,
                    modelName: streamingMessage.modelName,
                    isStreaming: false
                  }

                  // Find and replace the streaming message in the array
                  const index = messages.value.findIndex(m => m === streamingMessage)
                  if (index !== -1) {
                    messages.value[index] = finalMessage

                    // Temporarily save to localStorage cache for immediate UI update
                    if (currentChat.value?.id) {
                      saveChatToCache(currentChat.value.id, messages.value)
                    }

                    // Sync with H2 database after a short delay to ensure backend saved the message
                    // DB is the source of truth - this ensures cache stays synchronized
                    setTimeout(() => {
                      if (currentChat.value?.id) {
                        console.log('[Cache-Sync] Syncing with H2 database...')
                        loadChatHistory(currentChat.value.id)
                      }
                    }, 500) // 500ms delay to let backend finish saving
                  }

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
      // H2 Database is the source of truth
      const chat = await api.getChatHistory(chatId)
      currentChat.value = chat
      messages.value = chat.messages || []

      // Sync localStorage cache with database (DB wins in case of conflicts)
      saveChatToCache(chatId, messages.value)
      console.log('[Cache-Sync] Chat history loaded from H2 DB and synchronized with localStorage')
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

      // Remove from localStorage cache to keep it synchronized with H2 DB
      try {
        const cache = JSON.parse(localStorage.getItem(CHAT_CACHE_KEY) || '{}')
        delete cache[chatId]
        localStorage.setItem(CHAT_CACHE_KEY, JSON.stringify(cache))
        console.log('[Cache-Sync] Removed deleted chat from localStorage cache')
      } catch (e) {
        console.error('Failed to remove chat from cache:', e)
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

  async function setSelectedModel(model) {
    selectedModel.value = model

    // Save to database for persistence
    try {
      await api.saveSelectedModel(model)
      console.log('💾 Saved selected model to database:', model)
    } catch (e) {
      console.error('Failed to save model to database', e)
    }

    // Automatically update current chat's model when a new model is selected
    if (currentChat.value && currentChat.value.id) {
      await updateCurrentChatModel(model)
    }
  }

  async function updateCurrentChatModel(modelName) {
    if (!currentChat.value || !currentChat.value.id) {
      console.warn('No current chat to update model')
      return
    }

    try {
      console.log(`🔄 Updating chat ${currentChat.value.id} model to: ${modelName}`)
      const response = await api.updateChatModel(currentChat.value.id, modelName)

      // Update local state
      currentChat.value.model = modelName

      // Update in chat list
      const chatIndex = chats.value.findIndex(c => c.id === currentChat.value.id)
      if (chatIndex !== -1) {
        chats.value[chatIndex].model = modelName
      }

      console.log('✅ Chat model updated successfully')
      return response
    } catch (err) {
      console.error('Failed to update chat model:', err)
      throw err
    }
  }

  function setSystemPrompt(prompt, title = null) {
    systemPrompt.value = prompt
    systemPromptTitle.value = title

    // Sync with localStorage immediately
    localStorage.setItem(PROMPT_STORAGE_KEY, JSON.stringify({
      content: prompt,
      title: title
    }))
    console.log('✅ System prompt saved to localStorage:', title)
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
    updateCurrentChatModel,
    setSystemPrompt,
    toggleStreaming,
    startNewChat,
    abortCurrentRequest
  }
})
