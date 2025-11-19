import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

const STORAGE_KEY = 'fleet-navigator-settings'

// Default settings
const defaultSettings = {
  // General
  language: 'de',
  theme: 'auto',
  sidebarCollapsed: false,  // Sidebar collapsed state

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
      'moondream:latest',
      'bakllava:latest'
    ]
  }

  function isVisionModel(modelName) {
    if (!modelName) return false
    const visionKeywords = ['llava', 'vision', 'bakllava', 'moondream']
    return visionKeywords.some(keyword => modelName.toLowerCase().includes(keyword))
  }

  function toggleSidebar() {
    settings.value.sidebarCollapsed = !settings.value.sidebarCollapsed
  }

  return {
    settings,
    updateSettings,
    resetToDefaults,
    getSetting,
    setSetting,
    getVisionModels,
    isVisionModel,
    toggleSidebar
  }
})
