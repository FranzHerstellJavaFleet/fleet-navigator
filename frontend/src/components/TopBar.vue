<template>
  <div class="
    bg-white/80 dark:bg-gray-800/80
    backdrop-blur-xl backdrop-saturate-150
    border-b border-gray-200/50 dark:border-gray-700/50
    px-6 py-3
    shadow-sm
  ">
    <div class="flex items-center justify-between">
      <!-- Left Side: Hamburger + Logo + Title -->
      <div class="flex items-center space-x-4">
        <!-- Hamburger Menu Button -->
        <button
          @click="settingsStore.toggleSidebar()"
          class="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
          :title="settingsStore.settings.sidebarCollapsed ? 'Sidebar einblenden' : 'Sidebar ausblenden'"
        >
          <Bars3Icon class="w-6 h-6 text-gray-700 dark:text-gray-300" />
        </button>

        <!-- Logo + App Title (clickable link to website) -->
        <a
          href="https://www.java-developer.online"
          target="_blank"
          rel="noopener noreferrer"
          class="flex items-center space-x-3 hover:opacity-80 transition-opacity cursor-pointer"
          title="Visit java-developer.online"
        >
          <Logo :size="32" />
          <h1 class="text-xl font-bold bg-gradient-to-r from-fleet-orange-400 to-fleet-orange-600 bg-clip-text text-transparent">
            Fleet Navigator
          </h1>
        </a>

        <!-- Divider -->
        <div class="h-8 w-px bg-gray-300 dark:bg-gray-600"></div>

        <!-- Current Chat Title -->
        <div class="flex-1">
          <h2 v-if="chatStore.currentChat" class="text-lg font-semibold text-gray-800 dark:text-gray-100">
            {{ chatStore.currentChat.title }}
          </h2>
          <h2 v-else class="text-lg font-semibold text-gray-400 dark:text-gray-500">
            Wähle oder erstelle einen neuen Chat
          </h2>
        </div>
      </div>

      <!-- Right Side Controls -->
      <div class="flex items-center space-x-2">
        <!-- Current Model Display -->
        <div class="flex items-center space-x-3 mr-2">
          <!-- System Prompt Title -->
          <div v-if="chatStore.systemPromptTitle" class="
            flex items-center space-x-2 px-3 py-2
            bg-gradient-to-br from-purple-100 to-purple-50
            dark:from-purple-900/30 dark:to-purple-800/20
            rounded-lg border border-purple-200 dark:border-purple-700/50
            shadow-sm
          ">
            <ChatBubbleLeftRightIcon class="w-4 h-4 text-purple-600 dark:text-purple-400" />
            <span class="text-sm font-medium text-purple-900 dark:text-purple-100">
              {{ chatStore.systemPromptTitle }}
            </span>
          </div>

          <!-- Model -->
          <div class="
            flex items-center space-x-2 px-3 py-2
            bg-gradient-to-br from-gray-100 to-gray-50
            dark:from-gray-700/50 dark:to-gray-800/50
            rounded-lg border border-gray-200 dark:border-gray-700
            shadow-sm
          ">
            <CpuChipIcon class="w-4 h-4 text-gray-600 dark:text-gray-400" />
            <span class="text-sm font-medium text-gray-900 dark:text-white">
              {{ chatStore.selectedModel }}
            </span>
            <span class="text-xs">⭐</span>
          </div>
        </div>

        <!-- Theme Toggle -->
        <ActionButton
          @click="$emit('toggle-theme')"
          :title="darkMode ? 'Zum hellen Modus wechseln' : 'Zum dunklen Modus wechseln'"
          color="orange"
        >
          <SunIcon v-if="darkMode" class="w-5 h-5" />
          <MoonIcon v-else class="w-5 h-5" />
        </ActionButton>

        <!-- System Prompt Button -->
        <ActionButton
          @click="showSystemPrompt = !showSystemPrompt"
          :active="showSystemPrompt"
          title="System Prompt bearbeiten"
          color="orange"
        >
          <ChatBubbleLeftRightIcon class="w-5 h-5" />
        </ActionButton>

        <!-- DISTRIBUTED AGENTS -->
        <!-- Email Agent -->
        <ActionButton
          @click="$emit('toggle-email-agent')"
          title="Email Agent (Coming Soon)"
          color="blue"
          :has-badge="true"
        >
          <EnvelopeIcon class="w-5 h-5" />
        </ActionButton>

        <!-- Document Agent -->
        <ActionButton
          @click="$emit('toggle-document-agent')"
          title="Briefe Agent"
          color="green"
          :has-badge="true"
        >
          <DocumentTextIcon class="w-5 h-5" />
        </ActionButton>

        <!-- OS Agent -->
        <ActionButton
          @click="$emit('toggle-os-agent')"
          title="OS Agent (Coming Soon)"
          color="purple"
          :has-badge="true"
        >
          <CommandLineIcon class="w-5 h-5" />
        </ActionButton>

        <!-- Model Manager -->
        <ActionButton
          @click="$emit('toggle-model-manager')"
          title="Modelle verwalten"
          color="orange"
        >
          <WrenchScrewdriverIcon class="w-5 h-5" />
        </ActionButton>

        <!-- System Monitor -->
        <ActionButton
          @click="$emit('toggle-monitor')"
          title="System Monitor anzeigen"
          color="orange"
        >
          <ChartBarIcon class="w-5 h-5" />
        </ActionButton>

        <!-- Settings -->
        <ActionButton
          @click="$emit('toggle-settings')"
          title="Einstellungen öffnen"
          color="orange"
        >
          <Cog6ToothIcon class="w-5 h-5" />
        </ActionButton>
      </div>
    </div>

    <!-- System Prompt Editor -->
    <Transition
      enter-active-class="transition-all duration-300 ease-out"
      enter-from-class="opacity-0 max-h-0"
      enter-to-class="opacity-100 max-h-96"
      leave-active-class="transition-all duration-200 ease-in"
      leave-from-class="opacity-100 max-h-96"
      leave-to-class="opacity-0 max-h-0"
    >
      <div v-if="showSystemPrompt" class="mt-3 overflow-hidden">
        <!-- Gespeicherte Vorlagen -->
        <div v-if="promptTemplates.length > 0" class="mb-3">
          <div class="text-xs text-gray-500 dark:text-gray-400 mb-2">Gespeicherte Vorlagen:</div>
          <div class="space-y-1 max-h-32 overflow-y-auto">
            <div
              v-for="template in promptTemplates"
              :key="template.id"
              class="
                flex items-center justify-between p-2
                bg-gray-50 dark:bg-gray-700/50
                hover:bg-gray-100 dark:hover:bg-gray-700
                rounded-lg
                border border-transparent hover:border-gray-200 dark:hover:border-gray-600
                text-sm group
                transition-all duration-200
              "
            >
              <button
                @click="loadTemplate(template)"
                class="flex-1 text-left truncate text-gray-700 dark:text-gray-300 hover:text-fleet-orange-500 transition-colors"
              >
                {{ template.name }}
              </button>
              <button
                @click="deleteTemplate(template.id)"
                class="
                  opacity-0 group-hover:opacity-100
                  p-1 rounded
                  text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20
                  transition-all duration-200
                "
              >
                <TrashIcon class="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>

        <!-- Textarea -->
        <textarea
          v-model="chatStore.systemPrompt"
          placeholder="System-Prompt eingeben (z.B. 'Du bist ein hilfreicher Java-Experte...')"
          rows="4"
          class="
            w-full px-4 py-3
            bg-white dark:bg-gray-900
            border border-gray-300 dark:border-gray-600
            text-gray-900 dark:text-gray-100
            rounded-xl
            focus:outline-none focus:ring-2 focus:ring-fleet-orange-500 focus:border-transparent
            text-sm resize-none
            transition-all duration-200
          "
        ></textarea>

        <!-- Buttons -->
        <div class="mt-2 flex justify-between items-center">
          <button
            @click="showSaveTemplateModal = true"
            v-if="chatStore.systemPrompt.trim()"
            class="
              flex items-center gap-2
              px-3 py-1.5 text-sm
              text-fleet-orange-600 dark:text-fleet-orange-400
              hover:text-fleet-orange-700 dark:hover:text-fleet-orange-300
              hover:bg-fleet-orange-50 dark:hover:bg-fleet-orange-900/20
              rounded-lg
              transition-all duration-200
            "
          >
            <BookmarkIcon class="w-4 h-4" />
            Als Vorlage speichern
          </button>
          <div class="flex gap-2">
            <button
              @click="clearSystemPrompt"
              class="
                px-4 py-1.5 text-sm
                text-gray-600 dark:text-gray-400
                hover:text-gray-900 dark:hover:text-gray-100
                hover:bg-gray-100 dark:hover:bg-gray-700
                rounded-lg
                transition-all duration-200
              "
            >
              Löschen
            </button>
            <button
              @click="showSystemPrompt = false"
              class="
                px-4 py-1.5 text-sm
                bg-fleet-orange-500 hover:bg-fleet-orange-600
                text-white
                rounded-lg shadow-sm
                hover:shadow-md
                transition-all duration-200
                transform hover:scale-105 active:scale-95
              "
            >
              Fertig
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Save Template Modal -->
    <Transition name="fade">
      <div v-if="showSaveTemplateModal" class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50">
        <div class="
          bg-white dark:bg-gray-800
          rounded-2xl shadow-2xl
          p-6 w-full max-w-md
          border border-gray-200 dark:border-gray-700
          transform transition-all duration-300
        ">
          <h3 class="text-lg font-bold mb-4 text-gray-900 dark:text-white">Vorlage speichern</h3>
          <input
            v-model="newTemplateName"
            @keyup.enter="saveTemplate"
            type="text"
            placeholder="Name der Vorlage"
            class="
              w-full px-4 py-2 mb-4
              border border-gray-300 dark:border-gray-600
              bg-white dark:bg-gray-900
              text-gray-900 dark:text-white
              rounded-lg
              focus:outline-none focus:ring-2 focus:ring-fleet-orange-500
            "
          />
          <div class="flex justify-end gap-2">
            <button
              @click="showSaveTemplateModal = false"
              class="px-4 py-2 text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
            >
              Abbrechen
            </button>
            <button
              @click="saveTemplate"
              class="px-4 py-2 bg-fleet-orange-500 hover:bg-fleet-orange-600 text-white rounded-lg transition-colors"
            >
              Speichern
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  SunIcon,
  MoonIcon,
  ChatBubbleLeftRightIcon,
  EnvelopeIcon,
  DocumentTextIcon,
  CommandLineIcon,
  WrenchScrewdriverIcon,
  ChartBarIcon,
  Cog6ToothIcon,
  CpuChipIcon,
  TrashIcon,
  BookmarkIcon,
  Bars3Icon
} from '@heroicons/vue/24/outline'
import { useChatStore } from '../stores/chatStore'
import { useSettingsStore } from '../stores/settingsStore'
import api from '../services/api'

