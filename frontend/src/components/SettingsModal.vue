<template>
  <Transition name="modal">
    <div v-if="isOpen" class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4" @click.self="close">
      <div class="bg-white/95 dark:bg-gray-800/95 backdrop-blur-xl rounded-2xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-hidden border border-gray-200/50 dark:border-gray-700/50">
        <!-- Header with Gradient -->
        <div class="sticky top-0 bg-gradient-to-r from-fleet-orange-500/10 to-orange-500/10 dark:from-fleet-orange-500/20 dark:to-orange-500/20 backdrop-blur-sm border-b border-gray-200/50 dark:border-gray-700/50 px-6 py-4 flex justify-between items-center z-10">
          <div class="flex items-center gap-3">
            <div class="p-2 rounded-xl bg-gradient-to-br from-fleet-orange-500 to-orange-600 shadow-lg">
              <Cog6ToothIcon class="w-6 h-6 text-white" />
            </div>
            <h2 class="text-2xl font-bold text-gray-900 dark:text-white">Einstellungen</h2>
          </div>
          <button
            @click="close"
            class="p-2 rounded-lg text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-all transform hover:scale-110"
          >
            <XMarkIcon class="w-6 h-6" />
          </button>
        </div>

        <!-- Tab Navigation -->
        <div class="flex flex-wrap border-b border-gray-200 dark:border-gray-700 px-6 bg-gray-50/50 dark:bg-gray-900/50 gap-1">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            @click="activeTab = tab.id"
            class="flex items-center gap-2 px-3 py-2 text-sm font-medium transition-all relative whitespace-nowrap rounded-lg"
            :class="activeTab === tab.id
              ? 'text-fleet-orange-600 dark:text-fleet-orange-400'
              : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200'"
          >
            <component :is="tab.icon" class="w-4 h-4" />
            {{ tab.label }}
            <div
              v-if="activeTab === tab.id"
              class="absolute bottom-0 left-0 right-0 h-0.5 bg-fleet-orange-500"
            />
          </button>
        </div>

        <!-- Content with Custom Scrollbar -->
        <div class="overflow-y-auto p-6 space-y-6 custom-scrollbar" style="max-height: calc(90vh - 220px);">

          <!-- TAB: General Settings -->
          <div v-if="activeTab === 'general'">
          <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
              <GlobeAltIcon class="w-5 h-5 text-blue-500" />
              Allgemein
            </h3>

            <!-- Language -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-2">
                <LanguageIcon class="w-4 h-4" />
                Sprache
              </label>
              <select
                v-model="settings.language"
                class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-fleet-orange-500 focus:border-transparent"
              >
                <option value="de">🇩🇪 Deutsch</option>
                <option value="en">🇬🇧 English</option>
              </select>
            </div>

            <!-- Theme -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-2">
                <SunIcon class="w-4 h-4" />
                Theme
              </label>
              <select
                v-model="settings.theme"
                class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-fleet-orange-500 focus:border-transparent"
              >
                <option value="light">☀️ Hell</option>
                <option value="dark">🌙 Dunkel</option>
                <option value="auto">🔄 System</option>
              </select>
            </div>

            <!-- TopBar Toggle -->
            <div class="mb-4 p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
              <div class="flex items-center justify-between">
                <div class="flex-1">
                  <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-2">
                    <Bars3Icon class="w-4 h-4 text-blue-500" />
                    TopBar anzeigen
                  </label>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    Zeigt die obere Navigationsleiste mit Modellauswahl und Buttons
                  </p>
                </div>
                <label class="relative inline-flex items-center cursor-pointer">
                  <input type="checkbox" v-model="settings.showTopBar" @change="saveTopBarSetting" class="sr-only peer">
                  <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-fleet-orange-300 dark:peer-focus:ring-fleet-orange-800 rounded-full peer dark:bg-gray-600 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-500 peer-checked:bg-fleet-orange-500"></div>
                </label>
              </div>
            </div>

            <!-- Willkommen-Anzeige Toggle -->
            <div class="mb-4 p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
              <div class="flex items-center justify-between">
                <div class="flex-1">
                  <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-2">
                    <SparklesIcon class="w-4 h-4 text-fleet-orange-500" />
                    Willkommen-Anzeige
                  </label>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    {{ settings.showWelcomeTiles
                      ? 'Zeigt Logo, Tipps und Vorschläge für Einsteiger'
                      : 'Zeigt personalisierte Begrüßung mit deinem Namen' }}
                  </p>
                </div>
                <label class="relative inline-flex items-center cursor-pointer">
                  <input type="checkbox" v-model="settings.showWelcomeTiles" class="sr-only peer">
                  <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-fleet-orange-300 dark:peer-focus:ring-fleet-orange-800 rounded-full peer dark:bg-gray-600 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-500 peer-checked:bg-fleet-orange-500"></div>
                </label>
              </div>
            </div>

            <!-- Schriftgröße -->
            <div class="mb-4 p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
              <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-2">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16m-7 6h7" />
                </svg>
                Schriftgröße
              </label>
              <p class="text-xs text-gray-500 dark:text-gray-400 mb-3">
                Ändert die Textgröße in der gesamten Anwendung
              </p>
              <div class="grid grid-cols-4 gap-2">
                <button
                  type="button"
                  @click="setFontSize('small')"
                  :class="[
                    'px-3 py-2 rounded-lg border-2 transition-all text-center',
                    settings.fontSize === 'small'
                      ? 'border-blue-500 bg-blue-500/10 text-blue-600 dark:text-blue-400'
                      : 'border-gray-300 dark:border-gray-600 hover:border-blue-400 text-gray-700 dark:text-gray-300'
                  ]"
                >
                  <span class="text-xs font-medium">Klein</span>
                </button>
                <button
                  type="button"
                  @click="setFontSize('medium')"
                  :class="[
                    'px-3 py-2 rounded-lg border-2 transition-all text-center',
                    settings.fontSize === 'medium' || !settings.fontSize
                      ? 'border-blue-500 bg-blue-500/10 text-blue-600 dark:text-blue-400'
                      : 'border-gray-300 dark:border-gray-600 hover:border-blue-400 text-gray-700 dark:text-gray-300'
                  ]"
                >
                  <span class="text-sm font-medium">Normal</span>
                </button>
                <button
                  type="button"
                  @click="setFontSize('large')"
                  :class="[
                    'px-3 py-2 rounded-lg border-2 transition-all text-center',
                    settings.fontSize === 'large'
                      ? 'border-blue-500 bg-blue-500/10 text-blue-600 dark:text-blue-400'
                      : 'border-gray-300 dark:border-gray-600 hover:border-blue-400 text-gray-700 dark:text-gray-300'
                  ]"
                >
                  <span class="text-base font-medium">Groß</span>
                </button>
                <button
                  type="button"
                  @click="setFontSize('xlarge')"
                  :class="[
                    'px-3 py-2 rounded-lg border-2 transition-all text-center',
                    settings.fontSize === 'xlarge'
                      ? 'border-blue-500 bg-blue-500/10 text-blue-600 dark:text-blue-400'
                      : 'border-gray-300 dark:border-gray-600 hover:border-blue-400 text-gray-700 dark:text-gray-300'
                  ]"
                >
                  <span class="text-lg font-medium">XL</span>
                </button>
              </div>
            </div>

            <!-- UI Theme / Design Style -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-2">
                <SparklesIcon class="w-4 h-4" />
                Erscheinungsbild
              </label>
              <div class="grid grid-cols-2 gap-3">
                <!-- Tech Theme -->
                <button
                  type="button"
                  @click="settings.uiTheme = 'default'"
                  :class="[
                    'p-3 rounded-xl border-2 transition-all text-left',
                    settings.uiTheme === 'default' || !settings.uiTheme
                      ? 'border-purple-500 bg-purple-500/10'
                      : 'border-gray-300 dark:border-gray-600 hover:border-purple-400'
                  ]"
                >
                  <div class="flex items-center gap-2 mb-1">
                    <div class="w-6 h-6 rounded-full bg-gradient-to-br from-purple-500 to-indigo-600"></div>
                    <span class="font-medium text-sm text-gray-900 dark:text-white">Tech</span>
                  </div>
                  <p class="text-xs text-gray-500 dark:text-gray-400">
                    Dunkles Design mit lila Akzenten
                  </p>
                </button>

                <!-- Lawyer Theme -->
                <button
                  type="button"
                  @click="settings.uiTheme = 'lawyer'"
                  :class="[
                    'p-3 rounded-xl border-2 transition-all text-left',
                    settings.uiTheme === 'lawyer'
                      ? 'border-blue-800 bg-blue-100 dark:bg-blue-900/30'
                      : 'border-gray-300 dark:border-gray-600 hover:border-blue-600'
                  ]"
                >
                  <div class="flex items-center gap-2 mb-1">
                    <div class="w-6 h-6 rounded-full bg-gradient-to-br from-blue-900 to-blue-950 border-2 border-amber-500"></div>
                    <span class="font-medium text-sm text-gray-900 dark:text-white">Anwalt</span>
                  </div>
                  <p class="text-xs text-gray-500 dark:text-gray-400">
                    Helles Design mit Navy & Gold
                  </p>
                </button>
              </div>
            </div>
          </section>
          </div>

          <!-- TAB: LLM Provider -->
          <div v-if="activeTab === 'providers'">
            <ProviderSettings />
          </div>

          <!-- TAB: Model Selection -->
          <div v-if="activeTab === 'models'">
          <!-- Fleet Mates Model Assignment -->
          <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
              <CpuChipIcon class="w-5 h-5 text-purple-500" />
              Fleet Mates - Modell-Zuordnung
            </h3>

            <p class="text-sm text-gray-600 dark:text-gray-400 mb-4">
              Weise jedem Fleet Mate ein spezifisches Modell zu. Kleine, schnelle Modelle (1B-7B) sind für Mate-Aufgaben empfohlen.
            </p>

            <div class="space-y-4">
              <!-- Email Model (Thunderbird Mate) -->
              <div class="p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-2">
                  <span class="text-xl">📧</span>
                  Email-Modell
                  <span class="text-xs text-gray-500 dark:text-gray-400">(Thunderbird Mate)</span>
                </label>
                <select
                  v-model="mateModels.emailModel"
                  @change="saveMateModels"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">-- Standard (llama3.2:3b) --</option>
                  <option v-for="model in fastModels" :key="model.name" :value="model.name">
                    {{ model.name }} ({{ formatSize(model.size) }})
                  </option>
                </select>
                <p class="text-xs text-gray-500 mt-1">Für Email-Klassifizierung und Antwort-Generierung</p>
              </div>

              <!-- Document Model (Writer Mate) -->
              <div class="p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-2">
                  <span class="text-xl">✍️</span>
                  Document-Modell
                  <span class="text-xs text-gray-500 dark:text-gray-400">(Writer Mate)</span>
                </label>
                <select
                  v-model="mateModels.documentModel"
                  @change="saveMateModels"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-green-500"
                >
                  <option value="">-- Standard --</option>
                  <option v-for="model in availableModels" :key="model.name" :value="model.name">
                    {{ model.name }} ({{ formatSize(model.size) }})
                  </option>
                </select>
                <p class="text-xs text-gray-500 mt-1">Für Brief- und Dokumenten-Generierung in LibreOffice</p>
              </div>

              <!-- Log Analysis Model (OS Mate) -->
              <div class="p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2 flex items-center gap-2">
                  <span class="text-xl">📊</span>
                  Log-Analyse-Modell
                  <span class="text-xs text-gray-500 dark:text-gray-400">(OS Mate)</span>
                </label>
                <select
                  v-model="mateModels.logAnalysisModel"
                  @change="saveMateModels"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-orange-500"
                >
                  <option value="">-- Standard --</option>
                  <option v-for="model in availableModels" :key="model.name" :value="model.name">
                    {{ model.name }} ({{ formatSize(model.size) }})
                  </option>
                </select>
                <p class="text-xs text-gray-500 mt-1">Für Log-Datei-Analyse und Fehlersuche</p>
              </div>
            </div>

            <div class="mt-4 p-3 rounded-xl bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700/50">
              <div class="flex items-start gap-2">
                <InformationCircleIcon class="w-5 h-5 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" />
                <p class="text-xs text-blue-800 dark:text-blue-200">
                  <strong>Tipp:</strong> Für Email-Klassifizierung eignen sich schnelle Modelle wie <code>llama3.2:3b</code> oder <code>qwen2.5:7b</code>.
                </p>
              </div>
            </div>
          </section>

          <!-- Hardware & Performance Settings -->
          <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm mt-6">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
              <CpuChipIcon class="w-5 h-5 text-orange-500" />
              Hardware & Performance
            </h3>

            <!-- CPU-Only Mode Toggle -->
            <div class="p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
              <div class="flex items-center justify-between">
                <div class="flex-1">
                  <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-2">
                    <CpuChipIcon class="w-4 h-4 text-orange-500" />
                    CPU-Modus (ohne GPU)
                  </label>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    Deaktiviert CUDA/GPU für Demos auf Laptops ohne NVIDIA
                  </p>
                </div>
                <ToggleSwitch v-model="settings.cpuOnly" color="orange" />
              </div>
            </div>

            <!-- Info Box -->
            <div v-if="settings.cpuOnly" class="mt-3 p-3 rounded-lg bg-orange-50 dark:bg-orange-900/30 border border-orange-200 dark:border-orange-800">
              <p class="text-sm text-orange-800 dark:text-orange-200 flex items-center gap-2">
                <ExclamationTriangleIcon class="w-4 h-4 flex-shrink-0" />
                <span><strong>CPU-Modus aktiv:</strong> Antworten sind langsamer, ideal für Demos ohne NVIDIA GPU.</span>
              </p>
            </div>
            <div v-else class="mt-3 p-3 rounded-lg bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-800">
              <p class="text-sm text-green-800 dark:text-green-200 flex items-center gap-2">
                <CheckCircleIcon class="w-4 h-4 flex-shrink-0" />
                <span><strong>GPU aktiv:</strong> Maximale Performance mit NVIDIA CUDA.</span>
              </p>
            </div>
          </section>
          </div>

          <!-- TAB: Model Parameters -->
          <div v-if="activeTab === 'parameters'">
          <!-- Model Parameters -->
          <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
              <AdjustmentsHorizontalIcon class="w-5 h-5 text-orange-500" />
              🎛️ LLM Sampling Parameter
            </h3>

            <SimpleSamplingParams
              v-model="samplingParams"
            />
          </section>
          </div>

          <!-- TAB: Templates / System Prompts -->
          <div v-if="activeTab === 'templates'">
            <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm">
              <div class="flex items-center justify-between mb-4">
                <div>
                  <h3 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                    <DocumentTextIcon class="w-5 h-5 text-blue-500" />
                    System-Prompts Verwaltung
                  </h3>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    Erstelle und verwalte wiederverwendbare System-Prompts für deine Chats
                  </p>
                </div>
                <button
                  @click="showPromptEditor = true; editingPrompt = null; resetPromptForm()"
                  class="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white text-sm rounded-lg transition-colors flex items-center gap-2"
                >
                  <DocumentDuplicateIcon class="w-4 h-4" />
                  Neuer Prompt
                </button>
              </div>

              <!-- Prompts List -->
              <div v-if="systemPrompts.length === 0" class="text-center py-12 text-gray-500 dark:text-gray-400">
                <DocumentTextIcon class="w-16 h-16 mx-auto mb-3 opacity-20" />
                <p class="font-medium">Keine System-Prompts vorhanden</p>
                <p class="text-xs mt-2">Erstelle deinen ersten System-Prompt mit dem Button oben</p>
              </div>

              <div v-else class="space-y-2">
                <div
                  v-for="prompt in systemPrompts"
                  :key="prompt.id"
                  class="border border-gray-200 dark:border-gray-700 rounded-lg p-3 hover:border-blue-400 dark:hover:border-blue-600 transition-colors bg-white dark:bg-gray-800"
                >
                  <div class="flex items-start justify-between gap-3">
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2 mb-1">
                        <h4 class="font-semibold text-sm text-gray-900 dark:text-white">
                          {{ prompt.name }}
                        </h4>
                        <span v-if="prompt.isDefault" class="px-1.5 py-0.5 bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-200 text-xs rounded">
                          Standard
                        </span>
                      </div>
                      <p class="text-xs text-gray-600 dark:text-gray-400 line-clamp-2">
                        {{ prompt.content }}
                      </p>
                    </div>
                    <div class="flex items-center gap-1">
                      <!-- Aktivieren Button -->
                      <button
                        @click="activateSystemPrompt(prompt)"
                        class="p-1.5 rounded transition-colors"
                        :class="prompt.isDefault
                          ? 'text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-900/30'
                          : 'text-gray-400 dark:text-gray-500 hover:text-green-600 dark:hover:text-green-400 hover:bg-green-50 dark:hover:bg-green-900/20'"
                        :title="prompt.isDefault ? 'Aktiver Prompt' : 'Als Standard aktivieren'"
                      >
                        <CheckCircleIcon class="w-4 h-4" />
                      </button>
                      <button
                        @click="editSystemPrompt(prompt)"
                        class="p-1.5 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded transition-colors"
                        title="Bearbeiten"
                      >
                        <WrenchScrewdriverIcon class="w-4 h-4" />
                      </button>
                      <button
                        @click="deleteSystemPrompt(prompt.id)"
                        class="p-1.5 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 rounded transition-colors"
                        title="Löschen"
                      >
                        <TrashIcon class="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </section>
          </div>

          <!-- TAB: Personal Info -->
          <div v-if="activeTab === 'personal'">
            <PersonalInfoTab ref="personalInfoTabRef" />
          </div>

          <!-- TAB: Agents -->
          <div v-if="activeTab === 'agents'">
          <!-- Vision Settings -->
          <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
              <PhotoIcon class="w-5 h-5 text-indigo-500" />
              Vision Model
            </h3>

            <!-- Auto-select Vision Model -->
            <div class="mb-4 p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
              <div class="flex items-center justify-between">
                <div class="flex-1">
                  <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-2">
                    <EyeIcon class="w-4 h-4" />
                    Auto Vision Model
                  </label>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    Automatisch Vision Model bei Bild-Upload wählen
                  </p>
                </div>
                <ToggleSwitch v-model="settings.autoSelectVisionModel" color="indigo" />
              </div>
            </div>

            <!-- Preferred Vision Model -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Bevorzugtes Vision Model
              </label>
              <select
                v-model="modelSelectionSettings.visionModel"
                class="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-indigo-500"
              >
                <option v-for="model in visionModels" :key="model" :value="model">
                  {{ model }}
                </option>
              </select>
              <p v-if="visionModels.length > 0" class="mt-2 text-xs text-gray-500 dark:text-gray-400">
                {{ visionModels.length }} Vision-Modelle verfügbar
              </p>
              <p v-else class="mt-2 text-xs text-yellow-600 dark:text-yellow-400">
                ⚠️ Keine Vision-Modelle gefunden. Lade Vision-Modelle aus dem Model Store herunter.
              </p>
            </div>

            <!-- Vision Chaining -->
            <div class="mb-4 p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
              <div class="flex items-center justify-between">
                <div class="flex-1">
                  <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-2">
                    <LinkIcon class="w-4 h-4" />
                    Vision-Chaining
                  </label>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    Vision Model Output → Haupt-Model
                  </p>
                </div>
                <ToggleSwitch v-model="modelSelectionSettings.visionChainingEnabled" color="indigo" />
              </div>
            </div>
          </section>

          <!-- OS Mate - File Search (RAG) -->
          <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm mt-6">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
              <FolderIcon class="w-5 h-5 text-amber-500" />
              OS Mate - Dateisuche (RAG)
            </h3>
            <p class="text-sm text-gray-600 dark:text-gray-400 mb-4">
              Konfiguriere Ordner für die lokale Dokumentensuche. Der Index durchsucht PDF, DOCX und ODT Dateien.
            </p>

            <!-- File Search Status -->
            <div v-if="fileSearchStatus" class="mb-4 p-3 rounded-lg bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-800">
              <div class="flex items-center gap-2 text-sm text-blue-700 dark:text-blue-300">
                <span v-if="fileSearchStatus.indexingInProgress" class="animate-spin">⏳</span>
                <span v-else>📚</span>
                <span>
                  {{ fileSearchStatus.indexedFileCount }} Dateien indexiert
                  <span v-if="fileSearchStatus.locateAvailable" class="text-xs text-green-600 dark:text-green-400 ml-2">(locate verfügbar)</span>
                </span>
              </div>
            </div>

            <!-- Search Folders List -->
            <div class="space-y-3 mb-4">
              <div v-for="folder in fileSearchFolders" :key="folder.folderId"
                   class="p-3 rounded-lg bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700 flex items-center justify-between">
                <div class="flex items-center gap-3 flex-1">
                  <FolderOpenIcon class="w-5 h-5 text-amber-500" />
                  <div class="flex-1 min-w-0">
                    <div class="font-medium text-gray-900 dark:text-white truncate">{{ folder.name }}</div>
                    <div class="text-xs text-gray-500 dark:text-gray-400 truncate">{{ folder.folderPath }}</div>
                    <div class="text-xs text-gray-400 dark:text-gray-500">
                      {{ folder.fileCount || 0 }} Dateien
                      <span v-if="folder.lastIndexed" class="ml-2">• Indexiert: {{ formatDateAbsolute(folder.lastIndexed) }}</span>
                    </div>
                  </div>
                </div>
                <div class="flex items-center gap-2">
                  <button @click="reindexFolder(folder.folderId)"
                          class="p-1.5 rounded hover:bg-blue-100 dark:hover:bg-blue-900/30 text-blue-600 dark:text-blue-400"
                          title="Neu indexieren">
                    <ArrowPathIcon class="w-4 h-4" />
                  </button>
                  <button @click="removeSearchFolder(folder.folderId)"
                          class="p-1.5 rounded hover:bg-red-100 dark:hover:bg-red-900/30 text-red-600 dark:text-red-400"
                          title="Entfernen">
                    <TrashIcon class="w-4 h-4" />
                  </button>
                </div>
              </div>

              <div v-if="fileSearchFolders.length === 0"
                   class="p-4 rounded-lg bg-gray-100/50 dark:bg-gray-800/30 border border-dashed border-gray-300 dark:border-gray-600 text-center">
                <FolderIcon class="w-8 h-8 text-gray-400 mx-auto mb-2" />
                <p class="text-sm text-gray-500 dark:text-gray-400">Keine Ordner konfiguriert</p>
              </div>
            </div>

            <!-- Add Folder Form -->
            <div class="p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Ordner hinzufügen</label>
              <div class="flex gap-2">
                <input type="text" v-model="newFolderPath"
                       placeholder="/pfad/zum/ordner"
                       class="flex-1 px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500" />
                <button @click="addSearchFolder"
                        :disabled="!newFolderPath"
                        class="px-4 py-2 bg-amber-500 hover:bg-amber-600 disabled:bg-gray-300 disabled:cursor-not-allowed text-white rounded-lg text-sm font-medium transition-colors">
                  Hinzufügen
                </button>
              </div>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-2">
                Beispiel: /home/user/documents oder ~/Dokumente
              </p>
            </div>
          </section>
          </div>

          <!-- TAB: Web Search Settings -->
          <div v-if="activeTab === 'web-search'">
            <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm">
              <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                <MagnifyingGlassIcon class="w-5 h-5 text-blue-500" />
                Web-Suche (RAG)
              </h3>

              <!-- Suchzähler Tiles - Brave und SearXNG -->
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
                <!-- Brave Search API Zähler -->
                <div class="p-4 rounded-xl bg-gradient-to-r from-orange-500/10 to-amber-500/10 border-2 transition-all" :class="webSearchSettings.braveConfigured ? 'border-orange-500 dark:border-orange-400 shadow-lg shadow-orange-500/20' : 'border-orange-300/50 dark:border-orange-600/30'">
                  <div class="flex items-center gap-3 mb-3">
                    <div class="p-2 rounded-lg bg-orange-500/20">
                      <StarIcon class="w-6 h-6 text-orange-500" />
                    </div>
                    <div class="flex-1">
                      <h4 class="font-bold text-gray-900 dark:text-white flex items-center gap-2">
                        Brave Search API
                        <span v-if="webSearchSettings.braveConfigured" class="text-xs px-1.5 py-0.5 bg-orange-500 text-white rounded font-bold animate-pulse">
                          PRIMÄR
                        </span>
                        <span v-else class="text-xs px-1.5 py-0.5 bg-gray-200 dark:bg-gray-700 text-gray-500 dark:text-gray-400 rounded">
                          Nicht konfiguriert
                        </span>
                      </h4>
                      <p class="text-xs text-gray-500 dark:text-gray-400">
                        {{ webSearchSettings.currentMonth || 'Aktueller Monat' }}
                      </p>
                    </div>
                  </div>
                  <div class="text-right mb-2">
                    <div class="text-2xl font-bold" :class="searchCountColor">
                      {{ webSearchSettings.searchCount || 0 }} / {{ webSearchSettings.searchLimit || 2000 }}
                    </div>
                    <p class="text-xs text-gray-500 dark:text-gray-400">
                      {{ webSearchSettings.remainingSearches || 2000 }} verbleibend
                    </p>
                  </div>
                  <!-- Progress Bar -->
                  <div class="h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                    <div
                      class="h-full transition-all duration-500"
                      :class="searchCountColor.replace('text-', 'bg-')"
                      :style="{ width: searchCountPercent + '%' }"
                    ></div>
                  </div>
                </div>

                <!-- SearXNG Zähler -->
                <div class="p-4 rounded-xl bg-gradient-to-r from-green-500/10 to-emerald-500/10 border-2 transition-all" :class="!webSearchSettings.braveConfigured ? 'border-green-500 dark:border-green-400 shadow-lg shadow-green-500/20' : 'border-green-300/50 dark:border-green-600/30'">
                  <div class="flex items-center gap-3 mb-3">
                    <div class="p-2 rounded-lg bg-green-500/20">
                      <ServerIcon class="w-6 h-6 text-green-500" />
                    </div>
                    <div class="flex-1">
                      <h4 class="font-bold text-gray-900 dark:text-white flex items-center gap-2 flex-wrap">
                        SearXNG
                        <span v-if="!webSearchSettings.braveConfigured" class="text-xs px-1.5 py-0.5 bg-green-500 text-white rounded font-bold animate-pulse">
                          PRIMÄR
                        </span>
                        <span v-else class="text-xs px-1.5 py-0.5 bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400 rounded">
                          Fallback
                        </span>
                        <span v-if="webSearchSettings.customSearxngInstance" class="text-xs px-1.5 py-0.5 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 rounded">
                          Eigene Instanz
                        </span>
                      </h4>
                      <p class="text-xs text-gray-500 dark:text-gray-400 truncate max-w-[180px]" :title="webSearchSettings.customSearxngInstance || 'Öffentliche Instanzen'">
                        {{ webSearchSettings.customSearxngInstance || 'Öffentliche Instanzen' }}
                      </p>
                    </div>
                  </div>
                  <div class="flex justify-between items-end">
                    <div>
                      <p class="text-xs text-gray-500 dark:text-gray-400">Diesen Monat</p>
                      <div class="text-xl font-bold text-green-600 dark:text-green-400">
                        {{ webSearchSettings.searxngMonthCount || 0 }}
                      </div>
                    </div>
                    <div class="text-right">
                      <p class="text-xs text-gray-500 dark:text-gray-400">Gesamt</p>
                      <div class="text-xl font-bold text-green-700 dark:text-green-300">
                        {{ webSearchSettings.searxngTotalCount || 0 }}
                      </div>
                    </div>
                  </div>
                  <!-- Kein Limit Hinweis -->
                  <div class="mt-2 text-xs text-green-600 dark:text-green-400 text-center">
                    Kein Limit
                  </div>
                </div>
              </div>

              <!-- Brave API Key -->
              <div class="mb-6 p-4 rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
                <div class="flex items-start gap-3 mb-3">
                  <div class="p-2 rounded-lg bg-orange-500/20">
                    <StarIcon class="w-5 h-5 text-orange-500" />
                  </div>
                  <div class="flex-1">
                    <h4 class="font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                      Brave Search API
                      <span v-if="webSearchSettings.braveConfigured" class="text-xs px-2 py-0.5 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400 rounded-full">
                        Aktiv
                      </span>
                      <span v-else class="text-xs px-2 py-0.5 bg-gray-100 dark:bg-gray-700 text-gray-500 rounded-full">
                        Nicht konfiguriert
                      </span>
                    </h4>
                    <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                      2000 kostenlose Suchen/Monat • Zuverlässig • Keine Rate-Limits
                    </p>
                  </div>
                </div>

                <div class="flex gap-2">
                  <input
                    v-model="webSearchSettings.braveApiKey"
                    type="password"
                    placeholder="Brave API Key eingeben..."
                    class="flex-1 px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-orange-500 focus:border-transparent font-mono text-sm"
                  />
                  <button
                    @click="testBraveSearch"
                    :disabled="testingSearch"
                    class="px-4 py-2 bg-orange-500 hover:bg-orange-600 text-white rounded-xl transition-colors disabled:opacity-50 flex items-center gap-2"
                  >
                    <ArrowPathIcon v-if="testingSearch" class="w-4 h-4 animate-spin" />
                    <CheckIcon v-else class="w-4 h-4" />
                    Test
                  </button>
                </div>

                <div class="mt-2 flex items-center gap-2">
                  <a
                    href="https://brave.com/search/api/"
                    target="_blank"
                    class="text-xs text-blue-600 dark:text-blue-400 hover:underline"
                  >
                    → Kostenlosen API-Key holen (brave.com/search/api)
                  </a>
                </div>
              </div>

              <!-- Eigene SearXNG Instanz (Priorität 1) -->
              <div class="mb-6 p-4 rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
                <div class="flex items-start gap-3 mb-3">
                  <div class="p-2 rounded-lg bg-green-500/20">
                    <ServerIcon class="w-5 h-5 text-green-500" />
                  </div>
                  <div class="flex-1">
                    <h4 class="font-semibold text-gray-900 dark:text-white">
                      Eigene SearXNG Instanz
                    </h4>
                    <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                      Wird zuerst verwendet (Priorität 1) • Keine Limits • Volle Kontrolle
                    </p>
                  </div>
                </div>

                <div class="flex gap-2">
                  <input
                    v-model="webSearchSettings.customSearxngInstance"
                    type="url"
                    placeholder="https://search.java-fleet.com"
                    class="flex-1 px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-xl bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-green-500 focus:border-transparent text-sm"
                  />
                  <button
                    @click="testCustomSearxng"
                    :disabled="!webSearchSettings.customSearxngInstance || testingSearch"
                    class="px-4 py-2 bg-green-500 hover:bg-green-600 text-white rounded-xl transition-colors disabled:opacity-50 flex items-center gap-2"
                  >
                    <ArrowPathIcon v-if="testingSearch" class="w-4 h-4 animate-spin" />
                    <CheckIcon v-else class="w-4 h-4" />
                    Test
                  </button>
                </div>
              </div>

              <!-- SearXNG Fallback-Instanzen (editierbar) -->
              <details class="mb-4">
                <summary class="cursor-pointer text-sm font-medium text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white flex items-center gap-2">
                  <span>▸ Öffentliche Fallback-Instanzen</span>
                  <span class="text-xs bg-gray-200 dark:bg-gray-700 px-2 py-0.5 rounded">
                    {{ webSearchSettings.searxngInstances?.length || 0 }}
                  </span>
                </summary>
                <div class="mt-3 p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
                  <div class="space-y-2 max-h-48 overflow-y-auto custom-scrollbar">
                    <div
                      v-for="(instance, index) in webSearchSettings.searxngInstances"
                      :key="index"
                      class="flex items-center gap-2"
                    >
                      <span class="text-xs text-gray-400 w-5">{{ index + 1 }}.</span>
                      <input
                        v-model="webSearchSettings.searxngInstances[index]"
                        type="url"
                        class="flex-1 px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                      />
                      <button
                        @click="removeSearxngInstance(index)"
                        class="p-1.5 text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded transition-colors"
                        title="Entfernen"
                      >
                        <TrashIcon class="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                  <div class="mt-3 flex gap-2">
                    <button
                      @click="addSearxngInstance"
                      class="px-3 py-1.5 text-sm text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-lg transition-colors flex items-center gap-1"
                    >
                      <PlusIcon class="w-4 h-4" />
                      Hinzufügen
                    </button>
                    <button
                      @click="resetSearxngInstances"
                      class="px-3 py-1.5 text-sm text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
                    >
                      Zurücksetzen
                    </button>
                  </div>
                </div>
              </details>

              <!-- Erweiterte Such-Features -->
              <div class="mb-6 p-4 rounded-xl bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
                <h4 class="font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                  <AdjustmentsHorizontalIcon class="w-5 h-5 text-purple-500" />
                  Erweiterte Such-Features
                </h4>

                <div class="space-y-4">
                  <!-- Query-Optimierung -->
                  <div class="flex items-center justify-between">
                    <div class="flex-1">
                      <label class="font-medium text-gray-700 dark:text-gray-200 text-sm">Query-Optimierung</label>
                      <p class="text-xs text-gray-500 dark:text-gray-400">LLM optimiert Suchanfragen für bessere Ergebnisse</p>
                    </div>
                    <label class="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" v-model="webSearchSettings.queryOptimizationEnabled" class="sr-only peer">
                      <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-purple-300 dark:peer-focus:ring-purple-800 rounded-full peer dark:bg-gray-600 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-500 peer-checked:bg-purple-600"></div>
                    </label>
                  </div>

                  <!-- Modell für Query-Optimierung -->
                  <div v-if="webSearchSettings.queryOptimizationEnabled" class="ml-4 pl-4 border-l-2 border-purple-200 dark:border-purple-700">
                    <label class="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Optimierungs-Modell</label>
                    <select
                      v-model="webSearchSettings.queryOptimizationModel"
                      class="w-full px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-white"
                    >
                      <option v-if="smallModels.length === 0" value="">Keine kleinen Modelle gefunden</option>
                      <option v-for="model in smallModels" :key="model" :value="model">
                        {{ model }}
                      </option>
                      <!-- Fallback: Alle Modelle wenn keine kleinen gefunden -->
                      <optgroup v-if="smallModels.length > 0 && availableModels.length > smallModels.length" label="── Alle Modelle ──">
                        <option v-for="model in availableModels" :key="'all-' + model.name" :value="model.name">
                          {{ model.name }}
                        </option>
                      </optgroup>
                    </select>

                    <!-- Effektives Modell Status -->
                    <div class="mt-2 text-xs">
                      <!-- Warnung: Konfiguriertes Modell nicht verfügbar -->
                      <div v-if="webSearchSettings.effectiveOptimizationModel && webSearchSettings.effectiveOptimizationModel !== webSearchSettings.queryOptimizationModel"
                           class="flex items-center gap-1.5 text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-900/20 px-2 py-1.5 rounded-lg">
                        <svg class="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                          <path fill-rule="evenodd" d="M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 2.495zM10 5a.75.75 0 01.75.75v3.5a.75.75 0 01-1.5 0v-3.5A.75.75 0 0110 5zm0 9a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd"/>
                        </svg>
                        <span>
                          <strong>{{ webSearchSettings.queryOptimizationModel }}</strong> nicht verfügbar.
                          Verwende: <strong class="text-green-600 dark:text-green-400">{{ webSearchSettings.effectiveOptimizationModel }}</strong>
                        </span>
                      </div>
                      <!-- Info: Modell nicht verfügbar, Optimierung deaktiviert -->
                      <div v-else-if="!webSearchSettings.effectiveOptimizationModel"
                           class="flex items-center gap-1.5 text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 px-2 py-1.5 rounded-lg">
                        <svg class="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd"/>
                        </svg>
                        <span>Kein Modell verfügbar - Query-Optimierung deaktiviert</span>
                      </div>
                      <!-- OK: Konfiguriertes Modell wird verwendet -->
                      <div v-else class="flex items-center gap-1.5 text-green-600 dark:text-green-400">
                        <svg class="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                          <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z" clip-rule="evenodd"/>
                        </svg>
                        <span>Modell aktiv: <strong>{{ webSearchSettings.effectiveOptimizationModel }}</strong></span>
                      </div>
                    </div>

                    <p class="text-xs text-gray-400 mt-1">
                      <span v-if="smallModels.length > 0">{{ smallModels.length }} kleine Modelle (1B-7B) verfügbar</span>
                      <span v-else>Lade Modelle oder installiere ein kleines Modell (z.B. llama3.2:3b)</span>
                    </p>
                  </div>

                  <!-- Content-Scraping -->
                  <div class="flex items-center justify-between">
                    <div class="flex-1">
                      <label class="font-medium text-gray-700 dark:text-gray-200 text-sm">Vollständige Inhalte</label>
                      <p class="text-xs text-gray-500 dark:text-gray-400">Lädt Webseiten-Inhalte statt nur Snippets</p>
                    </div>
                    <label class="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" v-model="webSearchSettings.contentScrapingEnabled" class="sr-only peer">
                      <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 dark:peer-focus:ring-blue-800 rounded-full peer dark:bg-gray-600 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-500 peer-checked:bg-blue-600"></div>
                    </label>
                  </div>

                  <!-- Re-Ranking -->
                  <div class="flex items-center justify-between">
                    <div class="flex-1">
                      <label class="font-medium text-gray-700 dark:text-gray-200 text-sm">Re-Ranking</label>
                      <p class="text-xs text-gray-500 dark:text-gray-400">Sortiert Ergebnisse nach Relevanz</p>
                    </div>
                    <label class="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" v-model="webSearchSettings.reRankingEnabled" class="sr-only peer">
                      <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-green-300 dark:peer-focus:ring-green-800 rounded-full peer dark:bg-gray-600 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-500 peer-checked:bg-green-600"></div>
                    </label>
                  </div>

                  <!-- Multi-Query -->
                  <div class="flex items-center justify-between">
                    <div class="flex-1">
                      <label class="font-medium text-gray-700 dark:text-gray-200 text-sm">Multi-Query</label>
                      <p class="text-xs text-gray-500 dark:text-gray-400">Parallele Suchen mit Query-Variationen (mehr API-Calls)</p>
                    </div>
                    <label class="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" v-model="webSearchSettings.multiQueryEnabled" class="sr-only peer">
                      <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-amber-300 dark:peer-focus:ring-amber-800 rounded-full peer dark:bg-gray-600 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-500 peer-checked:bg-amber-600"></div>
                    </label>
                  </div>
                </div>

                <!-- Info -->
                <div class="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
                  <p class="text-xs text-gray-500 dark:text-gray-400">
                    💾 15 Min Cache • 🌐 Sprach-Erkennung automatisch • ⏱️ Zeitfilter verfügbar
                  </p>
                </div>
              </div>

              <!-- Info Box -->
              <div class="p-3 rounded-xl bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700/50">
                <div class="flex items-start gap-2">
                  <InformationCircleIcon class="w-5 h-5 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" />
                  <div class="text-xs text-blue-800 dark:text-blue-200">
                    <strong>So funktioniert's:</strong><br>
                    Wenn die Web-Suche aktiviert ist (Checkbox im Chat), werden Suchergebnisse
                    als Kontext an das KI-Modell übergeben. Das Modell kann dann aktuelle
                    Informationen aus dem Web in seine Antwort einbeziehen (RAG).
                  </div>
                </div>
              </div>
            </section>
          </div>

          <!-- TAB: Danger Zone -->
          <div v-if="activeTab === 'danger'">
            <section class="bg-gradient-to-br from-red-50 to-red-100 dark:from-red-900/30 dark:to-red-800/30 p-6 rounded-xl border-2 border-red-300 dark:border-red-700 shadow-lg">
              <div class="flex items-start gap-3 mb-6">
                <ShieldExclamationIcon class="w-8 h-8 text-red-600 dark:text-red-400 flex-shrink-0" />
                <div>
                  <h3 class="text-xl font-bold text-red-900 dark:text-red-100 mb-2">
                    ⚠️ DANGER ZONE
                  </h3>
                  <p class="text-sm text-red-800 dark:text-red-200">
                    Diese Aktionen sind <strong>permanent und können nicht rückgängig gemacht werden!</strong>
                  </p>
                </div>
              </div>

              <!-- Selective Data Reset -->
              <div class="bg-white/80 dark:bg-gray-900/80 p-5 rounded-xl border-2 border-red-400 dark:border-red-600">
                <div class="flex items-start gap-3 mb-4">
                  <TrashIcon class="w-6 h-6 text-red-600 dark:text-red-400 flex-shrink-0 mt-1" />
                  <div class="flex-1">
                    <h4 class="text-lg font-bold text-gray-900 dark:text-white mb-2">
                      Daten selektiv löschen & zurücksetzen
                    </h4>
                    <p class="text-sm text-gray-700 dark:text-gray-300 mb-4">
                      Wählen Sie aus, welche Daten gelöscht werden sollen:
                    </p>

                    <!-- Checkboxes for selective deletion -->
                    <div class="space-y-3 mb-4">
                      <!-- Chats & Messages -->
                      <label class="flex items-start gap-3 p-3 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer transition-colors">
                        <input
                          type="checkbox"
                          v-model="resetSelection.chats"
                          class="mt-1 w-4 h-4 text-red-600 rounded focus:ring-red-500"
                        />
                        <div class="flex-1">
                          <div class="font-semibold text-gray-900 dark:text-white">Chats & Nachrichten</div>
                          <div class="text-xs text-gray-600 dark:text-gray-400">Alle Chat-Verläufe und Konversationen</div>
                        </div>
                      </label>

                      <!-- Projects & Files -->
                      <label class="flex items-start gap-3 p-3 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer transition-colors">
                        <input
                          type="checkbox"
                          v-model="resetSelection.projects"
                          class="mt-1 w-4 h-4 text-red-600 rounded focus:ring-red-500"
                        />
                        <div class="flex-1">
                          <div class="font-semibold text-gray-900 dark:text-white">Projekte & Dateien</div>
                          <div class="text-xs text-gray-600 dark:text-gray-400">Alle Projekte, hochgeladene Dateien und Kontext-Dateien</div>
                        </div>
                      </label>

                      <!-- Custom Models -->
                      <label class="flex items-start gap-3 p-3 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer transition-colors border-2 border-orange-300 dark:border-orange-700">
                        <input
                          type="checkbox"
                          v-model="resetSelection.customModels"
                          class="mt-1 w-4 h-4 text-red-600 rounded focus:ring-red-500"
                        />
                        <div class="flex-1">
                          <div class="font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                            Custom Models
                            <span class="text-xs bg-orange-100 dark:bg-orange-900/30 text-orange-800 dark:text-orange-200 px-2 py-0.5 rounded">Optional</span>
                          </div>
                          <div class="text-xs text-gray-600 dark:text-gray-400">
                            Ihre eigenen benutzerdefinierten Modelle
                          </div>
                        </div>
                      </label>

                      <!-- Settings -->
                      <label class="flex items-start gap-3 p-3 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer transition-colors">
                        <input
                          type="checkbox"
                          v-model="resetSelection.settings"
                          class="mt-1 w-4 h-4 text-red-600 rounded focus:ring-red-500"
                        />
                        <div class="flex-1">
                          <div class="font-semibold text-gray-900 dark:text-white">Einstellungen & Konfiguration</div>
                          <div class="text-xs text-gray-600 dark:text-gray-400">Alle App-Einstellungen und Modell-Konfigurationen</div>
                        </div>
                      </label>

                      <!-- Personal Info -->
                      <label class="flex items-start gap-3 p-3 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer transition-colors">
                        <input
                          type="checkbox"
                          v-model="resetSelection.personalInfo"
                          class="mt-1 w-4 h-4 text-red-600 rounded focus:ring-red-500"
                        />
                        <div class="flex-1">
                          <div class="font-semibold text-gray-900 dark:text-white">Persönliche Informationen</div>
                          <div class="text-xs text-gray-600 dark:text-gray-400">Gespeicherte persönliche Daten</div>
                        </div>
                      </label>

                      <!-- Templates -->
                      <label class="flex items-start gap-3 p-3 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer transition-colors">
                        <input
                          type="checkbox"
                          v-model="resetSelection.templates"
                          class="mt-1 w-4 h-4 text-red-600 rounded focus:ring-red-500"
                        />
                        <div class="flex-1">
                          <div class="font-semibold text-gray-900 dark:text-white">Templates & Prompts</div>
                          <div class="text-xs text-gray-600 dark:text-gray-400">System-Prompts, Letter-Templates und Vorlagen</div>
                        </div>
                      </label>

                      <!-- Statistics -->
                      <label class="flex items-start gap-3 p-3 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer transition-colors">
                        <input
                          type="checkbox"
                          v-model="resetSelection.stats"
                          class="mt-1 w-4 h-4 text-red-600 rounded focus:ring-red-500"
                        />
                        <div class="flex-1">
                          <div class="font-semibold text-gray-900 dark:text-white">Statistiken & Metadaten</div>
                          <div class="text-xs text-gray-600 dark:text-gray-400">Globale Stats und Model-Metadaten</div>
                        </div>
                      </label>
                    </div>

                    <div class="p-3 bg-yellow-100 dark:bg-yellow-900/30 border border-yellow-400 dark:border-yellow-700 rounded-lg mb-4">
                      <p class="text-xs text-yellow-900 dark:text-yellow-200 flex items-start gap-2">
                        <ExclamationTriangleIcon class="w-4 h-4 flex-shrink-0 mt-0.5" />
                        <span>
                          <strong>Hinweis:</strong> Die Anwendung wird nach dem Löschen automatisch neu geladen.
                          Ideal für Testing und Demo-Zwecke.
                        </span>
                      </p>
                    </div>
                  </div>
                </div>

                <button
                  @click="handleResetAll"
                  :disabled="resetting || !hasAnySelection"
                  :title="hasAnySelection ? 'Ausgewählte Daten unwiderruflich löschen und mit Standard-Daten neu initialisieren' : 'Bitte wählen Sie mindestens eine Kategorie aus'"
                  class="w-full px-6 py-3 rounded-xl bg-gradient-to-r from-red-600 to-red-700 hover:from-red-700 hover:to-red-800 text-white font-bold shadow-lg hover:shadow-xl transition-all transform hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                >
                  <TrashIcon v-if="!resetting" class="w-5 h-5" />
                  <ArrowPathIcon v-else class="w-5 h-5 animate-spin" />
                  {{ resetting ? 'Lösche Daten...' : (hasAnySelection ? 'AUSGEWÄHLTE DATEN LÖSCHEN' : 'KEINE AUSWAHL') }}
                </button>
              </div>
            </section>
          </div>

        </div>

        <!-- System Prompt Editor Modal -->
        <Transition name="modal">
          <div v-if="showPromptEditor" class="absolute inset-0 bg-black/70 flex items-center justify-center z-50 p-4" @click.self="showPromptEditor = false">
            <div class="bg-white dark:bg-gray-800 rounded-xl shadow-2xl max-w-2xl w-full max-h-[80vh] overflow-y-auto">
              <div class="p-5 border-b border-gray-200 dark:border-gray-700">
                <h4 class="text-lg font-semibold text-gray-900 dark:text-white">
                  {{ editingPrompt ? 'System-Prompt bearbeiten' : 'Neuer System-Prompt' }}
                </h4>
              </div>

              <div class="p-5 space-y-4">
                <!-- Name -->
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    Name
                  </label>
                  <input
                    v-model="promptForm.name"
                    type="text"
                    placeholder="z.B. Java-Experte, Code-Reviewer, ..."
                    class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  >
                </div>

                <!-- Content -->
                <div>
                  <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    System-Prompt Text
                  </label>
                  <textarea
                    v-model="promptForm.content"
                    rows="8"
                    placeholder="Du bist ein hilfreicher Assistent, der..."
                    class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-y"
                  ></textarea>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    {{ promptForm.content.length }} Zeichen
                  </p>
                </div>

                <!-- Is Default -->
                <div class="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-900/50 rounded-lg">
                  <div>
                    <label class="text-sm font-medium text-gray-700 dark:text-gray-300">
                      Als Standard-Prompt setzen
                    </label>
                    <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                      Dieser Prompt wird automatisch für neue Chats verwendet
                    </p>
                  </div>
                  <input
                    type="checkbox"
                    v-model="promptForm.isDefault"
                    class="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
                  >
                </div>
              </div>

              <div class="p-5 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-3">
                <button
                  @click="showPromptEditor = false"
                  class="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
                >
                  Abbrechen
                </button>
                <button
                  @click="saveSystemPrompt"
                  :disabled="!promptForm.name.trim() || !promptForm.content.trim()"
                  class="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                >
                  <CheckIcon class="w-4 h-4" />
                  {{ editingPrompt ? 'Aktualisieren' : 'Erstellen' }}
                </button>
              </div>
            </div>
          </div>
        </Transition>

        <!-- Footer with Gradient -->
        <div class="sticky bottom-0 bg-gray-50/90 dark:bg-gray-900/90 backdrop-blur-sm border-t border-gray-200/50 dark:border-gray-700/50 px-6 py-4 flex justify-between">
          <button
            @click="resetToDefaults"
            class="px-4 py-2 rounded-xl text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white hover:bg-gray-200 dark:hover:bg-gray-700 transition-all flex items-center gap-2"
          >
            <ArrowPathIcon class="w-4 h-4" />
            Zurücksetzen
          </button>
          <div class="flex gap-3">
            <button
              @click="close"
              class="px-5 py-2 rounded-xl border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-all transform hover:scale-105"
            >
              Abbrechen
            </button>
            <button
              @click="save"
              :disabled="saving"
              class="px-6 py-2 rounded-xl bg-gradient-to-r from-fleet-orange-500 to-orange-600 hover:from-fleet-orange-400 hover:to-orange-500 text-white font-semibold shadow-lg hover:shadow-xl transition-all transform hover:scale-105 disabled:opacity-50 flex items-center gap-2"
            >
              <CheckIcon v-if="!saving" class="w-5 h-5" />
              <ArrowPathIcon v-else class="w-5 h-5 animate-spin" />
              {{ saving ? 'Speichere...' : 'Speichern' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import {
  Cog6ToothIcon,
  XMarkIcon,
  GlobeAltIcon,
  LanguageIcon,
  SunIcon,
  CpuChipIcon,
  SparklesIcon,
  ExclamationTriangleIcon,
  CodeBracketIcon,
  BoltIcon,
  CubeIcon,
  InformationCircleIcon,
  AdjustmentsHorizontalIcon,
  DocumentTextIcon,
  FireIcon,
  DocumentDuplicateIcon,
  PhotoIcon,
  EyeIcon,
  LinkIcon,
  WrenchScrewdriverIcon,
  Bars3Icon,
  HashtagIcon,
  BugAntIcon,
  ArrowPathIcon,
  CheckIcon,
  CheckCircleIcon,
  UserIcon,
  TrashIcon,
  ShieldExclamationIcon,
  UsersIcon,
  MagnifyingGlassIcon,
  StarIcon,
  ServerIcon,
  PlusIcon,
  FolderIcon,
  FolderOpenIcon
} from '@heroicons/vue/24/outline'
import { useSettingsStore } from '../stores/settingsStore'
import { useChatStore } from '../stores/chatStore'
import PersonalInfoTab from './PersonalInfoTab.vue'
import ProviderSettings from './ProviderSettings.vue'
import { useToast } from '../composables/useToast'
import { useConfirmDialog } from '../composables/useConfirmDialog'
import { formatDateAbsolute } from '../composables/useFormatters'
import api from '../services/api'
import { secureFetch } from '../utils/secureFetch'
import ToggleSwitch from './ToggleSwitch.vue'
import SimpleSamplingParams from './SimpleSamplingParams.vue'
import { filterVisionModels, filterCodeModels } from '../utils/modelFilters'

const { success, error: errorToast } = useToast()
const { confirm, confirmDelete } = useConfirmDialog()

const props = defineProps({
  isOpen: Boolean,
  initialTab: String
})

const emit = defineEmits(['close', 'save'])

const settingsStore = useSettingsStore()
const chatStore = useChatStore()

// Local copy of settings for editing
const settings = ref({ ...settingsStore.settings })
const saving = ref(false)
const resetting = ref(false)

// Sampling Parameters
const samplingParams = ref({})

// Reset selection checkboxes
const resetSelection = ref({
  chats: true,
  projects: true,
  customModels: false,  // Custom Models standardmäßig NICHT löschen
  settings: true,
  personalInfo: true,
  templates: true,
  stats: true
})

// Active tab
const activeTab = ref(props.initialTab || 'general')

// Watch for tab changes to reload data when needed
watch(activeTab, async (newTab) => {
  if (newTab === 'fleet-mates') {
    await loadTrustedMatesCount()
  }
})

// Sync showWelcomeTiles with settingsStore (persistent)
watch(() => settings.value.showWelcomeTiles, async (newValue) => {
  settingsStore.settings.showWelcomeTiles = newValue
  // Save to database
  try {
    await secureFetch('/api/settings/show-welcome-tiles', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newValue)
    })
  } catch (error) {
    console.error('Failed to save showWelcomeTiles:', error)
  }
}, { immediate: false })

