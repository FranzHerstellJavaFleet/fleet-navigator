/**
 * Zentrale Model-Utility-Funktionen
 *
 * Konsolidiert:
 * - Vision Model Detection (war in 4 Dateien dupliziert)
 * - Code Model Detection
 * - Model Category Detection
 * - Context Window Lookup
 */

// ============================================================
// VISION MODEL DETECTION
// ============================================================

/**
 * Patterns für Vision-fähige Modelle
 * Zentrale Definition - wird überall verwendet
 */
const VISION_PATTERNS = [
  /llava/i,
  /vision/i,
  /bakllava/i,
  /moondream/i,
  /minicpm/i,
  /cogvlm/i,
  /multimodal/i,
  /image/i
]

/**
 * Prüft ob ein Modell Vision-fähig ist (Bildverarbeitung)
 * @param {string} modelName - Name des Modells
 * @returns {boolean} true wenn Vision-fähig
 */
export function isVisionModel(modelName) {
  if (!modelName) return false
  return VISION_PATTERNS.some(pattern => pattern.test(modelName))
}

// ============================================================
// CODE MODEL DETECTION
// ============================================================

/**
 * Patterns für Code-spezialisierte Modelle
 */
const CODE_PATTERNS = [
  /code/i,
  /coder/i,
  /codellama/i,
  /deepseek-coder/i,
  /starcoder/i,
  /wizard-coder/i,
  /qwen.*coder/i
]

/**
 * Prüft ob ein Modell Code-spezialisiert ist
 * @param {string} modelName - Name des Modells
 * @returns {boolean} true wenn Code-spezialisiert
 */
export function isCodeModel(modelName) {
  if (!modelName) return false
  return CODE_PATTERNS.some(pattern => pattern.test(modelName))
}

// ============================================================
// MODEL CATEGORY DETECTION
// ============================================================

/**
 * Ermittelt die Kategorie eines Modells
 * @param {string} modelName - Name des Modells
 * @returns {'vision'|'coder'|'general'} Die Modell-Kategorie
 */
export function getModelCategory(modelName) {
  if (!modelName) return 'general'
  if (isVisionModel(modelName)) return 'vision'
  if (isCodeModel(modelName)) return 'coder'
  return 'general'
}

// ============================================================
// CONTEXT WINDOW LOOKUP
// ============================================================

/**
 * Bekannte Context Windows für häufige Modelle
 * Wird für Token-Limit-Berechnungen verwendet
 */
const KNOWN_CONTEXT_WINDOWS = {
  // Llama Familie
  'llama2:7b': 4096,
  'llama2:13b': 4096,
  'llama2:70b': 4096,
  'llama3:8b': 8192,
  'llama3:70b': 8192,
  'llama3.1:8b': 131072,
  'llama3.1:70b': 131072,
  'llama3.2:3b': 131072,

  // Mistral Familie
  'mistral:7b': 32768,
  'mistral:latest': 32768,
  'mistral-nemo:12b': 131072,
  'mixtral:8x7b': 32768,

  // Qwen Familie
  'qwen:7b': 32768,
  'qwen:14b': 32768,
  'qwen:72b': 32768,
  'qwen2:7b': 131072,
  'qwen2.5:7b': 131072,

  // Vision Modelle
  'llava:7b': 4096,
  'llava:13b': 4096,
  'llava:34b': 4096,
  'bakllava:7b': 4096,

  // Code Modelle
  'codellama:7b': 16384,
  'codellama:13b': 16384,
  'codellama:34b': 16384,
  'deepseek-coder:6.7b': 16384,
  'starcoder:7b': 8192,

  // Deutsche Modelle
  'em_german_leo_mistral': 32768,

  // Phi Familie
  'phi:2.7b': 2048,
  'phi3:mini': 128000,
  'phi3:medium': 128000,

  // Gemma Familie
  'gemma:2b': 8192,
  'gemma:7b': 8192,
  'gemma2:9b': 8192
}

/**
 * Ermittelt das Context Window für ein Modell
 * @param {string} modelName - Name des Modells
 * @returns {number} Context Window Size in Tokens
 */
export function getContextWindow(modelName) {
  if (!modelName) return 4096

  const modelLower = modelName.toLowerCase()

  // Exakter Match
  if (KNOWN_CONTEXT_WINDOWS[modelLower]) {
    return KNOWN_CONTEXT_WINDOWS[modelLower]
  }

  // Partial Match für Varianten (z.B. "llama3:8b-instruct-q4_0")
  for (const [key, value] of Object.entries(KNOWN_CONTEXT_WINDOWS)) {
    if (modelLower.includes(key.split(':')[0])) {
      return value
    }
  }

  // Heuristiken basierend auf Modell-Familie
  if (modelLower.includes('llama3.1') || modelLower.includes('llama-3.1')) return 131072
  if (modelLower.includes('llama3') || modelLower.includes('llama-3')) return 8192
  if (modelLower.includes('llama2') || modelLower.includes('llama-2')) return 4096
  if (modelLower.includes('mistral-nemo')) return 131072
  if (modelLower.includes('mistral')) return 32768
  if (modelLower.includes('mixtral')) return 32768
  if (modelLower.includes('qwen2') || modelLower.includes('qwen-2')) return 131072
  if (modelLower.includes('qwen')) return 32768
  if (modelLower.includes('phi3') || modelLower.includes('phi-3')) return 128000
  if (modelLower.includes('gemma')) return 8192
  if (modelLower.includes('llava') || modelLower.includes('bakllava')) return 4096
  if (modelLower.includes('codellama')) return 16384

  // Default für unbekannte Modelle
  return 4096
}

/**
 * Berechnet die prozentuale Context-Auslastung
 * @param {number} usedTokens - Verwendete Tokens
 * @param {string} modelName - Name des Modells
 * @returns {number} Prozentuale Auslastung (0-100)
 */
export function getContextUsagePercent(usedTokens, modelName) {
  const contextWindow = getContextWindow(modelName)
  return Math.min(100, Math.round((usedTokens / contextWindow) * 100))
}

// ============================================================
// MODEL FILTERING
// ============================================================

/**
 * Filtert Modelle für Dokumentenerstellung (keine Vision/Code)
 * @param {string[]} models - Array von Modellnamen
 * @returns {string[]} Gefilterte Modelle
 */
export function filterDocumentModels(models) {
  if (!Array.isArray(models)) return []
  return models.filter(m => !isVisionModel(m) && !isCodeModel(m))
}

/**
 * Filtert Vision-fähige Modelle
 * @param {string[]} models - Array von Modellnamen
 * @returns {string[]} Vision-Modelle
 */
export function filterVisionModels(models) {
  if (!Array.isArray(models)) return []
  return models.filter(m => isVisionModel(m))
}

/**
 * Filtert Code-spezialisierte Modelle
 * @param {string[]} models - Array von Modellnamen
 * @returns {string[]} Code-Modelle
 */
export function filterCodeModels(models) {
  if (!Array.isArray(models)) return []
  return models.filter(m => isCodeModel(m))
}
