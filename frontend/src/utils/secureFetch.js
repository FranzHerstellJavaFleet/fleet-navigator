/**
 * Secure Fetch Utility
 *
 * Diese Utility-Funktion sendet automatisch das CSRF-Token mit allen
 * state-changing Requests (POST, PUT, DELETE, PATCH).
 */

/**
 * Liest das CSRF-Token aus dem Cookie
 */
function getCsrfToken() {
  const token = document.cookie
    .split('; ')
    .find(row => row.startsWith('XSRF-TOKEN='))
    ?.split('=')[1]
  return token ? decodeURIComponent(token) : null
}

/**
 * Fetch mit automatischem CSRF-Token
 *
 * @param {string} url - Die URL für den Request
 * @param {RequestInit} options - Fetch-Optionen (method, body, headers, etc.)
 * @returns {Promise<Response>} - Die Fetch-Response
 *
 * @example
 * // DELETE Request
 * await secureFetch(`/api/chat/${chatId}`, { method: 'DELETE' })
 *
 * @example
 * // POST Request mit Body
 * await secureFetch('/api/chat/send', {
 *   method: 'POST',
 *   headers: { 'Content-Type': 'application/json' },
 *   body: JSON.stringify({ message: 'Hello' })
 * })
 */
export function secureFetch(url, options = {}) {
  const csrfToken = getCsrfToken()

  const headers = {
    ...options.headers,
    ...(csrfToken && { 'X-XSRF-TOKEN': csrfToken })
  }

  return fetch(url, {
    ...options,
    headers,
    credentials: 'include' // Wichtig für Session-Cookie
  })
}

/**
 * Convenience-Methoden für gängige HTTP-Verben
 */
export const secureApi = {
  /**
   * GET Request
   */
  async get(url) {
    return secureFetch(url, { method: 'GET' })
  },

  /**
   * POST Request mit JSON Body
   */
  async post(url, body) {
    return secureFetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
  },

  /**
   * PUT Request mit JSON Body
   */
  async put(url, body) {
    return secureFetch(url, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
  },

  /**
   * PATCH Request mit JSON Body
   */
  async patch(url, body) {
    return secureFetch(url, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
  },

  /**
   * DELETE Request
   */
  async delete(url) {
    return secureFetch(url, { method: 'DELETE' })
  },

  /**
   * POST Request mit Plain Text Body
   */
  async postText(url, text) {
    return secureFetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: text
    })
  }
}

export default secureFetch