// Sync cpuOnly with settingsStore (persistent) - für CPU-Only Mode Toggle
watch(() => settings.value.cpuOnly, (newValue) => {
  settingsStore.settings.cpuOnly = newValue
  console.log('🖥️ CPU-Only Mode:', newValue ? 'aktiviert' : 'deaktiviert')
}, { immediate: false })

// Ref to PersonalInfoTab
const personalInfoTabRef = ref(null)

// Fleet Mates Pairing
const trustedMatesCount = ref(0)
const trustedMates = ref([])
const forgettingMates = ref(false)
const removingMateId = ref(null)

// File Search (OS Mate RAG)
const fileSearchFolders = ref([])
const fileSearchStatus = ref(null)
const newFolderPath = ref('')

async function loadFileSearchStatus() {
  try {
    const response = await fetch('/api/file-search/status')
    if (response.ok) {
      fileSearchStatus.value = await response.json()
      fileSearchFolders.value = fileSearchStatus.value.searchFolders || []
    }
  } catch (err) {
    console.error('Failed to load file search status:', err)
  }
}

async function addSearchFolder() {
  if (!newFolderPath.value) return

  try {
    const response = await secureFetch('/api/file-search/folders', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ folderPath: newFolderPath.value })
    })

    if (response.ok) {
      newFolderPath.value = ''
      await loadFileSearchStatus()
      success('Ordner hinzugefügt und Indexierung gestartet')
    } else {
      const data = await response.json()
      errorToast(data.error || 'Fehler beim Hinzufügen')
    }
  } catch (err) {
    console.error('Failed to add search folder:', err)
    errorToast('Fehler beim Hinzufügen des Ordners')
  }
}

