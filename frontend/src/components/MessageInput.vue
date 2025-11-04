<template>
  <div class="message-input-container p-2 sm:p-4 flex-shrink-0 sticky bottom-0 z-10 relative overflow-visible">
    <!-- Enhanced Glassmorphism Background -->
    <div class="glassmorphism-backdrop"></div>
    <div class="max-w-4xl mx-auto relative z-10">
      <!-- Uploaded Files Display -->
      <TransitionGroup name="file" tag="div" class="mb-2 flex flex-wrap gap-2">
        <div
          v-for="(file, index) in uploadedFiles"
          :key="index"
          class="
            flex items-center gap-2 px-3 py-2 rounded-xl
            bg-gradient-to-br from-gray-100 to-gray-200
            dark:from-gray-700 dark:to-gray-600
            border border-gray-200 dark:border-gray-600
            text-sm
            transition-all duration-200
            hover:scale-105 hover:shadow-md
            group
          "
        >
          <component :is="getFileIcon(file.type)" class="w-4 h-4 text-fleet-orange-500 flex-shrink-0" />
          <span class="max-w-[150px] truncate font-medium">{{ file.name }}</span>
          <button
            @click="removeFile(index)"
            class="
              p-1 rounded-lg
              text-red-500 hover:text-red-700 dark:hover:text-red-400
              hover:bg-red-100 dark:hover:bg-red-900/30
              transition-all duration-200
              opacity-0 group-hover:opacity-100
              transform hover:scale-110
            "
            title="Datei entfernen"
          >
            <XMarkIcon class="w-3 h-3" />
          </button>
        </div>
      </TransitionGroup>

      <!-- Vision Model Warning -->
      <Transition name="fade">
        <div v-if="hasImages && !isVisionModel" class="mb-2 p-3 rounded-xl bg-gradient-to-r from-yellow-50 to-amber-50 dark:from-yellow-900/20 dark:to-amber-900/20 border border-yellow-300 dark:border-yellow-700/50 shadow-sm">
          <div class="flex items-start gap-2">
            <ExclamationTriangleIcon class="w-5 h-5 text-yellow-600 dark:text-yellow-400 flex-shrink-0" />
            <div class="flex-1 text-sm">
              <p class="text-yellow-800 dark:text-yellow-200 font-medium">
                Bild hochgeladen, aber kein Vision Model ausgewählt!
              </p>
              <p v-if="settingsStore.getSetting('autoSelectVisionModel')" class="text-xs mt-1 text-yellow-700 dark:text-yellow-300">
                Beim Senden wird automatisch zu {{ settingsStore.getSetting('preferredVisionModel') }} gewechselt.
              </p>
            </div>
          </div>
        </div>
      </Transition>

      <!-- Error Message -->
      <Transition name="fade">
        <div v-if="errorMessage" class="mb-2 p-3 rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-300 dark:border-red-700/50">
          <div class="flex items-center gap-2">
            <XCircleIcon class="w-5 h-5 text-red-500 flex-shrink-0" />
            <span class="text-sm text-red-700 dark:text-red-300">{{ errorMessage }}</span>
          </div>
        </div>
      </Transition>

      <!-- Main Input Row -->
      <div class="flex items-stretch gap-4 sm:gap-6">
        <!-- File Upload Button -->
        <button
          @click="triggerFileInput"
          class="
            p-2 sm:p-3 rounded-xl
            text-gray-600 dark:text-gray-300
            hover:text-fleet-orange-500 dark:hover:text-fleet-orange-400
            hover:bg-gray-100 dark:hover:bg-gray-700
            transition-all duration-200
            transform hover:scale-110 active:scale-95
            disabled:opacity-50 disabled:cursor-not-allowed
            flex items-center
          "
          :disabled="chatStore.isLoading"
          title="Datei anhängen (PDF, TXT, MD, PNG, JPG)"
        >
          <PaperClipIcon class="w-5 h-5 sm:w-6 sm:h-6" />
        </button>
        <input
          ref="fileInput"
          type="file"
          @change="handleFileSelect"
          accept=".pdf,.txt,.md,.png,.jpg,.jpeg"
          multiple
          class="hidden"
        />

        <!-- Text Input with Glassmorphism -->
        <div class="flex-1 relative">
          <textarea
            v-model="inputText"
            @keydown.enter.exact.prevent="handleSend"
            @keydown.shift.enter="handleNewLine"
            @input="adjustHeight"
            placeholder="Nachricht eingeben... (Shift+Enter für neue Zeile)"
            rows="1"
            class="
              w-full px-4 rounded-xl
              border border-gray-300/30 dark:border-gray-600/30
              bg-white/60 dark:bg-gray-700/60
              backdrop-blur-md
              text-gray-900 dark:text-gray-100
              placeholder-gray-400 dark:placeholder-gray-500
              focus:outline-none focus:ring-0 focus:border-gray-300/30 dark:focus:border-gray-600/30
              resize-none
              transition-all duration-200
              disabled:opacity-50 disabled:cursor-not-allowed
              overflow-hidden
            "
            style="line-height: 1.5rem; padding-top: 0.75rem; padding-bottom: 0.75rem; height: 48px; box-sizing: border-box; outline: none !important; border-radius: 0.75rem; scrollbar-width: none; -ms-overflow-style: none;"
            :disabled="chatStore.isLoading"
            ref="textareaRef"
          ></textarea>
        </div>

        <!-- Stop Button (when loading) -->
        <button
          v-if="chatStore.isLoading"
          @click="handleStop"
          class="
            px-3 sm:px-6 rounded-xl
            bg-gradient-to-r from-red-500 to-red-600
            hover:from-red-400 hover:to-red-500
            text-white font-semibold
            disabled:opacity-50 disabled:cursor-not-allowed
            transition-all duration-200
            flex items-center justify-center gap-2
            flex-shrink-0
          "
          style="height: 48px; box-sizing: border-box; box-shadow: none;"
          title="Generierung stoppen"
        >
          <StopIcon class="w-5 h-5" />
          <span class="hidden sm:inline">Stop</span>
        </button>

        <!-- Send Button -->
        <button
          v-else
          @click="handleSend"
          :disabled="!inputText.trim() || chatStore.isLoading"
          class="
            px-3 sm:px-6 rounded-xl
            bg-gradient-to-r from-fleet-orange-500 to-orange-600
            hover:from-fleet-orange-400 hover:to-orange-500
            text-white font-semibold
            disabled:opacity-50 disabled:cursor-not-allowed
            transition-all duration-200
            flex items-center justify-center gap-2
            flex-shrink-0
          "
          style="height: 48px; box-sizing: border-box; box-shadow: none;"
          title="Nachricht senden (Enter)"
        >
          <PaperAirplaneIcon class="w-5 h-5" />
          <span class="hidden sm:inline">Senden</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, inject, computed, onMounted } from 'vue'
