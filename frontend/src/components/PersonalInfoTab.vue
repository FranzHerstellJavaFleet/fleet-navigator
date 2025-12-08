<template>
  <div class="space-y-6">
    <!-- Header with Description -->
    <div class="p-4 rounded-xl bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700/50">
      <div class="flex items-start gap-2">
        <InformationCircleIcon class="w-5 h-5 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" />
        <div class="text-xs text-blue-800 dark:text-blue-200">
          <p class="font-semibold mb-1">Persönliche Daten für Dokumente</p>
          <p>Diese Informationen werden automatisch in generierte Briefe und Dokumente eingefügt (z.B. Kündigungen, Bewerbungen). Sie müssen nur einmal gespeichert werden.</p>
        </div>
      </div>
    </div>

    <!-- Personal Data Form -->
    <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm">
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
        <UserIcon class="w-5 h-5 text-purple-500" />
        Persönliche Angaben
      </h3>

      <div class="space-y-4">
        <!-- Title -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Titel (optional)
          </label>
          <input
            v-model="personalInfo.title"
            type="text"
            placeholder="Dr., Prof., etc."
            class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-purple-500 focus:border-transparent"
          >
        </div>

        <!-- First Name -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Vorname
          </label>
          <input
            v-model="personalInfo.firstName"
            type="text"
            placeholder="Max"
            class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-purple-500 focus:border-transparent"
          >
        </div>

        <!-- Last Name -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Nachname
          </label>
          <input
            v-model="personalInfo.lastName"
            type="text"
            placeholder="Mustermann"
            class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-purple-500 focus:border-transparent"
          >
        </div>

        <!-- Date of Birth -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Geburtsdatum (optional)
          </label>
          <input
            v-model="personalInfo.dateOfBirth"
            type="date"
            class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-purple-500 focus:border-transparent"
          >
        </div>
      </div>
    </section>

    <!-- Address -->
    <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm">
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
        <HomeIcon class="w-5 h-5 text-green-500" />
        Adresse
      </h3>

      <div class="space-y-4">
        <!-- Street and House Number -->
        <div class="grid grid-cols-3 gap-4">
          <div class="col-span-2">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Straße
            </label>
            <input
              v-model="personalInfo.street"
              type="text"
              placeholder="Musterstraße"
              class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-green-500 focus:border-transparent"
            >
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Nr.
            </label>
            <input
              v-model="personalInfo.houseNumber"
              type="text"
              placeholder="123"
              class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-green-500 focus:border-transparent"
            >
          </div>
        </div>

        <!-- Postal Code and City -->
        <div class="grid grid-cols-3 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              PLZ
            </label>
            <input
              v-model="personalInfo.postalCode"
              type="text"
              placeholder="12345"
              class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-green-500 focus:border-transparent"
            >
          </div>
          <div class="col-span-2">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Stadt
            </label>
            <input
              v-model="personalInfo.city"
              type="text"
              placeholder="Musterstadt"
              class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-green-500 focus:border-transparent"
            >
          </div>
        </div>

        <!-- Country -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Land
          </label>
          <input
            v-model="personalInfo.country"
            type="text"
            placeholder="Deutschland"
            class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-green-500 focus:border-transparent"
          >
        </div>
      </div>
    </section>

    <!-- Contact Information -->
    <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm">
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
        <PhoneIcon class="w-5 h-5 text-blue-500" />
        Kontakt
      </h3>

      <div class="space-y-4">
        <!-- Phone -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Telefon
          </label>
          <input
            v-model="personalInfo.phone"
            type="tel"
            placeholder="+49 123 456789"
            class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
        </div>

        <!-- Mobile -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Mobil (optional)
          </label>
          <input
            v-model="personalInfo.mobile"
            type="tel"
            placeholder="+49 171 123456"
            class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
        </div>

        <!-- Email -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            E-Mail
          </label>
          <input
            v-model="personalInfo.email"
            type="email"
            placeholder="max@mustermann.de"
            class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
        </div>
      </div>
    </section>

    <!-- Professional Information -->
    <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm">
      <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
        <BriefcaseIcon class="w-5 h-5 text-orange-500" />
        Berufliche Angaben (optional)
      </h3>

      <div class="space-y-4">
        <!-- Company -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Firma
          </label>
          <input
            v-model="personalInfo.company"
            type="text"
            placeholder="Musterfirma GmbH"
            class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-orange-500 focus:border-transparent"
          >
        </div>

        <!-- Position -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Position
          </label>
          <input
            v-model="personalInfo.position"
            type="text"
            placeholder="Geschäftsführer"
            class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-orange-500 focus:border-transparent"
          >
        </div>
      </div>
    </section>

    <!-- Action Buttons -->
    <div class="flex justify-between items-center pt-4">
      <button
        @click="deleteInfo"
        v-if="hasData"
        class="px-4 py-2 rounded-xl text-red-600 dark:text-red-400 hover:text-red-700 dark:hover:text-red-300 hover:bg-red-50 dark:hover:bg-red-900/20 transition-all flex items-center gap-2 border border-red-200 dark:border-red-800"
      >
        <TrashIcon class="w-4 h-4" />
        Daten löschen
      </button>
      <div v-else></div>

      <div class="text-xs text-gray-500 dark:text-gray-400">
        {{ hasData ? 'Daten vorhanden ✓' : 'Noch keine Daten gespeichert' }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  UserIcon,
  HomeIcon,
  PhoneIcon,
  BriefcaseIcon,
  InformationCircleIcon,
  TrashIcon
} from '@heroicons/vue/24/outline'
import api from '../services/api'
import { useToast } from '../composables/useToast'
import { useConfirmDialog } from '../composables/useConfirmDialog'

