<template>
  <div :class="[
    'message-input-container relative',
    props.heroMode ? 'p-2 sm:p-4' : 'px-2 sm:px-4 py-3'
  ]">

    <div class="max-w-3xl mx-auto relative">
      <!-- Uploaded Files Display -->
      <TransitionGroup name="file" tag="div" class="mb-2 flex flex-wrap gap-2 max-h-24 overflow-y-auto custom-scrollbar">
        <div
          v-for="(file, index) in uploadedFiles"
          :key="index"
          class="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-gray-700/50 border border-gray-600/50 text-sm group"
        >
          <component :is="getFileIcon(file.type)" class="w-4 h-4 text-fleet-orange-400 flex-shrink-0" />
          <span class="max-w-[120px] truncate text-gray-300">{{ file.name }}</span>
          <button @click="removeFile(index)" class="p-0.5 rounded text-gray-500 hover:text-red-400 transition-colors">
            <XMarkIcon class="w-3.5 h-3.5" />
          </button>
        </div>
      </TransitionGroup>

      <!-- Error/Warning Messages -->
      <Transition name="fade">
        <div v-if="hasImages && !isVisionModel && !settingsStore.getSetting('autoSelectVisionModel')"
             class="mb-2 p-2 rounded-lg bg-yellow-900/30 border border-yellow-700/50 text-yellow-300 text-sm flex items-center gap-2">
          <ExclamationTriangleIcon class="w-4 h-4 flex-shrink-0" />
          <span>Bild hochgeladen - Vision Model empfohlen</span>
        </div>
      </Transition>
      <Transition name="fade">
        <div v-if="errorMessage" class="mb-2 p-2 rounded-lg bg-red-900/30 border border-red-700/50 text-red-300 text-sm flex items-center gap-2">
          <XCircleIcon class="w-4 h-4 flex-shrink-0" />
          <span>{{ errorMessage }}</span>
        </div>
      </Transition>

      <!-- Main Input Tile - Seamless Style (transparent to match chat background) -->
      <div class="input-tile rounded-2xl bg-transparent border border-gray-300/30 dark:border-gray-700/50 overflow-hidden">
        <!-- Textarea Area -->
        <div class="p-5 pb-3">
          <textarea
            v-model="inputText"
            @keydown.enter.exact.prevent="handleSend"
            @keydown.shift.enter="handleNewLine"
            @input="adjustHeight"
            :placeholder="props.heroMode ? 'Wie kann ich dir heute helfen?' : 'Nachricht eingeben...'"
            rows="1"
            class="
              w-full bg-transparent
              text-gray-800 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500
              focus:outline-none
              resize-none
              text-base
            "
            :style="{ minHeight: props.heroMode ? '60px' : '24px', maxHeight: '200px' }"
            :disabled="chatStore.isLoading"
            ref="textareaRef"
          ></textarea>
        </div>

        <!-- Bottom Bar with Icons and Buttons -->
        <div class="px-3 pb-3 flex items-center justify-between">
          <!-- Left Side Icons -->
          <div class="flex items-center gap-1">
            <!-- File Upload -->
            <button
              @click="triggerFileInput"
              class="p-2 rounded-lg text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-200/50 dark:hover:bg-gray-700/50 transition-all"
              :disabled="chatStore.isLoading"
              title="Datei anhängen"
            >
              <PlusIcon class="w-5 h-5" />
            </button>
            <input
              ref="fileInput"
              type="file"
              @change="handleFileSelect"
              accept=".pdf,.txt,.md,.html,.json,.xml,.csv,.png,.jpg,.jpeg,.webp,.bmp,.gif,.tiff,.tif"
              multiple
              class="hidden"
            />

            <!-- Web Search Toggle -->
            <button
              @click="webSearchEnabled = !webSearchEnabled"
              :class="[
                'p-2 rounded-lg transition-all',
                webSearchEnabled
                  ? 'text-blue-400 bg-blue-500/20 hover:bg-blue-500/30'
                  : 'text-gray-400 hover:text-gray-200 hover:bg-gray-700/50'
              ]"
              :disabled="chatStore.isLoading"
              :title="webSearchEnabled ? 'Web-Suche aktiv' : 'Web-Suche aktivieren'"
            >
              <GlobeAltIcon class="w-5 h-5" />
            </button>

            <!-- Settings/Filter (placeholder for future) -->
            <button
              class="p-2 rounded-lg text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-200/50 dark:hover:bg-gray-700/50 transition-all"
              title="Einstellungen"
            >
              <AdjustmentsHorizontalIcon class="w-5 h-5" />
            </button>
          </div>

          <!-- Right Side - Expert/Model Selector & Send -->
          <div class="flex items-center gap-2">
            <!-- Expert/Model Dropdown -->
            <div class="hidden sm:block relative" ref="dropdownContainer">
              <button
                @click="toggleExpertDropdown"
                class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-gray-600 dark:text-gray-300 text-sm hover:bg-gray-200/50 dark:hover:bg-gray-700/50 transition-all"
                :disabled="chatStore.isLoading"
              >
                <span v-if="chatStore.selectedExpertId" class="text-purple-400">🎓</span>
                <CpuChipIcon v-else class="w-4 h-4 text-gray-500" />
                <span class="truncate max-w-[120px]">{{ displayModelName }}</span>
                <ChevronDownIcon class="w-4 h-4" :class="showExpertDropdown ? 'rotate-180' : ''" />
              </button>

              <!-- Dropdown Menu - fixed positioning to avoid clipping -->
              <Transition name="dropdown">
                <div
                  v-if="showExpertDropdown"
                  class="fixed right-[calc(50%-8rem)] bottom-28 w-64 max-h-80 overflow-y-auto bg-gray-800 border border-gray-700 rounded-xl shadow-xl z-[9999]"
                  style="transform: translateX(50%);"
                >
                  <!-- Experten Section -->
                  <div v-if="experts && experts.length > 0" class="p-2">
                    <div class="text-xs text-gray-500 px-2 py-1 font-semibold">🎓 Experten</div>
                    <button
                      v-for="expert in experts"
                      :key="expert.id"
                      @click="selectExpert(expert)"
                      class="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-left hover:bg-gray-700/70 transition-all"
                      :class="chatStore.selectedExpertId === expert.id ? 'bg-purple-500/20 text-purple-300' : 'text-gray-300'"
                    >
                      <img
                        v-if="expert.avatarUrl"
                        :src="expert.avatarUrl"
                        class="w-6 h-6 rounded-md object-cover"
                        alt=""
                      />
                      <div v-else class="w-6 h-6 rounded-md bg-gray-600 flex items-center justify-center text-xs">
                        {{ expert.name?.charAt(0) || '?' }}
                      </div>
                      <div class="flex-1 min-w-0">
                        <div class="text-sm font-medium truncate">{{ expert.name }}</div>
                        <div class="text-xs text-gray-500 truncate">{{ expert.role }}</div>
                      </div>
                      <CheckIcon v-if="chatStore.selectedExpertId === expert.id" class="w-4 h-4 text-purple-400" />
                    </button>
                  </div>

                  <!-- Hinweis wenn keine Experten -->
                  <div v-if="experts.length === 0" class="p-3 text-center text-gray-400 text-sm">
                    Keine Experten vorhanden.<br>
                    <span class="text-xs">Erstellen Sie einen im Experten-Manager.</span>
                  </div>
                </div>
              </Transition>
            </div>

            <!-- Stop Button -->
            <button
              v-if="chatStore.isLoading"
              @click="handleStop"
              class="p-2.5 rounded-xl bg-red-500 hover:bg-red-400 text-white transition-all"
              title="Stoppen"
            >
              <StopIcon class="w-5 h-5" />
            </button>

            <!-- Send Button -->
            <button
              v-else
              @click="handleSend"
              :disabled="!inputText.trim()"
              :class="[
                'send-button p-2.5 rounded-xl transition-all',
                inputText.trim()
                  ? 'bg-fleet-orange-500 hover:bg-fleet-orange-400 text-white'
                  : 'bg-gray-700 text-gray-500 cursor-not-allowed'
              ]"
              title="Senden (Enter)"
            >
              <ArrowUpIcon class="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, inject, computed, onMounted, onUnmounted } from 'vue'
