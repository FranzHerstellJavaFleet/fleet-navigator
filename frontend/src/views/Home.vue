<template>
  <div class="flex-1 flex flex-col overflow-hidden">
    <!-- Project View or Chat Window -->
    <ProjectView
      v-if="selectedProject"
      :project="selectedProject"
      :project-chats="projectChats"
      @close="closeProject"
      @refresh="refreshProject"
    />
    <ChatWindow v-else />
  </div>
</template>

<script setup>
import { ref, computed, inject } from 'vue'
import ChatWindow from '../components/ChatWindow.vue'
import ProjectView from '../components/ProjectView.vue'
import { useChatStore } from '../stores/chatStore'

const chatStore = useChatStore()

// This will be provided by MainLayout
const selectedProject = inject('selectedProject', ref(null))
const projectChats = inject('projectChats', computed(() => []))

function closeProject() {
  selectedProject.value = null
}

function refreshProject() {
  // Handled by MainLayout
}
</script>
