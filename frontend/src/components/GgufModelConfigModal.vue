<template>
  <Transition name="modal">
    <div v-if="show" class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-[60] p-4">
      <div class="
        bg-white/90 dark:bg-gray-800/90
        backdrop-blur-xl backdrop-saturate-150
        rounded-2xl shadow-2xl
        w-full max-w-4xl max-h-[90vh]
        border border-gray-200/50 dark:border-gray-700/50
        flex flex-col
        transform transition-all duration-300
      ">
        <!-- Header -->
        <div class="
          flex items-center justify-between p-6
          bg-gradient-to-r from-purple-500/10 to-indigo-500/10
          dark:from-purple-500/20 dark:to-indigo-500/20
          border-b border-gray-200/50 dark:border-gray-700/50
        ">
          <div class="flex items-center gap-4">
            <div class="p-3 rounded-xl bg-gradient-to-br from-purple-500 to-indigo-500 shadow-lg">
              <CpuChipIcon class="w-7 h-7 text-white" />
            </div>
            <div>
              <h2 class="text-2xl font-bold bg-gradient-to-r from-gray-900 to-gray-700 dark:from-white dark:to-gray-300 bg-clip-text text-transparent">
                {{ editMode ? 'GGUF Modell bearbeiten' : 'Neues GGUF Modell konfigurieren' }}
              </h2>
              <p class="text-sm text-gray-600 dark:text-gray-400">llama.cpp Model Configuration</p>
            </div>
          </div>
          <button
            @click="$emit('close')"
            class="
              p-2 rounded-lg
              text-gray-400 hover:text-gray-600 dark:hover:text-gray-300
              hover:bg-gray-100 dark:hover:bg-gray-700
              transition-all duration-200
              transform hover:scale-110 active:scale-95
            "
          >
            <XMarkIcon class="w-6 h-6" />
          </button>
        </div>

        <!-- Tabs -->
        <div class="flex border-b border-gray-200 dark:border-gray-700 px-6">
          <button
            @click="activeTab = 'config'"
            :class="[
              'px-6 py-3 font-medium text-sm transition-all duration-200',
              activeTab === 'config'
                ? 'text-purple-600 dark:text-purple-400 border-b-2 border-purple-600 dark:border-purple-400'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
            ]"
          >
            Konfiguration
          </button>
          <button
            @click="activeTab = 'help'"
            :class="[
              'px-6 py-3 font-medium text-sm transition-all duration-200',
              activeTab === 'help'
                ? 'text-purple-600 dark:text-purple-400 border-b-2 border-purple-600 dark:border-purple-400'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'
            ]"
          >
            Hilfe
          </button>
        </div>

        <!-- Content -->
        <div class="flex-1 overflow-y-auto p-6">
          <!-- Config Tab -->
          <div v-if="activeTab === 'config'" class="space-y-6">

            <!-- Model Name -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Display Name
                <span class="text-red-500">*</span>
              </label>
              <input
                v-model="formData.name"
                type="text"
                placeholder="z.B. Karla Coding Assistant"
                class="
                  w-full px-4 py-3 rounded-xl
                  bg-white dark:bg-gray-900
                  border border-gray-300 dark:border-gray-600
                  text-gray-900 dark:text-white
                  focus:ring-2 focus:ring-purple-500 focus:border-transparent
                  transition-all duration-200
                "
              />
              <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Eindeutiger Name für diese Konfiguration
              </p>
            </div>

            <!-- Base Model Selection -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Basis GGUF Model
                <span class="text-red-500">*</span>
              </label>
              <select
                v-model="formData.baseModel"
                class="
                  w-full px-4 py-3 rounded-xl
                  bg-white dark:bg-gray-900
                  border border-gray-300 dark:border-gray-600
                  text-gray-900 dark:text-white
                  focus:ring-2 focus:ring-purple-500 focus:border-transparent
                  transition-all duration-200
                "
              >
                <option value="">-- GGUF Model auswählen --</option>
                <option v-for="model in availableGgufModels" :key="model.name" :value="model.name">
                  {{ model.name }}
                </option>
              </select>
              <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Wähle ein bestehendes .gguf Model aus models/library/ oder models/custom/
              </p>
            </div>

            <!-- Category -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Kategorie (optional)
              </label>
              <input
                v-model="formData.category"
                type="text"
                placeholder="z.B. Coding, Chat, German, Vision"
                class="
                  w-full px-4 py-3 rounded-xl
                  bg-white dark:bg-gray-900
                  border border-gray-300 dark:border-gray-600
                  text-gray-900 dark:text-white
                  focus:ring-2 focus:ring-purple-500 focus:border-transparent
                  transition-all duration-200
                "
              />
            </div>

            <!-- Tags -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Tags (optional, Komma-getrennt)
              </label>
              <input
                v-model="formData.tags"
                type="text"
                placeholder="z.B. llama, qwen, instruct, deutsch"
                class="
                  w-full px-4 py-3 rounded-xl
                  bg-white dark:bg-gray-900
                  border border-gray-300 dark:border-gray-600
                  text-gray-900 dark:text-white
                  focus:ring-2 focus:ring-purple-500 focus:border-transparent
                  transition-all duration-200
                "
              />
            </div>

            <!-- Description -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Beschreibung (optional)
              </label>
              <textarea
                v-model="formData.description"
                rows="2"
                placeholder="Kurze Beschreibung dieser Model-Konfiguration..."
                class="
                  w-full px-4 py-3 rounded-xl
                  bg-white dark:bg-gray-900
                  border border-gray-300 dark:border-gray-600
                  text-gray-900 dark:text-white
                  focus:ring-2 focus:ring-purple-500 focus:border-transparent
                  transition-all duration-200
                  resize-none
                "
              ></textarea>
            </div>

            <!-- System Prompt Section -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Initialisierungs-Prompt / System Prompt
              </label>

              <!-- File Upload Button -->
              <div class="mb-3">
                <label class="
                  inline-flex items-center gap-2 px-4 py-2.5 rounded-xl
                  bg-gradient-to-r from-purple-500 to-indigo-500
                  hover:from-purple-600 hover:to-indigo-600
                  text-white font-medium
                  shadow-sm hover:shadow-md
                  transition-all duration-200
                  transform hover:scale-105 active:scale-95
                  cursor-pointer
                ">
                  <DocumentArrowUpIcon class="w-5 h-5" />
                  <span>Text-Datei hochladen</span>
                  <input
                    type="file"
                    accept=".txt,.md"
                    @change="handleFileUpload"
                    class="hidden"
                  />
                </label>
                <span v-if="uploadedFileName" class="ml-3 text-sm text-gray-600 dark:text-gray-400">
                  ✓ {{ uploadedFileName }}
                </span>
              </div>

              <!-- System Prompt Textarea -->
              <textarea
                v-model="formData.systemPrompt"
                rows="8"
                placeholder="Du bist ein hilfsbereiter KI-Assistent...&#10;&#10;Deine Aufgaben:&#10;- Beantworte Fragen präzise&#10;- Verwende Markdown-Formatierung&#10;- ..."
                class="
                  w-full px-4 py-3 rounded-xl
                  bg-white dark:bg-gray-900
                  border border-gray-300 dark:border-gray-600
                  text-gray-900 dark:text-white
                  focus:ring-2 focus:ring-purple-500 focus:border-transparent
                  transition-all duration-200
                  font-mono text-sm
                "
              ></textarea>
              <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">
                Dieser Text wird bei jedem Chat diesem Modell vorangestellt
              </p>
            </div>

            <!-- Context Size Selection -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">
                Context Window Size
                <span class="text-red-500">*</span>
              </label>
              <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
                <button
                  v-for="size in contextSizes"
                  :key="size.value"
                  @click="formData.contextSize = size.value"
                  class="
                    px-4 py-3 rounded-xl font-medium
                    border-2 transition-all duration-200
                    transform hover:scale-105 active:scale-95
                  "
                  :class="formData.contextSize === size.value
                    ? 'border-purple-500 bg-purple-500 text-white shadow-lg'
                    : 'border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-900 text-gray-700 dark:text-gray-300 hover:border-purple-400'"
                >
                  <div class="text-lg font-bold">{{ size.label }}</div>
                  <div class="text-xs opacity-75">{{ size.tokens }} Tokens</div>
                </button>
              </div>
              <p class="mt-2 text-xs text-gray-500 dark:text-gray-400">
                ⚠️ Größere Context-Größen benötigen mehr RAM/VRAM (z.B. 128K ≈ 16-32 GB)
              </p>
            </div>

            <!-- GPU Layers -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                GPU Layers
              </label>
              <div class="flex items-center gap-4">
                <input
                  v-model.number="formData.gpuLayers"
                  type="number"
                  min="0"
                  max="999"
                  class="
                    w-32 px-4 py-3 rounded-xl
                    bg-white dark:bg-gray-900
                    border border-gray-300 dark:border-gray-600
                    text-gray-900 dark:text-white
                    focus:ring-2 focus:ring-purple-500 focus:border-transparent
                    transition-all duration-200
                  "
                />
                <span class="text-sm text-gray-600 dark:text-gray-400">
                  (0 = CPU only, 999 = alle Layers auf GPU)
                </span>
              </div>
            </div>

            <!-- Advanced Parameters (Collapsible) -->
            <div class="border-t border-gray-200 dark:border-gray-700 pt-6">
              <button
                @click="showAdvanced = !showAdvanced"
                class="flex items-center gap-2 text-gray-700 dark:text-gray-300 hover:text-purple-600 dark:hover:text-purple-400 transition-colors"
              >
                <ChevronDownIcon class="w-5 h-5 transition-transform" :class="{ 'rotate-180': showAdvanced }" />
                <span class="font-medium">Erweiterte Parameter (optional)</span>
              </button>

              <Transition name="slide">
                <div v-if="showAdvanced" class="mt-4 space-y-4 pl-7">
                  <!-- Temperature -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Temperature
                    </label>
                    <input
                      v-model.number="formData.temperature"
                      type="number"
                      step="0.1"
                      min="0"
                      max="2"
                      placeholder="0.7 (Standard)"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- Top P -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Top P (Nucleus Sampling)
                    </label>
                    <input
                      v-model.number="formData.topP"
                      type="number"
                      step="0.05"
                      min="0"
                      max="1"
                      placeholder="0.9 (Standard)"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- Top K -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Top K
                    </label>
                    <input
                      v-model.number="formData.topK"
                      type="number"
                      min="1"
                      max="100"
                      placeholder="40 (Standard)"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- Repeat Penalty -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Repeat Penalty
                    </label>
                    <input
                      v-model.number="formData.repeatPenalty"
                      type="number"
                      step="0.1"
                      min="1"
                      max="2"
                      placeholder="1.1 (Standard)"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- Max Tokens -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Max Tokens (Antwort-Limit)
                    </label>
                    <input
                      v-model.number="formData.maxTokens"
                      type="number"
                      min="100"
                      max="8192"
                      placeholder="2048 (Standard)"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- ========== CPU/Threading Configuration ========== -->
                  <div class="col-span-2 pt-4 border-t border-gray-200 dark:border-gray-700">
                    <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">CPU/Threading</h4>
                  </div>

                  <!-- Threads -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Threads (CPU-Kerne)
                    </label>
                    <input
                      v-model.number="formData.threads"
                      type="number"
                      min="0"
                      max="64"
                      placeholder="0 = Auto"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- Batch Size -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Batch Size
                    </label>
                    <input
                      v-model.number="formData.batchSize"
                      type="number"
                      min="1"
                      max="2048"
                      placeholder="512 (Standard)"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- ========== RoPE Scaling ========== -->
                  <div class="col-span-2 pt-4 border-t border-gray-200 dark:border-gray-700">
                    <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">RoPE Scaling (Context-Erweiterung)</h4>
                  </div>

                  <!-- RoPE Freq Base -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      RoPE Freq Base
                    </label>
                    <input
                      v-model.number="formData.ropeFreqBase"
                      type="number"
                      step="1000"
                      min="1000"
                      placeholder="10000 (Standard)"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- RoPE Freq Scale -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      RoPE Freq Scale
                    </label>
                    <input
                      v-model.number="formData.ropeFreqScale"
                      type="number"
                      step="0.1"
                      min="0.1"
                      max="10"
                      placeholder="1.0 (Standard)"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- ========== Mirostat Sampling ========== -->
                  <div class="col-span-2 pt-4 border-t border-gray-200 dark:border-gray-700">
                    <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Mirostat Sampling</h4>
                  </div>

                  <!-- Mirostat Mode -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Mirostat Mode
                    </label>
                    <select
                      v-model.number="formData.mirostat"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    >
                      <option :value="null">Deaktiviert</option>
                      <option :value="1">Mirostat 1</option>
                      <option :value="2">Mirostat 2.0</option>
                    </select>
                  </div>

                  <!-- Mirostat Tau -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Mirostat Tau
                    </label>
                    <input
                      v-model.number="formData.mirostatTau"
                      type="number"
                      step="0.1"
                      min="0"
                      max="10"
                      placeholder="5.0 (Standard)"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- Mirostat Eta -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Mirostat Eta
                    </label>
                    <input
                      v-model.number="formData.mirostatEta"
                      type="number"
                      step="0.01"
                      min="0"
                      max="1"
                      placeholder="0.1 (Standard)"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- ========== Advanced Sampling ========== -->
                  <div class="col-span-2 pt-4 border-t border-gray-200 dark:border-gray-700">
                    <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Erweiterte Sampling-Parameter</h4>
                  </div>

                  <!-- TFS Z -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      TFS (Tail Free Sampling)
                    </label>
                    <input
                      v-model.number="formData.tfsZ"
                      type="number"
                      step="0.1"
                      min="0"
                      max="1"
                      placeholder="1.0 = Deaktiviert"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- Typical P -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Typical P
                    </label>
                    <input
                      v-model.number="formData.typicalP"
                      type="number"
                      step="0.1"
                      min="0"
                      max="1"
                      placeholder="1.0 = Deaktiviert"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- Presence Penalty -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Presence Penalty
                    </label>
                    <input
                      v-model.number="formData.presencePenalty"
                      type="number"
                      step="0.1"
                      min="0"
                      max="2"
                      placeholder="0.0 (Standard)"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- Frequency Penalty -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Frequency Penalty
                    </label>
                    <input
                      v-model.number="formData.frequencyPenalty"
                      type="number"
                      step="0.1"
                      min="0"
                      max="2"
                      placeholder="0.0 (Standard)"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- Min-P -->
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Min-P Sampling
                    </label>
                    <input
                      v-model.number="formData.minP"
                      type="number"
                      step="0.01"
                      min="0"
                      max="1"
                      placeholder="0.05 (Standard)"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- ========== Reproducibility ========== -->
                  <div class="col-span-2 pt-4 border-t border-gray-200 dark:border-gray-700">
                    <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Reproduzierbarkeit</h4>
                  </div>

                  <!-- Seed -->
                  <div class="col-span-2">
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Seed (für identische Antworten)
                    </label>
                    <input
                      v-model.number="formData.seed"
                      type="number"
                      placeholder="-1 = Zufällig"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- Stop Sequences -->
                  <div class="col-span-2">
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                      Stop Sequences (eine pro Zeile)
                    </label>
                    <textarea
                      v-model="formData.stopSequences"
                      rows="3"
                      placeholder="z.B.:\n</s>\n<|endoftext|>"
                      class="
                        w-full px-4 py-2 rounded-xl
                        bg-white dark:bg-gray-900
                        border border-gray-300 dark:border-gray-600
                        text-gray-900 dark:text-white
                        focus:ring-2 focus:ring-purple-500 focus:border-transparent
                      "
                    />
                  </div>

                  <!-- ========== Performance Optimization ========== -->
                  <div class="col-span-2 pt-4 border-t border-gray-200 dark:border-gray-700">
                    <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Performance-Optimierung</h4>
                  </div>

                  <!-- Flash Attention -->
                  <div class="col-span-2 flex items-center gap-3 p-3 bg-blue-50 dark:bg-blue-900/20 rounded-xl border border-blue-200 dark:border-blue-800">
                    <input
                      v-model="formData.flashAttention"
                      type="checkbox"
                      id="flashAttention"
                      class="w-5 h-5 rounded text-purple-500 focus:ring-purple-500 focus:ring-offset-0"
                    />
                    <label for="flashAttention" class="text-sm font-medium text-gray-700 dark:text-gray-300 cursor-pointer">
                      Flash Attention aktivieren (schnellere Inferenz)
                    </label>
                  </div>

                  <!-- Low VRAM Mode -->
                  <div class="col-span-2 flex items-center gap-3 p-3 bg-amber-50 dark:bg-amber-900/20 rounded-xl border border-amber-200 dark:border-amber-800">
                    <input
                      v-model="formData.lowVram"
                      type="checkbox"
                      id="lowVram"
                      class="w-5 h-5 rounded text-purple-500 focus:ring-purple-500 focus:ring-offset-0"
                    />
                    <label for="lowVram" class="text-sm font-medium text-gray-700 dark:text-gray-300 cursor-pointer">
                      Low VRAM Mode (reduziert GPU-Speicher)
                    </label>
                  </div>

                  <!-- Memory Mapping -->
                  <div class="col-span-2 flex items-center gap-3 p-3 bg-green-50 dark:bg-green-900/20 rounded-xl border border-green-200 dark:border-green-800">
                    <input
                      v-model="formData.mmapEnabled"
                      type="checkbox"
                      id="mmapEnabled"
                      class="w-5 h-5 rounded text-purple-500 focus:ring-purple-500 focus:ring-offset-0"
                    />
                    <label for="mmapEnabled" class="text-sm font-medium text-gray-700 dark:text-gray-300 cursor-pointer">
                      Memory Mapping aktivieren (schnelleres Laden)
                    </label>
                  </div>

                  <!-- Memory Lock -->
                  <div class="col-span-2 flex items-center gap-3 p-3 bg-red-50 dark:bg-red-900/20 rounded-xl border border-red-200 dark:border-red-800">
                    <input
                      v-model="formData.mlockEnabled"
                      type="checkbox"
                      id="mlockEnabled"
                      class="w-5 h-5 rounded text-purple-500 focus:ring-purple-500 focus:ring-offset-0"
                    />
                    <label for="mlockEnabled" class="text-sm font-medium text-gray-700 dark:text-gray-300 cursor-pointer">
                      Model in RAM sperren (verhindert Swapping)
                    </label>
                  </div>

                </div>
              </Transition>
            </div>

            <!-- Set as Default -->
            <div class="flex items-center gap-3 p-4 bg-yellow-50 dark:bg-yellow-900/20 rounded-xl border border-yellow-200 dark:border-yellow-800">
              <input
                v-model="formData.isDefault"
                type="checkbox"
                id="setDefault"
                class="w-5 h-5 rounded text-purple-500 focus:ring-purple-500 focus:ring-offset-0"
              />
              <label for="setDefault" class="text-sm font-medium text-gray-700 dark:text-gray-300 cursor-pointer">
                Als Standard-Modell setzen
              </label>
            </div>

          </div>

          <!-- Help Tab -->
          <div v-else-if="activeTab === 'help'" class="space-y-6 prose dark:prose-invert max-w-none">
            <h3 class="text-xl font-bold text-gray-900 dark:text-white">llama.cpp Model Parameter-Hilfe</h3>

            <p class="text-sm text-gray-600 dark:text-gray-400">
              Diese Seite erklärt alle verfügbaren Parameter für die GGUF Model-Konfiguration.
            </p>

            <!-- Basic Configuration -->
            <div class="space-y-4">
              <h4 class="text-lg font-semibold text-purple-600 dark:text-purple-400">Basis-Konfiguration</h4>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Context Size (Kontextfenster)</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Bestimmt, wie viele Tokens (Wörter/Zeichen) das Modell gleichzeitig "im Kopf behalten" kann.
                  Größere Werte ermöglichen längere Konversationen, benötigen aber mehr RAM/VRAM.
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-500 mt-2">
                  <strong>Empfehlung:</strong> 8K für normale Chats, 32K+ für lange Dokumente oder Code
                </p>
              </div>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">GPU Layers</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Anzahl der Modell-Schichten, die auf der GPU verarbeitet werden (999 = alle).
                  Mehr GPU-Layer = schneller, benötigt aber VRAM.
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-500 mt-2">
                  <strong>Empfehlung:</strong> 999 (alle) wenn genug VRAM, sonst reduzieren bis es passt
                </p>
              </div>
            </div>

            <!-- Sampling Parameters -->
            <div class="space-y-4">
              <h4 class="text-lg font-semibold text-purple-600 dark:text-purple-400">Sampling-Parameter (Kreativität & Qualität)</h4>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Temperature (0.0 - 2.0)</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Kontrolliert die Zufälligkeit der Antworten. Niedrig = vorhersagbar und fokussiert, Hoch = kreativ und variabel.
                </p>
                <ul class="text-xs text-gray-500 dark:text-gray-500 mt-2 list-disc list-inside">
                  <li>0.1-0.3: Faktisches Wissen, Code, präzise Antworten</li>
                  <li>0.7-0.9: Normale Konversation (Standard)</li>
                  <li>1.0-1.5: Kreatives Schreiben, Brainstorming</li>
                </ul>
              </div>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Top-P / Nucleus Sampling (0.0 - 1.0)</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Berücksichtigt nur die wahrscheinlichsten Tokens bis zur Summe P. Alternative zu Temperature.
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-500 mt-2">
                  <strong>Empfehlung:</strong> 0.9-0.95 für ausgewogene Ergebnisse
                </p>
              </div>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Top-K</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Betrachtet nur die K wahrscheinlichsten Tokens. Reduziert Unsinn bei hoher Temperature.
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-500 mt-2">
                  <strong>Empfehlung:</strong> 40-100, oder deaktiviert wenn Top-P genutzt wird
                </p>
              </div>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Min-P Sampling (0.0 - 1.0)</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Moderne Alternative zu Top-P. Filtert Tokens basierend auf relativer Wahrscheinlichkeit.
                  Oft bessere Ergebnisse als Top-P.
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-500 mt-2">
                  <strong>Empfehlung:</strong> 0.05 (Standard), höher für mehr Filterung
                </p>
              </div>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Repeat Penalty (1.0 - 2.0)</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Bestraft das Wiederholen von Wörtern/Phrasen. Verhindert langweilige Wiederholungen.
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-500 mt-2">
                  <strong>Empfehlung:</strong> 1.1-1.2 für normale Texte, niedriger für Code
                </p>
              </div>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Presence & Frequency Penalty (0.0 - 2.0)</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  <strong>Presence:</strong> Bestraft bereits vorhandene Tokens (fördert neue Themen)<br>
                  <strong>Frequency:</strong> Bestraft häufig verwendete Tokens (reduziert Wiederholungen)
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-500 mt-2">
                  <strong>Empfehlung:</strong> 0.0-0.5, experimentieren für optimale Vielfalt
                </p>
              </div>
            </div>

            <!-- Advanced Sampling -->
            <div class="space-y-4">
              <h4 class="text-lg font-semibold text-purple-600 dark:text-purple-400">Erweiterte Sampling-Methoden</h4>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Mirostat (Mode 0/1/2)</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Alternative Sampling-Methode die automatisch die Entropie (Vorhersagbarkeit) reguliert.
                  Kann zu kohärenteren Langtext-Generierungen führen.
                </p>
                <ul class="text-xs text-gray-500 dark:text-gray-500 mt-2 list-disc list-inside">
                  <li><strong>Tau:</strong> Ziel-Entropie (5.0 = Standard)</li>
                  <li><strong>Eta:</strong> Lernrate der Anpassung (0.1 = Standard)</li>
                </ul>
              </div>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">TFS (Tail Free Sampling)</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Entfernt unwahrscheinliche Tokens am "Schwanz" der Verteilung. 1.0 = deaktiviert.
                </p>
              </div>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Typical P</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Lokale Typikalität - bevorzugt "typische" Tokens. Alternative zu Top-P. 1.0 = deaktiviert.
                </p>
              </div>
            </div>

            <!-- CPU/Threading -->
            <div class="space-y-4">
              <h4 class="text-lg font-semibold text-purple-600 dark:text-purple-400">CPU/Threading Konfiguration</h4>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Threads (CPU-Kerne)</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Anzahl der CPU-Threads für Berechnungen. 0 = automatische Erkennung.
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-500 mt-2">
                  <strong>Empfehlung:</strong> 0 (Auto) oder Anzahl physischer Kerne
                </p>
              </div>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Batch Size</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Anzahl gleichzeitig verarbeiteter Tokens beim Prompt-Processing. Höher = schneller, aber mehr RAM.
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-500 mt-2">
                  <strong>Empfehlung:</strong> 512 (Standard), bis 2048 wenn genug RAM
                </p>
              </div>
            </div>

            <!-- RoPE Scaling -->
            <div class="space-y-4">
              <h4 class="text-lg font-semibold text-purple-600 dark:text-purple-400">RoPE Scaling (Context-Erweiterung)</h4>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">RoPE Frequency Base & Scale</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Fortgeschrittene Parameter zum Erweitern des Kontextfensters über das trainierte Limit hinaus.
                </p>
                <ul class="text-xs text-gray-500 dark:text-gray-500 mt-2 list-disc list-inside">
                  <li><strong>Freq Base:</strong> Basis-Frequenz (10000 Standard)</li>
                  <li><strong>Freq Scale:</strong> Skalierungsfaktor, &lt;1.0 erweitert Context</li>
                </ul>
                <p class="text-xs text-red-500 dark:text-red-400 mt-2">
                  <strong>⚠️ Achtung:</strong> Nur ändern wenn du verstehst was du tust! Kann Qualität beeinträchtigen.
                </p>
              </div>
            </div>

            <!-- Reproducibility -->
            <div class="space-y-4">
              <h4 class="text-lg font-semibold text-purple-600 dark:text-purple-400">Reproduzierbarkeit</h4>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Seed</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Fester Zufallswert für identische Antworten bei gleichen Inputs. -1 = zufällig (Standard).
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-500 mt-2">
                  <strong>Nutzung:</strong> Für Tests oder wenn exakt gleiche Outputs gewünscht
                </p>
              </div>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Stop Sequences</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Zeichenketten bei denen die Generation stoppt (z.B. "&lt;/s&gt;", "&lt;|endoftext|&gt;").
                  Eine Sequenz pro Zeile.
                </p>
              </div>
            </div>

            <!-- Performance -->
            <div class="space-y-4">
              <h4 class="text-lg font-semibold text-purple-600 dark:text-purple-400">Performance-Optimierung</h4>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Flash Attention</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Optimierte Attention-Berechnung für schnellere Inferenz bei neueren GPUs.
                </p>
                <p class="text-xs text-green-500 dark:text-green-400 mt-2">
                  <strong>✓ Empfohlen:</strong> Aktivieren wenn unterstützt (RTX 30/40, AMD RDNA3)
                </p>
              </div>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Low VRAM Mode</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Reduziert GPU-Speicherverbrauch auf Kosten von Geschwindigkeit.
                </p>
                <p class="text-xs text-gray-500 dark:text-gray-500 mt-2">
                  <strong>Nutzung:</strong> Wenn Modell nicht in VRAM passt oder OOM-Fehler auftreten
                </p>
              </div>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Memory Mapping (mmap)</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Lädt Modell direkt von Disk ohne vollständiges RAM-Laden. Schnellerer Start, evtl. langsamer bei Ausführung.
                </p>
                <p class="text-xs text-green-500 dark:text-green-400 mt-2">
                  <strong>✓ Empfohlen:</strong> Aktiviert lassen (Standard)
                </p>
              </div>

              <div class="bg-gray-50 dark:bg-gray-800 p-4 rounded-lg">
                <h5 class="font-semibold text-gray-900 dark:text-white">Memory Lock (mlock)</h5>
                <p class="text-sm text-gray-600 dark:text-gray-400 mt-1">
                  Sperrt Modell im RAM, verhindert Swapping auf Disk. Schneller aber mehr RAM-Verbrauch.
                </p>
                <p class="text-xs text-amber-500 dark:text-amber-400 mt-2">
                  <strong>⚠ Vorsicht:</strong> Nur aktivieren wenn genug RAM (Modellgröße + 2GB Reserve)
                </p>
              </div>
            </div>
          </div>

        </div>

        <!-- Footer Actions -->
        <div class="
          p-6 border-t border-gray-200/50 dark:border-gray-700/50
          bg-gray-50/50 dark:bg-gray-900/50
          flex justify-end items-center gap-3
        ">
          <button
            @click="$emit('close')"
            class="
              px-6 py-2.5 rounded-xl
              text-gray-700 dark:text-gray-300
              bg-white dark:bg-gray-800
              border border-gray-300 dark:border-gray-600
              hover:bg-gray-50 dark:hover:bg-gray-700
              font-medium
              shadow-sm hover:shadow-md
              transition-all duration-200
              transform hover:scale-105 active:scale-95
            "
          >
            Abbrechen
          </button>
          <button
            @click="saveConfig"
            :disabled="!isFormValid"
            class="
              px-6 py-2.5 rounded-xl
              bg-gradient-to-r from-purple-500 to-indigo-500
              hover:from-purple-600 hover:to-indigo-600
              text-white font-medium
              shadow-sm hover:shadow-md
              transition-all duration-200
              transform hover:scale-105 active:scale-95
              disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none
              flex items-center gap-2
            "
          >
            <CheckIcon class="w-5 h-5" />
            <span>{{ editMode ? 'Speichern' : 'Erstellen' }}</span>
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import {
  XMarkIcon,
  CpuChipIcon,
  DocumentArrowUpIcon,
  ChevronDownIcon,
  CheckIcon
} from '@heroicons/vue/24/outline'
import api from '../services/api'
import { useToast } from '../composables/useToast'

