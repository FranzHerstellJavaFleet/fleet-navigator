<template>
  <div v-if="downloadUrl" class="mt-4 p-4 bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 rounded-xl border border-blue-200 dark:border-blue-700/50">
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="p-2 rounded-lg bg-blue-500/20">
          <DocumentArrowDownIcon class="w-6 h-6 text-blue-600 dark:text-blue-400" />
        </div>
        <div>
          <h4 class="font-semibold text-gray-900 dark:text-white">
            {{ t('download.title') }}
          </h4>
          <p class="text-sm text-gray-600 dark:text-gray-400">
            {{ t('download.description') }}
          </p>
        </div>
      </div>

      <button
        @click="handleDownload"
        :disabled="downloading"
        class="
          px-6 py-3 rounded-lg
          bg-gradient-to-r from-blue-500 to-indigo-600
          hover:from-blue-600 hover:to-indigo-700
          text-white font-medium
          shadow-lg hover:shadow-xl
          transition-all duration-200
          transform hover:scale-105 active:scale-95
          disabled:opacity-50 disabled:cursor-not-allowed
          flex items-center gap-2
        "
      >
        <ArrowDownTrayIcon v-if="!downloading" class="w-5 h-5" />
        <ArrowPathIcon v-else class="w-5 h-5 animate-spin" />
        <span>{{ downloading ? t('download.downloading') : t('download.button') }}</span>
      </button>
    </div>

    <!-- Success Message -->
    <div v-if="downloaded" class="mt-3 flex items-center gap-2 text-sm text-green-600 dark:text-green-400">
      <CheckCircleIcon class="w-4 h-4" />
      <span>{{ t('download.success') }}</span>
    </div>

    <!-- Error Message -->
    <div v-if="error" class="mt-3 flex items-center gap-2 text-sm text-red-600 dark:text-red-400">
      <XCircleIcon class="w-4 h-4" />
      <span>{{ error }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import {
  DocumentArrowDownIcon,
  ArrowDownTrayIcon,
  ArrowPathIcon,
  CheckCircleIcon,
  XCircleIcon
} from '@heroicons/vue/24/outline'
import { useLocale } from '../composables/useLocale'
import axios from 'axios'

const props = defineProps({
  downloadUrl: {
    type: String,
    required: false,
    default: null
  }
})

const { t } = useLocale()
const downloading = ref(false)
const downloaded = ref(false)
const error = ref(null)

async function handleDownload() {
  if (!props.downloadUrl) return

  downloading.value = true
  error.value = null

  try {
    // Fetch the file
    const response = await axios.get(props.downloadUrl, {
      responseType: 'blob'
    })

    // Use content-type from response (for ODT, DOCX, PDF, ZIP, etc.)
    const contentType = response.headers['content-type'] || 'application/octet-stream'
    const blob = new Blob([response.data], { type: contentType })
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)

    // Extract filename from Content-Disposition header or determine from URL/content-type
    const contentDisposition = response.headers['content-disposition']
    let filename = 'dokument'

    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename="?([^"]+)"?/)
      if (filenameMatch) {
        filename = filenameMatch[1]
      }
    } else {
      // Fallback: determine extension from content-type
      if (contentType.includes('opendocument.text')) {
        filename = 'dokument.odt'
      } else if (contentType.includes('wordprocessingml')) {
        filename = 'dokument.docx'
      } else if (contentType.includes('pdf')) {
        filename = 'dokument.pdf'
      } else if (contentType.includes('zip')) {
        filename = 'projekt.zip'
      }
    }

    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(link.href)

    downloaded.value = true
    setTimeout(() => {
      downloaded.value = false
    }, 3000)

  } catch (err) {
    console.error('Download failed:', err)
    error.value = t('download.error')
  } finally {
    downloading.value = false
  }
}
</script>

<style scoped>
/* Add any additional styles here */
</style>
