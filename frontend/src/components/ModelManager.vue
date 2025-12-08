<template>
  <!-- GGUF Model Config Modal -->
  <GgufModelConfigModal
    :show="showGgufConfigModal"
    :config="currentGgufConfig"
    :availableGgufModels="ggufModels"
    @close="closeGgufConfigModal"
    @saved="handleGgufConfigSaved"
  />

  <!-- Model Download Modal for llama.cpp -->
  <ModelDownloadModal
    :isVisible="showLlamaCppDownloadModal"
    :currentModel="currentDownloadModel"
    :progress="currentDownloadProgress"
    :downloadedSize="currentDownloadedSize"
    :totalSize="currentTotalSize"
    :speed="currentSpeed"
    :statusMessages="downloadStatusMessages"
    @cancel="cancelLlamaCppDownload"
  />

  <!-- Create Expert Modal (for editing) -->
  <CreateExpertModal
    :show="showCreateExpertModal"
    :expert="editingExpertForCreate"
    :available-models="availableModelsForExpert"
    @close="closeCreateExpertModal"
    @saved="onExpertSaved"
  />

  <!-- Expert Creation Wizard (for new experts) -->
  <ExpertCreationWizard
    :show="showExpertWizard"
    @close="closeExpertWizard"
    @created="onExpertCreatedFromWizard"
  />

  <div class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div
        class="
          bg-white/90 dark:bg-gray-800/90
          backdrop-blur-xl backdrop-saturate-150
          rounded-2xl shadow-2xl
          w-full max-w-5xl
          border border-gray-200/50 dark:border-gray-700/50
          flex flex-col
          transform transition-all duration-300
        "
        style="height: 85vh; max-height: 85vh;"
      >
        <!-- Header with Gradient - NICHT SCHRUMPFEN -->
        <div class="
          flex items-center justify-between p-6
          bg-gradient-to-r from-purple-500/10 to-indigo-500/10
          dark:from-purple-500/20 dark:to-indigo-500/20
          border-b border-gray-200/50 dark:border-gray-700/50
          flex-shrink-0
        ">
          <div class="flex items-center gap-4">
            <div class="p-3 rounded-xl bg-gradient-to-br from-purple-500 to-indigo-500 shadow-lg">
              <CpuChipIcon class="w-7 h-7 text-white" />
            </div>
            <h2 class="text-2xl font-bold bg-gradient-to-r from-gray-900 to-gray-700 dark:from-white dark:to-gray-300 bg-clip-text text-transparent">
              Modellverwaltung
            </h2>
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

        <!-- Main Tabs - NICHT SCHRUMPFEN -->
        <div class="border-b border-gray-200/50 dark:border-gray-700/50 bg-gray-50/50 dark:bg-gray-900/50 flex-shrink-0">
          <div class="flex">
            <button
              @click="activeTab = 'installed'"
              class="
                px-6 py-3 font-medium transition-all duration-200
                flex items-center gap-2
                relative
              "
              :class="activeTab === 'installed'
                ? 'text-fleet-orange-500'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            >
              <ServerIcon class="w-5 h-5" />
              <span>Installierte Modelle</span>
              <div v-if="activeTab === 'installed'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r from-fleet-orange-500 to-orange-600"></div>
            </button>
            <button
              @click="activeTab = 'available'"
              class="
                px-6 py-3 font-medium transition-all duration-200
                flex items-center gap-2
                relative
              "
              :class="activeTab === 'available'
                ? 'text-fleet-orange-500'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            >
              <GlobeAltIcon class="w-5 h-5" />
              <span>Verfügbare Modelle</span>
              <div v-if="activeTab === 'available'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-gradient-to-r from-fleet-orange-500 to-orange-600"></div>
            </button>
          </div>
        </div>

        <!-- Hero Section - Currently Selected Model -->
        <div class="px-6 py-4 bg-gradient-to-r from-purple-500/10 via-indigo-500/10 to-purple-500/10 dark:from-purple-500/20 dark:via-indigo-500/20 dark:to-purple-500/20 border-b border-gray-200/50 dark:border-gray-700/50">
          <div class="flex items-center justify-between">
            <div class="flex-1">
              <div class="flex items-center gap-3 mb-2">
                <div class="p-2 rounded-lg bg-gradient-to-br from-purple-500 to-indigo-500 shadow-lg">
                  <CpuChipIcon class="w-5 h-5 text-white" />
                </div>
                <div>
                  <h3 class="text-lg font-bold text-gray-900 dark:text-white">Aktuelles Modell</h3>
                  <p class="text-sm text-gray-600 dark:text-gray-400">{{ chatStore.selectedModel || 'Kein Modell ausgewählt' }}</p>
                </div>
              </div>
            </div>
            <div class="flex items-center gap-4">
              <div class="text-right">
                <div class="text-sm font-medium text-gray-700 dark:text-gray-300">
                  <span v-if="getModelCategory(chatStore.selectedModel) === 'custom'" class="inline-flex items-center gap-1.5 px-3 py-1.5 bg-gradient-to-r from-indigo-100 to-purple-100 dark:from-indigo-900/40 dark:to-purple-900/40 text-indigo-700 dark:text-indigo-300 rounded-lg">
                    <SparklesIcon class="w-4 h-4" />
                    Eigenes Modell
                  </span>
                  <span v-else-if="getModelCategory(chatStore.selectedModel) === 'coder'" class="inline-flex items-center gap-1.5 px-3 py-1.5 bg-gradient-to-r from-blue-100 to-blue-100 dark:from-blue-900/40 dark:to-blue-900/40 text-blue-700 dark:text-blue-300 rounded-lg">
                    <CpuChipIcon class="w-4 h-4" />
                    Coder Modell
                  </span>
                  <span v-else-if="getModelCategory(chatStore.selectedModel) === 'vision'" class="inline-flex items-center gap-1.5 px-3 py-1.5 bg-gradient-to-r from-green-100 to-green-100 dark:from-green-900/40 dark:to-green-900/40 text-green-700 dark:text-green-300 rounded-lg">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                    Vision Modell
                  </span>
                  <span v-else-if="getModelCategory(chatStore.selectedModel) === 'general'" class="inline-flex items-center gap-1.5 px-3 py-1.5 bg-gradient-to-r from-orange-100 to-orange-100 dark:from-orange-900/40 dark:to-orange-900/40 text-orange-700 dark:text-orange-300 rounded-lg">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                    </svg>
                    Allgemeines Modell
                  </span>
                </div>
                <div v-if="models.find(m => m.name === chatStore.selectedModel && m.isDefault)" class="text-xs text-yellow-600 dark:text-yellow-400 font-medium mt-1 flex items-center gap-1 justify-end">
                  <StarIcon class="w-3.5 h-3.5" />
                  Standard-Modell
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Sub-Tabs (only for Installierte Modelle) -->
        <div v-if="activeTab === 'installed'" class="border-b border-gray-200/50 dark:border-gray-700/50 bg-gray-50/30 dark:bg-gray-900/30">
          <div class="flex px-4">
            <!-- 1. Experten (zuerst) -->
            <button
              @click="installedSubTab = 'experts'"
              class="
                px-4 py-2 font-medium text-sm transition-all duration-200
                flex items-center gap-2
                relative
              "
              :class="installedSubTab === 'experts'
                ? 'text-indigo-600 dark:text-indigo-400'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            >
              <UserGroupIcon class="w-4 h-4" />
              <span>Experten</span>
              <span v-if="chatStore.experts.length" class="ml-1 px-1.5 py-0.5 text-xs rounded-full bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400">
                {{ chatStore.experts.length }}
              </span>
              <div v-if="installedSubTab === 'experts'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-500"></div>
            </button>
            <!-- 2. Heruntergeladene Modelle -->
            <button
              @click="installedSubTab = 'downloaded'"
              class="
                px-4 py-2 font-medium text-sm transition-all duration-200
                flex items-center gap-2
                relative
              "
              :class="installedSubTab === 'downloaded'
                ? 'text-blue-600 dark:text-blue-400'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            >
              <ServerIcon class="w-4 h-4" />
              <span>Heruntergeladene Modelle</span>
              <div v-if="installedSubTab === 'downloaded'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-500"></div>
            </button>
            <!-- 3. Eigene Modelle (zuletzt) -->
            <button
              @click="installedSubTab = 'custom'"
              class="
                px-4 py-2 font-medium text-sm transition-all duration-200
                flex items-center gap-2
                relative
              "
              :class="installedSubTab === 'custom'
                ? 'text-purple-600 dark:text-purple-400'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            >
              <SparklesIcon class="w-4 h-4" />
              <span>Eigene Modelle</span>
              <div v-if="installedSubTab === 'custom'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-purple-500"></div>
            </button>
          </div>
        </div>

        <!-- Model Type Filter (only for Downloaded Models Sub-Tab) -->
        <div v-if="activeTab === 'installed' && installedSubTab === 'downloaded'" class="border-b border-gray-200/50 dark:border-gray-700/50 bg-white/50 dark:bg-gray-800/50 px-4 py-3">
          <div class="flex items-center gap-2">
            <span class="text-sm font-medium text-gray-600 dark:text-gray-400">🏷️ Filter:</span>
            <div class="flex gap-2">
              <button
                @click="downloadedFilter = 'all'"
                :class="[
                  'px-3 py-1 rounded-lg text-sm font-medium transition-colors',
                  downloadedFilter === 'all'
                    ? 'bg-gray-600 text-white'
                    : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                ]"
              >
                Alle
              </button>
              <button
                @click="downloadedFilter = 'coder'"
                :class="[
                  'px-3 py-1 rounded-lg text-sm font-medium transition-colors',
                  downloadedFilter === 'coder'
                    ? 'bg-blue-600 text-white'
                    : 'bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 hover:bg-blue-200 dark:hover:bg-blue-900/50'
                ]"
              >
                <CpuChipIcon class="w-4 h-4 inline mr-1" />
                Coder
              </button>
              <button
                @click="downloadedFilter = 'vision'"
                :class="[
                  'px-3 py-1 rounded-lg text-sm font-medium transition-colors',
                  downloadedFilter === 'vision'
                    ? 'bg-green-600 text-white'
                    : 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 hover:bg-green-200 dark:hover:bg-green-900/50'
                ]"
              >
                <svg class="w-4 h-4 inline mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                Vision
              </button>
              <button
                @click="downloadedFilter = 'general'"
                :class="[
                  'px-3 py-1 rounded-lg text-sm font-medium transition-colors',
                  downloadedFilter === 'general'
                    ? 'bg-orange-600 text-white'
                    : 'bg-orange-100 dark:bg-orange-900/30 text-orange-700 dark:text-orange-300 hover:bg-orange-200 dark:hover:bg-orange-900/50'
                ]"
              >
                <svg class="w-4 h-4 inline mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01" />
                </svg>
                Allgemein
              </button>
            </div>
          </div>
        </div>

        <!-- Action Buttons and Search -->
        <Transition name="fade">
          <!-- Installierte Modelle: Search + Actions (based on sub-tab) -->
          <div v-if="activeTab === 'installed'" class="p-4 border-b border-gray-200/50 dark:border-gray-700/50">
            <!-- Custom Models Sub-Tab -->
            <div v-if="installedSubTab === 'custom'" class="flex gap-3">
              <div class="flex-1 relative">
                <MagnifyingGlassIcon class="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  v-model="customSearchQuery"
                  type="text"
                  placeholder="Eigene Modelle durchsuchen..."
                  class="
                    w-full pl-10 pr-4 py-2.5 rounded-xl
                    border border-gray-300 dark:border-gray-600
                    bg-white dark:bg-gray-700
                    text-gray-900 dark:text-white
                    placeholder-gray-400 dark:placeholder-gray-500
                    focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent
                    transition-all duration-200
                  "
                />
              </div>
              <button
                @click="showCreateCustomModel = true"
                :disabled="isLoading"
                class="
                  px-4 py-2 rounded-xl
                  bg-gradient-to-r from-indigo-500 to-purple-500
                  hover:from-indigo-600 hover:to-purple-600
                  text-white
                  font-medium
                  shadow-sm hover:shadow-md
                  disabled:opacity-50 disabled:cursor-not-allowed
                  transition-all duration-200
                  transform hover:scale-105 active:scale-95
                  flex items-center gap-2
                "
              >
                <SparklesIcon class="w-5 h-5" />
                <span>Neues erstellen</span>
              </button>
              <button
                @click="loadCustomModels"
                :disabled="isLoadingCustom"
                class="
                  px-4 py-2 rounded-xl
                  bg-gradient-to-r from-gray-200 to-gray-300
                  dark:from-gray-700 dark:to-gray-600
                  hover:from-gray-300 hover:to-gray-400
                  dark:hover:from-gray-600 dark:hover:to-gray-500
                  text-gray-800 dark:text-white
                  font-medium
                  shadow-sm hover:shadow-md
                  disabled:opacity-50 disabled:cursor-not-allowed
                  transition-all duration-200
                  transform hover:scale-105 active:scale-95
                  flex items-center gap-2
                "
              >
                <ArrowPathIcon class="w-5 h-5" :class="{ 'animate-spin': isLoadingCustom }" />
                <span>Aktualisieren</span>
              </button>
            </div>

            <!-- Downloaded Models Sub-Tab -->
            <div v-else-if="installedSubTab === 'downloaded'" class="flex gap-3">
              <div class="flex-1 relative">
                <MagnifyingGlassIcon class="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  v-model="downloadedSearchQuery"
                  type="text"
                  placeholder="Heruntergeladene Modelle durchsuchen..."
                  class="
                    w-full pl-10 pr-4 py-2.5 rounded-xl
                    border border-gray-300 dark:border-gray-600
                    bg-white dark:bg-gray-700
                    text-gray-900 dark:text-white
                    placeholder-gray-400 dark:placeholder-gray-500
                    focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent
                    transition-all duration-200
                  "
                />
              </div>
              <button
                @click="refreshModels"
                :disabled="isLoading"
                class="
                  px-4 py-2 rounded-xl
                  bg-gradient-to-r from-gray-200 to-gray-300
                  dark:from-gray-700 dark:to-gray-600
                  hover:from-gray-300 hover:to-gray-400
                  dark:hover:from-gray-600 dark:hover:to-gray-500
                  text-gray-800 dark:text-white
                  font-medium
                  shadow-sm hover:shadow-md
                  disabled:opacity-50 disabled:cursor-not-allowed
                  transition-all duration-200
                  transform hover:scale-105 active:scale-95
                  flex items-center gap-2
                "
              >
                <ArrowPathIcon class="w-5 h-5" :class="{ 'animate-spin': isLoading }" />
                <span>Aktualisieren</span>
              </button>
            </div>
          </div>

        </Transition>

      <!-- Installed Models List - Shown based on sub-tab - MUSS SCROLLEN! -->
      <div v-if="activeTab === 'installed'" class="flex-1 overflow-y-auto p-6 min-h-0">
        <!-- Loading state -->
        <div v-if="isLoading && installedSubTab !== 'custom'" class="flex justify-center items-center py-8">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-fleet-orange-500"></div>
        </div>
        <div v-else-if="isLoadingCustom && installedSubTab === 'custom'" class="flex justify-center items-center py-8">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-500"></div>
        </div>

        <!-- Custom Models Sub-Tab -->
        <div v-else-if="installedSubTab === 'custom'">
          <div v-if="filteredCustomModels.length === 0" class="text-center py-12">
            <SparklesIcon class="w-16 h-16 mx-auto text-purple-400 dark:text-purple-600 mb-4" />
            <p class="text-lg text-gray-600 dark:text-gray-400 mb-2" v-if="customSearchQuery">
              Keine eigenen Modelle gefunden für: "{{ customSearchQuery }}"
            </p>
            <p class="text-lg text-gray-600 dark:text-gray-400 mb-2" v-else>
              Noch keine eigenen Modelle erstellt
            </p>
            <p class="text-sm text-gray-500 dark:text-gray-500 mb-6">
              Erstelle dein erstes Custom Model mit eigenem System Prompt
            </p>
            <button
              @click="showCreateCustomModel = true"
              class="
                px-6 py-3 rounded-xl
                bg-gradient-to-r from-indigo-500 to-purple-500
                hover:from-indigo-600 hover:to-purple-600
                text-white font-medium
                shadow-sm hover:shadow-md
                transition-all duration-200
                transform hover:scale-105 active:scale-95
                inline-flex items-center gap-2
              "
            >
              <SparklesIcon class="w-5 h-5" />
              <span>Jetzt erstellen</span>
            </button>
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="model in filteredCustomModels"
              :key="model.id || model.name"
              class="bg-gradient-to-br from-indigo-50 to-purple-50 dark:from-indigo-900/20 dark:to-purple-900/20 rounded-lg p-4 border border-indigo-200/50 dark:border-indigo-700/50"
            >
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <div class="flex items-center gap-3 mb-2">
                    <SparklesIcon class="w-5 h-5 text-indigo-500" />
                    <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
                      {{ model.name }}
                    </h3>
                    <!-- Provider Type Badge -->
                    <span
                      class="px-2 py-0.5 text-xs font-medium rounded-full"
                      :class="model.isGgufConfig
                        ? 'bg-blue-100 dark:bg-blue-900/40 text-blue-600 dark:text-blue-400'
                        : 'bg-orange-100 dark:bg-orange-900/40 text-orange-600 dark:text-orange-400'"
                    >
                      {{ model.isGgufConfig ? '⚡ GGUF' : '🦙 Ollama' }}
                    </span>
                  </div>

                  <div class="space-y-1 text-sm text-gray-600 dark:text-gray-400">
                    <p v-if="model.description">{{ model.description }}</p>
                    <p><span class="font-medium">Basis-Modell:</span> {{ model.baseModel }}</p>
                    <p v-if="model.systemPrompt" class="text-xs">
                      <span class="font-medium">System Prompt:</span>
                      <span class="line-clamp-2">{{ model.systemPrompt }}</span>
                    </p>
                    <p class="text-xs text-gray-500 dark:text-gray-500">
                      Erstellt: {{ formatDateLong(model.createdAt) }}
                    </p>
                  </div>
                </div>

                <div class="flex flex-col gap-2 ml-4 min-w-[200px]">
                  <!-- Konfigurieren Button for GGUF models -->
                  <button
                    v-if="model.baseModel?.endsWith('.gguf')"
                    @click="openGgufConfigModal(model.name)"
                    class="
                      w-full px-4 py-2 rounded-lg
                      bg-gradient-to-r from-indigo-500 to-purple-500
                      hover:from-indigo-600 hover:to-purple-600
                      text-white text-sm font-medium
                      shadow-sm hover:shadow-md
                      transition-all duration-200
                      transform hover:scale-105 active:scale-95
                      flex items-center justify-center gap-2
                    "
                    title="GGUF Model konfigurieren"
                  >
                    <Cog6ToothIcon class="w-4 h-4" />
                    <span>Konfigurieren</span>
                  </button>

                  <button
                    @click="selectAndSetDefault(model.name)"
                    class="
                      w-full px-4 py-2 rounded-lg
                      bg-gradient-to-r from-purple-500 to-indigo-500
                      hover:from-purple-600 hover:to-indigo-600
                      text-white text-sm font-medium
                      shadow-sm hover:shadow-md
                      transition-all duration-200
                      transform hover:scale-105 active:scale-95
                      flex items-center justify-center gap-2
                    "
                    title="Model auswählen"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                    </svg>
                    <span>Auswählen</span>
                  </button>
                  <button
                    @click="openEditCustomModelModal(model)"
                    class="
                      w-full px-4 py-2 rounded-lg
                      bg-gradient-to-r from-amber-500 to-orange-500
                      hover:from-amber-600 hover:to-orange-600
                      text-white text-sm font-medium
                      shadow-sm hover:shadow-md
                      transition-all duration-200
                      transform hover:scale-105 active:scale-95
                      flex items-center justify-center gap-2
                    "
                    title="Bearbeiten"
                  >
                    <PencilIcon class="w-4 h-4" />
                    <span>Bearbeiten</span>
                  </button>
                  <button
                    @click="deleteCustomModel(model.id, model.name, model.isGgufConfig)"
                    class="
                      w-full px-4 py-2 rounded-lg
                      bg-red-500 hover:bg-red-600
                      text-white text-sm font-medium
                      shadow-sm hover:shadow-md
                      transition-all duration-200
                      transform hover:scale-105 active:scale-95
                      flex items-center justify-center gap-2
                    "
                    title="Löschen"
                  >
                    <TrashIcon class="w-4 h-4" />
                    <span>Löschen</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Downloaded Models Sub-Tab -->
        <div v-else-if="installedSubTab === 'downloaded'">
          <div v-if="downloadedModels.length === 0" class="text-center py-12 text-gray-500 dark:text-gray-400">
            <svg class="w-16 h-16 mx-auto text-purple-400 dark:text-purple-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10" />
            </svg>
            <p v-if="downloadedFilter">Keine heruntergeladenen Modelle gefunden für: "{{ downloadedFilter }}"</p>
            <p v-else>Keine heruntergeladenen Modelle installiert</p>
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="model in downloadedModels"
              :key="model.name"
              class="bg-purple-50 dark:bg-purple-900/20 rounded-lg p-4 border border-purple-200/50 dark:border-purple-700/50 transition-colors hover:bg-purple-100 dark:hover:bg-purple-900/30"
            >
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <div class="flex items-center gap-3 mb-2">
                    <h3 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                      {{ model.name }}
                      <span v-if="!canUseModel(model.name)" class="text-xs font-normal px-2 py-1 bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-300 rounded" :title="getIncompatibilityMessage(model.name)">
                        ⚠️ Context zu groß
                      </span>
                    </h3>
                    <span v-if="model.isDefault" class="px-2 py-1 bg-fleet-orange-500 text-white text-xs rounded-full">
                      ⭐ Standard
                    </span>
                    <span v-if="hasUpdate(model.name)" class="px-2 py-1 bg-green-500 text-white text-xs rounded-full animate-pulse">
                      🔄 Update verfügbar
                    </span>
                  </div>

                  <div class="text-sm text-gray-600 dark:text-gray-400 space-y-1">
                    <div><strong>Größe:</strong> {{ formatSize(model.size) }}</div>
                    <div v-if="model.description"><strong>Beschreibung:</strong> {{ model.description }}</div>
                    <div v-if="model.specialties"><strong>Spezialitäten:</strong> {{ model.specialties }}</div>
                    <div v-if="model.publisher"><strong>Herausgeber:</strong> {{ model.publisher }}</div>
                    <div v-if="model.releaseDate">
                      <strong>Veröffentlicht:</strong> {{ formatDateLong(model.releaseDate) }}
                    </div>
                    <div v-if="model.trainedUntil"><strong>Trainiert bis:</strong> {{ model.trainedUntil }}</div>
                    <div v-if="model.license"><strong>Lizenz:</strong> {{ model.license }}</div>
                  </div>
                </div>

                <div class="flex flex-col gap-2 ml-4 min-w-[200px]">
                  <!-- Konfigurieren Button for GGUF models -->
                  <button
                    v-if="model.name.endsWith('.gguf')"
                    @click="openGgufConfigModal(model.name)"
                    class="
                      w-full px-4 py-2 rounded-lg
                      bg-gradient-to-r from-indigo-500 to-purple-500
                      hover:from-indigo-600 hover:to-purple-600
                      text-white text-sm font-medium
                      shadow-sm hover:shadow-md
                      transition-all duration-200
                      transform hover:scale-105 active:scale-95
                      flex items-center justify-center gap-2
                    "
                    title="GGUF Model konfigurieren"
                  >
                    <Cog6ToothIcon class="w-4 h-4" />
                    <span>Konfigurieren</span>
                  </button>

                  <button
                    @click="selectAndSetDefault(model.name)"
                    :disabled="!canUseModel(model.name)"
                    class="
                      w-full px-4 py-2 rounded-lg
                      bg-gradient-to-r from-purple-500 to-indigo-500
                      hover:from-purple-600 hover:to-indigo-600
                      disabled:from-gray-400 disabled:to-gray-500
                      text-white text-sm font-medium
                      shadow-sm hover:shadow-md
                      transition-all duration-200
                      transform hover:scale-105 active:scale-95
                      disabled:cursor-not-allowed disabled:transform-none
                      flex items-center justify-center gap-2
                    "
                    title="Model auswählen"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                    </svg>
                    <span>Auswählen</span>
                  </button>
                  <button
                    @click="confirmDelete(model.name)"
                    class="
                      w-full px-4 py-2 rounded-lg
                      bg-red-500 hover:bg-red-600
                      text-white text-sm font-medium
                      shadow-sm hover:shadow-md
                      transition-all duration-200
                      transform hover:scale-105 active:scale-95
                      flex items-center justify-center gap-2
                    "
                    title="Löschen"
                  >
                    <TrashIcon class="w-4 h-4" />
                    <span>Löschen</span>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Experts Sub-Tab -->
        <div v-else-if="installedSubTab === 'experts'">
          <!-- Header with create button -->
          <div class="flex items-center justify-between mb-4 pb-4 border-b border-gray-200 dark:border-gray-700">
            <div class="flex items-center gap-2">
              <UserGroupIcon class="w-5 h-5 text-indigo-500" />
              <span class="font-medium text-gray-700 dark:text-gray-300">Meine Experten</span>
              <span class="px-2 py-0.5 text-xs rounded-full bg-indigo-100 dark:bg-indigo-900/30 text-indigo-600 dark:text-indigo-400">
                {{ chatStore.experts.length }}
              </span>
            </div>
            <button
              @click="openCreateExpertModal"
              class="
                px-3 py-1.5 rounded-lg
                bg-gradient-to-r from-indigo-500 to-purple-500
                hover:from-indigo-600 hover:to-purple-600
                text-white text-sm font-medium
                transition-all duration-200
                flex items-center gap-1.5
                shadow-sm hover:shadow-md
              "
            >
              <PlusIcon class="w-4 h-4" />
              Neuer Experte
            </button>
          </div>

          <div v-if="chatStore.experts.length === 0" class="text-center py-12 text-gray-500 dark:text-gray-400">
            <UserGroupIcon class="w-16 h-16 mx-auto text-indigo-400 dark:text-indigo-600 mb-4" />
            <p class="mb-4">Noch keine Experten erstellt</p>
            <p class="text-sm text-gray-400 dark:text-gray-500">
              Klicke auf "Neuer Experte" um deinen ersten AI-Experten zu erstellen.
            </p>
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="expert in chatStore.experts"
              :key="expert.id"
              class="bg-indigo-50 dark:bg-indigo-900/20 rounded-lg p-4 border border-indigo-200/50 dark:border-indigo-700/50 transition-colors hover:bg-indigo-100 dark:hover:bg-indigo-900/30"
            >
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <div class="flex items-center gap-3 mb-2">
                    <!-- Avatar oder Fallback-Icon -->
                    <div
                      v-if="expert.avatarUrl"
                      class="w-10 h-10 rounded-lg overflow-hidden border-2 border-indigo-400/50 shadow-sm flex-shrink-0"
                    >
                      <img
                        :src="expert.avatarUrl"
                        :alt="expert.name"
                        class="w-full h-full object-cover"
                        @error="(e) => e.target.style.display = 'none'"
                      />
                    </div>
                    <div
                      v-else
                      class="p-2 rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600 flex-shrink-0"
                    >
                      <UserGroupIcon class="w-5 h-5 text-white" />
                    </div>
                    <div>
                      <h3 class="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                        {{ expert.name }}
                        <span v-if="chatStore.selectedExpertId === expert.id" class="px-2 py-1 bg-indigo-500 text-white text-xs rounded-full">
                          ✓ Aktiv
                        </span>
                      </h3>
                      <p class="text-sm text-indigo-600 dark:text-indigo-400 font-medium">{{ expert.role }}</p>
                    </div>
                  </div>

                  <div class="text-sm text-gray-600 dark:text-gray-400 space-y-1">
                    <div v-if="expert.description">{{ expert.description }}</div>
                    <div class="flex items-center gap-2 text-xs text-gray-500">
                      <CpuChipIcon class="w-4 h-4" />
                      <span>{{ expert.baseModel }}</span>
                    </div>
                    <div v-if="expert.modes && expert.modes.length > 0" class="flex flex-wrap gap-1 mt-2">
                      <span
                        v-for="mode in expert.modes"
                        :key="mode.id"
                        class="px-2 py-0.5 text-xs rounded-full bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300"
                      >
                        {{ mode.name }}
                      </span>
                    </div>
                  </div>
                </div>

                <div class="flex flex-col gap-2 ml-4 min-w-[160px]">
                  <button
                    @click="selectExpert(expert)"
                    class="
                      w-full px-4 py-2 rounded-lg
                      bg-gradient-to-r from-indigo-500 to-purple-500
                      hover:from-indigo-600 hover:to-purple-600
                      text-white text-sm font-medium
                      shadow-sm hover:shadow-md
                      transition-all duration-200
                      transform hover:scale-105 active:scale-95
                      flex items-center justify-center gap-2
                    "
                  >
                    <ChatBubbleLeftRightIcon class="w-4 h-4" />
                    <span>Auswählen</span>
                  </button>
                  <div class="flex gap-2">
                    <button
                      @click="editExpertInModal(expert)"
                      class="
                        flex-1 px-3 py-1.5 rounded-lg
                        bg-amber-100 dark:bg-amber-900/30
                        hover:bg-amber-200 dark:hover:bg-amber-900/50
                        text-amber-700 dark:text-amber-300 text-sm
                        transition-colors
                        flex items-center justify-center gap-1
                      "
                      title="Bearbeiten"
                    >
                      <PencilIcon class="w-3.5 h-3.5" />
                      <span>Bearbeiten</span>
                    </button>
                    <button
                      @click="deleteExpertFromModal(expert)"
                      class="
                        px-3 py-1.5 rounded-lg
                        bg-red-100 dark:bg-red-900/30
                        hover:bg-red-200 dark:hover:bg-red-900/50
                        text-red-700 dark:text-red-300 text-sm
                        transition-colors
                        flex items-center justify-center
                      "
                      title="Löschen"
                    >
                      <TrashIcon class="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Available Models List -->
      <div v-else-if="activeTab === 'available'" class="flex-1 overflow-y-auto p-6 min-h-0">
        <div v-if="isLoadingLibrary" class="flex justify-center items-center py-8">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-fleet-orange-500"></div>
          <span class="ml-3 text-gray-600 dark:text-gray-400">
            Lade Model Store...
          </span>
        </div>

        <div v-else class="space-y-4">
          <!-- llama.cpp Provider: GGUF Model Store -->
          <div v-if="activeProvider !== 'ollama'">

          <!-- HuggingFace Search -->
          <div class="bg-gradient-to-r from-yellow-50 to-orange-50 dark:from-yellow-900/20 dark:to-orange-900/20 border border-yellow-200 dark:border-yellow-700 rounded-lg p-4 mb-4">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-3 flex items-center gap-2">
              🔍 HuggingFace Modell-Suche
            </h3>

            <div class="flex gap-3 mb-3">
              <input
                v-model="hfSearchQuery"
                @keyup.enter="searchHuggingFace"
                type="text"
                placeholder="Suche nach Modellen (z.B. 'qwen', 'llama', 'german')..."
                class="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-fleet-orange-500"
              />
              <button
                @click="searchHuggingFace"
                :disabled="isSearchingHF || !hfSearchQuery.trim()"
                class="px-6 py-2 bg-fleet-orange-500 hover:bg-fleet-orange-600 disabled:bg-gray-400 text-white rounded-lg transition-colors font-medium"
              >
                {{ isSearchingHF ? '🔄 Suche...' : '🔍 Suchen' }}
              </button>
            </div>

            <div class="space-y-2">
              <!-- Row 1: Sprache & Beliebtheit -->
              <div class="flex gap-2 flex-wrap">
                <button
                  @click="loadPopularHF"
                  :disabled="isSearchingHF"
                  class="px-3 py-1 bg-blue-100 hover:bg-blue-200 dark:bg-blue-900 dark:hover:bg-blue-800 text-blue-800 dark:text-blue-200 text-sm rounded transition-colors"
                >
                  ⭐ Beliebte Modelle
                </button>
                <button
                  @click="loadGermanHF"
                  :disabled="isSearchingHF"
                  class="px-3 py-1 bg-green-100 hover:bg-green-200 dark:bg-green-900 dark:hover:bg-green-800 text-green-800 dark:text-green-200 text-sm rounded transition-colors"
                >
                  🇩🇪 Deutsche Modelle
                </button>
              </div>

              <!-- Row 2: Kategorien -->
              <div class="flex gap-2 flex-wrap">
                <button
                  @click="loadInstructHF"
                  :disabled="isSearchingHF"
                  class="px-3 py-1 bg-purple-100 hover:bg-purple-200 dark:bg-purple-900 dark:hover:bg-purple-800 text-purple-800 dark:text-purple-200 text-sm rounded transition-colors"
                >
                  💬 Instruct/Chat
                </button>
                <button
                  @click="loadCodeHF"
                  :disabled="isSearchingHF"
                  class="px-3 py-1 bg-teal-100 hover:bg-teal-200 dark:bg-teal-900 dark:hover:bg-teal-800 text-teal-800 dark:text-teal-200 text-sm rounded transition-colors"
                >
                  💻 Code
                </button>
                <button
                  @click="loadVisionHF"
                  :disabled="isSearchingHF"
                  class="px-3 py-1 bg-orange-100 hover:bg-orange-200 dark:bg-orange-900 dark:hover:bg-orange-800 text-orange-800 dark:text-orange-200 text-sm rounded transition-colors"
                >
                  👁️ Vision
                </button>
                <button
                  @click="clearHFSearch"
                  v-if="hfSearchResults.length > 0"
                  class="px-3 py-1 bg-gray-100 hover:bg-gray-200 dark:bg-gray-700 dark:hover:bg-gray-600 text-gray-800 dark:text-gray-200 text-sm rounded transition-colors"
                >
                  ✕ Zurücksetzen
                </button>
              </div>
            </div>

            <!-- HF Search Results -->
            <div v-if="hfSearchResults.length > 0" class="mt-4 border-t border-yellow-200 dark:border-yellow-700 pt-4">
              <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
                Gefunden: {{ hfSearchResults.length }} Modelle von HuggingFace
              </h4>
              <div class="max-h-96 overflow-y-auto space-y-3">
                <div
                  v-for="model in hfSearchResults"
                  :key="model.id"
                  class="bg-gray-50 dark:bg-gray-700 rounded-lg p-4 hover:bg-gray-100 dark:hover:bg-gray-650 transition-colors"
                >
                  <div class="flex items-start justify-between mb-3">
                    <div class="flex-1">
                      <h5 class="text-lg font-semibold text-gray-900 dark:text-white mb-1">
                        {{ model.displayName || model.name }}
                      </h5>
                      <div class="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400 mb-2">
                        <span>👤 {{ model.author }}</span>
                        <span>•</span>
                        <span>⬇️ {{ formatDownloads(model.downloads) }} Downloads</span>
                        <span v-if="model.likes">•</span>
                        <span v-if="model.likes">❤️ {{ model.likes }} Likes</span>
                      </div>
                      <!-- Modellgröße prominent anzeigen -->
                      <div v-if="model.modelSize" class="mt-2">
                        <span class="px-3 py-1.5 bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300 rounded-lg border border-blue-300 dark:border-blue-700 font-medium text-sm">
                          📦 {{ formatSize(model.modelSize) }}
                        </span>
                      </div>
                    </div>
                    <div class="flex flex-col gap-2 ml-3">
                      <button
                        class="px-4 py-2 bg-fleet-orange-500 hover:bg-fleet-orange-600 text-white rounded-lg transition-colors whitespace-nowrap font-medium"
                        @click.stop="downloadHFModel(model)"
                      >
                        ⬇ Download
                      </button>
                      <a
                        :href="`https://huggingface.co/${model.modelId || model.id}`"
                        target="_blank"
                        rel="noopener noreferrer"
                        class="px-4 py-2 bg-gray-100 hover:bg-gray-200 dark:bg-gray-600 dark:hover:bg-gray-500 text-gray-700 dark:text-gray-200 rounded-lg transition-colors whitespace-nowrap font-medium text-center text-sm"
                        @click.stop
                      >
                        ℹ️ Info
                      </a>
                    </div>
                  </div>

                  <!-- Description -->
                  <p v-if="model.shortDescription || model.description" class="text-sm text-gray-700 dark:text-gray-300 mb-3 leading-relaxed">
                    {{ model.shortDescription || model.description || 'Keine Beschreibung verfügbar' }}
                  </p>

                  <!-- Metadata Grid -->
                  <div class="grid grid-cols-2 gap-3 mb-3 text-sm">
                    <!-- Release Date -->
                    <div v-if="model.createdAt">
                      <span class="text-gray-600 dark:text-gray-400">📅 Veröffentlicht:</span>
                      <span class="ml-2 text-gray-900 dark:text-white">{{ formatDateLong(model.createdAt) }}</span>
                    </div>

                    <!-- Last Modified -->
                    <div v-if="model.lastModified">
                      <span class="text-gray-600 dark:text-gray-400">🔄 Aktualisiert:</span>
                      <span class="ml-2 text-gray-900 dark:text-white">{{ formatDateLong(model.lastModified) }}</span>
                    </div>

                    <!-- License -->
                    <div v-if="model.license">
                      <span class="text-gray-600 dark:text-gray-400">📜 Lizenz:</span>
                      <span class="ml-2 text-gray-900 dark:text-white">{{ model.license }}</span>
                    </div>

                    <!-- Pipeline Tag -->
                    <div v-if="model.pipeline_tag">
                      <span class="text-gray-600 dark:text-gray-400">🏷️ Typ:</span>
                      <span class="ml-2 text-gray-900 dark:text-white">{{ model.pipeline_tag }}</span>
                    </div>
                  </div>

                  <!-- Tags -->
                  <div v-if="model.tags && model.tags.length > 0" class="flex flex-wrap gap-1 mb-2">
                    <span
                      v-for="tag in model.tags.slice(0, 8)"
                      :key="tag"
                      class="px-2 py-0.5 bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 text-xs rounded"
                    >
                      {{ tag }}
                    </span>
                    <span v-if="model.tags.length > 8" class="px-2 py-0.5 text-gray-600 dark:text-gray-400 text-xs">
                      +{{ model.tags.length - 8 }} mehr
                    </span>
                  </div>

                  <!-- Languages -->
                  <div v-if="model.languages && model.languages.length > 0" class="text-sm">
                    <span class="text-gray-600 dark:text-gray-400">🌐 Sprachen:</span>
                    <span class="ml-2 text-gray-900 dark:text-white">{{ model.languages.join(', ') }}</span>
                  </div>

                  <!-- Privacy indicators -->
                  <div v-if="model.gated || model.private_model" class="mt-2 flex gap-2">
                    <span v-if="model.gated" class="px-2 py-1 bg-yellow-100 dark:bg-yellow-900 text-yellow-800 dark:text-yellow-200 text-xs rounded">
                      🔒 Gated Model (Zugriff beschränkt)
                    </span>
                    <span v-if="model.private_model" class="px-2 py-1 bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200 text-xs rounded">
                      🔐 Privates Model
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          </div> <!-- End llama.cpp Provider -->

          <!-- Ollama Provider: Ollama Library -->
          <div v-else>
            <!-- Info Banner -->
            <div class="bg-purple-50 dark:bg-purple-900/30 border border-purple-200 dark:border-purple-700 rounded-lg p-3 mb-4">
              <p class="text-sm text-purple-800 dark:text-purple-200">
                🔮 <strong>Ollama Library</strong> - Beliebte Modelle zum Download
              </p>
            </div>

            <!-- Category Filter Tabs -->
            <div class="flex gap-2 mb-4 overflow-x-auto pb-2">
              <button
                @click="ollamaCategoryFilter = 'alle'"
                :class="[
                  'px-4 py-2 rounded-lg font-medium transition-colors whitespace-nowrap',
                  ollamaCategoryFilter === 'alle'
                    ? 'bg-purple-500 text-white'
                    : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                ]"
              >
                🌐 Alle ({{ ollamaLibraryModels.length }})
              </button>
              <button
                @click="ollamaCategoryFilter = 'chat'"
                :class="[
                  'px-4 py-2 rounded-lg font-medium transition-colors whitespace-nowrap',
                  ollamaCategoryFilter === 'chat'
                    ? 'bg-purple-500 text-white'
                    : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                ]"
              >
                💬 Chat ({{ ollamaLibraryModels.filter(m => m.category === 'chat').length }})
              </button>
              <button
                @click="ollamaCategoryFilter = 'code'"
                :class="[
                  'px-4 py-2 rounded-lg font-medium transition-colors whitespace-nowrap',
                  ollamaCategoryFilter === 'code'
                    ? 'bg-purple-500 text-white'
                    : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                ]"
              >
                💻 Code ({{ ollamaLibraryModels.filter(m => m.category === 'code').length }})
              </button>
              <button
                @click="ollamaCategoryFilter = 'vision'"
                :class="[
                  'px-4 py-2 rounded-lg font-medium transition-colors whitespace-nowrap',
                  ollamaCategoryFilter === 'vision'
                    ? 'bg-purple-500 text-white'
                    : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                ]"
              >
                👁️ Vision ({{ ollamaLibraryModels.filter(m => m.category === 'vision').length }})
              </button>
              <button
                @click="ollamaCategoryFilter = 'spezialisiert'"
                :class="[
                  'px-4 py-2 rounded-lg font-medium transition-colors whitespace-nowrap',
                  ollamaCategoryFilter === 'spezialisiert'
                    ? 'bg-purple-500 text-white'
                    : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                ]"
              >
                🎯 Spezialisiert ({{ ollamaLibraryModels.filter(m => m.category === 'spezialisiert').length }})
              </button>
            </div>

            <!-- Loading State -->
            <div v-if="isLoadingOllamaLibrary" class="text-center py-8">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-purple-500 mx-auto mb-2"></div>
              <span class="text-sm text-gray-600 dark:text-gray-400">Lade Ollama Library...</span>
            </div>

            <!-- Ollama Library Models -->
            <div v-else class="space-y-3">
              <div
                v-for="model in filteredOllamaLibraryModels"
                :key="model.name"
                class="bg-white dark:bg-gray-800 border border-purple-200 dark:border-purple-700 rounded-lg p-4 hover:border-purple-400 dark:hover:border-purple-500 transition-colors"
              >
                <div class="flex items-start justify-between gap-4">
                  <div class="flex-1">
                    <!-- Model Header -->
                    <div class="flex items-center gap-3 mb-2">
                      <span class="text-2xl">{{ model.emoji || '🔮' }}</span>
                      <div class="flex-1">
                        <h4 class="font-semibold text-gray-900 dark:text-white">{{ model.name }}</h4>
                        <p class="text-sm text-gray-600 dark:text-gray-400">{{ model.description }}</p>
                      </div>
                    </div>

                    <!-- Metadata Tags -->
                    <div class="flex flex-wrap gap-2 mt-3">
                      <!-- Size & Parameters -->
                      <span v-if="model.size" class="text-xs px-2 py-1 bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300 rounded border border-blue-300 dark:border-blue-700">
                        📦 {{ model.size }}
                      </span>
                      <span v-if="model.parameters" class="text-xs px-2 py-1 bg-purple-100 dark:bg-purple-900/40 text-purple-700 dark:text-purple-300 rounded border border-purple-300 dark:border-purple-700">
                        🧠 {{ model.parameters }}
                      </span>

                      <!-- Publisher -->
                      <span v-if="model.publisher" class="text-xs px-2 py-1 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded border border-gray-300 dark:border-gray-600">
                        🏢 {{ model.publisher }}
                      </span>

                      <!-- RAM Requirement -->
                      <span v-if="model.recommendedRAM" class="text-xs px-2 py-1 bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-300 rounded border border-green-300 dark:border-green-700">
                        💾 {{ model.recommendedRAM }}
                      </span>

                      <!-- Release Date -->
                      <span v-if="model.releaseDate" class="text-xs px-2 py-1 bg-orange-100 dark:bg-orange-900/40 text-orange-700 dark:text-orange-300 rounded border border-orange-300 dark:border-orange-700">
                        📅 {{ model.releaseDate }}
                      </span>

                      <!-- Training Cutoff -->
                      <span v-if="model.trainedUntil" class="text-xs px-2 py-1 bg-yellow-100 dark:bg-yellow-900/40 text-yellow-700 dark:text-yellow-300 rounded border border-yellow-300 dark:border-yellow-700">
                        📚 Wissen bis {{ model.trainedUntil }}
                      </span>

                      <!-- Languages -->
                      <span v-if="model.languages" class="text-xs px-2 py-1 bg-indigo-100 dark:bg-indigo-900/40 text-indigo-700 dark:text-indigo-300 rounded border border-indigo-300 dark:border-indigo-700">
                        🌍 {{ model.languages }}
                      </span>

                      <!-- Perfect Match Badge (supports all 5 required languages: DE, TR, ES, EN, FR) -->
                      <span v-if="supportsAllRequiredLanguages(model)" class="text-xs px-2 py-1 bg-emerald-100 dark:bg-emerald-900/40 text-emerald-700 dark:text-emerald-300 rounded border border-emerald-300 dark:border-emerald-700 font-semibold">
                        ✓ DE + TR + ES + EN + FR
                      </span>
                    </div>

                    <!-- Specialties -->
                    <div v-if="model.specialties" class="mt-3">
                      <p class="text-xs text-gray-600 dark:text-gray-400">
                        <span class="font-semibold">🎯 Spezialitäten:</span> {{ model.specialties }}
                      </p>
                    </div>

                    <!-- License -->
                    <div v-if="model.license" class="mt-2">
                      <p class="text-xs text-gray-500 dark:text-gray-500">
                        <span class="font-semibold">⚖️ Lizenz:</span> {{ model.license }}
                      </p>
                    </div>
                  </div>

                  <!-- Action Buttons -->
                  <div class="flex-shrink-0 flex gap-2">
                    <!-- Info Link -->
                    <a
                      :href="getOllamaLibraryUrl(model.name)"
                      target="_blank"
                      rel="noopener noreferrer"
                      class="px-3 py-2 bg-gray-200 hover:bg-gray-300 dark:bg-gray-700 dark:hover:bg-gray-600 text-gray-700 dark:text-gray-300 rounded-lg transition-colors flex items-center gap-2"
                      title="Weitere Informationen auf Ollama.com"
                    >
                      ℹ️ Info
                    </a>

                    <!-- Download/Installed Button -->
                    <button
                      v-if="!isInstalled(model.name)"
                      @click="downloadOllamaModel(model.name)"
                      class="px-4 py-2 bg-purple-500 hover:bg-purple-600 text-white rounded-lg transition-colors flex items-center gap-2 whitespace-nowrap"
                    >
                      ⬇ Download
                    </button>
                    <span
                      v-else
                      class="px-4 py-2 bg-gray-300 dark:bg-gray-600 text-gray-600 dark:text-gray-400 rounded-lg flex items-center gap-2 whitespace-nowrap"
                    >
                      ✓ Installiert
                    </span>
                  </div>
                </div>
              </div>

              <!-- Empty State -->
              <div v-if="ollamaLibraryModels.length === 0" class="text-center py-8 text-gray-500 dark:text-gray-400">
                <p>Keine Modelle verfügbar</p>
                <p class="text-sm mt-2">Besuche <a href="https://ollama.com/library" target="_blank" class="text-purple-600 dark:text-purple-400 hover:underline">ollama.com/library</a></p>
              </div>
            </div>
          </div>
        </div>
      </div>
      </div>
    </div>

    <!-- Download Dialog -->
    <div
      v-if="showDownloadDialog"
      class="fixed inset-0 bg-black bg-opacity-70 flex items-center justify-center z-[60]"
    >
      <div class="bg-white dark:bg-gray-800 rounded-lg p-6 w-full max-w-lg shadow-2xl border-4 border-fleet-orange-500">
        <h3 class="text-2xl font-bold mb-4 text-gray-900 dark:text-white flex items-center gap-2">
          <span class="text-3xl">📥</span> Modell herunterladen
        </h3>

        <div v-if="!isDownloading">
          <input
            v-model="downloadModelName"
            type="text"
            placeholder="z.B. llama3.2:3b"
            class="w-full p-3 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white mb-4"
          />

          <!-- Warnung vor Download -->
          <div class="bg-yellow-50 dark:bg-yellow-900/30 border-2 border-yellow-400 dark:border-yellow-600 rounded-lg p-4 mb-4">
            <div class="flex items-start gap-3">
              <span class="text-2xl">⚠️</span>
              <div class="text-sm">
                <p class="font-bold text-yellow-800 dark:text-yellow-300 mb-2">WICHTIGE HINWEISE:</p>
                <ul class="list-disc list-inside space-y-1 text-yellow-700 dark:text-yellow-400">
                  <li><strong>Der Download kann 5-30 Minuten dauern</strong> (abhängig von Modellgröße)</li>
                  <li><strong>Internetverbindung erforderlich</strong></li>
                  <li><strong>Nicht abbrechen!</strong> Ein unvollständiger Download macht das Modell unbrauchbar</li>
                  <li>Modellgröße beachten: Große Modelle (> 10 GB) dauern länger</li>
                </ul>
              </div>
            </div>
          </div>

          <div class="text-xs text-gray-500 dark:text-gray-400 mb-4">
            <p><strong>Beliebte Modelle:</strong></p>
            <ul class="list-disc list-inside space-y-1 mt-2">
              <li>llama3.2:3b (2 GB) - ~3-5 Min</li>
              <li>qwen2.5-coder:7b (4.7 GB) - ~8-12 Min</li>
              <li>mistral:7b (4.1 GB) - ~7-10 Min</li>
              <li>codellama:7b (3.8 GB) - ~6-9 Min</li>
            </ul>
            <p class="mt-2">Weitere Modelle: <a href="https://ollama.com/library" target="_blank" class="text-fleet-orange-500 hover:underline">ollama.com/library</a></p>
          </div>

          <div class="flex gap-3">
            <button
              @click="startDownload"
              :disabled="!downloadModelName.trim()"
              class="flex-1 px-4 py-2 bg-fleet-orange-500 hover:bg-fleet-orange-600 disabled:bg-gray-400 text-white rounded-lg transition-colors font-bold"
            >
              ⬇ Jetzt herunterladen
            </button>
            <button
              @click="showDownloadDialog = false"
              class="px-4 py-2 bg-gray-500 hover:bg-gray-600 text-white rounded-lg transition-colors"
            >
              Abbrechen
            </button>
          </div>
        </div>

        <div v-else>
          <!-- Aktiver Download mit intensiver Anzeige -->
          <div class="text-center mb-6">
            <div class="text-6xl mb-4 animate-bounce">📥</div>
            <h4 class="text-xl font-bold text-gray-900 dark:text-white mb-2">Download läuft...</h4>
            <p class="text-lg text-gray-600 dark:text-gray-400">{{ downloadModelName }}</p>
          </div>

          <!-- Warnung während Download -->
          <div class="bg-red-50 dark:bg-red-900/30 border-2 border-red-500 rounded-lg p-4 mb-4 animate-pulse">
            <div class="flex items-center gap-3">
              <span class="text-3xl">🚫</span>
              <div class="text-sm">
                <p class="font-bold text-red-800 dark:text-red-300 mb-1">NICHT ABBRECHEN!</p>
                <p class="text-red-700 dark:text-red-400">
                  Der Download darf nicht unterbrochen werden. Wenn Sie abbrechen, wird das unvollständige Modell automatisch gelöscht.
                </p>
              </div>
            </div>
          </div>

          <!-- Fortschrittsbalken -->
          <div class="mb-4">
            <div class="flex justify-between items-center mb-2">
              <span class="text-sm font-medium text-gray-700 dark:text-gray-300">Fortschritt</span>
              <span class="text-sm font-bold text-fleet-orange-500">{{ downloadProgressPercent }}%</span>
            </div>
            <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-4 overflow-hidden">
              <div
                class="bg-gradient-to-r from-fleet-orange-500 to-fleet-orange-600 h-4 rounded-full transition-all duration-300 relative overflow-hidden"
                :style="{ width: downloadProgressPercent + '%' }"
              >
                <!-- Animierter Glanz-Effekt -->
                <div class="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent animate-shimmer"></div>
              </div>
            </div>
          </div>

          <!-- Status-Text -->
          <div class="bg-gray-50 dark:bg-gray-700 rounded-lg p-3 mb-4">
            <p class="text-sm text-gray-600 dark:text-gray-400 font-mono">{{ downloadProgress }}</p>
          </div>

          <!-- Zeitschätzung -->
          <div class="text-center mb-4">
            <p class="text-xs text-gray-500 dark:text-gray-400">
              ⏱ Geschätzte Dauer: 5-30 Minuten (je nach Modellgröße)
            </p>
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              Bitte Fenster nicht schließen!
            </p>
          </div>

          <!-- Notfall-Abbrechen Button -->
          <div class="border-t border-gray-200 dark:border-gray-700 pt-4">
            <button
              @click="cancelDownload"
              class="w-full px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors font-bold"
            >
              ⚠️ Trotzdem abbrechen (Modell wird gelöscht)
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Details Dialog -->
    <div
      v-if="showDetailsDialog"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60]"
    >
      <div class="bg-white dark:bg-gray-800 rounded-lg p-6 w-full max-w-2xl max-h-[80vh] overflow-y-auto">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-xl font-bold text-gray-900 dark:text-white">Modell-Details</h3>
          <button
            @click="showDetailsDialog = false"
            class="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"
          >
            ✕
          </button>
        </div>

        <div v-if="selectedModelDetails" class="space-y-3 text-sm">
          <div class="border-b border-gray-200 dark:border-gray-700 pb-2">
            <h4 class="font-semibold text-gray-900 dark:text-white">{{ selectedModelDetails.name }}</h4>
          </div>

          <div v-if="selectedModelDetails.description">
            <strong class="text-gray-700 dark:text-gray-300">Beschreibung:</strong>
            <p class="text-gray-600 dark:text-gray-400">{{ selectedModelDetails.description }}</p>
          </div>

          <div v-if="selectedModelDetails.publisher">
            <strong class="text-gray-700 dark:text-gray-300">Herausgeber:</strong>
            <p class="text-gray-600 dark:text-gray-400">{{ selectedModelDetails.publisher }}</p>
          </div>

          <div v-if="selectedModelDetails.releaseDate">
            <strong class="text-gray-700 dark:text-gray-300">Veröffentlicht:</strong>
            <p class="text-gray-600 dark:text-gray-400">{{ formatDateLong(selectedModelDetails.releaseDate) }}</p>
          </div>

          <div v-if="selectedModelDetails.trainedUntil">
            <strong class="text-gray-700 dark:text-gray-300">Trainiert bis:</strong>
            <p class="text-gray-600 dark:text-gray-400">{{ selectedModelDetails.trainedUntil }}</p>
          </div>

          <div v-if="selectedModelDetails.license">
            <strong class="text-gray-700 dark:text-gray-300">Lizenz:</strong>
            <p class="text-gray-600 dark:text-gray-400">{{ selectedModelDetails.license }}</p>
          </div>

          <div v-if="selectedModelDetails.specialties">
            <strong class="text-gray-700 dark:text-gray-300">Spezialitäten:</strong>
            <p class="text-gray-600 dark:text-gray-400">{{ selectedModelDetails.specialties }}</p>
          </div>

          <div v-if="selectedModelDetails.family">
            <strong class="text-gray-700 dark:text-gray-300">Familie:</strong>
            <p class="text-gray-600 dark:text-gray-400">{{ selectedModelDetails.family }}</p>
          </div>

          <div v-if="selectedModelDetails.parameter_size">
            <strong class="text-gray-700 dark:text-gray-300">Parameter:</strong>
            <p class="text-gray-600 dark:text-gray-400">{{ selectedModelDetails.parameter_size }}</p>
          </div>
        </div>

        <button
          @click="showDetailsDialog = false"
          class="mt-6 w-full px-4 py-2 bg-gray-500 hover:bg-gray-600 text-white rounded-lg transition-colors"
        >
          Schließen
        </button>
      </div>
    </div>

    <!-- Edit Metadata Dialog -->
    <div
      v-if="showEditDialog"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60]"
    >
      <div class="bg-white dark:bg-gray-800 rounded-lg p-6 w-full max-w-2xl max-h-[80vh] overflow-y-auto">
        <h3 class="text-xl font-bold mb-4 text-gray-900 dark:text-white">Metadaten bearbeiten</h3>

        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Beschreibung</label>
            <textarea
              v-model="editingModel.description"
              rows="2"
              class="w-full p-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            ></textarea>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Spezialitäten</label>
            <input
              v-model="editingModel.specialties"
              type="text"
              placeholder="z.B. Code-Generierung, Python, JavaScript"
              class="w-full p-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Herausgeber</label>
            <input
              v-model="editingModel.publisher"
              type="text"
              placeholder="z.B. Meta, Alibaba Cloud"
              class="w-full p-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Veröffentlichungsdatum</label>
            <input
              v-model="editingModel.releaseDate"
              type="date"
              class="w-full p-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Trainiert bis (Daten-Cutoff)</label>
            <input
              v-model="editingModel.trainedUntil"
              type="text"
              placeholder="z.B. Oktober 2023, Q4 2023"
              class="w-full p-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Lizenz</label>
            <input
              v-model="editingModel.license"
              type="text"
              placeholder="z.B. Apache 2.0, MIT"
              class="w-full p-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Notizen</label>
            <textarea
              v-model="editingModel.notes"
              rows="3"
              class="w-full p-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            ></textarea>
          </div>
        </div>

        <div class="flex gap-3 mt-6">
          <button
            @click="saveMetadata"
            class="flex-1 px-4 py-2 bg-fleet-orange-500 hover:bg-fleet-orange-600 text-white rounded-lg transition-colors"
          >
            Speichern
          </button>
          <button
            @click="showEditDialog = false"
            class="px-4 py-2 bg-gray-500 hover:bg-gray-600 text-white rounded-lg transition-colors"
          >
            Abbrechen
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Dialog -->
    <div
      v-if="showDeleteDialog"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60]"
    >
      <div class="bg-white dark:bg-gray-800 rounded-lg p-6 w-full max-w-md">
        <h3 class="text-xl font-bold mb-4 text-gray-900 dark:text-white">Modell löschen?</h3>
        <p class="text-gray-600 dark:text-gray-400 mb-6">
          Möchten Sie das Modell <strong>{{ modelToDelete }}</strong> wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.
        </p>
        <div class="flex gap-3">
          <button
            @click="deleteModel"
            class="flex-1 px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg transition-colors"
          >
            Ja, löschen
          </button>
          <button
            @click="showDeleteDialog = false"
            class="px-4 py-2 bg-gray-500 hover:bg-gray-600 text-white rounded-lg transition-colors"
          >
            Abbrechen
          </button>
        </div>
      </div>
  </div>

  <!-- File Selection Modal (for HuggingFace models with multiple GGUF files) -->
  <div v-if="showFileSelectionModal" class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-[60] p-4">
    <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-2xl border border-gray-200 dark:border-gray-700">
      <!-- Header -->
      <div class="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700 bg-gradient-to-r from-purple-500/10 to-indigo-500/10 dark:from-purple-500/20 dark:to-indigo-500/20">
        <div class="flex items-center gap-3">
          <div class="p-2 rounded-lg bg-gradient-to-br from-purple-500 to-indigo-500">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          </div>
          <div>
            <h3 class="text-xl font-bold text-gray-900 dark:text-white">GGUF-Datei auswählen</h3>
            <p class="text-sm text-gray-500 dark:text-gray-400">{{ fileSelectionModel?.displayName || fileSelectionModel?.name }}</p>
          </div>
        </div>
        <button
          @click="closeFileSelectionModal"
          class="p-2 rounded-lg text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-all"
        >
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Content -->
      <div class="p-6">
        <p class="text-sm text-gray-600 dark:text-gray-400 mb-4">
          Dieses Modell bietet mehrere GGUF-Varianten an. Wähle die gewünschte Datei aus:
        </p>

        <div class="space-y-2 max-h-96 overflow-y-auto">
          <label
            v-for="(file, index) in fileSelectionFiles"
            :key="index"
            class="flex items-center gap-3 p-4 rounded-lg border-2 transition-all cursor-pointer"
            :class="selectedFileIndex === index
              ? 'border-fleet-orange-500 bg-fleet-orange-50 dark:bg-fleet-orange-900/20'
              : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600 bg-white dark:bg-gray-800'"
          >
            <input
              type="radio"
              :value="index"
              v-model="selectedFileIndex"
              class="w-4 h-4 text-fleet-orange-500 focus:ring-fleet-orange-500"
            />
            <div class="flex-1 min-w-0">
              <p class="font-mono text-sm font-medium text-gray-900 dark:text-white truncate">
                {{ file }}
              </p>
            </div>
          </label>
        </div>
      </div>

      <!-- Footer -->
      <div class="flex items-center justify-end gap-3 p-6 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900/50">
        <button
          @click="closeFileSelectionModal"
          class="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-all"
        >
          Abbrechen
        </button>
        <button
          @click="confirmFileSelection"
          class="px-6 py-2 rounded-lg bg-gradient-to-r from-fleet-orange-500 to-orange-600 text-white font-medium hover:from-fleet-orange-600 hover:to-orange-700 transition-all shadow-lg hover:shadow-xl"
        >
          Download starten
        </button>
      </div>
    </div>
  </div>

  <!-- Create Custom Model Wizard (ersetzt das alte Modal) -->
  <CustomModelWizard
    :show="showCreateCustomModel"
    @close="showCreateCustomModel = false"
    @created="handleCustomModelCreated"
  />

  <!-- Edit Custom Model Modal -->
  <Transition name="modal">
    <div v-if="showEditCustomModel" class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-[60] p-4">
      <div class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <!-- Header -->
        <div class="flex items-center justify-between p-6 border-b border-gray-200 dark:border-gray-700">
          <div class="flex items-center gap-3">
            <PencilIcon class="w-6 h-6 text-amber-500" />
            <h3 class="text-xl font-bold text-gray-900 dark:text-white">
              Custom Model bearbeiten
            </h3>
          </div>
          <button
            @click="showEditCustomModel = false"
            :disabled="isUpdatingCustomModel"
            class="p-2 rounded-lg text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors disabled:opacity-50"
          >
            <XMarkIcon class="w-5 h-5" />
          </button>
        </div>

        <!-- Content -->
        <div class="p-6 space-y-5" v-if="!isUpdatingCustomModel">
          <!-- Model Info -->
          <div class="bg-gray-50 dark:bg-gray-900 rounded-lg p-4">
            <p class="text-sm text-gray-600 dark:text-gray-400">
              <span class="font-medium">Modell:</span> {{ editingCustomModel?.name }}
            </p>
            <p class="text-sm text-gray-600 dark:text-gray-400">
              <span class="font-medium">Basis:</span> {{ editingCustomModel?.baseModel }}
            </p>
          </div>

          <!-- Description -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Beschreibung
            </label>
            <input
              v-model="editForm.description"
              type="text"
              class="w-full px-4 py-2.5 bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-amber-500 focus:border-transparent"
            />
          </div>

          <!-- System Prompt -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              System Prompt
            </label>
            <textarea
              v-model="editForm.systemPrompt"
              rows="4"
              class="w-full px-4 py-2.5 bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-600 text-gray-900 dark:text-white rounded-lg focus:ring-2 focus:ring-amber-500 focus:border-transparent resize-none"
            ></textarea>
          </div>

          <!-- Parameters Grid -->
          <div class="grid grid-cols-2 gap-4">
            <!-- Temperature -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Temperature: {{ editForm.temperature }}
              </label>
              <input
                v-model.number="editForm.temperature"
                type="range"
                min="0"
                max="2"
                step="0.1"
                class="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-amber-500"
              />
            </div>

            <!-- Top P -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Top P: {{ editForm.topP }}
              </label>
              <input
                v-model.number="editForm.topP"
                type="range"
                min="0"
                max="1"
                step="0.05"
                class="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-amber-500"
              />
            </div>

            <!-- Top K -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Top K: {{ editForm.topK }}
              </label>
              <input
                v-model.number="editForm.topK"
                type="range"
                min="0"
                max="100"
                step="5"
                class="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-amber-500"
              />
            </div>

            <!-- Repeat Penalty -->
            <div>
              <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                Repeat Penalty: {{ editForm.repeatPenalty }}
              </label>
              <input
                v-model.number="editForm.repeatPenalty"
                type="range"
                min="1"
                max="2"
                step="0.1"
                class="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-amber-500"
              />
            </div>
          </div>

          <!-- Max Tokens -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Max Tokens: {{ editForm.numPredict }}
            </label>
            <input
              v-model.number="editForm.numPredict"
              type="range"
              min="128"
              max="32000"
              step="128"
              class="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-amber-500"
            />
          </div>

          <!-- Context Length -->
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
              Context Length: {{ editForm.numCtx.toLocaleString() }} Tokens
            </label>
            <input
              v-model.number="editForm.numCtx"
              type="range"
              min="2048"
              max="131072"
              step="2048"
              class="w-full h-2 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer accent-amber-500"
            />
            <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">
              Mehr Context = mehr VRAM benötigt
            </p>
          </div>
        </div>

        <!-- Progress indicator -->
        <div v-else class="p-6 flex flex-col items-center justify-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-amber-500 mb-4"></div>
          <p class="text-gray-600 dark:text-gray-400">{{ updateProgress }}</p>
        </div>

        <!-- Footer -->
        <div class="flex justify-end gap-3 p-6 border-t border-gray-200 dark:border-gray-700">
          <button
            @click="showEditCustomModel = false"
            :disabled="isUpdatingCustomModel"
            class="px-4 py-2 rounded-lg text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors disabled:opacity-50"
          >
            Abbrechen
          </button>
          <button
            @click="saveCustomModelEdit"
            :disabled="isUpdatingCustomModel"
            class="px-4 py-2 rounded-lg bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-600 hover:to-orange-600 text-white font-medium transition-all disabled:opacity-50 flex items-center gap-2"
          >
            <SparklesIcon class="w-4 h-4" />
            Neue Version erstellen
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  XMarkIcon,
  SparklesIcon,
  CpuChipIcon,
  ServerIcon,
  GlobeAltIcon,
  ArrowPathIcon,
  MagnifyingGlassIcon,
  StarIcon,
  ArrowDownTrayIcon,
  TrashIcon,
  PencilIcon,
  InformationCircleIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  XCircleIcon,
  CloudArrowDownIcon,
  Cog6ToothIcon,
  UserGroupIcon,
  ChatBubbleLeftRightIcon,
  PlusIcon
} from '@heroicons/vue/24/outline'
import api from '../services/api'
import { secureFetch } from '../utils/secureFetch'
import { useChatStore } from '../stores/chatStore'
import { useToast } from '../composables/useToast'
import { useConfirmDialog } from '../composables/useConfirmDialog'
import { formatDateLong } from '../composables/useFormatters'
import { canModelHandleContext, getIncompatibilityReason, getSafeContextLimit } from '../utils/modelContextWindows'
import CustomModelWizard from './CustomModelWizard.vue'
import ModelDownloadModal from './ModelDownloadModal.vue'
import GgufModelConfigModal from './GgufModelConfigModal.vue'
import CreateExpertModal from './CreateExpertModal.vue'
import ExpertCreationWizard from './ExpertCreationWizard.vue'