const props = defineProps({
  show: Boolean,
  config: Object, // For edit mode
  availableGgufModels: Array
})

const emit = defineEmits(['close', 'saved'])

const { success, error: errorToast } = useToast()
const showAdvanced = ref(false)
const uploadedFileName = ref('')
const activeTab = ref('config') // 'config' or 'help'

const editMode = computed(() => !!props.config)

const contextSizes = [
  { label: '4K', value: 4096, tokens: '4,096' },
  { label: '8K', value: 8192, tokens: '8,192' },
  { label: '16K', value: 16384, tokens: '16,384' },
  { label: '32K', value: 32768, tokens: '32,768' },
  { label: '64K', value: 65536, tokens: '65,536' },
  { label: '128K', value: 131072, tokens: '131,072' }
]

const formData = ref({
  name: '',
  baseModel: '',
  description: '',
  category: '',
  tags: '',
  systemPrompt: '',
  contextSize: 8192, // Default to 8K
  gpuLayers: 999,
  // Basic Sampling
  temperature: null,
  topP: null,
  topK: null,
  repeatPenalty: null,
  maxTokens: null,
  // CPU/Threading
  threads: null,
  batchSize: null,
  // RoPE Scaling
  ropeFreqBase: null,
  ropeFreqScale: null,
  // Mirostat Sampling
  mirostat: null,
  mirostatTau: null,
  mirostatEta: null,
  // Advanced Sampling
  tfsZ: null,
  typicalP: null,
  presencePenalty: null,
  frequencyPenalty: null,
  minP: null,
  // Reproducibility
  seed: null,
  stopSequences: '',
  // Performance
  flashAttention: false,
  lowVram: false,
  mmapEnabled: true,
  mlockEnabled: false,
  isDefault: false
})

