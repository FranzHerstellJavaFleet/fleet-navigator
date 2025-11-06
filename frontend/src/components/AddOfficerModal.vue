<template>
  <Transition name="modal">
    <div v-if="show" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-gray-800 rounded-2xl shadow-2xl max-w-md w-full border border-gray-700/50 transform transition-all">
        <!-- Header -->
        <div class="flex items-center justify-between p-6 border-b border-gray-700/50">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-lg bg-gradient-to-br from-fleet-orange-500 to-orange-600 flex items-center justify-center">
              <ServerIcon class="w-6 h-6 text-white" />
            </div>
            <h3 class="text-xl font-bold text-white">Officer hinzufügen</h3>
          </div>
          <button
            @click="$emit('close')"
            class="text-gray-400 hover:text-white transition-colors"
          >
            <XMarkIcon class="w-6 h-6" />
          </button>
        </div>

        <!-- Body -->
        <div class="p-6 space-y-4">
          <p class="text-sm text-gray-400">
            Gib die IP-Adresse oder den Domain-Namen des Remote-Officers ein. Der Officer muss bereits gestartet sein.
          </p>

          <!-- IP/Domain Input -->
          <div>
            <label class="text-sm font-medium text-gray-300 block mb-2">
              IP-Adresse oder Domain
            </label>
            <input
              v-model="formData.host"
              type="text"
              placeholder="z.B. 192.168.1.100 oder cubitruck.local"
              class="w-full px-4 py-3 bg-gray-700 text-white rounded-lg border border-gray-600
                     focus:border-fleet-orange-500 focus:outline-none focus:ring-2 focus:ring-fleet-orange-500/20
                     placeholder-gray-500"
              @keyup.enter="addOfficer"
            />
          </div>

          <!-- Port Input (Optional) -->
          <div>
            <label class="text-sm font-medium text-gray-300 block mb-2">
              WebSocket Port (optional)
            </label>
            <input
              v-model="formData.port"
              type="number"
              placeholder="Standard: 2025"
              class="w-full px-4 py-3 bg-gray-700 text-white rounded-lg border border-gray-600
                     focus:border-fleet-orange-500 focus:outline-none focus:ring-2 focus:ring-fleet-orange-500/20
                     placeholder-gray-500"
              @keyup.enter="addOfficer"
            />
          </div>

          <!-- Error Message -->
          <div v-if="errorMessage" class="flex items-center gap-2 p-3 bg-red-500/10 border border-red-500/20 rounded-lg">
            <XCircleIcon class="w-5 h-5 text-red-400 flex-shrink-0" />
            <span class="text-sm text-red-400">{{ errorMessage }}</span>
          </div>

          <!-- Success Message -->
          <div v-if="successMessage" class="flex items-center gap-2 p-3 bg-green-500/10 border border-green-500/20 rounded-lg">
            <CheckCircleIcon class="w-5 h-5 text-green-400 flex-shrink-0" />
            <span class="text-sm text-green-400">{{ successMessage }}</span>
          </div>

          <!-- Connecting State -->
          <div v-if="connecting" class="flex items-center gap-2 p-3 bg-blue-500/10 border border-blue-500/20 rounded-lg">
            <ArrowPathIcon class="w-5 h-5 text-blue-400 animate-spin flex-shrink-0" />
            <span class="text-sm text-blue-400">Verbinde mit Officer...</span>
          </div>
        </div>

        <!-- Footer -->
        <div class="flex gap-3 p-6 border-t border-gray-700/50">
          <button
            @click="$emit('close')"
            class="flex-1 px-4 py-2 rounded-lg bg-gray-700 hover:bg-gray-600 text-white font-medium
                   transition-colors"
          >
            Abbrechen
          </button>
          <button
            @click="addOfficer"
            :disabled="!formData.host || connecting"
            class="flex-1 px-4 py-2 rounded-lg bg-gradient-to-r from-fleet-orange-500 to-orange-600
                   hover:from-fleet-orange-400 hover:to-orange-500
                   text-white font-medium
                   disabled:opacity-50 disabled:cursor-not-allowed
                   transition-all transform hover:scale-105 active:scale-95
                   flex items-center justify-center gap-2"
          >
            <ServerIcon class="w-4 h-4" />
            Officer verbinden
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref } from 'vue'
import { ServerIcon, XMarkIcon, XCircleIcon, CheckCircleIcon, ArrowPathIcon } from '@heroicons/vue/24/outline'
import axios from 'axios'

const props = defineProps({
  show: Boolean
})

const emit = defineEmits(['close', 'officer-added'])

const formData = ref({
  host: '',
  port: 2025
})

const connecting = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

async function addOfficer() {
  if (!formData.value.host) {
    errorMessage.value = 'Bitte IP-Adresse oder Domain eingeben'
    return
  }

  connecting.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const response = await axios.post('/api/fleet-officer/connect-remote', {
      host: formData.value.host,
      port: formData.value.port || 2025
    })

    successMessage.value = `Officer erfolgreich verbunden: ${response.data.officerId}`

    setTimeout(() => {
      emit('officer-added')
      emit('close')
      resetForm()
    }, 1500)

  } catch (err) {
    console.error('Failed to connect officer:', err)
    errorMessage.value = err.response?.data?.error || 'Verbindung fehlgeschlagen. Stelle sicher, dass der Officer läuft.'
  } finally {
    connecting.value = false
  }
}

function resetForm() {
  formData.value = {
    host: '',
    port: 2025
  }
  errorMessage.value = ''
  successMessage.value = ''
}
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active > div,
.modal-leave-active > div {
  transition: transform 0.3s ease;
}

.modal-enter-from > div,
.modal-leave-to > div {
  transform: scale(0.9);
}
</style>