async function removeSearchFolder(folderId) {
  try {
    await secureFetch(`/api/file-search/folders/${folderId}`, { method: 'DELETE' })
    await loadFileSearchStatus()
    success('Ordner entfernt')
  } catch (err) {
    console.error('Failed to remove folder:', err)
    errorToast('Fehler beim Entfernen')
  }
}

async function reindexFolder(folderId) {
  try {
    await secureFetch(`/api/file-search/folders/${folderId}/reindex`, { method: 'POST' })
    success('Neu-Indexierung gestartet')
    // Refresh status after a delay
    setTimeout(loadFileSearchStatus, 2000)
  } catch (err) {
    console.error('Failed to reindex folder:', err)
    errorToast('Fehler beim Indexieren')
  }
}

// formatDate importiert aus useFormatters.js als formatDateAbsolute

// Tab configuration
const tabs = [
  { id: 'general', label: 'Allgemein', icon: GlobeAltIcon },
  { id: 'providers', label: 'LLM Provider', icon: CpuChipIcon },
  { id: 'models', label: 'Modellauswahl', icon: CubeIcon },
  { id: 'parameters', label: 'Parameter', icon: AdjustmentsHorizontalIcon },
  { id: 'templates', label: 'System-Prompts', icon: DocumentTextIcon },
  { id: 'personal', label: 'Persönliche Daten', icon: UserIcon },
  { id: 'agents', label: 'Agents', icon: SparklesIcon },
  { id: 'web-search', label: 'Web-Suche', icon: MagnifyingGlassIcon },
  { id: 'danger', label: 'Danger Zone', icon: ShieldExclamationIcon }
]