// Watch for config changes (edit mode)
watch(() => props.config, (newConfig) => {
  if (newConfig) {
    formData.value = {
      name: newConfig.name || '',
      baseModel: newConfig.baseModel || '',
      description: newConfig.description || '',
      category: newConfig.category || '',
      tags: newConfig.tags || '',
      systemPrompt: newConfig.systemPrompt || '',
      contextSize: newConfig.contextSize || 8192,
      gpuLayers: newConfig.gpuLayers ?? 999,
      temperature: newConfig.temperature,
      topP: newConfig.topP,
      topK: newConfig.topK,
      repeatPenalty: newConfig.repeatPenalty,
      maxTokens: newConfig.maxTokens,
      threads: newConfig.threads,
      batchSize: newConfig.batchSize,
      ropeFreqBase: newConfig.ropeFreqBase,
      ropeFreqScale: newConfig.ropeFreqScale,
      mirostat: newConfig.mirostat,
      mirostatTau: newConfig.mirostatTau,
      mirostatEta: newConfig.mirostatEta,
      tfsZ: newConfig.tfsZ,
      typicalP: newConfig.typicalP,
      presencePenalty: newConfig.presencePenalty,
      frequencyPenalty: newConfig.frequencyPenalty,
      minP: newConfig.minP,
      seed: newConfig.seed,
      stopSequences: newConfig.stopSequences || '',
      flashAttention: newConfig.flashAttention || false,
      lowVram: newConfig.lowVram || false,
      mmapEnabled: newConfig.mmapEnabled ?? true,
      mlockEnabled: newConfig.mlockEnabled || false,
      isDefault: newConfig.isDefault || false
    }
  } else {
    // Reset form for new config
    formData.value = {
      name: '',
      baseModel: '',
      description: '',
      category: '',
      tags: '',
      systemPrompt: '',
      contextSize: 8192,
      gpuLayers: 999,
      temperature: null,
      topP: null,
      topK: null,
      repeatPenalty: null,
      maxTokens: null,
      threads: null,
      batchSize: null,
      ropeFreqBase: null,
      ropeFreqScale: null,
      mirostat: null,
      mirostatTau: null,
      mirostatEta: null,
      tfsZ: null,
      typicalP: null,
      presencePenalty: null,
      frequencyPenalty: null,
      minP: null,
      seed: null,
      stopSequences: '',
      flashAttention: false,
      lowVram: false,
      mmapEnabled: true,
      mlockEnabled: false,
      isDefault: false
    }
    uploadedFileName.value = ''
  }
}, { immediate: true })

