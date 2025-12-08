<template>
  <div class="sampling-params bg-white dark:bg-gray-800 p-4 rounded-lg">
    <div class="flex items-center justify-between mb-4">
      <h4 class="text-lg font-semibold text-gray-900 dark:text-white">🎛️ Vollständige LLM Parameter-Kontrolle</h4>
      <button
        @click="showHelp = !showHelp"
        class="px-3 py-1 bg-indigo-500 text-white rounded hover:bg-indigo-600 flex items-center gap-2"
      >
        <span>{{ showHelp ? '✕ Hilfe schließen' : '❓ Hilfe anzeigen' }}</span>
      </button>
    </div>

    <!-- Hilfe Modal -->
    <div v-if="showHelp" class="mb-6 p-4 bg-indigo-50 dark:bg-indigo-900/30 border-2 border-indigo-300 dark:border-indigo-600 rounded-lg">
      <h5 class="font-bold text-lg mb-3 text-indigo-900 dark:text-indigo-200">📚 Parameter-Erklärungen</h5>

      <div class="space-y-4 text-sm">
        <!-- Generierung -->
        <div class="border-l-4 border-blue-500 pl-3">
          <h6 class="font-semibold text-blue-900 dark:text-blue-200 mb-2">🎯 Generierung</h6>
          <p class="text-gray-700 dark:text-gray-300">
            <strong>Max Tokens:</strong> Maximale Anzahl der generierten Tokens (Wörter/Teilwörter).
            Höhere Werte = längere Antworten, aber auch mehr Rechenzeit.
            <br><em>Standard: 512 | Bereich: 50-4096</em>
          </p>
        </div>

        <!-- Sampling -->
        <div class="border-l-4 border-purple-500 pl-3">
          <h6 class="font-semibold text-purple-900 dark:text-purple-200 mb-2">🎲 Sampling (Zufälligkeit)</h6>
          <p class="text-gray-700 dark:text-gray-300 space-y-2">
            <span class="block"><strong>Temperature:</strong> Steuert die Kreativität. Niedrig (0.1) = präzise/deterministisch, Hoch (1.5+) = kreativ/chaotisch.
            <br><em>Standard: 0.7 | Bereich: 0-2</em></span>

            <span class="block"><strong>Top P (Nucleus Sampling):</strong> Wählt aus den wahrscheinlichsten Tokens, deren kumulative Wahrscheinlichkeit P erreicht.
            0.9 = Top 90% der Wahrscheinlichkeitsmasse.
            <br><em>Standard: 0.9 | Bereich: 0-1</em></span>

            <span class="block"><strong>Top K:</strong> Begrenzt die Auswahl auf die K wahrscheinlichsten Tokens. Niedrig = konservativer, Hoch = vielfältiger.
            <br><em>Standard: 40 | Bereich: 1-100</em></span>

            <span class="block"><strong>Min P:</strong> Entfernt Tokens mit Wahrscheinlichkeit unter diesem Schwellenwert (relativ zum wahrscheinlichsten Token).
            <br><em>Standard: 0.05 | Bereich: 0-1</em></span>
          </p>
        </div>

        <!-- Wiederholung -->
        <div class="border-l-4 border-green-500 pl-3">
          <h6 class="font-semibold text-green-900 dark:text-green-200 mb-2">🔁 Wiederholungs-Kontrolle</h6>
          <p class="text-gray-700 dark:text-gray-300 space-y-2">
            <span class="block"><strong>Repeat Penalty:</strong> Bestraft wiederholte Tokens. Höher = weniger Wiederholungen, aber kann Kontext stören.
            <br><em>Standard: 1.1 | Bereich: 1.0-2.0</em></span>

            <span class="block"><strong>Repeat Last N:</strong> Anzahl der letzten Tokens, die für Wiederholungs-Erkennung berücksichtigt werden. 0 = deaktiviert.
            <br><em>Standard: 64 | Bereich: 0-256</em></span>

            <span class="block"><strong>Presence Penalty:</strong> Positiv = bestraft bereits vorhandene Tokens (neue Themen), Negativ = bevorzugt diese.
            <br><em>Standard: 0.0 | Bereich: -2.0 bis +2.0</em></span>

            <span class="block"><strong>Frequency Penalty:</strong> Bestraft Tokens basierend auf ihrer Häufigkeit im bisherigen Text.
            <br><em>Standard: 0.0 | Bereich: -2.0 bis +2.0</em></span>
          </p>
        </div>

        <!-- Mirostat -->
        <div class="border-l-4 border-orange-500 pl-3">
          <h6 class="font-semibold text-orange-900 dark:text-orange-200 mb-2">🎯 Mirostat (Erweitert)</h6>
          <p class="text-gray-700 dark:text-gray-300 space-y-2">
            <span class="block"><strong>Mirostat Mode:</strong> Fortgeschrittenes Sampling-Verfahren zur Perplexity-Kontrolle.
            <br>• 0 = Deaktiviert (Standard)
            <br>• 1 = Mirostat v1 (adaptive Sampling)
            <br>• 2 = Mirostat v2 (verbesserte Version)</span>

            <span class="block"><strong>Mirostat Tau:</strong> Ziel-Perplexity (Text-"Überraschung"). Höher = vielfältiger Text.
            <br><em>Standard: 5.0 | Bereich: 0-10</em></span>

            <span class="block"><strong>Mirostat Eta:</strong> Lernrate für Mirostat-Anpassung. Steuert wie schnell sich das Sampling anpasst.
            <br><em>Standard: 0.1 | Bereich: 0-1</em></span>
          </p>
        </div>

        <!-- Stop & Prompt -->
        <div class="border-l-4 border-gray-500 pl-3">
          <h6 class="font-semibold text-gray-900 dark:text-gray-200 mb-2">🛑 Stop-Sequenzen & Prompts</h6>
          <p class="text-gray-700 dark:text-gray-300 space-y-2">
            <span class="block"><strong>Stop Sequences:</strong> Texte, bei denen die Generierung sofort stoppt. Nützlich für Chat-Formate.
            <br><em>Beispiel: "\\n\\n\\n, USER:, ASSISTANT:"</em></span>

            <span class="block"><strong>Custom System Prompt:</strong> Eigener System-Prompt, der dem Modell zusätzliche Anweisungen gibt.
            <br><em>Optional - überschreibt Standard-Prompts</em></span>
          </p>
        </div>

        <!-- Preset-Tipps -->
        <div class="mt-4 p-3 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-300 dark:border-yellow-600 rounded">
          <h6 class="font-semibold text-yellow-900 dark:text-yellow-200 mb-2">💡 Preset-Empfehlungen</h6>
          <p class="text-gray-700 dark:text-gray-300 text-xs space-y-1">
            <span class="block"><strong>Ausgewogen:</strong> Gute Balance für die meisten Aufgaben (Temp: 0.7)</span>
            <span class="block"><strong>Präzise:</strong> Code, Fakten, technische Dokumentation (Temp: 0.1)</span>
            <span class="block"><strong>Kreativ:</strong> Storytelling, Brainstorming, kreative Texte (Temp: 1.2)</span>
          </p>
        </div>
      </div>
    </div>

    <!-- Presets -->
    <div class="mb-6">
      <label class="block text-sm font-medium mb-2 text-gray-700 dark:text-gray-200">Preset wählen:</label>
      <div class="flex gap-2 flex-wrap">
        <button @click="applyPreset('balanced')" class="px-3 py-1 bg-blue-500 text-white rounded hover:bg-blue-600">
          Ausgewogen
        </button>
        <button @click="applyPreset('precise')" class="px-3 py-1 bg-green-500 text-white rounded hover:bg-green-600">
          Präzise
        </button>
        <button @click="applyPreset('creative')" class="px-3 py-1 bg-purple-500 text-white rounded hover:bg-purple-600">
          Kreativ
        </button>
        <button @click="reset" class="px-3 py-1 bg-gray-500 text-white rounded hover:bg-gray-600">
          Reset
        </button>
      </div>
    </div>

    <!-- Parameters -->
    <div class="space-y-6">

      <!-- 🎯 Gruppe 1: Grundlegende Generierung -->
      <div class="p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-200 dark:border-blue-700">
        <h5 class="font-semibold mb-3 text-blue-900 dark:text-blue-200">🎯 Generierung</h5>
        <div class="space-y-3">
          <!-- Max Tokens -->
          <div>
            <label class="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-200">
              Max Tokens: {{ params.maxTokens }}
            </label>
            <input
              type="range"
              v-model.number="params.maxTokens"
              min="50"
              max="4096"
              step="50"
              class="w-full"
            />
          </div>
        </div>
      </div>

      <!-- 🎲 Gruppe 2: Sampling (Zufälligkeit) -->
      <div class="p-4 bg-purple-50 dark:bg-purple-900/20 rounded-lg border border-purple-200 dark:border-purple-700">
        <h5 class="font-semibold mb-3 text-purple-900 dark:text-purple-200">🎲 Sampling (Zufälligkeit)</h5>
        <div class="space-y-3">
          <!-- Temperature -->
          <div>
            <label class="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-200">
              Temperature: {{ params.temperature }}
            </label>
            <input
              type="range"
              v-model.number="params.temperature"
              min="0"
              max="2"
              step="0.05"
              class="w-full"
            />
          </div>

          <!-- Top P -->
          <div>
            <label class="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-200">
              Top P: {{ params.topP }}
            </label>
            <input
              type="range"
              v-model.number="params.topP"
              min="0"
              max="1"
              step="0.05"
              class="w-full"
            />
          </div>

          <!-- Top K -->
          <div>
            <label class="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-200">
              Top K: {{ params.topK }}
            </label>
            <input
              type="range"
              v-model.number="params.topK"
              min="1"
              max="100"
              step="1"
              class="w-full"
            />
          </div>

          <!-- Min P -->
          <div>
            <label class="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-200">
              Min P: {{ params.minP }}
            </label>
            <input
              type="range"
              v-model.number="params.minP"
              min="0"
              max="1"
              step="0.01"
              class="w-full"
            />
          </div>
        </div>
      </div>

      <!-- 🔁 Gruppe 3: Wiederholungs-Kontrolle -->
      <div class="p-4 bg-green-50 dark:bg-green-900/20 rounded-lg border border-green-200 dark:border-green-700">
        <h5 class="font-semibold mb-3 text-green-900 dark:text-green-200">🔁 Wiederholungs-Kontrolle</h5>
        <div class="space-y-3">
          <!-- Repeat Penalty -->
          <div>
            <label class="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-200">
              Repeat Penalty: {{ params.repeatPenalty }}
            </label>
            <input
              type="range"
              v-model.number="params.repeatPenalty"
              min="1.0"
              max="2.0"
              step="0.05"
              class="w-full"
            />
          </div>

          <!-- Repeat Last N -->
          <div>
            <label class="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-200">
              Repeat Last N: {{ params.repeatLastN }}
            </label>
            <input
              type="range"
              v-model.number="params.repeatLastN"
              min="0"
              max="256"
              step="8"
              class="w-full"
            />
          </div>

          <!-- Presence Penalty -->
          <div>
            <label class="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-200">
              Presence Penalty: {{ params.presencePenalty }}
            </label>
            <input
              type="range"
              v-model.number="params.presencePenalty"
              min="-2.0"
              max="2.0"
              step="0.1"
              class="w-full"
            />
          </div>

          <!-- Frequency Penalty -->
          <div>
            <label class="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-200">
              Frequency Penalty: {{ params.frequencyPenalty }}
            </label>
            <input
              type="range"
              v-model.number="params.frequencyPenalty"
              min="-2.0"
              max="2.0"
              step="0.1"
              class="w-full"
            />
          </div>
        </div>
      </div>

      <!-- 🎯 Gruppe 4: Mirostat (Perplexity Control) -->
      <div class="p-4 bg-orange-50 dark:bg-orange-900/20 rounded-lg border border-orange-200 dark:border-orange-700">
        <h5 class="font-semibold mb-3 text-orange-900 dark:text-orange-200">🎯 Mirostat (Erweitert)</h5>
        <div class="space-y-3">
          <!-- Mirostat Mode -->
          <div>
            <label class="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-200">
              Mirostat Mode: {{ params.mirostatMode }}
            </label>
            <select
              v-model.number="params.mirostatMode"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            >
              <option :value="0">0 - Deaktiviert</option>
              <option :value="1">1 - Mirostat v1</option>
              <option :value="2">2 - Mirostat v2</option>
            </select>
          </div>

          <!-- Mirostat Tau -->
          <div v-if="params.mirostatMode > 0">
            <label class="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-200">
              Mirostat Tau: {{ params.mirostatTau }}
            </label>
            <input
              type="range"
              v-model.number="params.mirostatTau"
              min="0"
              max="10"
              step="0.1"
              class="w-full"
            />
          </div>

          <!-- Mirostat Eta -->
          <div v-if="params.mirostatMode > 0">
            <label class="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-200">
              Mirostat Eta: {{ params.mirostatEta }}
            </label>
            <input
              type="range"
              v-model.number="params.mirostatEta"
              min="0"
              max="1"
              step="0.01"
              class="w-full"
            />
          </div>
        </div>
      </div>

      <!-- 🛑 Gruppe 5: Stop-Sequenzen & System Prompt -->
      <div class="p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg border border-gray-200 dark:border-gray-600">
        <h5 class="font-semibold mb-3 text-gray-900 dark:text-gray-200">🛑 Stop-Sequenzen & Prompts</h5>
        <div class="space-y-3">
          <!-- Stop Sequences -->
          <div>
            <label class="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-200">
              Stop Sequences (komma-getrennt)
            </label>
            <input
              type="text"
              v-model="stopSequencesText"
              @input="updateStopSequences"
              placeholder="z.B.: \n\n\n, USER:, ASSISTANT:"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            />
          </div>

          <!-- Custom System Prompt -->
          <div>
            <label class="block text-sm font-medium mb-1 text-gray-700 dark:text-gray-200">
              Custom System Prompt (optional)
            </label>
            <textarea
              v-model="params.customSystemPrompt"
              rows="3"
              placeholder="Eigener System-Prompt..."
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            ></textarea>
          </div>
        </div>
      </div>

    </div>

    <div class="mt-6 p-3 bg-blue-50 dark:bg-blue-900/20 rounded">
      <p class="text-sm text-blue-800 dark:text-blue-200">
        ✅ Alle Parameter werden automatisch gespeichert und bei der nächsten Anfrage verwendet.
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, watch, ref } from 'vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue'])

