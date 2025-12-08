import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import api from '../services/api'

const STORAGE_KEY = 'fleet-navigator-settings'
const VERSION_KEY = 'fleet-navigator-version'

// Current app version from Vite build
const currentVersion = __APP_VERSION__ || '0.0.0'

/**
 * Cache-Busting: Prüft ob die App-Version sich geändert hat.
 * Bei neuer Version wird localStorage gelöscht um alte Cache-Probleme zu vermeiden.
 */
function checkVersionAndClearCacheIfNeeded() {
  const storedVersion = localStorage.getItem(VERSION_KEY)

  if (storedVersion !== currentVersion) {
    console.log(`🔄 Version update detected: ${storedVersion || 'none'} → ${currentVersion}`)
    console.log('🧹 Clearing localStorage to ensure fresh start...')

    // Alle Fleet Navigator relevanten Keys löschen
    const keysToRemove = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key && (key.startsWith('fleet-navigator') || key.startsWith('chaining'))) {
        keysToRemove.push(key)
      }
    }
    keysToRemove.forEach(key => localStorage.removeItem(key))

    // Neue Version speichern
    localStorage.setItem(VERSION_KEY, currentVersion)
    console.log('✅ Cache cleared, new version stored:', currentVersion)

    return true // Cache was cleared
  }

  return false // No change
}

// Run cache check on module load (before store initialization)
const cacheWasCleared = checkVersionAndClearCacheIfNeeded()

// Default settings
const defaultSettings = {
  // General
  language: 'de',
  theme: 'auto',
  uiTheme: 'default',  // UI Theme: 'default' (dark/tech) or 'lawyer' (light/professional)
  fontSize: 'medium',  // Font size: 'small', 'medium', 'large', 'xlarge'
  sidebarCollapsed: false,  // Sidebar collapsed state
  showWelcomeTiles: false,  // Show suggestion tiles - loaded from DB (default: false until DB confirms)
  showTopBar: true,  // TopBar ein-/ausblenden

  // Model Settings
  markdownEnabled: true,  // Markdown-Formatierung in Antworten
  streamingEnabled: true,
  temperature: 0.7,
  topP: 0.9,
  topK: 40,
  repeatPenalty: 1.18,   // Erhöht auf 1.18 um Wiederholungen zu vermeiden (Llama-optimiert)
  contextLength: 32768,  // Erhöht für lange Ausgaben
  maxTokens: 32768,      // Erhöht für 15+ Seiten Text (~40k tokens)

  // Vision
  autoSelectVisionModel: true,
  preferredVisionModel: 'llava:7b',  // Default: llava:7b (schnell und effizient)
  visionChainEnabled: true,  // Vision Model Output an Haupt-Model weiterreichen

  // Hardware/Performance
  cpuOnly: false,  // Deaktiviert CUDA/GPU (num_gpu=0) - für Demos auf Laptops ohne NVIDIA

  // Advanced
  debugMode: false
}

