<template>
  <Transition name="fade">
    <div
      v-if="visible"
      class="fixed inset-0 z-[100] flex items-center justify-center bg-gray-900/95 backdrop-blur-md"
    >
      <div class="text-center">
        <!-- Animated Logo/Icon -->
        <div class="relative mb-8">
          <!-- Outer pulsing ring -->
          <div class="absolute inset-0 flex items-center justify-center">
            <div class="w-32 h-32 rounded-full border-4 border-fleet-orange-500/30 animate-ping"></div>
          </div>
          <!-- Middle rotating ring -->
          <div class="absolute inset-0 flex items-center justify-center">
            <div class="w-28 h-28 rounded-full border-4 border-t-fleet-orange-500 border-r-transparent border-b-transparent border-l-transparent animate-spin"></div>
          </div>
          <!-- Inner icon -->
          <div class="relative w-32 h-32 flex items-center justify-center">
            <div class="w-20 h-20 rounded-2xl bg-gradient-to-br from-fleet-orange-500 to-orange-600 flex items-center justify-center shadow-2xl shadow-orange-500/30">
              <CpuChipIcon class="w-10 h-10 text-white" />
            </div>
          </div>
        </div>

        <!-- Status Text -->
        <h2 class="text-2xl font-bold text-white mb-3">
          AI wird gestartet
        </h2>
        <p class="text-lg text-gray-300 mb-6 min-h-[28px]">
          {{ statusMessage }}
        </p>

        <!-- Progress dots -->
        <div class="flex justify-center gap-2 mb-6">
          <div
            v-for="i in 3"
            :key="i"
            class="w-3 h-3 rounded-full bg-fleet-orange-500"
            :class="{ 'animate-bounce': true }"
            :style="{ animationDelay: `${(i - 1) * 0.15}s` }"
          ></div>
        </div>

        <!-- Error message -->
        <div
          v-if="errorMessage"
          class="mt-4 px-6 py-3 bg-red-500/20 border border-red-500/50 rounded-xl text-red-300 max-w-md mx-auto"
        >
          <p class="text-sm">{{ errorMessage }}</p>
          <button
            @click="dismissOverlay"
            class="mt-3 px-4 py-2 bg-red-600 hover:bg-red-500 text-white rounded-lg text-sm transition-colors"
          >
            Trotzdem fortfahren
          </button>
        </div>

        <!-- Skip button (appears after 10 seconds) -->
        <Transition name="fade">
          <button
            v-if="showSkipButton && !errorMessage"
            @click="dismissOverlay"
            class="mt-8 px-4 py-2 text-gray-400 hover:text-white text-sm transition-colors underline"
          >
            Überspringen
          </button>
        </Transition>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { CpuChipIcon } from '@heroicons/vue/24/outline'
import api from '../services/api'

const visible = ref(true)
const statusMessage = ref('Initialisiere...')
const errorMessage = ref(null)
const showSkipButton = ref(false)

let pollInterval = null
let skipTimeout = null

onMounted(() => {
  // Start polling for status
  checkStatus()
  pollInterval = setInterval(checkStatus, 1000)

  // Show skip button after 10 seconds
  skipTimeout = setTimeout(() => {
    showSkipButton.value = true
  }, 10000)
})

onUnmounted(() => {
  if (pollInterval) clearInterval(pollInterval)
  if (skipTimeout) clearTimeout(skipTimeout)
})

async function checkStatus() {
  try {
    const status = await api.getAiStartupStatus()

    statusMessage.value = status.message || 'Starte Server...'

    if (status.error) {
      errorMessage.value = status.error
    }

    // Hide overlay when server is online
    if (status.serverOnline) {
      statusMessage.value = 'AI bereit!'
      // Short delay to show "AI bereit!" message
      setTimeout(() => {
        visible.value = false
      }, 800)
      if (pollInterval) {
        clearInterval(pollInterval)
        pollInterval = null
      }
    }

    // Also hide if startup complete (even with error, after showing message)
    if (status.complete && !status.inProgress && !status.serverOnline) {
      // If there's an error, keep showing until user dismisses
      // If no error but also no server, hide after delay
      if (!status.error) {
        setTimeout(() => {
          visible.value = false
        }, 2000)
      }
    }

  } catch (err) {
    // API not ready yet, keep showing loading
    console.debug('AI startup status check failed:', err.message)
  }
}

function dismissOverlay() {
  visible.value = false
  if (pollInterval) {
    clearInterval(pollInterval)
    pollInterval = null
  }
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

.animate-bounce {
  animation: bounce 0.6s ease-in-out infinite;
}
</style>
