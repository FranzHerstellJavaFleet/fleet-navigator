import { ref, computed } from 'vue'

/**
 * Global Locale Management für Fleet Navigator
 * Erkennt automatisch Browser-Sprache und bietet Übersetzungen
 */

// Global state (singleton pattern)
const browserLocale = ref(navigator.language || navigator.userLanguage || 'de-DE')
const currentLocale = ref(detectLanguage())

function detectLanguage() {
  const lang = browserLocale.value.toLowerCase()

  // Priorität: Browser-Sprache
  if (lang.startsWith('de')) return 'de'
  if (lang.startsWith('en')) return 'en'
  if (lang.startsWith('fr')) return 'fr'
  if (lang.startsWith('es')) return 'es'
  if (lang.startsWith('it')) return 'it'
  if (lang.startsWith('nl')) return 'nl'
  if (lang.startsWith('pl')) return 'pl'
  if (lang.startsWith('ru')) return 'ru'
  if (lang.startsWith('zh')) return 'zh'
  if (lang.startsWith('ja')) return 'ja'

  // Standard: Deutsch (für JavaFleet Systems Consulting)
  return 'de'
}

// Übersetzungen
const translations = {
  de: {
    app: {
      name: 'Fleet Navigator',
      tagline: 'Deine private AI - kostenlos, lokal und ohne Cloud',
      poweredBy: 'Powered by JavaFleet Systems Consulting'
    },
    welcome: {
      title: 'Willkommen bei Fleet Navigator',
      subtitle: 'Starte eine Konversation mit deiner AI-Flotte',
      suggestions: {
        letter: {
          title: '📝 Brief schreiben',
          description: 'Bewerbung, Kündigung, Geschäftsbrief',
          prompt: 'Hilf mir beim Schreiben eines Bewerbungsschreibens für eine Stelle als [Deine Position]'
        },
        question: {
          title: '💬 Fragen stellen',
          description: 'Zu jedem Thema - Wissenschaft, Geschichte, Alltag',
          prompt: 'Erkläre mir, wie Photosynthese funktioniert'
        },
        translate: {
          title: '🌐 Übersetzen',
          description: 'Texte in viele Sprachen übersetzen',
          prompt: 'Übersetze folgenden Text ins Englische: [Dein Text hier]'
        },
        learn: {
          title: '📚 Lernen',
          description: 'Komplexe Themen einfach erklärt',
          prompt: 'Erkläre mir Schritt für Schritt: Was ist künstliche Intelligenz?'
        },
        code: {
          title: '💻 Programmieren',
          description: 'Code schreiben und verstehen',
          prompt: 'Schreibe mir ein Python-Skript, das [beschreibe deine Aufgabe]'
        },
        creative: {
          title: '✨ Kreativ sein',
          description: 'Gedichte, Geschichten, Ideen',
          prompt: 'Schreibe mir ein Gedicht über den Herbst'
        }
      }
    },
    loading: {
      thinking: 'Denke nach...',
      searching: 'Suche im Web...',
      searchingAndThinking: 'Suche im Web und denke nach...'
    },
    health: {
      notOperational: 'System nicht vollständig einsatzbereit',
      warnings: 'Systemwarnungen',
      checkAgain: '🔄 Erneut prüfen',
      modelInstall: '🤖 Modell Installation:'
    },
    sidebar: {
      newChat: 'Neuer Chat',
      models: 'Modelle',
      settings: 'Einstellungen',
      letters: 'Briefe',
      agents: 'Agenten'
    },
    download: {
      title: 'Dokument bereit',
      description: 'Das generierte Dokument steht zum Download bereit',
      button: 'Herunterladen',
      downloading: 'Lädt herunter...',
      success: 'Download erfolgreich!',
      error: 'Download fehlgeschlagen. Bitte versuche es erneut.'
    }
  },
  en: {
    app: {
      name: 'Fleet Navigator',
      tagline: 'Your private AI - free, local and without cloud',
      poweredBy: 'Powered by JavaFleet Systems Consulting'
    },
    welcome: {
      title: 'Welcome to Fleet Navigator',
      subtitle: 'Start a conversation with your AI fleet',
      suggestions: {
        letter: {
          title: '📝 Write letter',
          description: 'Application, resignation, business letter',
          prompt: 'Help me write a cover letter for a position as [Your Position]'
        },
        question: {
          title: '💬 Ask questions',
          description: 'About any topic - science, history, everyday life',
          prompt: 'Explain to me how photosynthesis works'
        },
        translate: {
          title: '🌐 Translate',
          description: 'Texts into many languages',
          prompt: 'Translate the following text into German: [Your text here]'
        },
        learn: {
          title: '📚 Learn',
          description: 'Complex topics simply explained',
          prompt: 'Explain step by step: What is artificial intelligence?'
        },
        code: {
          title: '💻 Programming',
          description: 'Write and understand code',
          prompt: 'Write me a Python script that [describe your task]'
        },
        creative: {
          title: '✨ Be creative',
          description: 'Poems, stories, ideas',
          prompt: 'Write me a poem about autumn'
        }
      }
    },
    loading: {
      thinking: 'Thinking...',
      searching: 'Searching the web...',
      searchingAndThinking: 'Searching the web and thinking...'
    },
    health: {
      notOperational: 'System not fully operational',
      warnings: 'System Warnings',
      checkAgain: '🔄 Check again',
      modelInstall: '🤖 Model Installation:'
    },
    sidebar: {
      newChat: 'New Chat',
      models: 'Models',
      settings: 'Settings',
      letters: 'Letters',
      agents: 'Agents'
    },
    download: {
      title: 'Document ready',
      description: 'The generated document is ready for download',
      button: 'Download',
      downloading: 'Downloading...',
      success: 'Download successful!',
      error: 'Download failed. Please try again.'
    }
  }
}

export function useLocale() {
  const isGerman = computed(() => currentLocale.value === 'de')
  const isEnglish = computed(() => currentLocale.value === 'en')

  const t = (key) => {
    const keys = key.split('.')
    let value = translations[currentLocale.value]

    for (const k of keys) {
      if (value && typeof value === 'object' && k in value) {
        value = value[k]
      } else {
        // Fallback zu Deutsch wenn Übersetzung fehlt
        value = translations.de
        for (const fallbackKey of keys) {
          if (value && typeof value === 'object' && fallbackKey in value) {
            value = value[fallbackKey]
          } else {
            return key // Zeige Key wenn nichts gefunden
          }
        }
        break
      }
    }

    return value
  }

  const setLocale = (locale) => {
    if (translations[locale]) {
      currentLocale.value = locale
      localStorage.setItem('fleet-navigator-locale', locale)
    }
  }

  // Check localStorage for saved preference
  const savedLocale = localStorage.getItem('fleet-navigator-locale')
  if (savedLocale && translations[savedLocale]) {
    currentLocale.value = savedLocale
  }

  return {
    locale: computed(() => currentLocale.value),
    isGerman,
    isEnglish,
    t,
    setLocale,
    availableLocales: computed(() => Object.keys(translations))
  }
}
