<template>
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" @click="$emit('close')">
    <div @click.stop class="bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-5xl max-h-[90vh] overflow-hidden flex flex-col">
      <!-- Header -->
      <div class="px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex justify-between items-center">
        <h2 class="text-2xl font-bold text-gray-800 dark:text-gray-100">📁 Projektverwaltung</h2>
        <button
          @click="$emit('close')"
          class="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 text-2xl"
        >
          ×
        </button>
      </div>

      <!-- Content -->
      <div class="flex flex-1 overflow-hidden">
        <!-- Projects List (Left Sidebar) -->
        <div class="w-1/3 border-r border-gray-200 dark:border-gray-700 flex flex-col">
          <!-- New Project Button -->
          <div class="p-4 border-b border-gray-200 dark:border-gray-700">
            <button
              @click="showNewProjectModal = true"
              class="w-full px-4 py-2 bg-fleet-orange-500 hover:bg-fleet-orange-600 text-white rounded-lg transition-colors"
            >
              + Neues Projekt
            </button>
          </div>

          <!-- Project List -->
          <div class="flex-1 overflow-y-auto p-4 space-y-2">
            <div
              v-for="project in projects"
              :key="project.id"
              @click="selectProject(project)"
              class="p-3 rounded-lg cursor-pointer transition-colors"
              :class="selectedProject?.id === project.id
                ? 'bg-fleet-orange-100 dark:bg-fleet-orange-900 border-l-4 border-fleet-orange-500'
                : 'bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600'"
            >
              <div class="font-semibold text-gray-800 dark:text-gray-100">{{ project.name }}</div>
              <div class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                {{ project.contextFiles.length }} Dateien, {{ project.chatIds.length }} Chats
              </div>
              <div class="flex items-center justify-between mt-1">
                <div class="text-xs text-gray-500 dark:text-gray-400">
                  ~{{ project.estimatedTokens.toLocaleString() }} Tokens
                </div>
                <div class="text-xs font-bold" :class="getContextUsageColor(project.estimatedTokens)">
                  {{ getContextUsagePercent(project.estimatedTokens) }}%
                </div>
              </div>
            </div>

            <div v-if="projects.length === 0" class="text-center text-gray-500 dark:text-gray-400 mt-8">
              Noch keine Projekte vorhanden
            </div>
          </div>
        </div>

        <!-- Project Details (Right Panel) -->
        <div v-if="selectedProject" class="flex-1 flex flex-col overflow-hidden">
          <!-- Project Header -->
          <div class="p-6 border-b border-gray-200 dark:border-gray-700">
            <div class="flex justify-between items-start">
              <div class="flex-1">
                <h3 class="text-xl font-bold text-gray-800 dark:text-gray-100">{{ selectedProject.name }}</h3>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">{{ selectedProject.description }}</p>
              </div>
              <div class="flex space-x-2">
                <button
                  @click="editProject(selectedProject)"
                  class="px-3 py-1 text-sm text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300"
                >
                  ✏️ Bearbeiten
                </button>
                <button
                  @click="deleteProjectConfirm(selectedProject.id)"
                  class="px-3 py-1 text-sm text-red-600 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300"
                >
                  🗑️ Löschen
                </button>
              </div>
            </div>

            <!-- Project Stats -->
            <div class="mt-4 grid grid-cols-3 gap-4">
              <div class="bg-gray-100 dark:bg-gray-700 rounded-lg p-3">
                <div class="text-xs text-gray-500 dark:text-gray-400">Context-Dateien</div>
                <div class="text-lg font-bold text-gray-800 dark:text-gray-100">{{ selectedProject.contextFiles.length }}</div>
              </div>
              <div class="bg-gray-100 dark:bg-gray-700 rounded-lg p-3">
                <div class="text-xs text-gray-500 dark:text-gray-400">Zugeordnete Chats</div>
                <div class="text-lg font-bold text-gray-800 dark:text-gray-100">{{ selectedProject.chatIds.length }}</div>
              </div>
              <div class="bg-gray-100 dark:bg-gray-700 rounded-lg p-3">
                <div class="text-xs text-gray-500 dark:text-gray-400">Geschätzte Tokens</div>
                <div class="text-lg font-bold text-gray-800 dark:text-gray-100">~{{ selectedProject.estimatedTokens.toLocaleString() }}</div>
              </div>
            </div>

            <!-- Context Window Usage Bar -->
            <div class="mt-4 bg-gray-100 dark:bg-gray-700 rounded-lg p-4">
              <div class="flex justify-between items-center mb-2">
                <div class="text-sm font-medium text-gray-700 dark:text-gray-300">Context Window Auslastung</div>
                <div class="text-sm font-bold" :class="getContextUsageColor(selectedProject.estimatedTokens)">
                  {{ getContextUsagePercent(selectedProject.estimatedTokens) }}%
                </div>
              </div>

              <!-- Progress Bar -->
              <div class="w-full bg-gray-300 dark:bg-gray-600 rounded-full h-3 overflow-hidden">
                <div
                  class="h-full transition-all duration-300 rounded-full"
                  :class="getContextUsageBarColor(selectedProject.estimatedTokens)"
                  :style="{ width: getContextUsagePercent(selectedProject.estimatedTokens) + '%' }"
                ></div>
              </div>

              <div class="mt-2 text-xs text-gray-500 dark:text-gray-400">
                <div class="flex items-center justify-between">
                  <span>
                    {{ selectedProject.estimatedTokens.toLocaleString() }} / {{ getContextLimit().toLocaleString() }} Tokens
                  </span>
                  <span class="text-xs text-gray-400 dark:text-gray-500">
                    Model: {{ chatStore.selectedModel }}
                  </span>
                </div>
                <div v-if="getContextUsagePercent(selectedProject.estimatedTokens) > 80" class="mt-1">
                  <span class="text-orange-600 dark:text-orange-400 font-medium">
                    ⚠️ Warnung: Context fast voll!
                  </span>
                </div>
                <div v-if="getContextUsagePercent(selectedProject.estimatedTokens) > 95" class="mt-1">
                  <span class="text-red-600 dark:text-red-400 font-medium">
                    🚨 Context-Limit erreicht!
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- Tabs -->
          <div class="border-b border-gray-200 dark:border-gray-700">
            <div class="flex px-6">
              <button
                @click="activeTab = 'files'"
                class="px-4 py-2 text-sm font-medium transition-colors"
                :class="activeTab === 'files'
                  ? 'text-fleet-orange-600 border-b-2 border-fleet-orange-500'
                  : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'"
              >
                📄 Context-Dateien
              </button>
              <button
                @click="activeTab = 'chats'"
                class="px-4 py-2 text-sm font-medium transition-colors"
                :class="activeTab === 'chats'
                  ? 'text-fleet-orange-600 border-b-2 border-fleet-orange-500'
                  : 'text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200'"
              >
                💬 Chats
              </button>
            </div>
          </div>

          <!-- Tab Content -->
          <div class="flex-1 overflow-y-auto p-6">
            <!-- Context Files Tab -->
            <div v-if="activeTab === 'files'">
              <!-- Upload Button -->
              <div class="mb-4">
                <button
                  @click="showUploadModal = true"
                  class="px-4 py-2 bg-green-500 hover:bg-green-600 text-white rounded-lg transition-colors text-sm"
                >
                  + Datei hochladen
                </button>
              </div>

              <!-- Files List -->
              <div class="space-y-2">
                <div
                  v-for="file in selectedProject.contextFiles"
                  :key="file.id"
                  class="flex items-center justify-between p-3 bg-gray-100 dark:bg-gray-700 rounded-lg"
                >
                  <div class="flex-1">
                    <div class="font-medium text-gray-800 dark:text-gray-100">{{ file.filename }}</div>
                    <div class="text-xs text-gray-500 dark:text-gray-400">
                      {{ formatFileSize(file.size) }} • ~{{ file.estimatedTokens }} Tokens
                    </div>
                  </div>
                  <div class="flex space-x-2">
                    <button
                      @click="viewFileContent(file.id)"
                      class="px-2 py-1 text-xs text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300"
                    >
                      👁️ Anzeigen
                    </button>
                    <button
                      @click="deleteFileConfirm(file.id)"
                      class="px-2 py-1 text-xs text-red-600 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300"
                    >
                      🗑️
                    </button>
                  </div>
                </div>

                <div v-if="selectedProject.contextFiles.length === 0" class="text-center text-gray-500 dark:text-gray-400 py-8">
                  Noch keine Context-Dateien hochgeladen
                </div>
              </div>
            </div>

            <!-- Chats Tab -->
            <div v-if="activeTab === 'chats'">
              <!-- Assign Chat Button -->
              <div class="mb-4">
                <button
                  @click="showAssignChatModal = true"
                  class="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition-colors text-sm"
                >
                  + Chat zuordnen
                </button>
              </div>

              <!-- Chats List -->
              <div class="space-y-2">
                <div
                  v-for="chat in projectChats"
                  :key="chat.id"
                  class="flex items-center justify-between p-3 bg-gray-100 dark:bg-gray-700 rounded-lg"
                >
                  <div class="flex-1">
                    <div class="font-medium text-gray-800 dark:text-gray-100">{{ chat.title }}</div>
                    <div class="text-xs text-gray-500 dark:text-gray-400">
                      Model: {{ chat.model }}
                    </div>
                  </div>
                  <button
                    @click="unassignChat(chat.id)"
                    class="px-2 py-1 text-xs text-red-600 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300"
                  >
                    Trennen
                  </button>
                </div>

                <div v-if="projectChats.length === 0" class="text-center text-gray-500 dark:text-gray-400 py-8">
                  Noch keine Chats zugeordnet
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- No Project Selected -->
        <div v-else class="flex-1 flex items-center justify-center text-gray-500 dark:text-gray-400">
          <div class="text-center">
            <div class="text-6xl mb-4">📁</div>
            <div>Wähle ein Projekt aus oder erstelle ein neues</div>
          </div>
        </div>
      </div>
    </div>

    <!-- New/Edit Project Modal -->
    <div
      v-if="showNewProjectModal || editingProject"
      @click="closeProjectModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60]"
    >
      <div @click.stop class="bg-white dark:bg-gray-800 rounded-lg p-6 w-96 shadow-xl">
        <h3 class="text-lg font-bold mb-4 dark:text-white">
          {{ editingProject ? 'Projekt bearbeiten' : 'Neues Projekt' }}
        </h3>
        <input
          v-model="newProjectName"
          type="text"
          placeholder="Projekt-Name"
          class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 rounded-lg focus:outline-none focus:border-fleet-orange-500 mb-3"
        />
        <textarea
          v-model="newProjectDescription"
          placeholder="Beschreibung (optional)"
          rows="3"
          class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 rounded-lg focus:outline-none focus:border-fleet-orange-500 mb-4 resize-none"
        ></textarea>
        <div class="flex justify-end space-x-2">
          <button
            @click="closeProjectModal"
            class="px-4 py-2 bg-gray-300 dark:bg-gray-700 hover:bg-gray-400 dark:hover:bg-gray-600 rounded-lg transition-colors dark:text-gray-100"
          >
            Abbrechen
          </button>
          <button
            @click="saveProject"
            :disabled="!newProjectName.trim()"
            class="px-4 py-2 bg-fleet-orange-500 hover:bg-fleet-orange-600 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ editingProject ? 'Speichern' : 'Erstellen' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Upload File Modal -->
    <div
      v-if="showUploadModal"
      @click="showUploadModal = false"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60]"
    >
      <div @click.stop class="bg-white dark:bg-gray-800 rounded-lg p-6 w-[600px] shadow-xl">
        <h3 class="text-lg font-bold mb-4 dark:text-white">Context-Datei hochladen</h3>

        <input
          v-model="uploadFileName"
          type="text"
          placeholder="Dateiname (z.B. requirements.txt)"
          class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 rounded-lg focus:outline-none focus:border-fleet-orange-500 mb-3"
        />

        <textarea
          v-model="uploadFileContent"
          placeholder="Dateiinhalt..."
          rows="10"
          class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 rounded-lg focus:outline-none focus:border-fleet-orange-500 mb-4 resize-none font-mono text-sm"
        ></textarea>

        <div class="text-xs text-gray-500 dark:text-gray-400 mb-4">
          Größe: {{ formatFileSize(uploadFileContent.length) }} •
          ~{{ Math.floor(uploadFileContent.length / 4) }} Tokens
        </div>

        <div class="flex justify-end space-x-2">
          <button
            @click="showUploadModal = false"
            class="px-4 py-2 bg-gray-300 dark:bg-gray-700 hover:bg-gray-400 dark:hover:bg-gray-600 rounded-lg transition-colors dark:text-gray-100"
          >
            Abbrechen
          </button>
          <button
            @click="uploadFile"
            :disabled="!uploadFileName.trim() || !uploadFileContent.trim()"
            class="px-4 py-2 bg-green-500 hover:bg-green-600 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Hochladen
          </button>
        </div>
      </div>
    </div>

    <!-- Assign Chat Modal -->
    <div
      v-if="showAssignChatModal"
      @click="showAssignChatModal = false"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60]"
    >
      <div @click.stop class="bg-white dark:bg-gray-800 rounded-lg p-6 w-96 shadow-xl max-h-[70vh] flex flex-col">
        <h3 class="text-lg font-bold mb-4 dark:text-white">Chat zuordnen</h3>

        <div class="flex-1 overflow-y-auto space-y-2 mb-4">
          <div
            v-for="chat in availableChats"
            :key="chat.id"
            @click="assignChat(chat.id)"
            class="p-3 bg-gray-100 dark:bg-gray-700 rounded-lg cursor-pointer hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
          >
            <div class="font-medium text-gray-800 dark:text-gray-100">{{ chat.title }}</div>
            <div class="text-xs text-gray-500 dark:text-gray-400">{{ chat.model }}</div>
          </div>

          <div v-if="availableChats.length === 0" class="text-center text-gray-500 dark:text-gray-400 py-4">
            Keine verfügbaren Chats
          </div>
        </div>

        <button
          @click="showAssignChatModal = false"
          class="w-full px-4 py-2 bg-gray-300 dark:bg-gray-700 hover:bg-gray-400 dark:hover:bg-gray-600 rounded-lg transition-colors dark:text-gray-100"
        >
          Schließen
        </button>
      </div>
    </div>

    <!-- View File Content Modal -->
    <div
      v-if="viewingFileContent"
      @click="viewingFileContent = null"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60]"
    >
      <div @click.stop class="bg-white dark:bg-gray-800 rounded-lg p-6 w-[800px] max-h-[80vh] shadow-xl flex flex-col">
        <div class="flex justify-between items-center mb-4">
          <h3 class="text-lg font-bold dark:text-white">Dateiinhalt</h3>
          <button
            @click="viewingFileContent = null"
            class="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200 text-2xl"
          >
            ×
          </button>
        </div>

        <pre class="flex-1 overflow-auto p-4 bg-gray-100 dark:bg-gray-900 rounded-lg text-sm font-mono whitespace-pre-wrap dark:text-gray-100">{{ viewingFileContent }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../services/api'
import { useChatStore } from '../stores/chatStore'
import { formatFileSize } from '../composables/useFormatters'
import { useConfirmDialog } from '../composables/useConfirmDialog'

const chatStore = useChatStore()
const { confirmDelete } = useConfirmDialog()

// State
const projects = ref([])
const selectedProject = ref(null)
const projectChats = ref([])
const activeTab = ref('files')

// Modals
const showNewProjectModal = ref(false)
const showUploadModal = ref(false)
const showAssignChatModal = ref(false)
const editingProject = ref(null)
const viewingFileContent = ref(null)

// Form data
const newProjectName = ref('')
const newProjectDescription = ref('')
const uploadFileName = ref('')
const uploadFileContent = ref('')

onMounted(async () => {
  await loadProjects()
})

async function loadProjects() {
  try {
    projects.value = await api.getAllProjects()
  } catch (e) {
    console.error('Failed to load projects:', e)
    alert('Fehler beim Laden der Projekte!')
  }
}

async function selectProject(project) {
  selectedProject.value = project
  activeTab.value = 'files'
  await loadProjectChats()
}

async function loadProjectChats() {
  if (!selectedProject.value) return
  try {
    projectChats.value = await api.getProjectChats(selectedProject.value.id)
  } catch (e) {
    console.error('Failed to load project chats:', e)
  }
}

function closeProjectModal() {
  showNewProjectModal.value = false
  editingProject.value = null
  newProjectName.value = ''
  newProjectDescription.value = ''
}

async function saveProject() {
  if (!newProjectName.value.trim()) return

  try {
    if (editingProject.value) {
      // Update existing project
      await api.updateProject(editingProject.value.id, {
        name: newProjectName.value,
        description: newProjectDescription.value
      })
    } else {
      // Create new project
      const newProject = await api.createProject({
        name: newProjectName.value,
        description: newProjectDescription.value
      })
      selectedProject.value = newProject
    }

    await loadProjects()
    closeProjectModal()
  } catch (e) {
    console.error('Failed to save project:', e)
    alert('Fehler beim Speichern des Projekts!')
  }
}

function editProject(project) {
  editingProject.value = project
  newProjectName.value = project.name
  newProjectDescription.value = project.description || ''
  showNewProjectModal.value = true
}

async function deleteProjectConfirm(projectId) {
  const confirmed = await confirmDelete('Projekt', 'Alle Context-Dateien werden ebenfalls gelöscht.')
  if (!confirmed) return

  try {
    await api.deleteProject(projectId)
    if (selectedProject.value?.id === projectId) {
      selectedProject.value = null
    }
    await loadProjects()
  } catch (e) {
    console.error('Failed to delete project:', e)
    alert('Fehler beim Löschen des Projekts!')
  }
}

async function uploadFile() {
  if (!uploadFileName.value.trim() || !uploadFileContent.value.trim()) return

  try {
    const fileType = uploadFileName.value.split('.').pop()
    await api.uploadContextFile({
      projectId: selectedProject.value.id,
      filename: uploadFileName.value,
      content: uploadFileContent.value,
      fileType: fileType
    })

    uploadFileName.value = ''
    uploadFileContent.value = ''
    showUploadModal.value = false

    // Reload project
    const updated = await api.getProject(selectedProject.value.id)
    selectedProject.value = updated
    await loadProjects()
  } catch (e) {
    console.error('Failed to upload file:', e)
    alert('Fehler beim Hochladen der Datei!')
  }
}

async function viewFileContent(fileId) {
  try {
    viewingFileContent.value = await api.getContextFileContent(fileId)
  } catch (e) {
    console.error('Failed to load file content:', e)
    alert('Fehler beim Laden der Datei!')
  }
}

async function deleteFileConfirm(fileId) {
  const confirmed = await confirmDelete('Datei')
  if (!confirmed) return

  try {
    await api.deleteContextFile(fileId)

    // Reload project
    const updated = await api.getProject(selectedProject.value.id)
    selectedProject.value = updated
    await loadProjects()
  } catch (e) {
    console.error('Failed to delete file:', e)
    alert('Fehler beim Löschen der Datei!')
  }
}

const availableChats = computed(() => {
  // All chats that are not assigned to current project
  const assignedChatIds = new Set(selectedProject.value?.chatIds || [])
  return chatStore.chats.filter(chat => !assignedChatIds.has(chat.id))
})

async function assignChat(chatId) {
  try {
    await api.assignChatToProject(chatId, selectedProject.value.id)
    showAssignChatModal.value = false

    // Reload project and chats
    const updated = await api.getProject(selectedProject.value.id)
    selectedProject.value = updated
    await loadProjects()
    await loadProjectChats()
    await chatStore.loadChats()
  } catch (e) {
    console.error('Failed to assign chat:', e)
    alert('Fehler beim Zuordnen des Chats!')
  }
}

async function unassignChat(chatId) {
  try {
    await api.unassignChatFromProject(chatId)

    // Reload project and chats
    const updated = await api.getProject(selectedProject.value.id)
    selectedProject.value = updated
    await loadProjects()
    await loadProjectChats()
    await chatStore.loadChats()
  } catch (e) {
    console.error('Failed to unassign chat:', e)
    alert('Fehler beim Trennen des Chats!')
  }
}

// formatFileSize importiert aus useFormatters.js

// Context Window calculation functions
function getContextLimit() {
  const chatStore = useChatStore()
  const currentModel = chatStore.selectedModel || 'qwen2.5-coder:7b'

  // Model-specific context windows
  // Source: Based on model documentation
  const contextWindows = {
    // Qwen models
    'qwen2.5-coder:7b': 128000,
    'qwen2.5-coder:14b': 128000,
    'qwen2.5-coder:32b': 128000,
    'qwen2.5:7b': 128000,
    'qwen2.5:14b': 128000,
    'qwen2.5:32b': 128000,

    // DeepSeek models
    'deepseek-coder-v2:16b': 128000,
    'deepseek-coder-v2:236b': 128000,
    'deepseek-coder:6.7b': 16000,
    'deepseek-coder:33b': 16000,

    // Llama models
    'llama3.2:1b': 128000,
    'llama3.2:3b': 128000,
    'llama3.1:8b': 128000,
    'llama3.1:70b': 128000,
    'llama3.1:405b': 128000,

    // CodeLlama
    'codellama:7b': 16000,
    'codellama:13b': 16000,
    'codellama:34b': 16000,
    'codellama:70b': 100000,

    // Mistral
    'mistral:7b': 32000,
    'mistral-small:22b': 32000,
    'mistral-large:123b': 128000,

    // Mixtral
    'mixtral:8x7b': 32000,
    'mixtral:8x22b': 64000,

    // Gemma
    'gemma:2b': 8000,
    'gemma:7b': 8000,
    'gemma2:9b': 8000,
    'gemma2:27b': 8000,

    // Vision models
    'llava:7b': 4000,
    'llava:13b': 4000,
    'llava:34b': 4000,
    'bakllava:7b': 4000,
    'moondream:latest': 2000,

    // Phi models
    'phi3:mini': 128000,
    'phi3:medium': 128000,

    // Other models
    'dolphin-mixtral:8x7b': 32000,
    'starling-lm:7b': 8000,
    'solar:10.7b': 4000,
  }

  // Try exact match first
  let contextWindow = contextWindows[currentModel]

  // If not found, try fuzzy match by model family
  if (!contextWindow) {
    const modelLower = currentModel.toLowerCase()

    // Check by family name
    if (modelLower.includes('qwen2.5')) contextWindow = 128000
    else if (modelLower.includes('deepseek-coder-v2')) contextWindow = 128000
    else if (modelLower.includes('deepseek')) contextWindow = 16000
    else if (modelLower.includes('llama3.2') || modelLower.includes('llama3.1')) contextWindow = 128000
    else if (modelLower.includes('llama')) contextWindow = 8000
    else if (modelLower.includes('codellama:70')) contextWindow = 100000
    else if (modelLower.includes('codellama')) contextWindow = 16000
    else if (modelLower.includes('mistral-large')) contextWindow = 128000
    else if (modelLower.includes('mistral')) contextWindow = 32000
    else if (modelLower.includes('mixtral:8x22b')) contextWindow = 64000
    else if (modelLower.includes('mixtral')) contextWindow = 32000
    else if (modelLower.includes('gemma')) contextWindow = 8000
    else if (modelLower.includes('llava') || modelLower.includes('bakllava')) contextWindow = 4000
    else if (modelLower.includes('moondream')) contextWindow = 2000
    else if (modelLower.includes('phi3')) contextWindow = 128000
    else contextWindow = 8000 // Conservative default
  }

  // Use 80% of context window as safe limit (leaving room for user messages + responses)
  return Math.floor(contextWindow * 0.8)
}

function getContextUsagePercent(tokens) {
  const limit = getContextLimit()
  const percent = (tokens / limit) * 100
  return Math.min(Math.round(percent), 100)
}

function getContextUsageColor(tokens) {
  const percent = getContextUsagePercent(tokens)
  if (percent < 50) return 'text-green-600 dark:text-green-400'
  if (percent < 80) return 'text-yellow-600 dark:text-yellow-400'
  if (percent < 95) return 'text-orange-600 dark:text-orange-400'
  return 'text-red-600 dark:text-red-400'
}

function getContextUsageBarColor(tokens) {
  const percent = getContextUsagePercent(tokens)
  if (percent < 50) return 'bg-green-500'
  if (percent < 80) return 'bg-yellow-500'
  if (percent < 95) return 'bg-orange-500'
  return 'bg-red-500'
}

function getModelContextWindowSize() {
  const chatStore = useChatStore()
  const currentModel = chatStore.selectedModel || 'qwen2.5-coder:7b'

  // Return the full context window size (not the safe limit)
  const contextWindows = {
    'qwen2.5-coder:7b': 128000,
    'qwen2.5-coder:14b': 128000,
    'qwen2.5-coder:32b': 128000,
    'deepseek-coder-v2:16b': 128000,
    'deepseek-coder-v2:236b': 128000,
    'llama3.2:1b': 128000,
    'llama3.2:3b': 128000,
    'llama3.1:8b': 128000,
    'llama3.1:70b': 128000,
    'mistral-large:123b': 128000,
    'phi3:mini': 128000,
    'phi3:medium': 128000,
    'codellama:70b': 100000,
    'mixtral:8x22b': 64000,
    'mistral:7b': 32000,
    'mixtral:8x7b': 32000,
    'codellama:7b': 16000,
    'codellama:13b': 16000,
    'deepseek-coder:6.7b': 16000,
    'gemma:2b': 8000,
    'gemma:7b': 8000,
    'llava:7b': 4000,
    'llava:13b': 4000,
    'moondream:latest': 2000,
  }

  let contextWindow = contextWindows[currentModel]

  if (!contextWindow) {
    const modelLower = currentModel.toLowerCase()
    if (modelLower.includes('qwen2.5') || modelLower.includes('deepseek-coder-v2') ||
        modelLower.includes('llama3.2') || modelLower.includes('llama3.1') ||
        modelLower.includes('mistral-large') || modelLower.includes('phi3')) {
      contextWindow = 128000
    } else if (modelLower.includes('codellama:70')) {
      contextWindow = 100000
    } else if (modelLower.includes('mixtral:8x22b')) {
      contextWindow = 64000
    } else if (modelLower.includes('mistral') || modelLower.includes('mixtral')) {
      contextWindow = 32000
    } else if (modelLower.includes('codellama') || modelLower.includes('deepseek')) {
      contextWindow = 16000
    } else if (modelLower.includes('gemma')) {
      contextWindow = 8000
    } else if (modelLower.includes('llava') || modelLower.includes('bakllava')) {
      contextWindow = 4000
    } else if (modelLower.includes('moondream')) {
      contextWindow = 2000
    } else {
      contextWindow = 8000
    }
  }

  return contextWindow
}
</script>
