<template>
  <div class="fleet-officers-dashboard min-h-screen p-6 bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 overflow-y-auto">
    <!-- Header -->
    <div class="mb-8">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <div class="p-3 rounded-xl bg-gradient-to-br from-fleet-orange-500 to-orange-600 shadow-lg">
            <ServerIcon class="w-8 h-8 text-white" />
          </div>
          <div>
            <h1 class="text-3xl font-bold text-white">Fleet Officers</h1>
            <p class="text-gray-400 mt-1">Verwalte und überwache deine Remote-Agents</p>
          </div>
        </div>

      </div>

      <div class="flex items-center justify-between mt-4">
        <!-- Add Officer Button -->
        <button
          @click="showAddOfficerModal = true"
          class="px-4 py-2 rounded-lg bg-gradient-to-r from-fleet-orange-500 to-orange-600
                 hover:from-fleet-orange-400 hover:to-orange-500
                 text-white font-medium text-sm
                 transition-all transform hover:scale-105 active:scale-95
                 flex items-center gap-2 shadow-lg"
        >
          <PlusIcon class="w-5 h-5" />
          Officer hinzufügen
        </button>

        <!-- Summary Cards -->
        <div class="flex gap-4">
          <div class="bg-gradient-to-br from-gray-800/50 to-gray-900/50 backdrop-blur-sm px-6 py-3 rounded-xl border border-gray-700/50">
            <div class="text-xs text-gray-400 mb-1">Gesamt</div>
            <div class="text-2xl font-bold text-white">{{ officers.length }}</div>
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

    <!-- No Officers State -->
    <div v-if="officers.length === 0" class="
      bg-gradient-to-br from-gray-800/30 to-gray-900/30
      backdrop-blur-sm
      p-12 rounded-2xl
      border border-gray-700/30 border-dashed
      text-center
    ">
      <ServerIcon class="w-20 h-20 text-gray-600 mx-auto mb-4" />
      <h3 class="text-xl font-semibold text-gray-400 mb-2">Keine Fleet Officers verbunden</h3>
      <p class="text-sm text-gray-500 max-w-md mx-auto">
        Starte einen Fleet Officer auf deinem Remote-System, um Hardware-Daten zu sammeln und Commands auszuführen.
      </p>
      <div class="mt-6 p-4 bg-gray-800/50 rounded-lg border border-gray-700/50 inline-block text-left">
        <code class="text-xs text-gray-400">
          cd Fleet-Officer-Linux<br />
          ./fleet-officer
        </code>
      </div>
    </div>

    <!-- Officer Tiles Grid -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      <div
        v-for="officer in officers"
        :key="officer.officerId"
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
        <!-- Officer Header -->
        <div class="flex items-start justify-between mb-4">
          <div class="flex items-center gap-3">
            <div class="
              p-2 rounded-lg
              bg-gradient-to-br from-fleet-orange-500 to-orange-600
              group-hover:scale-110 transition-transform duration-300
            ">
              <ServerIcon class="w-5 h-5 text-white" />
            </div>
            <div>
              <h3 class="font-bold text-white text-lg">{{ officer.name }}</h3>
            </div>
          </div>
          <span
            class="px-2 py-1 rounded-full text-xs font-semibold flex items-center gap-1"
            :class="officer.status === 'ONLINE'
              ? 'bg-green-500/20 text-green-400 border border-green-500/30'
              : 'bg-red-500/20 text-red-400 border border-red-500/30'"
          >
            <component :is="officer.status === 'ONLINE' ? CheckCircleIcon : XCircleIcon" class="w-3 h-3" />
            {{ officer.status }}
          </span>
        </div>

        <!-- Officer Info -->
        <div class="space-y-2 mb-4">
          <div class="text-xs text-gray-400">{{ officer.description }}</div>
          <div class="flex items-center gap-2 text-xs text-gray-500">
            <ComputerDesktopIcon class="w-4 h-4" />
            <span>{{ officer.officerId }}</span>
          </div>
        </div>

        <!-- Quick Stats -->
        <div v-if="officerStats[officer.officerId] && officer.status === 'ONLINE'" class="space-y-3">
          <!-- OS Info -->
          <div class="flex items-center gap-2 text-sm">
            <div class="p-1.5 rounded bg-blue-500/20">
              <CommandLineIcon class="w-4 h-4 text-blue-400" />
            </div>
            <span class="text-gray-300">
              {{ formatOS(officerStats[officer.officerId].system) }}
            </span>
          </div>

          <!-- CPU Usage -->
          <div>
            <div class="flex items-center justify-between mb-1">
              <span class="text-xs text-gray-400">CPU</span>
              <span class="text-xs font-semibold" :class="getCPUColor(officerStats[officer.officerId].cpu?.usage_percent)">
                {{ officerStats[officer.officerId].cpu?.usage_percent?.toFixed(1) }}%
              </span>
            </div>
            <div class="h-2 bg-gray-700 rounded-full overflow-hidden">
              <div
                class="h-full transition-all duration-300"
                :class="getCPUBarColor(officerStats[officer.officerId].cpu?.usage_percent)"
                :style="{ width: officerStats[officer.officerId].cpu?.usage_percent + '%' }"
              />
            </div>
          </div>

          <!-- RAM Usage -->
          <div>
            <div class="flex items-center justify-between mb-1">
              <span class="text-xs text-gray-400">RAM</span>
              <span class="text-xs font-semibold" :class="getRAMColor(officerStats[officer.officerId].memory?.used_percent)">
                {{ officerStats[officer.officerId].memory?.used_percent?.toFixed(1) }}%
              </span>
            </div>
            <div class="h-2 bg-gray-700 rounded-full overflow-hidden">
              <div
                class="h-full transition-all duration-300"
                :class="getRAMBarColor(officerStats[officer.officerId].memory?.used_percent)"
                :style="{ width: officerStats[officer.officerId].memory?.used_percent + '%' }"
              />
            </div>
          </div>

          <!-- Temperature -->
          <div v-if="officerStats[officer.officerId].temperature?.cpu_package" class="flex items-center justify-between">
            <span class="text-xs text-gray-400">Temperatur</span>
            <span
              class="text-xs font-semibold px-2 py-0.5 rounded-full"
              :class="getTempBadgeColor(officerStats[officer.officerId].temperature?.cpu_package)"
            >
              {{ officerStats[officer.officerId].temperature?.cpu_package?.toFixed(0) }}°C
            </span>
          </div>
        </div>

        <!-- Offline State -->
        <div v-else-if="officer.status === 'OFFLINE'" class="text-center py-4">
          <XCircleIcon class="w-8 h-8 text-red-400/50 mx-auto mb-2" />
          <p class="text-xs text-gray-500">Officer ist offline</p>
        </div>

        <!-- Loading State -->
        <div v-else class="text-center py-4">
          <ArrowPathIcon class="w-6 h-6 text-gray-500 mx-auto mb-2 animate-spin" />
          <p class="text-xs text-gray-500">Lade Daten...</p>
        </div>

        <!-- Action Buttons -->
        <div class="mt-4 pt-4 border-t border-gray-700/50 flex items-center gap-2">
          <button
            @click.stop="openOfficerDetail(officer)"
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
            @click.stop="openOfficerInNewTab(officer)"
            class="
              py-2 px-3 rounded-lg
              bg-fleet-orange-500/20 hover:bg-fleet-orange-500/30
              text-xs text-fleet-orange-400 hover:text-fleet-orange-300
              border border-fleet-orange-500/30 hover:border-fleet-orange-500/50
              transition-all duration-200
              flex items-center gap-1
            "
            title="In neuem Tab öffnen"
          >
            <PlusIcon class="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- Refresh Button -->
    <div v-if="officers.length > 0" class="mt-8 flex justify-center">
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

    <!-- Officer Detail Modal -->
    <OfficerDetailModal
      v-if="selectedOfficer"
      :officer="selectedOfficer"
      :stats="officerStats[selectedOfficer.officerId]"
      @close="closeOfficerDetail"
    />

    <!-- Add Officer Modal -->
    <AddOfficerModal
      :show="showAddOfficerModal"
      @close="showAddOfficerModal = false"
      @officer-added="handleOfficerAdded"
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
  PlusIcon
} from '@heroicons/vue/24/outline'
import OfficerDetailModal from '../../components/OfficerDetailModal.vue'
import AddOfficerModal from '../../components/AddOfficerModal.vue'
import axios from 'axios'