import {
  PlusIcon,
  XMarkIcon,
  StopIcon,
  ExclamationTriangleIcon,
  XCircleIcon,
  DocumentTextIcon,
  DocumentIcon,
  PhotoIcon,
  GlobeAltIcon,
  AdjustmentsHorizontalIcon,
  ChevronDownIcon,
  ArrowUpIcon,
  CheckIcon,
  CpuChipIcon
} from '@heroicons/vue/24/outline'
import { useChatStore } from '../stores/chatStore'
import { useSettingsStore } from '../stores/settingsStore'
import { storeToRefs } from 'pinia'
import { useToast } from '../composables/useToast'
import api from '../services/api'

const { success, error: errorToast } = useToast()
const emit = defineEmits(['send'])
const props = defineProps({
  heroMode: {
    type: Boolean,
    default: false
  }
})
const chatStore = useChatStore()
const settingsStore = useSettingsStore()

// Destructure reactive refs from store
const { experts, models } = storeToRefs(chatStore)

const inputText = ref('')
const textareaRef = ref(null)
const fileInput = ref(null)
const uploadedFiles = ref([])
const isUploading = ref(false)
const uploadProgress = ref('')
const errorMessage = ref('')
const webSearchEnabled = ref(false)

// Expert/Model Dropdown
const showExpertDropdown = ref(false)
const dropdownContainer = ref(null)