import {
  PaperClipIcon,
  PaperAirplaneIcon,
  XMarkIcon,
  StopIcon,
  ExclamationTriangleIcon,
  XCircleIcon,
  CpuChipIcon,
  BoltIcon,
  DocumentTextIcon,
  ArrowUpTrayIcon,
  DocumentIcon,
  PhotoIcon
} from '@heroicons/vue/24/outline'
import { useChatStore } from '../stores/chatStore'
import { useSettingsStore } from '../stores/settingsStore'
import { useToast } from '../composables/useToast'
import api from '../services/api'

const { success, error: errorToast } = useToast()
const emit = defineEmits(['send'])
const chatStore = useChatStore()
const settingsStore = useSettingsStore()

const inputText = ref('')
const textareaRef = ref(null)
const fileInput = ref(null)
const uploadedFiles = ref([])
const isUploading = ref(false)
const uploadProgress = ref('')
const errorMessage = ref('')

// Get showAbortModal from App.vue
const showAbortModal = inject('showAbortModal')

// Computed properties
const hasImages = computed(() => uploadedFiles.value.some(f => f.type === 'image'))
const isVisionModel = computed(() => settingsStore.isVisionModel(chatStore.selectedModel))

// Get file icon component
function getFileIcon(type) {
  if (type === 'image') return PhotoIcon
  if (type === 'pdf') return DocumentIcon
  return DocumentTextIcon
}

// File Upload Functions
function triggerFileInput() {
  fileInput.value?.click()
}

async function handleFileSelect(event) {
  const files = Array.from(event.target.files)
  await processFiles(files)
  event.target.value = ''
}

