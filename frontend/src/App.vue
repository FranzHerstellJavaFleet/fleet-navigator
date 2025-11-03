<template>
  <div class="h-screen flex flex-col overflow-hidden bg-gray-50 dark:bg-gray-900 text-gray-900 dark:text-gray-100" :class="darkMode ? 'dark' : ''">
    <!-- Top Bar (Full Width) -->
    <TopBar
      @toggle-monitor="showMonitor = !showMonitor"
      @toggle-model-manager="showModelManager = !showModelManager"
      @toggle-settings="showSettings = !showSettings"
      @toggle-email-agent="showEmailAgent = !showEmailAgent"
      @toggle-document-agent="showDocumentAgent = !showDocumentAgent"
      @toggle-os-agent="showOSAgent = !showOSAgent"
      @toggle-theme="toggleDarkMode"
      :dark-mode="darkMode"
    />

    <!-- Content Area: Sidebar + Main -->
    <div class="flex flex-1 overflow-hidden">
      <!-- Sidebar -->
      <Sidebar
        @select-project="handleSelectProject"
        @new-chat="handleNewChat"
      />

      <!-- Main Content -->
      <div class="flex-1 flex flex-col">
        <!-- Project View or Chat Window -->
        <ProjectView
          v-if="selectedProject"
          :project="selectedProject"
          :project-chats="projectChats"
          @close="selectedProject = null"
          @refresh="refreshProject"
        />
        <ChatWindow v-else />
      </div>
    </div>

    <!-- System Monitor (collapsible) -->
    <Transition name="slide">
      <SystemMonitor v-if="showMonitor" @close="showMonitor = false" />
    </Transition>

    <!-- Model Manager Modal -->
    <Transition name="fade">
      <ModelManager v-if="showModelManager" @close="showModelManager = false" />
    </Transition>

    <!-- Settings Modal -->
    <Transition name="fade">
      <SettingsModal v-if="showSettings" :is-open="showSettings" @close="showSettings = false" />
    </Transition>

    <!-- DISTRIBUTED AGENTS MODALS (NEW!) -->
    <!-- Email Agent Modal -->
    <Transition name="fade">
      <EmailAgentModal v-if="showEmailAgent" :show="showEmailAgent" @close="showEmailAgent = false" @saved="handleAgentSettingsSaved" />
    </Transition>

    <!-- Document Agent Modal -->
    <Transition name="fade">
      <DocumentAgentModal v-if="showDocumentAgent" :show="showDocumentAgent" @close="showDocumentAgent = false" @saved="handleAgentSettingsSaved" />
    </Transition>

    <!-- OS Agent Modal -->
    <Transition name="fade">
      <OSAgentModal v-if="showOSAgent" :show="showOSAgent" @close="showOSAgent = false" @saved="handleAgentSettingsSaved" />
    </Transition>

    <!-- Abort Confirmation Modal -->
    <Transition name="fade">
      <div v-if="showAbortModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div class="bg-white dark:bg-gray-800 rounded-lg p-6 max-w-md shadow-xl">
          <h3 class="text-lg font-bold mb-2 text-gray-900 dark:text-white">✋ Anfrage abgebrochen</h3>
          <p class="text-gray-600 dark:text-gray-300 mb-4">
            Die AI-Anfrage wurde erfolgreich abgebrochen.
          </p>
          <button
            @click="showAbortModal = false"
            class="w-full px-4 py-2 bg-fleet-orange-500 hover:bg-fleet-orange-600 text-white rounded-lg transition-colors"
          >
            OK
          </button>
        </div>
      </div>
    </Transition>

    <!-- Toast Notifications -->
    <ToastContainer />
  </div>
</template>

<script setup>
import { ref, provide, onMounted, watch, computed } from 'vue'
import Sidebar from './components/Sidebar.vue'
import TopBar from './components/TopBar.vue'
import ChatWindow from './components/ChatWindow.vue'
import SystemMonitor from './components/SystemMonitor.vue'
import ModelManager from './components/ModelManager.vue'
import SettingsModal from './components/SettingsModal.vue'
import EmailAgentModal from './components/EmailAgentModal.vue'
import DocumentAgentModal from './components/DocumentAgentModal.vue'
import OSAgentModal from './components/OSAgentModal.vue'
import ProjectView from './components/ProjectView.vue'
import ToastContainer from './components/ToastContainer.vue'
import { useChatStore } from './stores/chatStore'
import api from './services/api'

const chatStore = useChatStore()
const showMonitor = ref(false)
const showModelManager = ref(false)
const showSettings = ref(false)
const showEmailAgent = ref(false)
const showDocumentAgent = ref(false)
const showOSAgent = ref(false)
const showAbortModal = ref(false)
const selectedProject = ref(null)

// Get chats that belong to the selected project
const projectChats = computed(() => {
  if (!selectedProject.value) return []
  return chatStore.chats.filter(chat => chat.projectId === selectedProject.value.id)
})

// Dark Mode IMMER als Default (localStorage wird ignoriert beim ersten Mal)
const darkMode = ref(true)

onMounted(() => {
  // Setze Dark Mode immer auf true beim ersten Laden
  // (überschreibt alte localStorage-Werte)
  darkMode.value = true
  localStorage.setItem('darkMode', 'true')
})

// Toggle function
function toggleDarkMode() {
  darkMode.value = !darkMode.value
  localStorage.setItem('darkMode', JSON.stringify(darkMode.value))
}

// Watch for chat changes - close project view if chat is not part of current project
watch(() => chatStore.currentChat, (newChat) => {
  if (newChat && selectedProject.value) {
    // If the chat is not assigned to the current project, close project view
    if (!newChat.projectId || newChat.projectId !== selectedProject.value.id) {
      selectedProject.value = null
    }
  }
})

// Handle project selection
async function handleSelectProject(project) {
  try {
    // Reload project with full details
    const fullProject = await api.getProject(project.id)
    selectedProject.value = fullProject
  } catch (err) {
    console.error('Failed to load project:', err)
    selectedProject.value = project
  }
}

// Handle new chat - close project view and start new chat
function handleNewChat() {
  selectedProject.value = null
  chatStore.startNewChat()
}

// Refresh project after file changes
async function refreshProject() {
  if (!selectedProject.value) return
  try {
    const fullProject = await api.getProject(selectedProject.value.id)
    selectedProject.value = fullProject
  } catch (err) {
    console.error('Failed to refresh project:', err)
  }
}

// Handler for agent settings saved
const handleAgentSettingsSaved = () => {
  console.log('Agent settings saved successfully')
  // Optionally reload data or show notification
}

// Provide to child components
provide('darkMode', darkMode)
provide('showAbortModal', showAbortModal)
</script>

<style>
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>

<style scoped>
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease;
}

.slide-enter-from {
  transform: translateX(100%);
}

.slide-leave-to {
  transform: translateX(100%);
}
</style>