// Model selection settings
const modelSelectionSettings = ref({
  enabled: true,
  codeModel: 'qwen2.5-coder:7b',
  fastModel: 'llama3.2:3b',
  visionModel: 'llava:13b',
  defaultModel: 'qwen2.5-coder:7b',
  visionChainingEnabled: true,
  visionChainingSmartSelection: true
})

// Fleet Mates Model Settings
const mateModels = ref({
  emailModel: '',
  documentModel: '',
  logAnalysisModel: ''
})

// Web Search Settings (Brave API + SearXNG)
const webSearchSettings = ref({
  braveApiKey: '',
  braveConfigured: false,
  searchCount: 0,
  searchLimit: 2000,
  remainingSearches: 2000,
  currentMonth: '',
  customSearxngInstance: '',  // Eigene Instanz (Priorität 1)
  searxngInstances: [],       // Öffentliche Fallback-Instanzen
  searxngTotalCount: 0,       // Gesamte SearXNG-Suchen
  searxngMonthCount: 0,       // SearXNG-Suchen diesen Monat
  // Feature Flags
  queryOptimizationEnabled: true,
  contentScrapingEnabled: true,
  multiQueryEnabled: false,
  reRankingEnabled: true,
  queryOptimizationModel: 'llama3.2:3b',
  effectiveOptimizationModel: null  // Das tatsächlich verwendete Modell (nach Fallback)
})

