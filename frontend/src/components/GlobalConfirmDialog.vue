<template>
  <ConfirmDialog
    :isOpen="isOpen"
    :title="dialogConfig.title"
    :message="dialogConfig.message"
    :type="dialogConfig.type"
    :confirmText="dialogConfig.confirmText"
    :cancelText="dialogConfig.cancelText"
    @confirm="handleConfirm"
    @cancel="handleCancel"
  />
</template>

<script setup>
import ConfirmDialog from './ConfirmDialog.vue'
import { useConfirmDialogState } from '../composables/useConfirmDialog'

const { isOpen, dialogConfig, handleConfirm, handleCancel } = useConfirmDialogState()
</script>
