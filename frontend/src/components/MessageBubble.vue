<template>
  <div
    class="flex animate-fade-in"
    :class="isUser ? 'justify-end' : 'justify-start'"
  >
    <div
      class="
        max-w-3xl rounded-2xl relative group
        transition-all duration-300
        hover:shadow-lg
      "
      :class="messageClasses"
    >
      <!-- Message Header -->
      <div class="flex items-center justify-between mb-2">
        <div class="flex items-center gap-2">
          <!-- Avatar Icon -->
          <div
            class="flex-shrink-0 p-1.5 rounded-lg"
            :class="isUser ? 'bg-fleet-orange-500/20' : 'bg-blue-500/20'"
          >
            <UserCircleIcon v-if="isUser" class="w-5 h-5 text-fleet-orange-600 dark:text-fleet-orange-400" />
            <CpuChipIcon v-else class="w-5 h-5 text-blue-600 dark:text-blue-400" />
          </div>

          <span class="text-sm font-semibold" :class="isUser ? 'text-fleet-orange-700 dark:text-fleet-orange-400' : 'text-blue-700 dark:text-blue-400'">
            {{ isUser ? 'Du' : 'AI Assistant' }}
          </span>

          <!-- Metadata -->
          <div class="flex items-center gap-2 text-xs opacity-60">
            <div class="flex items-center gap-1">
              <ClockIcon class="w-3 h-3" />
              <span>{{ formatTime(message.createdAt) }}</span>
            </div>
            <span v-if="message.tokens" class="flex items-center gap-1">
              •
              <CpuChipIcon class="w-3 h-3" />
              {{ message.tokens }} tokens
            </span>
            <!-- Expert/Model Name (only for AI messages) -->
            <span v-if="!isUser" class="flex items-center gap-1">
              •
              <component :is="displayExpertName ? 'span' : CpuChipIcon" :class="displayExpertName ? 'text-purple-400' : 'w-3 h-3'">
                {{ displayExpertName ? '🎓' : '' }}
              </component>
              {{ displayExpertName || message.modelName || chatStore.selectedModel || 'Unbekannt' }}
            </span>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
          <!-- Copy Button (only for AI messages) -->
          <button
            v-if="!isUser"
            @click="copyToClipboard"
            class="
              p-1.5 rounded-lg
              hover:bg-gray-100 dark:hover:bg-gray-700
              transition-all duration-200
              transform hover:scale-110
            "
            :title="copied ? 'Kopiert!' : 'Text kopieren'"
          >
            <CheckIcon v-if="copied" class="w-4 h-4 text-green-500" />
            <ClipboardDocumentIcon v-else class="w-4 h-4 text-gray-600 dark:text-gray-400" />
          </button>

          <!-- Delete Button -->
          <button
            @click="deleteMessage"
            class="
              p-1.5 rounded-lg
              hover:bg-red-100 dark:hover:bg-red-900/30
              transition-all duration-200
              transform hover:scale-110
            "
            title="Nachricht löschen"
          >
            <TrashIcon class="w-4 h-4 text-gray-400 hover:text-red-500 dark:text-gray-500 dark:hover:text-red-400" />
          </button>
        </div>
      </div>

      <!-- Attachments (for user messages with files) -->
      <div v-if="parsedAttachments.length > 0" class="mb-3 flex flex-wrap gap-2">
        <div
          v-for="(attachment, index) in parsedAttachments"
          :key="index"
          class="
            flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs
            bg-gray-100/80 dark:bg-gray-700/80
            border border-gray-200/50 dark:border-gray-600/50
            text-gray-700 dark:text-gray-300
          "
        >
          <component :is="getAttachmentIcon(attachment.type)" class="w-3.5 h-3.5 text-fleet-orange-500" />
          <span class="max-w-[120px] truncate">{{ attachment.name }}</span>
          <span class="text-gray-400 dark:text-gray-500">{{ formatFileSize(attachment.size) }}</span>
        </div>
      </div>

      <!-- Message Content -->
      <div
        ref="contentRef"
        class="prose dark:prose-invert max-w-none message-content"
        v-html="displayContent"
      ></div>

      <!-- Streaming Indicator -->
      <div v-if="message.isStreaming" class="mt-3 flex items-center gap-2">
        <div class="flex gap-1">
          <div class="w-2 h-2 bg-fleet-orange-500 rounded-full animate-bounce"></div>
          <div class="w-2 h-2 bg-fleet-orange-500 rounded-full animate-bounce" style="animation-delay: 0.2s"></div>
          <div class="w-2 h-2 bg-fleet-orange-500 rounded-full animate-bounce" style="animation-delay: 0.4s"></div>
        </div>
        <span class="text-xs text-fleet-orange-500 dark:text-fleet-orange-400 font-medium">Streaming...</span>
      </div>

      <!-- Download Button (only for AI messages with downloadUrl, NOT for Fleet-Mate local documents) -->
      <DownloadButton v-if="!isUser && message.downloadUrl && !isFleetMateDocument" :downloadUrl="message.downloadUrl" />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUpdated, nextTick } from 'vue'