const defaultSearxngInstances = [
  'https://search.sapti.me',
  'https://searx.tiekoetter.com',
  'https://priv.au',
  'https://search.ononoki.org',
  'https://search.bus-hit.me',
  'https://paulgo.io'
]

const testingSearch = ref(false)

// Computed: Farbe des Zählers basierend auf Verbrauch
const searchCountColor = computed(() => {
  const percent = (webSearchSettings.value.searchCount / webSearchSettings.value.searchLimit) * 100
  if (percent >= 90) return 'text-red-500'
  if (percent >= 70) return 'text-yellow-500'
  return 'text-green-500'
})

// Computed: Prozent für Progress Bar
const searchCountPercent = computed(() => {
  return Math.min(100, (webSearchSettings.value.searchCount / webSearchSettings.value.searchLimit) * 100)
})

// Available models
const availableModels = ref([])

// Fast models (< 10GB, good for Mates)
const fastModels = computed(() => {
  return availableModels.value.filter(m => m.size && m.size < 10 * 1024 * 1024 * 1024)
})

// Format file size
function formatSize(bytes) {
  if (!bytes) return '?'
  const gb = bytes / (1024 * 1024 * 1024)
  if (gb >= 1) return `${gb.toFixed(1)}GB`
  const mb = bytes / (1024 * 1024)
  return `${mb.toFixed(0)}MB`
}

