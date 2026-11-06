# govai-supervision-ui

监管端（govai-supervision）前端大屏与后台管理。Vue3 + Vite + Element Plus + ECharts。

## 技术栈
- Vue 3（`<script setup>`，JavaScript）
- Vite（dev 端口 5176）
- Element Plus（UI）+ ECharts（大屏图表）
- axios（统一封装修 + 鉴权头 + 401 跳登录）

## 启动
```bash
npm install
npm run dev      # http://localhost:5176
```
依赖后端在 8081（MySQL/Redis/RabbitMQ 就绪，`mvn spring-boot:run`）。

## 打通方式（重要）
浏览器在 `http://localhost:5176`，后端在 `http://localhost:8081`，跨域由 **Vite 代理**解决（见 `vite.config.js`）：
- `/api` → `http://localhost:8081`（REST）
- `/ws`  → `ws://localhost:8081`（WebSocket 大屏推送，无 token 直连）

无需改动任何后端 CORS 代码。

## 登录
预置账号：`admin` / `approver1` / `supervisor1`，密码均 `123456`。JWT 存 `localStorage['govai_token']`，请求自动带 `Authorization: Bearer <token>`。

## 目录
```
src/api/request.js       axios 封装（baseURL /api/v1、带 token、401 跳登录、解 Result）
src/api/modules.js       所有接口函数
src/utils/webSocket.js   /ws 连接 + 事件回调 + 断线重连
src/layout/Layout.vue    后台布局（侧边菜单 + 用户/退出）
src/views/               Login / Dashboard / system / credit / risk / supervise / setting
```

## 大屏实时刷新
`DashboardView.vue` 连 `/ws`，收到 `{event:'approval.updated'}` 立即重拉 `/stats/overview` + `/stats/daily` 刷新 KPI 与图表；另有 30s 轮询兜底。

## 部署备注
Vite 代理仅用于开发。生产打包用 `npm run build`（产出 `dist/`），由 Nginx 反代或后端 CORS 打通，见 `doc/监管端开发执行计划书.md` 阶段 J。