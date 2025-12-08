<template>
  <!-- Update-Banner (oben auf der Seite) -->
  <Transition name="slide-down">
    <div v-if="showBanner" class="update-banner">
      <div class="update-content">
        <div class="update-icon">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
        </div>
        <div class="update-text">
          <span class="update-title">Update verfügbar!</span>
          <span class="update-version">
            Version {{ latestVersion }} ist bereit
            <span v-if="releaseName">({{ releaseName }})</span>
          </span>
        </div>
        <div class="update-actions">
          <button
            v-if="!downloadInProgress && !downloadComplete"
            @click="startDownload"
            class="btn-update"
          >
            Jetzt aktualisieren
          </button>
          <button
            v-else-if="downloadInProgress"
            class="btn-progress"
            disabled
          >
            {{ downloadProgress }}
          </button>
          <button
            v-else-if="downloadComplete"
            @click="installAndRestart"
            class="btn-install"
          >
            Installieren & Neustarten
          </button>
          <button @click="dismissBanner" class="btn-dismiss" title="Später erinnern">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </Transition>

  <!-- Update-Modal für Details -->
  <Transition name="fade">
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="update-modal">
        <div class="modal-header">
          <h2>Update auf Version {{ latestVersion }}</h2>
          <button @click="closeModal" class="close-btn">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/>
              <line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>

        <div class="modal-body">
          <div class="version-info">
            <div class="version-badge current">
              <span class="label">Aktuell</span>
              <span class="version">v{{ currentVersion }}</span>
            </div>
            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="arrow">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
            <div class="version-badge new">
              <span class="label">Neu</span>
              <span class="version">v{{ latestVersion }}</span>
            </div>
          </div>

          <div v-if="releaseNotes" class="release-notes">
            <h3>Release Notes</h3>
            <div class="notes-content" v-html="formattedReleaseNotes"></div>
          </div>

          <div v-if="downloadSize" class="download-info">
            <span>Download-Größe: {{ formatSize(downloadSize) }}</span>
          </div>

          <div v-if="downloadInProgress" class="progress-section">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            </div>
            <span class="progress-text">{{ downloadProgress }}</span>
          </div>

          <div v-if="error" class="error-message">
            {{ error }}
          </div>
        </div>

        <div class="modal-footer">
          <a v-if="releaseUrl" :href="releaseUrl" target="_blank" class="btn-secondary">
            Auf GitHub ansehen
          </a>
          <button
            v-if="!downloadInProgress && !downloadComplete"
            @click="startDownload"
            class="btn-primary"
          >
            Download starten
          </button>
          <button
            v-else-if="downloadComplete"
            @click="installAndRestart"
            class="btn-primary"
          >
            Installieren & Neustarten
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api from '../services/api'
import { marked } from 'marked'

// State
const showBanner = ref(false)
const showModal = ref(false)
const currentVersion = ref('')
const latestVersion = ref('')
const releaseName = ref('')
const releaseNotes = ref('')
const releaseUrl = ref('')
const downloadSize = ref(0)
const downloadInProgress = ref(false)
const downloadProgress = ref('')
const downloadComplete = ref(false)
const error = ref('')

let pollInterval = null
let checkInterval = null

// Computed
const formattedReleaseNotes = computed(() => {
  if (!releaseNotes.value) return ''
  return marked(releaseNotes.value)
})

const progressPercent = computed(() => {
  const match = downloadProgress.value.match(/(\d+)%/)
  return match ? parseInt(match[1]) : 0
})

// Methods
function formatSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function checkForUpdates() {
  try {
    const status = await api.getUpdateStatus()
    currentVersion.value = status.currentVersion

    if (status.updateAvailable) {
      latestVersion.value = status.latestVersion
      releaseName.value = status.releaseName
      releaseNotes.value = status.releaseNotes
      releaseUrl.value = status.releaseUrl
      downloadSize.value = status.downloadSize
      downloadInProgress.value = status.downloadInProgress
      downloadProgress.value = status.downloadProgress

      // Banner nur zeigen wenn nicht dismissed
      const dismissedUntil = localStorage.getItem('update-dismissed-until')
      if (!dismissedUntil || new Date() > new Date(dismissedUntil)) {
        showBanner.value = true
      }
    } else {
      showBanner.value = false
    }
  } catch (err) {
    console.warn('Update-Check fehlgeschlagen:', err)
  }
}

async function startDownload() {
  error.value = ''
  downloadInProgress.value = true
  showModal.value = true

  try {
    // Start download
    const result = await api.downloadUpdate()

    if (result.success) {
      downloadComplete.value = true
      downloadProgress.value = 'Download abgeschlossen'
    } else {
      error.value = result.message
      downloadInProgress.value = false
    }
  } catch (err) {
    error.value = err.message || 'Download fehlgeschlagen'
    downloadInProgress.value = false
  }
}

async function pollProgress() {
  if (!downloadInProgress.value) return

  try {
    const progress = await api.getUpdateProgress()
    downloadProgress.value = progress.progress
    downloadInProgress.value = progress.inProgress

    if (!progress.inProgress && downloadProgress.value.includes('abgeschlossen')) {
      downloadComplete.value = true
    }
  } catch (err) {
    console.warn('Progress-Polling fehlgeschlagen:', err)
  }
}