const { success, error: errorToast } = useToast()
const { confirm } = useConfirmDialog()

// Personal info object
const personalInfo = ref({
  title: '',
  firstName: '',
  lastName: '',
  dateOfBirth: '',
  street: '',
  houseNumber: '',
  postalCode: '',
  city: '',
  country: 'Deutschland',
  phone: '',
  mobile: '',
  email: '',
  company: '',
  position: ''
})

// Expose save method to parent
defineExpose({
  save: savePersonalInfo,
  personalInfo
})

// Check if data exists
const hasData = computed(() => {
  return personalInfo.value.firstName ||
         personalInfo.value.lastName ||
         personalInfo.value.email ||
         personalInfo.value.phone
})

// Load personal info on mount
onMounted(async () => {
  await loadPersonalInfo()
})

async function loadPersonalInfo() {
  try {
    const data = await api.getPersonalInfo()
    if (data && data.id) {
      personalInfo.value = {
        title: data.title || '',
        firstName: data.firstName || '',
        lastName: data.lastName || '',
        dateOfBirth: data.dateOfBirth || '',
        street: data.street || '',
        houseNumber: data.houseNumber || '',
        postalCode: data.postalCode || '',
        city: data.city || '',
        country: data.country || 'Deutschland',
        phone: data.phone || '',
        mobile: data.mobile || '',
        email: data.email || '',
        company: data.company || '',
        position: data.position || ''
      }
    }
  } catch (error) {
    console.error('Failed to load personal info:', error)
  }
}

async function savePersonalInfo() {
  try {
    await api.savePersonalInfo(personalInfo.value)
    success('Persönliche Daten gespeichert')
  } catch (error) {
    console.error('Failed to save personal info:', error)
    errorToast('Fehler beim Speichern der persönlichen Daten')
    throw error
  }
}

async function deleteInfo() {
  const confirmed = await confirm({
    title: 'Persönliche Daten löschen?',
    message: 'Möchten Sie wirklich alle persönlichen Daten löschen?',
    type: 'danger',
    confirmText: 'Alles löschen'
  })
  if (confirmed) {
    try {
      await api.deletePersonalInfo()
      // Reset form
      personalInfo.value = {
        title: '',
        firstName: '',
        lastName: '',
        dateOfBirth: '',
        street: '',
        houseNumber: '',
        postalCode: '',
        city: '',
        country: 'Deutschland',
        phone: '',
        mobile: '',
        email: '',
        company: '',
        position: ''
      }
      success('Persönliche Daten gelöscht')
    } catch (error) {
      console.error('Failed to delete personal info:', error)
      errorToast('Fehler beim Löschen der persönlichen Daten')
    }
  }
}
</script>
