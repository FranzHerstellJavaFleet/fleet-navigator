<template>
  <Transition name="modal">
    <div class="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div class="
        bg-white/90 dark:bg-gray-800/90
        backdrop-blur-xl backdrop-saturate-150
        rounded-2xl shadow-2xl
        w-full max-w-5xl max-h-[90vh]
        border border-gray-200/50 dark:border-gray-700/50
        flex flex-col
        transform transition-all duration-300
      ">
        <!-- Header with Gradient -->
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

        <!-- Main Tabs -->
        <div class="border-b border-gray-200/50 dark:border-gray-700/50 bg-gray-50/50 dark:bg-gray-900/50">
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
            <button
              @click="installedSubTab = 'coder'"
              class="
                px-4 py-2 font-medium text-sm transition-all duration-200
                flex items-center gap-2
                relative
              "
              :class="installedSubTab === 'coder'
                ? 'text-blue-600 dark:text-blue-400'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            >
              <CpuChipIcon class="w-4 h-4" />
              <span>Coder Modelle</span>
              <div v-if="installedSubTab === 'coder'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-500"></div>
            </button>
            <button
              @click="installedSubTab = 'vision'"
              class="
                px-4 py-2 font-medium text-sm transition-all duration-200
                flex items-center gap-2
                relative
              "
              :class="installedSubTab === 'vision'
                ? 'text-green-600 dark:text-green-400'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
              <span>Vision Modelle</span>
              <div v-if="installedSubTab === 'vision'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-green-500"></div>
            </button>
            <button
              @click="installedSubTab = 'general'"
              class="
                px-4 py-2 font-medium text-sm transition-all duration-200
                flex items-center gap-2
                relative
              "
              :class="installedSubTab === 'general'
                ? 'text-orange-600 dark:text-orange-400'
                : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300'"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              <span>Allgemeine Modelle</span>
              <div v-if="installedSubTab === 'general'" class="absolute bottom-0 left-0 right-0 h-0.5 bg-orange-500"></div>
            </button>
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

            <!-- Coder Models Sub-Tab -->
            <div v-else-if="installedSubTab === 'coder'" class="flex gap-3">
              <div class="flex-1 relative">
                <MagnifyingGlassIcon class="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  v-model="coderSearchQuery"
                  type="text"
                  placeholder="Coder Modelle durchsuchen..."
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

            <!-- Vision Models Sub-Tab -->
            <div v-else-if="installedSubTab === 'vision'" class="flex gap-3">
              <div class="flex-1 relative">
                <MagnifyingGlassIcon class="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  v-model="visionSearchQuery"
                  type="text"
                  placeholder="Vision Modelle durchsuchen..."
                  class="
                    w-full pl-10 pr-4 py-2.5 rounded-xl
                    border border-gray-300 dark:border-gray-600
                    bg-white dark:bg-gray-700
                    text-gray-900 dark:text-white
                    placeholder-gray-400 dark:placeholder-gray-500
                    focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent
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

            <!-- General Models Sub-Tab -->
            <div v-else-if="installedSubTab === 'general'" class="flex gap-3">
              <div class="flex-1 relative">
                <MagnifyingGlassIcon class="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  v-model="generalSearchQuery"
                  type="text"
                  placeholder="Allgemeine Modelle durchsuchen..."
                  class="
                    w-full pl-10 pr-4 py-2.5 rounded-xl
                    border border-gray-300 dark:border-gray-600
                    bg-white dark:bg-gray-700
                    text-gray-900 dark:text-white
                    placeholder-gray-400 dark:placeholder-gray-500
                    focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent
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

          <!-- Verfügbare Modelle: Search only -->
          <div v-else-if="activeTab === 'available'" class="p-4 border-b border-gray-200/50 dark:border-gray-700/50">
            <div class="relative">
              <MagnifyingGlassIcon class="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                v-model="searchQuery"
                type="text"
                placeholder="Verfügbare Modelle durchsuchen..."
                class="
                  w-full pl-10 pr-4 py-3 rounded-xl
                  border border-gray-300 dark:border-gray-600
                  bg-white dark:bg-gray-700
                  text-gray-900 dark:text-white
                  placeholder-gray-400 dark:placeholder-gray-500
                  focus:outline-none focus:ring-2 focus:ring-fleet-orange-500 focus:border-transparent
                  transition-all duration-200
                "
              />
            </div>
          </div>
        </Transition>

      <!-- Installed Models List - Shown based on sub-tab -->
      <div v-if="activeTab === 'installed'" class="flex-1 overflow-y-auto p-6">
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
              :key="model.id"
              class="bg-gradient-to-br from-indigo-50 to-purple-50 dark:from-indigo-900/20 dark:to-purple-900/20 rounded-lg p-4 border border-indigo-200/50 dark:border-indigo-700/50"
            >
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <div class="flex items-center gap-3 mb-2">
                    <SparklesIcon class="w-5 h-5 text-indigo-500" />
                    <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
                      {{ model.name }}
                    </h3>
                  </div>

                  <div class="space-y-1 text-sm text-gray-600 dark:text-gray-400">
                    <p v-if="model.description">{{ model.description }}</p>
                    <p><span class="font-medium">Basis-Modell:</span> {{ model.baseModel }}</p>
                    <p v-if="model.systemPrompt" class="text-xs">
                      <span class="font-medium">System Prompt:</span>
                      <span class="line-clamp-2">{{ model.systemPrompt }}</span>
                    </p>
                    <p class="text-xs text-gray-500 dark:text-gray-500">
                      Erstellt: {{ formatDate(model.createdAt) }}
                    </p>
                  </div>
                </div>

                <div class="flex flex-col gap-2 ml-4 min-w-[200px]">
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
                    @click="deleteCustomModel(model.id, model.name)"
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

        <!-- Coder Models Sub-Tab -->
        <div v-else-if="installedSubTab === 'coder'">
          <div v-if="coderModels.length === 0" class="text-center py-12 text-gray-500 dark:text-gray-400">
            <CpuChipIcon class="w-16 h-16 mx-auto text-blue-400 dark:text-blue-600 mb-4" />
            <p v-if="coderSearchQuery">Keine Coder Modelle gefunden für: "{{ coderSearchQuery }}"</p>
            <p v-else>Keine Coder Modelle installiert</p>
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="model in coderModels"
              :key="model.name"
              class="bg-blue-50 dark:bg-blue-900/20 rounded-lg p-4 border border-blue-200/50 dark:border-blue-700/50 transition-colors hover:bg-blue-100 dark:hover:bg-blue-900/30"
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
                    <div><strong>Größe:</strong> {{ model.size }}</div>
                    <div v-if="model.description"><strong>Beschreibung:</strong> {{ model.description }}</div>
                    <div v-if="model.specialties"><strong>Spezialitäten:</strong> {{ model.specialties }}</div>
                  </div>
                </div>

                <div class="flex flex-col gap-2 ml-4 min-w-[200px]">
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

        <!-- Vision Models Sub-Tab -->
        <div v-else-if="installedSubTab === 'vision'">
          <div v-if="visionModels.length === 0" class="text-center py-12 text-gray-500 dark:text-gray-400">
            <svg class="w-16 h-16 mx-auto text-green-400 dark:text-green-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            </svg>
            <p v-if="visionSearchQuery">Keine Vision Modelle gefunden für: "{{ visionSearchQuery }}"</p>
            <p v-else>Keine Vision Modelle installiert</p>
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="model in visionModels"
              :key="model.name"
              class="bg-green-50 dark:bg-green-900/20 rounded-lg p-4 border border-green-200/50 dark:border-green-700/50 transition-colors hover:bg-green-100 dark:hover:bg-green-900/30"
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
                    <div><strong>Größe:</strong> {{ model.size }}</div>
                    <div v-if="model.description"><strong>Beschreibung:</strong> {{ model.description }}</div>
                    <div v-if="model.specialties"><strong>Spezialitäten:</strong> {{ model.specialties }}</div>
                  </div>
                </div>

                <div class="flex flex-col gap-2 ml-4 min-w-[200px]">
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

        <!-- General Models Sub-Tab -->
        <div v-else-if="installedSubTab === 'general'">
          <div v-if="generalModels.length === 0" class="text-center py-12 text-gray-500 dark:text-gray-400">
            <svg class="w-16 h-16 mx-auto text-orange-400 dark:text-orange-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            <p v-if="generalSearchQuery">Keine allgemeinen Modelle gefunden für: "{{ generalSearchQuery }}"</p>
            <p v-else>Keine allgemeinen Modelle installiert</p>
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="model in generalModels"
              :key="model.name"
              class="bg-orange-50 dark:bg-orange-900/20 rounded-lg p-4 border border-orange-200/50 dark:border-orange-700/50 transition-colors hover:bg-orange-100 dark:hover:bg-orange-900/30"
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
                    <div><strong>Größe:</strong> {{ model.size }}</div>
                    <div v-if="model.description"><strong>Beschreibung:</strong> {{ model.description }}</div>
                    <div v-if="model.specialties"><strong>Spezialitäten:</strong> {{ model.specialties }}</div>
                  </div>
                </div>

                <div class="flex flex-col gap-2 ml-4 min-w-[200px]">
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
      </div>

      <!-- Available Models List -->
      <div v-else-if="activeTab === 'available'" class="flex-1 overflow-y-auto p-6">
        <div v-if="isLoadingLibrary" class="flex justify-center items-center py-8">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-fleet-orange-500"></div>
          <span class="ml-3 text-gray-600 dark:text-gray-400">Lade alle Ollama-Modelle...</span>
        </div>

        <div v-else class="space-y-4">
          <div class="bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-700 rounded-lg p-3 mb-4">
            <p class="text-sm text-blue-800 dark:text-blue-200">
              📚 <strong>{{ availableModels.length }} Modelle</strong> aus der Ollama Library verfügbar
            </p>
          </div>

          <!-- Filtered Models -->
          <div class="space-y-3">
            <div
              v-for="model in filteredAvailableModels"
              :key="model.name"
              class="bg-gray-50 dark:bg-gray-700 rounded-lg p-4 hover:bg-gray-100 dark:hover:bg-gray-650 transition-colors"
            >
              <div class="flex items-start justify-between">
                <!-- Model Info -->
                <div class="flex-1">
                  <div class="flex items-center gap-3 mb-2">
                    <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
                      {{ model.name }}
                    </h3>
                    <span
                      v-if="isInstalled(model.name)"
                      class="px-2 py-1 bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200 text-xs rounded-full"
                    >
                      ✓ Installiert
                    </span>
                  </div>

                  <div class="text-sm text-gray-600 dark:text-gray-400 space-y-1">
                    <div><strong>Größe:</strong> {{ model.size }}</div>
                    <div v-if="model.modifiedAt"><strong>Aktualisiert:</strong> {{ formatDate(model.modifiedAt) }}</div>
                  </div>
                </div>

                <!-- Actions -->
                <div class="flex flex-col gap-2 ml-4">
                  <button
                    v-if="!isInstalled(model.name)"
                    @click="downloadFromLibrary(model.name)"
                    class="px-3 py-1 bg-fleet-orange-500 hover:bg-fleet-orange-600 text-white text-sm rounded transition-colors whitespace-nowrap"
                  >
                    ⬇ Download
                  </button>
                  <span
                    v-else
                    class="px-3 py-1 bg-gray-300 dark:bg-gray-600 text-gray-600 dark:text-gray-400 text-sm rounded text-center"
                  >
                    Installiert
                  </span>
                </div>
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
            <p class="text-gray-600 dark:text-gray-400">{{ formatDate(selectedModelDetails.releaseDate) }}</p>
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
  </Transition>

  <!-- Create Custom Model Modal -->
  <CreateCustomModelModal
    :show="showCreateCustomModel"
    @close="showCreateCustomModel = false"
    @created="handleCustomModelCreated"
  />
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
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
  CloudArrowDownIcon
} from '@heroicons/vue/24/outline'
import api from '../services/api'
import { useChatStore } from '../stores/chatStore'
import { useToast } from '../composables/useToast'
import { canModelHandleContext, getIncompatibilityReason, getSafeContextLimit } from '../utils/modelContextWindows'
import CreateCustomModelModal from './CreateCustomModelModal.vue'

