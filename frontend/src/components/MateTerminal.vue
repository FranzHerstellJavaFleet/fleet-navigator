<template>
  <div class="space-y-4">
    <!-- Quick Actions -->
    <div class="bg-gradient-to-br from-gray-800/50 to-gray-900/50 p-5 rounded-xl border border-gray-700/50">
      <h4 class="text-sm font-semibold text-gray-300 mb-3 flex items-center gap-2">
        <CommandLineIcon class="w-4 h-4 text-green-400" />
        Quick Actions
      </h4>
      <div class="grid grid-cols-2 md:grid-cols-3 gap-2">
        <button
          v-for="action in quickActions"
          :key="action.label"
          @click="executeQuickAction(action)"
          :disabled="executing"
          class="
            px-3 py-2 rounded-lg text-xs font-medium
            bg-gray-700/50 hover:bg-gray-700
            text-gray-300 hover:text-white
            border border-gray-600/50 hover:border-gray-500
            disabled:opacity-50 disabled:cursor-not-allowed
            transition-all duration-200
            transform hover:scale-105 active:scale-95
            flex items-center justify-center gap-2
          "
        >
          {{ action.label }}
        </button>
      </div>
    </div>

    <!-- Custom Command -->
    <div class="bg-gradient-to-br from-gray-800/50 to-gray-900/50 p-5 rounded-xl border border-gray-700/50">
      <h4 class="text-sm font-semibold text-gray-300 mb-3 flex items-center gap-2">
        <CommandLineIcon class="w-4 h-4 text-blue-400" />
        Custom Command
      </h4>
      <div class="flex gap-2">
        <input
          v-model="customCommand"
          @keydown.enter="executeCustomCommand"
          placeholder="z.B. ls -la /var/log"
          :disabled="executing"
          class="
            flex-1 px-3 py-2 rounded-lg text-sm
            bg-gray-700 text-white
            border border-gray-600
            focus:border-fleet-orange-500 focus:outline-none
            disabled:opacity-50 disabled:cursor-not-allowed
            placeholder-gray-500
          "
        />
        <button
          @click="executeCustomCommand"
          :disabled="executing || !customCommand.trim()"
          class="
            px-4 py-2 rounded-lg text-sm font-semibold
            bg-gradient-to-r from-fleet-orange-500 to-orange-600
            hover:from-fleet-orange-400 hover:to-orange-500
            text-white
            disabled:opacity-50 disabled:cursor-not-allowed
            transition-all duration-200
            transform hover:scale-105 active:scale-95
            flex items-center gap-2
          "
        >
          <CommandLineIcon class="w-4 h-4" />
          Ausführen
        </button>
      </div>
      <p class="text-xs text-gray-500 mt-2">
        Nur whitelisted Commands erlaubt (df, free, ps, systemctl, etc.)
      </p>
    </div>

    <!-- Terminal Output -->
    <div class="bg-black/70 rounded-xl border border-gray-700/50 font-mono text-xs min-h-[400px] max-h-[600px] overflow-y-auto custom-scrollbar">
      <!-- Terminal Header -->
      <div class="flex items-center justify-between px-4 py-2 bg-gray-800/50 border-b border-gray-700 sticky top-0 z-10">
        <div class="flex items-center gap-2 text-gray-400">
          <span class="text-green-400">●</span>
          <span class="font-semibold">fleet-mate@{{ mateId }}</span>
        </div>
        <div class="flex gap-2">
          <button
            @click="clearTerminal"
            class="px-2 py-1 rounded text-xs text-gray-400 hover:text-gray-300 hover:bg-gray-700 transition-colors"
          >
            Clear
          </button>
          <button
            v-if="commandHistory.length > 0"
            @click="showHistory = !showHistory"
            class="px-2 py-1 rounded text-xs text-gray-400 hover:text-gray-300 hover:bg-gray-700 transition-colors"
          >
            History ({{ commandHistory.length }})
          </button>
        </div>
      </div>

      <!-- History Sidebar (if shown) -->
      <div v-if="showHistory" class="border-b border-gray-700 bg-gray-900/50">
        <div class="p-3 space-y-1 max-h-48 overflow-y-auto">
          <div
            v-for="(entry, index) in commandHistory.slice().reverse()"
            :key="index"
            class="text-xs p-2 rounded hover:bg-gray-800 cursor-pointer transition-colors"
            @click="customCommand = entry.fullCommand; showHistory = false"
          >
            <div class="flex items-center justify-between gap-2">
              <span class="text-gray-400 truncate flex-1">{{ entry.fullCommand }}</span>
              <span
                class="px-1.5 py-0.5 rounded text-xs font-semibold"
                :class="entry.exitCode === 0 ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'"
              >
                {{ entry.exitCode }}
              </span>
            </div>
            <div class="text-gray-600 text-xs mt-1">
              {{ formatTimestamp(entry.executedAt) }} · {{ entry.durationMs }}ms
            </div>
          </div>
        </div>
      </div>

      <!-- Terminal Content -->
      <div class="p-4">
        <div v-if="terminalOutput.length === 0" class="text-gray-500 text-center py-8">
          Noch keine Befehle ausgeführt. Nutze Quick Actions oder gib einen Command ein.
        </div>
        <div v-else>
          <div
            v-for="(entry, index) in terminalOutput"
            :key="index"
            class="mb-4 pb-4 border-b border-gray-800 last:border-0"
          >
            <!-- Command Line -->
            <div class="flex items-start gap-2 mb-1">
              <span class="text-blue-400">$</span>
              <span class="text-gray-300 font-semibold">{{ entry.command }}</span>
            </div>

            <!-- Output -->
            <div class="ml-4">
              <!-- stdout -->
              <div v-if="entry.stdout" class="text-green-400 whitespace-pre-wrap">{{ entry.stdout }}</div>

              <!-- stderr -->
              <div v-if="entry.stderr" class="text-red-400 whitespace-pre-wrap">{{ entry.stderr }}</div>

              <!-- Exit code -->
              <div v-if="entry.exitCode !== undefined" class="mt-2 flex items-center gap-2">
                <span
                  class="px-2 py-1 rounded text-xs font-semibold"
                  :class="entry.exitCode === 0 ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'"
                >
                  Exit Code: {{ entry.exitCode }}
                </span>
                <span v-if="entry.duration" class="text-gray-500 text-xs">
                  ({{ entry.duration }}ms)
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Typing indicator -->
        <div v-if="executing" class="flex items-center gap-2 text-gray-500 animate-pulse">
          <span>Executing</span>
          <span class="animate-bounce">.</span>
          <span class="animate-bounce animation-delay-200">.</span>
          <span class="animate-bounce animation-delay-400">.</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { CommandLineIcon } from '@heroicons/vue/24/outline'
