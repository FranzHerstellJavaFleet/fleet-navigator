import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/main.css'

const VERSION_STORAGE_KEY = 'fleet-navigator-version'

/**
 * Check if backend version changed and clear cache if needed
 * Fixes "old frontend in browser cache" problem
 */
async function checkVersionAndClearCache() {
  try {
    const response = await fetch('/api/system/version')
    if (!response.ok) {
      console.warn('⚠️ Could not fetch version info')
      return
    }

    const versionInfo = await response.json()
    const currentVersion = versionInfo.version
    const storedVersion = localStorage.getItem(VERSION_STORAGE_KEY)

    console.log(`🔄 Version check: stored=${storedVersion}, current=${currentVersion}`)

    if (storedVersion && storedVersion !== currentVersion) {
      console.log('🧹 Version changed! Clearing caches and reloading...')

      // Clear all localStorage except important user data
      const keysToKeep = ['fleet-navigator-chat-cache', 'fleet-navigator-selected-model']
      const savedData = {}
      keysToKeep.forEach(key => {
        const value = localStorage.getItem(key)
        if (value) savedData[key] = value
      })

      // Clear localStorage
      localStorage.clear()

      // Restore important data
      Object.entries(savedData).forEach(([key, value]) => {
        localStorage.setItem(key, value)
      })

      // Save new version
      localStorage.setItem(VERSION_STORAGE_KEY, currentVersion)

      // Clear browser caches if available
      if ('caches' in window) {
        const cacheNames = await caches.keys()
        await Promise.all(cacheNames.map(name => caches.delete(name)))
        console.log('🧹 Browser caches cleared')
      }

      // Force reload from server (bypass cache)
      window.location.reload(true)
      return false // Don't continue mounting app
    }

    // First time or same version - just save it
    localStorage.setItem(VERSION_STORAGE_KEY, currentVersion)
    console.log('✅ Version check passed:', currentVersion)
    return true

  } catch (error) {
    console.warn('⚠️ Version check failed:', error.message)
    return true // Continue anyway
  }
}

// Check version before mounting app
checkVersionAndClearCache().then(shouldMount => {
  if (shouldMount !== false) {
    const app = createApp(App)
    app.use(createPinia())
    app.use(router)
    app.mount('#app')
  }
})