const { success } = useToast()

defineEmits(['close'])

const chatStore = useChatStore()
const models = ref([])
const isLoading = ref(false)

// Custom Model Modal
const showCreateCustomModel = ref(false)

// Tabs
const activeTab = ref('installed')
const installedSubTab = ref('custom')  // Sub-tab for installed models: custom, coder, vision, general
const searchQuery = ref('')  // For available models
const installedSearchQuery = ref('')  // For installed models
const customSearchQuery = ref('')  // For custom models (in installed sub-tabs)
const coderSearchQuery = ref('')  // For coder models
const visionSearchQuery = ref('')  // For vision models
const generalSearchQuery = ref('')  // For general models
const selectedCategory = ref('Alle')

// Custom Models
const customModels = ref([])
const isLoadingCustom = ref(false)

// Categories
const categories = ['Alle', 'Code', 'Chat', 'Vision', 'Embedding']

// Real Ollama Library models (loaded from API)
const availableModels = ref([])
const isLoadingLibrary = ref(false)

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
const filteredCustomModels = computed(() => {
  if (!customSearchQuery.value.trim()) {
    return customModels.value
  }
  const query = customSearchQuery.value.toLowerCase()
  return customModels.value.filter(m =>
    m.name.toLowerCase().includes(query) ||
    m.description?.toLowerCase().includes(query) ||
    m.baseModel?.toLowerCase().includes(query)
  )
})

