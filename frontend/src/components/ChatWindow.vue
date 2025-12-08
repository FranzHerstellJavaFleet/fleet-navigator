<template>
  <div class="chat-window-container relative bg-gradient-to-b from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-950">
    <!-- Error Banner - Shows LLM/Server errors to user -->
    <Transition
      enter-active-class="transition-all duration-300 ease-out"
      enter-from-class="opacity-0 -translate-y-full"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition-all duration-200 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-full"
    >
      <div
        v-if="chatStore.error"
        class="absolute top-0 left-0 right-0 z-50 mx-4 mt-4"
      >
        <div class="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-800 rounded-xl p-4 shadow-lg">
          <div class="flex items-start gap-3">
            <div class="flex-shrink-0">
              <svg class="w-6 h-6 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <div class="flex-1">
              <h4 class="font-semibold text-red-800 dark:text-red-200 mb-1">KI-Fehler</h4>
              <p class="text-red-700 dark:text-red-300 text-sm">{{ chatStore.error }}</p>
            </div>
            <button
              @click="chatStore.clearError()"
              class="flex-shrink-0 p-1 rounded-lg hover:bg-red-100 dark:hover:bg-red-800/50 transition-colors"
            >
              <svg class="w-5 h-5 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Messages Area with Custom Scrollbar - scrollt bis zum Bildschirmende -->
    <div ref="messagesContainer" class="messages-area overflow-y-auto p-6 pb-32 space-y-4 custom-scrollbar">
      <!-- Welcome Message - Show when NO chat is selected -->
      <div v-if="!chatStore.currentChat" class="flex items-center justify-center min-h-full">
        <div class="text-center max-w-3xl mx-auto px-4">
          <!-- System Health Banner -->
          <SystemHealthBanner class="mb-6 text-left" />

          <!-- FLIP-FLOP: Werbung ODER personalisierte Begrüßung -->

          <!-- Option A: Werbung (Logo, Standard-Welcome, Tiles) -->
          <template v-if="settingsStore.settings.showWelcomeTiles">
            <!-- Animated Logo - JavaFleet Segelboot -->
            <div class="mb-8 flex justify-center">
              <div class="relative">
                <div class="absolute inset-0 bg-gradient-to-r from-fleet-orange-400 to-fleet-orange-600 rounded-full blur-3xl opacity-20 animate-pulse"></div>
                <div class="relative transform hover:scale-110 transition-transform duration-300">
                  <img
                    src="/javafleet-logo.png"
                    alt="JavaFleet Logo"
                    class="w-48 h-auto rounded-2xl shadow-2xl"
                  />
                </div>
              </div>
            </div>

            <!-- Welcome Text -->
            <h2 class="text-3xl font-bold bg-gradient-to-r from-gray-800 to-gray-600 dark:from-gray-100 dark:to-gray-300 bg-clip-text text-transparent mb-3">
              {{ t('welcome.title') }}
            </h2>
            <p class="text-lg text-gray-600 dark:text-gray-400 mb-2">
              {{ t('welcome.subtitle') }}
            </p>
            <p class="text-sm text-gray-500 dark:text-gray-500 mb-8">
              {{ t('app.poweredBy') }}
            </p>
          </template>

          <!-- Option B: Personalisierte Begrüßung (wenn Werbung aus) -->
          <template v-else>
            <div class="py-8">
              <!-- Personalisierte Begrüßung -->
              <h2 v-if="personalGreeting" class="greeting-text text-4xl font-bold bg-gradient-to-r from-fleet-orange-500 to-orange-600 bg-clip-text text-transparent mb-4">
                {{ personalGreeting }}
              </h2>
              <h2 v-else class="text-3xl font-bold text-gray-700 dark:text-gray-300 mb-4">
                Willkommen zurück!
              </h2>
              <p class="text-lg text-gray-500 dark:text-gray-400 mb-8">
                Wie kann ich dir heute helfen?
              </p>
              <!-- Zentrierte Eingabe im Hero-Stil -->
              <div class="max-w-2xl mx-auto">
                <MessageInput @send="handleSendMessage" :hero-mode="true" />
              </div>
            </div>
          </template>

          <!-- Suggestion Cards - nur wenn Werbung an -->
          <div v-if="settingsStore.settings.showWelcomeTiles" class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <!-- Brief schreiben -->
            <button
              @click="sendSuggestion(t('welcome.suggestions.letter.prompt'))"
              class="
                group p-5 rounded-2xl
                bg-white/80 dark:bg-gray-800/80
                backdrop-blur-sm
                border border-gray-200/50 dark:border-gray-700/50
                hover:border-fleet-orange-400 dark:hover:border-fleet-orange-500
                transition-all duration-300
                transform hover:scale-105 hover:shadow-xl
                text-left
              "
            >
              <div class="flex items-center gap-3 mb-2">
                <div class="p-2 rounded-xl bg-blue-500/10 group-hover:bg-blue-500/20 transition-colors">
                  <DocumentTextIcon class="w-6 h-6 text-blue-500" />
                </div>
                <div class="font-semibold text-gray-800 dark:text-gray-100">{{ t('welcome.suggestions.letter.title') }}</div>
              </div>
              <div class="text-sm text-gray-500 dark:text-gray-400 ml-11">{{ t('welcome.suggestions.letter.description') }}</div>
            </button>

            <!-- Fragen stellen -->
            <button
              @click="sendSuggestion(t('welcome.suggestions.question.prompt'))"
              class="
                group p-5 rounded-2xl
                bg-white/80 dark:bg-gray-800/80
                backdrop-blur-sm
                border border-gray-200/50 dark:border-gray-700/50
                hover:border-fleet-orange-400 dark:hover:border-fleet-orange-500
                transition-all duration-300
                transform hover:scale-105 hover:shadow-xl
                text-left
              "
            >
              <div class="flex items-center gap-3 mb-2">
                <div class="p-2 rounded-xl bg-green-500/10 group-hover:bg-green-500/20 transition-colors">
                  <LightBulbIcon class="w-6 h-6 text-green-500" />
                </div>
                <div class="font-semibold text-gray-800 dark:text-gray-100">{{ t('welcome.suggestions.question.title') }}</div>
              </div>
              <div class="text-sm text-gray-500 dark:text-gray-400 ml-11">{{ t('welcome.suggestions.question.description') }}</div>
            </button>

            <!-- Übersetzen -->
            <button
              @click="sendSuggestion(t('welcome.suggestions.translate.prompt'))"
              class="
                group p-5 rounded-2xl
                bg-white/80 dark:bg-gray-800/80
                backdrop-blur-sm
                border border-gray-200/50 dark:border-gray-700/50
                hover:border-fleet-orange-400 dark:hover:border-fleet-orange-500
                transition-all duration-300
                transform hover:scale-105 hover:shadow-xl
                text-left
              "
            >
              <div class="flex items-center gap-3 mb-2">
                <div class="p-2 rounded-xl bg-purple-500/10 group-hover:bg-purple-500/20 transition-colors">
                  <LanguageIcon class="w-6 h-6 text-purple-500" />
                </div>
                <div class="font-semibold text-gray-800 dark:text-gray-100">{{ t('welcome.suggestions.translate.title') }}</div>
              </div>
              <div class="text-sm text-gray-500 dark:text-gray-400 ml-11">{{ t('welcome.suggestions.translate.description') }}</div>
            </button>

            <!-- Lernen -->
            <button
              @click="sendSuggestion(t('welcome.suggestions.learn.prompt'))"
              class="
                group p-5 rounded-2xl
                bg-white/80 dark:bg-gray-800/80
                backdrop-blur-sm
                border border-gray-200/50 dark:border-gray-700/50
                hover:border-fleet-orange-400 dark:hover:border-fleet-orange-500
                transition-all duration-300
                transform hover:scale-105 hover:shadow-xl
                text-left
              "
            >
              <div class="flex items-center gap-3 mb-2">
                <div class="p-2 rounded-xl bg-orange-500/10 group-hover:bg-orange-500/20 transition-colors">
                  <AcademicCapIcon class="w-6 h-6 text-orange-500" />
                </div>
                <div class="font-semibold text-gray-800 dark:text-gray-100">{{ t('welcome.suggestions.learn.title') }}</div>
              </div>
              <div class="text-sm text-gray-500 dark:text-gray-400 ml-11">{{ t('welcome.suggestions.learn.description') }}</div>
            </button>

            <!-- Programmieren -->
            <button
              @click="sendSuggestion(t('welcome.suggestions.code.prompt'))"
              class="
                group p-5 rounded-2xl
                bg-white/80 dark:bg-gray-800/80
                backdrop-blur-sm
                border border-gray-200/50 dark:border-gray-700/50
                hover:border-fleet-orange-400 dark:hover:border-fleet-orange-500
                transition-all duration-300
                transform hover:scale-105 hover:shadow-xl
                text-left
              "
            >
              <div class="flex items-center gap-3 mb-2">
                <div class="p-2 rounded-xl bg-indigo-500/10 group-hover:bg-indigo-500/20 transition-colors">
                  <CodeBracketIcon class="w-6 h-6 text-indigo-500" />
                </div>
                <div class="font-semibold text-gray-800 dark:text-gray-100">{{ t('welcome.suggestions.code.title') }}</div>
              </div>
              <div class="text-sm text-gray-500 dark:text-gray-400 ml-11">{{ t('welcome.suggestions.code.description') }}</div>
            </button>

            <!-- Kreativ schreiben -->
            <button
              @click="sendSuggestion(t('welcome.suggestions.creative.prompt'))"
              class="
                group p-5 rounded-2xl
                bg-white/80 dark:bg-gray-800/80
                backdrop-blur-sm
                border border-gray-200/50 dark:border-gray-700/50
                hover:border-fleet-orange-400 dark:hover:border-fleet-orange-500
                transition-all duration-300
                transform hover:scale-105 hover:shadow-xl
                text-left
              "
            >
              <div class="flex items-center gap-3 mb-2">
                <div class="p-2 rounded-xl bg-pink-500/10 group-hover:bg-pink-500/20 transition-colors">
                  <SparklesIcon class="w-6 h-6 text-pink-500" />
                </div>
                <div class="font-semibold text-gray-800 dark:text-gray-100">{{ t('welcome.suggestions.creative.title') }}</div>
              </div>
              <div class="text-sm text-gray-500 dark:text-gray-400 ml-11">{{ t('welcome.suggestions.creative.description') }}</div>
            </button>

            <!-- Zentrierte Eingabe unter den Tiles - NUR wenn Tiles sichtbar -->
            <div class="col-span-full max-w-2xl mx-auto mt-8">
              <MessageInput @send="handleSendMessage" :hero-mode="true" />
            </div>
          </div>
        </div>
      </div>

      <!-- Messages with Smooth Transitions - Only show when chat is selected -->
      <template v-if="chatStore.currentChat">
        <TransitionGroup name="message">
          <div
            v-for="(message, index) in chatStore.messages"
            :key="(message.id || index) + '-' + (message.isStreaming ? 'stream' : 'done')"
            class="message-item"
          >
            <MessageBubble :message="message" @delete="handleDeleteMessage" />
          </div>
        </TransitionGroup>

        <!-- Enhanced Loading Indicator -->
        <div v-if="chatStore.isLoading" class="flex items-start gap-4 p-4">
          <div class="flex-shrink-0">
            <!-- Spinning Globe for Web Search -->
            <div v-if="chatStore.isWebSearching" class="p-3 rounded-2xl bg-gradient-to-br from-blue-500 to-blue-600 shadow-lg">
              <GlobeAltIcon class="w-6 h-6 text-white animate-spin-slow" />
            </div>
            <!-- Normal CPU Icon for regular loading -->
            <div v-else class="p-3 rounded-2xl bg-gradient-to-br from-fleet-orange-500 to-fleet-orange-600 shadow-lg">
              <CpuChipIcon class="w-6 h-6 text-white animate-pulse" />
            </div>
          </div>
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-2">
              <!-- Web Search Animation: Pulsing blue dots -->
              <template v-if="chatStore.isWebSearching">
                <div class="w-2.5 h-2.5 bg-blue-500 rounded-full animate-bounce"></div>
                <div class="w-2.5 h-2.5 bg-blue-500 rounded-full animate-bounce" style="animation-delay: 0.15s"></div>
                <div class="w-2.5 h-2.5 bg-blue-500 rounded-full animate-bounce" style="animation-delay: 0.3s"></div>
                <span class="ml-2 text-blue-500 dark:text-blue-400 font-medium text-sm">
                  {{ loadingText }}
                </span>
              </template>
              <!-- Normal Loading Animation: Orange dots -->
              <template v-else>
                <div class="w-2.5 h-2.5 bg-fleet-orange-500 rounded-full animate-bounce"></div>
                <div class="w-2.5 h-2.5 bg-fleet-orange-500 rounded-full animate-bounce" style="animation-delay: 0.15s"></div>
                <div class="w-2.5 h-2.5 bg-fleet-orange-500 rounded-full animate-bounce" style="animation-delay: 0.3s"></div>
                <span class="ml-2 text-fleet-orange-500 dark:text-fleet-orange-400 font-medium text-sm">
                  {{ loadingText }}
                </span>
              </template>
            </div>
            <!-- Typing indicator bars -->
            <div class="flex gap-1">
              <div class="h-2 w-12 rounded-full animate-pulse" :class="chatStore.isWebSearching ? 'bg-blue-200 dark:bg-blue-800' : 'bg-gray-200 dark:bg-gray-700'"></div>
              <div class="h-2 w-20 rounded-full animate-pulse" :class="chatStore.isWebSearching ? 'bg-blue-200 dark:bg-blue-800' : 'bg-gray-200 dark:bg-gray-700'" style="animation-delay: 0.1s"></div>
              <div class="h-2 w-16 rounded-full animate-pulse" :class="chatStore.isWebSearching ? 'bg-blue-200 dark:bg-blue-800' : 'bg-gray-200 dark:bg-gray-700'" style="animation-delay: 0.2s"></div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- Message Input - fixiert am unteren Rand -->
    <div v-if="chatStore.currentChat" class="fixed-input-container absolute bottom-0 left-0 right-0 z-10">
      <MessageInput @send="handleSendMessage" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, computed, onMounted } from 'vue'
