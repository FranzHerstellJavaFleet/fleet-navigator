<template>
  <!-- Modal Overlay -->
  <Transition name="modal">
    <div
      v-if="isVisible"
      class="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-[100] p-4"
      @click.self="preventClose"
    >
      <!-- Modal Dialog -->
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-2xl border border-gray-200 dark:border-gray-700 overflow-hidden">

        <!-- Header -->
        <div class="bg-gradient-to-r from-blue-500 to-blue-600 dark:from-blue-600 dark:to-blue-700 px-6 py-5">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <div class="p-2 bg-white/20 rounded-lg">
                <svg class="w-8 h-8 text-white animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
              </div>
              <div>
                <h2 class="text-2xl font-bold text-white">Modell wird heruntergeladen</h2>
                <p class="text-blue-100 text-sm mt-1">{{ currentModel }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Content -->
        <div class="p-8">

          <!-- Progress Info -->
          <div class="mb-6 text-center">
            <div class="text-6xl font-bold text-gray-900 dark:text-white mb-2">
              {{ progress }}%
            </div>
            <div class="text-lg text-gray-600 dark:text-gray-400">
              {{ downloadedSize }} / {{ totalSize }}
            </div>
          </div>

          <!-- Progress Bar -->
          <div class="mb-6">
            <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-6 overflow-hidden shadow-inner">
              <div
                class="bg-gradient-to-r from-blue-500 to-blue-600 h-6 rounded-full transition-all duration-300 flex items-center justify-end px-2"
                :style="{ width: progress + '%' }"
              >
                <span v-if="progress > 10" class="text-xs font-semibold text-white">
                  {{ progress }}%
                </span>
              </div>
            </div>
          </div>

          <!-- Speed & Time -->
          <div class="grid grid-cols-2 gap-4 mb-6">
            <div class="bg-blue-50 dark:bg-blue-900/20 rounded-lg p-4 border border-blue-200 dark:border-blue-800">
              <div class="text-sm text-blue-600 dark:text-blue-400 mb-1">Download-Geschwindigkeit</div>
              <div class="text-2xl font-bold text-blue-900 dark:text-blue-100">
                {{ speed }} MB/s
              </div>
            </div>
            <div class="bg-purple-50 dark:bg-purple-900/20 rounded-lg p-4 border border-purple-200 dark:border-purple-800">
              <div class="text-sm text-purple-600 dark:text-purple-400 mb-1">Verbleibende Zeit</div>
              <div class="text-2xl font-bold text-purple-900 dark:text-purple-100">
                {{ estimatedTime }}
              </div>
            </div>
          </div>

          <!-- Info Box -->
          <div class="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-4 mb-6">
            <div class="flex items-start gap-3">
              <svg class="w-6 h-6 text-yellow-600 dark:text-yellow-400 flex-shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <div>
                <div class="font-semibold text-yellow-900 dark:text-yellow-100 mb-1">
                  ⏱️ Bitte habe etwas Geduld
                </div>
                <div class="text-sm text-yellow-800 dark:text-yellow-200">
                  Der Download kann je nach Modellgröße und Internetverbindung <strong>mehrere Minuten</strong> dauern.
                  Bitte schließe dieses Fenster nicht und starte keinen weiteren Download.
                </div>
              </div>
            </div>
          </div>

          <!-- Status Messages -->
          <div class="bg-gray-50 dark:bg-gray-900/50 rounded-lg p-4 max-h-32 overflow-y-auto custom-scrollbar">
            <div class="space-y-1">
              <div
                v-for="(message, idx) in statusMessages.slice(-5)"
                :key="idx"
                class="text-sm text-gray-600 dark:text-gray-400 font-mono"
              >
                {{ message }}
              </div>
            </div>
          </div>

        </div>

        <!-- Footer -->
        <div class="bg-gray-50 dark:bg-gray-900/50 px-8 py-5 flex justify-between items-center border-t border-gray-200 dark:border-gray-700">
          <div class="text-sm text-gray-500 dark:text-gray-400">
            🔒 Download läuft - Bitte warten...
          </div>
          <button
            @click="confirmCancel"
            class="px-6 py-2.5 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 rounded-lg font-medium hover:bg-red-200 dark:hover:bg-red-900/50 transition-colors border border-red-200 dark:border-red-800"
          >
            ✕ Abbrechen
          </button>
        </div>

      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useConfirmDialog } from '../composables/useConfirmDialog'

const { confirm } = useConfirmDialog()

const props = defineProps({
  isVisible: {
    type: Boolean,
    required: true
  },
  currentModel: {
    type: String,
    default: ''
  },
  progress: {
    type: Number,
    default: 0
  },
  downloadedSize: {
    type: String,
    default: '0 MB'
  },
  totalSize: {
    type: String,
    default: '0 MB'
  },
  speed: {
    type: [String, Number],
    default: '0.0'
  },
  statusMessages: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['cancel'])

const estimatedTime = computed(() => {
  if (!props.speed || parseFloat(props.speed) === 0) {
    return '-- min'
  }

  // Parse sizes (handle both "1.2 GB" and "500 MB" formats)
  const downloadedStr = props.downloadedSize.toString()
  const totalStr = props.totalSize.toString()
  const speedMB = parseFloat(props.speed)

  // Convert to MB
  const downloaded = parseSizeToMB(downloadedStr)
  const total = parseSizeToMB(totalStr)

  if (!downloaded || !total || !speedMB || downloaded >= total) {
    return '-- min'
  }

  const remaining = total - downloaded
  const secondsRemaining = remaining / speedMB

  if (secondsRemaining < 0) {
    return '-- min'
  }

  if (secondsRemaining < 60) {
    return Math.ceil(secondsRemaining) + ' sek'
  } else {
    const minutes = Math.ceil(secondsRemaining / 60)
    return minutes + ' min'
  }
})

function parseSizeToMB(sizeStr) {
  const match = sizeStr.match(/([\d.]+)\s*([GM]B)/)
  if (!match) return 0

  const value = parseFloat(match[1])
  const unit = match[2]

  if (unit === 'GB') {
    return value * 1024  // Convert GB to MB
  } else {
    return value  // Already in MB
  }
}

function preventClose() {
  // Don't close on backdrop click during download
}

async function confirmCancel() {
  const confirmed = await confirm({
    title: 'Download abbrechen?',
    message: 'Möchtest du den Download wirklich abbrechen? Der Fortschritt geht verloren.',
    type: 'warning',
    confirmText: 'Abbrechen'
  })
  if (confirmed) {
    emit('cancel')
  }
}
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active .bg-white,
.modal-leave-active .bg-white {
  transition: transform 0.3s ease;
}

.modal-enter-from .bg-white,
.modal-leave-to .bg-white {
  transform: scale(0.95);
}

.custom-scrollbar {
  scrollbar-width: thin;
  scrollbar-color: rgb(156 163 175) rgb(243 244 246);
}

.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: rgb(243 244 246);
  border-radius: 3px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgb(156 163 175);
  border-radius: 3px;
}

.dark .custom-scrollbar {
  scrollbar-color: rgb(75 85 99) rgb(31 41 55);
}

.dark .custom-scrollbar::-webkit-scrollbar-track {
  background: rgb(31 41 55);
}

.dark .custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgb(75 85 99);
}
</style>
