{
  "name": "${project.artifactId}",
  "private": true,
  "version": "0.0.1",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc --noEmit && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "@element-plus/icons-vue": "2.3.2",
    "axios": "1.19.0",
    "element-plus": "2.14.4",
    "vue": "3.5.41",
    "vue-router": "5.2.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "6.0.8",
    "typescript": "~5.9.3",
    "vite": "8.2.1",
    "vue-tsc": "3.3.9"
  },
  "engines": {
    "node": "^20.19.0 || >=22.12.0"
  }
}
