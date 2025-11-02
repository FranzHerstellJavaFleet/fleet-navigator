<template>
  <div class="flex-1 flex flex-col min-h-0 bg-gradient-to-br from-white to-gray-50 dark:from-gray-900 dark:to-gray-950">
    <!-- Project Header with Glassmorphism -->
    <div class="
      p-6 border-b border-gray-200/50 dark:border-gray-700/50
      bg-gradient-to-r from-indigo-500/5 to-purple-500/5
      dark:from-indigo-500/10 dark:to-purple-500/10
      backdrop-blur-sm flex-shrink-0
    ">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center space-x-4">
          <div class="p-3 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-500 shadow-lg">
            <FolderIcon class="w-8 h-8 text-white" />
          </div>
          <div>
            <h1
              @click="showChatList"
              class="
                text-2xl font-bold
                bg-gradient-to-r from-gray-900 to-gray-700
                dark:from-white dark:to-gray-300
                bg-clip-text text-transparent
                cursor-pointer
                hover:from-fleet-orange-500 hover:to-orange-600
                transition-all duration-200
              "
              :title="isChatOpen ? 'Zurück zur Chat-Liste' : ''"
            >
              {{ project.name }}
            </h1>
            <p v-if="project.description" class="text-gray-600 dark:text-gray-400 text-sm mt-1">
              {{ project.description }}
            </p>
          </div>
        </div>
        <button
          @click="$emit('close')"
          class="
            px-4 py-2 rounded-xl
            text-gray-600 dark:text-gray-400
            hover:text-gray-900 dark:hover:text-white
            hover:bg-gray-100 dark:hover:bg-gray-800
            transition-all duration-200
            transform hover:scale-105 active:scale-95
            flex items-center gap-2
          "
        >
          <XMarkIcon class="w-5 h-5" />
          <span>Schließen</span>
        </button>
      </div>

      <!-- Context Usage Stats with Icons -->
      <div class="grid grid-cols-3 gap-4">
        <div class="
          bg-gradient-to-br from-purple-100 to-purple-200
          dark:from-purple-900/50 dark:to-purple-800/50
          backdrop-blur-sm
          rounded-xl p-4
          border border-purple-200 dark:border-purple-700/50
          shadow-sm hover:shadow-md
          transition-all duration-200
        ">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm font-medium text-purple-700 dark:text-purple-300">Kontext-Dateien</span>
            <DocumentTextIcon class="w-5 h-5 text-purple-500" />
          </div>
          <div class="text-3xl font-bold text-purple-900 dark:text-purple-100">
            {{ project.contextFiles?.length || 0 }}
          </div>
        </div>

        <div class="
          bg-gradient-to-br from-blue-100 to-blue-200
          dark:from-blue-900/50 dark:to-blue-800/50
          backdrop-blur-sm
          rounded-xl p-4
          border border-blue-200 dark:border-blue-700/50
          shadow-sm hover:shadow-md
          transition-all duration-200
        ">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm font-medium text-blue-700 dark:text-blue-300">Zugeordnete Chats</span>
            <ChatBubbleLeftRightIcon class="w-5 h-5 text-blue-500" />
          </div>
          <div class="text-3xl font-bold text-blue-900 dark:text-blue-100">
            {{ projectChats.length }}
          </div>
        </div>

        <div class="
          bg-gradient-to-br from-green-100 to-green-200
          dark:from-green-900/50 dark:to-green-800/50
          backdrop-blur-sm
          rounded-xl p-4
          border border-green-200 dark:border-green-700/50
          shadow-sm hover:shadow-md
          transition-all duration-200
        ">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm font-medium text-green-700 dark:text-green-300">Context Tokens</span>
            <CpuChipIcon class="w-5 h-5 text-green-500" />
          </div>
          <div class="text-3xl font-bold text-green-900 dark:text-green-100">
            {{ formatNumber(totalContextTokens) }}
          </div>
        </div>
      </div>

      <!-- Context Usage Progress with Animation -->
      <div class="mt-4">
        <div class="flex items-center justify-between mb-2">
          <div class="flex items-center gap-2">
            <ChartBarIcon class="w-4 h-4 text-gray-500" />
            <span class="text-sm font-medium text-gray-700 dark:text-gray-300">Context-Nutzung</span>
          </div>
          <span class="text-sm font-bold px-3 py-1 rounded-lg" :class="getContextUsageColor()">
            {{ contextUsagePercent }}% ({{ formatNumber(totalContextTokens) }} / {{ formatNumber(contextLimit) }})
          </span>
        </div>
        <div class="w-full bg-gray-200/50 dark:bg-gray-700/50 rounded-full h-3 shadow-inner overflow-hidden">
          <div
            class="h-3 rounded-full transition-all duration-500 ease-out relative"
            :class="getContextUsageBarColor()"
            :style="{ width: Math.min(contextUsagePercent, 100) + '%' }"
          >
            <div class="absolute inset-0 bg-white/20 animate-pulse"></div>
          </div>
        </div>
        <Transition name="fade">
          <div v-if="contextUsagePercent >= 80" class="
            mt-3 p-3 rounded-xl
            bg-gradient-to-r from-orange-50 to-amber-50
            dark:from-orange-900/20 dark:to-amber-900/20
            border border-orange-200 dark:border-orange-700/50
            flex items-start gap-2
          ">
            <ExclamationTriangleIcon class="w-5 h-5 text-orange-600 dark:text-orange-400 flex-shrink-0 mt-0.5" />
            <span class="text-sm text-orange-700 dark:text-orange-300">
              Context-Limit bald erreicht! Einige Modelle könnten nicht verfügbar sein.
            </span>
          </div>
        </Transition>
      </div>
    </div>

    <!-- Tabs with Icons -->
    <div class="border-b border-gray-200/50 dark:border-gray-700/50 bg-gray-50/50 dark:bg-gray-900/50 flex-shrink-0">
      <div class="flex space-x-1 px-6">
        <button
          @click="activeTab = 'chats'"
          class="
            px-4 py-3 font-medium text-sm
            transition-all duration-200
            flex items-center gap-2
            relative
          "
          :class="activeTab === 'chats'
            ? 'text-fleet-orange-600 dark:text-fleet-orange-400'
            : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200'"
        >
          <ChatBubbleLeftRightIcon class="w-5 h-5" />
          <span>Chats ({{ projectChats.length }})</span>
          <div v-if="activeTab === 'chats'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r from-fleet-orange-500 to-orange-600"></div>
        </button>
        <button
          @click="activeTab = 'files'"
          class="
            px-4 py-3 font-medium text-sm
            transition-all duration-200
            flex items-center gap-2
            relative
          "
          :class="activeTab === 'files'
            ? 'text-fleet-orange-600 dark:text-fleet-orange-400'
            : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200'"
        >
          <DocumentTextIcon class="w-5 h-5" />
          <span>Dateien ({{ project.contextFiles?.length || 0 }})</span>
          <div v-if="activeTab === 'files'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r from-fleet-orange-500 to-orange-600"></div>
        </button>
      </div>
    </div>

    <!-- Content -->
    <div class="flex-1 flex flex-col min-h-0">
      <!-- Chats Tab -->
      <div v-if="activeTab === 'chats'" class="flex-1 flex flex-col min-h-0">
        <!-- Show Chat List OR Chat Content -->

        <!-- Chat List (when no chat is selected) -->
        <div v-if="!isChatOpen" class="p-6 flex-1 min-h-0 flex flex-col">
          <div v-if="projectChats.length === 0" class="
            text-center py-12
            bg-gradient-to-br from-gray-50 to-gray-100
            dark:from-gray-800/50 dark:to-gray-900/50
            rounded-2xl border-2 border-dashed border-gray-300 dark:border-gray-700
          ">
            <ChatBubbleLeftRightIcon class="w-16 h-16 text-gray-400 dark:text-gray-600 mx-auto mb-4" />
            <p class="text-sm font-medium text-gray-600 dark:text-gray-400 mb-1">Noch keine Chats in diesem Projekt</p>
            <p class="text-xs text-gray-500 dark:text-gray-500 flex items-center justify-center gap-1">
              <span>Ordne Chats über das</span>
              <EllipsisVerticalIcon class="w-4 h-4" />
              <span>Menü zu</span>
            </p>
          </div>
          <div v-else class="grid gap-2 overflow-y-auto">
            <div
              v-for="chat in projectChats"
              :key="chat.id"
              class="relative group bg-gray-50 dark:bg-gray-800 rounded-lg p-3 border-2 border-transparent hover:border-fleet-orange-300 transition-colors"
            >
              <div class="flex items-center justify-between">
                <div @click="selectChat(chat)" class="flex-1 cursor-pointer">
                  <h3 class="font-medium text-gray-900 dark:text-white">{{ chat.title }}</h3>
                  <p class="text-xs text-gray-600 dark:text-gray-400">{{ formatDate(chat.updatedAt) }}</p>
                </div>
                <!-- Context Menu Button (⋮) -->
                <button
                  @click.stop="toggleChatMenu(chat.id)"
                  class="opacity-0 group-hover:opacity-100 text-gray-400 hover:text-white transition-opacity px-2 py-1"
                >
                  ⋮
                </button>
              </div>

              <!-- Chat Dropdown Menu -->
              <div
                v-if="openChatMenuId === chat.id"
                class="absolute right-2 top-12 bg-gray-800 border border-gray-700 rounded-lg shadow-xl z-50 w-56"
                @click.stop
              >
                <button
                  @click.stop="removeFromProject(chat)"
                  class="w-full text-left px-4 py-2 hover:bg-gray-700 transition-colors flex items-center space-x-2 text-white"
                >
                  <span>↗️</span>
                  <span>Aus Projekt entfernen</span>
                </button>
                <button
                  @click.stop="reassignChat(chat)"
                  class="w-full text-left px-4 py-2 hover:bg-gray-700 transition-colors flex items-center space-x-2 text-white"
                >
                  <span>📁</span>
                  <span>Anderes Projekt zuordnen</span>
                </button>
                <button
                  @click.stop="deleteChat(chat.id)"
                  class="w-full text-left px-4 py-2 hover:bg-gray-700 text-red-400 hover:text-red-300 transition-colors flex items-center space-x-2"
                >
                  <span>🗑️</span>
                  <span>Chat löschen</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Chat Window (when chat is selected) -->
        <div v-if="isChatOpen" class="flex-1 flex flex-col min-h-0">
          <ChatWindow />
        </div>
      </div>

      <!-- Files Tab -->
      <div v-if="activeTab === 'files'" class="p-6 overflow-y-auto">
        <!-- Upload Button -->
        <div class="mb-4">
          <input
            type="file"
            @change="handleFileUpload"
            accept=".txt,.md,.java,.js,.py,.json,.xml,.yaml,.yml"
            class="hidden"
            ref="fileInput"
          />
          <button
            @click="$refs.fileInput.click()"
            class="
              px-4 py-2 rounded-xl
              bg-gradient-to-r from-purple-600 to-indigo-600
              hover:from-purple-500 hover:to-indigo-500
              text-white font-medium
              shadow-lg hover:shadow-xl
              transition-all duration-200
              transform hover:scale-105 active:scale-95
              flex items-center gap-2
            "
          >
            <CloudArrowUpIcon class="w-5 h-5" />
            <span>Datei hochladen</span>
          </button>
        </div>

        <!-- File List -->
        <div v-if="project.contextFiles?.length > 0" class="grid gap-3">
          <div
            v-for="file in project.contextFiles"
            :key="file.id"
            class="
              bg-gradient-to-br from-gray-50 to-gray-100
              dark:from-gray-800/50 dark:to-gray-900/50
              backdrop-blur-sm
              rounded-xl p-4
              border border-gray-200 dark:border-gray-700/50
              flex items-center justify-between
              hover:shadow-md
              transition-all duration-200
            "
          >
            <div class="flex items-center gap-3 flex-1">
              <DocumentTextIcon class="w-6 h-6 text-purple-500 flex-shrink-0" />
              <div class="flex-1 min-w-0">
                <h3 class="font-medium text-gray-900 dark:text-white truncate">{{ file.filename }}</h3>
                <p class="text-sm text-gray-600 dark:text-gray-400 flex items-center gap-2">
                  <span>{{ formatFileSize(file.size) }}</span>
                  <span>·</span>
                  <CpuChipIcon class="w-3.5 h-3.5" />
                  <span>~{{ file.estimatedTokens || Math.floor(file.size / 4) }} tokens</span>
                </p>
              </div>
            </div>
            <button
              @click="deleteFile(file.id)"
              class="
                px-3 py-2 rounded-lg
                text-red-500 hover:text-red-600
                hover:bg-red-50 dark:hover:bg-red-900/20
                transition-all duration-200
                transform hover:scale-105 active:scale-95
                flex items-center gap-1.5
              "
            >
              <TrashIcon class="w-4 h-4" />
              <span class="text-sm font-medium">Löschen</span>
            </button>
          </div>
        </div>
        <div v-else class="
          text-center py-16
          bg-gradient-to-br from-gray-50 to-gray-100
          dark:from-gray-800/50 dark:to-gray-900/50
          rounded-2xl border-2 border-dashed border-gray-300 dark:border-gray-700
        ">
          <DocumentTextIcon class="w-20 h-20 text-gray-400 dark:text-gray-600 mx-auto mb-4" />
          <p class="text-base font-medium text-gray-600 dark:text-gray-400 mb-2">Noch keine Dateien hochgeladen</p>
          <p class="text-sm text-gray-500 dark:text-gray-500">Lade Context-Dateien hoch, um sie in Chats zu verwenden</p>
        </div>
      </div>
    </div>

    <!-- Reassign Project Modal -->
    <div
      v-if="showReassignModal"
      @click="showReassignModal = false"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
    >
      <div @click.stop class="bg-white dark:bg-gray-800 rounded-lg p-6 w-96 shadow-xl">
        <h3 class="text-lg font-bold mb-4 text-gray-900 dark:text-white">Chat zu anderem Projekt zuordnen</h3>
        <div class="mb-4">
          <label class="block text-sm text-gray-600 dark:text-gray-400 mb-2">Chat: {{ reassigningChat?.title }}</label>
          <select
            v-model="selectedProjectId"
            class="w-full px-4 py-2 bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:border-fleet-orange-500 text-gray-900 dark:text-white"
          >
            <option
              v-for="proj in allProjects.filter(p => p.id !== project.id)"
              :key="proj.id"
              :value="proj.id"
            >
              {{ proj.name }}
            </option>
          </select>
          <p v-if="allProjects.filter(p => p.id !== project.id).length === 0" class="mt-2 text-sm text-gray-500 dark:text-gray-400">
            Keine anderen Projekte verfügbar
          </p>
        </div>
        <div class="flex justify-end space-x-2">
          <button
            @click="showReassignModal = false"
            class="px-4 py-2 bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 text-gray-900 dark:text-white rounded-lg transition-colors"
          >
            Abbrechen
          </button>
          <button
            @click="confirmReassign"
            class="px-4 py-2 bg-fleet-orange-500 hover:bg-fleet-orange-600 text-white rounded-lg transition-colors"
          >
            Zuordnen
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, defineEmits, onMounted, onBeforeUnmount } from 'vue'
import {
  XMarkIcon,
  FolderIcon,
  DocumentTextIcon,
  ChatBubbleLeftRightIcon,
  CpuChipIcon,
  ChartBarIcon,
  ExclamationTriangleIcon,
  EllipsisVerticalIcon,
  CloudArrowUpIcon,
  TrashIcon
} from '@heroicons/vue/24/outline'
import { useChatStore } from '../stores/chatStore'
import { getModelContextWindow, getSafeContextLimit } from '../utils/modelContextWindows'
import api from '../services/api'
import ChatWindow from './ChatWindow.vue'