const { success, error: errorToast } = useToast()
const { confirm: showConfirm, confirmDelete: showDeleteConfirm } = useConfirmDialog()
const router = useRouter()

const emit = defineEmits(['close'])

const chatStore = useChatStore()
const models = ref([])
const isLoading = ref(false)

// Active Provider Detection (Ollama or llama.cpp)
const activeProvider = ref('java-llama-cpp') // Default to llama.cpp
const providerAvailable = ref(true)

// Custom Model Modal
const showCreateCustomModel = ref(false)

// Edit Custom Model Modal
const showEditCustomModel = ref(false)
const editingCustomModel = ref(null)
const editForm = ref({
  systemPrompt: '',
  description: '',
  temperature: 0.8,
  topP: 0.9,
  topK: 40,
  repeatPenalty: 1.1,
  numPredict: 2048,
  numCtx: 8192
})
const isUpdatingCustomModel = ref(false)
const updateProgress = ref('')

// GGUF Model Config Modal
const showGgufConfigModal = ref(false)
const currentGgufConfig = ref(null)
const ggufModels = computed(() => {
  return generalModels.value.filter(m => m.name.endsWith('.gguf'))
})

// Expert Create Modal
const showCreateExpertModal = ref(false)
const editingExpertForCreate = ref(null)
const availableModelsForExpert = ref([])