const officers = ref([])
const officerStats = ref({})
const isRefreshing = ref(false)
const selectedOfficer = ref(null)
const showAddOfficerModal = ref(false)
let intervalId = null

const onlineCount = computed(() => officers.value.filter(o => o.status === 'ONLINE').length)
const offlineCount = computed(() => officers.value.filter(o => o.status === 'OFFLINE').length)

onMounted(async () => {
  await loadOfficers()
  await loadAllStats()
  // Auto-refresh every 5 seconds
  intervalId = setInterval(async () => {
    await loadOfficers()
    await loadAllStats()
  }, 5000)
})

onUnmounted(() => {
  if (intervalId) clearInterval(intervalId)
})

async function loadOfficers() {
  try {
    const response = await axios.get('/api/fleet-officer/officers')
    officers.value = response.data
  } catch (error) {
    console.error('Failed to load officers:', error)
  }
}

async function loadAllStats() {
  for (const officer of officers.value) {
    if (officer.status === 'ONLINE') {
      try {
        const response = await axios.get(`/api/fleet-officer/officers/${officer.officerId}/stats`)
        officerStats.value[officer.officerId] = response.data
      } catch (error) {
        console.error(`Failed to load stats for ${officer.officerId}:`, error)
      }
    }
  }
}

async function refreshAllData() {
  isRefreshing.value = true
  try {
    await loadOfficers()
    await loadAllStats()
  } finally {
    setTimeout(() => {
      isRefreshing.value = false
    }, 500)
  }
}

function openOfficerDetail(officer) {
  selectedOfficer.value = officer
}

function openOfficerInNewTab(officer) {
  window.open(`/agents/fleet-officers/${officer.officerId}`, '_blank')
}

function closeOfficerDetail() {
  selectedOfficer.value = null
}

async function handleOfficerAdded() {
  // Reload officers list after new officer was added
  await loadOfficers()
  await loadAllStats()
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
</script>
