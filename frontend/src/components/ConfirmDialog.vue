<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="isOpen"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4"
        @click.self="handleCancel"
      >
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>

        <!-- Dialog -->
        <div
          class="
            relative z-10
            bg-white dark:bg-gray-800
            rounded-2xl shadow-2xl
            w-full max-w-md
            border border-gray-200 dark:border-gray-700
            transform transition-all duration-200
          "
        >
          <!-- Header -->
          <div class="p-6 pb-4">
            <div class="flex items-start gap-4">
              <!-- Icon -->
              <div
                class="flex-shrink-0 p-3 rounded-full"
                :class="iconClasses"
              >
                <component :is="iconComponent" class="w-6 h-6" />
              </div>

              <!-- Title & Message -->
              <div class="flex-1 min-w-0">
                <h3 class="text-lg font-bold text-gray-900 dark:text-white mb-1">
                  {{ title }}
                </h3>
                <p class="text-sm text-gray-600 dark:text-gray-400 whitespace-pre-line">
                  {{ message }}
                </p>
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="px-6 py-4 bg-gray-50 dark:bg-gray-900/50 rounded-b-2xl flex justify-end gap-3">
            <button
              @click="handleCancel"
              class="
                px-4 py-2 rounded-xl
                text-gray-700 dark:text-gray-300
                bg-white dark:bg-gray-800
                border border-gray-300 dark:border-gray-600
                hover:bg-gray-50 dark:hover:bg-gray-700
                font-medium text-sm
                transition-all duration-200
              "
            >
              {{ cancelText }}
            </button>
            <button
              @click="handleConfirm"
              class="
                px-4 py-2 rounded-xl
                font-medium text-sm
                transition-all duration-200
              "
              :class="confirmButtonClasses"
            >
              {{ confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'
import {
  ExclamationTriangleIcon,
  TrashIcon,
  QuestionMarkCircleIcon,
  InformationCircleIcon
} from '@heroicons/vue/24/outline'

const props = defineProps({
  isOpen: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: 'Bestätigung'
  },
  message: {
    type: String,
    default: 'Sind Sie sicher?'
  },
  type: {
    type: String,
    default: 'warning', // 'warning', 'danger', 'info', 'question'
    validator: (v) => ['warning', 'danger', 'info', 'question'].includes(v)
  },
  confirmText: {
    type: String,
    default: 'Bestätigen'
  },
  cancelText: {
    type: String,
    default: 'Abbrechen'
  }
})

const emit = defineEmits(['confirm', 'cancel'])

const iconComponent = computed(() => {
  switch (props.type) {
    case 'danger': return TrashIcon
    case 'info': return InformationCircleIcon
    case 'question': return QuestionMarkCircleIcon
    default: return ExclamationTriangleIcon
  }
})

const iconClasses = computed(() => {
  switch (props.type) {
    case 'danger':
      return 'bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400'
    case 'info':
      return 'bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400'
    case 'question':
      return 'bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400'
    default:
      return 'bg-amber-100 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400'
  }
})

const confirmButtonClasses = computed(() => {
  switch (props.type) {
    case 'danger':
      return 'bg-red-500 hover:bg-red-600 text-white'
    case 'info':
      return 'bg-blue-500 hover:bg-blue-600 text-white'
    case 'question':
      return 'bg-purple-500 hover:bg-purple-600 text-white'
    default:
      return 'bg-amber-500 hover:bg-amber-600 text-white'
  }
})

function handleConfirm() {
  emit('confirm')
}

function handleCancel() {
  emit('cancel')
}
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active > div:last-child,
.modal-leave-active > div:last-child {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.modal-enter-from > div:last-child,
.modal-leave-to > div:last-child {
  transform: scale(0.95);
  opacity: 0;
}
</style>
