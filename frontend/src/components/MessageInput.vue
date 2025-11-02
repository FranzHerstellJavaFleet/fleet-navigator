<template>
  <div class="border-t border-gray-200/50 dark:border-gray-700/50 bg-white/90 dark:bg-gray-800/90 backdrop-blur-sm p-4 flex-shrink-0">
    <div class="max-w-4xl mx-auto">
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
      <div class="flex items-end gap-2">
        <!-- File Upload Button -->
        <button
          @click="triggerFileInput"
          class="
            p-3 rounded-xl
            text-gray-600 dark:text-gray-300
            hover:text-fleet-orange-500 dark:hover:text-fleet-orange-400
            hover:bg-gray-100 dark:hover:bg-gray-700
            transition-all duration-200
            transform hover:scale-110 active:scale-95
            disabled:opacity-50 disabled:cursor-not-allowed
          "
          :disabled="chatStore.isLoading"
          title="Datei anhängen (PDF, TXT, MD, PNG, JPG)"
        >
          <PaperClipIcon class="w-6 h-6" />
        </button>
        <input
          ref="fileInput"
          type="file"
          @change="handleFileSelect"
          accept=".pdf,.txt,.md,.png,.jpg,.jpeg"
          multiple
          class="hidden"
        />

        <!-- Letter Template Button -->
        <div class="relative">
          <button
            @click="showTemplateMenu = !showTemplateMenu"
            class="
              p-3 rounded-xl
              text-gray-600 dark:text-gray-300
              hover:text-blue-500 dark:hover:text-blue-400
              hover:bg-gray-100 dark:hover:bg-gray-700
              transition-all duration-200
              transform hover:scale-110 active:scale-95
              disabled:opacity-50 disabled:cursor-not-allowed
            "
            :disabled="chatStore.isLoading"
            title="Briefvorlage auswählen"
          >
            <DocumentTextIcon class="w-6 h-6" />
          </button>

          <!-- Template Dropdown Menu -->
          <Transition name="fade">
            <div
              v-if="showTemplateMenu"
              class="
                absolute bottom-full left-0 mb-2
                w-80 max-h-96 overflow-y-auto
                bg-white dark:bg-gray-800
                border border-gray-200 dark:border-gray-700
                rounded-xl shadow-2xl
                z-50
              "
              @click.stop
            >
              <div class="p-3 border-b border-gray-200 dark:border-gray-700">
                <h3 class="font-semibold text-gray-900 dark:text-white">Briefvorlagen</h3>
              </div>

              <div v-if="letterTemplates.length > 0" class="p-2">
                <button
                  v-for="template in letterTemplates"
                  :key="template.id"
                  @click="selectTemplate(template)"
                  class="
                    w-full text-left p-3 rounded-lg
                    hover:bg-gray-100 dark:hover:bg-gray-700
                    transition-colors
                    group
                  "
                >
                  <div class="font-medium text-gray-900 dark:text-white group-hover:text-blue-600 dark:group-hover:text-blue-400">
                    {{ template.name }}
                  </div>
                  <div v-if="template.description" class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    {{ template.description }}
                  </div>
                  <div v-if="template.category" class="mt-1">
                    <span class="px-2 py-0.5 rounded text-xs bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300">
                      {{ template.category }}
                    </span>
                  </div>
                </button>
              </div>

              <div v-else class="p-8 text-center text-gray-500 dark:text-gray-400">
                <p class="text-sm">Keine Vorlagen verfügbar</p>
                <p class="text-xs mt-1">Erstelle Vorlagen in den Einstellungen</p>
              </div>
            </div>
          </Transition>
        </div>

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
              w-full px-4 py-3 pr-12 rounded-xl
              border border-gray-300 dark:border-gray-600
              bg-white dark:bg-gray-700
              text-gray-900 dark:text-gray-100
              placeholder-gray-400 dark:placeholder-gray-500
              focus:outline-none focus:ring-2 focus:ring-fleet-orange-500 focus:border-transparent
              resize-none
              transition-all duration-200
              disabled:opacity-50 disabled:cursor-not-allowed
            "
            :disabled="chatStore.isLoading"
            ref="textareaRef"
          ></textarea>

          <!-- Character Count Badge -->
          <div class="absolute bottom-2 right-2 text-xs text-gray-400 dark:text-gray-500 bg-white/80 dark:bg-gray-700/80 backdrop-blur-sm px-2 py-1 rounded-lg">
            {{ inputText.length }}
          </div>
        </div>

        <!-- Stop Button (when loading) -->
        <button
          v-if="chatStore.isLoading"
          @click="handleStop"
          class="
            px-6 py-3 rounded-xl
            bg-gradient-to-r from-red-500 to-red-600
            hover:from-red-400 hover:to-red-500
            text-white font-semibold
            shadow-lg hover:shadow-xl
            transition-all duration-200
            transform hover:scale-105 active:scale-95
            flex items-center gap-2
          "
          title="Generierung stoppen"
        >
          <StopIcon class="w-5 h-5" />
          <span>Stop</span>
        </button>

        <!-- Send Button -->
        <button
          v-else
          @click="handleSend"
          :disabled="!inputText.trim() || chatStore.isLoading"
          class="
            px-6 py-3 rounded-xl
            bg-gradient-to-r from-fleet-orange-500 to-orange-600
            hover:from-fleet-orange-400 hover:to-orange-500
            text-white font-semibold
            shadow-lg hover:shadow-xl
            disabled:opacity-50 disabled:cursor-not-allowed
            transition-all duration-200
            transform hover:scale-105 active:scale-95
            flex items-center gap-2
          "
          title="Nachricht senden (Enter)"
        >
          <PaperAirplaneIcon class="w-5 h-5" />
          <span class="hidden sm:inline">Senden</span>
        </button>
      </div>

      <!-- Status Bar -->
      <div class="mt-2 flex items-center justify-between text-xs">
        <div class="flex items-center gap-4">
          <!-- Token Counter -->
          <div class="flex items-center gap-1.5 text-gray-500 dark:text-gray-400">
            <CpuChipIcon class="w-4 h-4" />
            <span>Chat Tokens:</span>
            <span class="text-fleet-orange-500 font-semibold">{{ chatStore.currentChatTokens }}</span>
          </div>

          <!-- Streaming Status -->
          <div class="flex items-center gap-1.5 text-gray-500 dark:text-gray-400">
            <component :is="chatStore.streamingEnabled ? BoltIcon : DocumentTextIcon" class="w-4 h-4" />
            <span>{{ chatStore.streamingEnabled ? 'Streaming' : 'Normal' }}</span>
          </div>

          <!-- Upload Progress -->
          <div v-if="isUploading" class="flex items-center gap-1.5 text-fleet-orange-500 animate-pulse">
            <ArrowUpTrayIcon class="w-4 h-4" />
            <span>{{ uploadProgress }}</span>
          </div>
        </div>
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

// Letter Templates
const letterTemplates = ref([])
const showTemplateMenu = ref(false)

// Get showAbortModal from App.vue
const showAbortModal = inject('showAbortModal')

// Computed properties
const hasImages = computed(() => uploadedFiles.value.some(f => f.type === 'image'))
const isVisionModel = computed(() => settingsStore.isVisionModel(chatStore.selectedModel))

// Load letter templates on mount
onMounted(async () => {
  try {
    letterTemplates.value = await api.getLetterTemplates()
  } catch (err) {
    console.error('Error loading letter templates:', err)
  }
})

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

// Letter Template Functions
function selectTemplate(template) {
  inputText.value = template.prompt
  showTemplateMenu.value = false

  // Focus textarea and adjust height
  if (textareaRef.value) {
    textareaRef.value.focus()
    adjustHeight()
  }

  success(`Vorlage "${template.name}" eingefügt`)
}
</script>

<style scoped>
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
