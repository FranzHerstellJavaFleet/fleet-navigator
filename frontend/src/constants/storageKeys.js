/**
 * Zentrale localStorage Keys für Fleet Navigator
 *
 * WICHTIG: Alle localStorage-Zugriffe sollten diese Konstanten verwenden!
 * Dies verhindert Tippfehler und ermöglicht einfaches Refactoring.
 *
 * Naming Convention: fleet-navigator-{bereich}-{detail}
 */

export const STORAGE_KEYS = {
  // Settings
  SETTINGS: 'fleet-navigator-settings',
  VERSION: 'fleet-navigator-version',

  // Chat
  CHAT_CACHE: 'fleet-navigator-chat-cache',
  LAST_PROMPT: 'fleet-navigator-last-prompt',
  SELECTED_MODEL: 'fleet-navigator-selected-model',

  // Model Settings
  CHAINING: 'fleet-navigator-chaining',
  SAMPLING_PARAMS: 'fleet-navigator-sampling-params',

  // UI
  LOCALE: 'fleet-navigator-locale',
  DARK_MODE: 'fleet-navigator-dark-mode',
  SIDEBAR_COLLAPSED: 'fleet-navigator-sidebar-collapsed'
}

/**
 * Liest einen Wert aus localStorage mit Fehlerbehandlung
 * @param {string} key - Der Storage-Key
 * @param {*} defaultValue - Fallback-Wert bei Fehler
 * @returns {*} Der gespeicherte oder default Wert
 */
export function getStorageItem(key, defaultValue = null) {
  try {
    const item = localStorage.getItem(key)
    return item ? JSON.parse(item) : defaultValue
  } catch (e) {
    console.warn(`Failed to read localStorage key "${key}":`, e.message)
    return defaultValue
  }
}

/**
 * Speichert einen Wert in localStorage mit Fehlerbehandlung
 * @param {string} key - Der Storage-Key
 * @param {*} value - Der zu speichernde Wert
 * @returns {boolean} true wenn erfolgreich
 */
export function setStorageItem(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
    return true
  } catch (e) {
    console.error(`Failed to write localStorage key "${key}":`, e.message)
    return false
  }
}

/**
 * Entfernt einen Wert aus localStorage
 * @param {string} key - Der Storage-Key
 */
export function removeStorageItem(key) {
  try {
    localStorage.removeItem(key)
  } catch (e) {
    console.warn(`Failed to remove localStorage key "${key}":`, e.message)
  }
}

/**
 * Löscht alle Fleet Navigator localStorage-Einträge
 * Nützlich für Version-Updates oder Reset
 */
export function clearAllFleetStorage() {
  const keysToRemove = []
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i)
    if (key && key.startsWith('fleet-navigator')) {
      keysToRemove.push(key)
    }
  }
  keysToRemove.forEach(key => localStorage.removeItem(key))
  console.log(`🧹 Cleared ${keysToRemove.length} Fleet Navigator storage entries`)
  return keysToRemove.length
}