// Expert Creation Wizard
const showExpertWizard = ref(false)

// File Selection Modal (for HuggingFace models with multiple GGUF files)
const showFileSelectionModal = ref(false)
const fileSelectionModel = ref(null)
const fileSelectionFiles = ref([])
const selectedFileIndex = ref(0)

// Tabs
const activeTab = ref('installed')
const installedSubTab = ref('experts')  // Sub-tab for installed models: experts, downloaded, custom
const downloadedFilter = ref('all')  // Filter for downloaded models: all, coder, vision, general
const searchQuery = ref('')  // For available models
const installedSearchQuery = ref('')  // For installed models
const customSearchQuery = ref('')  // For custom models
const downloadedSearchQuery = ref('')  // For downloaded models
const selectedCategory = ref('Alle')

// Custom Models
const customModels = ref([])
const isLoadingCustom = ref(false)

// Categories
const categories = ['Alle', 'Chat', 'Code', 'Vision', 'Compact']
const activeCategory = ref('Alle')

// GGUF Model Store models (loaded from API)
const availableModels = ref([])
const isLoadingLibrary = ref(false)

// Ollama Library models
const ollamaLibraryModels = ref([])
const isLoadingOllamaLibrary = ref(false)
const ollamaCategoryFilter = ref('alle')

