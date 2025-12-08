<template>
  <div class="relative">
    <button
      @click="showDropdown = !showDropdown"
      class="
        flex items-center space-x-2 px-3 py-2
        bg-gradient-to-br from-amber-100 to-amber-50
        dark:from-amber-900/30 dark:to-amber-800/20
        rounded-lg border border-amber-200 dark:border-amber-700/50
        shadow-sm
        hover:from-amber-200 hover:to-amber-100
        dark:hover:from-amber-800/40 dark:hover:to-amber-700/30
        hover:border-amber-300 dark:hover:border-amber-600/50
        hover:shadow-md
        transition-all duration-200
        cursor-pointer
        transform hover:scale-105 active:scale-95
      "
      title="Erscheinungsbild wechseln"
    >
      <SunIcon v-if="darkMode" class="w-4 h-4 text-amber-600 dark:text-amber-400" />
      <MoonIcon v-else class="w-4 h-4 text-amber-600 dark:text-amber-400" />
      <span class="text-sm font-medium text-amber-900 dark:text-amber-100">
        {{ currentThemeLabel }}
      </span>
      <ChevronDownIcon class="w-3 h-3 text-amber-500 dark:text-amber-400" />
    </button>

    <!-- Dropdown Menu -->
    <Transition name="dropdown">
      <div
        v-if="showDropdown"
        @click.stop
        class="absolute right-0 mt-2 w-56 bg-white dark:bg-gray-800 rounded-lg shadow-xl border border-gray-200 dark:border-gray-700 z-[9999]"
      >
        <div class="p-2">
          <div class="text-xs font-semibold text-gray-500 dark:text-gray-400 px-3 py-1 uppercase">Tech</div>

          <!-- Tech Dark - Cyberpunk Theme mit Cyan Glow -->
          <button
            @click="setTheme('tech-dark')"
            class="
              w-full flex items-center justify-between px-3 py-2 rounded-lg
              hover:bg-gray-100 dark:hover:bg-gray-700
              cursor-pointer transition-colors
            "
            :class="{ 'bg-cyan-50 dark:bg-cyan-900/20': uiTheme === 'tech-dark' || uiTheme === 'default' || !uiTheme }"
          >
            <div class="flex items-center gap-2">
              <!-- Farbvorschau: Cyan Glow -->
              <div class="w-5 h-5 rounded-full bg-gradient-to-br from-[#00D9FF] to-[#00FF88] border border-[#0A0A0F]" style="box-shadow: 0 0 8px rgba(0, 217, 255, 0.6);"></div>
              <span class="text-sm font-medium text-gray-900 dark:text-white">Tech Dark</span>
            </div>
            <CheckIcon
              v-if="uiTheme === 'tech-dark' || uiTheme === 'default' || !uiTheme"
              class="w-4 h-4 text-cyan-500"
            />
          </button>

          <!-- Tech Hell - Kommt noch -->
          <button
            @click="setTheme('tech-light')"
            class="
              w-full flex items-center justify-between px-3 py-2 rounded-lg
              hover:bg-gray-100 dark:hover:bg-gray-700
              cursor-pointer transition-colors
            "
            :class="{ 'bg-cyan-50 dark:bg-cyan-900/20': uiTheme === 'tech-light' }"
          >
            <div class="flex items-center gap-2">
              <div class="w-5 h-5 rounded-full bg-gradient-to-br from-[#E0F7FF] to-[#FFFFFF] border border-[#00D9FF]" style="box-shadow: 0 0 6px rgba(0, 217, 255, 0.4);"></div>
              <span class="text-sm font-medium text-gray-900 dark:text-white">Tech Hell</span>
            </div>
            <CheckIcon
              v-if="uiTheme === 'tech-light'"
              class="w-4 h-4 text-cyan-500"
            />
          </button>

          <div class="border-t border-gray-200 dark:border-gray-700 my-2"></div>
          <div class="text-xs font-semibold text-gray-500 dark:text-gray-400 px-3 py-1 uppercase">Crazy</div>

          <!-- Crazy Hell -->
          <button
            @click="setTheme('crazy-light')"
            class="
              w-full flex items-center justify-between px-3 py-2 rounded-lg
              hover:bg-gray-100 dark:hover:bg-gray-700
              cursor-pointer transition-colors
            "
            :class="{ 'bg-pink-50 dark:bg-pink-900/20': uiTheme === 'crazy-light' }"
          >
            <div class="flex items-center gap-2">
              <!-- Farbvorschau: Neon Pink + Violett -->
              <div class="w-5 h-5 rounded-full bg-gradient-to-br from-[#FF0D57] to-[#6A0dad] border border-[#ffcccb]"></div>
              <span class="text-sm font-medium text-gray-900 dark:text-white">Crazy Hell</span>
            </div>
            <CheckIcon
              v-if="uiTheme === 'crazy-light'"
              class="w-4 h-4 text-pink-600 dark:text-pink-400"
            />
          </button>

          <!-- Crazy Dunkel -->
          <button
            @click="setTheme('crazy-dark')"
            class="
              w-full flex items-center justify-between px-3 py-2 rounded-lg
              hover:bg-gray-100 dark:hover:bg-gray-700
              cursor-pointer transition-colors
            "
            :class="{ 'bg-purple-50 dark:bg-purple-900/20': uiTheme === 'crazy-dark' }"
          >
            <div class="flex items-center gap-2">
              <!-- Farbvorschau: Dunkel Violett + Neon Pink -->
              <div class="w-5 h-5 rounded-full bg-gradient-to-br from-[#813c8a] to-[#FF0D57] border border-[#6A0dad]"></div>
              <span class="text-sm font-medium text-gray-900 dark:text-white">Crazy Dunkel</span>
            </div>
            <CheckIcon
              v-if="uiTheme === 'crazy-dark'"
              class="w-4 h-4 text-purple-600 dark:text-purple-400"
            />
          </button>

          <div class="border-t border-gray-200 dark:border-gray-700 my-2"></div>
          <div class="text-xs font-semibold text-gray-500 dark:text-gray-400 px-3 py-1 uppercase">Anwalt</div>

          <!-- Anwalt Hell -->
          <button
            @click="setTheme('lawyer-light')"
            class="
              w-full flex items-center justify-between px-3 py-2 rounded-lg
              hover:bg-gray-100 dark:hover:bg-gray-700
              cursor-pointer transition-colors
            "
            :class="{ 'bg-blue-50 dark:bg-blue-900/20': uiTheme === 'lawyer-light' }"
          >
            <div class="flex items-center gap-2">
              <div class="w-5 h-5 rounded-full bg-gradient-to-br from-gray-100 to-white border-2 border-gray-800"></div>
              <span class="text-sm font-medium text-gray-900 dark:text-white">Anwalt Hell</span>
            </div>
            <CheckIcon
              v-if="uiTheme === 'lawyer-light'"
              class="w-4 h-4 text-blue-600 dark:text-blue-400"
            />
          </button>

          <!-- Anwalt Dunkel -->
          <button
            @click="setTheme('lawyer-dark')"
            class="
              w-full flex items-center justify-between px-3 py-2 rounded-lg
              hover:bg-gray-100 dark:hover:bg-gray-700
              cursor-pointer transition-colors
            "
            :class="{ 'bg-gray-100 dark:bg-gray-700': uiTheme === 'lawyer-dark' }"
          >
            <div class="flex items-center gap-2">
              <div class="w-5 h-5 rounded-full bg-gradient-to-br from-gray-900 to-black border border-blue-500"></div>
              <span class="text-sm font-medium text-gray-900 dark:text-white">Anwalt Dunkel</span>
            </div>
            <CheckIcon
              v-if="uiTheme === 'lawyer-dark'"
              class="w-4 h-4 text-gray-600 dark:text-gray-400"
            />
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { SunIcon, MoonIcon, ChevronDownIcon, CheckIcon } from '@heroicons/vue/24/outline'
import { useSettingsStore } from '../../stores/settingsStore'

