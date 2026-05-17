import '@/echarts/register'
import '@/assets/main.css'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import VChart from 'vue-echarts'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.component('VChart', VChart)
app.use(createPinia())
app.use(router)
app.mount('#app')