// Get showAbortModal from App.vue
const showAbortModal = inject('showAbortModal')

// Available models (use storeToRefs 'models' directly)

// Computed properties
const hasImages = computed(() => uploadedFiles.value.some(f => f.type === 'image'))
const isVisionModel = computed(() => settingsStore.isVisionModel(chatStore.selectedModel))
// Display expert name or model name
const displayModelName = computed(() => {
  // If expert is selected, show expert name (ohne Emoji, das ist jetzt im Template)
  if (chatStore.selectedExpertId) {
    const expert = chatStore.getExpertById(chatStore.selectedExpertId)
    if (expert) {
      const name = expert.name // z.B. "Roland Navarro"
      if (name.length > 15) {
        return name.substring(0, 12) + '...'
      }
      return name
    }
  }

  // Fallback to model name
  const model = chatStore.selectedModel || 'Kein Model'
  if (model.length > 15) {
    return model.substring(0, 12) + '...'
  }
  return model
})

// Get file icon component
function getFileIcon(type) {
  if (type === 'image') return PhotoIcon
  if (type === 'pdf') return DocumentIcon
  return DocumentTextIcon
}

// Get model name from model object or string
function getModelName(model) {
  if (typeof model === 'string') return model
  if (model && model.name) return model.name
  return String(model)
}

// Expert/Model Dropdown Functions
function toggleExpertDropdown() {
  showExpertDropdown.value = !showExpertDropdown.value
}

function selectExpert(expert) {
  chatStore.selectExpert(expert)
  showExpertDropdown.value = false
  // Update current chat's expert if chat exists
  if (chatStore.currentChat) {
    chatStore.currentChat.expertId = expert.id
    chatStore.currentChat.expertName = expert.name
  }
}

function selectModel(model) {
  chatStore.setSelectedModel(model)
  showExpertDropdown.value = false
}