// HuggingFace search
const hfSearchQuery = ref('')
const hfSearchResults = ref([])
const isSearchingHF = ref(false)

// Filtered installed models based on search
const filteredInstalledModels = computed(() => {
  if (!installedSearchQuery.value.trim()) {
    return models.value
  }
  const query = installedSearchQuery.value.toLowerCase()
  return models.value.filter(m =>
    m.name.toLowerCase().includes(query) ||
    m.description?.toLowerCase().includes(query)
  )
})

// Filtered custom models based on search
// Combines both: Database custom models + Ollama custom models (with custom: true flag)
// DEDUPLICATES by model name to prevent duplicates
const filteredCustomModels = computed(() => {
  // Merge database custom models with Ollama custom models
  const ollamaCustomModels = models.value.filter(m => m.custom === true || m.isCustom === true)

  // Deduplicate by name - database models take priority
  const seenNames = new Set()
  const allCustomModels = []

  // First add database custom models (priority)
  for (const model of customModels.value) {
    const normalizedName = model.name?.toLowerCase()
    if (normalizedName && !seenNames.has(normalizedName)) {
      seenNames.add(normalizedName)
      allCustomModels.push(model)
    }
  }

  // Then add Ollama custom models (only if not already in list)
  for (const model of ollamaCustomModels) {
    const normalizedName = model.name?.toLowerCase()
    if (normalizedName && !seenNames.has(normalizedName)) {
      seenNames.add(normalizedName)
      allCustomModels.push(model)
    }
  }

  if (!customSearchQuery.value.trim()) {
    return allCustomModels
  }
  const query = customSearchQuery.value.toLowerCase()
  return allCustomModels.filter(m =>
    m.name.toLowerCase().includes(query) ||
    m.description?.toLowerCase().includes(query) ||
    m.baseModel?.toLowerCase().includes(query)
  )
})