const props = defineProps({
  project: {
    type: Object,
    required: true
  },
  projectChats: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'refresh'])

const chatStore = useChatStore()
const activeTab = ref('chats')
const fileInput = ref(null)
const openChatMenuId = ref(null)
const showReassignModal = ref(false)
const reassigningChat = ref(null)
const selectedProjectId = ref(null)
const allProjects = ref([])

// Close menu when clicking outside
onMounted(() => {
  document.addEventListener('click', closeMenus)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeMenus)
})

function closeMenus() {
  openChatMenuId.value = null
}

// Check if a chat from this project is currently open
const isChatOpen = computed(() => {
  return chatStore.currentChat && chatStore.currentChat.projectId === props.project.id
})

// Calculate total context tokens
const totalContextTokens = computed(() => {
  if (!props.project.contextFiles) return 0
  return props.project.contextFiles.reduce((sum, file) => {
    return sum + (file.estimatedTokens || Math.floor(file.size / 4))
  }, 0)
})

// Get context limit based on current model
const contextLimit = computed(() => {
  return getSafeContextLimit(chatStore.selectedModel)
})

// Calculate context usage percentage
const contextUsagePercent = computed(() => {
  if (contextLimit.value === 0) return 0
  return Math.round((totalContextTokens.value / contextLimit.value) * 100)
})

