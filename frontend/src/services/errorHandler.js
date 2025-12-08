/**
 * Error Handler Service für Fleet Navigator
 * Verarbeitet strukturierte API-Fehlerantworten und zeigt benutzerfreundliche Meldungen.
 */

import { useToast } from '../composables/useToast'

// Error Codes mit deutschen Übersetzungen
const ERROR_MESSAGES = {
  // Ollama
  'OLLAMA_CONNECTION_FAILED': 'Ollama-Server ist nicht erreichbar',

  // Models
  'MODEL_NOT_FOUND': 'Modell nicht gefunden',
  'MODEL_LOAD_FAILED': 'Modell konnte nicht geladen werden',

  // Experts & Chats
  'EXPERT_NOT_FOUND': 'Experte nicht gefunden',
  'CHAT_NOT_FOUND': 'Chat nicht gefunden',

  // Providers
  'PROVIDER_NOT_AVAILABLE': 'KI-Provider nicht verfügbar',

  // HuggingFace
  'HUGGINGFACE_ERROR': 'HuggingFace-Fehler',

  // Files
  'FILE_UPLOAD_ERROR': 'Datei-Upload fehlgeschlagen',
  'FILE_TOO_LARGE': 'Datei ist zu groß',

  // Validation
  'VALIDATION_ERROR': 'Ungültige Eingabe',
  'INVALID_ARGUMENT': 'Ungültiger Parameter',

  // Connection
  'CONNECTION_FAILED': 'Verbindungsfehler',

  // Generic
  'INTERNAL_ERROR': 'Interner Fehler'
}

/**
 * Extrahiert strukturierte Fehlerinformationen aus einer Axios-Error-Response
 * @param {Error} error - Axios Error Objekt
 * @returns {Object} Strukturierte Fehlerinformation
 */
export function parseApiError(error) {
  // Kein Netzwerk/Server-Antwort
  if (!error.response) {
    return {
      message: 'Keine Verbindung zum Server. Bitte prüfen Sie Ihre Netzwerkverbindung.',
      errorCode: 'NETWORK_ERROR',
      status: 0,
      suggestions: [
        'Prüfen Sie, ob Fleet Navigator läuft',
        'Prüfen Sie Ihre Internetverbindung'
      ]
    }
  }

  const { data, status } = error.response

  // Strukturierte Fehlerantwort vom Backend
  if (data && data.errorCode) {
    return {
      message: data.message || ERROR_MESSAGES[data.errorCode] || 'Ein Fehler ist aufgetreten',
      details: data.details,
      errorCode: data.errorCode,
      status: data.status || status,
      path: data.path,
      timestamp: data.timestamp,
      suggestions: data.suggestions || []
    }
  }

  // Standard HTTP Fehler ohne strukturierte Antwort
  return {
    message: getHttpErrorMessage(status),
    errorCode: `HTTP_${status}`,
    status: status,
    suggestions: []
  }
}

/**
 * Gibt eine deutsche Fehlermeldung für HTTP-Statuscodes zurück
 */
function getHttpErrorMessage(status) {
  switch (status) {
    case 400: return 'Ungültige Anfrage'
    case 401: return 'Nicht angemeldet. Bitte melden Sie sich erneut an.'
    case 403: return 'Zugriff verweigert'
    case 404: return 'Ressource nicht gefunden'
    case 408: return 'Zeitüberschreitung bei der Anfrage'
    case 409: return 'Konflikt mit bestehenden Daten'
    case 413: return 'Datei ist zu groß'
    case 429: return 'Zu viele Anfragen. Bitte warten Sie einen Moment.'
    case 500: return 'Interner Serverfehler'
    case 502: return 'Bad Gateway - Server nicht erreichbar'
    case 503: return 'Dienst vorübergehend nicht verfügbar'
    case 504: return 'Gateway-Timeout'
    default: return `HTTP-Fehler ${status}`
  }
}

/**
 * Zeigt einen Toast mit der Fehlermeldung an
 * @param {Error} error - Axios Error Objekt
 * @param {Object} options - Toast-Optionen
 */
export function showErrorToast(error, options = {}) {
  const toast = useToast()
  const parsed = parseApiError(error)

  const duration = options.timeout || 8000

  // Kurze Nachricht für den Toast
  let toastMessage = parsed.message

  // Bei Vorschlägen: Ersten Vorschlag anhängen
  if (parsed.suggestions && parsed.suggestions.length > 0) {
    toastMessage += ` - Tipp: ${parsed.suggestions[0]}`
  }

  toast.error(toastMessage, duration)

  // Für Debugging: Details in Konsole
  if (import.meta.env.DEV) {
    console.group('API Error')
    console.error('Error Code:', parsed.errorCode)
    console.error('Message:', parsed.message)
    if (parsed.details) console.error('Details:', parsed.details)
    if (parsed.path) console.error('Path:', parsed.path)
    if (parsed.suggestions?.length > 0) console.info('Suggestions:', parsed.suggestions)
    console.groupEnd()
  }

  return parsed
}

/**
 * Wrapper für API-Aufrufe mit automatischer Fehlerbehandlung
 * @param {Function} apiCall - Async-Funktion die den API-Aufruf macht
 * @param {Object} options - Optionen (showToast, toastOptions)
 * @returns {Promise<{data: any, error: Object|null}>}
 */
export async function safeApiCall(apiCall, options = {}) {
  const { showToast = true, toastOptions = {} } = options

  try {
    const data = await apiCall()
    return { data, error: null }
  } catch (error) {
    const parsed = parseApiError(error)

    if (showToast) {
      showErrorToast(error, toastOptions)
    }

    return { data: null, error: parsed }
  }
}

/**
 * Prüft ob ein Fehler ein bestimmter Fehlertyp ist
 * @param {Error} error - Axios Error
 * @param {string} errorCode - Erwarteter Error Code
 */
export function isErrorType(error, errorCode) {
  if (!error.response?.data) return false
  return error.response.data.errorCode === errorCode
}

/**
 * Prüft ob der Fehler ein Netzwerkfehler ist
 */
export function isNetworkError(error) {
  return !error.response
}

/**
 * Prüft ob der Fehler ein Authentifizierungsfehler ist
 */
export function isAuthError(error) {
  return error.response?.status === 401
}

/**
 * Prüft ob der Fehler ein Provider-/Verbindungsfehler ist
 */
export function isProviderError(error) {
  const errorCode = error.response?.data?.errorCode
  return [
    'OLLAMA_CONNECTION_FAILED',
    'PROVIDER_NOT_AVAILABLE',
    'CONNECTION_FAILED'
  ].includes(errorCode)
}

export default {
  parseApiError,
  showErrorToast,
  safeApiCall,
  isErrorType,
  isNetworkError,
  isAuthError,
  isProviderError
}