// System Prompts Management
const systemPrompts = ref([])
const showPromptEditor = ref(false)
const editingPrompt = ref(null)
const promptForm = ref({
  name: '',
  content: '',
  isDefault: false
})

// Filtered models for specific use cases
const visionModels = computed(() => {
  const allModelNames = availableModels.value.map(m => m.name)
  const filtered = filterVisionModels(allModelNames)
  console.log('Vision Models:', filtered) // Debug
  return filtered
})
const codeModels = computed(() => {
  const allModelNames = availableModels.value.map(m => m.name)
  const filtered = filterCodeModels(allModelNames)
  console.log('Code Models:', filtered) // Debug
  return filtered
})

// Kleine/schnelle Modelle für Query-Optimierung (1B-7B Parameter)
const smallModels = computed(() => {
  const smallPatterns = [
    /llama.*[1-3]b/i,
    /qwen.*[1-3]b/i,
    /phi.*[1-3]/i,
    /gemma.*2b/i,
    /tinyllama/i,
    /smollm/i,
    /mistral.*7b/i,
    /llama.*7b/i,
    /qwen.*7b/i,
  ]

  return availableModels.value
    .map(m => m.name)
    .filter(name => smallPatterns.some(pattern => pattern.test(name)))
    .sort((a, b) => {
      // Sortiere nach Parametergröße (kleinste zuerst)
      const sizeA = parseInt(a.match(/(\d+)b/i)?.[1] || '99')
      const sizeB = parseInt(b.match(/(\d+)b/i)?.[1] || '99')
      return sizeA - sizeB
    })
})

