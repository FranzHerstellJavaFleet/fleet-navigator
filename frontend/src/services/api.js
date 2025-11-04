import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 300000, // 5 minutes for long LLM responses
  headers: {
    'Content-Type': 'application/json'
  }
})

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

  async getOllamaLibraryModels() {
    const response = await api.get('/models/library')
    return response.data
  },

  async pullModel(modelName, progressCallback) {
    return new Promise((resolve, reject) => {
      const eventSource = new EventSource(`/api/models/pull?name=${encodeURIComponent(modelName)}`)

      // We need to use fetch for POST with body
      fetch('/api/models/pull', {
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

  // File upload
  async uploadFile(file) {
    const formData = new FormData()
    formData.append('file', file)

    const response = await axios.post('/api/files/upload', formData, {
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
      fetch('/api/custom-models', {
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
      fetch(`/api/custom-models/${id}`, {
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
  }
}