function getContextUsageColor() {
  const percent = contextUsagePercent.value
  if (percent < 50) return 'text-green-600 dark:text-green-400'
  if (percent < 80) return 'text-yellow-600 dark:text-yellow-400'
  if (percent < 95) return 'text-orange-600 dark:text-orange-400'
  return 'text-red-600 dark:text-red-400'
}

function getContextUsageBarColor() {
  const percent = contextUsagePercent.value
  if (percent < 50) return 'bg-green-500'
  if (percent < 80) return 'bg-yellow-500'
  if (percent < 95) return 'bg-orange-500'
  return 'bg-red-500'
}

async function selectChat(chat) {
  await chatStore.loadChatHistory(chat.id)
}

function showChatList() {
  // Clear current chat to return to chat list
  if (isChatOpen.value) {
    chatStore.currentChat = null
  }
}

async function handleFileUpload(event) {
  const file = event.target.files[0]
  if (!file) return

  try {
    const reader = new FileReader()
    reader.onload = async (e) => {
      const content = e.target.result

      await api.uploadContextFile({
        projectId: props.project.id,
        filename: file.name,
        content: content,
        fileType: file.type || 'text/plain',
        size: file.size
      })

      emit('refresh')
    }
    reader.readAsText(file)
  } catch (err) {
    alert('Fehler beim Hochladen der Datei')
    console.error(err)
  }

  event.target.value = ''
}

