/**
 * useFormatters - Zentrale Formatierungsfunktionen
 * Dedupliziert formatDate, formatFileSize, formatNumber
 */

/**
 * Formatiert ein Datum relativ zur aktuellen Zeit
 * @param {string|Date} dateString - Das zu formatierende Datum
 * @returns {string} Formatiertes Datum (z.B. "vor 5min", "Gerade eben", "01.01.2025")
 */
export function formatDate(dateString) {
  if (!dateString) return ''
  const date = new Date(dateString)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return 'Gerade eben'
  if (diff < 3600000) return `vor ${Math.floor(diff / 60000)}min`
  if (diff < 86400000) return `vor ${Math.floor(diff / 3600000)}h`
  return date.toLocaleDateString('de-DE')
}

/**
 * Formatiert ein Datum absolut mit Zeit (DD.MM.YYYY HH:MM)
 * @param {string|Date} dateString - Das zu formatierende Datum
 * @returns {string} Formatiertes Datum (z.B. "01.01.2025 14:30")
 */
export function formatDateAbsolute(dateString) {
  if (!dateString) return ''
  try {
    const date = new Date(dateString)
    return date.toLocaleDateString('de-DE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch (e) {
    return dateString
  }
}

/**
 * Formatiert ein Datum mit ausgeschriebenem Monat
 * @param {string|Date} dateString - Das zu formatierende Datum
 * @param {boolean} withTime - Optional: Mit Uhrzeit (default: false)
 * @returns {string} Formatiertes Datum (z.B. "1. Januar 2025" oder "1. Januar 2025, 14:30")
 */
export function formatDateLong(dateString, withTime = false) {
  if (!dateString) return ''
  try {
    const date = new Date(dateString)
    const options = {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    }
    if (withTime) {
      options.hour = '2-digit'
      options.minute = '2-digit'
    }
    return date.toLocaleDateString('de-DE', options)
  } catch (e) {
    return dateString
  }
}

/**
 * Formatiert eine Zahl mit K/M Suffix
 * @param {number} num - Die zu formatierende Zahl
 * @returns {string} Formatierte Zahl (z.B. "1.5K", "2.3M")
 */
export function formatNumber(num) {
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K'
  return num.toString()
}

/**
 * Formatiert Dateigröße in KB/MB/GB
 * @param {number} bytes - Die Anzahl der Bytes
 * @returns {string} Formatierte Dateigröße (z.B. "1.50 MB", "500 KB")
 */
export function formatFileSize(bytes) {
  if (bytes >= 1024 * 1024 * 1024) return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
  if (bytes >= 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
  if (bytes >= 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return bytes + ' B'
}

/**
 * Composable für alle Formatierungsfunktionen
 * @returns {Object} Objekt mit allen Formatierungsfunktionen
 */
export function useFormatters() {
  return {
    formatDate,
    formatDateAbsolute,
    formatDateLong,
    formatNumber,
    formatFileSize
  }
}
