<template>
  <div class="h-screen flex flex-col overflow-hidden bg-gray-50 dark:bg-gray-900 text-gray-900 dark:text-gray-100" :class="darkMode ? 'dark' : ''">
    <!-- Top Bar (Full Width) -->
    <TopBar
      @toggle-monitor="showMonitor = !showMonitor"
      @toggle-model-manager="showModelManager = !showModelManager"
      @toggle-settings="showSettings = !showSettings"
      @open-settings-tab="openSettingsOnTab"
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

      <!-- Main Content (Router View) -->
      <div class="flex-1 flex flex-col overflow-hidden">
        <router-view
          v-slot="{ Component }"
          :selected-project="selectedProject"
          :project-chats="projectChats"
          @close-project="selectedProject = null"
          @refresh-project="refreshProject"
        >
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>

    <!-- System Monitor (collapsible slide-in panel) -->
    <Transition name="slide">
      <SystemMonitor v-if="showMonitor" @close="showMonitor = false" />
    </Transition>

    <!-- Model Manager Modal -->
    <Transition name="fade">
      <ModelManager v-if="showModelManager" @close="showModelManager = false" />
    </Transition>

    <!-- Settings Modal -->
    <Transition name="fade">
      <SettingsModal v-if="showSettings" :is-open="showSettings" :initial-tab="settingsInitialTab" @close="showSettings = false; settingsInitialTab = null" />
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
import Sidebar from '../Sidebar.vue'
import TopBar from '../TopBar.vue'
import SystemMonitor from '../SystemMonitor.vue'
import ModelManager from '../ModelManager.vue'
import SettingsModal from '../SettingsModal.vue'
import ToastContainer from '../ToastContainer.vue'
import { useChatStore } from '../../stores/chatStore'
import api from '../../services/api'

const chatStore = useChatStore()
const showMonitor = ref(false)
const showModelManager = ref(false)
const showSettings = ref(false)
const showAbortModal = ref(false)
const selectedProject = ref(null)
const settingsInitialTab = ref(null)

// Get chats that belong to the selected project
const projectChats = computed(() => {
  if (!selectedProject.value) return []
  return chatStore.chats.filter(chat => chat.projectId === selectedProject.value.id)
})

// Dark Mode IMMER als Default
const darkMode = ref(true)

onMounted(async () => {
  darkMode.value = true
  localStorage.setItem('darkMode', 'true')
  await chatStore.loadModels()
})

// Toggle function
function toggleDarkMode() {
  darkMode.value = !darkMode.value
  localStorage.setItem('darkMode', JSON.stringify(darkMode.value))
}

function openSettingsOnTab(tabName) {
  settingsInitialTab.value = tabName
  showSettings.value = true
}

// Watch for chat changes - close project view if chat is not part of current project
watch(() => chatStore.currentChat, (newChat) => {
  if (newChat && selectedProject.value) {
    if (!newChat.projectId || newChat.projectId !== selectedProject.value.id) {
      selectedProject.value = null
    }
  }
})

// Handle project selection
async function handleSelectProject(project) {
  try {
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

// Provide to child components
provide('darkMode', darkMode)
provide('showAbortModal', showAbortModal)
provide('selectedProject', selectedProject)
provide('projectChats', projectChats)
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
