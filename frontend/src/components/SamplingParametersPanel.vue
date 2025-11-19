<template>
  <div class="sampling-params-panel">
    <!-- Header mit Preset-Auswahl und Aktionen -->
    <div class="params-header">
      <div class="preset-selector">
        <label>Preset:</label>
        <select v-model="selectedPreset" @change="applyPreset" class="preset-dropdown">
          <option value="">Benutzerdefiniert</option>
          <option value="ultra-precise">Ultra-Präzise (0.05 temp)</option>
          <option value="balanced">Ausgewogen (0.1 temp)</option>
          <option value="detailed">Detailliert (0.2 temp)</option>
          <option value="creative">Kreativ (0.5 temp)</option>
          <option value="mirostat">Mirostat (experimentell)</option>
        </select>
      </div>
      <div class="action-buttons">
        <button @click="resetToDefaults" class="btn-secondary" title="Setzt alle Parameter auf Standard-Werte zurück">
          <i class="fas fa-undo"></i> Reset
        </button>
        <button @click="autoDetect" class="btn-secondary" title="Erkennt optimale Parameter basierend auf Modell">
          <i class="fas fa-magic"></i> Auto
        </button>
        <button @click="showHelp = true" class="btn-info" title="Zeigt Hilfe zu allen Parametern">
          <i class="fas fa-question-circle"></i> Hilfe
        </button>
      </div>
    </div>

    <!-- Parameter-Gruppen -->
    <div class="params-content">
      <!-- Generation Control -->
      <div class="param-group">
        <h4 class="group-title">
          <i class="fas fa-sliders-h"></i> Generation Control
        </h4>

        <div class="param-row">
          <label for="maxTokens" class="param-label">
            Max Tokens (num_predict)
            <span class="param-info" title="Maximale Anzahl generierter Tokens (Ollama: num_predict, llama.cpp: n_predict)">ⓘ</span>
          </label>
          <div class="param-control">
            <input
              type="range"
              id="maxTokens"
              v-model.number="params.maxTokens"
              min="512"
              max="131072"
              step="512"
              class="param-slider"
            />
            <input
              type="number"
              v-model.number="params.maxTokens"
              min="512"
              max="131072"
              class="param-value"
            />
          </div>
          <span class="param-hint">{{ getMaxTokensHint() }}</span>
        </div>
      </div>

      <!-- Sampling Parameters -->
      <div class="param-group">
        <h4 class="group-title">
          <i class="fas fa-dice"></i> Sampling Parameters
        </h4>

        <div class="param-row">
          <label for="temperature" class="param-label">
            Temperature
            <span class="param-info" title="Zufälligkeit: 0=deterministisch, 2=sehr kreativ">ⓘ</span>
          </label>
          <div class="param-control">
            <input
              type="range"
              id="temperature"
              v-model.number="params.temperature"
              min="0"
              max="2"
              step="0.05"
              class="param-slider"
            />
            <input
              type="number"
              v-model.number="params.temperature"
              min="0"
              max="2"
              step="0.05"
              class="param-value"
            />
          </div>
          <span class="param-hint" :class="{'warning': params.temperature > 0.5}">
            {{ getTemperatureHint() }}
          </span>
        </div>

        <div class="param-row">
          <label for="topP" class="param-label">
            Top P
            <span class="param-info" title="Nucleus Sampling: Wahrscheinlichkeits-Cutoff">ⓘ</span>
          </label>
          <div class="param-control">
            <input
              type="range"
              id="topP"
              v-model.number="params.topP"
              min="0"
              max="1"
              step="0.05"
              class="param-slider"
            />
            <input
              type="number"
              v-model.number="params.topP"
              min="0"
              max="1"
              step="0.05"
              class="param-value"
            />
          </div>
        </div>

        <div class="param-row">
          <label for="topK" class="param-label">
            Top K
            <span class="param-info" title="Limitiert auf K wahrscheinlichste Tokens (0=disabled)">ⓘ</span>
          </label>
          <div class="param-control">
            <input
              type="range"
              id="topK"
              v-model.number="params.topK"
              min="0"
              max="100"
              step="5"
              class="param-slider"
            />
            <input
              type="number"
              v-model.number="params.topK"
              min="0"
              max="100"
              class="param-value"
            />
          </div>
        </div>

        <div class="param-row">
          <label for="minP" class="param-label">
            Min P
            <span class="param-info" title="Minimale Token-Wahrscheinlichkeit">ⓘ</span>
          </label>
          <div class="param-control">
            <input
              type="range"
              id="minP"
              v-model.number="params.minP"
              min="0"
              max="1"
              step="0.01"
              class="param-slider"
            />
            <input
              type="number"
              v-model.number="params.minP"
              min="0"
              max="1"
              step="0.01"
              class="param-value"
            />
          </div>
        </div>
      </div>

      <!-- Repetition Control -->
      <div class="param-group">
        <h4 class="group-title">
          <i class="fas fa-ban"></i> Repetition Control
        </h4>

        <div class="param-row">
          <label for="repeatPenalty" class="param-label">
            Repeat Penalty
            <span class="param-info" title="Bestraft Wiederholungen (>1.0 empfohlen)">ⓘ</span>
          </label>
          <div class="param-control">
            <input
              type="range"
              id="repeatPenalty"
              v-model.number="params.repeatPenalty"
              min="1"
              max="1.5"
              step="0.05"
              class="param-slider"
            />
            <input
              type="number"
              v-model.number="params.repeatPenalty"
              min="1"
              max="1.5"
              step="0.05"
              class="param-value"
            />
          </div>
        </div>

        <div class="param-row">
          <label for="repeatLastN" class="param-label">
            Repeat Last N
            <span class="param-info" title="Anzahl Tokens für Repeat-Detection">ⓘ</span>
          </label>
          <div class="param-control">
            <input
              type="range"
              id="repeatLastN"
              v-model.number="params.repeatLastN"
              min="0"
              max="512"
              step="16"
              class="param-slider"
            />
            <input
              type="number"
              v-model.number="params.repeatLastN"
              min="0"
              max="512"
              class="param-value"
            />
          </div>
        </div>

        <div class="param-row">
          <label for="presencePenalty" class="param-label">
            Presence Penalty
            <span class="param-info" title="Ermutigt neue Themen (positiv) oder fokussiert (negativ)">ⓘ</span>
          </label>
          <div class="param-control">
            <input
              type="range"
              id="presencePenalty"
              v-model.number="params.presencePenalty"
              min="-2"
              max="2"
              step="0.1"
              class="param-slider"
            />
            <input
              type="number"
              v-model.number="params.presencePenalty"
              min="-2"
              max="2"
              step="0.1"
              class="param-value"
            />
          </div>
        </div>

        <div class="param-row">
          <label for="frequencyPenalty" class="param-label">
            Frequency Penalty
            <span class="param-info" title="Reduziert Wort-Wiederholungen">ⓘ</span>
          </label>
          <div class="param-control">
            <input
              type="range"
              id="frequencyPenalty"
              v-model.number="params.frequencyPenalty"
              min="-2"
              max="2"
              step="0.1"
              class="param-slider"
            />
            <input
              type="number"
              v-model.number="params.frequencyPenalty"
              min="-2"
              max="2"
              step="0.1"
              class="param-value"
            />
          </div>
        </div>
      </div>

      <!-- Mirostat (Advanced) - Collapsible -->
      <div class="param-group">
        <h4 class="group-title" @click="showMirostat = !showMirostat" style="cursor: pointer">
          <i class="fas" :class="showMirostat ? 'fa-chevron-down' : 'fa-chevron-right'"></i>
          <i class="fas fa-flask"></i> Mirostat (Experimentell)
        </h4>

        <div v-if="showMirostat" class="mirostat-section">
          <div class="param-row">
            <label for="mirostatMode" class="param-label">
              Mirostat Mode
              <span class="param-info" title="0=aus, 2=Mirostat 2.0 (deaktiviert top_p/top_k)">ⓘ</span>
            </label>
            <div class="param-control">
              <select v-model.number="params.mirostatMode" id="mirostatMode" class="param-select">
                <option :value="0">Aus</option>
                <option :value="1">Mirostat 1.0</option>
                <option :value="2">Mirostat 2.0</option>
              </select>
            </div>
          </div>

          <div v-if="params.mirostatMode > 0">
            <div class="param-row">
              <label for="mirostatTau" class="param-label">
                Mirostat Tau
                <span class="param-info" title="Target Entropy/Perplexität">ⓘ</span>
              </label>
              <div class="param-control">
                <input
                  type="range"
                  id="mirostatTau"
                  v-model.number="params.mirostatTau"
                  min="0"
                  max="10"
                  step="0.5"
                  class="param-slider"
                />
                <input
                  type="number"
                  v-model.number="params.mirostatTau"
                  min="0"
                  max="10"
                  step="0.1"
                  class="param-value"
                />
              </div>
            </div>

            <div class="param-row">
              <label for="mirostatEta" class="param-label">
                Mirostat Eta
                <span class="param-info" title="Learning Rate">ⓘ</span>
              </label>
              <div class="param-control">
                <input
                  type="range"
                  id="mirostatEta"
                  v-model.number="params.mirostatEta"
                  min="0.01"
                  max="1"
                  step="0.01"
                  class="param-slider"
                />
                <input
                  type="number"
                  v-model.number="params.mirostatEta"
                  min="0.01"
                  max="1"
                  step="0.01"
                  class="param-value"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Stop Sequences -->
      <div class="param-group">
        <h4 class="group-title">
          <i class="fas fa-stop-circle"></i> Stop Sequences
        </h4>
        <div class="param-row">
          <label for="stopSequences" class="param-label">
            Stop Strings
            <span class="param-info" title="Sequenzen die Generation stoppen (komma-getrennt)">ⓘ</span>
          </label>
          <textarea
            id="stopSequences"
            v-model="stopSequencesText"
            @blur="updateStopSequences"
            rows="2"
            class="param-textarea"
            placeholder='\n\n\n,USER:,ASSISTANT:,###'
          ></textarea>
        </div>
      </div>

      <!-- Custom System Prompt -->
      <div class="param-group">
        <h4 class="group-title">
          <i class="fas fa-comment-dots"></i> Custom System Prompt
        </h4>
        <div class="param-row">
          <textarea
            v-model="params.customSystemPrompt"
            rows="3"
            class="param-textarea"
            placeholder="Optionaler custom System Prompt (überschreibt Standard-Prompt)"
          ></textarea>
        </div>
      </div>
    </div>

    <!-- Hilfe-Modal -->
    <div v-if="showHelp" class="help-modal-overlay" @click.self="showHelp = false">
      <div class="help-modal">
        <div class="help-header">
          <h3><i class="fas fa-question-circle"></i> Sampling Parameter Hilfe</h3>
          <button @click="showHelp = false" class="close-btn">&times;</button>
        </div>
        <div class="help-content">
          <div v-if="helpData.parameters" class="help-section">
            <div v-for="(param, key) in helpData.parameters" :key="key" class="help-item">
              <h4>{{ param.name }}</h4>
              <p><strong>Beschreibung:</strong> {{ param.description }}</p>
              <p><strong>Range:</strong> {{ param.range }} | <strong>Default:</strong> {{ param.default }}</p>
              <p><strong>Beispiel:</strong> {{ param.example }}</p>
              <p v-if="param.tip" class="help-tip"><i class="fas fa-lightbulb"></i> {{ param.tip }}</p>
              <p v-if="param.warning" class="help-warning"><i class="fas fa-exclamation-triangle"></i> {{ param.warning }}</p>
            </div>
          </div>

          <div v-if="helpData.quickTips" class="help-section">
            <h4><i class="fas fa-bolt"></i> Quick Tips</h4>
            <ul>
              <li v-for="(tip, key) in helpData.quickTips" :key="key">
                <strong>{{ key }}:</strong> {{ tip }}
              </li>
            </ul>
          </div>

          <div v-if="helpData.commonIssues" class="help-section">
            <h4><i class="fas fa-wrench"></i> Häufige Probleme</h4>
            <ul>
              <li v-for="(solution, problem) in helpData.commonIssues" :key="problem">
                <strong>{{ problem }}:</strong> {{ solution }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, watch, onMounted } from 'vue';
