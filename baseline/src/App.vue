<script setup lang="ts">
</script>

<template>
  <div class="app-container">
    <main class="simple-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
}


.simple-main {
  padding: 24px;
}
</style>