// Check if at least one option is selected
const hasAnySelection = computed(() => {
  return Object.values(resetSelection.value).some(val => val === true)
})

// Load model selection settings and available models on mount
onMounted(async () => {
  // Initialize sampling params from settings store
  samplingParams.value = {
    maxTokens: settingsStore.settings.maxTokens || 512,
    temperature: settingsStore.settings.temperature || 0.7,
    topP: settingsStore.settings.topP || 0.9,
    topK: settingsStore.settings.topK || 40,
    minP: settingsStore.settings.minP || 0.05,
    repeatPenalty: settingsStore.settings.repeatPenalty || 1.18,
    repeatLastN: settingsStore.settings.repeatLastN || 64,
    presencePenalty: settingsStore.settings.presencePenalty || 0.0,
    frequencyPenalty: settingsStore.settings.frequencyPenalty || 0.0,
    mirostatMode: settingsStore.settings.mirostatMode || 0,
    mirostatTau: settingsStore.settings.mirostatTau || 5.0,
    mirostatEta: settingsStore.settings.mirostatEta || 0.1
  }

  // Set initial tab if provided
  if (props.initialTab) {
    activeTab.value = props.initialTab
  }

  await loadModelSelectionSettings()
  await loadAvailableModels()
  await loadMateModels()
  await loadSystemPrompts()
  await loadTrustedMatesCount()
  await loadWebSearchSettings()
  await loadShowWelcomeTiles()
  await loadFileSearchStatus()
})

async function loadShowWelcomeTiles() {
  try {
    const response = await fetch('/api/settings/show-welcome-tiles')
    if (response.ok) {
      const value = await response.json()
      settings.value.showWelcomeTiles = value
      settingsStore.settings.showWelcomeTiles = value
    }
  } catch (error) {
    console.error('Failed to load showWelcomeTiles:', error)
  }
}

// TopBar Setting speichern (sofort bei Änderung)
async function saveTopBarSetting() {
  try {
    // Update settingsStore
    settingsStore.settings.showTopBar = settings.value.showTopBar
    // Save to backend database
    await settingsStore.saveShowTopBarToBackend(settings.value.showTopBar)
    success('TopBar-Einstellung gespeichert')
  } catch (error) {
    console.error('Failed to save showTopBar:', error)
    errorToast('Fehler beim Speichern der TopBar-Einstellung')
  }
}

// Schriftgröße setzen und persistieren
function setFontSize(size) {
  settings.value.fontSize = size
  settingsStore.settings.fontSize = size
  // CSS-Klasse auf root-Element anwenden
  applyFontSize(size)
  success('Schriftgröße geändert')
}

// Schriftgröße auf das root-Element anwenden
function applyFontSize(size) {
  const root = document.documentElement
  // Alle vorherigen font-size Klassen entfernen
  root.classList.remove('font-size-small', 'font-size-medium', 'font-size-large', 'font-size-xlarge')
  // Neue Klasse hinzufügen
  root.classList.add(`font-size-${size}`)
}

async function loadModelSelectionSettings() {
  try {
    const loadedSettings = await api.getModelSelectionSettings()
    modelSelectionSettings.value = loadedSettings
  } catch (error) {
    console.error('Failed to load model selection settings:', error)
  }
}

async function loadAvailableModels() {
  try {
    const models = await api.getAvailableModels()
    availableModels.value = models
  } catch (error) {
    console.error('Failed to load available models:', error)
  }
}

// Fleet Mates Model Functions
async function loadMateModels() {
  try {
    const [emailRes, docRes, logRes] = await Promise.all([
      fetch('/api/settings/email-model'),
      fetch('/api/settings/document-model'),
      fetch('/api/settings/log-analysis-model')
    ])

    mateModels.value.emailModel = emailRes.ok ? await emailRes.text() : ''
    mateModels.value.documentModel = docRes.ok ? await docRes.text() : ''
    mateModels.value.logAnalysisModel = logRes.ok ? await logRes.text() : ''
  } catch (error) {
    console.error('Failed to load mate models:', error)
  }
}

async function saveMateModels() {
  try {
    await Promise.all([
      fetch('/api/settings/email-model', {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain' },
        body: mateModels.value.emailModel
      }),
      fetch('/api/settings/document-model', {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain' },
        body: mateModels.value.documentModel
      }),
      fetch('/api/settings/log-analysis-model', {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain' },
        body: mateModels.value.logAnalysisModel
      })
    ])
    success('Mate-Modelle gespeichert!')
  } catch (error) {
    console.error('Failed to save mate models:', error)
    errorToast('Fehler beim Speichern')
  }
}

// Fleet Mates Functions
async function loadTrustedMatesCount() {
  try {
    const response = await fetch('/api/pairing/trusted')
    if (response.ok) {
      const mates = await response.json()
      trustedMates.value = mates
      trustedMatesCount.value = mates.length
    }
  } catch (error) {
    console.error('Failed to load trusted mates count:', error)
  }
}

async function forgetAllMates() {
  const confirmed = await confirm({
    title: 'Alle Mates vergessen?',
    message: 'Wirklich ALLE gepairten Mates vergessen? Diese müssen danach erneut gepairt werden.',
    type: 'danger',
    confirmText: 'Alle vergessen'
  })
  if (!confirmed) return

  forgettingMates.value = true
  try {
    const response = await secureFetch('/api/pairing/trusted', { method: 'DELETE' })
    if (response.ok) {
      trustedMates.value = []
      trustedMatesCount.value = 0
      success('Alle Mates wurden vergessen!')
    } else {
      throw new Error('Failed to forget mates')
    }
  } catch (err) {
    console.error('Failed to forget all mates:', err)
    errorToast('Fehler beim Vergessen der Mates')
  } finally {
    forgettingMates.value = false
  }
}

async function removeMate(mateId) {
  const confirmed = await confirmDelete(mateId, 'Der Mate muss danach erneut gepairt werden.')
  if (!confirmed) return

  removingMateId.value = mateId
  try {
    const response = await secureFetch(`/api/pairing/trusted/${mateId}`, { method: 'DELETE' })
    if (response.ok) {
      await loadTrustedMatesCount()
      success('Mate wurde vergessen!')
    } else {
      throw new Error('Failed to remove mate')
    }
  } catch (err) {
    console.error('Failed to remove mate:', err)
    errorToast('Fehler beim Entfernen des Mates')
  } finally {
    removingMateId.value = null
  }
}

function getMateTypeIcon(mateType) {
  const icons = {
    'mail': '📧',
    'os': '🖥️',
    'browser': '🌐',
    'office': '📄'
  }
  return icons[mateType] || '🔌'
}

function getMateTypeName(mateType) {
  const names = {
    'mail': 'Email Client',
    'os': 'Betriebssystem',
    'browser': 'Browser',
    'office': 'Office Suite'
  }
  return names[mateType] || mateType || 'Unbekannt'
}

// Watch for changes from store
watch(() => settingsStore.settings, (newSettings) => {
  settings.value = { ...newSettings }
}, { deep: true })

function close() {
  emit('close')
}

async function save() {
  saving.value = true
  try {
    // Merge sampling params into settings before saving
    const mergedSettings = {
      ...settings.value,
      ...samplingParams.value
    }

    // Save general settings + sampling parameters
    settingsStore.updateSettings(mergedSettings)

    // Apply streaming setting to chatStore
    if (chatStore.streamingEnabled !== mergedSettings.streamingEnabled) {
      chatStore.toggleStreaming()
    }

    // Save model selection settings to backend
    await api.updateModelSelectionSettings(modelSelectionSettings.value)

    // Save web search settings
    await saveWebSearchSettings()

    // Save personal info if on that tab
    if (personalInfoTabRef.value && activeTab.value === 'personal') {
      await personalInfoTabRef.value.save()
    }

    success('Einstellungen gespeichert')
    emit('save')
    close()
  } catch (error) {
    console.error('Failed to save settings:', error)
    errorToast('Fehler beim Speichern der Einstellungen')
  } finally {
    saving.value = false
  }
}