const isFormValid = computed(() => {
  return formData.value.name.trim() !== '' &&
         formData.value.baseModel !== '' &&
         formData.value.contextSize > 0
})

async function handleFileUpload(event) {
  const file = event.target.files[0]
  if (!file) return

  // Create FormData for upload
  const uploadFormData = new FormData()
  uploadFormData.append('file', file)

  try {
    const response = await api.uploadGgufPrompt(uploadFormData)
    formData.value.systemPrompt = response.content
    uploadedFileName.value = response.filename
    success(`Prompt aus "${response.filename}" geladen`)
  } catch (error) {
    console.error('Failed to upload prompt file:', error)
    errorToast('Fehler beim Hochladen: ' + error.message)
  }

  // Reset file input
  event.target.value = ''
}

async function saveConfig() {
  if (!isFormValid.value) return

  try {
    if (editMode.value) {
      // Update existing config
      await api.updateGgufModelConfig(props.config.id, formData.value)
      success('GGUF Model-Konfiguration aktualisiert')
    } else {
      // Create new config
      await api.createGgufModelConfig(formData.value)
      success('GGUF Model-Konfiguration erstellt')
    }

    emit('saved')
    emit('close')
  } catch (error) {
    console.error('Failed to save GGUF model config:', error)
    errorToast('Fehler beim Speichern: ' + error.message)
  }
}
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
  max-height: 1000px;
  overflow: hidden;
}

.slide-enter-from,
.slide-leave-to {
  max-height: 0;
  opacity: 0;
}
</style>
