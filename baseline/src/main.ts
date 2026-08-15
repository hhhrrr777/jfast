import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { hasPermiDirective } from './directive/hasPermi'
import './styles/index.css'

const app = createApp(App)

app.use(ElementPlus)
app.use(router)
app.directive('hasPermi', hasPermiDirective)
app.mount('#app')