const props = defineProps({
  darkMode: Boolean
})

const emit = defineEmits(['toggle-theme'])

const settingsStore = useSettingsStore()
const showDropdown = ref(false)

// Computed
const uiTheme = computed(() => settingsStore.settings.uiTheme)

const currentThemeLabel = computed(() => {
  if (uiTheme.value === 'tech-dark' || uiTheme.value === 'default' || !uiTheme.value) return 'Tech Dark'
  if (uiTheme.value === 'tech-light') return 'Tech Hell'
  if (uiTheme.value === 'crazy-light') return 'Crazy Hell'
  if (uiTheme.value === 'crazy-dark') return 'Crazy Dunkel'
  if (uiTheme.value === 'lawyer-light') return 'Anwalt Hell'
  if (uiTheme.value === 'lawyer-dark') return 'Anwalt Dunkel'
  return 'Tech Dark'
})

// Actions
function setTheme(theme) {
  settingsStore.settings.uiTheme = theme
  // Speichere in Datenbank für Persistenz über Neustarts
  settingsStore.saveUiThemeToBackend(theme)
  showDropdown.value = false
}

function setDarkMode(isDark) {
  if (props.darkMode !== isDark) {
    emit('toggle-theme')
  }
  showDropdown.value = false
}

// Close dropdown on outside click
function handleClickOutside(event) {
  if (showDropdown.value && !event.target.closest('.relative')) {
    showDropdown.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
