<template>
  <router-view />
  <!-- Global Confirm Dialog (available everywhere) -->
  <GlobalConfirmDialog />
  <!-- Auto-Update Notification Banner -->
  <UpdateNotification />
</template>

<script setup>
import GlobalConfirmDialog from './components/GlobalConfirmDialog.vue'
import UpdateNotification from './components/UpdateNotification.vue'
// App.vue is now just a router container
// All layout logic moved to MainLayout.vue
</script>