async function deleteFile(fileId) {
  if (confirm('Diese Datei löschen?')) {
    try {
      await api.deleteContextFile(fileId)
      emit('refresh')
    } catch (err) {
      alert('Fehler beim Löschen der Datei')
      console.error(err)
    }
  }
}

// Chat menu functions
function toggleChatMenu(chatId) {
  openChatMenuId.value = openChatMenuId.value === chatId ? null : chatId
}

async function deleteChat(chatId) {
  openChatMenuId.value = null
  if (confirm('Diesen Chat wirklich löschen?')) {
    try {
      await chatStore.deleteChat(chatId)
      emit('refresh')
    } catch (err) {
      alert('Fehler beim Löschen des Chats')
      console.error(err)
    }
  }
}

async function removeFromProject(chat) {
  openChatMenuId.value = null
  if (confirm(`Chat "${chat.title}" aus diesem Projekt entfernen?`)) {
    try {
      await api.unassignChatFromProject(chat.id)
      await chatStore.loadChats()
      emit('refresh')
    } catch (err) {
      alert('Fehler beim Entfernen des Chats aus dem Projekt')
      console.error(err)
    }
  }
}

async function reassignChat(chat) {
  openChatMenuId.value = null
  reassigningChat.value = chat

  // Load all projects
  try {
    allProjects.value = await api.getAllProjects()
    // Pre-select another project if available
    const otherProjects = allProjects.value.filter(p => p.id !== props.project.id)
    selectedProjectId.value = otherProjects.length > 0 ? otherProjects[0].id : null
  } catch (err) {
    console.error('Failed to load projects:', err)
  }

  showReassignModal.value = true
}

async function confirmReassign() {
  if (!reassigningChat.value || !selectedProjectId.value) return

  try {
    await api.assignChatToProject(reassigningChat.value.id, selectedProjectId.value)
    await chatStore.loadChats()
    emit('refresh')
    showReassignModal.value = false
    reassigningChat.value = null
  } catch (err) {
    alert('Fehler beim Zuordnen des Chats zu anderem Projekt')
    console.error(err)
  }
}

function formatDate(dateString) {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return 'Just now'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`
  return date.toLocaleDateString()
}

function formatNumber(num) {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

function formatFileSize(bytes) {
  if (bytes >= 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
  if (bytes >= 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return bytes + ' B'
}
</script>

<style scoped>
/* Fade Transition */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