// Helper function: Determine model category
function getModelCategory(modelName) {
  const name = modelName.toLowerCase()

  // Custom models (not in official list)
  const knownOfficialModels = [
    'llama2', 'llama3', 'llama3.1', 'llama3.2', 'llama3.3',
    'mistral', 'mixtral',
    'phi', 'phi3',
    'gemma', 'gemma2',
    'codellama',
    'qwen', 'qwen2', 'qwen2.5', 'qwen3',
    'qwen-coder', 'qwen2.5-coder', 'qwen3-coder',
    'deepseek-coder', 'deepseek-coder-v2',
    'deepseek-r1',
    'llava',
    'moondream',
    'bge-m3',
    'nomic-embed-text',
    'mxbai-embed-large',
    'all-minilm'
  ]

  const baseName = name.split(':')[0]
  const isOfficial = knownOfficialModels.includes(baseName)

  if (!isOfficial) {
    return 'custom'  // Eigene Modelle
  }

  // Vision models
  if (name.includes('llava') || name.includes('moondream') || name.includes('vision') || name.includes('-vl')) {
    return 'vision'
  }

  // Coder models
  if (name.includes('coder') || name.includes('codellama') || name.includes('code')) {
    return 'coder'
  }

  // General chat models (everything else)
  return 'general'
}