// Helper function: Determine model category
// Helper function: Determine model category based on GGUF metadata
function getModelCategory(modelName) {
  // Handle undefined or null model names
  if (!modelName) {
    console.warn('getModelCategory called with invalid modelName:', modelName)
    return 'unknown'
  }

  // Find the model object to access its metadata
  const model = models.value.find(m => m.name === modelName)

  // If no model found, return unknown
  if (!model) {
    return 'unknown'
  }

  // Check if it's a custom model (GgufModelConfig from database OR Ollama custom model)
  if (model.isCustom || model.custom || model.baseModel) {
    return 'custom'
  }

  // Get metadata for categorization
  const specialties = (model.specialties || '').toLowerCase()
  const description = (model.description || '').toLowerCase()
  const name = modelName.toLowerCase()

  // Vision models: Check for vision-related keywords in GGUF metadata
  const visionKeywords = ['vision', 'image', 'multimodal', 'visual', 'bild', 'foto', 'image to text', 'image-to-text']
  if (visionKeywords.some(keyword =>
    specialties.includes(keyword) ||
    description.includes(keyword) ||
    name.includes('llava') ||
    name.includes('moondream') ||
    name.includes('vision')
  )) {
    return 'vision'
  }

  // Coder models: Check for code-related keywords in GGUF metadata
  const coderKeywords = ['code', 'coding', 'programming', 'coder', 'python', 'javascript', 'java', 'programmier', 'software']
  if (coderKeywords.some(keyword =>
    specialties.includes(keyword) ||
    description.includes(keyword) ||
    name.includes('coder') ||
    name.includes('codellama')
  )) {
    return 'coder'
  }

  // General chat models (everything else: chat, instruct, etc.)
  return 'general'
}

// Downloaded models (all non-custom GGUF models) with filter
const downloadedModels = computed(() => {
  // Get all models that are NOT custom (regular GGUF files from HuggingFace/llama.cpp)
  let filtered = models.value.filter(m => getModelCategory(m.name) !== 'custom')

  // Apply type filter (coder, vision, general)
  if (downloadedFilter.value !== 'all') {
    filtered = filtered.filter(m => getModelCategory(m.name) === downloadedFilter.value)
  }

  // Apply search query
  if (downloadedSearchQuery.value.trim()) {
    const query = downloadedSearchQuery.value.toLowerCase()
    filtered = filtered.filter(m =>
      m.name.toLowerCase().includes(query) ||
      m.description?.toLowerCase().includes(query)
    )
  }

  return filtered
})

// Keep these for backward compatibility (some code might still reference them)
const coderModels = computed(() => models.value.filter(m => getModelCategory(m.name) === 'coder'))
const visionModels = computed(() => models.value.filter(m => getModelCategory(m.name) === 'vision'))
const generalModels = computed(() => models.value.filter(m => getModelCategory(m.name) === 'general'))

// Filtered available models based on category and search
const filteredAvailableModels = computed(() => {
  let filtered = availableModels.value

  // Filter by category
  if (activeCategory.value !== 'Alle') {
    const categoryLower = activeCategory.value.toLowerCase()
    filtered = filtered.filter(m => {
      // Model Store models have a 'category' property
      return m.category && m.category.toLowerCase() === categoryLower
    })
  }

  // Filter by search query
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    filtered = filtered.filter(m =>
      m.name?.toLowerCase().includes(query) ||
      m.displayName?.toLowerCase().includes(query) ||
      m.description?.toLowerCase().includes(query) ||
      m.model?.toLowerCase().includes(query)
    )
  }

  // Sort by size (largest first)
  return filtered.sort((a, b) => (b.sizeBytes || 0) - (a.sizeBytes || 0))
})

// Filtered Ollama Library models based on category
const filteredOllamaLibraryModels = computed(() => {
  if (ollamaCategoryFilter.value === 'alle') {
    return ollamaLibraryModels.value
  }
  return ollamaLibraryModels.value.filter(m => m.category === ollamaCategoryFilter.value)
})

// Check if model is already installed
function isInstalled(modelName) {
  return models.value.some(m => m.name === modelName)
}

// Get Ollama Library URL for model
function getOllamaLibraryUrl(modelName) {
  // Extract base name (before colon) for URL
  // Example: "llama3.2:3b" -> "llama3.2"
  const baseName = modelName.includes(':') ? modelName.split(':')[0] : modelName
  return `https://ollama.com/library/${baseName}`
}

// Check if model supports all 5 required languages: DE, TR, ES, EN, FR
function supportsAllRequiredLanguages(model) {
  if (!model.languages) return false

  const langs = model.languages.toUpperCase()
  const requiredLanguages = ['DE', 'TR', 'ES', 'EN', 'FR']

  // Check if all required languages are present in the languages string
  return requiredLanguages.every(lang => langs.includes(lang))
}

// Check if model has updates available by comparing digests
function hasUpdate(modelName) {
  // Find installed model
  const installedModel = models.value.find(m => m.name === modelName)
  if (!installedModel || !installedModel.digest) {
    return false
  }

  // Find library model
  const libraryModel = availableModels.value.find(m => m.name === modelName)
  if (!libraryModel || !libraryModel.digest) {
    return false
  }

  // Compare digests - different digest means update available
  return installedModel.digest !== libraryModel.digest
}

// Download dialog
const showDownloadDialog = ref(false)
const downloadModelName = ref('')
const isDownloading = ref(false)
const downloadProgress = ref('')
const downloadProgressPercent = ref(0)

// Details dialog
const showDetailsDialog = ref(false)
const selectedModelDetails = ref(null)

// Edit dialog
const showEditDialog = ref(false)
const editingModel = ref({})

// Delete dialog
const showDeleteDialog = ref(false)
const modelToDelete = ref('')

// llama.cpp Download Modal
const showLlamaCppDownloadModal = ref(false)
const currentDownloadModel = ref('')
const currentDownloadProgress = ref(0)
const currentDownloadedSize = ref('0 MB')
const currentTotalSize = ref('0 MB')
const currentSpeed = ref('0.0')
const downloadStatusMessages = ref([])
const currentDownloadModelId = ref('')
const activeDownloadEventSource = ref(null)

onMounted(async () => {
  await loadProviderInfo()   // First: Load active provider
  await loadModels()          // Then: Load models for that provider (Flip-Flop!)

  // Load library models based on provider
  if (activeProvider.value === 'ollama') {
    await loadOllamaLibrary()
  } else {
    await loadLibraryModels()
  }

  await loadCustomModels()   // Then detect custom models

  // Listen for provider changes
  window.addEventListener('provider-changed', handleProviderChange)
})

onUnmounted(() => {
  // Clean up event listener
  window.removeEventListener('provider-changed', handleProviderChange)
})

// Handle provider change event
async function handleProviderChange(event) {
  console.log('Provider changed event received:', event.detail.provider)
  activeProvider.value = event.detail.provider

  // Reload models for new provider
  await loadModels()

  // Reload library models if switching to llama.cpp
  if (activeProvider.value !== 'ollama') {
    await loadLibraryModels()
  }
}

// Load active provider information
async function loadProviderInfo() {
  try {
    const response = await api.getProviderStatus()
    activeProvider.value = response.activeProvider || 'java-llama-cpp'
    console.log('Active provider:', activeProvider.value)
  } catch (error) {
    console.error('Failed to load provider info:', error)
    activeProvider.value = 'java-llama-cpp' // Fallback to llama.cpp
  }
}

// Load models based on active provider (Flip-Flop!)
async function loadModels() {
  isLoading.value = true
  try {
    if (activeProvider.value === 'ollama') {
      // Load Ollama models
      console.log('Loading Ollama models...')
      models.value = await api.getOllamaModels()
      console.log(`Loaded ${models.value.length} Ollama models`)
    } else {
      // Load llama.cpp models (default)
      console.log('Loading llama.cpp models...')
      models.value = await api.getAvailableModels()
      console.log(`Loaded ${models.value.length} llama.cpp models`)
    }
  } catch (error) {
    console.error('Failed to load models:', error)
    models.value = []
  } finally {
    isLoading.value = false
  }
}

async function loadLibraryModels() {
  isLoadingLibrary.value = true
  try {
    console.log('Loading GGUF Model Store...')
    availableModels.value = await api.getAllModelStoreModels()
    console.log(`Loaded ${availableModels.value.length} models from Model Store`)
  } catch (error) {
    console.error('Failed to load library models:', error)
    availableModels.value = []
  } finally {
    isLoadingLibrary.value = false
  }
}

/**
 * Load Ollama Library models
 */