import api from '../services/api';

export default {
  name: 'SamplingParametersPanel',
  props: {
    modelValue: {
      type: Object,
      default: () => ({})
    },
    modelName: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const params = reactive({
      maxTokens: 32768,  // Default: 32k tokens für lange Antworten (ca. 15-20 Seiten)
      temperature: 0.7,
      topP: 0.9,
      topK: 40,
      minP: 0.05,
      repeatPenalty: 1.1,
      repeatLastN: 64,
      presencePenalty: 0.0,
      frequencyPenalty: 0.0,
      mirostatMode: 0,
      mirostatTau: 5.0,
      mirostatEta: 0.1,
      stopSequences: ['\n\n\n', 'USER:', 'ASSISTANT:'],
      customSystemPrompt: null
    });

    const selectedPreset = ref('');
    const showMirostat = ref(false);
    const showHelp = ref(false);
    const helpData = ref({});
    const stopSequencesText = ref('\\n\\n\\n,USER:,ASSISTANT:');

    // Watch für Parameter-Änderungen
    watch(params, (newParams) => {
      emit('update:modelValue', { ...newParams });
      selectedPreset.value = ''; // Custom wenn manuell geändert
    }, { deep: true });

    // Watch für modelValue von außen
    watch(() => props.modelValue, (newValue) => {
      if (newValue && Object.keys(newValue).length > 0) {
        Object.assign(params, newValue);
        updateStopSequencesText();
      }
    }, { immediate: true, deep: true });

    // Preset anwenden
    const applyPreset = async () => {
      if (!selectedPreset.value) return;

      try {
        const response = await api.get(`/api/sampling/presets/${selectedPreset.value}`);
        Object.assign(params, response.data);
        updateStopSequencesText();
      } catch (error) {
        console.error('Fehler beim Laden des Presets:', error);
      }
    };

    // Reset zu Defaults
    const resetToDefaults = async () => {
      try {
        const response = await api.get('/api/sampling/defaults');
        Object.assign(params, response.data.parameters);
        selectedPreset.value = '';
        updateStopSequencesText();
      } catch (error) {
        console.error('Fehler beim Laden der Defaults:', error);
      }
    };

    // Auto-Detect basierend auf Modell
    const autoDetect = async () => {
      if (!props.modelName) {
        alert('Kein Modell ausgewählt!');
        return;
      }

      try {
        const response = await api.get(`/api/sampling/defaults/auto/${encodeURIComponent(props.modelName)}`);
        Object.assign(params, response.data.parameters);
        selectedPreset.value = '';
        updateStopSequencesText();

        // Zeige Modell-Typ
        if (response.data.modelType) {
          console.log(`Auto-detected model type: ${response.data.modelType}`);
        }
      } catch (error) {
        console.error('Fehler beim Auto-Detect:', error);
      }
    };

    // Hilfe laden
    const loadHelp = async () => {
      try {
        const response = await api.get('/api/sampling/help/de');
        helpData.value = response.data;
      } catch (error) {
        console.error('Fehler beim Laden der Hilfe:', error);
      }
    };

    // Stop Sequences Text aktualisieren
    const updateStopSequencesText = () => {
      if (params.stopSequences && Array.isArray(params.stopSequences)) {
        stopSequencesText.value = params.stopSequences
          .map(s => s.replace(/\n/g, '\\n'))
          .join(',');
      }
    };

    // Stop Sequences von Text aktualisieren
    const updateStopSequences = () => {
      if (stopSequencesText.value) {
        params.stopSequences = stopSequencesText.value
          .split(',')
          .map(s => s.trim().replace(/\\n/g, '\n'))
          .filter(s => s.length > 0);
      }
    };

    // Hints
    const getMaxTokensHint = () => {
      if (params.maxTokens < 2048) return 'Sehr kurz (< 1 Seite)';
      if (params.maxTokens < 8192) return 'Kurz (1-3 Seiten)';
      if (params.maxTokens < 32768) return 'Mittel (3-15 Seiten)';
      if (params.maxTokens < 65536) return 'Lang (15-30 Seiten)';
      return 'Sehr lang (30+ Seiten)';
    };

    const getTemperatureHint = () => {
      if (params.temperature <= 0.1) return 'Sehr faktisch (Vision!)';
      if (params.temperature <= 0.3) return 'Faktisch';
      if (params.temperature <= 0.7) return 'Ausgewogen';
      if (params.temperature <= 1.0) return 'Kreativ';
      return '⚠️ SEHR kreativ (Halluzination möglich!)';
    };

    onMounted(() => {
      loadHelp();
      if (props.modelValue && Object.keys(props.modelValue).length > 0) {
        Object.assign(params, props.modelValue);
      }
      updateStopSequencesText();
    });

    return {
      params,
      selectedPreset,
      showMirostat,
      showHelp,
      helpData,
      stopSequencesText,
      applyPreset,
      resetToDefaults,
      autoDetect,
      updateStopSequences,
      getMaxTokensHint,
      getTemperatureHint
    };
  }
};
</script>

