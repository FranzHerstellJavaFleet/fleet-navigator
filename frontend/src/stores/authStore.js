import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * Authentication store for user session management
 */
export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref(null)
  const isLoading = ref(false)
  const error = ref(null)
  const isInitialized = ref(false)

  // Getters
  const isAuthenticated = computed(() => !!user.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const username = computed(() => user.value?.username || '')
  const displayName = computed(() => user.value?.displayName || user.value?.username || '')

  /**
   * Check current authentication status
   */
  async function checkAuth() {
    try {
      isLoading.value = true
      const response = await fetch('/api/auth/check', {
        credentials: 'include'
      })

      if (response.ok) {
        const data = await response.json()
        if (data.authenticated) {
          user.value = {
            username: data.username,
            displayName: data.displayName,
            role: data.role
          }
        } else {
          user.value = null
        }
      } else {
        user.value = null
      }
    } catch (e) {
      console.error('Auth check failed:', e)
      user.value = null
    } finally {
      isLoading.value = false
      isInitialized.value = true
    }
  }

  /**
   * Login with username and password
   */
  async function login(username, password) {
    try {
      isLoading.value = true
      error.value = null

      // Spring Security expects form data for login
      const formData = new URLSearchParams()
      formData.append('username', username)
      formData.append('password', password)

      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: formData,
        credentials: 'include'
      })

      if (response.ok) {
        const data = await response.json()
        // Fetch full user info
        await checkAuth()
        return { success: true, message: data.message }
      } else {
        const data = await response.json().catch(() => ({ error: 'Login fehlgeschlagen' }))
        error.value = data.error || 'Login fehlgeschlagen'
        return { success: false, error: error.value }
      }
    } catch (e) {
      error.value = 'Verbindungsfehler: ' + e.message
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Register a new user
   */
  async function register(userData) {
    try {
      isLoading.value = true
      error.value = null

      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(userData),
        credentials: 'include'
      })

      const data = await response.json()

      if (response.ok) {
        return { success: true, message: data.message }
      } else {
        error.value = data.error || 'Registrierung fehlgeschlagen'
        return { success: false, error: error.value }
      }
    } catch (e) {
      error.value = 'Verbindungsfehler: ' + e.message
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Logout
   */
  async function logout() {
    try {
      isLoading.value = true
      await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include'
      })
    } catch (e) {
      console.error('Logout error:', e)
    } finally {
      user.value = null
      isLoading.value = false
    }
  }

  /**
   * Change password
   */
  async function changePassword(oldPassword, newPassword) {
    try {
      isLoading.value = true
      error.value = null

      const response = await fetch('/api/auth/change-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ oldPassword, newPassword }),
        credentials: 'include'
      })

      const data = await response.json()

      if (response.ok) {
        return { success: true, message: data.message }
      } else {
        error.value = data.error || 'Passwortänderung fehlgeschlagen'
        return { success: false, error: error.value }
      }
    } catch (e) {
      error.value = 'Verbindungsfehler: ' + e.message
      return { success: false, error: error.value }
    } finally {
      isLoading.value = false
    }
  }

  return {
    // State
    user,
    isLoading,
    error,
    isInitialized,
    // Getters
    isAuthenticated,
    isAdmin,
    username,
    displayName,
    // Actions
    checkAuth,
    login,
    register,
    logout,
    changePassword
  }
})