async function installAndRestart() {
  try {
    const result = await api.installUpdate()

    if (result.success && result.restartRequired) {
      // Zeige Neustart-Hinweis
      alert('Update installiert! Die Anwendung wird jetzt neu gestartet...')
      // Reload nach kurzer Pause
      setTimeout(() => {
        window.location.reload()
      }, 2000)
    } else if (!result.success) {
      error.value = result.message
    }
  } catch (err) {
    error.value = err.message || 'Installation fehlgeschlagen'
  }
}

function dismissBanner() {
  showBanner.value = false
  // Für 24 Stunden nicht mehr anzeigen
  const tomorrow = new Date()
  tomorrow.setHours(tomorrow.getHours() + 24)
  localStorage.setItem('update-dismissed-until', tomorrow.toISOString())
}

function closeModal() {
  showModal.value = false
}

function openModal() {
  showModal.value = true
}

// Lifecycle
onMounted(() => {
  // Initialer Check nach 5 Sekunden
  setTimeout(checkForUpdates, 5000)

  // Periodischer Check alle 30 Minuten
  checkInterval = setInterval(checkForUpdates, 30 * 60 * 1000)

  // Progress-Polling wenn Download läuft
  pollInterval = setInterval(() => {
    if (downloadInProgress.value) {
      pollProgress()
    }
  }, 1000)
})

onUnmounted(() => {
  if (checkInterval) clearInterval(checkInterval)
  if (pollInterval) clearInterval(pollInterval)
})

// Expose für externe Nutzung
defineExpose({
  checkForUpdates,
  openModal
})
</script>

<style scoped>
/* Banner Styles */
.update-banner {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  padding: 0.75rem 1rem;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}

.update-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.update-icon {
  flex-shrink: 0;
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

.update-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.update-title {
  font-weight: 600;
  font-size: 1rem;
}

.update-version {
  font-size: 0.875rem;
  opacity: 0.9;
}

.update-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-update {
  background: white;
  color: #059669;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-update:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.btn-progress {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
}

.btn-install {
  background: #fbbf24;
  color: #1f2937;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  font-weight: 600;
  cursor: pointer;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(251, 191, 36, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(251, 191, 36, 0); }
}

.btn-dismiss {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 0.25rem;
  transition: all 0.2s;
}

.btn-dismiss:hover {
  color: white;
  background: rgba(255, 255, 255, 0.1);
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  backdrop-filter: blur(4px);
}

.update-modal {
  background: var(--bg-secondary, #1a1a2e);
  border-radius: 1rem;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid var(--border-color, #333);
}

.modal-header h2 {
  margin: 0;
  font-size: 1.25rem;
  color: var(--text-primary, #fff);
}

.close-btn {
  background: transparent;
  border: none;
  color: var(--text-secondary, #888);
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 0.25rem;
}

.close-btn:hover {
  color: var(--text-primary, #fff);
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
  flex: 1;
}

.version-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.version-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1rem 1.5rem;
  border-radius: 0.75rem;
  min-width: 100px;
}

.version-badge.current {
  background: var(--bg-tertiary, #252540);
}

.version-badge.new {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
}

.version-badge .label {
  font-size: 0.75rem;
  text-transform: uppercase;
  opacity: 0.7;
  margin-bottom: 0.25rem;
}

.version-badge .version {
  font-size: 1.25rem;
  font-weight: 700;
}

.arrow {
  color: var(--fleet-orange, #10b981);
}

.release-notes {
  margin-bottom: 1.5rem;
}

.release-notes h3 {
  font-size: 1rem;
  margin-bottom: 0.75rem;
  color: var(--text-primary, #fff);
}

.notes-content {
  background: var(--bg-tertiary, #252540);
  padding: 1rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  line-height: 1.6;
  max-height: 200px;
  overflow-y: auto;
  color: var(--text-secondary, #aaa);
}

.notes-content :deep(h2) {
  font-size: 1rem;
  margin: 1rem 0 0.5rem;
  color: var(--text-primary, #fff);
}

.notes-content :deep(ul) {
  margin: 0.5rem 0;
  padding-left: 1.5rem;
}

.download-info {
  text-align: center;
  font-size: 0.875rem;
  color: var(--text-secondary, #888);
  margin-bottom: 1rem;
}

.progress-section {
  margin-top: 1rem;
}

.progress-bar {
  height: 8px;
  background: var(--bg-tertiary, #252540);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 0.5rem;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981, #059669);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.875rem;
  color: var(--text-secondary, #888);
  text-align: center;
  display: block;
}

.error-message {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid #ef4444;
  color: #ef4444;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  margin-top: 1rem;
  font-size: 0.875rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--border-color, #333);
}

.btn-secondary {
  background: transparent;
  border: 1px solid var(--border-color, #444);
  color: var(--text-primary, #fff);
  padding: 0.625rem 1.25rem;
  border-radius: 0.5rem;
  cursor: pointer;
  text-decoration: none;
  font-size: 0.875rem;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background: var(--bg-tertiary, #252540);
}

.btn-primary {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border: none;
  color: white;
  padding: 0.625rem 1.25rem;
  border-radius: 0.5rem;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.875rem;
  transition: all 0.2s;
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

/* Animations */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Responsive */
@media (max-width: 640px) {
  .update-content {
    flex-wrap: wrap;
  }

  .update-text {
    flex-basis: 100%;
    order: 2;
  }

  .update-actions {
    order: 3;
    width: 100%;
    justify-content: center;
  }

  .update-modal {
    width: 95%;
    max-height: 90vh;
  }

  .version-info {
    flex-direction: column;
  }

  .arrow {
    transform: rotate(90deg);
  }
}
</style>