// Close dropdown when clicking outside
function handleClickOutside(event) {
  if (dropdownContainer.value && !dropdownContainer.value.contains(event.target)) {
    showExpertDropdown.value = false
  }
}

// Lifecycle hooks for click-outside handler
onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

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
    const allowedTypes = [
      'application/pdf',
      'text/plain', 'text/markdown', 'text/html', 'text/csv',
      'application/json', 'application/xml', 'text/xml',
      'image/png', 'image/jpeg', 'image/webp', 'image/bmp', 'image/gif', 'image/tiff'
    ]
    const allowedExtensions = [
      '.pdf', '.txt', '.md', '.html', '.htm', '.json', '.xml', '.csv',
      '.png', '.jpg', '.jpeg', '.webp', '.bmp', '.gif', '.tiff', '.tif'
    ]

    const isValidType = allowedTypes.includes(file.type) ||
                       allowedExtensions.some(ext => file.name.toLowerCase().endsWith(ext))

    if (!isValidType) {
      errorMessage.value = `Nicht unterstützt: ${file.name}`
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
      if (response.type === 'scanned-pdf' && response.pageImages && response.pageImages.length > 0) {
        for (let i = 0; i < response.pageImages.length; i++) {
          const pageFile = {
            name: `${response.filename} (S.${i + 1})`,
            type: 'image',
            textContent: null,
            base64Content: response.pageImages[i],
            size: response.pageImages[i].length
          }
          uploadedFiles.value.push(pageFile)
        }
        success(`${file.name}: ${response.pageImages.length} Seiten`)
      } else {
        const uploadedFile = {
          name: response.filename,
          type: response.type,
          textContent: response.textContent || null,
          base64Content: response.base64Content || null,
          size: response.size
        }
        uploadedFiles.value.push(uploadedFile)
        success(`${file.name} hochgeladen`)
      }
    } else {
      errorMessage.value = response.error || 'Upload fehlgeschlagen'
      errorToast('Upload fehlgeschlagen')
    }
  } catch (error) {
    console.error('Upload error:', error)
    errorMessage.value = `Fehler: ${error.message}`
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
    files: uploadedFiles.value,
    webSearchEnabled: webSearchEnabled.value
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
  console.log('Stop button clicked')
  const result = await chatStore.abortCurrentRequest()

  if (result) {
    showAbortModal.value = true
    setTimeout(() => {
      showAbortModal.value = false
    }, 3000)
  }
}

function adjustHeight() {
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
    const newHeight = Math.min(textareaRef.value.scrollHeight, 200)
    textareaRef.value.style.height = newHeight + 'px'
  }
}
</script>

<style scoped>
.input-tile {
  /* Kein Shadow - seamless mit Chat-Hintergrund */
}

/* File Transition */
.file-enter-active {
  animation: file-in 0.2s ease-out;
}
.file-leave-active {
  animation: file-out 0.15s ease-in;
}

@keyframes file-in {
  from { opacity: 0; transform: scale(0.9); }
  to { opacity: 1; transform: scale(1); }
}

@keyframes file-out {
  from { opacity: 1; transform: scale(1); }
  to { opacity: 0; transform: scale(0.9); }
}

/* Fade Transition */
.fade-enter-active, .fade-leave-active {
  transition: all 0.2s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

/* Custom Scrollbar */
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
  height: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.3);
  border-radius: 2px;
}

/* Textarea no scrollbar */
textarea::-webkit-scrollbar {
  display: none;
}
textarea {
  scrollbar-width: none;
}

/* Dropdown Transition */
.dropdown-enter-active {
  animation: dropdown-in 0.15s ease-out;
}
.dropdown-leave-active {
  animation: dropdown-out 0.1s ease-in;
}

@keyframes dropdown-in {
  from { opacity: 0; transform: translateY(8px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes dropdown-out {
  from { opacity: 1; transform: translateY(0) scale(1); }
  to { opacity: 0; transform: translateY(8px) scale(0.95); }
}
</style>