async function processFiles(files) {
  errorMessage.value = ''

  for (const file of files) {
    const allowedTypes = ['application/pdf', 'text/plain', 'text/markdown', 'image/png', 'image/jpeg']
    const allowedExtensions = ['.pdf', '.txt', '.md', '.png', '.jpg', '.jpeg']

    const isValidType = allowedTypes.includes(file.type) ||
                       allowedExtensions.some(ext => file.name.toLowerCase().endsWith(ext))

    if (!isValidType) {
      errorMessage.value = `Nicht unterstützt: ${file.name}. Erlaubt: PDF, TXT, MD, PNG, JPG`
      errorToast('Dateityp nicht unterstützt')
      continue
    }

    if (file.size > 50 * 1024 * 1024) {
      errorMessage.value = `Datei zu groß: ${file.name} (max. 50MB)`
      errorToast('Datei zu groß')
      continue
    }

    await uploadFile(file)
  }
}

async function uploadFile(file) {
  isUploading.value = true
  uploadProgress.value = file.name

  try {
    const response = await api.uploadFile(file)

    if (response.success) {
      const uploadedFile = {
        name: response.filename,
        type: response.type,
        textContent: response.textContent || null,
        base64Content: response.base64Content || null,
        size: response.size
      }

      uploadedFiles.value.push(uploadedFile)
      success(`${file.name} hochgeladen`)
    } else {
      errorMessage.value = response.error || 'Upload fehlgeschlagen'
      errorToast('Upload fehlgeschlagen')
    }
  } catch (error) {
    console.error('Upload error:', error)
    errorMessage.value = `Fehler beim Hochladen: ${error.message}`
    errorToast('Upload-Fehler')
  } finally {
    isUploading.value = false
    uploadProgress.value = ''
  }
}

function removeFile(index) {
  uploadedFiles.value.splice(index, 1)
}

// Send Message Functions
function handleSend() {
  if (!inputText.value.trim() || chatStore.isLoading) return

  const messageData = {
    text: inputText.value,
    files: uploadedFiles.value
  }

  emit('send', messageData)
  inputText.value = ''
  uploadedFiles.value = []
  errorMessage.value = ''

  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
  }
}

function handleNewLine() {
  adjustHeight()
}

async function handleStop() {
  console.log('Stop button clicked - aborting request')
  const success = await chatStore.abortCurrentRequest()

  if (success) {
    showAbortModal.value = true
    setTimeout(() => {
      showAbortModal.value = false
    }, 3000)
  }
}

function adjustHeight() {
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
    textareaRef.value.style.height = textareaRef.value.scrollHeight + 'px'
  }
}
</script>

<style scoped>
/* Enhanced Glassmorphism Effect */
.message-input-container {
  position: relative;
}

.glassmorphism-backdrop {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;

  /* Modern Glassmorphism with strong blur */
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);

  /* Light mode gradient */
  background: linear-gradient(
    to top,
    rgba(255, 255, 255, 0.75),
    rgba(255, 255, 255, 0.45),
    rgba(255, 255, 255, 0.15),
    transparent
  );

  /* Subtle border effect */
  border-top: 1px solid rgba(255, 255, 255, 0.4);

  /* Smooth transition for theme changes */
  transition: all 0.3s ease;
}

/* Dark mode glassmorphism */
:deep(.dark) .glassmorphism-backdrop {
  background: linear-gradient(
    to top,
    rgba(17, 24, 39, 0.85),
    rgba(17, 24, 39, 0.65),
    rgba(17, 24, 39, 0.35),
    transparent
  );
  border-top: 1px solid rgba(75, 85, 99, 0.4);
}

/* Alternative approach for dark mode if :deep doesn't work */
.dark .glassmorphism-backdrop {
  background: linear-gradient(
    to top,
    rgba(17, 24, 39, 0.85),
    rgba(17, 24, 39, 0.65),
    rgba(17, 24, 39, 0.35),
    transparent
  );
  border-top: 1px solid rgba(75, 85, 99, 0.4);
}

/* File Transition */
.file-enter-active {
  animation: file-in 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.file-leave-active {
  animation: file-out 0.2s ease-in;
}

@keyframes file-in {
  from {
    opacity: 0;
    transform: translateY(-10px) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes file-out {
  from {
    opacity: 1;
    transform: scale(1);
  }
  to {
    opacity: 0;
    transform: scale(0.8);
  }
}

/* Fade Transition */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}
</style>
