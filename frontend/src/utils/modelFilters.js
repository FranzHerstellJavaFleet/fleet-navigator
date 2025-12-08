/**
 * Utility functions for filtering AI models by capability
 */

/**
 * Filter models suitable for document generation (text-only models)
 * Excludes vision models, code-specific models
 *
 * @param {string[]} models - Array of model names
 * @returns {string[]} - Filtered model names suitable for documents
 */
export function filterDocumentModels(models) {
  if (!Array.isArray(models)) return []

  const excludePatterns = [
    /llava/i,        // Vision models
    /vision/i,       // Vision models
    /code/i,         // Code-specific models (codellama, deepseek-coder, etc.)
    /coder/i,        // Coder models
    /codellama/i,    // CodeLlama
    /deepseek-coder/i // DeepSeek Coder
  ]

  return models.filter(model => {
    // Check if model matches any exclude pattern
    const shouldExclude = excludePatterns.some(pattern => pattern.test(model))
    return !shouldExclude
  })
}

/**
 * Filter models suitable for vision tasks
 *
 * @param {string[]} models - Array of model names
 * @returns {string[]} - Filtered model names suitable for vision
 */
export function filterVisionModels(models) {
  if (!Array.isArray(models)) return []

  const includePatterns = [
    /llava/i,
    /vision/i,
    /bakllava/i,
    /moondream/i,
    /minicpm/i,
    /cogvlm/i
  ]

  return models.filter(model => {
    return includePatterns.some(pattern => pattern.test(model))
  })
}

/**
 * Filter models suitable for code generation
 *
 * @param {string[]} models - Array of model names
 * @returns {string[]} - Filtered model names suitable for code
 */
export function filterCodeModels(models) {
  if (!Array.isArray(models)) return []

  const includePatterns = [
    /code/i,
    /coder/i,
    /codellama/i,
    /deepseek-coder/i,
    /starcoder/i,
    /wizard-coder/i
  ]

  return models.filter(model => {
    return includePatterns.some(pattern => pattern.test(model))
  })
}
