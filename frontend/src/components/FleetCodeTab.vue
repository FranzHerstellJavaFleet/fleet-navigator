<template>
  <div class="space-y-4">
    <!-- Provider Warning -->
    <div v-if="currentProvider && currentProvider !== 'llama-server'"
         class="p-4 rounded-xl border border-amber-500/50 bg-amber-500/10">
      <div class="flex items-start gap-3">
        <ExclamationTriangleIcon class="w-5 h-5 text-amber-400 flex-shrink-0 mt-0.5" />
        <div class="flex-1">
          <h4 class="text-sm font-semibold text-amber-300">Provider-Hinweis</h4>
          <p class="text-sm text-amber-200/80 mt-1">
            FleetCode benötigt den <strong>llama-server</strong> Provider.
            Aktuell ist <strong>{{ currentProvider }}</strong> aktiv.
          </p>
          <button
            @click="openProviderSettings"
            class="mt-2 px-3 py-1.5 text-xs font-medium rounded-lg
                   bg-amber-500 hover:bg-amber-400 text-gray-900
                   transition-colors duration-200"
          >
            Zu llama-server wechseln
          </button>
        </div>
      </div>
    </div>

    <!-- FleetCode Input Form -->
    <div class="bg-gray-800/50 p-4 rounded-xl border border-gray-700/50">
      <h4 class="text-sm font-semibold text-gray-300 mb-3 flex items-center gap-2">
        <CodeBracketIcon class="w-4 h-4 text-blue-400" />
        FleetCode AI Coding Agent
      </h4>

      <div class="space-y-3">
        <!-- Task Input -->
        <div>
          <label class="text-xs text-gray-400 block mb-1">Aufgabe</label>
          <textarea
            v-model="task"
            rows="3"
            placeholder="z.B. 'Finde alle TODO-Kommentare und erstelle eine Liste' oder 'Erstelle eine README.md fuer dieses Projekt'"
            class="w-full px-3 py-2 bg-gray-700 text-white rounded-lg text-sm border border-gray-600 focus:border-fleet-orange-500 focus:outline-none resize-none"
          ></textarea>
        </div>

        <!-- Working Directory -->
        <div>
          <label class="text-xs text-gray-400 block mb-1">Arbeitsverzeichnis</label>
          <input
            v-model="workingDir"
            type="text"
            placeholder="/home/user/projekt"
            class="w-full px-3 py-2 bg-gray-700 text-white rounded-lg text-sm border border-gray-600 focus:border-fleet-orange-500 focus:outline-none"
          />
        </div>

        <!-- Execute Button -->
        <button
          @click="executeFleetCode"
          :disabled="executing || !task.trim() || !mateOnline"
          class="w-full px-4 py-2 rounded-lg bg-gradient-to-r from-blue-500 to-cyan-600
                 hover:from-blue-400 hover:to-cyan-500
                 text-white font-semibold text-sm
                 disabled:opacity-50 disabled:cursor-not-allowed
                 transition-all duration-200 transform hover:scale-105 active:scale-95
                 flex items-center justify-center gap-2"
        >
          <PlayIcon v-if="!executing" class="w-4 h-4" />
          <ArrowPathIcon v-else class="w-4 h-4 animate-spin" />
          {{ executing ? 'Wird ausgefuehrt...' : 'FleetCode starten' }}
        </button>

        <p v-if="!mateOnline" class="text-xs text-red-400 text-center">
          Mate ist offline - FleetCode nicht verfuegbar
        </p>
      </div>
    </div>

    <!-- Execution Progress -->
    <div v-if="executing || steps.length > 0" class="bg-gray-800/50 p-4 rounded-xl border border-gray-700/50">
      <h4 class="text-sm font-semibold text-gray-300 mb-3 flex items-center gap-2">
        <ClockIcon class="w-4 h-4 text-yellow-400" />
        Ausfuehrung
        <span v-if="executing" class="text-xs text-gray-500">(laeuft...)</span>
      </h4>

      <!-- Steps -->
      <div class="space-y-2 max-h-[300px] overflow-y-auto">
        <div
          v-for="(step, index) in steps"
          :key="index"
          class="p-2 rounded-lg text-sm"
          :class="step.error ? 'bg-red-500/10 border border-red-500/30' : 'bg-gray-700/50'"
        >
          <div class="flex items-center gap-2">
            <span class="text-xs text-gray-500">{{ step.step }}</span>
            <span class="font-mono text-blue-400">{{ step.tool }}</span>
            <CheckCircleIcon v-if="!step.error" class="w-4 h-4 text-green-400 ml-auto" />
            <XCircleIcon v-else class="w-4 h-4 text-red-400 ml-auto" />
          </div>
          <div v-if="step.output" class="mt-1 text-xs text-gray-400 font-mono truncate">
            {{ truncate(step.output, 100) }}
          </div>
          <div v-if="step.error" class="mt-1 text-xs text-red-400">
            {{ step.error }}
          </div>
        </div>

        <!-- Loading indicator -->
        <div v-if="executing && steps.length === 0" class="flex items-center gap-2 text-gray-400">
          <ArrowPathIcon class="w-4 h-4 animate-spin" />
          <span class="text-sm">Verbinde mit Mate...</span>
        </div>
      </div>
    </div>

    <!-- Final Result -->
    <div v-if="result" class="p-4 rounded-xl border" :class="result.success ? 'bg-green-500/10 border-green-500/30' : 'bg-red-500/10 border-red-500/30'">
      <h4 class="text-sm font-semibold mb-2 flex items-center gap-2" :class="result.success ? 'text-green-400' : 'text-red-400'">
        <CheckCircleIcon v-if="result.success" class="w-4 h-4" />
        <XCircleIcon v-else class="w-4 h-4" />
        {{ result.success ? 'Erfolgreich abgeschlossen' : 'Fehlgeschlagen' }}
      </h4>

      <div class="text-sm text-gray-300 whitespace-pre-wrap font-mono bg-gray-900/50 p-3 rounded-lg">
        {{ result.summary || result.error }}
      </div>

      <div class="mt-2 flex items-center gap-4 text-xs text-gray-500">
        <span>{{ result.totalSteps }} Schritte</span>
        <span>{{ result.durationSecs?.toFixed(1) }}s</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, defineProps, onMounted, defineEmits } from 'vue'