async function loadOllamaLibrary() {
  isLoadingOllamaLibrary.value = true
  try {
    console.log('Loading Ollama Library...')
    // Curated list of 40 popular Ollama models with comprehensive metadata
    ollamaLibraryModels.value = [
      // === General Chat Models (Top Performers) ===
      {
        name: 'llama3.2:3b',
        description: 'Meta Llama 3.2 - Klein, schnell, für den Alltag',
        size: '2.0 GB',
        parameters: '3B',
        emoji: '🦙',
        publisher: 'Meta AI',
        releaseDate: '2024-09',
        trainedUntil: '2023-12',
        license: 'Llama 3.2 Community License',
        specialties: 'Chat, Instruction Following, Mehrsprachig',
        recommendedRAM: '4 GB',
        category: 'chat',
        languages: 'Mehrsprachig (DE, EN, FR, ES, IT, PT, TR, ...)'
      },
      {
        name: 'llama3.2:1b',
        description: 'Meta Llama 3.2 - Ultra kompakt für schwache Hardware',
        size: '1.3 GB',
        parameters: '1B',
        emoji: '🦙',
        publisher: 'Meta AI',
        releaseDate: '2024-09',
        trainedUntil: '2023-12',
        license: 'Llama 3.2 Community License',
        specialties: 'Chat, Edge Devices, Mobile',
        recommendedRAM: '2 GB',
        category: 'chat',
        languages: 'Mehrsprachig (DE, EN, FR, ES, IT, PT, TR, ...)'
      },
      {
        name: 'llama3.1:8b',
        description: 'Meta Llama 3.1 - Balanced Performance',
        size: '4.7 GB',
        parameters: '8B',
        emoji: '🦙',
        publisher: 'Meta AI',
        releaseDate: '2024-07',
        trainedUntil: '2023-12',
        license: 'Llama 3.1 Community License',
        specialties: 'Chat, Reasoning, Multilingua',
        recommendedRAM: '8 GB',
        category: 'chat',
        languages: 'Mehrsprachig (DE, EN, FR, ES, IT, PT, TR, ...)'
      },
      {
        name: 'llama3.1:70b',
        description: 'Meta Llama 3.1 - Top-Tier Performance, High Quality',
        size: '40 GB',
        parameters: '70B',
        emoji: '🦙',
        publisher: 'Meta AI',
        releaseDate: '2024-07',
        trainedUntil: '2023-12',
        license: 'Llama 3.1 Community License',
        specialties: 'Advanced Reasoning, Professional Use, Research',
        recommendedRAM: '64 GB',
        category: 'chat',
        languages: 'Mehrsprachig (DE, EN, FR, ES, IT, PT, TR, ...)'
      },
      {
        name: 'mistral:7b',
        description: 'Mistral AI - Ausgewogen und leistungsstark',
        size: '4.1 GB',
        parameters: '7B',
        emoji: '🌪️',
        publisher: 'Mistral AI',
        releaseDate: '2023-09',
        trainedUntil: '2023-09',
        license: 'Apache 2.0',
        specialties: 'Chat, Instruction, Reasoning',
        recommendedRAM: '8 GB',
        category: 'chat',
        languages: 'Mehrsprachig (DE, EN, FR, ES, IT, TR, ...)'
      },
      {
        name: 'mistral:7b-instruct',
        description: 'Mistral AI - Instruction-tuned für präzise Antworten',
        size: '4.1 GB',
        parameters: '7B',
        emoji: '🌪️',
        publisher: 'Mistral AI',
        releaseDate: '2023-10',
        trainedUntil: '2023-09',
        license: 'Apache 2.0',
        specialties: 'Instructions, Tasks, Q&A',
        recommendedRAM: '8 GB',
        category: 'chat',
        languages: 'Mehrsprachig (DE, EN, FR, ES, IT, TR, ...)'
      },
      {
        name: 'mixtral:8x7b',
        description: 'Mistral AI - Mixture of Experts, extrem leistungsstark',
        size: '26 GB',
        parameters: '47B (8x7B MoE)',
        emoji: '⚡',
        publisher: 'Mistral AI',
        releaseDate: '2023-12',
        trainedUntil: '2023-09',
        license: 'Apache 2.0',
        specialties: 'Advanced Reasoning, Multilingual, Code',
        recommendedRAM: '32 GB',
        category: 'chat',
        languages: 'Mehrsprachig (DE, EN, FR, ES, IT, TR, ...)'
      },
      {
        name: 'qwen2.5:7b',
        description: 'Alibaba Qwen 2.5 - Hervorragend für Chat & Code',
        size: '4.7 GB',
        parameters: '7B',
        emoji: '🧠',
        publisher: 'Alibaba Cloud',
        releaseDate: '2024-09',
        trainedUntil: '2024-06',
        license: 'Apache 2.0',
        specialties: 'Chat, Code, Multilingual (29 Sprachen)',
        recommendedRAM: '8 GB',
        category: 'chat',
        languages: '29 Sprachen (DE, EN, ZH, FR, ES, IT, PT, TR, ...)'
      },
      {
        name: 'qwen2.5:14b',
        description: 'Alibaba Qwen 2.5 - Größere Version für komplexe Aufgaben',
        size: '9.0 GB',
        parameters: '14B',
        emoji: '🧠',
        publisher: 'Alibaba Cloud',
        releaseDate: '2024-09',
        trainedUntil: '2024-06',
        license: 'Apache 2.0',
        specialties: 'Advanced Chat, Code, Math, Reasoning',
        recommendedRAM: '16 GB',
        category: 'chat',
        languages: '29 Sprachen (DE, EN, ZH, FR, ES, IT, PT, TR, ...)'
      },
      {
        name: 'qwen2.5:32b',
        description: 'Alibaba Qwen 2.5 - Premium-Modell für höchste Qualität',
        size: '19 GB',
        parameters: '32B',
        emoji: '🧠',
        publisher: 'Alibaba Cloud',
        releaseDate: '2024-09',
        trainedUntil: '2024-06',
        license: 'Apache 2.0',
        specialties: 'Professional Use, Research, Advanced Reasoning',
        recommendedRAM: '32 GB',
        category: 'chat',
        languages: '29 Sprachen (DE, EN, ZH, FR, ES, IT, PT, TR, ...)'
      },
      {
        name: 'gemma2:2b',
        description: 'Google Gemma 2 - Ultra kompakt und schnell',
        size: '1.6 GB',
        parameters: '2B',
        emoji: '💎',
        publisher: 'Google DeepMind',
        releaseDate: '2024-06',
        trainedUntil: '2024-02',
        license: 'Gemma Terms of Use',
        specialties: 'Fast Chat, Edge Devices',
        recommendedRAM: '4 GB',
        category: 'chat',
        languages: 'Mehrsprachig (EN, DE, FR, ES, IT, TR, ...)'
      },
      {
        name: 'gemma2:9b',
        description: 'Google Gemma 2 - Effizient und präzise',
        size: '5.4 GB',
        parameters: '9B',
        emoji: '💎',
        publisher: 'Google DeepMind',
        releaseDate: '2024-06',
        trainedUntil: '2024-02',
        license: 'Gemma Terms of Use',
        specialties: 'Chat, Reasoning, Multilingual',
        recommendedRAM: '12 GB',
        category: 'chat',
        languages: 'Mehrsprachig (EN, DE, FR, ES, IT, TR, ...)'
      },
      {
        name: 'gemma2:27b',
        description: 'Google Gemma 2 - Höchste Qualität, sehr leistungsstark',
        size: '16 GB',
        parameters: '27B',
        emoji: '💎',
        publisher: 'Google DeepMind',
        releaseDate: '2024-06',
        trainedUntil: '2024-02',
        license: 'Gemma Terms of Use',
        specialties: 'Advanced Reasoning, Professional Use',
        recommendedRAM: '32 GB',
        category: 'chat',
        languages: 'Mehrsprachig (EN, DE, FR, ES, IT, TR, ...)'
      },
      {
        name: 'phi3:14b',
        description: 'Microsoft Phi-3 - Mittelgroß, sehr effizient',
        size: '7.9 GB',
        parameters: '14B',
        emoji: '🔬',
        publisher: 'Microsoft Research',
        releaseDate: '2024-04',
        trainedUntil: '2023-10',
        license: 'MIT',
        specialties: 'Reasoning, Math, Code',
        recommendedRAM: '16 GB',
        category: 'chat',
        languages: 'Mehrsprachig (EN, DE, FR, ES, IT, TR, ...)'
      },

      // === Code Specialists ===
      {
        name: 'codellama:7b',
        description: 'Meta Code Llama - Code-Spezialist, schnell',
        size: '3.8 GB',
        parameters: '7B',
        emoji: '💻',
        publisher: 'Meta AI',
        releaseDate: '2023-08',
        trainedUntil: '2023-07',
        license: 'Llama 2 Community License',
        specialties: 'Code Generation, Completion, Infilling',
        recommendedRAM: '8 GB',
        category: 'code',
        languages: 'Englisch (Code-Kommentare mehrsprachig)'
      },
      {
        name: 'codellama:13b',
        description: 'Meta Code Llama - Ausgewogene Code-Performance',
        size: '7.3 GB',
        parameters: '13B',
        emoji: '💻',
        publisher: 'Meta AI',
        releaseDate: '2023-08',
        trainedUntil: '2023-07',
        license: 'Llama 2 Community License',
        specialties: 'Code, Python, C++, Java',
        recommendedRAM: '16 GB',
        category: 'code',
        languages: 'Englisch (Code-Kommentare mehrsprachig)'
      },
      {
        name: 'codellama:34b',
        description: 'Meta Code Llama - Professionelle Code-Qualität',
        size: '19 GB',
        parameters: '34B',
        emoji: '💻',
        publisher: 'Meta AI',
        releaseDate: '2023-08',
        trainedUntil: '2023-07',
        license: 'Llama 2 Community License',
        specialties: 'Advanced Code, Architecture, Debugging',
        recommendedRAM: '32 GB',
        category: 'code',
        languages: 'Englisch (Code-Kommentare mehrsprachig)'
      },
      {
        name: 'deepseek-coder:6.7b',
        description: 'DeepSeek Coder - Exzellenter Code-Experte',
        size: '3.8 GB',
        parameters: '6.7B',
        emoji: '🔎',
        publisher: 'DeepSeek AI',
        releaseDate: '2023-11',
        trainedUntil: '2023-06',
        license: 'DeepSeek License',
        specialties: 'Code, 80+ Programming Languages',
        recommendedRAM: '8 GB',
        category: 'code',
        languages: 'Englisch + Chinesisch'
      },
      {
        name: 'deepseek-coder:33b',
        description: 'DeepSeek Coder - Top-Tier Code Generation',
        size: '18 GB',
        parameters: '33B',
        emoji: '🔎',
        publisher: 'DeepSeek AI',
        releaseDate: '2023-11',
        trainedUntil: '2023-06',
        license: 'DeepSeek License',
        specialties: 'Advanced Code, Refactoring, Code Review',
        recommendedRAM: '32 GB',
        category: 'code',
        languages: 'Englisch + Chinesisch'
      },
      {
        name: 'qwen2.5-coder:7b',
        description: 'Qwen Coder - Spezialisiert auf Code, sehr modern',
        size: '4.3 GB',
        parameters: '7B',
        emoji: '⌨️',
        publisher: 'Alibaba Cloud',
        releaseDate: '2024-11',
        trainedUntil: '2024-08',
        license: 'Apache 2.0',
        specialties: 'Code, 40+ Languages, Code Reasoning',
        recommendedRAM: '8 GB',
        category: 'code',
        languages: 'Mehrsprachig (DE, EN, ZH, FR, ES, IT, TR, ...)'
      },
      {
        name: 'qwen2.5-coder:14b',
        description: 'Qwen Coder - Professionelle Code-Qualität',
        size: '8.7 GB',
        parameters: '14B',
        emoji: '⌨️',
        publisher: 'Alibaba Cloud',
        releaseDate: '2024-11',
        trainedUntil: '2024-08',
        license: 'Apache 2.0',
        specialties: 'Advanced Code, Architecture, Testing',
        recommendedRAM: '16 GB',
        category: 'code',
        languages: 'Mehrsprachig (DE, EN, ZH, FR, ES, IT, TR, ...)'
      },
      {
        name: 'starcoder2:7b',
        description: 'BigCode StarCoder2 - Open-Source Code-Champion',
        size: '4.0 GB',
        parameters: '7B',
        emoji: '⭐',
        publisher: 'BigCode Project',
        releaseDate: '2024-02',
        trainedUntil: '2023-09',
        license: 'BigCode OpenRAIL-M',
        specialties: 'Code, 600+ Languages, Fill-in-Middle',
        recommendedRAM: '8 GB',
        category: 'code',
        languages: 'Englisch'
      },
      {
        name: 'starcoder2:15b',
        description: 'BigCode StarCoder2 - Leistungsstarke Code-Generation',
        size: '9.1 GB',
        parameters: '15B',
        emoji: '⭐',
        publisher: 'BigCode Project',
        releaseDate: '2024-02',
        trainedUntil: '2023-09',
        license: 'BigCode OpenRAIL-M',
        specialties: 'Advanced Code, Multiple Languages',
        recommendedRAM: '16 GB',
        category: 'code',
        languages: 'Englisch'
      },

      // === Vision Models (Multimodal) ===
      {
        name: 'llava:7b',
        description: 'LLaVA - Vision & Chat kombiniert, kompakte Version',
        size: '4.5 GB',
        parameters: '7B',
        emoji: '👁️',
        publisher: 'Microsoft/University of Wisconsin',
        releaseDate: '2023-10',
        trainedUntil: '2023-09',
        license: 'Apache 2.0',
        specialties: 'Vision, Image Understanding, Visual Q&A',
        recommendedRAM: '8 GB',
        category: 'vision',
        languages: 'Englisch'
      },
      {
        name: 'llava:13b',
        description: 'LLaVA - Multimodal mit Vision, ausgewogen',
        size: '8.0 GB',
        parameters: '13B',
        emoji: '👁️',
        publisher: 'Microsoft/University of Wisconsin',
        releaseDate: '2023-10',
        trainedUntil: '2023-09',
        license: 'Apache 2.0',
        specialties: 'Advanced Vision, OCR, Chart Analysis',
        recommendedRAM: '16 GB',
        category: 'vision',
        languages: 'Englisch'
      },
      {
        name: 'llava:34b',
        description: 'LLaVA - Höchste Vision-Qualität, professionell',
        size: '19 GB',
        parameters: '34B',
        emoji: '👁️',
        publisher: 'Microsoft/University of Wisconsin',
        releaseDate: '2024-01',
        trainedUntil: '2023-12',
        license: 'Apache 2.0',
        specialties: 'Professional Vision, Complex Scenes, Details',
        recommendedRAM: '32 GB',
        category: 'vision',
        languages: 'Englisch + begrenzt mehrsprachig'
      },
      {
        name: 'bakllava:7b',
        description: 'BakLLaVA - Verbesserte Vision-Fähigkeiten',
        size: '4.5 GB',
        parameters: '7B',
        emoji: '🥐',
        publisher: 'SkunkworksAI',
        releaseDate: '2023-11',
        trainedUntil: '2023-10',
        license: 'Llama 2 License',
        specialties: 'Vision, Image Description, Visual Reasoning',
        recommendedRAM: '8 GB',
        category: 'vision',
        languages: 'Englisch'
      },

      // === Small & Efficient Models ===
      {
        name: 'tinyllama:1.1b',
        description: 'TinyLlama - Kleinster Chat-Bot, für schwache Hardware',
        size: '637 MB',
        parameters: '1.1B',
        emoji: '🐁',
        publisher: 'TinyLlama Team',
        releaseDate: '2024-01',
        trainedUntil: '2023-09',
        license: 'Apache 2.0',
        specialties: 'Edge Devices, Embedded, Raspberry Pi',
        recommendedRAM: '2 GB',
        category: 'spezialisiert',
        languages: 'Englisch'
      },
      {
        name: 'phi:3b',
        description: 'Microsoft Phi - Klein aber leistungsstark',
        size: '2.3 GB',
        parameters: '3B',
        emoji: '🔬',
        publisher: 'Microsoft Research',
        releaseDate: '2023-12',
        trainedUntil: '2023-10',
        license: 'MIT',
        specialties: 'Compact, Reasoning, Knowledge',
        recommendedRAM: '4 GB',
        category: 'spezialisiert',
        languages: 'Mehrsprachig (EN, DE, FR, ES, IT, TR, ...)'
      },

      // === Specialized Models ===
      {
        name: 'neural-chat:7b',
        description: 'Intel Neural Chat - Optimiert für CPU-Performance',
        size: '4.1 GB',
        parameters: '7B',
        emoji: '🧪',
        publisher: 'Intel',
        releaseDate: '2023-11',
        trainedUntil: '2023-10',
        license: 'Apache 2.0',
        specialties: 'CPU Optimized, Chat, Instruction',
        recommendedRAM: '8 GB',
        category: 'spezialisiert',
        languages: 'Englisch'
      },
      {
        name: 'dolphin-mistral:7b',
        description: 'Dolphin - Uncensored Mistral für kreative Aufgaben',
        size: '4.1 GB',
        parameters: '7B',
        emoji: '🐬',
        publisher: 'Eric Hartford',
        releaseDate: '2023-10',
        trainedUntil: '2023-09',
        license: 'Apache 2.0',
        specialties: 'Uncensored, Creative Writing, Roleplay',
        recommendedRAM: '8 GB',
        category: 'spezialisiert',
        languages: 'Mehrsprachig (DE, EN, FR, ES, ...)'
      },
      {
        name: 'orca2:7b',
        description: 'Microsoft Orca 2 - Reasoning-Spezialist',
        size: '3.8 GB',
        parameters: '7B',
        emoji: '🐋',
        publisher: 'Microsoft Research',
        releaseDate: '2023-11',
        trainedUntil: '2023-06',
        license: 'Microsoft Research License',
        specialties: 'Step-by-Step Reasoning, Complex Tasks',
        recommendedRAM: '8 GB',
        category: 'spezialisiert',
        languages: 'Englisch'
      },
      {
        name: 'orca2:13b',
        description: 'Microsoft Orca 2 - Advanced Reasoning',
        size: '7.3 GB',
        parameters: '13B',
        emoji: '🐋',
        publisher: 'Microsoft Research',
        releaseDate: '2023-11',
        trainedUntil: '2023-06',
        license: 'Microsoft Research License',
        specialties: 'Advanced Reasoning, Problem Solving',
        recommendedRAM: '16 GB',
        category: 'spezialisiert',
        languages: 'Englisch'
      },
      {
        name: 'wizard-vicuna:13b',
        description: 'WizardLM + Vicuna - Hybrid für komplexe Aufgaben',
        size: '7.3 GB',
        parameters: '13B',
        emoji: '🧙',
        publisher: 'WizardLM Team',
        releaseDate: '2023-06',
        trainedUntil: '2023-03',
        license: 'Non-Commercial',
        specialties: 'Complex Instructions, Creative Tasks',
        recommendedRAM: '16 GB',
        category: 'spezialisiert',
        languages: 'Englisch'
      },
      {
        name: 'openchat:7b',
        description: 'OpenChat - Fine-tuned für präzise Konversation',
        size: '4.1 GB',
        parameters: '7B',
        emoji: '💬',
        publisher: 'OpenChat Team',
        releaseDate: '2023-07',
        trainedUntil: '2023-06',
        license: 'Apache 2.0',
        specialties: 'Conversation, Q&A, Helpfulness',
        recommendedRAM: '8 GB',
        category: 'spezialisiert',
        languages: 'Mehrsprachig (EN, ZH, ...)'
      },
      {
        name: 'solar:10.7b',
        description: 'Upstage SOLAR - Depth Upscaling Technology',
        size: '6.1 GB',
        parameters: '10.7B',
        emoji: '☀️',
        publisher: 'Upstage AI',
        releaseDate: '2023-12',
        trainedUntil: '2023-09',
        license: 'Apache 2.0',
        specialties: 'Advanced Architecture, Efficiency',
        recommendedRAM: '12 GB',
        category: 'spezialisiert',
        languages: 'Englisch + Koreanisch'
      },
      {
        name: 'yi:6b',
        description: 'Yi - Bilingual (English/Chinese), kompakt',
        size: '3.5 GB',
        parameters: '6B',
        emoji: '🀄',
        publisher: '01.AI',
        releaseDate: '2023-11',
        trainedUntil: '2023-06',
        license: 'Yi License',
        specialties: 'Bilingual, Chinese/English, Chat',
        recommendedRAM: '8 GB',
        category: 'spezialisiert',
        languages: 'Zweisprachig (EN, ZH)'
      },
      {
        name: 'yi:34b',
        description: 'Yi - High-Performance Bilingual Model',
        size: '19 GB',
        parameters: '34B',
        emoji: '🀄',
        publisher: '01.AI',
        releaseDate: '2023-11',
        trainedUntil: '2023-06',
        license: 'Yi License',
        specialties: 'Advanced Bilingual, Professional Use',
        recommendedRAM: '32 GB',
        category: 'spezialisiert',
        languages: 'Zweisprachig (EN, ZH)'
      },
      {
        name: 'nous-hermes2:10.7b',
        description: 'Nous Hermes 2 - Fine-tuned für Hilfsbereitschaft',
        size: '6.4 GB',
        parameters: '10.7B',
        emoji: '🎭',
        publisher: 'Nous Research',
        releaseDate: '2024-01',
        trainedUntil: '2023-09',
        license: 'Apache 2.0',
        specialties: 'Helpful, Honest, Harmless (3H)',
        recommendedRAM: '12 GB',
        category: 'spezialisiert',
        languages: 'Englisch'
      },
      {
        name: 'zephyr:7b',
        description: 'Zephyr - Aligned Mistral für sichere Antworten',
        size: '4.1 GB',
        parameters: '7B',
        emoji: '💨',
        publisher: 'Hugging Face H4',
        releaseDate: '2023-10',
        trainedUntil: '2023-09',
        license: 'Apache 2.0',
        specialties: 'Aligned, Safe, Helpful Responses',
        recommendedRAM: '8 GB',
        category: 'spezialisiert',
        languages: 'Mehrsprachig (DE, EN, FR, ES, ...)'
      }
    ]
    console.log(`Loaded ${ollamaLibraryModels.value.length} Ollama library models`)
  } catch (error) {
    console.error('Failed to load Ollama library:', error)
    ollamaLibraryModels.value = []
  } finally {
    isLoadingOllamaLibrary.value = false
  }
}