import {
  LightBulbIcon,
  CodeBracketIcon,
  CpuChipIcon,
  DocumentTextIcon,
  LanguageIcon,
  AcademicCapIcon,
  SparklesIcon,
  GlobeAltIcon,
  DocumentArrowDownIcon,
  ArrowPathIcon
} from '@heroicons/vue/24/outline'
import { useChatStore } from '../stores/chatStore'
import { useSettingsStore } from '../stores/settingsStore'
import { useLocale } from '../composables/useLocale'
import MessageBubble from './MessageBubble.vue'
import MessageInput from './MessageInput.vue'
import SystemHealthBanner from './SystemHealthBanner.vue'
import api from '../services/api'
import { secureFetch } from '../utils/secureFetch'

const chatStore = useChatStore()
const settingsStore = useSettingsStore()
const { t } = useLocale()
const messagesContainer = ref(null)
const isGeneratingPdf = ref(false)

// Persönliche Daten für personalisierte Begrüßung
const personalInfo = ref(null)

// Computed: Current expert name for display
const currentExpertName = computed(() => {
  if (chatStore.selectedExpertId) {
    const expert = chatStore.getExpertById(chatStore.selectedExpertId)
    return expert ? expert.name : 'Experte'
  }
  return 'Experte'
})

// Generate PDF summary with expert
async function generateExpertSummaryPdf() {
  if (!chatStore.currentChat?.id || !chatStore.selectedExpertId) return

  isGeneratingPdf.value = true
  try {
    const response = await secureFetch(`/api/chat/${chatStore.currentChat.id}/expert-summary-pdf`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        expertId: chatStore.selectedExpertId
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP error: ${response.status}`)
    }

    // Get the blob and download
    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)

    // Get filename from Content-Disposition header or create default
    const contentDisposition = response.headers.get('Content-Disposition')
    let filename = `Abschlussbericht_${currentExpertName.value}_${new Date().toISOString().split('T')[0]}.pdf`
    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename="?([^";\n]+)"?/)
      if (filenameMatch) {
        filename = filenameMatch[1]
      }
    }

    // Trigger download
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)

    console.log('✅ PDF downloaded:', filename)
  } catch (error) {
    console.error('Failed to generate PDF summary:', error)
    alert('Fehler beim Erstellen der PDF-Zusammenfassung: ' + error.message)
  } finally {
    isGeneratingPdf.value = false
  }
}

// Computed: Personalisierte Begrüßung
const personalGreeting = computed(() => {
  if (!personalInfo.value) return null
  const { title, firstName, lastName } = personalInfo.value

  // Baue den Namen zusammen
  let name = ''
  if (title) name += title + ' '
  if (firstName) name += firstName + ' '
  if (lastName) name += lastName
  name = name.trim()

  if (!name) return null

  // Tageszeit-abhängige Begrüßung
  const hour = new Date().getHours()
  let greeting = 'Hallo'
  if (hour < 12) greeting = 'Guten Morgen'
  else if (hour < 18) greeting = 'Guten Tag'
  else greeting = 'Guten Abend'

  return `${greeting}, ${name}!`
})

// Lade persönliche Daten beim Start
onMounted(async () => {
  try {
    personalInfo.value = await api.getPersonalInfo()
  } catch (error) {
    console.debug('No personal info available')
  }
})

// Computed: Loading-Text basierend auf Websuche-Status
const loadingText = computed(() => {
  if (chatStore.isWebSearching) {
    return t('loading.searchingAndThinking')
  }
  return t('loading.thinking')
})

// Auto-scroll to bottom when new messages arrive
watch(() => chatStore.messages.length, async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTo({
      top: messagesContainer.value.scrollHeight,
      behavior: 'smooth'
    })
  }
})

async function handleSendMessage(messageData) {
  // Handle both string (from suggestions) and object (from MessageInput with files)
  if (typeof messageData === 'string') {
    await chatStore.sendMessage({ text: messageData, files: [] })
  } else {
    await chatStore.sendMessage(messageData)
  }
}

async function handleDeleteMessage(messageId) {
  if (!chatStore.currentChat?.id) return

  try {
    await secureFetch(`/api/chat/${chatStore.currentChat.id}/messages/${messageId}`, {
      method: 'DELETE'
    })
    // Remove message from local state
    chatStore.messages = chatStore.messages.filter(m => m.id !== messageId)
  } catch (err) {
    console.error('Failed to delete message:', err)
  }
}

function sendSuggestion(text) {
  handleSendMessage(text)
}
</script>

<style scoped>
/* Chat Window Layout - Messages scrollen unter dem Input durch */
.chat-window-container {
  height: 100%;
  position: relative;
  overflow: hidden;
}

.messages-area {
  height: 100%;
  overflow-y: auto;
}

/* Custom Scrollbar */
.custom-scrollbar::-webkit-scrollbar {
  width: 8px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.3);
  border-radius: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(156, 163, 175, 0.5);
}

/* Message Transitions */
.message-enter-active {
  animation: message-in 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.message-leave-active {
  animation: message-out 0.3s ease-in;
}

@keyframes message-in {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes message-out {
  from {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateY(-10px) scale(0.95);
  }
}

/* Slow spinning animation for globe icon */
@keyframes spin-slow {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.animate-spin-slow {
  animation: spin-slow 2s linear infinite;
}
</style>