import axios from 'axios'
import { useToast } from '../composables/useToast'

const props = defineProps({
  mateId: {
    type: String,
    required: true
  }
})

const { success, error } = useToast()

const quickActions = ref([])
const customCommand = ref('')
const executing = ref(false)
const terminalOutput = ref([])
const commandHistory = ref([])
const showHistory = ref(false)

onMounted(async () => {
  // Load quick actions from backend
  try {
    const response = await axios.get('/api/fleet-mate/whitelisted-commands')
    quickActions.value = response.data.quickActions || []
  } catch (err) {
    console.error('Failed to load quick actions:', err)
  }

  // Load command history
  await loadCommandHistory()
})

async function loadCommandHistory() {
  try {
    const response = await axios.get(`/api/fleet-mate/mates/${props.mateId}/command-history`)
    commandHistory.value = response.data || []
  } catch (err) {
    console.error('Failed to load command history:', err)
  }
}

async function executeQuickAction(action) {
  const commandStr = `${action.command} ${action.args}`.trim()
  await executeCommand(action.command, action.args.split(' ').filter(a => a.length > 0))
}

async function executeCustomCommand() {
  if (!customCommand.value.trim()) return

  const parts = customCommand.value.trim().split(/\s+/)
  const command = parts[0]
  const args = parts.slice(1)

  await executeCommand(command, args)
}

async function executeCommand(command, args) {
  executing.value = true

  const outputEntry = {
    command: `${command} ${args.join(' ')}`.trim(),
    stdout: '',
    stderr: '',
    exitCode: undefined,
    duration: undefined
  }

  terminalOutput.value.push(outputEntry)

  try {
    // Send execute command request
    const response = await axios.post(
      `/api/fleet-mate/mates/${props.mateId}/execute`,
      {
        command: command,
        args: args,
        workingDirectory: '/tmp',
        timeoutSeconds: 300,
        captureStderr: true
      }
    )

    const sessionId = response.data.sessionId

    // Connect to SSE stream for output
    const eventSource = new EventSource(`/api/fleet-mate/exec-stream/${sessionId}`)

    eventSource.addEventListener('start', (event) => {
      console.log('Command execution started')
    })

    eventSource.addEventListener('chunk', (event) => {
      const data = JSON.parse(event.data)
      if (data.type === 'stdout') {
        outputEntry.stdout += data.content
      } else if (data.type === 'stderr') {
        outputEntry.stderr += data.content
      }
    })

    eventSource.addEventListener('done', (event) => {
      const data = JSON.parse(event.data)
      outputEntry.exitCode = data.exitCode
      outputEntry.duration = data.durationMs
      eventSource.close()
      executing.value = false

      // Reload history
      loadCommandHistory()

      if (data.exitCode === 0) {
        success('Command erfolgreich ausgeführt')
      } else {
        error(`Command failed mit Exit Code ${data.exitCode}`)
      }
    })

    eventSource.addEventListener('error', (event) => {
      outputEntry.stderr += '\nError: Failed to execute command\n'
      outputEntry.exitCode = -1
      eventSource.close()
      executing.value = false
      error('Command Execution fehlgeschlagen')
    })

  } catch (err) {
    console.error('Failed to execute command:', err)
    outputEntry.stderr = err.response?.data?.error || err.message
    outputEntry.exitCode = -1
    executing.value = false

    if (err.response?.status === 403) {
      error('Command nicht erlaubt (Security Whitelist)')
    } else {
      error('Command Execution fehlgeschlagen')
    }
  }
}

function clearTerminal() {
  terminalOutput.value = []
}

function formatTimestamp(timestamp) {
  if (!timestamp) return 'N/A'
  const date = new Date(timestamp)
  return date.toLocaleString('de-DE', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: rgba(31, 41, 55, 0.5);
  border-radius: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(75, 85, 99, 0.8);
  border-radius: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(107, 114, 128, 0.9);
}

.animation-delay-200 {
  animation-delay: 0.2s;
}

.animation-delay-400 {
  animation-delay: 0.4s;
}
</style>
