<template>
  <div class="h-full flex flex-col bg-gray-50 dark:bg-gray-900">
    <!-- Header -->
    <div class="border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-6">
      <h1
        class="text-2xl font-bold text-gray-900 dark:text-white cursor-default select-none"
        @click="handleTitleClick"
        :title="developerMode ? 'Entwicklermodus aktiv' : ''"
      >
        ⚙️ Einstellungen
        <span v-if="developerMode" class="text-xs text-purple-500 ml-2">🔧 DEV</span>
      </h1>
      <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
        Konfiguriere die automatische Modellauswahl und andere Einstellungen
      </p>
    </div>

    <!-- Content -->
    <div class="flex-1 overflow-y-auto p-6">
      <div class="max-w-4xl mx-auto space-y-6">

        <!-- Provider Settings (nur im Entwicklermodus sichtbar) -->
        <ProviderSettings v-if="developerMode" />

        <!-- General Settings -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            🎨 Allgemeine Einstellungen
          </h2>

          <!-- TopBar Toggle -->
          <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-900/50 rounded-lg">
            <div>
              <label class="text-sm font-medium text-gray-700 dark:text-gray-300">
                TopBar anzeigen
              </label>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Zeige die obere Navigationsleiste mit Modellauswahl und Buttons
              </p>
            </div>
            <label class="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                v-model="settingsStore.settings.showTopBar"
                class="sr-only peer"
              >
              <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 dark:peer-focus:ring-blue-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-blue-600"></div>
            </label>
          </div>

          <!-- Welcome Tiles Toggle -->
          <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-900/50 rounded-lg mt-4">
            <div>
              <label class="text-sm font-medium text-gray-700 dark:text-gray-300">
                Vorschläge-Kacheln anzeigen
              </label>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Zeige Schnellauswahl-Kacheln auf dem Willkommensbildschirm
              </p>
            </div>
            <label class="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                v-model="settingsStore.settings.showWelcomeTiles"
                class="sr-only peer"
              >
              <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 dark:peer-focus:ring-blue-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-blue-600"></div>
            </label>
          </div>

          <!-- UI Theme Selection -->
          <div class="p-4 bg-gray-50 dark:bg-gray-900/50 rounded-lg mt-4">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Erscheinungsbild / Theme
            </label>
            <p class="text-xs text-gray-500 dark:text-gray-400 mb-3">
              Wähle das visuelle Design der Anwendung
            </p>
            <div class="grid grid-cols-2 gap-3">
              <!-- Default Theme -->
              <button
                @click="settingsStore.settings.uiTheme = 'default'"
                :class="[
                  'p-4 rounded-lg border-2 transition-all text-left',
                  settingsStore.settings.uiTheme === 'default' || !settingsStore.settings.uiTheme
                    ? 'border-purple-500 bg-purple-500/10'
                    : 'border-gray-300 dark:border-gray-600 hover:border-purple-400'
                ]"
              >
                <div class="flex items-center gap-3 mb-2">
                  <div class="w-8 h-8 rounded-full bg-gradient-to-br from-purple-500 to-indigo-600"></div>
                  <span class="font-medium text-gray-900 dark:text-white">Tech</span>
                </div>
                <p class="text-xs text-gray-500 dark:text-gray-400">
                  Dunkles Design mit lila Akzenten
                </p>
              </button>

              <!-- Lawyer Theme -->
              <button
                @click="settingsStore.settings.uiTheme = 'lawyer'"
                :class="[
                  'p-4 rounded-lg border-2 transition-all text-left',
                  settingsStore.settings.uiTheme === 'lawyer'
                    ? 'border-navy-700 bg-navy-100'
                    : 'border-gray-300 dark:border-gray-600 hover:border-navy-500'
                ]"
              >
                <div class="flex items-center gap-3 mb-2">
                  <div class="w-8 h-8 rounded-full bg-gradient-to-br from-navy-800 to-navy-900 border-2 border-gold-500"></div>
                  <span class="font-medium text-gray-900 dark:text-white">Anwalt</span>
                </div>
                <p class="text-xs text-gray-500 dark:text-gray-400">
                  Helles Design mit Navy Blue & Gold
                </p>
              </button>
            </div>
          </div>
        </div>

        <!-- Fleet Mates Settings -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            🤝 Fleet Mates Pairing
          </h2>
          <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">
            Verwalte gepairte Fleet Mates (ähnlich wie Bluetooth-Geräte)
          </p>

          <!-- Trusted Mates Count -->
          <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-900/50 rounded-lg mb-4">
            <div>
              <label class="text-sm font-medium text-gray-700 dark:text-gray-300">
                Vertrauenswürdige Mates
              </label>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Anzahl der aktuell gepairten Geräte
              </p>
            </div>
            <span class="px-3 py-1 bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-200 rounded-full font-semibold">
              {{ trustedMatesCount }}
            </span>
          </div>

          <!-- Forget All Button -->
          <div class="p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
            <div class="flex items-center justify-between">
              <div>
                <label class="text-sm font-medium text-red-700 dark:text-red-300">
                  Alle Mates vergessen
                </label>
                <p class="text-xs text-red-600 dark:text-red-400 mt-1">
                  Entfernt alle gepairten Geräte. Diese müssen erneut gepairt werden.
                </p>
              </div>
              <button
                @click="forgetAllMates"
                :disabled="trustedMatesCount === 0 || forgettingMates"
                class="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
              >
                <span v-if="forgettingMates">⏳</span>
                <span v-else>🗑️</span>
                {{ forgettingMates ? 'Wird vergessen...' : 'Alle vergessen' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Smart Model Selection -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <div class="flex items-start justify-between mb-4">
            <div>
              <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
                🤖 Intelligente Modellauswahl
              </h2>
              <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
                Automatisches Routing zu spezialisierten Modellen basierend auf der Aufgabe
              </p>
            </div>
            <label class="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                v-model="settings.enabled"
                class="sr-only peer"
                @change="saveSettings"
              >
              <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 dark:peer-focus:ring-blue-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-blue-600"></div>
            </label>
          </div>

          <div v-if="!settings.enabled" class="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-4 mb-4">
            <p class="text-sm text-yellow-800 dark:text-yellow-200">
              ⚠️ Intelligente Modellauswahl ist deaktiviert. Es wird immer das Standard-Modell verwendet.
            </p>
          </div>

          <div class="space-y-4" :class="{ 'opacity-50 pointer-events-none': !settings.enabled }">
            <!-- Code Model -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                💻 Code-Modell
                <span class="text-xs text-gray-500 dark:text-gray-400 ml-2">
                  (für Code-Generierung, Debugging, technische Fragen)
                </span>
              </label>
              <select
                v-model="settings.codeModel"
                @change="saveSettings"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option v-for="model in codeModels.length > 0 ? codeModels : availableModels" :key="model.name" :value="model.name">
                  {{ model.name }} ({{ formatSize(model.size) }})
                </option>
              </select>
              <p v-if="codeModels.length === 0" class="text-xs text-yellow-600 dark:text-yellow-400 mt-2">
                ⚠️ Keine Coder-Modelle gefunden - zeige alle Modelle
              </p>
            </div>

            <!-- Fast Model -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                ⚡ Schnelles Modell
                <span class="text-xs text-gray-500 dark:text-gray-400 ml-2">
                  (für einfache Fragen, Definitionen)
                </span>
              </label>
              <select
                v-model="settings.fastModel"
                @change="saveSettings"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option v-for="model in availableModels" :key="model.name" :value="model.name">
                  {{ model.name }} ({{ formatSize(model.size) }})
                </option>
              </select>
            </div>

            <!-- Vision Model -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                👁️ Vision-Modell
                <span class="text-xs text-gray-500 dark:text-gray-400 ml-2">
                  (für Bildanalyse PDF/JPEG/PNG)
                </span>
              </label>
              <select
                v-model="settings.visionModel"
                @change="saveSettings"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option v-for="model in visionModels.length > 0 ? visionModels : availableModels" :key="model.name" :value="model.name">
                  {{ model.name }} ({{ formatSize(model.size) }})
                </option>
              </select>
              <p v-if="visionModels.length === 0" class="text-xs text-yellow-600 dark:text-yellow-400 mt-2">
                ⚠️ Keine Vision-Modelle gefunden - zeige alle Modelle
              </p>
              <p v-else class="text-xs text-green-600 dark:text-green-400 mt-2">
                ✅ Wird automatisch bei Bild-Upload aktiviert
              </p>
            </div>

            <!-- Email Model -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                📧 Email-Modell
                <span class="text-xs text-gray-500 dark:text-gray-400 ml-2">
                  (für Email-Klassifizierung & Antwort-Generierung)
                </span>
              </label>
              <select
                v-model="settings.emailModel"
                @change="saveSettings"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="">-- Standard-Modell verwenden --</option>
                <option v-for="model in fastModels" :key="model.name" :value="model.name">
                  {{ model.name }} ({{ formatSize(model.size) }})
                </option>
              </select>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-2">
                💡 Tipp: Verwende ein schnelles, kleines Modell (1B-3B) für Email-Klassifizierung!
              </p>
            </div>

            <!-- Log Analysis Model -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                📊 Log-Analyse-Modell
                <span class="text-xs text-gray-500 dark:text-gray-400 ml-2">
                  (für Log-Datei-Analyse & Fehlersuche)
                </span>
              </label>
              <select
                v-model="settings.logAnalysisModel"
                @change="saveSettings"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="">-- Standard-Modell verwenden --</option>
                <option v-for="model in codeModels" :key="model.name" :value="model.name">
                  {{ model.name }} ({{ formatSize(model.size) }})
                </option>
              </select>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-2">
                💡 Tipp: Coder-Modelle (DeepSeek-Coder, Qwen2.5-Coder) sind ideal für Log-Analyse!
              </p>
            </div>

            <!-- Document Generation Model -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                📝 Brief-/Dokumenten-Modell
                <span class="text-xs text-gray-500 dark:text-gray-400 ml-2">
                  (für Brief-Generierung & formale Texte)
                </span>
              </label>
              <select
                v-model="settings.documentModel"
                @change="saveSettings"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="">-- Standard-Modell verwenden --</option>
                <option v-for="model in germanModels" :key="model.name" :value="model.name">
                  {{ model.name }} ({{ formatSize(model.size) }})
                </option>
              </select>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-2">
                💡 Tipp: Qwen-Modelle haben hervorragende Deutsch-Kenntnisse für Briefe!
              </p>
            </div>
          </div>
        </div>

        <!-- Vision-to-Model Chaining -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <div class="flex items-start justify-between mb-4">
            <div>
              <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
                🔗 Vision-to-Model Chaining
              </h2>
              <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
                Automatische Bild-Analyse mit LLaVA → Übergabe an aktuelles Modell
              </p>
            </div>
            <label class="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                v-model="chainingSettings.enabled"
                class="sr-only peer"
                @change="saveChainingSettings"
              >
              <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-purple-300 dark:peer-focus:ring-purple-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-purple-600"></div>
            </label>
          </div>

          <div v-if="!chainingSettings.enabled" class="bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg p-4 mb-4">
            <p class="text-sm text-yellow-800 dark:text-yellow-200">
              ⚠️ Vision-Chaining ist deaktiviert. Bilder werden nur mit Vision-Modell analysiert.
            </p>
          </div>

          <div v-else class="bg-purple-50 dark:bg-purple-900/20 border border-purple-200 dark:border-purple-800 rounded-lg p-4 mb-4">
            <p class="text-sm text-purple-800 dark:text-purple-200">
              ✨ <strong>Aktiv:</strong> PNG/JPEG/PDF → LLaVA analysiert → Beschreibung → Aktuelles Modell (Chat/Code/etc.)
            </p>
          </div>

          <div class="space-y-4" :class="{ 'opacity-50 pointer-events-none': !chainingSettings.enabled }">
            <!-- Vision Model Selection -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                👁️ Vision-Modell für Chaining
                <span class="text-xs text-gray-500 dark:text-gray-400 ml-2">
                  (Bild-Analyse im ersten Schritt)
                </span>
              </label>
              <select
                v-model="chainingSettings.visionModel"
                @change="saveChainingSettings"
                class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-purple-500 focus:border-transparent"
              >
                <option value="llava:7b">llava:7b (4.5 GB) - ⚡ Schnell</option>
                <option value="llava:13b">llava:13b (8.0 GB) - ⚖️ Ausgewogen</option>
                <option value="llava:34b">llava:34b (19 GB) - 🏆 Beste Qualität</option>
              </select>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-2">
                💡 Empfohlen: llava:7b für schnelle Antworten
              </p>
            </div>

            <!-- Show Intermediate Output -->
            <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-900/50 rounded-lg">
              <div>
                <label class="text-sm font-medium text-gray-700 dark:text-gray-300">
                  📄 LLaVA-Zwischenergebnis anzeigen
                </label>
                <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                  Zeige Bildbeschreibung vor finaler Antwort
                </p>
              </div>
              <label class="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  v-model="chainingSettings.showIntermediateOutput"
                  class="sr-only peer"
                  @change="saveChainingSettings"
                >
                <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-purple-300 dark:peer-focus:ring-purple-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-purple-600"></div>
              </label>
            </div>

            <!-- Supported File Types -->
            <div class="p-4 bg-gray-50 dark:bg-gray-900/50 rounded-lg">
              <label class="text-sm font-medium text-gray-700 dark:text-gray-300 block mb-2">
                📎 Unterstützte Dateiformate
              </label>
              <div class="flex flex-wrap gap-2">
                <span class="px-3 py-1 bg-purple-100 dark:bg-purple-900/40 text-purple-700 dark:text-purple-300 rounded-full text-xs font-medium">
                  .png
                </span>
                <span class="px-3 py-1 bg-purple-100 dark:bg-purple-900/40 text-purple-700 dark:text-purple-300 rounded-full text-xs font-medium">
                  .jpg / .jpeg
                </span>
                <span class="px-3 py-1 bg-purple-100 dark:bg-purple-900/40 text-purple-700 dark:text-purple-300 rounded-full text-xs font-medium">
                  .gif
                </span>
                <span class="px-3 py-1 bg-purple-100 dark:bg-purple-900/40 text-purple-700 dark:text-purple-300 rounded-full text-xs font-medium">
                  .webp
                </span>
                <span class="px-3 py-1 bg-purple-100 dark:bg-purple-900/40 text-purple-700 dark:text-purple-300 rounded-full text-xs font-medium">
                  .pdf
                </span>
              </div>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-2">
                ℹ️ Diese Formate werden automatisch erkannt und via LLaVA verarbeitet
              </p>
            </div>

            <!-- Example Workflow -->
            <div class="p-4 bg-gradient-to-r from-purple-50 to-blue-50 dark:from-purple-900/20 dark:to-blue-900/20 border border-purple-200 dark:border-purple-700 rounded-lg">
              <label class="text-sm font-medium text-gray-700 dark:text-gray-300 block mb-2">
                🔄 Beispiel-Workflow
              </label>
              <div class="space-y-2 text-xs text-gray-600 dark:text-gray-400">
                <div class="flex items-center gap-2">
                  <span class="text-purple-600 dark:text-purple-400">1.</span>
                  <span>Du lädst <strong>code-screenshot.png</strong> hoch</span>
                </div>
                <div class="flex items-center gap-2">
                  <span class="text-purple-600 dark:text-purple-400">2.</span>
                  <span><strong>LLaVA</strong> analysiert: "Python code with function..."</span>
                </div>
                <div class="flex items-center gap-2">
                  <span class="text-purple-600 dark:text-purple-400">3.</span>
                  <span>Beschreibung → <strong>Dein aktuelles Modell</strong> (z.B. qwen2.5-coder:14b)</span>
                </div>
                <div class="flex items-center gap-2">
                  <span class="text-purple-600 dark:text-purple-400">4.</span>
                  <span><strong>Finale Antwort</strong> in deiner Sprache (DE/TR/ES/EN/FR)</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Hardware/Performance Settings -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            🖥️ Hardware & Performance
          </h2>
          <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">
            Einstellungen für GPU-Beschleunigung und Hardware-Nutzung
          </p>

          <!-- CPU-Only Mode Toggle -->
          <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-900/50 rounded-lg">
            <div>
              <label class="text-sm font-medium text-gray-700 dark:text-gray-300">
                CPU-Modus (ohne GPU)
              </label>
              <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Deaktiviert CUDA/GPU-Beschleunigung für Demos auf Laptops ohne NVIDIA
              </p>
            </div>
            <label class="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                v-model="settingsStore.settings.cpuOnly"
                class="sr-only peer"
              >
              <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-orange-300 dark:peer-focus:ring-orange-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-orange-500"></div>
            </label>
          </div>

          <!-- Info Box when CPU-Only is active -->
          <div v-if="settingsStore.settings.cpuOnly" class="mt-4 bg-orange-50 dark:bg-orange-900/20 border border-orange-200 dark:border-orange-800 rounded-lg p-4">
            <p class="text-sm text-orange-800 dark:text-orange-200">
              ⚠️ <strong>CPU-Modus aktiv:</strong> Ollama verwendet nur die CPU. Die Antworten sind langsamer, aber ideal für Demos auf Hardware ohne NVIDIA GPU.
            </p>
          </div>

          <!-- Info about GPU usage when inactive -->
          <div v-else class="mt-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4">
            <p class="text-sm text-green-800 dark:text-green-200">
              ✅ <strong>GPU-Beschleunigung aktiv:</strong> Ollama nutzt automatisch verfügbare NVIDIA GPUs für maximale Performance.
            </p>
          </div>
        </div>

        <!-- Default Model -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">
            🎯 Standard-Modell
          </h2>
          <p class="text-sm text-gray-500 dark:text-gray-400 mb-4">
            Dieses Modell wird für neue Chats verwendet und als Fallback, wenn keine automatische Auswahl möglich ist.
          </p>

          <div v-if="settings.enabled" class="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4 mb-4">
            <p class="text-sm text-blue-800 dark:text-blue-200">
              ℹ️ <strong>Hinweis:</strong> Wenn die intelligente Modellauswahl aktiviert ist, wird das Standard-Modell
              automatisch durch das passende spezialisierte Modell ersetzt (Code-, Fast- oder Vision-Modell).
            </p>
          </div>

          <select
            v-model="settings.defaultModel"
            @change="saveSettings"
            class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option v-for="model in availableModels" :key="model.name" :value="model.name">
              {{ model.name }} ({{ formatSize(model.size) }})
            </option>
          </select>
        </div>

        <!-- Info Panel -->
        <div class="bg-gradient-to-r from-blue-50 to-purple-50 dark:from-blue-900/20 dark:to-purple-900/20 rounded-lg border border-blue-200 dark:border-blue-800 p-6">
          <h3 class="text-md font-semibold text-gray-900 dark:text-white mb-3">
            📚 Wie funktioniert die intelligente Modellauswahl?
          </h3>
          <ul class="space-y-2 text-sm text-gray-700 dark:text-gray-300">
            <li class="flex items-start">
              <span class="mr-2">💻</span>
              <span><strong>Code-Aufgaben:</strong> Automatische Erkennung von Code-Keywords (function, class, bug, etc.) und technischen Mustern → Code-Modell wird verwendet</span>
            </li>
            <li class="flex items-start">
              <span class="mr-2">⚡</span>
              <span><strong>Einfache Fragen:</strong> Kurze Fragen mit "Was ist", "Erkläre", etc. → Schnelles Modell für effiziente Antworten</span>
            </li>
            <li class="flex items-start">
              <span class="mr-2">🎯</span>
              <span><strong>Komplexe Aufgaben:</strong> Alle anderen Anfragen → Standard-Modell wird verwendet</span>
            </li>
            <li class="flex items-start">
              <span class="mr-2">👁️</span>
              <span><strong>Bilder:</strong> Wenn Bilder hochgeladen werden → Vision-Modell wird automatisch verwendet</span>
            </li>
          </ul>
        </div>

        <!-- Sampling Parameters -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <div class="mb-4">
            <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
              🎛️ LLM Sampling Parameter
            </h2>
            <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
              Feinsteuerung aller Parameter für text- und vision-basierte Modelle
            </p>
          </div>

          <div class="p-4 bg-red-100 border-2 border-red-500 text-red-900 font-bold text-xl">
            TEST: Wenn du das hier siehst, wird die Sektion geladen!
          </div>

          <SamplingParametersPanel
            v-model="defaultSamplingParams"
            :model-name="settings.defaultModel"
          />

          <div class="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
            <button
              @click="saveSamplingParams"
              class="w-full px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors"
            >
              <i class="fas fa-save mr-2"></i>
              Parameter als Standard speichern
            </button>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-2 text-center">
              Diese Parameter werden für alle neuen Chats als Standard verwendet
            </p>
          </div>
        </div>

        <!-- System Prompt Management -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
          <div class="flex items-center justify-between mb-4">
            <div>
              <h2 class="text-lg font-semibold text-gray-900 dark:text-white">
                📝 System-Prompts Verwaltung
              </h2>
              <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">
                Erstelle und verwalte wiederverwendbare System-Prompts für deine Chats
              </p>
            </div>
            <button
              @click="showPromptEditor = true; editingPrompt = null"
              class="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors flex items-center gap-2"
            >
              <i class="fas fa-plus"></i>
              Neuer Prompt
            </button>
          </div>

          <!-- Prompts List -->
          <div v-if="systemPrompts.length === 0" class="text-center py-8 text-gray-500 dark:text-gray-400">
            <i class="fas fa-inbox text-4xl mb-3 opacity-30"></i>
            <p>Keine System-Prompts vorhanden</p>
            <p class="text-sm mt-2">Erstelle deinen ersten System-Prompt mit dem Button oben</p>
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="prompt in systemPrompts"
              :key="prompt.id"
              class="border border-gray-200 dark:border-gray-700 rounded-lg p-4 hover:border-blue-500 dark:hover:border-blue-400 transition-colors"
            >
              <div class="flex items-start justify-between gap-4">
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-2">
                    <h3 class="font-semibold text-gray-900 dark:text-white">
                      {{ prompt.name }}
                    </h3>
                  </div>
                  <p class="text-sm text-gray-600 dark:text-gray-400 line-clamp-2">
                    {{ prompt.content }}
                  </p>
                </div>
                <div class="flex items-center gap-2">
                  <!-- Aktivieren Button -->
                  <button
                    @click="activatePrompt(prompt)"
                    :class="[
                      'px-3 py-1.5 rounded-lg text-sm font-medium transition-colors',
                      prompt.isDefault
                        ? 'bg-green-500 text-white cursor-default'
                        : 'bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300 hover:bg-purple-200 dark:hover:bg-purple-900/50'
                    ]"
                    :disabled="prompt.isDefault"
                    :title="prompt.isDefault ? 'Bereits aktiv' : 'Diesen Prompt aktivieren'"
                  >
                    {{ prompt.isDefault ? '✓ Aktiv' : 'Aktivieren' }}
                  </button>
                  <button
                    @click="editPrompt(prompt)"
                    class="p-2 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-lg transition-colors"
                    title="Bearbeiten"
                  >
                    <i class="fas fa-edit"></i>
                  </button>
                  <button
                    @click="deletePrompt(prompt.id)"
                    class="p-2 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors"
                    title="Löschen"
                  >
                    <i class="fas fa-trash"></i>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- System Prompt Editor Modal -->
        <div v-if="showPromptEditor" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div class="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div class="p-6 border-b border-gray-200 dark:border-gray-700">
              <h3 class="text-xl font-semibold text-gray-900 dark:text-white">
                {{ editingPrompt ? 'System-Prompt bearbeiten' : 'Neuer System-Prompt' }}
              </h3>
            </div>

            <div class="p-6 space-y-4">
              <!-- Name -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  Name
                </label>
                <input
                  v-model="promptForm.name"
                  type="text"
                  placeholder="z.B. Java-Experte, Code-Reviewer, ..."
                  class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
              </div>

              <!-- Content -->
              <div>
                <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                  System-Prompt Text
                </label>
                <textarea
                  v-model="promptForm.content"
                  rows="10"
                  placeholder="Du bist ein hilfreicher Assistent, der..."
                  class="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-y"
                ></textarea>
                <p class="text-xs text-gray-500 dark:text-gray-400 mt-2">
                  {{ promptForm.content.length }} Zeichen
                </p>
              </div>

              <!-- Is Default -->
              <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-900/50 rounded-lg">
                <div>
                  <label class="text-sm font-medium text-gray-700 dark:text-gray-300">
                    Als Standard-Prompt setzen
                  </label>
                  <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    Dieser Prompt wird automatisch für neue Chats verwendet
                  </p>
                </div>
                <label class="relative inline-flex items-center cursor-pointer">
                  <input
                    type="checkbox"
                    v-model="promptForm.isDefault"
                    class="sr-only peer"
                  >
                  <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 dark:peer-focus:ring-blue-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-blue-600"></div>
                </label>
              </div>
            </div>

            <div class="p-6 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-3">
              <button
                @click="showPromptEditor = false; editingPrompt = null"
                class="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
              >
                Abbrechen
              </button>
              <button
                @click="savePrompt"
                :disabled="!promptForm.name.trim() || !promptForm.content.trim()"
                class="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <i class="fas fa-save mr-2"></i>
                {{ editingPrompt ? 'Aktualisieren' : 'Erstellen' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Save Status -->
        <div v-if="saveStatus" class="text-center">
          <div class="inline-flex items-center px-4 py-2 rounded-lg"
               :class="saveStatus.success ? 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-200' : 'bg-red-100 dark:bg-red-900/30 text-red-800 dark:text-red-200'">
            <span class="mr-2">{{ saveStatus.success ? '✅' : '❌' }}</span>
            <span>{{ saveStatus.message }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import * as api from '../services/api'
import { secureFetch } from '../utils/secureFetch'
import ProviderSettings from './ProviderSettings.vue'
import SamplingParametersPanel from './SamplingParametersPanel.vue'
import { useSettingsStore } from '../stores/settingsStore'
import { useChatStore } from '../stores/chatStore'
import { useConfirmDialog } from '../composables/useConfirmDialog'

const { confirm, confirmDelete } = useConfirmDialog()

const settingsStore = useSettingsStore()
const chatStore = useChatStore()

// Developer Mode - 7x auf Titel klicken um zu aktivieren
const developerMode = ref(false)
const titleClickCount = ref(0)
const titleClickTimeout = ref(null)

function handleTitleClick() {
  titleClickCount.value++

  // Reset nach 2 Sekunden ohne Klick
  if (titleClickTimeout.value) {
    clearTimeout(titleClickTimeout.value)
  }
  titleClickTimeout.value = setTimeout(() => {
    titleClickCount.value = 0
  }, 2000)

  // Nach 7 Klicks: Developer-Modus umschalten
  if (titleClickCount.value >= 7) {
    developerMode.value = !developerMode.value
    titleClickCount.value = 0

    if (developerMode.value) {
      console.log('🔧 Entwicklermodus aktiviert - Provider-Einstellungen sichtbar')
      saveStatus.value = { success: true, message: '🔧 Entwicklermodus aktiviert!' }
    } else {
      console.log('🔧 Entwicklermodus deaktiviert')
      saveStatus.value = { success: true, message: 'Entwicklermodus deaktiviert' }
    }

    setTimeout(() => {
      saveStatus.value = null
    }, 3000)
  }
}

// Fleet Mates Pairing
const trustedMatesCount = ref(0)
const forgettingMates = ref(false)

const settings = ref({
  enabled: true,
  codeModel: 'qwen2.5-coder:7b',
  fastModel: 'llama3.2:3b',
  visionModel: 'llava:13b',
  emailModel: '',
  logAnalysisModel: '',
  documentModel: '',
  defaultModel: 'qwen2.5-coder:7b'
})

const chainingSettings = ref({
  enabled: true,
  visionModel: 'llava:7b',
  showIntermediateOutput: false
})

const availableModels = ref([])
const saveStatus = ref(null)
const defaultSamplingParams = ref({})

// System Prompts Management
const systemPrompts = ref([])
const showPromptEditor = ref(false)
const editingPrompt = ref(null)
const promptForm = ref({
  name: '',
  content: '',
  isDefault: false
})

// Gefilterte Model-Listen basierend auf Capabilities
const visionModels = computed(() => {
  return availableModels.value.filter(m =>
    m.name.toLowerCase().includes('llava') ||
    m.name.toLowerCase().includes('bakllava') ||
    m.name.toLowerCase().includes('minicpm-v') ||
    m.name.toLowerCase().includes('vision')
  )
})

const codeModels = computed(() => {
  return availableModels.value.filter(m =>
    m.name.toLowerCase().includes('coder') ||
    m.name.toLowerCase().includes('code') ||
    m.name.toLowerCase().includes('deepseek') ||
    m.name.toLowerCase().includes('starcoder')
  )
})

const fastModels = computed(() => {
  return availableModels.value.filter(m =>
    m.name.toLowerCase().includes('1b') ||
    m.name.toLowerCase().includes('3b') ||
    m.name.toLowerCase().includes('tiny') ||
    m.name.toLowerCase().includes('mini')
  )
})

const germanModels = computed(() => {
  return availableModels.value.filter(m =>
    m.name.toLowerCase().includes('qwen') ||
    m.name.toLowerCase().includes('german') ||
    m.name.toLowerCase().includes('leo')
  )
})

onMounted(async () => {
  await loadSettings()
  await loadAvailableModels()
  loadChainingSettings()
  await loadSystemPrompts()
  await loadTrustedMatesCount()
})

async function loadSettings() {
  try {
    const response = await api.getModelSelectionSettings()
    settings.value = response

    // Load task-specific models separately
    const emailModel = await api.getEmailModel()
    settings.value.emailModel = emailModel || ''

    const logAnalysisModel = await api.getLogAnalysisModel()
    settings.value.logAnalysisModel = logAnalysisModel || ''

    const documentModel = await api.getDocumentModel()
    settings.value.documentModel = documentModel || ''
  } catch (error) {
    console.error('Failed to load settings:', error)
  }
}

async function loadAvailableModels() {
  try {
    const response = await api.getAvailableModels()
    availableModels.value = response
  } catch (error) {
    console.error('Failed to load models:', error)
  }
}

async function saveSettings() {
  try {
    // Save model selection settings (without task-specific models)
    const { emailModel, logAnalysisModel, documentModel, ...modelSelectionSettings } = settings.value
    await api.updateModelSelectionSettings(modelSelectionSettings)

    // Save task-specific models separately
    await api.updateEmailModel(emailModel)
    await api.updateLogAnalysisModel(logAnalysisModel)
    await api.updateDocumentModel(documentModel)

    saveStatus.value = { success: true, message: 'Einstellungen erfolgreich gespeichert!' }

    // Clear status after 3 seconds
    setTimeout(() => {
      saveStatus.value = null
    }, 3000)
  } catch (error) {
    console.error('Failed to save settings:', error)
    saveStatus.value = { success: false, message: 'Fehler beim Speichern der Einstellungen' }
  }
}

function loadChainingSettings() {
  try {
    const saved = localStorage.getItem('chainingSettings')
    if (saved) {
      chainingSettings.value = JSON.parse(saved)
      console.log('📥 Chaining settings loaded:', chainingSettings.value)
    } else {
      console.log('🔗 Using default chaining settings')
    }
  } catch (error) {
    console.error('Failed to load chaining settings:', error)
  }
}

function saveChainingSettings() {
  try {
    localStorage.setItem('chainingSettings', JSON.stringify(chainingSettings.value))
    console.log('💾 Chaining settings saved:', chainingSettings.value)

    saveStatus.value = { success: true, message: 'Vision-Chaining Einstellungen gespeichert!' }

    // Clear status after 3 seconds
    setTimeout(() => {
      saveStatus.value = null
    }, 3000)
  } catch (error) {
    console.error('Failed to save chaining settings:', error)
    saveStatus.value = { success: false, message: 'Fehler beim Speichern der Chaining-Einstellungen' }
  }
}

async function saveSamplingParams() {
  try {
    // Speichere Sampling Parameters im localStorage für neue Chats
    localStorage.setItem('defaultSamplingParams', JSON.stringify(defaultSamplingParams.value))

    saveStatus.value = { success: true, message: 'Sampling Parameter erfolgreich gespeichert!' }

    // Clear status after 3 seconds
    setTimeout(() => {
      saveStatus.value = null
    }, 3000)
  } catch (error) {
    console.error('Failed to save sampling params:', error)
    saveStatus.value = { success: false, message: 'Fehler beim Speichern der Parameter' }
  }
}

function formatSize(bytes) {
  if (!bytes) return 'N/A'
  const gb = bytes / (1024 * 1024 * 1024)
  return `${gb.toFixed(1)} GB`
}

// System Prompts Functions
async function loadSystemPrompts() {
  try {
    const prompts = await api.getAllSystemPrompts()
    systemPrompts.value = prompts
  } catch (error) {
    console.error('Failed to load system prompts:', error)
    saveStatus.value = { success: false, message: 'Fehler beim Laden der System-Prompts' }
  }
}

// System-Prompt aktivieren (in TopBar anzeigen und als Standard in DB speichern)
async function activatePrompt(prompt) {
  try {
    // 1. Als Standard in der Datenbank speichern
    await api.setDefaultSystemPrompt(prompt.id)

    // 2. Im chatStore setzen (für sofortige UI-Aktualisierung)
    chatStore.systemPrompt = prompt.content
    chatStore.systemPromptTitle = prompt.name

    // 3. Prompts neu laden (um isDefault-Status zu aktualisieren)
    await loadSystemPrompts()

    console.log(`✅ System-Prompt "${prompt.name}" aktiviert und als Standard gespeichert`)
    saveStatus.value = { success: true, message: `"${prompt.name}" aktiviert!` }

    setTimeout(() => {
      saveStatus.value = null
    }, 2000)
  } catch (error) {
    console.error('Failed to activate prompt:', error)
    saveStatus.value = { success: false, message: 'Fehler beim Aktivieren des Prompts' }
  }
}

function editPrompt(prompt) {
  editingPrompt.value = prompt
  promptForm.value = {
    name: prompt.name,
    content: prompt.content,
    isDefault: prompt.isDefault || false
  }
  showPromptEditor.value = true
}

async function savePrompt() {
  try {
    if (!promptForm.value.name.trim() || !promptForm.value.content.trim()) {
      saveStatus.value = { success: false, message: 'Name und Inhalt dürfen nicht leer sein' }
      return
    }

    if (editingPrompt.value) {
      // Update existing prompt
      await api.updateSystemPrompt(editingPrompt.value.id, promptForm.value)
      saveStatus.value = { success: true, message: 'System-Prompt erfolgreich aktualisiert!' }
    } else {
      // Create new prompt
      await api.createSystemPrompt(promptForm.value)
      saveStatus.value = { success: true, message: 'System-Prompt erfolgreich erstellt!' }
    }

    // Reload prompts and close editor
    await loadSystemPrompts()
    showPromptEditor.value = false
    editingPrompt.value = null
    promptForm.value = { name: '', content: '', isDefault: false }

    // Clear status after 3 seconds
    setTimeout(() => {
      saveStatus.value = null
    }, 3000)
  } catch (error) {
    console.error('Failed to save system prompt:', error)
    saveStatus.value = { success: false, message: 'Fehler beim Speichern des System-Prompts' }
  }
}

async function deletePrompt(promptId) {
  const confirmed = await confirmDelete('System-Prompt', 'Diese Aktion kann nicht rückgängig gemacht werden.')
  if (!confirmed) return

  try {
    await api.deleteSystemPrompt(promptId)
    saveStatus.value = { success: true, message: 'System-Prompt erfolgreich gelöscht!' }
    await loadSystemPrompts()

    // Clear status after 3 seconds
    setTimeout(() => {
      saveStatus.value = null
    }, 3000)
  } catch (error) {
    console.error('Failed to delete system prompt:', error)
    saveStatus.value = { success: false, message: 'Fehler beim Löschen des System-Prompts' }
  }
}

// Fleet Mates Functions
async function loadTrustedMatesCount() {
  try {
    const response = await fetch('/api/pairing/trusted')
    if (response.ok) {
      const mates = await response.json()
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
      trustedMatesCount.value = 0
      saveStatus.value = { success: true, message: 'Alle Mates wurden vergessen!' }
    } else {
      throw new Error('Failed to forget mates')
    }

    // Clear status after 3 seconds
    setTimeout(() => {
      saveStatus.value = null
    }, 3000)
  } catch (error) {
    console.error('Failed to forget all mates:', error)
    saveStatus.value = { success: false, message: 'Fehler beim Vergessen der Mates' }
  } finally {
    forgettingMates.value = false
  }
}
</script>
