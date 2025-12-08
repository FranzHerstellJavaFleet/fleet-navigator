<template>
  <Transition name="modal">
    <div v-if="pendingRequests.length > 0" class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div class="bg-white/95 dark:bg-gray-800/95 backdrop-blur-xl rounded-2xl shadow-2xl w-full max-w-md overflow-hidden border border-gray-200/50 dark:border-gray-700/50">
        <!-- Header -->
        <div class="bg-gradient-to-r from-blue-500/10 to-cyan-500/10 dark:from-blue-500/20 dark:to-cyan-500/20 backdrop-blur-sm border-b border-gray-200/50 dark:border-gray-700/50 px-6 py-4">
          <div class="flex items-center gap-3">
            <div class="p-2 rounded-xl bg-gradient-to-br from-blue-500 to-cyan-600 shadow-lg animate-pulse">
              <LinkIcon class="w-6 h-6 text-white" />
            </div>
            <div>
              <h2 class="text-xl font-bold text-gray-900 dark:text-white">Neue Pairing-Anfrage</h2>
              <p class="text-sm text-gray-600 dark:text-gray-400">Ein Fleet Mate möchte sich verbinden</p>
            </div>
          </div>
        </div>

        <!-- Content -->
        <div class="p-6 space-y-6">
          <!-- Current Request -->
          <div v-if="currentRequest" class="space-y-4">
            <!-- Mate Info -->
            <div class="bg-gray-50 dark:bg-gray-900/50 rounded-xl p-4 border border-gray-200 dark:border-gray-700">
              <div class="flex items-center gap-3 mb-3">
                <div class="p-2 rounded-lg" :class="getMateTypeColor(currentRequest.mateType)">
                  <component :is="getMateTypeIcon(currentRequest.mateType)" class="w-5 h-5 text-white" />
                </div>
                <div>
                  <h3 class="font-semibold text-gray-900 dark:text-white">{{ currentRequest.mateName }}</h3>
                  <p class="text-sm text-gray-500 dark:text-gray-400">{{ getMateTypeLabel(currentRequest.mateType) }}</p>
                </div>
              </div>
            </div>

            <!-- Pairing Code -->
            <div class="text-center">
              <p class="text-sm text-gray-600 dark:text-gray-400 mb-2">Pairing-Code</p>
              <div class="flex justify-center gap-2">
                <span
                  v-for="(digit, index) in currentRequest.pairingCode.split('')"
                  :key="index"
                  class="w-12 h-14 flex items-center justify-center text-2xl font-mono font-bold bg-gradient-to-br from-blue-500 to-cyan-500 text-white rounded-lg shadow-lg"
                >
                  {{ digit }}
                </span>
              </div>
              <p class="text-xs text-gray-500 dark:text-gray-500 mt-2">
                Vergleiche diesen Code mit dem auf deinem Mate
              </p>
            </div>

            <!-- Expiry Timer -->
            <div class="text-center">
              <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-400 text-sm">
                <ClockIcon class="w-4 h-4" />
                <span>Läuft ab in {{ timeRemaining }}</span>
              </div>
            </div>

            <!-- Security Warning -->
            <div class="bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700/50 rounded-xl p-4">
              <div class="flex gap-3">
                <ShieldExclamationIcon class="w-5 h-5 text-amber-500 flex-shrink-0 mt-0.5" />
                <div class="text-sm">
                  <p class="font-medium text-amber-800 dark:text-amber-300">Sicherheitshinweis</p>
                  <p class="text-amber-700 dark:text-amber-400 mt-1">
                    Akzeptiere nur Anfragen von Geräten, die du kennst. Nach dem Pairing kann dieser Mate verschlüsselt mit dem Navigator kommunizieren.
                  </p>
                </div>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex gap-3">
            <button
              @click="rejectPairing"
              :disabled="isProcessing"
              class="flex-1 px-4 py-3 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-xl font-medium hover:bg-gray-200 dark:hover:bg-gray-600 transition-all disabled:opacity-50"
            >
              <span class="flex items-center justify-center gap-2">
                <XMarkIcon class="w-5 h-5" />
                Ablehnen
              </span>
            </button>
            <button
              @click="approvePairing"
              :disabled="isProcessing"
              class="flex-1 px-4 py-3 bg-gradient-to-r from-green-500 to-emerald-600 text-white rounded-xl font-medium hover:from-green-600 hover:to-emerald-700 transition-all shadow-lg hover:shadow-xl disabled:opacity-50"
            >
              <span class="flex items-center justify-center gap-2">
                <CheckIcon class="w-5 h-5" />
                {{ isProcessing ? 'Wird verbunden...' : 'Akzeptieren' }}
              </span>
            </button>
          </div>

          <!-- Queue indicator -->
          <div v-if="pendingRequests.length > 1" class="text-center text-sm text-gray-500 dark:text-gray-400">
            {{ pendingRequests.length - 1 }} weitere Anfrage(n) warten
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import {
  LinkIcon,
  ClockIcon,
  CheckIcon,
  XMarkIcon,
  ShieldExclamationIcon,
  ComputerDesktopIcon,
  EnvelopeIcon,
  DocumentIcon,
  GlobeAltIcon
} from '@heroicons/vue/24/outline'

