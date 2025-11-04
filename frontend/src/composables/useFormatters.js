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
  const date = new Date(dateString)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return 'Gerade eben'
  if (diff < 3600000) return `vor ${Math.floor(diff / 60000)}min`
  if (diff < 86400000) return `vor ${Math.floor(diff / 3600000)}h`
  return date.toLocaleDateString('de-DE')
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
    formatNumber,
    formatFileSize
  }
}
