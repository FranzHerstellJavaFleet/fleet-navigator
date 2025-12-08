<template>
  <div class="fleet-mates-dashboard min-h-screen p-6 bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 overflow-y-auto">
    <!-- Header -->
    <div class="mb-8">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <div class="p-3 rounded-xl bg-gradient-to-br from-fleet-orange-500 to-orange-600 shadow-lg">
            <ServerIcon class="w-8 h-8 text-white" />
          </div>
          <div>
            <h1 class="text-3xl font-bold text-white">Fleet Maate</h1>
            <p class="text-gray-400 mt-1">Verwalte und überwache deine Remote-Agents</p>
          </div>
        </div>

      </div>

      <div class="flex items-center justify-end mt-4">
        <!-- Summary Cards -->
        <div class="flex gap-4">
          <div class="bg-gradient-to-br from-gray-800/50 to-gray-900/50 backdrop-blur-sm px-6 py-3 rounded-xl border border-gray-700/50">
            <div class="text-xs text-gray-400 mb-1">Gesamt</div>
            <div class="text-2xl font-bold text-white">{{ mates.length }}</div>
          </div>
          <div class="bg-gradient-to-br from-green-500/20 to-emerald-500/20 backdrop-blur-sm px-6 py-3 rounded-xl border border-green-500/30">
            <div class="text-xs text-green-400 mb-1">Online</div>
            <div class="text-2xl font-bold text-green-400">{{ onlineCount }}</div>
          </div>
          <div class="bg-gradient-to-br from-red-500/20 to-rose-500/20 backdrop-blur-sm px-6 py-3 rounded-xl border border-red-500/30">
            <div class="text-xs text-red-400 mb-1">Offline</div>
            <div class="text-2xl font-bold text-red-400">{{ offlineCount }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Pending Pairing Requests -->
    <div v-if="pendingRequests.length > 0" class="mb-8">
      <h2 class="text-xl font-bold text-white mb-4 flex items-center gap-2">
        <LinkIcon class="w-6 h-6 text-blue-400 animate-pulse" />
        Neue Pairing-Anfragen
        <span class="px-2 py-0.5 bg-blue-500 text-white text-sm rounded-full">{{ pendingRequests.length }}</span>
      </h2>

      <div class="space-y-4">
        <div
          v-for="request in pendingRequests"
          :key="request.requestId"
          class="bg-gradient-to-br from-blue-900/30 to-cyan-900/30 backdrop-blur-sm rounded-2xl border border-blue-500/30 p-6 animate-pulse-slow"
        >
          <div class="flex flex-col lg:flex-row lg:items-center gap-6">
            <!-- Mate Info -->
            <div class="flex items-center gap-4 flex-1">
              <div class="p-3 rounded-xl bg-gradient-to-br from-blue-500 to-cyan-600 shadow-lg">
                <component :is="getMateTypeIcon(request.mateType)" class="w-8 h-8 text-white" />
              </div>
              <div>
                <h3 class="text-lg font-bold text-white">{{ request.mateName }}</h3>
                <p class="text-sm text-gray-400">{{ getMateTypeLabel(request.mateType) }}</p>
                <!-- Fingerprint -->
                <p v-if="request.fingerprint" class="text-xs font-mono text-cyan-400 mt-1">
                  ID: {{ request.fingerprint }}
                </p>
              </div>
            </div>

            <!-- IP Addresses -->
            <div v-if="request.ipv4 || request.ipv6" class="text-center lg:text-left">
              <p class="text-xs text-gray-400 mb-1">Netzwerk</p>
              <div class="space-y-1">
                <div v-if="request.ipv4" class="flex items-center gap-2 text-sm text-gray-300">
                  <span class="px-1.5 py-0.5 bg-green-500/20 text-green-400 text-xs rounded">IPv4</span>
                  <span class="font-mono">{{ request.ipv4 }}</span>
                </div>
                <div v-if="request.ipv6" class="flex items-center gap-2 text-sm text-gray-300">
                  <span class="px-1.5 py-0.5 bg-purple-500/20 text-purple-400 text-xs rounded">IPv6</span>
                  <span class="font-mono text-xs">{{ request.ipv6 }}</span>
                </div>
              </div>
            </div>

            <!-- Pairing Code -->
            <div class="text-center lg:text-left">
              <p class="text-xs text-gray-400 mb-1">Pairing-Code</p>
              <div class="flex gap-1">
                <span
                  v-for="(digit, index) in request.pairingCode.split('')"
                  :key="index"
                  class="w-8 h-10 flex items-center justify-center text-lg font-mono font-bold bg-gradient-to-br from-blue-500 to-cyan-500 text-white rounded-lg shadow-lg"
                >
                  {{ digit }}
                </span>
              </div>
            </div>

            <!-- Timer -->
            <div class="flex items-center gap-2 text-yellow-400">
              <ClockIcon class="w-5 h-5" />
              <span class="font-mono">{{ formatTimeRemaining(request.expiresAt) }}</span>
            </div>

            <!-- Actions -->
            <div class="flex gap-3">
              <button
                @click="rejectPairing(request.requestId)"
                :disabled="processingRequest === request.requestId"
                class="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-gray-300 rounded-xl font-medium transition-all disabled:opacity-50 flex items-center gap-2"
              >
                <XMarkIcon class="w-5 h-5" />
                Ablehnen
              </button>
              <button
                @click="approvePairing(request.requestId)"
                :disabled="processingRequest === request.requestId"
                class="px-6 py-2 bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-400 hover:to-emerald-500 text-white rounded-xl font-medium shadow-lg hover:shadow-xl transition-all disabled:opacity-50 flex items-center gap-2"
              >
                <CheckIcon class="w-5 h-5" />
                {{ processingRequest === request.requestId ? 'Wird verbunden...' : 'Akzeptieren' }}
              </button>
            </div>
          </div>

          <!-- Security Notice -->
          <div class="mt-4 flex items-start gap-3 p-3 bg-amber-900/20 rounded-xl border border-amber-500/20">
            <ShieldExclamationIcon class="w-5 h-5 text-amber-400 flex-shrink-0 mt-0.5" />
            <p class="text-sm text-amber-300/80">
              <span class="font-medium">DSGVO-Hinweis:</span> Nach Akzeptierung kann dieser Mate verschlüsselt mit dem Navigator kommunizieren. Nur Anfragen von bekannten Geräten akzeptieren.
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- No Mates State -->
    <div v-if="mates.length === 0 && pendingRequests.length === 0" class="
      bg-gradient-to-br from-gray-800/30 to-gray-900/30
      backdrop-blur-sm
      p-12 rounded-2xl
      border border-gray-700/30 border-dashed
      text-center
    ">
      <ServerIcon class="w-20 h-20 text-gray-600 mx-auto mb-4" />
      <h3 class="text-xl font-semibold text-gray-400 mb-2">Keine Fleet Maate verbunden</h3>
      <p class="text-sm text-gray-500 max-w-md mx-auto">
        Starte einen Fleet Maat auf deinem Remote-System, um Hardware-Daten zu sammeln und Commands auszuführen.
      </p>
      <div class="mt-6 p-4 bg-gray-800/50 rounded-lg border border-gray-700/50 inline-block text-left">
        <code class="text-xs text-gray-400">
          cd Fleet-Mate-Linux<br />
          ./fleet-mate
        </code>
      </div>
    </div>

    <!-- Mate Tiles Grid -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">

      <!-- ==================== SYSTEM AGENT TILE ==================== -->
      <template v-for="mate in mates" :key="mate.mateId">
        <div
          v-if="mate.mateType === 'os'"
          class="
            group
            bg-gradient-to-br from-gray-800/50 to-gray-900/50
            backdrop-blur-sm
            rounded-2xl
            border border-gray-700/50
            p-6
            transition-all duration-300
            hover:shadow-2xl
            hover:shadow-blue-500/20
            hover:border-blue-500/50
          "
        >
          <!-- System Mate Header -->
          <div class="flex items-start justify-between mb-4">
            <div class="flex items-center gap-3">
              <div class="
                p-2 rounded-lg
                bg-gradient-to-br from-blue-500 to-cyan-600
                group-hover:scale-110 transition-transform duration-300
              ">
                <ComputerDesktopIcon class="w-5 h-5 text-white" />
              </div>
              <div>
                <h3 class="font-bold text-white text-lg">{{ mate.name }}</h3>
                <span class="text-xs px-2 py-0.5 rounded-full bg-blue-500/20 text-blue-400 border border-blue-500/30">
                  System-Agent
                </span>
              </div>
            </div>
            <span
              class="px-2 py-1 rounded-full text-xs font-semibold flex items-center gap-1"
              :class="mate.status === 'ONLINE'
                ? 'bg-green-500/20 text-green-400 border border-green-500/30'
                : 'bg-red-500/20 text-red-400 border border-red-500/30'"
            >
              <component :is="mate.status === 'ONLINE' ? CheckCircleIcon : XCircleIcon" class="w-3 h-3" />
              {{ mate.status }}
            </span>
          </div>

          <!-- System Mate Info -->
          <div class="space-y-2 mb-4">
            <div class="text-xs text-gray-400">{{ mate.description }}</div>
            <div class="flex items-center gap-2 text-xs text-gray-500">
              <CommandLineIcon class="w-4 h-4" />
              <span class="truncate">{{ mate.mateId }}</span>
            </div>
          </div>

          <!-- System Stats -->
          <div v-if="mateStats[mate.mateId] && mate.status === 'ONLINE'" class="space-y-3">
            <!-- OS Info -->
            <div class="flex items-center gap-2 text-sm">
              <div class="p-1.5 rounded bg-blue-500/20">
                <CommandLineIcon class="w-4 h-4 text-blue-400" />
              </div>
              <span class="text-gray-300">
                {{ formatOS(mateStats[mate.mateId].system) }}
              </span>
            </div>

            <!-- CPU Usage -->
            <div>
              <div class="flex items-center justify-between mb-1">
                <span class="text-xs text-gray-400">CPU</span>
                <span class="text-xs font-semibold" :class="getCPUColor(mateStats[mate.mateId].cpu?.usage_percent)">
                  {{ mateStats[mate.mateId].cpu?.usage_percent?.toFixed(1) }}%
                </span>
              </div>
              <div class="h-2 bg-gray-700 rounded-full overflow-hidden">
                <div
                  class="h-full transition-all duration-300"
                  :class="getCPUBarColor(mateStats[mate.mateId].cpu?.usage_percent)"
                  :style="{ width: mateStats[mate.mateId].cpu?.usage_percent + '%' }"
                />
              </div>
            </div>

            <!-- RAM Usage -->
            <div>
              <div class="flex items-center justify-between mb-1">
                <span class="text-xs text-gray-400">RAM</span>
                <span class="text-xs font-semibold" :class="getRAMColor(mateStats[mate.mateId].memory?.used_percent)">
                  {{ mateStats[mate.mateId].memory?.used_percent?.toFixed(1) }}%
                </span>
              </div>
              <div class="h-2 bg-gray-700 rounded-full overflow-hidden">
                <div
                  class="h-full transition-all duration-300"
                  :class="getRAMBarColor(mateStats[mate.mateId].memory?.used_percent)"
                  :style="{ width: mateStats[mate.mateId].memory?.used_percent + '%' }"
                />
              </div>
            </div>

            <!-- Temperature -->
            <div v-if="mateStats[mate.mateId].temperature?.cpu_package" class="flex items-center justify-between">
              <span class="text-xs text-gray-400">Temperatur</span>
              <span
                class="text-xs font-semibold px-2 py-0.5 rounded-full"
                :class="getTempBadgeColor(mateStats[mate.mateId].temperature?.cpu_package)"
              >
                {{ mateStats[mate.mateId].temperature?.cpu_package?.toFixed(0) }}°C
              </span>
            </div>
          </div>

          <!-- Offline State -->
          <div v-else-if="mate.status === 'OFFLINE'" class="text-center py-4">
            <XCircleIcon class="w-8 h-8 text-red-400/50 mx-auto mb-2" />
            <p class="text-xs text-gray-500">System-Agent ist offline</p>
          </div>

          <!-- Loading State -->
          <div v-else class="text-center py-4">
            <ArrowPathIcon class="w-6 h-6 text-gray-500 mx-auto mb-2 animate-spin" />
            <p class="text-xs text-gray-500">Lade System-Daten...</p>
          </div>

          <!-- System Agent Actions -->
          <div class="mt-4 pt-4 border-t border-gray-700/50 flex items-center gap-2">
            <button
              @click.stop="openMateDetail(mate)"
              class="
                flex-1 py-2 px-3 rounded-lg
                bg-gray-700/50 hover:bg-gray-700
                text-xs text-gray-300 hover:text-white
                transition-all duration-200
                flex items-center justify-center gap-2
              "
            >
              <span>Details</span>
              <ChevronRightIcon class="w-4 h-4" />
            </button>
            <button
              @click.stop="openMateInNewTab(mate)"
              class="
                py-2 px-3 rounded-lg
                bg-blue-500/20 hover:bg-blue-500/30
                text-xs text-blue-400 hover:text-blue-300
                border border-blue-500/30 hover:border-blue-500/50
                transition-all duration-200
                flex items-center gap-1
              "
              title="In neuem Tab öffnen"
            >
              <PlusIcon class="w-4 h-4" />
            </button>
            <button
              @click.stop="removeMate(mate)"
              class="
                py-2 px-3 rounded-lg
                bg-red-500/20 hover:bg-red-500/30
                text-xs text-red-400 hover:text-red-300
                border border-red-500/30 hover:border-red-500/50
                transition-all duration-200
                flex items-center gap-1
              "
              title="Mate entfernen"
            >
              <TrashIcon class="w-4 h-4" />
            </button>
          </div>
        </div>

        <!-- ==================== EMAIL AGENT TILE ==================== -->
        <div
          v-else-if="mate.mateType === 'mail' || mate.mateType === 'email'"
          class="
            group
            bg-gradient-to-br from-purple-900/30 to-violet-900/30
            backdrop-blur-sm
            rounded-2xl
            border border-purple-700/50
            p-6
            transition-all duration-300
            hover:shadow-2xl
            hover:shadow-purple-500/20
            hover:border-purple-500/50
          "
        >
          <!-- Email Mate Header -->
          <div class="flex items-start justify-between mb-4">
            <div class="flex items-center gap-3">
              <div class="
                p-2 rounded-lg
                bg-gradient-to-br from-purple-500 to-violet-600
                group-hover:scale-110 transition-transform duration-300
              ">
                <EnvelopeIcon class="w-5 h-5 text-white" />
              </div>
              <div>
                <h3 class="font-bold text-white text-lg">{{ mate.name }}</h3>
                <span class="text-xs px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-400 border border-purple-500/30">
                  E-Mail-Agent
                </span>
              </div>
            </div>
            <span
              class="px-2 py-1 rounded-full text-xs font-semibold flex items-center gap-1"
              :class="mate.status === 'ONLINE'
                ? 'bg-green-500/20 text-green-400 border border-green-500/30'
                : 'bg-red-500/20 text-red-400 border border-red-500/30'"
            >
              <component :is="mate.status === 'ONLINE' ? CheckCircleIcon : XCircleIcon" class="w-3 h-3" />
              {{ mate.status }}
            </span>
          </div>

          <!-- Email Mate Info -->
          <div class="space-y-2 mb-4">
            <div class="flex items-center gap-2 text-sm text-purple-300">
              <EnvelopeIcon class="w-4 h-4" />
              <span>Thunderbird Extension</span>
            </div>
          </div>

          <!-- Email Stats - Online -->
          <div v-if="mate.status === 'ONLINE'" class="space-y-4">
            <!-- Total Processed -->
            <div class="bg-purple-900/30 rounded-xl p-4 border border-purple-700/30">
              <div class="text-center">
                <div class="text-3xl font-bold text-purple-300">
                  {{ mateStats[mate.mateId]?.totalEmails || 0 }}
                </div>
                <div class="text-xs text-purple-400 mt-1">E-Mails verarbeitet</div>
              </div>
            </div>

            <!-- Category Breakdown -->
            <div v-if="mateStats[mate.mateId] && getEmailCategories(mateStats[mate.mateId]).length > 0" class="space-y-2">
              <div class="text-xs text-gray-400 mb-2">Kategorien:</div>
              <div
                v-for="cat in getEmailCategories(mateStats[mate.mateId])"
                :key="cat.name"
                class="flex items-center justify-between text-sm"
              >
                <span class="text-gray-300 truncate flex-1">{{ cat.name }}</span>
                <span class="px-3 py-1 rounded-full bg-purple-500/20 text-purple-300 font-semibold ml-2">
                  {{ cat.count }}
                </span>
              </div>
            </div>

            <!-- No activity yet -->
            <div v-else class="text-center py-2">
              <p class="text-sm text-purple-400/60">Warte auf E-Mail-Aktivität...</p>
              <p class="text-xs text-gray-500 mt-1">Emails werden automatisch klassifiziert</p>
            </div>
          </div>

          <!-- Offline State -->
          <div v-else class="text-center py-6">
            <XCircleIcon class="w-10 h-10 text-purple-400/30 mx-auto mb-2" />
            <p class="text-sm text-purple-400/60">E-Mail-Agent ist offline</p>
            <p class="text-xs text-gray-500 mt-1">Thunderbird starten um zu verbinden</p>
          </div>

          <!-- Email Agent Actions - Simplified -->
          <div class="mt-4 pt-4 border-t border-purple-700/30 flex items-center gap-2">
            <button
              @click.stop="removeMate(mate)"
              class="
                flex-1 py-2 px-3 rounded-lg
                bg-red-500/20 hover:bg-red-500/30
                text-xs text-red-400 hover:text-red-300
                border border-red-500/30 hover:border-red-500/50
                transition-all duration-200
                flex items-center justify-center gap-2
              "
              title="Mate entfernen"
            >
              <TrashIcon class="w-4 h-4" />
              <span>Entfernen</span>
            </button>
          </div>
        </div>

        <!-- ==================== GENERIC/OTHER AGENT TILE ==================== -->
        <div
          v-else
          class="
            group
            bg-gradient-to-br from-gray-800/50 to-gray-900/50
            backdrop-blur-sm
            rounded-2xl
            border border-gray-700/50
            p-6
            transition-all duration-300
            hover:shadow-2xl
            hover:shadow-fleet-orange-500/20
            hover:border-fleet-orange-500/50
          "
        >
          <!-- Generic Mate Header -->
          <div class="flex items-start justify-between mb-4">
            <div class="flex items-center gap-3">
              <div class="
                p-2 rounded-lg
                bg-gradient-to-br from-fleet-orange-500 to-orange-600
                group-hover:scale-110 transition-transform duration-300
              ">
                <component :is="getMateTypeIcon(mate.mateType)" class="w-5 h-5 text-white" />
              </div>
              <div>
                <h3 class="font-bold text-white text-lg">{{ mate.name }}</h3>
                <span class="text-xs px-2 py-0.5 rounded-full" :class="getMateTypeBadgeClass(mate.mateType)">
                  {{ getMateTypeLabel(mate.mateType) }}
                </span>
              </div>
            </div>
            <span
              class="px-2 py-1 rounded-full text-xs font-semibold flex items-center gap-1"
              :class="mate.status === 'ONLINE'
                ? 'bg-green-500/20 text-green-400 border border-green-500/30'
                : 'bg-red-500/20 text-red-400 border border-red-500/30'"
            >
              <component :is="mate.status === 'ONLINE' ? CheckCircleIcon : XCircleIcon" class="w-3 h-3" />
              {{ mate.status }}
            </span>
          </div>

          <!-- Generic Info -->
          <div class="space-y-2 mb-4">
            <div class="text-xs text-gray-400">{{ mate.description }}</div>
            <div class="flex items-center gap-2 text-xs text-gray-500">
              <ServerIcon class="w-4 h-4" />
              <span class="truncate">{{ mate.mateId }}</span>
            </div>
          </div>

          <!-- Status -->
          <div v-if="mate.status === 'ONLINE'" class="text-center py-4">
            <CheckCircleIcon class="w-8 h-8 text-green-400/50 mx-auto mb-2" />
            <p class="text-xs text-gray-400">Agent verbunden</p>
          </div>
          <div v-else class="text-center py-4">
            <XCircleIcon class="w-8 h-8 text-red-400/50 mx-auto mb-2" />
            <p class="text-xs text-gray-500">Agent ist offline</p>
          </div>

          <!-- Generic Actions -->
          <div class="mt-4 pt-4 border-t border-gray-700/50 flex items-center gap-2">
            <button
              @click.stop="removeMate(mate)"
              class="
                flex-1 py-2 px-3 rounded-lg
                bg-red-500/20 hover:bg-red-500/30
                text-xs text-red-400 hover:text-red-300
                border border-red-500/30 hover:border-red-500/50
                transition-all duration-200
                flex items-center justify-center gap-2
              "
              title="Mate entfernen"
            >
              <TrashIcon class="w-4 h-4" />
              <span>Entfernen</span>
            </button>
          </div>
        </div>
      </template>
    </div>

    <!-- Refresh Button -->
    <div v-if="mates.length > 0" class="mt-8 flex justify-center">
      <button
        @click.stop="refreshAllData"
        :disabled="isRefreshing"
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
      >
        <ArrowPathIcon class="w-5 h-5" :class="{ 'animate-spin': isRefreshing }" />
        <span>{{ isRefreshing ? 'Aktualisiere...' : 'Alle Daten aktualisieren' }}</span>
      </button>
    </div>

    <!-- Mate Detail Modal -->
    <MateDetailModal
      v-if="selectedMate"
      :mate="selectedMate"
      :stats="mateStats[selectedMate.mateId]"
      @close="closeMateDetail"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import {
  ServerIcon,
  CheckCircleIcon,
  XCircleIcon,
  XMarkIcon,
  ArrowPathIcon,
  ComputerDesktopIcon,
  CommandLineIcon,
  ChevronRightIcon,
  PlusIcon,
  LinkIcon,
  ShieldExclamationIcon,
  ClockIcon,
  CheckIcon,
  EnvelopeIcon,
  DocumentIcon,
  GlobeAltIcon,
  TrashIcon
} from '@heroicons/vue/24/outline'
import MateDetailModal from '../../components/MateDetailModal.vue'
import axios from 'axios'
import { useConfirmDialog } from '../../composables/useConfirmDialog'

const { confirmDelete } = useConfirmDialog()

const mates = ref([])
const mateStats = ref({})
const isRefreshing = ref(false)
const selectedMate = ref(null)
const pendingRequests = ref([])
const processingRequest = ref(null)
let intervalId = null
let pairingIntervalId = null

const onlineCount = computed(() => mates.value.filter(m => m.status === 'ONLINE').length)
const offlineCount = computed(() => mates.value.filter(m => m.status === 'OFFLINE').length)

onMounted(async () => {
  await loadMates()
  await loadAllStats()
  await loadPendingRequests()
  // Auto-refresh mates every 30 seconds
  intervalId = setInterval(async () => {
    await loadMates()
    await loadAllStats()
  }, 30000)
  // Check for pairing requests every 2 seconds
  pairingIntervalId = setInterval(loadPendingRequests, 2000)
})

onUnmounted(() => {
  if (intervalId) clearInterval(intervalId)
  if (pairingIntervalId) clearInterval(pairingIntervalId)
})

async function loadMates() {
  try {
    // Load both trusted mates (from DB) and connected mates (from WebSocket)
    const [trustedResponse, connectedResponse] = await Promise.all([
      axios.get('/api/pairing/trusted'),
      axios.get('/api/fleet-mate/mates')
    ])

    const trustedMates = trustedResponse.data || []
    const connectedMates = connectedResponse.data || []

    // Create a map of connected mates by mateId
    const connectedMap = new Map()
    for (const cm of connectedMates) {
      connectedMap.set(cm.mateId, cm)
    }

    // Merge: show ALL trusted mates, with ONLINE status if connected
    mates.value = trustedMates.map(tm => {
      const connected = connectedMap.get(tm.mateId)
      return {
        mateId: tm.mateId,
        name: tm.name,
        description: connected?.description || `${getMateTypeLabel(tm.mateType)} - Akzeptiert`,
        status: connected ? 'ONLINE' : 'OFFLINE',
        mateType: tm.mateType,
        lastAuthAt: tm.lastAuthAt
      }
    })
  } catch (error) {
    console.error('Failed to load mates:', error)
  }
}

async function loadPendingRequests() {
  try {
    const response = await axios.get('/api/pairing/pending')
    pendingRequests.value = response.data
  } catch (error) {
    console.error('Failed to load pending requests:', error)
  }
}

async function approvePairing(requestId) {
  processingRequest.value = requestId
  try {
    await axios.post(`/api/pairing/approve/${requestId}`)
    pendingRequests.value = pendingRequests.value.filter(r => r.requestId !== requestId)
    // Refresh mates after approval
    setTimeout(loadMates, 1000)
  } catch (error) {
    console.error('Failed to approve pairing:', error)
    alert('Fehler: ' + (error.response?.data?.error || error.message))
  } finally {
    processingRequest.value = null
  }
}

async function rejectPairing(requestId) {
  processingRequest.value = requestId
  try {
    await axios.post(`/api/pairing/reject/${requestId}`)
    pendingRequests.value = pendingRequests.value.filter(r => r.requestId !== requestId)
  } catch (error) {
    console.error('Failed to reject pairing:', error)
  } finally {
    processingRequest.value = null
  }
}

async function removeMate(mate) {
  const confirmed = await confirmDelete(mate.name, 'Dies entfernt den Mate aus der Trusted-Liste und trennt die Verbindung.')
  if (!confirmed) return

  try {
    await axios.delete(`/api/pairing/trusted/${mate.mateId}`)
    // Remove from local list immediately
    mates.value = mates.value.filter(m => m.mateId !== mate.mateId)
    delete mateStats.value[mate.mateId]
  } catch (error) {
    console.error('Failed to remove mate:', error)
    alert('Fehler beim Entfernen: ' + (error.response?.data?.error || error.message))
  }
}

function getMateTypeIcon(type) {
  switch (type) {
    case 'os': return ComputerDesktopIcon
    case 'mail': return EnvelopeIcon
    case 'office': return DocumentIcon
    case 'browser': return GlobeAltIcon
    default: return ServerIcon
  }
}

function getMateTypeLabel(type) {
  switch (type) {
    case 'os': return 'System-Agent'
    case 'mail': return 'E-Mail-Agent'
    case 'office': return 'Office-Agent'
    case 'browser': return 'Browser-Agent'
    default: return 'Fleet Mate'
  }
}

function getMateTypeBadgeClass(type) {
  switch (type) {
    case 'os': return 'bg-blue-500/20 text-blue-400 border border-blue-500/30'
    case 'mail': return 'bg-purple-500/20 text-purple-400 border border-purple-500/30'
    case 'office': return 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
    case 'browser': return 'bg-cyan-500/20 text-cyan-400 border border-cyan-500/30'
    default: return 'bg-gray-500/20 text-gray-400 border border-gray-500/30'
  }
}

function formatTimeRemaining(expiresAt) {
  const now = new Date()
  const expires = new Date(expiresAt)
  const seconds = Math.max(0, Math.floor((expires - now) / 1000))
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${minutes}:${secs.toString().padStart(2, '0')}`
}

async function loadAllStats() {
  for (const mate of mates.value) {
    if (mate.status === 'ONLINE') {
      try {
        const response = await axios.get(`/api/fleet-mate/mates/${mate.mateId}/stats`)
        mateStats.value[mate.mateId] = response.data
      } catch (error) {
        console.error(`Failed to load stats for ${mate.mateId}:`, error)
      }
    }
  }
}

async function refreshAllData() {
  isRefreshing.value = true
  try {
    await loadMates()
    await loadAllStats()
  } finally {
    setTimeout(() => {
      isRefreshing.value = false
    }, 500)
  }
}

function openMateDetail(mate) {
  selectedMate.value = mate
}

function openMateInNewTab(mate) {
  window.open(`/agents/fleet-mates/${mate.mateId}`, '_blank')
}

function closeMateDetail() {
  selectedMate.value = null
}

function formatOS(system) {
  if (!system) return 'Unknown'
  const os = system.platform || 'Linux'
  const version = system.platform_version || ''
  return `${os.charAt(0).toUpperCase() + os.slice(1)} ${version}`
}

function getCPUColor(percent) {
  if (!percent) return 'text-gray-400'
  if (percent < 50) return 'text-green-400'
  if (percent < 75) return 'text-yellow-400'
  return 'text-red-400'
}

function getCPUBarColor(percent) {
  if (!percent) return 'bg-gray-600'
  if (percent < 50) return 'bg-gradient-to-r from-green-500 to-emerald-500'
  if (percent < 75) return 'bg-gradient-to-r from-yellow-500 to-amber-500'
  return 'bg-gradient-to-r from-red-500 to-rose-500'
}

function getRAMColor(percent) {
  if (!percent) return 'text-gray-400'
  if (percent < 70) return 'text-blue-400'
  if (percent < 85) return 'text-yellow-400'
  return 'text-red-400'
}

function getRAMBarColor(percent) {
  if (!percent) return 'bg-gray-600'
  if (percent < 70) return 'bg-gradient-to-r from-blue-500 to-cyan-500'
  if (percent < 85) return 'bg-gradient-to-r from-yellow-500 to-amber-500'
  return 'bg-gradient-to-r from-red-500 to-rose-500'
}

function getTempBadgeColor(temp) {
  if (!temp) return 'bg-gray-600 text-gray-300'
  if (temp < 60) return 'bg-green-500/20 text-green-400 border border-green-500/30'
  if (temp < 80) return 'bg-yellow-500/20 text-yellow-400 border border-yellow-500/30'
  return 'bg-red-500/20 text-red-400 border border-red-500/30'
}

// Extract email categories from stats object (exclude known meta fields)
function getEmailCategories(stats) {
  if (!stats) return []
  const excludeKeys = ['totalEmails', 'timestamp', 'mate_id', 'mateId']
  return Object.entries(stats)
    .filter(([key]) => !excludeKeys.includes(key))
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5) // Show top 5 categories
}
</script>