const pendingRequests = ref([])
const isProcessing = ref(false)
let pollInterval = null
let countdownInterval = null
const timeRemainingSeconds = ref(0)

// Current request is the first in queue
const currentRequest = computed(() => pendingRequests.value[0] || null)

// Format time remaining
const timeRemaining = computed(() => {
  const minutes = Math.floor(timeRemainingSeconds.value / 60)
  const seconds = timeRemainingSeconds.value % 60
  return `${minutes}:${seconds.toString().padStart(2, '0')}`
})

// Mate type helpers
const getMateTypeIcon = (type) => {
  switch (type) {
    case 'os': return ComputerDesktopIcon
    case 'mail': return EnvelopeIcon
    case 'office': return DocumentIcon
    case 'browser': return GlobeAltIcon
    default: return ComputerDesktopIcon
  }
}

const getMateTypeColor = (type) => {
  switch (type) {
    case 'os': return 'bg-blue-500'
    case 'mail': return 'bg-purple-500'
    case 'office': return 'bg-green-500'
    case 'browser': return 'bg-orange-500'
    default: return 'bg-gray-500'
  }
}

const getMateTypeLabel = (type) => {
  switch (type) {
    case 'os': return 'System-Agent'
    case 'mail': return 'E-Mail-Agent'
    case 'office': return 'Office-Agent'
    case 'browser': return 'Browser-Agent'
    default: return 'Fleet Mate'
  }
}

// Poll for pending requests
const fetchPendingRequests = async () => {
  try {
    const response = await axios.get('/api/pairing/pending')
    const newRequests = response.data

    // Check if we have new requests
    if (newRequests.length > 0 && pendingRequests.value.length === 0) {
      // Play notification sound if available
      playNotificationSound()
    }

    pendingRequests.value = newRequests

    // Update countdown for current request
    if (currentRequest.value) {
      const expiresAt = new Date(currentRequest.value.expiresAt)
      const now = new Date()
      timeRemainingSeconds.value = Math.max(0, Math.floor((expiresAt - now) / 1000))
    }
  } catch (error) {
    console.error('Failed to fetch pending pairing requests:', error)
  }
}

// Approve current pairing request
const approvePairing = async () => {
  if (!currentRequest.value || isProcessing.value) return

  isProcessing.value = true
  try {
    await axios.post(`/api/pairing/approve/${currentRequest.value.requestId}`)
    // Remove from local list
    pendingRequests.value.shift()
  } catch (error) {
    console.error('Failed to approve pairing:', error)
    alert('Fehler beim Genehmigen: ' + (error.response?.data?.error || error.message))
  } finally {
    isProcessing.value = false
  }
}

// Reject current pairing request
const rejectPairing = async () => {
  if (!currentRequest.value || isProcessing.value) return

  isProcessing.value = true
  try {
    await axios.post(`/api/pairing/reject/${currentRequest.value.requestId}`)
    // Remove from local list
    pendingRequests.value.shift()
  } catch (error) {
    console.error('Failed to reject pairing:', error)
  } finally {
    isProcessing.value = false
  }
}

// Play notification sound
const playNotificationSound = () => {
  try {
    const audio = new Audio('/notification.mp3')
    audio.volume = 0.5
    audio.play().catch(() => {})
  } catch (e) {
    // Ignore audio errors
  }
}

// Countdown timer
const startCountdown = () => {
  countdownInterval = setInterval(() => {
    if (timeRemainingSeconds.value > 0) {
      timeRemainingSeconds.value--
    }
  }, 1000)
}

onMounted(() => {
  // Initial fetch
  fetchPendingRequests()

  // Poll every 2 seconds
  pollInterval = setInterval(fetchPendingRequests, 2000)

  // Start countdown
  startCountdown()
})

onUnmounted(() => {
  if (pollInterval) {
    clearInterval(pollInterval)
  }
  if (countdownInterval) {
    clearInterval(countdownInterval)
  }
})
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

.modal-enter-to,
.modal-leave-from {
  opacity: 1;
  transform: scale(1);
}
</style>
