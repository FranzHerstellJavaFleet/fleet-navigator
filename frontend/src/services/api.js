import axios from 'axios'
import { parseApiError } from './errorHandler'

const api = axios.create({
  baseURL: '/api',
  timeout: 300000, // 5 minutes for long LLM responses
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true // wichtig für Cookies (Session + CSRF)
})

// Helper: CSRF-Token aus Cookie lesen
function getCsrfToken() {
  const token = document.cookie
    .split('; ')
    .find(row => row.startsWith('XSRF-TOKEN='))
    ?.split('=')[1]
  return token ? decodeURIComponent(token) : null
}

// CSRF-Token aus Cookie lesen und bei jedem Request mitsenden
api.interceptors.request.use((config) => {
  const csrfToken = getCsrfToken()
  if (csrfToken) {
    config.headers['X-XSRF-TOKEN'] = csrfToken
  }
  return config
})

// Response-Interceptor für strukturierte Fehlerverarbeitung
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Parse den Fehler für bessere Debugging-Informationen
    const parsed = parseApiError(error)

    // Füge die geparsten Infos zum Error hinzu
    error.parsedError = parsed

    // Bei 401 (Unauthorized) → Redirect zu Login
    if (error.response?.status === 401) {
      // Nur weiterleiten wenn wir nicht bereits auf der Login-Seite sind
      if (!window.location.pathname.includes('/login')) {
        console.warn('Session abgelaufen, weiterleitung zu Login...')
        // Optional: Event dispatchen für Auth-Store
        window.dispatchEvent(new CustomEvent('auth:session-expired'))
      }
    }

    return Promise.reject(error)
  }
)

// Helper für fetch mit CSRF-Token (für Streaming-Endpoints)
function fetchWithCsrf(url, options = {}) {
  const csrfToken = getCsrfToken()
  const headers = {
    ...options.headers,
    ...(csrfToken && { 'X-XSRF-TOKEN': csrfToken })
  }
  return fetch(url, { ...options, headers, credentials: 'include' })
}