const showHelp = ref(false)

// Initialize params from modelValue props or use defaults
const params = reactive({
  // Generierung
  maxTokens: props.modelValue.maxTokens || 512,

  // Sampling
  temperature: props.modelValue.temperature || 0.7,
  topP: props.modelValue.topP || 0.9,
  topK: props.modelValue.topK || 40,
  minP: props.modelValue.minP || 0.05,

  // Wiederholung
  repeatPenalty: props.modelValue.repeatPenalty || 1.18,
  repeatLastN: props.modelValue.repeatLastN || 64,
  presencePenalty: props.modelValue.presencePenalty || 0.0,
  frequencyPenalty: props.modelValue.frequencyPenalty || 0.0,

  // Mirostat
  mirostatMode: props.modelValue.mirostatMode || 0,
  mirostatTau: props.modelValue.mirostatTau || 5.0,
  mirostatEta: props.modelValue.mirostatEta || 0.1,

  // Stop & Prompt
  stopSequences: props.modelValue.stopSequences || ['\n\n\n', 'USER:', 'ASSISTANT:'],
  customSystemPrompt: props.modelValue.customSystemPrompt || null
})

// Stop Sequences als Text für Input-Feld
const stopSequencesText = ref(params.stopSequences.join(', '))

const updateStopSequences = () => {
  params.stopSequences = stopSequencesText.value
    .split(',')
    .map(s => s.trim())
    .filter(s => s.length > 0)
}