import {
  CodeBracketIcon,
  PlayIcon,
  ArrowPathIcon,
  ClockIcon,
  CheckCircleIcon,
  XCircleIcon,
  ExclamationTriangleIcon
} from '@heroicons/vue/24/outline'
import axios from 'axios'
import { useToast } from '../composables/useToast'

const props = defineProps({
  mateId: {
    type: String,
    required: true
  },
  mateOnline: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['openSettings'])

const { success: successToast, error: errorToast } = useToast()

const task = ref('')
const workingDir = ref('/home/trainer/ProjekteFMH')
const executing = ref(false)
const steps = ref([])
const result = ref(null)
const currentProvider = ref(null)

// Load current provider on mount
onMounted(async () => {
  try {
    const response = await axios.get('/api/llm/providers')
    currentProvider.value = response.data.activeProvider
  } catch (err) {
    console.error('Could not load provider info:', err)
  }
})

function openProviderSettings() {
  emit('openSettings', 'provider')
}

async function executeFleetCode() {
  if (!task.value.trim()) return

  executing.value = true
  steps.value = []
  result.value = null

  try {
    // Start execution
    const response = await axios.post(`/api/fleetcode/execute/${props.mateId}`, {
      task: task.value,
      workingDir: workingDir.value
    })

    const sessionId = response.data.sessionId
    console.log('FleetCode session started:', sessionId)

    // Connect to SSE stream
    const eventSource = new EventSource(`/api/fleetcode/stream/${sessionId}`)

    eventSource.addEventListener('connected', (event) => {
      console.log('SSE connected:', event.data)
    })

    eventSource.addEventListener('step', (event) => {
      const stepData = JSON.parse(event.data)
      steps.value.push(stepData)
      console.log('FleetCode step:', stepData)
    })

    eventSource.addEventListener('result', (event) => {
      const resultData = JSON.parse(event.data)
      result.value = resultData
      executing.value = false
      eventSource.close()

      if (resultData.success) {
        successToast('FleetCode abgeschlossen')
      } else {
        errorToast('FleetCode fehlgeschlagen')
      }
    })

    eventSource.addEventListener('error', (event) => {
      // Check if this is just the stream closing after completion
      if (result.value) {
        console.log('SSE closed after completion')
        return
      }

      console.error('SSE error:', event)
      executing.value = false
      eventSource.close()
      errorToast('Verbindung unterbrochen')
    })

  } catch (err) {
    console.error('FleetCode error:', err)
    executing.value = false
    errorToast(err.response?.data?.error || 'Fehler beim Starten')
  }
}

function truncate(str, maxLen) {
  if (!str) return ''
  if (str.length <= maxLen) return str
  return str.substring(0, maxLen) + '...'
}
</script>
