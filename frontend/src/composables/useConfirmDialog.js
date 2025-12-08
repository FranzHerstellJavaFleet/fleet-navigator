import { ref, readonly } from 'vue'

// Shared state for the confirm dialog
const isOpen = ref(false)
const dialogConfig = ref({
  title: 'Bestätigung',
  message: 'Sind Sie sicher?',
  type: 'warning',
  confirmText: 'Bestätigen',
  cancelText: 'Abbrechen'
})

let resolvePromise = null

/**
 * Composable for showing confirmation dialogs
 *
 * Usage:
 * const { confirm } = useConfirmDialog()
 *
 * // Simple usage
 * const confirmed = await confirm('Wirklich löschen?')
 *
 * // With options
 * const confirmed = await confirm({
 *   title: 'Chat löschen',
 *   message: 'Diese Aktion kann nicht rückgängig gemacht werden.',
 *   type: 'danger',
 *   confirmText: 'Löschen'
 * })
 */
export function useConfirmDialog() {

  /**
   * Show a confirmation dialog
   * @param {string|Object} options - Message string or config object
   * @returns {Promise<boolean>} - Resolves to true if confirmed, false if cancelled
   */
  function confirm(options) {
    return new Promise((resolve) => {
      resolvePromise = resolve

      if (typeof options === 'string') {
        // Simple string message
        dialogConfig.value = {
          title: 'Bestätigung',
          message: options,
          type: 'warning',
          confirmText: 'Bestätigen',
          cancelText: 'Abbrechen'
        }
      } else {
        // Full config object
        dialogConfig.value = {
          title: options.title || 'Bestätigung',
          message: options.message || 'Sind Sie sicher?',
          type: options.type || 'warning',
          confirmText: options.confirmText || 'Bestätigen',
          cancelText: options.cancelText || 'Abbrechen'
        }
      }

      isOpen.value = true
    })
  }

  /**
   * Shortcut for delete confirmation
   */
  function confirmDelete(itemName, additionalMessage = '') {
    return confirm({
      title: `"${itemName}" löschen?`,
      message: additionalMessage || 'Diese Aktion kann nicht rückgängig gemacht werden.',
      type: 'danger',
      confirmText: 'Löschen',
      cancelText: 'Abbrechen'
    })
  }

  /**
   * Handle confirm action (called by ConfirmDialog component)
   */
  function handleConfirm() {
    isOpen.value = false
    if (resolvePromise) {
      resolvePromise(true)
      resolvePromise = null
    }
  }

  /**
   * Handle cancel action (called by ConfirmDialog component)
   */
  function handleCancel() {
    isOpen.value = false
    if (resolvePromise) {
      resolvePromise(false)
      resolvePromise = null
    }
  }

  return {
    // State (readonly for external use)
    isOpen: readonly(isOpen),
    dialogConfig: readonly(dialogConfig),

    // Methods
    confirm,
    confirmDelete,
    handleConfirm,
    handleCancel
  }
}

// Export singleton state for the dialog component
export function useConfirmDialogState() {
  return {
    isOpen,
    dialogConfig,
    handleConfirm() {
      isOpen.value = false
      if (resolvePromise) {
        resolvePromise(true)
        resolvePromise = null
      }
    },
    handleCancel() {
      isOpen.value = false
      if (resolvePromise) {
        resolvePromise(false)
        resolvePromise = null
      }
    }
  }
}