// Components
import ActionButton from './ActionButton.vue'
import Logo from './Logo.vue'

defineProps({
  darkMode: Boolean
})

const chatStore = useChatStore()
const settingsStore = useSettingsStore()
const showSystemPrompt = ref(false)
const showSaveTemplateModal = ref(false)
const newTemplateName = ref('')
const promptTemplates = ref([])

onMounted(async () => {
  await loadTemplates()
})

const loadTemplates = async () => {
  try {
    const response = await api.getSystemPrompts()
    promptTemplates.value = response.data
  } catch (error) {
    console.error('Failed to load templates:', error)
  }
}

const loadTemplate = async (template) => {
  chatStore.systemPrompt = template.content
  chatStore.systemPromptTitle = template.name
}

const deleteTemplate = async (id) => {
  if (confirm('Vorlage wirklich löschen?')) {
    try {
      await api.deleteSystemPrompt(id)
      await loadTemplates()
    } catch (error) {
      console.error('Failed to delete template:', error)
    }
  }
}

const saveTemplate = async () => {
  if (!newTemplateName.value.trim()) return

  try {
    await api.createSystemPrompt({
      name: newTemplateName.value,
      content: chatStore.systemPrompt
    })
    await loadTemplates()
    newTemplateName.value = ''
    showSaveTemplateModal.value = false
  } catch (error) {
    console.error('Failed to save template:', error)
  }
}

const clearSystemPrompt = () => {
  chatStore.systemPrompt = ''
  chatStore.systemPromptTitle = null
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