/**
 * Download Ollama model
 */
async function downloadOllamaModel(modelName) {
  console.log(`Downloading Ollama model: ${modelName}`)
  try {
    const response = await secureFetch('/api/models/pull', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: modelName })
    })

    if (!response.ok) {
      throw new Error('Download failed')
    }

    // Start downloading - show progress via SSE
    showDownloadDialog.value = true
    downloadProgress.value = 0
    downloadModelName.value = modelName
    downloadStatus.value = 'downloading'

    // Monitor progress via SSE
    const reader = response.body.getReader()
    const decoder = new TextDecoder()

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const text = decoder.decode(value)
      const lines = text.split('\n').filter(line => line.trim())

      for (const line of lines) {
        try {
          const data = JSON.parse(line.replace(/^data: /, ''))
          if (data.status) {
            downloadStatus.value = data.status
            // Parse percentage from status message like "downloading 1.5 GB / 4.0 GB (37%)"
            const match = data.status.match(/(\d+)%/)
            if (match) {
              downloadProgress.value = parseInt(match[1])
            }
          }
        } catch (e) {
          // Ignore parsing errors
        }
      }
    }

    downloadStatus.value = 'completed'
    setTimeout(() => {
      showDownloadDialog.value = false
      loadModels() // Refresh model list
    }, 2000)

  } catch (error) {
    console.error('Failed to download model:', error)
    downloadStatus.value = 'error'
    setTimeout(() => {
      showDownloadDialog.value = false
    }, 3000)
  }
}

async function refreshModels() {
  await loadProviderInfo()   // Re-check active provider
  await loadModels()          // Reload models for active provider

  // Load library models only for llama.cpp
  if (activeProvider.value !== 'ollama') {
    await loadLibraryModels()
  }

  await loadCustomModels()
}

async function setAsDefault(modelName) {
  try {
    await api.setDefaultModel(modelName)

    // Immediately update chatStore selected model for instant feedback
    chatStore.setSelectedModel(modelName)

    // Show success message
    success(`Modell "${modelName}" als Standard gesetzt`)

    // Then reload to get backend confirmation
    await loadModels()
    await chatStore.loadModels()
  } catch (error) {
    console.error('Failed to set default model:', error)
  }
}

// Select model and automatically set as default
async function selectAndSetDefault(modelName) {
  try {
    // Set as default in backend
    await api.setDefaultModel(modelName)

    // Activate in chatStore
    chatStore.setSelectedModel(modelName)

    // Show success message
    success(`Modell "${modelName}" ausgewählt und als Standard gesetzt`)

    // Reload to get backend confirmation
    await loadModels()
    await chatStore.loadModels()

    // Close dialog automatically after selection
    emit('close')
  } catch (error) {
    console.error('Failed to select and set default model:', error)
    errorToast(`Fehler: ${error.message}`)
  }
}

// Select expert and set it up for chat
async function selectExpert(expert) {
  try {
    await chatStore.selectExpert(expert)
    // Nur Benachrichtigung anzeigen wenn beim Experten aktiviert (default: true)
    if (expert.showSwitchNotification !== false) {
      success(`Gewechselt zu ${expert.name}, ${expert.role}`)
    }
    emit('close')
  } catch (error) {
    console.error('Failed to select expert:', error)
    errorToast(`Fehler: ${error.message}`)
  }
}

// Navigate to Expert System page
function openExpertSystem() {
  emit('close')
  router.push('/experts')
}

// Open create expert modal (now opens Wizard instead)
async function openCreateExpertModal() {
  // Open the new Expert Creation Wizard
  showExpertWizard.value = true
}

// Close expert wizard
function closeExpertWizard() {
  showExpertWizard.value = false
}

// Handle expert created from wizard
async function onExpertCreatedFromWizard(expert) {
  // Reload experts in chatStore
  await chatStore.loadModels()
  success(`Experte "${expert?.name || 'Neuer Experte'}" erstellt!`)
}

// Open old expert modal for editing
async function openCreateExpertModalForEditing() {
  // Load available models for the expert
  try {
    availableModelsForExpert.value = await api.getAvailableModels()
  } catch (e) {
    console.warn('Could not load models for expert creation:', e)
    availableModelsForExpert.value = generalModels.value.map(m => m.name)
  }
  editingExpertForCreate.value = null
  showCreateExpertModal.value = true
}

// Close create expert modal
function closeCreateExpertModal() {
  showCreateExpertModal.value = false
  editingExpertForCreate.value = null
}

// Handle expert saved
async function onExpertSaved() {
  closeCreateExpertModal()
  // Reload experts in chatStore
  await chatStore.loadModels()
  success(editingExpertForCreate.value ? 'Experte aktualisiert' : 'Experte erstellt')
}

// Edit expert in modal
async function editExpertInModal(expert) {
  try {
    availableModelsForExpert.value = await api.getAvailableModels()
  } catch (e) {
    console.warn('Could not load models for expert editing:', e)
    availableModelsForExpert.value = generalModels.value.map(m => m.name)
  }
  editingExpertForCreate.value = expert
  showCreateExpertModal.value = true
}

// Delete expert from modal
async function deleteExpertFromModal(expert) {
  const confirmed = await showDeleteConfirm(expert.name, 'Der Experte und alle seine Blickwinkel werden gelöscht.')
  if (!confirmed) return

  try {
    await api.deleteExpert(expert.id)
    await chatStore.loadModels()
    success(`Experte "${expert.name}" gelöscht`)
  } catch (error) {
    console.error('Failed to delete expert:', error)
    errorToast('Fehler beim Löschen des Experten')
  }
}

async function updateModel(modelName) {
  try {
    // Re-pull the model to get the latest version
    downloadModelName.value = modelName
    showDownloadDialog.value = true
    // Automatically start the download
    await startDownload()
  } catch (error) {
    console.error('Failed to update model:', error)
  }
}

async function viewDetails(modelName) {
  try {
    selectedModelDetails.value = await api.getModelDetails(modelName)
    showDetailsDialog.value = true
  } catch (error) {
    console.error('Failed to load model details:', error)
  }
}

function editMetadata(model) {
  editingModel.value = { ...model }
  showEditDialog.value = true
}

async function saveMetadata() {
  try {
    await api.updateModelMetadata(editingModel.value.name, editingModel.value)
    showEditDialog.value = false
    await loadModels()
  } catch (error) {
    console.error('Failed to save metadata:', error)
  }
}

function confirmDelete(modelName) {
  modelToDelete.value = modelName
  showDeleteDialog.value = true
}

async function deleteModel() {
  try {
    await api.deleteModel(modelToDelete.value)
    showDeleteDialog.value = false
    await loadModels()
  } catch (error) {
    console.error('Failed to delete model:', error)
  }
}

function downloadFromLibrary(modelName, model = null) {
  // Use Model Store download with modal
  // modelName is actually the model ID for LlamaCpp
  startLlamaCppDownload(modelName)
}