async function handleResetAll() {
  // Build list of selected categories
  const selectedCategories = []
  if (resetSelection.value.chats) selectedCategories.push('Chats & Nachrichten')
  if (resetSelection.value.projects) selectedCategories.push('Projekte & Dateien')
  if (resetSelection.value.customModels) selectedCategories.push('Custom Models')
  if (resetSelection.value.settings) selectedCategories.push('Einstellungen')
  if (resetSelection.value.personalInfo) selectedCategories.push('Persönliche Informationen')
  if (resetSelection.value.templates) selectedCategories.push('Templates & Prompts')
  if (resetSelection.value.stats) selectedCategories.push('Statistiken')

  if (selectedCategories.length === 0) {
    errorToast('Bitte wählen Sie mindestens eine Kategorie aus!')
    return
  }

  // Confirmation with selected categories
  const confirmation1 = confirm(
    '⚠️ ACHTUNG! ⚠️\n\n' +
    'Sie sind dabei, folgende Daten zu löschen:\n\n' +
    selectedCategories.map(cat => `• ${cat}`).join('\n') + '\n\n' +
    'Diese Aktion kann NICHT rückgängig gemacht werden!\n\n' +
    'Möchten Sie wirklich fortfahren?'
  )

  if (!confirmation1) return

  const confirmation2 = confirm(
    '⚠️ LETZTE WARNUNG! ⚠️\n\n' +
    'Dies ist Ihre letzte Chance!\n\n' +
    'Die ausgewählten Daten werden unwiderruflich gelöscht.\n' +
    'Die Anwendung wird danach neu geladen.\n\n' +
    'Sind Sie ABSOLUT SICHER?'
  )

  if (!confirmation2) return

  resetting.value = true

  try {
    // Call backend to delete selected data
    await api.resetSelectedData(resetSelection.value)

    // Show success message
    success('Ausgewählte Daten wurden gelöscht. Die Anwendung wird jetzt neu geladen...')

    // Wait a moment to show the message
    await new Promise(resolve => setTimeout(resolve, 1500))

    // Reload the page to reset to initial state
    window.location.reload()
  } catch (error) {
    console.error('Failed to reset data:', error)
    errorToast('Fehler beim Löschen der Daten: ' + (error.message || 'Unbekannter Fehler'))
    resetting.value = false
  }
}

async function resetToDefaults() {
  const confirmed = await confirm({
    title: 'Einstellungen zurücksetzen?',
    message: 'Alle Einstellungen auf Standard zurücksetzen?',
    type: 'warning',
    confirmText: 'Zurücksetzen'
  })
  if (confirmed) {
    settingsStore.resetToDefaults()
    settings.value = { ...settingsStore.settings }
    success('Einstellungen zurückgesetzt')
  }
}

// System Prompts Functions
async function loadSystemPrompts() {
  try {
    const prompts = await api.getAllSystemPrompts()
    systemPrompts.value = prompts
  } catch (error) {
    console.error('Failed to load system prompts:', error)
    errorToast('Fehler beim Laden der System-Prompts')
  }
}

function resetPromptForm() {
  promptForm.value = {
    name: '',
    content: '',
    isDefault: false
  }
}

function editSystemPrompt(prompt) {
  editingPrompt.value = prompt
  promptForm.value = {
    name: prompt.name,
    content: prompt.content,
    isDefault: prompt.isDefault || false
  }
  showPromptEditor.value = true
}

async function saveSystemPrompt() {
  try {
    if (!promptForm.value.name.trim() || !promptForm.value.content.trim()) {
      errorToast('Name und Inhalt dürfen nicht leer sein')
      return
    }

    if (editingPrompt.value) {
      // Update existing prompt
      await api.updateSystemPrompt(editingPrompt.value.id, promptForm.value)
      success('System-Prompt erfolgreich aktualisiert!')
    } else {
      // Create new prompt
      await api.createSystemPrompt(promptForm.value)
      success('System-Prompt erfolgreich erstellt!')
    }

    // Reload prompts and close editor
    await loadSystemPrompts()
    showPromptEditor.value = false
    editingPrompt.value = null
    resetPromptForm()
  } catch (error) {
    console.error('Failed to save system prompt:', error)
    errorToast('Fehler beim Speichern des System-Prompts')
  }
}

async function deleteSystemPrompt(promptId) {
  const confirmed = await confirmDelete('System-Prompt', 'Diese Aktion kann nicht rückgängig gemacht werden.')
  if (!confirmed) return

  try {
    await api.deleteSystemPrompt(promptId)
    success('System-Prompt erfolgreich gelöscht!')
    await loadSystemPrompts()
  } catch (error) {
    console.error('Failed to delete system prompt:', error)
    errorToast('Fehler beim Löschen des System-Prompts')
  }
}

async function activateSystemPrompt(prompt) {
  // Wenn bereits aktiv, nichts tun
  if (prompt.isDefault) {
    return
  }

  try {
    // 1. Als Standard in DB speichern (neuer dedizierter Endpoint)
    await api.setDefaultSystemPrompt(prompt.id)

    // 2. chatStore aktualisieren (für TopBar und Chat-Anfragen)
    chatStore.systemPrompt = prompt.content
    chatStore.systemPromptTitle = prompt.name

    // 3. Liste neu laden (UI aktualisieren)
    await loadSystemPrompts()

    success(`"${prompt.name}" aktiviert!`)
    console.log(`✅ System-Prompt "${prompt.name}" aktiviert und in chatStore gesetzt`)
  } catch (error) {
    console.error('Failed to activate system prompt:', error)
    errorToast('Fehler beim Aktivieren des System-Prompts')
  }
}

// Web Search Functions
async function loadWebSearchSettings() {
  try {
    const response = await fetch('/api/search/settings')
    if (response.ok) {
      const data = await response.json()
      webSearchSettings.value = {
        braveApiKey: data.braveApiKey || '',
        braveConfigured: data.braveConfigured || false,
        searchCount: data.searchCount || 0,
        searchLimit: data.searchLimit || 2000,
        remainingSearches: data.remainingSearches || 2000,
        currentMonth: data.currentMonth || '',
        customSearxngInstance: data.customSearxngInstance || '',
        searxngInstances: data.searxngInstances?.length > 0 ? data.searxngInstances : [...defaultSearxngInstances],
        searxngTotalCount: data.searxngTotalCount || 0,
        searxngMonthCount: data.searxngMonthCount || 0,
        // Feature Flags
        queryOptimizationEnabled: data.queryOptimizationEnabled ?? true,
        contentScrapingEnabled: data.contentScrapingEnabled ?? true,
        multiQueryEnabled: data.multiQueryEnabled ?? false,
        reRankingEnabled: data.reRankingEnabled ?? true,
        queryOptimizationModel: data.queryOptimizationModel || 'llama3.2:3b',
        effectiveOptimizationModel: data.effectiveOptimizationModel || null
      }
    }
  } catch (error) {
    console.error('Failed to load web search settings:', error)
    // Fallback auf Defaults
    webSearchSettings.value.searxngInstances = [...defaultSearxngInstances]
  }
}

async function saveWebSearchSettings() {
  try {
    const response = await secureFetch('/api/search/settings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        braveApiKey: webSearchSettings.value.braveApiKey,
        customSearxngInstance: webSearchSettings.value.customSearxngInstance,
        searxngInstances: webSearchSettings.value.searxngInstances.filter(i => i && i.trim()),
        // Feature Flags
        queryOptimizationEnabled: webSearchSettings.value.queryOptimizationEnabled,
        contentScrapingEnabled: webSearchSettings.value.contentScrapingEnabled,
        multiQueryEnabled: webSearchSettings.value.multiQueryEnabled,
        reRankingEnabled: webSearchSettings.value.reRankingEnabled,
        queryOptimizationModel: webSearchSettings.value.queryOptimizationModel
      })
    })
    if (!response.ok) {
      throw new Error('Failed to save')
    }
    // Reload to get updated status
    await loadWebSearchSettings()
  } catch (error) {
    console.error('Failed to save web search settings:', error)
    throw error
  }
}

async function testBraveSearch() {
  testingSearch.value = true
  try {
    await saveWebSearchSettings()
    const response = await secureFetch('/api/search/test', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: 'test' })
    })
    const result = await response.json()
    if (result.success) {
      success(`✓ Suche funktioniert! ${result.resultCount} Ergebnisse via ${result.source}`)
      await loadWebSearchSettings()
    } else {
      errorToast(`✗ Suche fehlgeschlagen: ${result.error || 'Unbekannter Fehler'}`)
    }
  } catch (error) {
    errorToast('✗ Test fehlgeschlagen: ' + error.message)
  } finally {
    testingSearch.value = false
  }
}

async function testCustomSearxng() {
  if (!webSearchSettings.value.customSearxngInstance) return
  testingSearch.value = true
  try {
    await saveWebSearchSettings()
    const response = await secureFetch('/api/search/test', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: 'test' })
    })
    const result = await response.json()
    if (result.success) {
      success(`✓ SearXNG funktioniert! ${result.resultCount} Ergebnisse`)
    } else {
      errorToast(`✗ SearXNG nicht erreichbar: ${result.error || 'Unbekannter Fehler'}`)
    }
  } catch (error) {
    errorToast('✗ Test fehlgeschlagen: ' + error.message)
  } finally {
    testingSearch.value = false
  }
}

function addSearxngInstance() {
  webSearchSettings.value.searxngInstances.push('')
}

function removeSearxngInstance(index) {
  webSearchSettings.value.searxngInstances.splice(index, 1)
}

async function resetSearxngInstances() {
  const confirmed = await confirm({
    title: 'Instanzen zurücksetzen?',
    message: 'Fallback-Instanzen auf Standard zurücksetzen?',
    type: 'warning',
    confirmText: 'Zurücksetzen'
  })
  if (confirmed) {
    webSearchSettings.value.searxngInstances = [...defaultSearxngInstances]
    success('Instanzen zurückgesetzt')
  }
}
</script>

<style scoped>
/* Custom Scrollbar */
.custom-scrollbar::-webkit-scrollbar {
  width: 8px;
}

.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(156, 163, 175, 0.3);
  border-radius: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(156, 163, 175, 0.5);
}

/* Modal Transitions */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active > div,
.modal-leave-active > div {
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.modal-enter-from > div {
  transform: scale(0.9) translateY(-20px);
}

.modal-leave-to > div {
  transform: scale(0.9) translateY(20px);
}
</style>