import {
  UserCircleIcon,
  CpuChipIcon,
  ClockIcon,
  ClipboardDocumentIcon,
  CheckIcon,
  DocumentIcon,
  PhotoIcon,
  DocumentTextIcon,
  CodeBracketIcon,
  TableCellsIcon,
  TrashIcon
} from '@heroicons/vue/24/outline'
import { useSettingsStore } from '../stores/settingsStore'
import { useChatStore } from '../stores/chatStore'
import { useToast } from '../composables/useToast'
import { useConfirmDialog } from '../composables/useConfirmDialog'
import { formatFileSize } from '../composables/useFormatters'
import { marked } from 'marked'
import hljs from 'highlight.js'
import DownloadButton from './DownloadButton.vue'

const { success } = useToast()
const { confirmDelete } = useConfirmDialog()

const props = defineProps({
  message: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['delete'])

const settingsStore = useSettingsStore()
const chatStore = useChatStore()
const isUser = computed(() => props.message.role === 'USER')
const copied = ref(false)
const contentRef = ref(null)

// Get expert name for display (full name like "Roland Navarro")
// Priority: 1. Chat's expertId (persisted), 2. Currently selected expert
const displayExpertName = computed(() => {
  // First check if this chat has a saved expert
  if (chatStore.currentChat?.expertId) {
    const expert = chatStore.getExpertById(chatStore.currentChat.expertId)
    if (expert) {
      return expert.name // z.B. "Roland Navarro"
    }
  }
  // Fallback to currently selected expert
  if (chatStore.selectedExpertId) {
    const expert = chatStore.getExpertById(chatStore.selectedExpertId)
    if (expert) {
      return expert.name
    }
  }
  return null // Fallback to model name
})

// Parse attachments from JSON string
const parsedAttachments = computed(() => {
  if (!props.message.attachments) return []
  try {
    return JSON.parse(props.message.attachments)
  } catch (e) {
    console.warn('Failed to parse attachments:', e)
    return []
  }
})

// Get icon component for attachment type
function getAttachmentIcon(type) {
  switch (type) {
    case 'image': return PhotoIcon
    case 'pdf': return DocumentIcon
    case 'json':
    case 'xml':
    case 'html': return CodeBracketIcon
    case 'csv': return TableCellsIcon
    default: return DocumentTextIcon
  }
}

// formatFileSize importiert aus useFormatters.js

// Check if this is a Fleet-Mate document (saved locally)
const isFleetMateDocument = computed(() => {
  return props.message.downloadUrl && props.message.downloadUrl.startsWith('fleet-mate://')
})

const displayContent = computed(() => {
  const content = props.message.content
  const streaming = props.message.isStreaming

  // For user messages, escape HTML
  if (isUser.value) {
    return escapeHtml(content)
  }

  // For Fleet-Mate documents: show a short summary instead of full content
  // This takes priority because we have downloadUrl
  if (isFleetMateDocument.value) {
    // Extract full path from fleet-mate:// URL
    // Format: fleet-mate:///home/trainer/Dokumente/Fleet-Navigator/Roland/Mietvertrag_2025-11-29_13-22.odt
    const fullPath = props.message.downloadUrl.replace('fleet-mate://', '')

    // Check if we have a real path (starts with /) or just a session ID
    const isRealPath = fullPath.startsWith('/')

    // Extract filename and directory
    const lastSlash = fullPath.lastIndexOf('/')
    const filename = lastSlash > 0 ? fullPath.substring(lastSlash + 1) : fullPath
    const directory = lastSlash > 0 ? fullPath.substring(0, lastSlash) : ''

    // Detect document type from file extension
    const lowerPath = fullPath.toLowerCase()
    let docTypeLabel = 'Dokument'
    let docTypeIcon = '📄'
    if (lowerPath.endsWith('.pdf')) {
      docTypeLabel = 'PDF Dokument'
      docTypeIcon = '📕'
    } else if (lowerPath.endsWith('.odt')) {
      docTypeLabel = 'ODT Dokument'
      docTypeIcon = '📄'
    } else if (lowerPath.endsWith('.docx')) {
      docTypeLabel = 'Word Dokument'
      docTypeIcon = '📘'
    }

    // Show full path info with filename and location
    const pathInfo = isRealPath
      ? `<div class="text-xs mt-2 space-y-1 text-gray-600 dark:text-gray-400">
           <div><strong>Datei:</strong> ${filename}</div>
           <div><strong>Speicherort:</strong> ${directory}</div>
         </div>`
      : `<div class="text-xs text-gray-500 dark:text-gray-400 mt-1">⏳ Pfad wird geladen...</div>`

    return `<div class="flex items-center gap-3 p-4 bg-green-50 dark:bg-green-900/20 rounded-xl border border-green-200 dark:border-green-700/50">
      <div class="flex-shrink-0 p-2 bg-green-500/20 rounded-lg">
        <svg class="w-6 h-6 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
        </svg>
      </div>
      <div>
        <div class="font-semibold text-green-700 dark:text-green-300">${docTypeIcon} ${docTypeLabel} erstellt</div>
        <div class="text-sm text-green-600 dark:text-green-400">Das Dokument wurde lokal gespeichert.</div>
        ${pathInfo}
      </div>
    </div>`
  }

  // For document requests during streaming: show "creating" card immediately
  // This prevents the ugly flash of document text before card appears
  if (props.message.isDocumentRequest && streaming) {
    // Show document type if available (PDF, ODT, DOCX)
    const docType = props.message.documentType
    const typeLabel = docType === 'PDF' ? 'PDF' : docType === 'DOCX' ? 'Word (DOCX)' : docType === 'ODT' ? 'LibreOffice (ODT)' : 'Dokument'
    const typeIcon = docType === 'PDF' ? '📕' : docType === 'DOCX' ? '📘' : '📄'

    return `<div class="flex items-center gap-3 p-4 bg-amber-50 dark:bg-amber-900/20 rounded-xl border border-amber-200 dark:border-amber-700/50">
      <div class="flex-shrink-0 p-2 bg-amber-500/20 rounded-lg">
        <svg class="w-6 h-6 text-amber-600 dark:text-amber-400 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
        </svg>
      </div>
      <div>
        <div class="font-semibold text-amber-700 dark:text-amber-300">${typeIcon} Erstelle ${typeLabel}...</div>
        <div class="text-sm text-amber-600 dark:text-amber-400">Das Dokument wird generiert und lokal gespeichert.</div>
      </div>
    </div>`
  }

  // During streaming (non-document): show raw text with line breaks
  if (streaming) {
    return '<div class="whitespace-pre-wrap">' + escapeHtml(content) + '</div>'
  }

  // After streaming: parse markdown with syntax highlighting
  try {
    const parsed = marked.parse(content, { async: false })
    return parsed
  } catch (error) {
    console.error('Markdown parsing error:', error)
    return content.replace(/\n/g, '<br>')
  }
})

// Apply syntax highlighting and add copy buttons
const applyHighlighting = () => {
  nextTick(() => {
    if (contentRef.value && !props.message.isStreaming) {
      const codeBlocks = contentRef.value.querySelectorAll('pre code')

      if (codeBlocks.length > 0) {
        codeBlocks.forEach((block) => {
          // Apply syntax highlighting
          hljs.highlightElement(block)

          // Add copy button if not already present
          const pre = block.parentElement
          if (pre && !pre.querySelector('.code-copy-btn')) {
            const copyBtn = document.createElement('button')
            copyBtn.className = 'code-copy-btn'
            copyBtn.innerHTML = `
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"></path>
              </svg>
            `
            copyBtn.title = 'Code kopieren'

            copyBtn.addEventListener('click', async () => {
              try {
                await navigator.clipboard.writeText(block.textContent)
                copyBtn.innerHTML = `
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
                  </svg>
                `
                success('Code kopiert')
                setTimeout(() => {
                  copyBtn.innerHTML = `
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"></path>
                    </svg>
                  `
                }, 2000)
              } catch (err) {
                console.error('Failed to copy code:', err)
              }
            })

            pre.style.position = 'relative'
            pre.appendChild(copyBtn)
          }
        })
      }
    }
  })
}

onMounted(() => {
  applyHighlighting()
})

onUpdated(() => {
  applyHighlighting()
})

// Configure marked to use highlight.js
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (err) {
        console.error('Highlight error:', err)
      }
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

// Custom renderer für Links (öffnen in neuem Tab)
// WICHTIG: marked v5+ verwendet Objekt-Parameter statt einzelner Argumente!
const renderer = new marked.Renderer()
renderer.link = function({ href, title, text }) {
  const titleAttr = title ? ` title="${title}"` : ''
  return `<a href="${href}"${titleAttr} target="_blank" rel="noopener noreferrer" class="text-blue-600 dark:text-blue-400 hover:underline">${text}</a>`
}
marked.use({ renderer })

const messageClasses = computed(() => {
  if (isUser.value) {
    return `
      message-user
      px-5 py-4
      bg-gradient-to-br from-fleet-orange-50 to-orange-50
      dark:from-fleet-orange-900/30 dark:to-orange-900/30
      border-2 border-fleet-orange-400/50 dark:border-fleet-orange-500/50
      text-gray-800 dark:text-gray-100
    `
  } else {
    return `
      message-assistant
      px-5 py-4
      bg-white/90 dark:bg-gray-800/90
      backdrop-blur-sm
      border border-gray-200/50 dark:border-gray-700/50
      text-gray-800 dark:text-gray-100
      shadow-sm
    `
  }
})


function escapeHtml(text) {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

function formatTime(dateString) {
  const date = new Date(dateString)
  return date.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' })
}

async function copyToClipboard() {
  try {
    await navigator.clipboard.writeText(props.message.content)
    copied.value = true
    success('Text kopiert')
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch (err) {
    console.error('Failed to copy text:', err)
  }
}

async function deleteMessage() {
  const confirmed = await confirmDelete('Nachricht')
  if (confirmed) {
    emit('delete', props.message.id)
  }
}
</script>

<style scoped>
@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in {
  animation: fade-in 0.3s ease-out;
}

/* Message Content - verhindert Overflow */
:deep(.message-content) {
  overflow-wrap: break-word;
  word-wrap: break-word;
  word-break: break-word;
}

/* Prose Styling for Dark Mode */
:deep(.prose) {
  @apply text-gray-800 dark:text-gray-200;
}

:deep(.prose h1),
:deep(.prose h2),
:deep(.prose h3) {
  @apply text-gray-900 dark:text-gray-100;
}

:deep(.prose code) {
  @apply bg-gray-200 dark:bg-gray-700;
}

:deep(.prose pre) {
  @apply bg-gray-900 dark:bg-black;
}

/* Highlight.js Code Blocks */
:deep(.message-content pre) {
  @apply rounded-lg my-4 overflow-x-auto;
}

:deep(.message-content pre code) {
  @apply block p-4;
  font-family: 'Courier New', Courier, monospace;
  font-size: 0.9em;
  line-height: 1.5;
}

/* Override Highlight.js theme for dark mode consistency */
:deep(.message-content pre code.hljs) {
  background: #1e1e1e !important;
  color: #d4d4d4 !important;
  padding: 1rem !important;
}

:deep(.prose a) {
  @apply text-blue-600 dark:text-blue-400;
}

:deep(.prose strong) {
  @apply text-gray-900 dark:text-gray-100;
}

/* HTML Content Styling */
:deep(.message-content p) {
  @apply mb-3 last:mb-0;
}

:deep(.message-content br) {
  @apply block my-1;
}

/* Headings */
:deep(.message-content h1) {
  @apply text-2xl font-bold mt-4 mb-3 text-gray-900 dark:text-gray-100;
}

:deep(.message-content h2) {
  @apply text-xl font-bold mt-3 mb-2 text-gray-900 dark:text-gray-100;
}

:deep(.message-content h3) {
  @apply text-lg font-bold mt-3 mb-2 text-gray-900 dark:text-gray-100;
}

/* Lists */
:deep(.message-content ul) {
  @apply list-disc list-inside mb-3 ml-4 space-y-1;
}

:deep(.message-content ol) {
  @apply list-decimal list-inside mb-3 ml-4 space-y-1;
}

:deep(.message-content li) {
  @apply text-gray-800 dark:text-gray-200;
}

/* Blockquotes */
:deep(.message-content blockquote) {
  @apply border-l-4 border-blue-500 pl-4 py-2 my-3 bg-blue-50 dark:bg-blue-900/20 italic text-gray-700 dark:text-gray-300;
}

/* Horizontal Rule */
:deep(.message-content hr) {
  @apply my-4 border-gray-300 dark:border-gray-600;
}

/* Links - mit Word-Break für lange URLs */
:deep(.message-content a) {
  @apply text-blue-600 dark:text-blue-400 hover:underline;
  word-break: break-all;
  overflow-wrap: break-word;
}

/* Inline formatting */
:deep(.message-content strong) {
  @apply font-bold text-gray-900 dark:text-gray-100;
}

:deep(.message-content em) {
  @apply italic;
}

:deep(.message-content del) {
  @apply line-through text-gray-500 dark:text-gray-400;
}

/* Copy Button for Code Blocks */
:deep(.code-copy-btn) {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  padding: 0.5rem;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 0.375rem;
  color: #d4d4d4;
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(pre:hover .code-copy-btn) {
  opacity: 1;
}

:deep(.code-copy-btn:hover) {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
}

:deep(.code-copy-btn:active) {
  transform: scale(0.95);
}
</style>
