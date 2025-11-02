<template>
  <div class="fixed bottom-4 right-4 z-[100] flex flex-col gap-2 pointer-events-none">
    <TransitionGroup
      name="toast"
      tag="div"
      class="flex flex-col gap-2"
    >
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="pointer-events-auto transform transition-all duration-300"
        @click="removeToast(toast.id)"
      >
        <div
          class="
            flex items-center gap-3
            min-w-[320px] max-w-md
            px-4 py-3
            rounded-xl shadow-xl
            backdrop-blur-xl
            border
            cursor-pointer
            hover:scale-105
            active:scale-95
            transition-transform duration-200
          "
          :class="toastClasses(toast.type)"
        >
          <!-- Icon -->
          <div class="flex-shrink-0">
            <CheckCircleIcon v-if="toast.type === 'success'" class="w-5 h-5" />
            <XCircleIcon v-else-if="toast.type === 'error'" class="w-5 h-5" />
            <ExclamationTriangleIcon v-else-if="toast.type === 'warning'" class="w-5 h-5" />
            <InformationCircleIcon v-else class="w-5 h-5" />
          </div>

          <!-- Message -->
          <div class="flex-1 text-sm font-medium">
            {{ toast.message }}
          </div>

          <!-- Close Button -->
          <button
            @click.stop="removeToast(toast.id)"
            class="flex-shrink-0 opacity-70 hover:opacity-100 transition-opacity"
          >
            <XMarkIcon class="w-4 h-4" />
          </button>
        </div>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup>
import {
  CheckCircleIcon,
  XCircleIcon,
  ExclamationTriangleIcon,
  InformationCircleIcon,
  XMarkIcon
} from '@heroicons/vue/24/outline'
import { useToast } from '../composables/useToast'

const { toasts, removeToast } = useToast()

const toastClasses = (type) => {
  switch (type) {
    case 'success':
      return 'bg-green-500/90 text-white border-green-400'
    case 'error':
      return 'bg-red-500/90 text-white border-red-400'
    case 'warning':
      return 'bg-yellow-500/90 text-white border-yellow-400'
    case 'info':
      return 'bg-blue-500/90 text-white border-blue-400'
    default:
      return 'bg-gray-800/90 text-white border-gray-700'
  }
}
</script>

<style scoped>
.toast-enter-active {
  animation: toast-in 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.toast-leave-active {
  animation: toast-out 0.2s ease-in;
}

@keyframes toast-in {
  from {
    opacity: 0;
    transform: translateX(100%) scale(0.8);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}

@keyframes toast-out {
  from {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateX(100%) scale(0.8);
  }
}
</style>
