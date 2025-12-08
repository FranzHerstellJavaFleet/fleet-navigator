<template>
  <div class="file-upload-container">
    <!-- File Input Trigger -->
    <div
      class="flex items-center gap-2 p-2 bg-gray-100 dark:bg-gray-700 rounded-lg"
      @dragover.prevent="dragOver = true"
      @dragleave.prevent="dragOver = false"
      @drop.prevent="handleDrop"
      :class="{ 'border-2 border-fleet-orange-500': dragOver }"
    >
      <button
        @click="triggerFileInput"
        class="p-2 text-gray-600 dark:text-gray-300 hover:text-fleet-orange-500 dark:hover:text-fleet-orange-400 transition-colors"
        title="Datei anhängen"
      >
        📎
      </button>
      <input
        ref="fileInput"
        type="file"
        @change="handleFileSelect"
        accept=".pdf,.txt,.md,.png,.jpg,.jpeg"
        multiple
        class="hidden"
      />

      <!-- Uploaded Files Display -->
      <div v-if="uploadedFiles.length > 0" class="flex-1 flex flex-wrap gap-2">
        <div
          v-for="(file, index) in uploadedFiles"
          :key="index"
          class="flex items-center gap-2 px-3 py-1 bg-white dark:bg-gray-600 rounded text-sm"
        >
          <span>{{ file.icon }}</span>
          <span class="max-w-[150px] truncate">{{ file.name }}</span>
          <button
            @click="removeFile(index)"
            class="text-red-500 hover:text-red-700"
          >
            ✕
          </button>
        </div>
      </div>

      <div v-else class="text-sm text-gray-500 dark:text-gray-400">
        Datei anhängen oder hier ablegen
      </div>
    </div>

    <!-- Upload Progress -->
    <div v-if="isUploading" class="mt-2 text-sm text-gray-600 dark:text-gray-400">
      Lade hoch... {{ uploadProgress }}
    </div>

    <!-- Error Message -->
    <div v-if="errorMessage" class="mt-2 text-sm text-red-500">
      {{ errorMessage }}
    </div>

    <!-- Vision Model Warning -->
    <div v-if="hasImages && !isVisionModel" class="mt-2 p-2 bg-yellow-50 dark:bg-yellow-900 border border-yellow-300 dark:border-yellow-700 rounded text-sm text-yellow-800 dark:text-yellow-200">
      ⚠️ Hinweis: Bild hochgeladen, aber kein Vision Model ausgewählt!
      <span v-if="settingsStore.getSetting('autoSelectVisionModel')" class="block text-xs mt-1">
        Beim Senden wird automatisch zu {{ settingsStore.getSetting('preferredVisionModel') }} gewechselt.
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import api from '../services/api'
import { useChatStore } from '../stores/chatStore'
import { useSettingsStore } from '../stores/settingsStore'

const emit = defineEmits(['files-uploaded', 'files-changed'])

const chatStore = useChatStore()
const settingsStore = useSettingsStore()

const fileInput = ref(null)
const uploadedFiles = ref([])
const isUploading = ref(false)
const uploadProgress = ref('')
const errorMessage = ref('')
const dragOver = ref(false)

// Computed properties
const hasImages = computed(() => uploadedFiles.value.some(f => f.type === 'image'))
const isVisionModel = computed(() => settingsStore.isVisionModel(chatStore.selectedModel))

function triggerFileInput() {
  fileInput.value?.click()
}

async function handleFileSelect(event) {
  const files = Array.from(event.target.files)
  await processFiles(files)
  // Reset input so same file can be selected again
  event.target.value = ''
}

async function handleDrop(event) {
  dragOver.value = false
  const files = Array.from(event.dataTransfer.files)
  await processFiles(files)
}

async function processFiles(files) {
  errorMessage.value = ''

  for (const file of files) {
    // Validate file type
    const allowedTypes = ['application/pdf', 'text/plain', 'text/markdown', 'image/png', 'image/jpeg']
    const allowedExtensions = ['.pdf', '.txt', '.md', '.png', '.jpg', '.jpeg']

    const isValidType = allowedTypes.includes(file.type) ||
                       allowedExtensions.some(ext => file.name.toLowerCase().endsWith(ext))

    if (!isValidType) {
      errorMessage.value = `Nicht unterstützt: ${file.name}. Erlaubt: PDF, TXT, MD, PNG, JPG`
      continue
    }

    // Validate file size (50MB max)
    if (file.size > 50 * 1024 * 1024) {
      errorMessage.value = `Datei zu groß: ${file.name} (max. 50MB)`
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
      // Determine icon based on file type
      let icon = '📄'
      if (response.type === 'pdf') icon = '📄'
      else if (response.type === 'image') icon = '🖼️'
      else if (response.type === 'text') icon = '📝'

      const uploadedFile = {
        name: response.filename,
        type: response.type,
        icon: icon,
        textContent: response.textContent || null,
        base64Content: response.base64Content || null,
        size: response.size
      }

      uploadedFiles.value.push(uploadedFile)
      emit('files-uploaded', uploadedFiles.value)
      emit('files-changed', uploadedFiles.value)
    } else {
      errorMessage.value = response.error || 'Upload fehlgeschlagen'
    }
  } catch (error) {
    console.error('Upload error:', error)
    errorMessage.value = `Fehler beim Hochladen: ${error.message}`
  } finally {
    isUploading.value = false
    uploadProgress.value = ''
  }
}

function removeFile(index) {
  uploadedFiles.value.splice(index, 1)
  emit('files-changed', uploadedFiles.value)
}

// Expose method to clear files
function clearFiles() {
  uploadedFiles.value = []
  errorMessage.value = ''
}

defineExpose({
  clearFiles,
  uploadedFiles
})
</script>

<style scoped>
.file-upload-container {
  width: 100%;
}
</style>
