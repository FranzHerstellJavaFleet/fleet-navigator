import { defineStore } from 'pinia'
import { ref, computed, reactive, watch } from 'vue'
import api from '../services/api'
import { secureFetch } from '../utils/secureFetch'
import { useSettingsStore } from './settingsStore'

const PROMPT_STORAGE_KEY = 'fleet-navigator-last-prompt'
const MODEL_STORAGE_KEY = 'fleet-navigator-selected-model'
const CHAT_CACHE_KEY = 'fleet-navigator-chat-cache'
const SAMPLING_PARAMS_KEY = 'defaultSamplingParams'

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

  // Load sampling parameters from localStorage
  const loadSamplingParams = () => {
    try {
      const stored = localStorage.getItem(SAMPLING_PARAMS_KEY)
      if (stored) {
        return JSON.parse(stored)
      }
    } catch (e) {
      console.error('Failed to load sampling params', e)
    }
    return null
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
  const isWebSearching = ref(false)  // True wenn gerade Web-Suche läuft
  const error = ref(null)
  const models = ref([])
  const experts = ref([])  // Loaded experts for model selection
  const selectedModel = ref(loadLastModel()) // Load from localStorage
  const selectedExpertId = ref(null)  // Currently selected expert (null = regular model)
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

  // Context usage tracking for current chat (for progressbar)
  const contextUsage = ref({
    totalChatTokens: 0,
    maxContextTokens: null  // null = kein Experte ausgewählt
  })

  // Custom models from database (for detecting when to skip system prompt)
  const customModels = ref([])

  // System status
  const systemStatus = ref({
    cpuUsage: 0,
    totalMemory: 0,
    freeMemory: 0,
    usedMemory: 0
  })

  // Helper function: Check if model is a custom model
  // Simple rule: If it's in the custom_models database, it's custom
  const isCustomModel = (modelName) => {
    if (!modelName) return false

    // Check if model exists in custom models database
    return customModels.value.some(cm =>
      cm.name === modelName ||
      cm.name.toLowerCase() === modelName.toLowerCase()
    )
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

      // Load custom models from database (for system prompt detection)
      try {
        const customModelsData = await api.getAllCustomModels()
        customModels.value = customModelsData || []
        console.log(`📦 Loaded ${customModels.value.length} custom model configurations from database`)
      } catch (e) {
        console.warn('⚠️ Could not load custom models:', e.message)
        customModels.value = []
      }

      // Load experts for model selection
      try {
        const expertsData = await api.getAllExperts()
        experts.value = expertsData || []
        console.log(`🎓 Loaded ${experts.value.length} experts for model selection`)
      } catch (e) {
        console.warn('⚠️ Could not load experts:', e.message)
        experts.value = []
      }

      // Load user's last selected expert from database (do this FIRST, before model)
      try {
        const savedExpertId = await api.getSelectedExpert()
        if (savedExpertId) {
          // Find the expert in loaded experts (use Number() for type-safe comparison)
          const numId = Number(savedExpertId)
          const expert = experts.value.find(e => Number(e.id) === numId)
          if (expert) {
            // Restore expert selection (sets model, system prompt, etc.)
            selectedExpertId.value = expert.id
            selectedModel.value = expert.baseModel
            // Use combined prompt (basePrompt + personalityPrompt)
            let combinedPrompt = expert.basePrompt
            if (expert.personalityPrompt && expert.personalityPrompt.trim()) {
              combinedPrompt = `${expert.basePrompt}\n\n## Kommunikationsstil:\n${expert.personalityPrompt}`
            }
            systemPrompt.value = combinedPrompt
            systemPromptTitle.value = `🎓 ${expert.name}`
            console.log(`🎓 Restored selected expert from database: ${expert.name} (${expert.role})`)
          } else {
            console.warn(`⚠️ Saved expert ID ${savedExpertId} not found in experts list`)
          }
        }
      } catch (e) {
        console.warn('⚠️ Could not load selected expert:', e.message)
      }

      // Load user's last selected model from database (only if no expert selected)
      if (!selectedExpertId.value) {
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
      }

      // ALWAYS sync system prompt with database (localStorage = cache, DB = source of truth)
      // Only if no expert selected (experts have their own prompts)
      if (!selectedExpertId.value) {
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
      } else {
        console.log('🎓 Skipping system prompt sync - expert selected')
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
        model: selectedModel.value,
        expertId: selectedExpertId.value  // Speichere aktuellen Experten mit dem Chat
      })
      chats.value.unshift(chat)
      currentChat.value = chat
      messages.value = []

      // Initialize empty cache for new chat
      saveChatToCache(chat.id, [])
      console.log('[Cache-Sync] Initialized cache for new chat with expertId:', selectedExpertId.value)

      return chat
    } catch (err) {
      error.value = 'Failed to create chat'
      console.error(err)
    } finally {
      isLoading.value = false
    }
  }

  async function sendMessage(messageData) {
    // Handle both string (legacy) and object with files and webSearchEnabled
    const messageText = typeof messageData === 'string' ? messageData : messageData.text
    const files = typeof messageData === 'object' ? messageData.files || [] : []
    const webSearchEnabled = typeof messageData === 'object' ? messageData.webSearchEnabled || false : false

    if (!messageText.trim()) return

    try {
      isLoading.value = true
      isWebSearching.value = webSearchEnabled  // Track wenn Web-Suche aktiv
      error.value = null

      // Process uploaded files
      const images = []
      let documentContext = ''
      const fileMetadata = []

      for (const file of files) {
        // Collect metadata for all files (for persistent storage)
        fileMetadata.push({
          name: file.name,
          type: file.type,
          size: file.size
        })

        if (file.type === 'image' && file.base64Content) {
          images.push(file.base64Content)
        } else if (file.textContent) {
          // All text-based files (pdf, text, json, xml, csv, html)
          documentContext += `\n\n=== ${file.name} ===\n${file.textContent}`
        }
      }

      // Automatisches Vision-Chaining: Wenn Bilder vorhanden → IMMER Vision Model aus Settings nutzen
      // Vision Model analysiert das Bild, Ergebnis geht an das ausgewählte Hauptmodell
      const settingsStore = useSettingsStore()
      let visionChainEnabled = false
      let visionModel = null

      if (images.length > 0) {
        const isCurrentModelVision = settingsStore.isVisionModel(selectedModel.value)

        // Automatisches Chaining: Vision Model → Hauptmodell (still, ohne UI-Feedback)
        if (!isCurrentModelVision) {
          visionChainEnabled = true
          visionModel = settingsStore.getSetting('preferredVisionModel')
        }
      }

      // Add user message optimistically (with attachments if present)
      const userMessage = {
        role: 'USER',
        content: messageText,
        createdAt: new Date().toISOString(),
        attachments: fileMetadata.length > 0 ? JSON.stringify(fileMetadata) : null
      }
      messages.value.push(userMessage)

      // Check if streaming is enabled
      if (streamingEnabled.value) {
        // Use streaming endpoint (pass webSearchEnabled for source URLs)
        await sendMessageStreaming(messageText, images, documentContext, visionChainEnabled, visionModel, fileMetadata, webSearchEnabled)
      } else {
        // Build request
        const settingsStore = useSettingsStore()

        // Load sampling parameters (from localStorage or defaults)
        const samplingParams = loadSamplingParams()

        // For custom models: don't send system prompt (they have their own built-in)
        const useCustomModelPrompt = isCustomModel(selectedModel.value)

        const request = {
          chatId: currentChat.value?.id,
          message: messageText,
          model: selectedModel.value,
          systemPrompt: useCustomModelPrompt ? null : systemPrompt.value,
          stream: false,
          expertId: selectedExpertId.value || null  // Expert ID für Backend (searchDomains Fallback)
        }

        if (useCustomModelPrompt) {
          console.log('🤖 Custom Model detected - skipping external system prompt')
        }

        // Add sampling parameters if available
        if (samplingParams && Object.keys(samplingParams).length > 0) {
          request.samplingParameters = samplingParams
        } else {
          // Fallback to deprecated parameters from settings
          request.maxTokens = settingsStore.settings.maxTokens
          request.temperature = settingsStore.settings.temperature
          request.topP = settingsStore.settings.topP
          request.topK = settingsStore.settings.topK
          request.repeatPenalty = settingsStore.settings.repeatPenalty
        }

        // CPU-Only Mode (deaktiviert CUDA/GPU für Demos auf Laptops ohne NVIDIA)
        if (settingsStore.settings.cpuOnly) {
          request.cpuOnly = true
          console.log('🖥️ CPU-Only Mode aktiviert - GPU/CUDA deaktiviert')
        }

        // Add images if present
        if (images.length > 0) {
          request.images = images
        }

        // Add document context if present
        if (documentContext.trim()) {
          request.documentContext = documentContext.trim()
        }

        // Add file metadata for persistent storage
        if (fileMetadata.length > 0) {
          request.fileMetadata = fileMetadata
        }

        // Web-Suche aktivieren (entweder durch Checkbox oder Experten-Einstellung)
        if (webSearchEnabled) {
          request.webSearchEnabled = true
          request.includeSourceUrls = true  // Quellen-URLs in Antwort einbauen
          request.maxSearchResults = 5
          console.log('🔍 Web-Suche mit Quellen aktiviert (Checkbox)')
          isWebSearching.value = true
        }

        // Add expert's web search settings (RAG)
        if (selectedExpertId.value) {
          const expert = getExpertById(selectedExpertId.value)
          if (expert && expert.autoWebSearch) {
            request.webSearchEnabled = true
            request.includeSourceUrls = true  // Quellen-URLs in Antwort einbauen
            request.maxSearchResults = expert.maxSearchResults || 5
            if (expert.searchDomains) {
              request.searchDomains = expert.searchDomains.split(',').map(d => d.trim()).filter(d => d)
            }
            console.log(`🔍 Web-Suche aktiviert für Experte "${expert.name}"`, {
              domains: request.searchDomains,
              maxResults: request.maxSearchResults
            })
            isWebSearching.value = true
          }
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
          createdAt: new Date().toISOString(),
          downloadUrl: response.downloadUrl  // Add download URL if present
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
      isWebSearching.value = false
      currentRequestId.value = null
    }
  }

  async function sendMessageStreaming(messageText, images = [], documentContext = '', visionChainEnabled = false, visionModel = null, fileMetadata = [], webSearchEnabled = false) {
    return new Promise((resolve, reject) => {
      // Construct SSE endpoint URL
      const baseURL = '/api/chat/send-stream'

      const settingsStore = useSettingsStore()

      // Load sampling parameters (from localStorage or defaults)
      const samplingParams = loadSamplingParams()

      // DEBUG: Log settings before sending
      console.log('🔍 Sampling Params:', samplingParams)

      // Create EventSource with POST body (using fetch to send body, then EventSource for reading)
      // For custom models: don't send system prompt (they have their own built-in)
      const useCustomModelPrompt = isCustomModel(selectedModel.value)

      const requestBody = {
        chatId: currentChat.value?.id,
        message: messageText,
        model: selectedModel.value,
        systemPrompt: useCustomModelPrompt ? null : systemPrompt.value,
        stream: true,
        expertId: selectedExpertId.value || null  // Expert ID für Backend (searchDomains Fallback)
      }

      if (useCustomModelPrompt) {
        console.log('🤖 Custom Model detected - skipping external system prompt')
      }

      // Add sampling parameters if available
      if (samplingParams && Object.keys(samplingParams).length > 0) {
        requestBody.samplingParameters = samplingParams
      } else {
        // Fallback to deprecated parameters from settings
        requestBody.maxTokens = settingsStore.settings.maxTokens
        requestBody.temperature = settingsStore.settings.temperature
        requestBody.topP = settingsStore.settings.topP
        requestBody.topK = settingsStore.settings.topK
        requestBody.repeatPenalty = settingsStore.settings.repeatPenalty
      }

      // CPU-Only Mode (deaktiviert CUDA/GPU für Demos auf Laptops ohne NVIDIA)
      if (settingsStore.settings.cpuOnly) {
        requestBody.cpuOnly = true
        console.log('🖥️ CPU-Only Mode aktiviert - GPU/CUDA deaktiviert')
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

      // Add file metadata for persistent storage
      if (fileMetadata.length > 0) {
        requestBody.fileMetadata = fileMetadata
      }

      // Add Vision-Chaining parameters
      if (visionChainEnabled && visionModel) {
        requestBody.visionChainEnabled = true
        requestBody.visionModel = visionModel

        // Load chaining settings to check showIntermediateOutput
        try {
          const chainingSettingsStr = localStorage.getItem('chainingSettings')
          if (chainingSettingsStr) {
            const chainingSettings = JSON.parse(chainingSettingsStr)
            requestBody.showIntermediateOutput = chainingSettings.showIntermediateOutput || false
            console.log('🔗 Chaining Settings:', chainingSettings)
          }
        } catch (e) {
          console.warn('Failed to load chaining settings', e)
          requestBody.showIntermediateOutput = false
        }

        // Erzwinge deutsche Ausgabe im System-Prompt (nur wenn kein Custom Model)
        if (!isCustomModel(selectedModel.value)) {
          const deutschPrompt = 'Du antwortest IMMER auf Deutsch.'
          requestBody.systemPrompt = requestBody.systemPrompt
            ? deutschPrompt + '\n\n' + requestBody.systemPrompt
            : deutschPrompt
        }

        // Vision-Chaining läuft automatisch im Hintergrund (kein UI-Feedback)
      }

      // Web-Suche aktivieren (Checkbox oder Experten-Einstellung)
      if (webSearchEnabled) {
        requestBody.webSearchEnabled = true
        requestBody.includeSourceUrls = true  // Quellen-URLs in Antwort einbauen
        requestBody.maxSearchResults = 5
        console.log('🔍 Web-Suche mit Quellen aktiviert (Checkbox, Streaming)')
        isWebSearching.value = true
      }

      // Add expert's web search settings (RAG)
      if (selectedExpertId.value) {
        const expert = getExpertById(selectedExpertId.value)
        if (expert && expert.autoWebSearch) {
          requestBody.webSearchEnabled = true
          requestBody.includeSourceUrls = true  // Quellen-URLs in Antwort einbauen
          requestBody.maxSearchResults = expert.maxSearchResults || 5
          if (expert.searchDomains) {
            requestBody.searchDomains = expert.searchDomains.split(',').map(d => d.trim()).filter(d => d)
          }
          console.log(`🔍 Web-Suche aktiviert für Experte "${expert.name}" (Streaming)`, {
            domains: requestBody.searchDomains,
            maxResults: requestBody.maxSearchResults
          })
          isWebSearching.value = true
        }
      }

      // We need to use fetch for POST with body, then read SSE
      // Using secureFetch to include CSRF token
      secureFetch(baseURL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody)
      }).then(async response => {
        if (!response.ok) {
          // Parse error response for better user feedback
          let errorMessage = `Fehler ${response.status}`
          let errorDetails = ''

          try {
            const errorBody = await response.text()
            console.error('[LLM Error] Response:', response.status, errorBody)

            // Parse JSON error if possible
            try {
              const errorJson = JSON.parse(errorBody)
              if (errorJson.error) {
                errorDetails = errorJson.error
              }
            } catch (e) {
              errorDetails = errorBody.substring(0, 200)
            }
          } catch (e) {
            console.error('[LLM Error] Could not read error body')
          }

          // Provide user-friendly error messages based on status code
          switch (response.status) {
            case 400:
              errorMessage = 'Die Anfrage konnte nicht verarbeitet werden. '
              if (errorDetails.toLowerCase().includes('context') || errorDetails.toLowerCase().includes('token')) {
                errorMessage += 'Der Chat-Verlauf ist zu lang. Bitte starte einen neuen Chat oder lösche einige Nachrichten.'
              } else if (errorDetails.toLowerCase().includes('model')) {
                errorMessage += 'Das gewählte Modell ist nicht verfügbar.'
              } else {
                errorMessage += 'Mögliche Ursache: Chat-Kontext zu groß oder LLM-Server-Problem.'
              }
              break
            case 500:
              errorMessage = 'Server-Fehler beim KI-Modell. Der LLM-Server ist möglicherweise überlastet oder abgestürzt. Bitte versuche es erneut.'
              break
            case 502:
            case 503:
            case 504:
              errorMessage = 'Der KI-Server ist gerade nicht erreichbar. Bitte warte einen Moment und versuche es erneut.'
              break
            case 401:
            case 403:
              errorMessage = 'Nicht autorisiert. Bitte melde dich erneut an.'
              break
            default:
              errorMessage = `Unerwarteter Fehler (${response.status}). ${errorDetails || 'Bitte versuche es erneut.'}`
          }

          // Set the user-visible error
          error.value = errorMessage

          throw new Error(errorMessage)
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
            isWebSearching.value = false
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
                  console.log('[SSE] Start event - chatId:', parsed.chatId, 'isDocumentRequest:', parsed.isDocumentRequest, 'documentType:', parsed.documentType)
                  if (!currentChat.value) {
                    currentChat.value = { id: parsed.chatId }
                    loadChats()
                  }
                  currentRequestId.value = parsed.requestId
                  // Mark as document request so MessageBubble can show card immediately
                  if (parsed.isDocumentRequest) {
                    streamingMessage.isDocumentRequest = true
                    streamingMessage.documentType = parsed.documentType // PDF, ODT, DOCX
                  }
                } else if (parsed.tokens !== undefined) {
                  // Done event - replace reactive object with plain object to trigger re-render
                  const finalMessage = {
                    role: streamingMessage.role,
                    content: streamingMessage.content,
                    tokens: parsed.tokens,
                    createdAt: streamingMessage.createdAt,
                    modelName: streamingMessage.modelName,
                    isStreaming: false,
                    isDocumentRequest: streamingMessage.isDocumentRequest || false,  // Keep document flag
                    documentType: streamingMessage.documentType || null,  // PDF, ODT, DOCX
                    downloadUrl: parsed.downloadUrl || null  // Document download URL if generated
                  }

                  // Update context usage for progressbar
                  if (parsed.totalChatTokens !== undefined) {
                    contextUsage.value.totalChatTokens = parsed.totalChatTokens
                    contextUsage.value.maxContextTokens = parsed.maxContextTokens || null
                    console.log(`📊 Context: ${parsed.totalChatTokens.toLocaleString()}${parsed.maxContextTokens ? ` / ${parsed.maxContextTokens.toLocaleString()}` : ''} Tokens`)
                  }

                  if (parsed.downloadUrl) {
                    console.log('📄 Document generated:', parsed.downloadUrl)
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
                    // WICHTIG: Wir laden nur die Nachrichten, NICHT den Experten ändern!
                    setTimeout(async () => {
                      if (currentChat.value?.id) {
                        console.log('[Cache-Sync] Syncing messages with H2 database (keeping expert)...')
                        try {
                          const chat = await api.getChatHistory(currentChat.value.id)
                          messages.value = chat.messages || []
                          saveChatToCache(currentChat.value.id, messages.value)
                          // Experte bleibt unverändert!
                        } catch (e) {
                          console.error('[Cache-Sync] Failed to sync messages:', e)
                        }
                      }
                    }, 500) // 500ms delay to let backend finish saving

                    // Second sync for Fleet-Mate documents (they need more time for document_generated response)
                    // Fleet-Mate creates the file locally and sends back the real path
                    if (finalMessage.downloadUrl?.startsWith('fleet-mate://')) {
                      setTimeout(async () => {
                        if (currentChat.value?.id) {
                          console.log('[Cache-Sync] Fleet-Mate document sync (keeping expert)...')
                          try {
                            const chat = await api.getChatHistory(currentChat.value.id)
                            messages.value = chat.messages || []
                            saveChatToCache(currentChat.value.id, messages.value)
                          } catch (e) {
                            console.error('[Cache-Sync] Failed to sync messages:', e)
                          }
                        }
                      }, 3000) // 3s delay for Fleet-Mate to finish document creation
                    }
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
            isWebSearching.value = false
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

      // Restore expert if chat has one saved
      if (chat.expertId) {
        // Use == for comparison to handle number/string type differences
        const chatExpertId = Number(chat.expertId)
        const expert = experts.value.find(e => Number(e.id) === chatExpertId)

        console.log(`🔍 Looking for expert ID ${chat.expertId} (type: ${typeof chat.expertId})`)
        console.log(`🔍 Available experts:`, experts.value.map(e => ({ id: e.id, type: typeof e.id, name: e.name })))

        if (expert) {
          // Restore expert without saving to DB again (it's already there)
          selectedExpertId.value = expert.id
          selectedModel.value = expert.baseModel

          // Set system prompt to expert's combined prompt
          let combinedPrompt = expert.basePrompt
          if (expert.personalityPrompt && expert.personalityPrompt.trim()) {
            combinedPrompt = `${expert.basePrompt}\n\n## Kommunikationsstil:\n${expert.personalityPrompt}`
          }
          systemPrompt.value = combinedPrompt
          systemPromptTitle.value = `🎓 ${expert.name}`

          console.log(`🎓 Restored expert from chat: ${expert.name} (ID: ${expert.id})`)
        } else {
          console.warn(`Expert with ID ${chat.expertId} not found in loaded experts (searched ${experts.value.length} experts)`)
          // Expert not found - clear expert selection and restore default prompt
          selectedExpertId.value = null
          try {
            const defaultPrompt = await api.getDefaultSystemPrompt()
            if (defaultPrompt && defaultPrompt.content) {
              systemPrompt.value = defaultPrompt.content
              systemPromptTitle.value = defaultPrompt.name || 'Standard'
            }
          } catch (e) {
            console.error('Failed to load default system prompt', e)
          }
        }
      } else {
        // Chat has no expert - clear expert selection and restore default prompt
        // This ensures that when switching from an expert chat to a non-expert chat,
        // the UI shows the correct state (no expert selected)
        if (selectedExpertId.value !== null) {
          console.log('[Chat] Clearing expert selection for non-expert chat')
          selectedExpertId.value = null
          // Restore default system prompt
          try {
            const defaultPrompt = await api.getDefaultSystemPrompt()
            if (defaultPrompt && defaultPrompt.content) {
              systemPrompt.value = defaultPrompt.content
              systemPromptTitle.value = defaultPrompt.name || 'Standard'
              console.log('🔄 Restored default system prompt:', defaultPrompt.name)
            }
          } catch (e) {
            console.error('Failed to load default system prompt', e)
            systemPrompt.value = 'Du bist ein hilfreicher Assistent.'
            systemPromptTitle.value = 'Standard'
          }
        } else {
          console.log('[Chat] No expert associated with this chat (already cleared)')
        }
      }

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
      console.log('[Chat-Delete] Deleting chat:', chatId, 'type:', typeof chatId)
      console.log('[Chat-Delete] Current chats before delete:', chats.value.map(c => ({ id: c.id, type: typeof c.id })))

      await api.deleteChat(chatId)

      // Use Number for type-safe comparison (chatId could be string or number from various sources)
      const chatIdNum = Number(chatId)

      // Create a NEW array to ensure Vue reactivity triggers
      const filteredChats = chats.value.filter(c => {
        const cId = Number(c.id)
        const keep = cId !== chatIdNum
        console.log(`[Chat-Delete] Chat ${c.id} (${cId}) vs ${chatIdNum}: keep=${keep}`)
        return keep
      })

      console.log('[Chat-Delete] Filtered chats:', filteredChats.length, 'from', chats.value.length)

      // Assign the new array (triggers Vue reactivity)
      chats.value = filteredChats

      // Clear current chat if it was the deleted one
      if (currentChat.value && Number(currentChat.value.id) === chatIdNum) {
        currentChat.value = null
        messages.value = []
        console.log('[Chat-Delete] Cleared current chat')
      }

      console.log('[Chat-Delete] Success! Remaining chats:', chats.value.length)

      // Remove from localStorage cache to keep it synchronized with H2 DB
      try {
        const cache = JSON.parse(localStorage.getItem(CHAT_CACHE_KEY) || '{}')
        delete cache[chatId]
        delete cache[String(chatIdNum)]
        delete cache[chatIdNum]
        localStorage.setItem(CHAT_CACHE_KEY, JSON.stringify(cache))
        console.log('[Cache-Sync] Removed deleted chat from localStorage cache')
      } catch (e) {
        console.error('Failed to remove chat from cache:', e)
      }
    } catch (err) {
      error.value = 'Failed to delete chat'
      console.error('[Chat-Delete] ERROR:', err)
      throw err // Re-throw so the caller knows it failed
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
    selectedExpertId.value = null  // Clear expert when regular model selected

    // Reset system prompt to default when switching from expert to regular model
    try {
      const defaultPrompt = await api.getDefaultSystemPrompt()
      if (defaultPrompt && defaultPrompt.content) {
        systemPrompt.value = defaultPrompt.content
        systemPromptTitle.value = defaultPrompt.name || 'Standard'
        console.log('🔄 Reset system prompt to default:', defaultPrompt.name)
      } else {
        // Fallback if no default prompt exists
        systemPrompt.value = 'Du bist ein hilfreicher Assistent.'
        systemPromptTitle.value = 'Standard'
        console.log('🔄 Reset system prompt to fallback default')
      }
    } catch (e) {
      console.error('Failed to load default system prompt', e)
      systemPrompt.value = 'Du bist ein hilfreicher Assistent.'
      systemPromptTitle.value = 'Standard'
    }

    // Save to database for persistence
    try {
      await api.saveSelectedModel(model)
      await api.saveSelectedExpert(null)  // Clear expert in DB
      console.log('💾 Saved selected model to database:', model)
    } catch (e) {
      console.error('Failed to save model to database', e)
    }

    // Automatically update current chat's model AND clear expert when a new model is selected
    if (currentChat.value && currentChat.value.id) {
      await updateCurrentChatModel(model)
      // Clear expert from chat when switching to regular model
      try {
        await api.updateChatExpert(currentChat.value.id, null)
        console.log('🎓 Cleared expert from current chat (switched to regular model)')
      } catch (e) {
        console.error('Failed to clear chat expert', e)
      }
    }
  }

  // Select an expert - sets model and system prompt automatically
  async function selectExpert(expert) {
    if (!expert) {
      selectedExpertId.value = null
      // Clear expert in DB but keep model
      try {
        await api.saveSelectedExpert(null)
        // Also clear expert from current chat
        if (currentChat.value && currentChat.value.id) {
          await api.updateChatExpert(currentChat.value.id, null)
          console.log('🎓 Cleared expert from current chat')
        }
      } catch (e) {
        console.error('Failed to clear expert in database', e)
      }
      return
    }

    selectedExpertId.value = expert.id
    selectedModel.value = expert.baseModel

    // Set system prompt to expert's combined prompt (basePrompt + personalityPrompt)
    let combinedPrompt = expert.basePrompt
    if (expert.personalityPrompt && expert.personalityPrompt.trim()) {
      combinedPrompt = `${expert.basePrompt}\n\n## Kommunikationsstil:\n${expert.personalityPrompt}`
    }
    systemPrompt.value = combinedPrompt
    systemPromptTitle.value = `🎓 ${expert.name}`

    console.log(`🎓 Selected expert: ${expert.name} (${expert.role}) using model: ${expert.baseModel}`)

    // Save expert AND model to database (global selection)
    try {
      await api.saveSelectedExpert(expert.id)
      await api.saveSelectedModel(expert.baseModel)
      console.log(`💾 Saved selected expert to database: ${expert.name} (ID: ${expert.id})`)
    } catch (e) {
      console.error('Failed to save expert to database', e)
    }

    // Update current chat's model AND expert
    if (currentChat.value && currentChat.value.id) {
      await updateCurrentChatModel(expert.baseModel)
      try {
        await api.updateChatExpert(currentChat.value.id, expert.id)
        console.log(`💾 Updated current chat expert to: ${expert.name}`)
      } catch (e) {
        console.error('Failed to update chat expert', e)
      }
    }
  }

  // Get expert by ID (uses Number() for type-safe comparison)
  function getExpertById(expertId) {
    if (!expertId) return null
    const numId = Number(expertId)
    return experts.value.find(e => Number(e.id) === numId) || null
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

  /**
   * Clear the current error message.
   * Called when user dismisses the error banner.
   */
  function clearError() {
    error.value = null
  }

  return {
    // State
    chats,
    currentChat,
    messages,
    isLoading,
    isWebSearching,
    error,
    models,
    experts,
    selectedModel,
    selectedExpertId,
    systemPrompt,
    systemPromptTitle,
    streamingEnabled,
    globalStats,
    systemStatus,
    currentRequestId,
    customModels,
    contextUsage,  // Token-Zählung für Context-Progressbar

    // Computed
    currentChatTokens,
    memoryUsagePercent,

    // Helper Functions
    isCustomModel,
    getExpertById,

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
    selectExpert,
    updateCurrentChatModel,
    setSystemPrompt,
    toggleStreaming,
    startNewChat,
    abortCurrentRequest,
    clearError
  }
})