// Category-specific filtered models
const coderModels = computed(() => {
  const filtered = models.value.filter(m => getModelCategory(m.name) === 'coder')
  if (!coderSearchQuery.value.trim()) return filtered

  const query = coderSearchQuery.value.toLowerCase()
  return filtered.filter(m =>
    m.name.toLowerCase().includes(query) ||
    m.description?.toLowerCase().includes(query)
  )
})

const visionModels = computed(() => {
  const filtered = models.value.filter(m => getModelCategory(m.name) === 'vision')
  if (!visionSearchQuery.value.trim()) return filtered

  const query = visionSearchQuery.value.toLowerCase()
  return filtered.filter(m =>
    m.name.toLowerCase().includes(query) ||
    m.description?.toLowerCase().includes(query)
  )
})

const generalModels = computed(() => {
  const filtered = models.value.filter(m => getModelCategory(m.name) === 'general')
  if (!generalSearchQuery.value.trim()) return filtered

  const query = generalSearchQuery.value.toLowerCase()
  return filtered.filter(m =>
    m.name.toLowerCase().includes(query) ||
    m.description?.toLowerCase().includes(query)
  )
})

// Filtered available models based on search only
const filteredAvailableModels = computed(() => {
  let filtered = availableModels.value

  // Filter by search query
  if (searchQuery.value.trim()) {
    const query = searchQuery.value.toLowerCase()
    filtered = filtered.filter(m =>
      m.name.toLowerCase().includes(query) ||
      m.model?.toLowerCase().includes(query)
    )
  }

  // Sort by size (largest first)
  return filtered.sort((a, b) => (b.sizeBytes || 0) - (a.sizeBytes || 0))
})

