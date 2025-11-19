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
        <div class="flex border-b border-gray-200 dark:border-gray-700 px-6 bg-gray-50/50 dark:bg-gray-900/50">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            @click="activeTab = tab.id"
            class="flex items-center gap-2 px-4 py-3 text-sm font-medium transition-all relative"
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
          </section>
          </div>

          <!-- TAB: LLM Provider -->
          <div v-if="activeTab === 'providers'">
            <ProviderSettings />
          </div>

          <!-- TAB: Model Selection -->
          <div v-if="activeTab === 'models'">
          <!-- Smart Model Selection -->
          <section class="bg-gradient-to-br from-gray-50 to-gray-100 dark:from-gray-900 dark:to-gray-800 p-5 rounded-xl border border-gray-200/50 dark:border-gray-700/50 shadow-sm">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
              <CpuChipIcon class="w-5 h-5 text-purple-500" />
              Intelligente Modellauswahl
            </h3>

            <!-- Enable/Disable Toggle -->
            <div class="mb-4 p-4 rounded-xl bg-white/50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700">
              <div class="flex items-center justify-between">
                <div class="flex-1">
                  <label class="block text-sm font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-2">
                    <SparklesIcon class="w-4 h-4 text-purple-500" />
                    Automatisches Modell-Routing
                  </label>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    Wählt automatisch das beste Modell basierend auf der Aufgabe
                  </p>
                </div>
                <ToggleSwitch v-model="modelSelectionSettings.enabled" color="purple" />
              </div>
            </div>

            <div v-if="!modelSelectionSettings.enabled" class="mb-4 p-3 rounded-xl bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-700/50">
              <div class="flex items-start gap-2">
                <ExclamationTriangleIcon class="w-5 h-5 text-yellow-600 dark:text-yellow-400 flex-shrink-0 mt-0.5" />
                <p class="text-xs text-yellow-800 dark:text-yellow-200">
                  Intelligente Modellauswahl ist deaktiviert
                </p>
              </div>
            </div>

            <div class="space-y-3" :class="{ 'opacity-50 pointer-events-none': !modelSelectionSettings.enabled }">
              <!-- Code Model -->
              <div>
                <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-1.5">
                  <CodeBracketIcon class="w-4 h-4 text-blue-500" />
                  Code-Modell
                  <span class="text-xs text-gray-500 dark:text-gray-400">(für Code, Debugging)</span>
                </label>
                <select
                  v-model="modelSelectionSettings.codeModel"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-blue-500"
                >
                  <option v-for="model in codeModels" :key="model" :value="model">
                    {{ model }}
                  </option>
                </select>
              </div>

              <!-- Fast Model -->
              <div>
                <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-1.5">
                  <BoltIcon class="w-4 h-4 text-green-500" />
                  Schnelles Modell
                  <span class="text-xs text-gray-500 dark:text-gray-400">(für einfache Fragen)</span>
                </label>
                <select
                  v-model="modelSelectionSettings.fastModel"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-green-500"
                >
                  <option v-for="model in availableModels" :key="model.name" :value="model.name">
                    {{ model.name }}
                  </option>
                </select>
              </div>

              <!-- Default Model -->
              <div>
                <label class="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1 flex items-center gap-1.5">
                  <CubeIcon class="w-4 h-4 text-purple-500" />
                  Standard-Modell
                  <span class="text-xs text-gray-500 dark:text-gray-400">(Fallback)</span>
                </label>
                <select
                  v-model="modelSelectionSettings.defaultModel"
                  class="w-full px-3 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white transition-all focus:ring-2 focus:ring-purple-500"
                >
                  <option v-for="model in availableModels" :key="model.name" :value="model.name">
                    {{ model.name }}
                  </option>
                </select>
              </div>
            </div>

            <div class="mt-3 p-3 rounded-xl bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700/50">
              <div class="flex items-start gap-2">
                <InformationCircleIcon class="w-5 h-5 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" />
                <p class="text-xs text-blue-800 dark:text-blue-200">
                  <strong>Hinweis:</strong> Wenn aktiviert, wird das Modell automatisch basierend auf deiner Frage ausgewählt.
                </p>
              </div>
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
  HashtagIcon,
  BugAntIcon,
  ArrowPathIcon,
  CheckIcon,
  UserIcon,
  TrashIcon,
  ShieldExclamationIcon
} from '@heroicons/vue/24/outline'
import { useSettingsStore } from '../stores/settingsStore'
import { useChatStore } from '../stores/chatStore'
import PersonalInfoTab from './PersonalInfoTab.vue'
import ProviderSettings from './ProviderSettings.vue'
import { useToast } from '../composables/useToast'
import api from '../services/api'
import ToggleSwitch from './ToggleSwitch.vue'
import SimpleSamplingParams from './SimpleSamplingParams.vue'
import { filterVisionModels, filterCodeModels } from '../utils/modelFilters'

const { success, error: errorToast } = useToast()

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

// Ref to PersonalInfoTab
const personalInfoTabRef = ref(null)

// Tab configuration
const tabs = [
  { id: 'general', label: 'Allgemein', icon: GlobeAltIcon },
  { id: 'providers', label: 'LLM Provider', icon: CpuChipIcon },
  { id: 'models', label: 'Modellauswahl', icon: CubeIcon },
  { id: 'parameters', label: 'Parameter', icon: AdjustmentsHorizontalIcon },
  { id: 'templates', label: 'System-Prompts', icon: DocumentTextIcon },
  { id: 'personal', label: 'Persönliche Daten', icon: UserIcon },
  { id: 'agents', label: 'Agents', icon: SparklesIcon },
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

// Available models
const availableModels = ref([])

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
  await loadSystemPrompts()
})

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

function resetToDefaults() {
  if (confirm('Alle Einstellungen auf Standard zurücksetzen?')) {
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
  if (!confirm('Möchtest du diesen System-Prompt wirklich löschen?')) {
    return
  }

  try {
    await api.deleteSystemPrompt(promptId)
    success('System-Prompt erfolgreich gelöscht!')
    await loadSystemPrompts()
  } catch (error) {
    console.error('Failed to delete system prompt:', error)
    errorToast('Fehler beim Löschen des System-Prompts')
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