<style scoped>
.sampling-params-panel {
  background: var(--bg-secondary);
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 1rem;
}

.params-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  gap: 1rem;
  flex-wrap: wrap;
}

.preset-selector {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
}

.preset-selector label {
  font-weight: 600;
  color: var(--text-primary);
}

.preset-dropdown {
  flex: 1;
  max-width: 300px;
  padding: 0.5rem;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 0.9rem;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
}

.btn-secondary, .btn-info {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  transition: all 0.2s;
}

.btn-secondary {
  background: var(--primary-color);
  color: white;
}

.btn-secondary:hover {
  background: var(--primary-dark);
  transform: translateY(-1px);
}

.btn-info {
  background: #3b82f6;
  color: white;
}

.btn-info:hover {
  background: #2563eb;
  transform: translateY(-1px);
}

.params-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.param-group {
  background: var(--bg-primary);
  border-radius: 6px;
  padding: 1rem;
  border: 1px solid var(--border-color);
}

.group-title {
  color: var(--primary-color);
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 1rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.param-row {
  margin-bottom: 1rem;
}

.param-row:last-child {
  margin-bottom: 0;
}

.param-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
}

.param-info {
  cursor: help;
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.param-control {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.param-slider {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: var(--border-color);
  outline: none;
  -webkit-appearance: none;
}

.param-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--primary-color);
  cursor: pointer;
  transition: all 0.2s;
}