watch(params, (newParams) => {
  emit('update:modelValue', { ...newParams })
}, { deep: true })

const applyPreset = (preset) => {
  if (preset === 'balanced') {
    params.maxTokens = 512
    params.temperature = 0.7
    params.topP = 0.9
    params.topK = 40
    params.minP = 0.05
    params.repeatPenalty = 1.1
    params.repeatLastN = 64
    params.presencePenalty = 0.0
    params.frequencyPenalty = 0.0
    params.mirostatMode = 0
    params.mirostatTau = 5.0
    params.mirostatEta = 0.1
  } else if (preset === 'precise') {
    params.maxTokens = 512
    params.temperature = 0.1
    params.topP = 0.95
    params.topK = 10
    params.minP = 0.1
    params.repeatPenalty = 1.2
    params.repeatLastN = 128
    params.presencePenalty = 0.5
    params.frequencyPenalty = 0.5
    params.mirostatMode = 0
    params.mirostatTau = 5.0
    params.mirostatEta = 0.1
  } else if (preset === 'creative') {
    params.maxTokens = 1024
    params.temperature = 1.2
    params.topP = 0.95
    params.topK = 80
    params.minP = 0.01
    params.repeatPenalty = 1.0
    params.repeatLastN = 32
    params.presencePenalty = -0.5
    params.frequencyPenalty = -0.5
    params.mirostatMode = 0
    params.mirostatTau = 5.0
    params.mirostatEta = 0.1
  }
}

const reset = () => {
  applyPreset('balanced')
}
</script>

<style scoped>
input[type="range"] {
  accent-color: #3b82f6;
}
</style>
