<template>
  <div class="model-card border border-gray-200 dark:border-gray-700 rounded-lg p-4 hover:shadow-lg transition-shadow bg-white dark:bg-gray-800">
    <!-- Header -->
    <div class="flex justify-between items-start mb-3">
      <h4 class="font-semibold text-gray-900 dark:text-white text-lg">
        {{ model.displayName }}
      </h4>
      <span class="text-yellow-500 text-sm">
        ⭐ {{ model.rating }}
      </span>
    </div>

    <!-- Provider & Size -->
    <div class="flex justify-between text-sm text-gray-600 dark:text-gray-400 mb-3">
      <span>{{ model.provider }}</span>
      <span>📦 {{ model.sizeHuman }}</span>
    </div>

    <!-- Description -->
    <p class="text-sm text-gray-700 dark:text-gray-300 mb-3 line-clamp-3">
      {{ model.description }}
    </p>

    <!-- Languages -->
    <div class="mb-3">
      <div class="text-xs text-gray-500 dark:text-gray-400 mb-1">Sprachen:</div>
      <div class="flex flex-wrap gap-1">
        <span
          v-for="(lang, idx) in model.languages.slice(0, 3)"
          :key="idx"
          class="text-xs bg-blue-100 dark:bg-blue-900 text-blue-700 dark:text-blue-300 px-2 py-1 rounded"
        >
          {{ lang }}
        </span>
        <span v-if="model.languages.length > 3" class="text-xs text-gray-500 dark:text-gray-400">
          +{{ model.languages.length - 3 }}
        </span>
      </div>
    </div>

    <!-- Use Cases -->
    <div class="mb-3">
      <div class="text-xs text-gray-500 dark:text-gray-400 mb-1">Einsatzgebiete:</div>
      <div class="flex flex-wrap gap-1">
        <span
          v-for="(useCase, idx) in model.useCases.slice(0, 3)"
          :key="idx"
          class="text-xs bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 px-2 py-1 rounded"
        >
          {{ useCase }}
        </span>
      </div>
    </div>

    <!-- Requirements -->
    <div class="mb-4 flex items-center gap-3 text-xs text-gray-600 dark:text-gray-400">
      <span>💾 Min {{ model.minRamGB }} GB RAM</span>
      <span v-if="model.gpuAccelSupported" class="text-green-600 dark:text-green-400">
        🎮 GPU
      </span>
    </div>

    <!-- Actions -->
    <div class="mt-4">
      <!-- Already Downloaded -->
      <div v-if="downloaded && !downloading" class="space-y-2">
        <button
          @click="$emit('select', model.filename)"
          class="w-full px-4 py-2 bg-gradient-to-r from-purple-500 to-indigo-500 hover:from-purple-600 hover:to-indigo-600 text-white rounded-lg font-medium shadow-sm hover:shadow-md transition-all transform hover:scale-105 active:scale-95 flex items-center justify-center gap-2"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
          <span>Auswählen</span>
        </button>
        <button
          disabled
          class="w-full px-4 py-2 bg-green-500 text-white rounded-lg font-medium cursor-not-allowed"
        >
          ✓ Installiert
        </button>
        <button
          @click="$emit('delete', model.id)"
          class="w-full px-4 py-2 bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-300 rounded-lg font-medium hover:bg-red-200 dark:hover:bg-red-800"
        >
          🗑️ Löschen
        </button>
      </div>

      <!-- Downloading -->
      <div v-else-if="downloading" class="space-y-2">
        <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-3">
          <div
            class="bg-blue-600 dark:bg-blue-500 h-3 rounded-full transition-all"
            :style="{ width: progress?.percentComplete + '%' }"
          ></div>
        </div>
        <div class="flex justify-between items-center text-sm">
          <span class="text-gray-600 dark:text-gray-400">
            {{ progress?.percentComplete }}% - {{ progress?.speedMBps?.toFixed(1) }} MB/s
          </span>
          <button
            @click="$emit('cancel', model.id)"
            class="px-3 py-1 bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-300 rounded hover:bg-red-200 dark:hover:bg-red-800"
          >
            ✕ Abbrechen
          </button>
        </div>
      </div>

      <!-- Download Button -->
      <button
        v-else
        @click="$emit('download', model.id)"
        :disabled="disabled"
        :class="[
          'w-full px-4 py-2 rounded-lg font-medium transition-colors',
          disabled
            ? 'bg-gray-300 dark:bg-gray-700 text-gray-500 dark:text-gray-400 cursor-not-allowed'
            : 'bg-blue-600 text-white hover:bg-blue-700'
        ]"
      >
        {{ disabled ? '⏳ Download läuft...' : '⬇️ Herunterladen' }}
      </button>
    </div>

    <!-- Badges -->
    <div v-if="model.featured || model.trending" class="mt-3 flex gap-2">
      <span v-if="model.featured" class="text-xs bg-purple-100 dark:bg-purple-900 text-purple-700 dark:text-purple-300 px-2 py-1 rounded">
        ⭐ Empfohlen
      </span>
      <span v-if="model.trending" class="text-xs bg-orange-100 dark:bg-orange-900 text-orange-700 dark:text-orange-300 px-2 py-1 rounded">
        🔥 Trending
      </span>
    </div>

    <!-- Metadata Section -->
    <div v-if="model.releaseDate || model.trainedUntil || model.contextWindow"
         class="mt-3 pt-3 border-t border-gray-200 dark:border-gray-700 space-y-2">
      <div class="text-xs font-semibold text-gray-700 dark:text-gray-300 mb-2">
        📊 Modell-Informationen
      </div>

      <div v-if="model.releaseDate" class="flex justify-between text-xs text-gray-600 dark:text-gray-400">
        <span>📅 Release:</span>
        <span class="font-medium">{{ model.releaseDate }}</span>
      </div>

      <div v-if="model.trainedUntil" class="flex justify-between text-xs text-gray-600 dark:text-gray-400">
        <span>📚 Trainiert bis:</span>
        <span class="font-medium">{{ model.trainedUntil }}</span>
      </div>

      <div v-if="model.contextWindow" class="flex justify-between text-xs text-gray-600 dark:text-gray-400">
        <span>💬 Context:</span>
        <span class="font-medium">{{ model.contextWindow }}</span>
      </div>

      <div v-if="model.primaryTasks" class="text-xs text-gray-600 dark:text-gray-400 mt-2">
        <div class="font-medium mb-1">🎯 Aufgaben:</div>
        <div class="text-gray-500 dark:text-gray-500">{{ model.primaryTasks }}</div>
      </div>

      <div v-if="model.strengths" class="text-xs text-gray-600 dark:text-gray-400 mt-2">
        <div class="font-medium mb-1 text-green-600 dark:text-green-400">✓ Stärken:</div>
        <div class="text-gray-500 dark:text-gray-500">{{ model.strengths }}</div>
      </div>

      <div v-if="model.limitations" class="text-xs text-gray-600 dark:text-gray-400 mt-2">
        <div class="font-medium mb-1 text-orange-600 dark:text-orange-400">⚠️ Limitierungen:</div>
        <div class="text-gray-500 dark:text-gray-500">{{ model.limitations }}</div>
      </div>
    </div>

    <!-- Stats -->
    <div class="mt-3 pt-3 border-t border-gray-200 dark:border-gray-700 text-xs text-gray-500 dark:text-gray-400">
      <div class="flex justify-between">
        <span>⬇️ {{ formatDownloads(model.downloads) }} Downloads</span>
        <span>{{ model.license }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  model: {
    type: Object,
    required: true
  },
  downloaded: {
    type: Boolean,
    default: false
  },
  downloading: {
    type: Boolean,
    default: false
  },
  progress: {
    type: Object,
    default: null
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

defineEmits(['download', 'cancel', 'delete', 'select'])

function formatDownloads(count) {
  if (count >= 1000) {
    return Math.floor(count / 1000) + 'k'
  }
  return count
}
</script>

<style scoped>
.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