// Check if model is already installed
function isInstalled(modelName) {
  return models.value.some(m => m.name === modelName)
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

onMounted(async () => {
  await loadModels()
  await loadLibraryModels()  // Load library first to know which models are official
  await loadCustomModels()   // Then detect custom models
})

async function loadModels() {
  isLoading.value = true
  try {
    models.value = await api.getAvailableModels()
  } catch (error) {
    console.error('Failed to load models:', error)
  } finally {
    isLoading.value = false
  }
}

async function loadLibraryModels() {
  isLoadingLibrary.value = true
  try {
    console.log('Loading Ollama Library models...')
    availableModels.value = await api.getOllamaLibraryModels()
    console.log(`Loaded ${availableModels.value.length} models from Ollama Library`)
  } catch (error) {
    console.error('Failed to load Ollama Library models:', error)
  } finally {
    isLoadingLibrary.value = false
  }
}

async function refreshModels() {
  await loadModels()
  await loadLibraryModels()
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

function downloadFromLibrary(modelName) {
  downloadModelName.value = modelName
  showDownloadDialog.value = true
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
      // Ollama progress format: "pulling manifest", "downloading sha256:...", "verifying sha256:...", etc.
      if (progress.includes('pulling')) {
        downloadProgressPercent.value = 5
      } else if (progress.includes('downloading')) {
        // Try to extract percentage from progress string
        const match = progress.match(/(\d+)%/)
        if (match) {
          downloadProgressPercent.value = Math.min(90, parseInt(match[1]))
        } else {
          downloadProgressPercent.value = 50
        }
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

function formatDate(dateString) {
  if (!dateString) return ''
  try {
    const date = new Date(dateString)
    return date.toLocaleDateString('de-DE', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    })
  } catch (e) {
    return dateString
  }
}

/**
 * Load custom models from database AND detect custom models from Ollama
 */
async function loadCustomModels() {
  try {
    isLoadingCustom.value = true

    // Load custom models from database
    const response = await fetch('/api/custom-models')
    if (!response.ok) throw new Error('Failed to load custom models')
    const dbCustomModels = await response.json()

    // Whitelist of known official Ollama models (including older versions)
    // This prevents false positives for legacy models no longer in the library
    const knownOfficialModels = [
      'llama2', 'llama3', 'llama3.1', 'llama3.2', 'llama3.3',
      'mistral', 'mixtral',
      'phi', 'phi3',
      'gemma', 'gemma2',
      'codellama',
      'qwen', 'qwen2', 'qwen2.5', 'qwen3',  // All qwen versions
      'qwen-coder', 'qwen2.5-coder', 'qwen3-coder',
      'deepseek-coder', 'deepseek-coder-v2',
      'deepseek-r1',
      'llava',
      'moondream',
      'bge-m3',
      'nomic-embed-text',
      'mxbai-embed-large',
      'all-minilm'
    ]

    // Detect custom models from installed Ollama models
    // A model is considered "custom" if:
    // 1. It's not in the official Ollama library (availableModels), AND
    // 2. It's not in the known official models whitelist, AND
    // 3. It has a custom name format (e.g., "nova:latest", "mymodel:v1")
    const detectedCustomModels = []

    // Get list of official model base names from Ollama library
    const officialModelNames = availableModels.value.map(m => {
      // Extract base name without tag (e.g., "llama2" from "llama2:latest")
      return m.name.split(':')[0]
    })

    // Combine library models with known official models
    const allOfficialNames = [...new Set([...officialModelNames, ...knownOfficialModels])]

    for (const installedModel of models.value) {
      const baseName = installedModel.name.split(':')[0]

      // Check if this model is in the official list or whitelist
      const isOfficial = allOfficialNames.includes(baseName)

      // Check if already in database custom models
      const isInDatabase = dbCustomModels.some(dm => dm.name === installedModel.name)

      if (!isOfficial && !isInDatabase) {
        // This is a custom model not in our database
        detectedCustomModels.push({
          id: `detected-${installedModel.name}`,
          name: installedModel.name,
          baseModel: 'Unknown',
          description: installedModel.description || 'Custom Model',
          size: installedModel.size,
          modifiedAt: installedModel.modifiedAt,
          isDetected: true  // Flag to indicate it was auto-detected
        })
      }
    }

    // Combine database custom models with detected ones
    customModels.value = [...dbCustomModels, ...detectedCustomModels]

    console.log(`Loaded ${dbCustomModels.length} custom models from database and detected ${detectedCustomModels.length} additional custom models`)
  } catch (error) {
    console.error('Failed to load custom models:', error)
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
 * Delete custom model
 */
async function deleteCustomModel(id, name) {
  if (!confirm(`Möchten Sie das Custom Model "${name}" wirklich löschen?`)) return

  try {
    let dbDeleted = false
    let ollamaDeleted = false

    // Check if this is a detected model (not in database)
    const isDetected = typeof id === 'string' && id.startsWith('detected-')

    if (!isDetected) {
      // Delete from database
      try {
        const response = await fetch(`/api/custom-models/${id}`, {
          method: 'DELETE'
        })
        if (response.ok) {
          dbDeleted = true
        }
      } catch (e) {
        console.error('Failed to delete from database:', e)
      }
    }

    // Try to delete from Ollama (ignore error if model doesn't exist)
    try {
      await api.deleteModel(name)
      ollamaDeleted = true
    } catch (e) {
      console.error('Failed to delete from Ollama (model might not exist):', e)
      // Don't throw error - model might not exist in Ollama anymore
    }

    // Show appropriate success message
    if (dbDeleted && ollamaDeleted) {
      success(`Custom Model "${name}" vollständig gelöscht`)
    } else if (dbDeleted) {
      success(`Custom Model "${name}" aus Datenbank gelöscht (war nicht in Ollama vorhanden)`)
    } else if (ollamaDeleted) {
      success(`Custom Model "${name}" aus Ollama gelöscht`)
    } else {
      success(`Custom Model "${name}" entfernt`)
    }

    // Reload lists
    loadModels()
    loadCustomModels()
  } catch (error) {
    console.error('Failed to delete custom model:', error)
    alert(`Fehler beim Löschen: ${error.message}`)
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