.param-slider::-webkit-slider-thumb:hover {
  transform: scale(1.2);
  background: var(--primary-dark);
}

.param-slider::-moz-range-thumb {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--primary-color);
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.param-slider::-moz-range-thumb:hover {
  transform: scale(1.2);
  background: var(--primary-dark);
}

.param-value {
  width: 80px;
  padding: 0.4rem;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  text-align: center;
  font-size: 0.9rem;
}

.param-select {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-size: 0.9rem;
}

.param-textarea {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-family: 'Courier New', monospace;
  font-size: 0.85rem;
  resize: vertical;
}

.param-hint {
  display: block;
  margin-top: 0.25rem;
  font-size: 0.8rem;
  color: var(--text-secondary);
  font-style: italic;
}

.param-hint.warning {
  color: #f59e0b;
  font-weight: 600;
}

.mirostat-section {
  padding-top: 0.5rem;
  border-top: 1px dashed var(--border-color);
}

/* Hilfe-Modal */
.help-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  padding: 1rem;
}

.help-modal {
  background: var(--bg-primary);
  border-radius: 8px;
  max-width: 800px;
  width: 100%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

.help-header {
  padding: 1.5rem;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.help-header h3 {
  margin: 0;
  color: var(--primary-color);
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.close-btn {
  background: none;
  border: none;
  font-size: 2rem;
  color: var(--text-secondary);
  cursor: pointer;
  line-height: 1;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
}

.help-content {
  padding: 1.5rem;
  overflow-y: auto;
  flex: 1;
}

.help-section {
  margin-bottom: 2rem;
}

.help-section:last-child {
  margin-bottom: 0;
}

.help-section h4 {
  color: var(--primary-color);
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.help-item {
  margin-bottom: 1.5rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--border-color);
}

.help-item:last-child {
  border-bottom: none;
}

.help-item h4 {
  color: var(--text-primary);
  font-size: 1rem;
  margin-bottom: 0.5rem;
}

.help-item p {
  margin: 0.5rem 0;
  color: var(--text-secondary);
  line-height: 1.5;
}

.help-tip {
  background: #dbeafe;
  color: #1e40af;
  padding: 0.5rem;
  border-radius: 4px;
  border-left: 3px solid #3b82f6;
}

.help-warning {
  background: #fef3c7;
  color: #92400e;
  padding: 0.5rem;
  border-radius: 4px;
  border-left: 3px solid #f59e0b;
}

.help-section ul {
  list-style: none;
  padding: 0;
}

.help-section ul li {
  padding: 0.5rem 0;
  color: var(--text-secondary);
  line-height: 1.5;
}

/* Responsive */
@media (max-width: 768px) {
  .params-header {
    flex-direction: column;
    align-items: stretch;
  }

  .preset-selector {
    flex-direction: column;
    align-items: stretch;
  }

  .preset-dropdown {
    max-width: none;
  }

  .action-buttons {
    justify-content: stretch;
  }

  .action-buttons button {
    flex: 1;
  }

  .param-control {
    flex-wrap: wrap;
  }

  .param-slider {
    width: 100%;
  }
}
</style>