async function startLlamaCppDownload(modelId) {
  if (showLlamaCppDownloadModal.value) {
    alert('⚠️ Es läuft bereits ein Download.')
    return
  }

  // Find model info
  const model = availableModels.value.find(m => m.id === modelId || m.name === modelId)
  if (!model) {
    alert('Modell nicht gefunden')
    return
  }

  // Show modal
  showLlamaCppDownloadModal.value = true
  currentDownloadModelId.value = model.id || modelId
  currentDownloadModel.value = model.displayName || model.name
  currentDownloadProgress.value = 0
  currentDownloadedSize.value = '0 MB'
  currentTotalSize.value = model.sizeHuman || '0 MB'
  currentSpeed.value = '0.0'
  downloadStatusMessages.value = ['📥 Starte Download...']

  // Create EventSource for SSE
  const eventSource = new EventSource(`/api/model-store/download/${currentDownloadModelId.value}`)
  activeDownloadEventSource.value = eventSource

  eventSource.addEventListener('progress', (event) => {
    const message = event.data
    downloadStatusMessages.value.push(message)

    // Parse progress
    const percentMatch = message.match(/(\d+)%/)
    const downloadedMatch = message.match(/([\d.]+\s+[GM]B)\s+\//)
    const totalMatch = message.match(/\/\s+([\d.]+\s+[GM]B)/)
    const speedMatch = message.match(/([\d.]+)\s+MB\/s/)

    if (percentMatch) currentDownloadProgress.value = parseInt(percentMatch[1])
    if (downloadedMatch) currentDownloadedSize.value = downloadedMatch[1]
    if (totalMatch) currentTotalSize.value = totalMatch[1]
    if (speedMatch) currentSpeed.value = parseFloat(speedMatch[1]).toFixed(1)
  })

  eventSource.addEventListener('complete', (event) => {
    downloadStatusMessages.value.push('✅ ' + event.data)
    eventSource.close()

    // Reload models
    loadModels()

    setTimeout(() => {
      showLlamaCppDownloadModal.value = false
      currentDownloadModelId.value = ''
    }, 2000)

    success('Modell heruntergeladen!')
  })

  eventSource.addEventListener('error', (event) => {
    downloadStatusMessages.value.push('❌ Download fehlgeschlagen')
    eventSource.close()
    showLlamaCppDownloadModal.value = false
    currentDownloadModelId.value = ''
    errorToast('Download fehlgeschlagen')
  })
}

function cancelLlamaCppDownload() {
  if (currentDownloadModelId.value && activeDownloadEventSource.value) {
    api.cancelModelStoreDownload(currentDownloadModelId.value)
    activeDownloadEventSource.value.close()
    showLlamaCppDownloadModal.value = false
    currentDownloadModelId.value = ''
  }
}

async function cancelDownload() {
  if (!downloadModelName.value) return

  try {
    // Show cleanup message
    downloadProgress.value = '🗑️ Räume unvollständigen Download auf...'
    downloadProgressPercent.value = 0

    // Delete the incomplete model
    await api.deleteModel(downloadModelName.value)

    // Reset state
    showDownloadDialog.value = false
    isDownloading.value = false
    downloadModelName.value = ''
    downloadProgressPercent.value = 0

    // Refresh model list
    await loadModels()
  } catch (error) {
    console.error('Failed to cleanup incomplete download:', error)
    // Reset anyway
    showDownloadDialog.value = false
    isDownloading.value = false
    downloadModelName.value = ''
    downloadProgressPercent.value = 0
  }
}

async function startDownload() {
  if (!downloadModelName.value.trim()) return

  isDownloading.value = true
  downloadProgress.value = 'Starte Download...'
  downloadProgressPercent.value = 0

  try {
    await api.pullModel(downloadModelName.value, (progress) => {
      downloadProgress.value = progress

      // Parse progress for percentage
      // Progress format: "downloading 1.5 GB / 4.0 GB (37%)", "verifying... (95%)", "success (100%)"
      const match = progress.match(/(\d+)%/)
      if (match) {
        downloadProgressPercent.value = parseInt(match[1])
      } else if (progress.includes('pulling')) {
        downloadProgressPercent.value = 2
      } else if (progress.includes('downloading')) {
        downloadProgressPercent.value = 10
      } else if (progress.includes('verifying')) {
        downloadProgressPercent.value = 95
      } else if (progress.includes('success')) {
        downloadProgressPercent.value = 100
      }
    })

    // Download completed
    downloadProgress.value = '✅ Download erfolgreich abgeschlossen!'
    downloadProgressPercent.value = 100

    setTimeout(() => {
      showDownloadDialog.value = false
      isDownloading.value = false
      downloadModelName.value = ''
      downloadProgressPercent.value = 0
      loadModels()
    }, 2000)
  } catch (error) {
    console.error('Failed to download model:', error)

    // Check if it was a user cancellation
    if (error.message && error.message.includes('cancel')) {
      downloadProgress.value = '🗑️ Download abgebrochen - Räume auf...'
      // Cleanup incomplete model
      try {
        await api.deleteModel(downloadModelName.value)
      } catch (cleanupError) {
        console.error('Failed to cleanup after cancellation:', cleanupError)
      }
    } else {
      downloadProgress.value = '❌ Fehler beim Download: ' + error.message
    }

    downloadProgressPercent.value = 0
    isDownloading.value = false

    // Close dialog after brief delay
    setTimeout(() => {
      showDownloadDialog.value = false
      downloadModelName.value = ''
      loadModels()
    }, 2000)
  }
}

// formatDate importiert aus useFormatters.js als formatDateLong

/**
 * Format file size from bytes to human-readable format (KB, MB, GB)
 */
function formatSize(sizeString) {
  if (!sizeString) return ''

  // If already formatted (contains "GB" or "MB"), return as is
  if (typeof sizeString === 'string' && (sizeString.includes('GB') || sizeString.includes('MB') || sizeString.includes('KB'))) {
    return sizeString
  }

  // Parse bytes from string (e.g., "4803985408 bytes" or just "4803985408")
  const bytes = typeof sizeString === 'number' ? sizeString : parseInt(sizeString.replace(/[^\d]/g, ''))

  if (isNaN(bytes)) return sizeString

  const gb = bytes / (1024 * 1024 * 1024)
  const mb = bytes / (1024 * 1024)
  const kb = bytes / 1024

  if (gb >= 1) {
    return `${gb.toFixed(2)} GB`
  } else if (mb >= 1) {
    return `${mb.toFixed(2)} MB`
  } else if (kb >= 1) {
    return `${kb.toFixed(2)} KB`
  } else {
    return `${bytes} Bytes`
  }
}

/**
 * Load custom models from database - both Ollama CustomModels AND GgufModelConfigs
 */
async function loadCustomModels() {
  try {
    isLoadingCustom.value = true

    const allCustomModels = []

    // 1. Load Ollama Custom Models (for Ollama provider)
    try {
      const ollamaResponse = await fetch('/api/custom-models')
      if (ollamaResponse.ok) {
        const ollamaModels = await ollamaResponse.json()
        // Mark as Ollama custom models
        ollamaModels.forEach(m => {
          m.providerType = 'ollama'
          m.displayType = 'Ollama Custom Model'
        })
        allCustomModels.push(...ollamaModels)
      }
    } catch (e) {
      console.warn('Could not load Ollama custom models:', e)
    }

    // 2. Load GGUF Model Configs (for java-llama-cpp provider)
    try {
      const ggufConfigs = await api.getAllGgufModelConfigs()
      if (ggufConfigs && ggufConfigs.length > 0) {
        // Mark as GGUF configs and transform to common format
        ggufConfigs.forEach(config => {
          allCustomModels.push({
            id: config.id,
            name: config.name,
            baseModel: config.baseModel,
            description: config.description,
            systemPrompt: config.systemPrompt,
            temperature: config.temperature,
            topP: config.topP,
            topK: config.topK,
            numCtx: config.contextSize,
            createdAt: config.createdAt,
            updatedAt: config.updatedAt,
            providerType: 'java-llama-cpp',
            displayType: 'GGUF Konfiguration',
            isGgufConfig: true
          })
        })
      }
    } catch (e) {
      console.warn('Could not load GGUF model configs:', e)
    }

    customModels.value = allCustomModels
    console.log(`Loaded ${allCustomModels.length} custom model configurations (Ollama + GGUF)`)
  } catch (error) {
    console.error('Failed to load custom models:', error)
    customModels.value = []
  } finally {
    isLoadingCustom.value = false
  }
}

/**
 * View custom model details
 */
function viewCustomModelDetails(model) {
  // TODO: Open details modal with full modelfile info
  console.log('View details:', model)
}

/**
 * Open Edit Custom Model Modal
 */
function openEditCustomModelModal(model) {
  editingCustomModel.value = model
  editForm.value = {
    systemPrompt: model.systemPrompt || '',
    description: model.description || '',
    temperature: model.temperature ?? 0.8,
    topP: model.topP ?? 0.9,
    topK: model.topK ?? 40,
    repeatPenalty: model.repeatPenalty ?? 1.1,
    numPredict: model.numPredict ?? 2048,
    numCtx: model.numCtx ?? 8192
  }
  showEditCustomModel.value = true
}

/**
 * Save Custom Model Edit (creates new version)
 */
async function saveCustomModelEdit() {
  if (!editingCustomModel.value) return

  isUpdatingCustomModel.value = true
  updateProgress.value = 'Aktualisiere Modell...'

  try {
    const request = {
      systemPrompt: editForm.value.systemPrompt,
      description: editForm.value.description,
      temperature: editForm.value.temperature,
      topP: editForm.value.topP,
      topK: editForm.value.topK,
      repeatPenalty: editForm.value.repeatPenalty,
      numPredict: editForm.value.numPredict,
      numCtx: editForm.value.numCtx
    }

    await api.updateCustomModel(editingCustomModel.value.id, request, (progress) => {
      updateProgress.value = progress
    })

    success('Custom Model erfolgreich aktualisiert!')
    showEditCustomModel.value = false
    editingCustomModel.value = null
    await loadCustomModels()
  } catch (err) {
    console.error('Failed to update custom model:', err)
    error('Fehler beim Aktualisieren: ' + err.message)
  } finally {
    isUpdatingCustomModel.value = false
    updateProgress.value = ''
  }
}

/**
 * Delete custom model
 */
async function deleteCustomModel(id, name, isGgufConfig = false) {
  const confirmed = await showDeleteConfirm(name, isGgufConfig
    ? 'Die GGUF-Konfiguration wird gelöscht. Das Basis-Modell bleibt erhalten.'
    : 'Das Custom Model wird aus der Datenbank entfernt.')
  if (!confirmed) return

  try {
    let dbDeleted = false
    let modelDeleted = false

    // Check if this is a detected model (not in database)
    const isDetected = typeof id === 'string' && id.startsWith('detected-')

    if (!isDetected) {
      // Delete from database based on type
      try {
        if (isGgufConfig) {
          // Delete GGUF Model Config
          await api.deleteGgufModelConfig(id)
          dbDeleted = true
        } else {
          // Delete Ollama Custom Model
          const response = await secureFetch(`/api/custom-models/${id}`, {
            method: 'DELETE'
          })
          if (response.ok) {
            dbDeleted = true
          }
        }
      } catch (e) {
        console.error('Failed to delete from database:', e)
      }
    }

    // For Ollama models, try to delete the model itself
    if (!isGgufConfig) {
      try {
        await api.deleteModel(name)
        modelDeleted = true
      } catch (e) {
        console.error('Failed to delete model (model might not exist):', e)
        // Don't throw error - model might not exist anymore
      }
    }

    // Show appropriate success message
    if (isGgufConfig) {
      success(`GGUF Konfiguration "${name}" gelöscht`)
    } else if (dbDeleted && modelDeleted) {
      success(`Custom Model "${name}" vollständig gelöscht`)
    } else if (dbDeleted) {
      success(`Custom Model "${name}" aus Datenbank gelöscht`)
    } else if (modelDeleted) {
      success(`Custom Model "${name}" gelöscht`)
    } else {
      success(`Custom Model "${name}" entfernt`)
    }

    // Reload lists
    loadModels()
    loadCustomModels()
  } catch (error) {
    console.error('Failed to delete custom model:', error)
    errorToast(`Fehler beim Löschen: ${error.message}`)
  }
}

/**
 * Handle custom model created event
 */
async function handleCustomModelCreated() {
  success('Eigenes Modell erfolgreich erstellt!')
  await loadModels() // Reload models list
  await loadLibraryModels() // Reload library to ensure detection works
  await loadCustomModels() // Reload custom models list (will detect the new model)
  activeTab.value = 'installed' // Switch to installed tab
  installedSubTab.value = 'custom' // Switch to custom sub-tab to show the new model
}

/**
 * Check if a model can be used with the current project context
 */
function canUseModel(modelName) {
  // If there's no current chat or no project assigned, all models are usable
  if (!chatStore.currentChat || !chatStore.currentChat.projectName) {
    return true
  }

  // Get project context tokens (we need to fetch this from the project)
  // For now, we'll use a safe approach and check if the project has context
  const projectTokens = chatStore.currentChat.projectTokens || 0

  // If no context, all models are usable
  if (projectTokens === 0) {
    return true
  }

  // Check if model can handle the project context
  return canModelHandleContext(modelName, projectTokens)
}

/**
 * Get incompatibility message for a model
 */
function getIncompatibilityMessage(modelName) {
  if (!chatStore.currentChat || !chatStore.currentChat.projectName) {
    return ''
  }

  const projectTokens = chatStore.currentChat.projectTokens || 0

  if (projectTokens === 0) {
    return ''
  }

  return getIncompatibilityReason(modelName, projectTokens)
}

// ============================================================================
// HUGGINGFACE SEARCH & DISCOVERY
// ============================================================================

async function searchHuggingFace() {
  if (!hfSearchQuery.value.trim()) return

  isSearchingHF.value = true
  try {
    const results = await api.searchHuggingFaceModels(hfSearchQuery.value, 50)
    hfSearchResults.value = results
    console.log('HuggingFace search results:', results.length)
  } catch (error) {
    console.error('HuggingFace search failed:', error)
    alert('❌ Suche fehlgeschlagen: ' + error.message)
  } finally {
    isSearchingHF.value = false
  }
}

async function loadPopularHF() {
  isSearchingHF.value = true
  hfSearchQuery.value = ''
  try {
    const results = await api.getPopularHuggingFaceModels(30)
    hfSearchResults.value = results
    console.log('Popular HuggingFace models:', results.length)
  } catch (error) {
    console.error('Failed to load popular models:', error)
    alert('❌ Laden fehlgeschlagen: ' + error.message)
  } finally {
    isSearchingHF.value = false
  }
}

async function loadGermanHF() {
  isSearchingHF.value = true
  hfSearchQuery.value = ''
  try {
    const results = await api.getGermanHuggingFaceModels(30)
    hfSearchResults.value = results
    console.log('German HuggingFace models:', results.length)
  } catch (error) {
    console.error('Failed to load German models:', error)
    alert('❌ Laden fehlgeschlagen: ' + error.message)
  } finally {
    isSearchingHF.value = false
  }
}

async function loadInstructHF() {
  isSearchingHF.value = true
  hfSearchQuery.value = ''
  try {
    const results = await api.getInstructHuggingFaceModels(30)
    hfSearchResults.value = results
    console.log('Instruct HuggingFace models:', results.length)
  } catch (error) {
    console.error('Failed to load instruct models:', error)
    alert('❌ Laden fehlgeschlagen: ' + error.message)
  } finally {
    isSearchingHF.value = false
  }
}

async function loadCodeHF() {
  isSearchingHF.value = true
  hfSearchQuery.value = ''
  try {
    const results = await api.getCodeHuggingFaceModels(30)
    hfSearchResults.value = results
    console.log('Code HuggingFace models:', results.length)
  } catch (error) {
    console.error('Failed to load code models:', error)
    alert('❌ Laden fehlgeschlagen: ' + error.message)
  } finally {
    isSearchingHF.value = false
  }
}

async function loadVisionHF() {
  isSearchingHF.value = true
  hfSearchQuery.value = ''
  try {
    const results = await api.getVisionHuggingFaceModels(20)
    hfSearchResults.value = results
    console.log('Vision HuggingFace models:', results.length)
  } catch (error) {
    console.error('Failed to load vision models:', error)
    alert('❌ Laden fehlgeschlagen: ' + error.message)
  } finally {
    isSearchingHF.value = false
  }
}

function clearHFSearch() {
  hfSearchQuery.value = ''
  hfSearchResults.value = []
}

async function showHFModelDetails(model) {
  // TODO: Show detailed modal with full README, files, etc.
  console.log('Show details for:', model)
  alert(`📄 Details für: ${model.displayName || model.name}\n\nDownloads: ${formatDownloads(model.downloads)}\nLikes: ${model.likes}\nLizenz: ${model.license || 'N/A'}\n\nErstellungsdatum: ${model.createdAt ? new Date(model.createdAt).toLocaleDateString('de-DE') : 'N/A'}\nLetztes Update: ${model.lastModified ? new Date(model.lastModified).toLocaleDateString('de-DE') : 'N/A'}`)
}

async function downloadHFModel(model) {
  // Check if this is a Model Store model (curated models from ModelRegistry)
  // Model Store models have an 'id' property and come from our curated list
  if (model.id && model.filename && !model.siblings) {
    console.log('Detected Model Store model:', model.id)
    await downloadModelStoreModel(model)
    return
  }

  // If siblings are missing (from search/german/popular endpoints), load details first
  if (!model.siblings || model.siblings.length === 0) {
    console.log('Loading model details for:', model.modelId || model.id)
    try {
      const detailsResponse = await api.getHuggingFaceModelDetails(model.modelId || model.id)
      if (detailsResponse && detailsResponse.siblings) {
        model.siblings = detailsResponse.siblings
        console.log(`Loaded ${model.siblings.length} files for model`)
      } else {
        alert('❌ Keine Dateien gefunden für dieses Modell')
        return
      }
    } catch (error) {
      console.error('Failed to load model details:', error)
      alert('❌ Fehler beim Laden der Modelldetails: ' + error.message)
      return
    }
  }

  // For HuggingFace models, we need to select which file to download
  if (model.siblings && model.siblings.length > 0) {
    const ggufFiles = model.siblings.filter(f => f.toLowerCase().endsWith('.gguf'))
    if (ggufFiles.length === 0) {
      alert('❌ Keine GGUF-Dateien gefunden für dieses Modell')
      return
    }

    if (ggufFiles.length === 1) {
      // Only one file - download directly
      confirmAndDownloadHFFile(model, ggufFiles[0])
    } else {
      // Multiple files - show modal to let user choose
      fileSelectionModel.value = model
      fileSelectionFiles.value = ggufFiles
      selectedFileIndex.value = 0
      showFileSelectionModal.value = true
    }
  } else {
    alert('❌ Keine Dateien gefunden für dieses Modell')
  }
}

async function downloadModelStoreModel(model) {
  // Prevent multiple downloads
  if (showLlamaCppDownloadModal.value) {
    alert('⚠️ Es läuft bereits ein Download. Bitte warte, bis dieser abgeschlossen ist.')
    return
  }

  console.log('Starting Model Store download for:', model.id)

  // Show download modal
  showLlamaCppDownloadModal.value = true
  currentDownloadModel.value = model.displayName
  downloadProgress.value = 0
  downloadedSize.value = '0 MB'
  totalSize.value = model.sizeHuman || 'N/A'
  downloadSpeed.value = '0.0'
  downloadStatusMessages.value = ['📥 Starte Download...']

  // Create EventSource for SSE
  const eventSource = new EventSource(`/api/model-store/download/${model.id}`)

  eventSource.addEventListener('progress', (event) => {
    const message = event.data
    console.log('Progress:', message)

    // Add to status messages
    downloadStatusMessages.value.push(message)
    if (downloadStatusMessages.value.length > 10) {
      downloadStatusMessages.value.shift() // Keep last 10 messages
    }

    // Parse progress from message like "⬇️ 45% - 1.2 GB / 2.0 GB - 5.3 MB/s"
    const percentMatch = message.match(/(\d+)%/)
    const downloadedMatch = message.match(/([\d.]+\s+[GM]B)\s+\//)
    const totalMatch = message.match(/\/\s+([\d.]+\s+[GM]B)/)
    const speedMatch = message.match(/([\d.]+)\s+MB\/s/)

    if (percentMatch) {
      downloadProgress.value = parseInt(percentMatch[1])
    }
    if (downloadedMatch) {
      downloadedSize.value = downloadedMatch[1]
    }
    if (totalMatch) {
      totalSize.value = totalMatch[1]
    }
    if (speedMatch) {
      downloadSpeed.value = parseFloat(speedMatch[1]).toFixed(1)
    }
  })

  eventSource.addEventListener('complete', (event) => {
    console.log('Download complete:', event.data)
    downloadStatusMessages.value.push('✅ ' + event.data)

    eventSource.close()

    setTimeout(() => {
      showLlamaCppDownloadModal.value = false
      downloadProgress.value = 0
      downloadStatusMessages.value = []

      // Refresh installed models
      loadInstalledModels()

      alert('✅ Download erfolgreich!\n\nModell: ' + model.displayName + '\n\nDas Modell ist jetzt verfügbar.')
    }, 2000)
  })

  eventSource.addEventListener('error', (event) => {
    console.error('Download error:', event)

    let errorMessage = 'Unbekannter Fehler'
    if (event.data) {
      try {
        errorMessage = event.data
      } catch (e) {
        errorMessage = 'Download fehlgeschlagen'
      }
    }

    downloadStatusMessages.value.push('❌ ' + errorMessage)
    eventSource.close()

    setTimeout(() => {
      showLlamaCppDownloadModal.value = false
      alert('❌ Download fehlgeschlagen!\n\n' + errorMessage)
    }, 2000)
  })

  eventSource.onerror = (error) => {
    console.error('EventSource error:', error)
    eventSource.close()

    setTimeout(() => {
      showLlamaCppDownloadModal.value = false
      alert('❌ Download-Verbindung unterbrochen!')
    }, 1000)
  }
}

async function confirmAndDownloadHFFile(model, filename) {
  // Prevent multiple downloads
  if (showLlamaCppDownloadModal.value) {
    alert('⚠️ Es läuft bereits ein Download. Bitte warte, bis dieser abgeschlossen ist.')
    return
  }

  const confirmed = confirm(`Möchtest du "${filename}" herunterladen?\n\nModell: ${model.displayName || model.name}\nAutor: ${model.author}\n\nDieser Download kann lange dauern!`)
  if (!confirmed) {
    return
  }

  console.log('Starting HuggingFace download:', model.modelId, filename)

  // Show download modal
  showLlamaCppDownloadModal.value = true
  currentDownloadModel.value = filename
  downloadStatusMessages.value = []
  downloadStatusMessages.value.push('🚀 Starte HuggingFace Download...')

  // Create EventSource for Server-Sent Events (same as Registry download)
  const eventSource = new EventSource(
    `/api/model-store/huggingface/download?modelId=${encodeURIComponent(model.modelId)}&filename=${encodeURIComponent(filename)}`
  )
  activeDownloadEventSource.value = eventSource

  eventSource.addEventListener('progress', (event) => {
    const message = event.data
    downloadStatusMessages.value.push(message)

    // Parse progress (same format as registry download)
    const percentMatch = message.match(/(\d+)%/)
    const downloadedMatch = message.match(/([\d.]+\s+[GM]B)\s+\//)
    const totalMatch = message.match(/\/\s+([\d.]+\s+[GM]B)/)
    const speedMatch = message.match(/([\d.]+)\s+MB\/s/)

    if (percentMatch) currentDownloadProgress.value = parseInt(percentMatch[1])
    if (downloadedMatch) currentDownloadedSize.value = downloadedMatch[1]
    if (totalMatch) currentTotalSize.value = totalMatch[1]
    if (speedMatch) currentSpeed.value = parseFloat(speedMatch[1]).toFixed(1)
  })

  eventSource.addEventListener('complete', (event) => {
    downloadStatusMessages.value.push('✅ ' + event.data)
    eventSource.close()

    // Reload models
    loadModels()

    setTimeout(() => {
      showLlamaCppDownloadModal.value = false
      currentDownloadModelId.value = ''
    }, 2000)

    success('Modell heruntergeladen!')
  })

  eventSource.addEventListener('error', (event) => {
    downloadStatusMessages.value.push('❌ Download fehlgeschlagen')
    eventSource.close()
    showLlamaCppDownloadModal.value = false
    currentDownloadModelId.value = ''
    errorToast('Download fehlgeschlagen')
  })
}

// File Selection Modal Handlers
function confirmFileSelection() {
  if (fileSelectionModel.value && fileSelectionFiles.value.length > 0) {
    const selectedFile = fileSelectionFiles.value[selectedFileIndex.value]
    confirmAndDownloadHFFile(fileSelectionModel.value, selectedFile)
    closeFileSelectionModal()
  }
}

function closeFileSelectionModal() {
  showFileSelectionModal.value = false
  fileSelectionModel.value = null
  fileSelectionFiles.value = []
  selectedFileIndex.value = 0
}

function formatDownloads(downloads) {
  if (!downloads) return '0'
  if (downloads >= 1000000) return `${(downloads / 1000000).toFixed(1)}M`
  if (downloads >= 1000) return `${(downloads / 1000).toFixed(1)}K`
  return downloads.toString()
}

// GGUF Model Config Modal Functions
async function openGgufConfigModal(modelName) {
  try {
    // Try to load existing config for this model
    try {
      const config = await api.getGgufModelConfigByName(modelName)
      currentGgufConfig.value = config
    } catch (e) {
      // No existing config - create new one with this base model pre-selected
      currentGgufConfig.value = null
    }
    showGgufConfigModal.value = true
  } catch (error) {
    console.error('Failed to load GGUF model config:', error)
    errorToast('Fehler beim Laden der Konfiguration')
  }
}

function closeGgufConfigModal() {
  showGgufConfigModal.value = false
  currentGgufConfig.value = null
}

async function handleGgufConfigSaved() {
  success('GGUF Model-Konfiguration gespeichert')
  closeGgufConfigModal()
  // Refresh models to show updated configs
  await refreshModels()
}
</script>

<style scoped>
@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

.animate-shimmer {
  animation: shimmer 2s infinite;
}

/* Modal Transition */
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

/* Fade Transition */
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