export const useSettingsStore = defineStore('settings', () => {
  // Load settings from localStorage or use defaults
  const loadSettings = () => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY)
      if (stored) {
        const storedSettings = JSON.parse(stored)
        // Merge: defaults first, then stored values (but ensure all new defaults are present)
        // This ensures new settings get their defaults even if not in localStorage
        const merged = { ...defaultSettings, ...storedSettings }

        // Load chaining settings from separate localStorage key and override merged settings
        try {
          const chainingSettingsStr = localStorage.getItem('chainingSettings')
          if (chainingSettingsStr) {
            const chainingSettings = JSON.parse(chainingSettingsStr)
            merged.visionChainEnabled = chainingSettings.enabled
            merged.preferredVisionModel = chainingSettings.visionModel
            console.log('🔗 Loaded chaining settings:', chainingSettings)
          }
        } catch (e) {
          console.warn('Failed to load chaining settings, using defaults', e)
        }

        // Log if we're using stored settings
        console.log('✅ Settings loaded from localStorage')
        console.log('📊 maxTokens:', merged.maxTokens, 'temperature:', merged.temperature)

        return merged
      }
    } catch (e) {
      console.error('Failed to load settings from localStorage', e)
    }
    console.log('📝 Using default settings')
    return { ...defaultSettings }
  }

  const settings = ref(loadSettings())

  // Watch for changes and save to localStorage
  watch(settings, (newSettings) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(newSettings))
    } catch (e) {
      console.error('Failed to save settings to localStorage', e)
    }
  }, { deep: true })

  // Actions
  function updateSettings(newSettings) {
    settings.value = { ...settings.value, ...newSettings }
  }

  function resetToDefaults() {
    settings.value = { ...defaultSettings }
  }

  function getSetting(key) {
    return settings.value[key]
  }

  function setSetting(key, value) {
    settings.value[key] = value
  }

  // Vision Model helpers
  function getVisionModels() {
    return [
      'llava:7b',
      'llava:13b',
      'llava-llama3:latest',
      'minicpm-v:latest',
      'moondream:latest',
      'bakllava:latest'
    ]
  }

  function isVisionModel(modelName) {
    if (!modelName) return false
    const visionKeywords = ['llava', 'vision', 'bakllava', 'moondream', 'minicpm', 'cogvlm']
    return visionKeywords.some(keyword => modelName.toLowerCase().includes(keyword))
  }

  function toggleSidebar() {
    settings.value.sidebarCollapsed = !settings.value.sidebarCollapsed
  }

  // Load showWelcomeTiles from backend database (Source of Truth)
  // This MUST override localStorage value as DB is the source of truth
  async function loadShowWelcomeTilesFromBackend() {
    try {
      const response = await fetch('/api/settings/show-welcome-tiles')
      if (response.ok) {
        const value = await response.json()
        settings.value.showWelcomeTiles = value
        // Force localStorage update to match DB
        const stored = localStorage.getItem(STORAGE_KEY)
        if (stored) {
          const storedSettings = JSON.parse(stored)
          storedSettings.showWelcomeTiles = value
          localStorage.setItem(STORAGE_KEY, JSON.stringify(storedSettings))
        }
        console.log('🔄 showWelcomeTiles loaded from backend:', value)
      }
    } catch (e) {
      console.warn('⚠️ Could not load showWelcomeTiles from backend:', e.message)
    }
  }

  // Load showTopBar from backend database (Source of Truth)
  async function loadShowTopBarFromBackend() {
    try {
      const response = await fetch('/api/settings/show-top-bar')
      if (response.ok) {
        const value = await response.json()
        settings.value.showTopBar = value
        // Force localStorage update to match DB
        const stored = localStorage.getItem(STORAGE_KEY)
        if (stored) {
          const storedSettings = JSON.parse(stored)
          storedSettings.showTopBar = value
          localStorage.setItem(STORAGE_KEY, JSON.stringify(storedSettings))
        }
        console.log('🔄 showTopBar loaded from backend:', value)
      }
    } catch (e) {
      console.warn('⚠️ Could not load showTopBar from backend:', e.message)
    }
  }

  // Load uiTheme from backend database (Source of Truth)
  async function loadUiThemeFromBackend() {
    try {
      const response = await fetch('/api/settings/ui-theme')
      if (response.ok) {
        const value = await response.text()
        // Handle quoted strings or plain text
        const theme = value.replace(/"/g, '').trim() || 'tech-dark'
        settings.value.uiTheme = theme
        // Force localStorage update to match DB
        const stored = localStorage.getItem(STORAGE_KEY)
        if (stored) {
          const storedSettings = JSON.parse(stored)
          storedSettings.uiTheme = theme
          localStorage.setItem(STORAGE_KEY, JSON.stringify(storedSettings))
        }
        console.log('🔄 uiTheme loaded from backend:', theme)
      }
    } catch (e) {
      console.warn('⚠️ Could not load uiTheme from backend:', e.message)
    }
  }

  // Save uiTheme to backend database
  async function saveUiThemeToBackend(theme) {
    try {
      await fetch('/api/settings/ui-theme', {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain' },
        body: theme
      })
      console.log('✅ uiTheme saved to backend:', theme)
    } catch (e) {
      console.warn('⚠️ Could not save uiTheme to backend:', e.message)
    }
  }

  // Save showTopBar to backend database
  async function saveShowTopBarToBackend(show) {
    try {
      await fetch('/api/settings/show-top-bar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(show)
      })
      console.log('✅ showTopBar saved to backend:', show)
    } catch (e) {
      console.warn('⚠️ Could not save showTopBar to backend:', e.message)
    }
  }

  // Sync vision settings with backend (H2 Database = Source of Truth)
  async function syncVisionSettingsWithBackend() {
    try {
      const backendSettings = await api.getModelSelectionSettings()
      if (backendSettings) {
        // Update local settings with backend values
        if (backendSettings.visionModel) {
          settings.value.preferredVisionModel = backendSettings.visionModel
          console.log('🔄 Vision Model synced from backend:', backendSettings.visionModel)
        }
        if (backendSettings.visionChainingEnabled !== undefined) {
          settings.value.visionChainEnabled = backendSettings.visionChainingEnabled
          console.log('🔄 Vision Chaining synced from backend:', backendSettings.visionChainingEnabled)
        }

        // Also update localStorage chainingSettings to keep in sync
        const chainingSettings = {
          enabled: backendSettings.visionChainingEnabled,
          visionModel: backendSettings.visionModel,
          showIntermediateOutput: false
        }
        localStorage.setItem('chainingSettings', JSON.stringify(chainingSettings))
        console.log('✅ Vision settings synced with backend')
      }
    } catch (e) {
      console.warn('⚠️ Could not sync vision settings with backend:', e.message)
    }
  }

  // Initialize: load settings from backend
  loadShowWelcomeTilesFromBackend()
  loadShowTopBarFromBackend()
  loadUiThemeFromBackend()

  return {
    settings,
    updateSettings,
    resetToDefaults,
    getSetting,
    setSetting,
    getVisionModels,
    isVisionModel,
    toggleSidebar,
    syncVisionSettingsWithBackend,
    loadShowWelcomeTilesFromBackend,
    loadShowTopBarFromBackend,
    saveShowTopBarToBackend,
    loadUiThemeFromBackend,
    saveUiThemeToBackend
  }
})
