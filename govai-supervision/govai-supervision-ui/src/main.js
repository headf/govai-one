import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as Icons from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './styles/global.css'

const app = createApp(App)
app.use(ElementPlus, { locale: zhCn })
// 全局注册图标，便于布局菜单按 name 动态渲染
for (const [name, comp] of Object.entries(Icons)) {
  app.component(name, comp)
}
app.use(router)
app.mount('#app')