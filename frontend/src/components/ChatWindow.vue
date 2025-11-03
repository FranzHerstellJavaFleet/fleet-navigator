<template>
  <div class="flex flex-col flex-1 overflow-hidden bg-gradient-to-b from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-950">
    <!-- Messages Area with Custom Scrollbar -->
    <div ref="messagesContainer" class="flex-1 overflow-y-auto p-6 space-y-4 custom-scrollbar">
      <!-- Welcome Message - Show when NO chat is selected -->
      <div v-if="!chatStore.currentChat" class="flex items-center justify-center min-h-full">
        <div class="text-center max-w-3xl mx-auto px-4">
          <!-- System Health Banner -->
          <SystemHealthBanner class="mb-6 text-left" />

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

          <!-- Suggestion Cards - Benutzerfreundliche Einstiegspunkte -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
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
          </div>
        </div>
      </div>

      <!-- Messages with Smooth Transitions - Only show when chat is selected -->
      <template v-if="chatStore.currentChat">
        <TransitionGroup name="message">
          <div
            v-for="(message, index) in chatStore.messages"
            :key="message.id || index"
            class="message-item"
          >
            <MessageBubble :message="message" />
          </div>
        </TransitionGroup>

        <!-- Enhanced Loading Indicator -->
        <div v-if="chatStore.isLoading" class="flex items-start gap-4 p-4">
        <div class="flex-shrink-0">
          <div class="p-3 rounded-2xl bg-gradient-to-br from-fleet-orange-500 to-fleet-orange-600 shadow-lg">
            <CpuChipIcon class="w-6 h-6 text-white animate-pulse" />
          </div>
        </div>
        <div class="flex-1">
          <div class="flex items-center gap-2 mb-2">
            <div class="w-2.5 h-2.5 bg-fleet-orange-500 rounded-full animate-bounce"></div>
            <div class="w-2.5 h-2.5 bg-fleet-orange-500 rounded-full animate-bounce" style="animation-delay: 0.15s"></div>
            <div class="w-2.5 h-2.5 bg-fleet-orange-500 rounded-full animate-bounce" style="animation-delay: 0.3s"></div>
            <span class="ml-2 text-fleet-orange-500 dark:text-fleet-orange-400 font-medium text-sm">
              {{ t('loading.thinking') }}
            </span>
          </div>
          <!-- Typing indicator bars -->
          <div class="flex gap-1">
            <div class="h-2 w-12 bg-gray-200 dark:bg-gray-700 rounded-full animate-pulse"></div>
            <div class="h-2 w-20 bg-gray-200 dark:bg-gray-700 rounded-full animate-pulse" style="animation-delay: 0.1s"></div>
            <div class="h-2 w-16 bg-gray-200 dark:bg-gray-700 rounded-full animate-pulse" style="animation-delay: 0.2s"></div>
          </div>
        </div>
      </div>
      </template>
    </div>

    <!-- Message Input -->
    <MessageInput @send="handleSendMessage" />
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import {
  LightBulbIcon,
  CodeBracketIcon,
  CpuChipIcon,
  DocumentTextIcon,
  LanguageIcon,
  AcademicCapIcon,
  SparklesIcon
} from '@heroicons/vue/24/outline'
import { useChatStore } from '../stores/chatStore'
import { useLocale } from '../composables/useLocale'
import MessageBubble from './MessageBubble.vue'
import MessageInput from './MessageInput.vue'
import SystemHealthBanner from './SystemHealthBanner.vue'

const chatStore = useChatStore()
const { t } = useLocale()
const messagesContainer = ref(null)

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

function sendSuggestion(text) {
  handleSendMessage(text)
}
</script>

<style scoped>
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
</style>