export default {
  // Chat endpoints
  async sendMessage(request) {
    const response = await api.post('/chat/send', request)
    return response.data
  },

  async createNewChat(request) {
    const response = await api.post('/chat/new', request)
    return response.data
  },

  async getChatHistory(chatId) {
    const response = await api.get(`/chat/history/${chatId}`)
    return response.data
  },

  async getAllChats() {
    const response = await api.get('/chat/all')
    return response.data
  },

  async renameChat(chatId, newTitle) {
    const response = await api.patch(`/chat/${chatId}/rename`, { newTitle })
    return response.data
  },

  async updateChatModel(chatId, modelName) {
    const response = await api.patch(`/chat/${chatId}/model`, { model: modelName })
    return response.data
  },

  async updateChatExpert(chatId, expertId) {
    const response = await api.patch(`/chat/${chatId}/expert`, { expertId })
    return response.data
  },

  async deleteChat(chatId) {
    await api.delete(`/chat/${chatId}`)
  },

  // Model endpoints
  async getAvailableModels() {
    const response = await api.get('/models')
    return response.data
  },

  async deleteModel(modelName) {
    const response = await api.delete(`/models/${encodeURIComponent(modelName)}`)
    return response.data
  },

  async getModelDetails(modelName) {
    const response = await api.get(`/models/${encodeURIComponent(modelName)}/details`)
    return response.data
  },

  async setDefaultModel(modelName) {
    const response = await api.post(`/models/${encodeURIComponent(modelName)}/default`)
    return response.data
  },

  async updateModelMetadata(modelName, metadata) {
    const response = await api.put(`/models/${encodeURIComponent(modelName)}/metadata`, metadata)
    return response.data
  },

  async getDefaultModel() {
    const response = await api.get('/models/default')
    return response.data
  },


  async pullModel(modelName, progressCallback) {
    return new Promise((resolve, reject) => {
      const eventSource = new EventSource(`/api/models/pull?name=${encodeURIComponent(modelName)}`)

      // We need to use fetch for POST with body
      fetchWithCsrf('/api/models/pull', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ name: modelName })
      }).then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        function processChunk({ done, value }) {
          if (done) {
            resolve()
            return
          }

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          for (const line of lines) {
            if (line.startsWith('data:')) {
              const data = line.substring(5).trim()
              try {
                const parsed = JSON.parse(data)
                if (parsed.status) {
                  progressCallback(parsed.status)
                }
                if (parsed.error) {
                  reject(new Error(parsed.error))
                }
              } catch (e) {
                // Plain text
                progressCallback(data)
              }
            }
          }

          reader.read().then(processChunk).catch(reject)
        }

        reader.read().then(processChunk).catch(reject)
      }).catch(reject)
    })
  },

  // Stats endpoints
  async getGlobalStats() {
    const response = await api.get('/stats/global')
    return response.data
  },

  async getChatStats(chatId) {
    const response = await api.get(`/stats/chat/${chatId}`)
    return response.data
  },

  // System endpoints
  async getSystemStatus() {
    const response = await api.get('/system/status')
    return response.data
  },

  async getSystemHealth() {
    const response = await api.get('/system/health')
    return response.data
  },

  // Abort request
  async abortRequest(requestId) {
    const response = await api.post(`/chat/abort/${requestId}`)
    return response.data
  },

  // File upload (uses configured 'api' instance for CSRF token)
  async uploadFile(file) {
    const formData = new FormData()
    formData.append('file', file)

    const response = await api.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 60000 // 1 minute for file uploads
    })

    return response.data
  },

  // System Prompt Templates
  async getSystemPrompts() {
    const response = await api.get('/system-prompts')
    return response
  },

  async getAllSystemPrompts() {
    const response = await api.get('/system-prompts')
    return response.data
  },

  async getDefaultSystemPrompt() {
    const response = await api.get('/system-prompts/default')
    return response.data
  },

  async createSystemPrompt(prompt) {
    const response = await api.post('/system-prompts', prompt)
    return response.data
  },

  async updateSystemPrompt(id, prompt) {
    const response = await api.put(`/system-prompts/${id}`, prompt)
    return response.data
  },

  async deleteSystemPrompt(id) {
    await api.delete(`/system-prompts/${id}`)
  },

  async setDefaultSystemPrompt(id) {
    const response = await api.put(`/system-prompts/${id}/set-default`)
    return response.data
  },

  async initDefaultPrompts() {
    const response = await api.post('/system-prompts/init-defaults')
    return response.data
  },

  // Project endpoints
  async getAllProjects() {
    const response = await api.get('/projects')
    return response.data
  },

  async getProject(projectId) {
    const response = await api.get(`/projects/${projectId}`)
    return response.data
  },

  async createProject(project) {
    const response = await api.post('/projects', project)
    return response.data
  },

  async updateProject(projectId, project) {
    const response = await api.put(`/projects/${projectId}`, project)
    return response.data
  },

  async deleteProject(projectId) {
    await api.delete(`/projects/${projectId}`)
  },

  // Context file endpoints
  async uploadContextFile(file) {
    const response = await api.post('/projects/context-files', file)
    return response.data
  },

  async getContextFileContent(fileId) {
    const response = await api.get(`/projects/context-files/${fileId}/content`)
    return response.data
  },

  async deleteContextFile(fileId) {
    await api.delete(`/projects/context-files/${fileId}`)
  },

  // Project-Chat assignment endpoints
  async assignChatToProject(chatId, projectId) {
    const response = await api.put(`/projects/${projectId}/chats/${chatId}`)
    return response.data
  },

  async unassignChatFromProject(chatId) {
    const response = await api.delete(`/projects/chats/${chatId}`)
    return response.data
  },

  async getProjectChats(projectId) {
    const response = await api.get(`/projects/${projectId}/chats`)
    return response.data
  },

  // Settings endpoints
  async getModelSelectionSettings() {
    const response = await api.get('/settings/model-selection')
    return response.data
  },

  async updateModelSelectionSettings(settings) {
    const response = await api.put('/settings/model-selection', settings)
    return response.data
  },

  async getEmailModel() {
    const response = await api.get('/settings/email-model')
    return response.data
  },

  async updateEmailModel(modelName) {
    await api.post('/settings/email-model', modelName, {
      headers: { 'Content-Type': 'text/plain' }
    })
  },

  async getLogAnalysisModel() {
    const response = await api.get('/settings/log-analysis-model')
    return response.data
  },

  async updateLogAnalysisModel(modelName) {
    await api.post('/settings/log-analysis-model', modelName, {
      headers: { 'Content-Type': 'text/plain' }
    })
  },

  async getDocumentModel() {
    const response = await api.get('/settings/document-model')
    return response.data
  },

  async updateDocumentModel(modelName) {
    await api.post('/settings/document-model', modelName, {
      headers: { 'Content-Type': 'text/plain' }
    })
  },

  // ============================================================================
  // DISTRIBUTED AGENTS
  // ============================================================================

  // Email Agent
  async getEmailAgentSettings() {
    const response = await api.get('/agents/email/settings')
    return response
  },

  async updateEmailAgentSettings(settings) {
    const response = await api.put('/agents/email/settings', settings)
    return response
  },

  async getEmailAgentStatus() {
    const response = await api.get('/agents/email/status')
    return response
  },

  // Document Agent
  async getDocumentAgentSettings() {
    const response = await api.get('/agents/document/settings')
    return response
  },

  async updateDocumentAgentSettings(settings) {
    const response = await api.put('/agents/document/settings', settings)
    return response
  },

  async getDocumentAgentStatus() {
    const response = await api.get('/agents/document/status')
    return response
  },

  // OS Agent
  async getOSAgentSettings() {
    const response = await api.get('/agents/os/settings')
    return response
  },

  async updateOSAgentSettings(settings) {
    const response = await api.put('/agents/os/settings', settings)
    return response
  },

  async getOSAgentStatus() {
    const response = await api.get('/agents/os/status')
    return response
  },

  // ============================================================================
  // USER SETTINGS
  // ============================================================================

  async getSelectedModel() {
    try {
      const response = await api.get('/settings/selected-model')
      return response.data
    } catch (error) {
      if (error.response && error.response.status === 204) {
        return null // No model saved yet
      }
      throw error
    }
  },

  async saveSelectedModel(modelName) {
    await api.post('/settings/selected-model', modelName, {
      headers: { 'Content-Type': 'text/plain' }
    })
  },

  async getSelectedExpert() {
    try {
      const response = await api.get('/settings/selected-expert')
      return response.data
    } catch (error) {
      if (error.response && error.response.status === 204) {
        return null // No expert saved yet
      }
      throw error
    }
  },

  async saveSelectedExpert(expertId) {
    await api.post('/settings/selected-expert', expertId ? String(expertId) : 'null', {
      headers: { 'Content-Type': 'text/plain' }
    })
  },

  // DANGER ZONE: Reset selected application data
  async resetSelectedData(selection) {
    await api.post('/settings/reset-selective', selection)
  },

  // ============================================================================
  // LETTER TEMPLATES
  // ============================================================================

  // Get all letter templates
  async getLetterTemplates() {
    const response = await api.get('/letter-templates')
    return response.data
  },

  // Get template by ID
  async getLetterTemplateById(id) {
    const response = await api.get(`/letter-templates/${id}`)
    return response.data
  },

  // Get templates by category
  async getLetterTemplatesByCategory(category) {
    const response = await api.get(`/letter-templates/category/${category}`)
    return response.data
  },

  // Search templates
  async searchLetterTemplates(searchTerm) {
    const response = await api.get(`/letter-templates/search?q=${encodeURIComponent(searchTerm)}`)
    return response.data
  },

  // Get all categories
  async getLetterTemplateCategories() {
    const response = await api.get('/letter-templates/categories')
    return response.data
  },

  // Create new template
  async createLetterTemplate(template) {
    const response = await api.post('/letter-templates', template)
    return response.data
  },

  // Update template
  async updateLetterTemplate(id, template) {
    const response = await api.put(`/letter-templates/${id}`, template)
    return response.data
  },

  // Delete template
  async deleteLetterTemplate(id) {
    await api.delete(`/letter-templates/${id}`)
  },

  // Agents Overview
  async getAgentsOverview() {
    const response = await api.get('/agents/overview')
    return response
  },

  // Document Agent
  async generateDocument(request) {
    const response = await api.post('/agents/document/generate', request)
    return response.data
  },

  // Personal Info
  async getPersonalInfo() {
    const response = await api.get('/personal-info')
    return response.data
  },

  async savePersonalInfo(personalInfo) {
    const response = await api.put('/personal-info', personalInfo)
    return response.data
  },

  async deletePersonalInfo() {
    await api.delete('/personal-info')
  },

  // ============================================================================
  // CUSTOM MODELS
  // ============================================================================

  // Get all custom models
  async getAllCustomModels() {
    const response = await api.get('/custom-models')
    return response.data
  },

  // Get custom model by ID
  async getCustomModelById(id) {
    const response = await api.get(`/custom-models/${id}`)
    return response.data
  },

  // Get ancestry chain for a custom model
  async getCustomModelAncestry(id) {
    const response = await api.get(`/custom-models/${id}/ancestry`)
    return response.data
  },

  // Create custom model with streaming progress
  async createCustomModel(request, progressCallback) {
    return new Promise((resolve, reject) => {
      fetchWithCsrf('/api/custom-models', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request)
      }).then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        let modelId = null

        function processChunk({ done, value }) {
          if (done) {
            resolve({ modelId })
            return
          }

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          for (const line of lines) {
            if (line.startsWith('data:')) {
              const data = line.substring(5).trim()
              try {
                const parsed = JSON.parse(data)
                if (parsed.status) {
                  progressCallback(parsed.status)
                }
                if (parsed.modelId) {
                  modelId = parsed.modelId
                }
                if (parsed.error) {
                  reject(new Error(parsed.error))
                }
              } catch (e) {
                // Plain text
                progressCallback(data)
              }
            }
          }

          reader.read().then(processChunk).catch(reject)
        }

        reader.read().then(processChunk).catch(reject)
      }).catch(reject)
    })
  },

  // Update custom model (creates new version) with streaming
  async updateCustomModel(id, request, progressCallback) {
    return new Promise((resolve, reject) => {
      fetchWithCsrf(`/api/custom-models/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request)
      }).then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        let result = {}

        function processChunk({ done, value }) {
          if (done) {
            resolve(result)
            return
          }

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          for (const line of lines) {
            if (line.startsWith('data:')) {
              const data = line.substring(5).trim()
              try {
                const parsed = JSON.parse(data)
                if (parsed.status) {
                  progressCallback(parsed.status)
                }
                if (parsed.modelId) {
                  result.modelId = parsed.modelId
                }
                if (parsed.version) {
                  result.version = parsed.version
                }
                if (parsed.error) {
                  reject(new Error(parsed.error))
                }
              } catch (e) {
                // Plain text
                progressCallback(data)
              }
            }
          }

          reader.read().then(processChunk).catch(reject)
        }

        reader.read().then(processChunk).catch(reject)
      }).catch(reject)
    })
  },

  // Delete custom model
  async deleteCustomModel(id) {
    const response = await api.delete(`/custom-models/${id}`)
    return response.data
  },

  // Generate Modelfile preview
  async generateModelfilePreview(request) {
    const response = await api.post('/custom-models/generate-modelfile', request)
    return response.data
  },

  // LLM Provider endpoints
  async getActiveProvider() {
    const response = await api.get('/llm/providers/active')
    return response.data
  },

  async getProviderStatus() {
    const response = await api.get('/llm/providers')
    return response.data
  },

  async getProviderConfig() {
    const response = await api.get('/llm/providers/config')
    return response.data
  },

  async switchProvider(provider) {
    const response = await api.post('/llm/providers/switch', { provider })
    return response.data
  },

  // Ollama endpoints
  async getOllamaModels() {
    const response = await api.get('/ollama/models')
    return response.data
  },

  async getOllamaStatus() {
    const response = await api.get('/ollama/status')
    return response.data
  },

  // Model Store endpoints (for llama.cpp)
  async getAllModelStoreModels() {
    const response = await api.get('/model-store/all')
    return response.data
  },

  async getFeaturedModels() {
    const response = await api.get('/model-store/featured')
    return response.data
  },

  async getModelStoreDetails(modelId) {
    const response = await api.get(`/model-store/${modelId}`)
    return response.data
  },

  async isModelDownloaded(modelId) {
    const response = await api.get(`/model-store/${modelId}/downloaded`)
    return response.data
  },

  async deleteModelStoreModel(modelId) {
    const response = await api.delete(`/model-store/${modelId}`)
    return response.data
  },

  async cancelModelStoreDownload(modelId) {
    const response = await api.post(`/model-store/download/${modelId}/cancel`)
    return response.data
  },

  // HuggingFace Search & Discovery
  async searchHuggingFaceModels(query, limit = 50) {
    const response = await api.get('/model-store/huggingface/search', {
      params: { query, limit }
    })
    return response.data
  },

  async getHuggingFaceModelDetails(modelId) {
    const response = await api.get('/model-store/huggingface/details', {
      params: { modelId }
    })
    return response.data
  },

  async getPopularHuggingFaceModels(limit = 20) {
    const response = await api.get('/model-store/huggingface/popular', {
      params: { limit }
    })
    return response.data
  },

  async getGermanHuggingFaceModels(limit = 20) {
    const response = await api.get('/model-store/huggingface/german', {
      params: { limit }
    })
    return response.data
  },

  async getInstructHuggingFaceModels(limit = 30) {
    const response = await api.get('/model-store/huggingface/instruct', {
      params: { limit }
    })
    return response.data
  },

  async getCodeHuggingFaceModels(limit = 30) {
    const response = await api.get('/model-store/huggingface/code', {
      params: { limit }
    })
    return response.data
  },

  async getVisionHuggingFaceModels(limit = 20) {
    const response = await api.get('/model-store/huggingface/vision', {
      params: { limit }
    })
    return response.data
  },

  // GGUF Model Config endpoints
  async getAllGgufModelConfigs() {
    const response = await api.get('/gguf-models')
    return response.data
  },

  async getGgufModelConfig(id) {
    const response = await api.get(`/gguf-models/${id}`)
    return response.data
  },

  async getGgufModelConfigByName(name) {
    const response = await api.get(`/gguf-models/by-name/${encodeURIComponent(name)}`)
    return response.data
  },

  async createGgufModelConfig(config) {
    const response = await api.post('/gguf-models', config)
    return response.data
  },

  async updateGgufModelConfig(id, config) {
    const response = await api.put(`/gguf-models/${id}`, config)
    return response.data
  },

  async deleteGgufModelConfig(id) {
    await api.delete(`/gguf-models/${id}`)
  },

  async getDefaultGgufModelConfig() {
    const response = await api.get('/gguf-models/default')
    return response.data
  },

  async setGgufModelConfigAsDefault(id) {
    const response = await api.patch(`/gguf-models/${id}/set-default`)
    return response.data
  },

  async uploadGgufPrompt(formData) {
    const response = await api.post('/gguf-models/upload-prompt', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response.data
  },

  // ==================== EXPERT SYSTEM ENDPOINTS ====================

  // Expert CRUD
  async getAllExperts() {
    const response = await api.get('/experts')
    return response.data
  },

  async getExpertById(id) {
    const response = await api.get(`/experts/${id}`)
    return response.data
  },

  async createExpert(request) {
    const response = await api.post('/experts', request)
    return response.data
  },

  async updateExpert(id, request) {
    const response = await api.put(`/experts/${id}`, request)
    return response.data
  },

  async deleteExpert(id) {
    await api.delete(`/experts/${id}`)
  },

  // Expert Mode CRUD
  async getExpertModes(expertId) {
    const response = await api.get(`/experts/${expertId}/modes`)
    return response.data
  },

  async addExpertMode(expertId, request) {
    const response = await api.post(`/experts/${expertId}/modes`, request)
    return response.data
  },

  async updateExpertMode(modeId, request) {
    const response = await api.put(`/experts/modes/${modeId}`, request)
    return response.data
  },

  async deleteExpertMode(modeId) {
    await api.delete(`/experts/modes/${modeId}`)
  },

  // Ask Expert
  async askExpert(expertId, request) {
    const response = await api.post(`/experts/${expertId}/ask`, request)
    return response.data
  },

  async askExpertByName(name, request) {
    const response = await api.post(`/experts/name/${name}/ask`, request)
    return response.data
  },

  // ==================== LLAMA-SERVER STATUS ====================

  /**
   * Prüft den Health-Status des llama-servers
   * @param {number} port - Port des llama-servers (default: 2026)
   * @returns {Promise<{port: number, online: boolean, status: string}>}
   */
  async checkLlamaServerHealth(port = 2026) {
    const response = await api.get('/llm/providers/llama-server/health', {
      params: { port },
      timeout: 5000 // Kurzer Timeout für Health-Checks
    })
    return response.data
  },

  /**
   * Prüft den AI-Startup-Status (für Loading-Animation)
   * @returns {Promise<{inProgress: boolean, complete: boolean, message: string, error: string|null, serverOnline: boolean}>}
   */
  async getAiStartupStatus() {
    const response = await api.get('/system/ai-startup-status', {
      timeout: 5000
    })
    return response.data
  },

  // ==================== UI SETTINGS ====================

  /**
   * Lädt die showTopBar Einstellung aus der Datenbank
   * @returns {Promise<boolean>}
   */
  async getShowTopBar() {
    const response = await api.get('/settings/show-top-bar')
    return response.data
  },

  /**
   * Speichert die showTopBar Einstellung in der Datenbank
   * @param {boolean} show
   */
  async saveShowTopBar(show) {
    await api.post('/settings/show-top-bar', show)
  },

  /**
   * Lädt das UI Theme aus der Datenbank
   * @returns {Promise<string>} z.B. 'tech-dark', 'tech-light', 'lawyer-dark', 'lawyer-light'
   */
  async getUiTheme() {
    const response = await api.get('/settings/ui-theme')
    return response.data
  },

  /**
   * Speichert das UI Theme in der Datenbank
   * @param {string} theme z.B. 'tech-dark', 'tech-light', 'lawyer-dark', 'lawyer-light'
   */
  async saveUiTheme(theme) {
    await api.post('/settings/ui-theme', theme, {
      headers: { 'Content-Type': 'text/plain' }
    })
  },

  // ==================== AUTO-UPDATE ENDPOINTS ====================

  /**
   * Prüft den aktuellen Update-Status
   * @returns {Promise<{currentVersion, updateAvailable, downloadInProgress, downloadProgress, lastCheckTime, latestVersion, releaseName, releaseNotes, releaseUrl, downloadSize}>}
   */
  async getUpdateStatus() {
    const response = await api.get('/update/status')
    return response.data
  },

  /**
   * Prüft manuell auf Updates
   * @returns {Promise<{updateAvailable, currentVersion, updateInfo, message}>}
   */
  async checkForUpdates() {
    const response = await api.get('/update/check')
    return response.data
  },

  /**
   * Lädt das verfügbare Update herunter
   * @returns {Promise<{success, message, extractedPath}>}
   */
  async downloadUpdate() {
    const response = await api.post('/update/download')
    return response.data
  },

  /**
   * Installiert das heruntergeladene Update
   * @returns {Promise<{success, message, restartRequired}>}
   */
  async installUpdate() {
    const response = await api.post('/update/install')
    return response.data
  },

  /**
   * Holt den aktuellen Download-Fortschritt
   * @returns {Promise<{inProgress, progress}>}
   */
  async getUpdateProgress() {
    const response = await api.get('/update/progress')
    return response.data
  }
}
