<script setup lang="ts">
<#if conditions.systemAdmin>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const isEntityForm = computed(() => route.name === 'entity-form')
</#if>
</script>

<template>
  <div class="app-container">
    <#if conditions.systemAdmin>
    <header class="admin-header">
      <span class="logo">${project.artifactId}</span>
      <nav class="admin-nav">
        <router-link to="/">首页</router-link>
      </nav>
    </header>
    <main class="admin-main" :class="{ 'form-page': isEntityForm }">
      <router-view />
    </main>
    <#else>
    <main class="simple-main">
      <router-view />
    </main>
    </#if>
  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
}

<#if conditions.systemAdmin>
.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 16px;
  background-color: #409eff;
  color: #fff;
}

.logo {
  font-weight: 600;
}

.admin-nav a {
  color: #fff;
  text-decoration: none;
}

.admin-main {
  padding: 16px;
}

.admin-main.form-page {
  padding: 24px;
}
</#if>

.simple-main {
  padding: 24px;
}
</style>
