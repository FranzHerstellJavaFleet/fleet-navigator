/**
 * Model context window sizes
 * Returns the context window size in tokens for a given model
 */

export const MODEL_CONTEXT_WINDOWS = {
  // Qwen models - 128k
  'qwen2.5-coder:7b': 128000,
  'qwen2.5-coder:14b': 128000,
  'qwen2.5-coder:32b': 128000,
  'qwen2.5:7b': 128000,
  'qwen2.5:14b': 128000,
  'qwen2.5:32b': 128000,

  // DeepSeek models
  'deepseek-coder-v2:16b': 128000,
  'deepseek-coder-v2:236b': 128000,
  'deepseek-coder:6.7b': 16000,
  'deepseek-coder:33b': 16000,

  // Llama models - 128k
  'llama3.2:1b': 128000,
  'llama3.2:3b': 128000,
  'llama3.1:8b': 128000,
  'llama3.1:70b': 128000,
  'llama3.1:405b': 128000,

  // CodeLlama
  'codellama:7b': 16000,
  'codellama:13b': 16000,
  'codellama:34b': 16000,
  'codellama:70b': 100000,

  // Mistral
  'mistral:7b': 32000,
  'mistral-small:22b': 32000,
  'mistral-large:123b': 128000,

  // Mixtral
  'mixtral:8x7b': 32000,
  'mixtral:8x22b': 64000,

  // Gemma
  'gemma:2b': 8000,
  'gemma:7b': 8000,
  'gemma2:9b': 8000,
  'gemma2:27b': 8000,

  // Vision models (smaller context)
  'llava:7b': 4000,
  'llava:13b': 4000,
  'llava:34b': 4000,
  'bakllava:7b': 4000,
  'moondream:latest': 2000,

  // Phi models - 128k
  'phi3:mini': 128000,
  'phi3:medium': 128000,

  // Other models
  'dolphin-mixtral:8x7b': 32000,
  'starling-lm:7b': 8000,
  'solar:10.7b': 4000,
}

/**
 * Get context window size for a model
 * Uses fuzzy matching if exact match not found
 */
export function getModelContextWindow(modelName) {
  if (!modelName) return 8000 // Conservative default

  // Try exact match first
  let contextWindow = MODEL_CONTEXT_WINDOWS[modelName]

  // If not found, try fuzzy match by model family
  if (!contextWindow) {
    const modelLower = modelName.toLowerCase()

    // Check by family name (order matters - most specific first)
    if (modelLower.includes('qwen2.5')) contextWindow = 128000
    else if (modelLower.includes('deepseek-coder-v2')) contextWindow = 128000
    else if (modelLower.includes('deepseek')) contextWindow = 16000
    else if (modelLower.includes('llama3.2') || modelLower.includes('llama3.1')) contextWindow = 128000
    else if (modelLower.includes('llama')) contextWindow = 8000
    else if (modelLower.includes('codellama:70')) contextWindow = 100000
    else if (modelLower.includes('codellama')) contextWindow = 16000
    else if (modelLower.includes('mistral-large')) contextWindow = 128000
    else if (modelLower.includes('mistral')) contextWindow = 32000
    else if (modelLower.includes('mixtral:8x22b')) contextWindow = 64000
    else if (modelLower.includes('mixtral')) contextWindow = 32000
    else if (modelLower.includes('gemma')) contextWindow = 8000
    else if (modelLower.includes('llava') || modelLower.includes('bakllava')) contextWindow = 4000
    else if (modelLower.includes('moondream')) contextWindow = 2000
    else if (modelLower.includes('phi3')) contextWindow = 128000
    else contextWindow = 8000 // Conservative default
  }

  return contextWindow
}

/**
 * Get safe context limit (80% of full window)
 * Leaves 20% for user messages and responses
 */
export function getSafeContextLimit(modelName) {
  return Math.floor(getModelContextWindow(modelName) * 0.8)
}

/**
 * Check if a model can handle the given project context
 */
export function canModelHandleContext(modelName, projectTokens) {
  const safeLimit = getSafeContextLimit(modelName)
  return projectTokens <= safeLimit
}

/**
 * Get reason why model cannot be used with current context
 */
export function getIncompatibilityReason(modelName, projectTokens) {
  const safeLimit = getSafeContextLimit(modelName)
  const fullWindow = getModelContextWindow(modelName)

  return `Context zu groß: ${projectTokens.toLocaleString()} Tokens (Limit: ${safeLimit.toLocaleString()} / ${fullWindow.toLocaleString()}k)`
}
