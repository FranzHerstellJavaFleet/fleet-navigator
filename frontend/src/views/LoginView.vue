<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900">
    <div class="max-w-md w-full mx-4">
      <!-- Logo -->
      <div class="text-center mb-8">
        <img
          src="/javafleet-logo.png"
          alt="Fleet Navigator"
          class="w-24 h-24 mx-auto mb-4 rounded-2xl shadow-2xl"
        />
        <h1 class="text-3xl font-bold text-white">Fleet Navigator</h1>
        <p class="text-gray-400 mt-2">Navigieren Sie Ihre KI-Flotte</p>
      </div>

      <!-- Card -->
      <div class="bg-gray-800/80 backdrop-blur-sm rounded-2xl shadow-2xl p-8 border border-gray-700">
        <!-- Tabs -->
        <div class="flex mb-6 bg-gray-700/50 rounded-lg p-1">
          <button
            @click="mode = 'login'"
            :class="[
              'flex-1 py-2 px-4 rounded-md text-sm font-medium transition-all',
              mode === 'login'
                ? 'bg-orange-500 text-white'
                : 'text-gray-400 hover:text-white'
            ]"
          >
            Anmelden
          </button>
          <button
            @click="mode = 'register'"
            :class="[
              'flex-1 py-2 px-4 rounded-md text-sm font-medium transition-all',
              mode === 'register'
                ? 'bg-orange-500 text-white'
                : 'text-gray-400 hover:text-white'
            ]"
          >
            Registrieren
          </button>
        </div>

        <!-- Error Message -->
        <div
          v-if="error"
          class="mb-4 p-3 bg-red-500/20 border border-red-500/50 rounded-lg text-red-400 text-sm"
        >
          {{ error }}
        </div>

        <!-- Success Message -->
        <div
          v-if="success"
          class="mb-4 p-3 bg-green-500/20 border border-green-500/50 rounded-lg text-green-400 text-sm"
        >
          {{ success }}
        </div>

        <!-- Login Form -->
        <form v-if="mode === 'login'" @submit.prevent="handleLogin" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">
              Benutzername
            </label>
            <input
              v-model="loginForm.username"
              type="text"
              required
              autocomplete="username"
              class="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white
                     focus:ring-2 focus:ring-orange-500 focus:border-transparent
                     placeholder-gray-500"
              placeholder="admin"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">
              Passwort
            </label>
            <input
              v-model="loginForm.password"
              type="password"
              required
              autocomplete="current-password"
              class="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white
                     focus:ring-2 focus:ring-orange-500 focus:border-transparent
                     placeholder-gray-500"
              placeholder="********"
            />
          </div>

          <button
            type="submit"
            :disabled="authStore.isLoading"
            class="w-full py-3 px-4 bg-gradient-to-r from-orange-500 to-orange-600
                   text-white font-medium rounded-lg
                   hover:from-orange-600 hover:to-orange-700
                   focus:ring-2 focus:ring-orange-500 focus:ring-offset-2 focus:ring-offset-gray-800
                   disabled:opacity-50 disabled:cursor-not-allowed
                   transition-all"
          >
            <span v-if="authStore.isLoading">Anmelden...</span>
            <span v-else>Anmelden</span>
          </button>
        </form>

        <!-- Register Form -->
        <form v-if="mode === 'register'" @submit.prevent="handleRegister" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">
              Benutzername *
            </label>
            <input
              v-model="registerForm.username"
              type="text"
              required
              minlength="3"
              autocomplete="username"
              class="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white
                     focus:ring-2 focus:ring-orange-500 focus:border-transparent
                     placeholder-gray-500"
              placeholder="maxmustermann"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">
              Anzeigename
            </label>
            <input
              v-model="registerForm.displayName"
              type="text"
              autocomplete="name"
              class="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white
                     focus:ring-2 focus:ring-orange-500 focus:border-transparent
                     placeholder-gray-500"
              placeholder="Max Mustermann"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">
              E-Mail
            </label>
            <input
              v-model="registerForm.email"
              type="email"
              autocomplete="email"
              class="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white
                     focus:ring-2 focus:ring-orange-500 focus:border-transparent
                     placeholder-gray-500"
              placeholder="max@example.com"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">
              Passwort *
            </label>
            <input
              v-model="registerForm.password"
              type="password"
              required
              minlength="4"
              autocomplete="new-password"
              class="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white
                     focus:ring-2 focus:ring-orange-500 focus:border-transparent
                     placeholder-gray-500"
              placeholder="********"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-300 mb-1">
              Passwort wiederholen *
            </label>
            <input
              v-model="registerForm.passwordConfirm"
              type="password"
              required
              autocomplete="new-password"
              class="w-full px-4 py-3 bg-gray-700 border border-gray-600 rounded-lg text-white
                     focus:ring-2 focus:ring-orange-500 focus:border-transparent
                     placeholder-gray-500"
              placeholder="********"
            />
          </div>

          <button
            type="submit"
            :disabled="authStore.isLoading"
            class="w-full py-3 px-4 bg-gradient-to-r from-orange-500 to-orange-600
                   text-white font-medium rounded-lg
                   hover:from-orange-600 hover:to-orange-700
                   focus:ring-2 focus:ring-orange-500 focus:ring-offset-2 focus:ring-offset-gray-800
                   disabled:opacity-50 disabled:cursor-not-allowed
                   transition-all"
          >
            <span v-if="authStore.isLoading">Registrieren...</span>
            <span v-else>Registrieren</span>
          </button>
        </form>

        <!-- Default Credentials Hint -->
        <div v-if="mode === 'login'" class="mt-6 pt-4 border-t border-gray-700">
          <p class="text-xs text-gray-500 text-center">
            Standard-Login: <code class="text-gray-400">admin</code> / <code class="text-gray-400">admin</code>
          </p>
        </div>
      </div>

      <!-- Footer -->
      <p class="text-center text-gray-500 text-sm mt-6">
        &copy; 2025 JavaFleet Systems Consulting
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const authStore = useAuthStore()

const mode = ref('login')
const error = ref('')
const success = ref('')

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  displayName: '',
  email: '',
  password: '',
  passwordConfirm: ''
})

async function handleLogin() {
  error.value = ''
  success.value = ''

  const result = await authStore.login(loginForm.username, loginForm.password)

  if (result.success) {
    router.push('/')
  } else {
    error.value = result.error
  }
}

async function handleRegister() {
  error.value = ''
  success.value = ''

  // Validate passwords match
  if (registerForm.password !== registerForm.passwordConfirm) {
    error.value = 'Passwörter stimmen nicht überein'
    return
  }

  const result = await authStore.register({
    username: registerForm.username,
    displayName: registerForm.displayName || registerForm.username,
    email: registerForm.email || null,
    password: registerForm.password
  })

  if (result.success) {
    success.value = 'Registrierung erfolgreich! Sie können sich jetzt anmelden.'
    mode.value = 'login'
    loginForm.username = registerForm.username
    // Clear register form
    registerForm.username = ''
    registerForm.displayName = ''
    registerForm.email = ''
    registerForm.password = ''
    registerForm.passwordConfirm = ''
  } else {
    error.value = result.error
  }
}
</script>
