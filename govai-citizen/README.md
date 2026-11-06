# GovAI One 智慧政务系统 — 群众端 (govai-citizen)

负责人:张梦凡 ｜ 后端端口:8083 ｜ 前端端口:5174 ｜ schema:`govai_citizen`(9 张表)

包含 **后端 Spring Boot 服务** 与 **前端 uni-app(Vue3 + H5 / 微信小程序)**。

---

## 目录结构

```
govai-citizen/
├─ govai-citizen-server/       后端 (Spring Boot 3.2.5, 端口 8083)
├─ govai-citizen-ui/           前端 · uni-app CLI 版 (Vue3 + Vite, 端口 5174)
├─ govai-citizen-ui-hx/        前端 · HBuilderX 标准工程  ← 正式维护目录
├─ govai-citizen-ui-backup/    前端 · 早期备份 (已被上面两个取代, 仅留档)
├─ sql/
│   ├─ init.sql                建库建表 + 预置数据(9 张表)
│   └─ upgrade_sso.sql         接入监管端 SSO 的增量脚本(citizen_user 加 sys_user_id)
├─ start-backend.bat           启动后端 (8083)
├─ start-frontend.bat          启动前端 H5 dev (5174)
├─ project.config.json         微信开发者工具配置 (appid: wx919dcd5fbb623f1e)
├─ project.private.config.json
└─ .shots/                     测试截图
```

### 三个前端目录怎么用

| 目录 | 性质 | 用途 |
|---|---|---|
| `govai-citizen-ui-hx` | **HBuilderX 标准工程** | **正式维护**。HBuilderX 打开目录 → 运行到小程序模拟器(微信开发者工具)/ 浏览器 / 真机 |
| `govai-citizen-ui` | uni-app CLI 工程(Vue3 + Vite) | H5 快捷调试(`npm run dev:h5` → 5174, 带 vite proxy)。HBuilderX 无法对它"运行到小程序" |
| `govai-citizen-ui-backup` | 早期备份副本 | 无用途,仅留档,勿改 |

> **约定**:后续前端改动以 `govai-citizen-ui-hx` 为准;CLI 目录只作 H5 快捷运行副本。**勿两边同时大改,否则会分叉。**

- HBuilderX 位置:`D:\Desktop\HBuilderX`
- 微信开发者工具:`D:\微信web开发者工具`(需在设置里开启"服务端口")
- 真机调试:把 `utils/config.js` 里 `#ifndef H5` 分支的 `BASE_URL` 改成电脑的局域网 IP(模拟器用 `localhost:8083` 即可)

---

## 技术栈

- **后端**:Spring Boot 3.2.5 · MyBatis-Plus 3.5.5 · MySQL 8.0 · Redis 7(可选,未装自动降级) · JWT(jjwt 0.12.5) · OpenFeign · Knife4j 4.4.0 · Hutool
- **前端**:uni-app + Vue3 + Pinia(微信小程序 + H5 双端)
- **认证**:经监管端 SSO 登录,透传监管端签发的 token(见下)

## 运行时版本

本机实际使用 **PATH 上的 java / npm**,启动脚本不再写死绝对路径:

| 组件 | 版本 | 位置 |
|---|---|---|
| JDK | **22**(`java version "22.0.2"`) | `D:\download\jdk-22` |
| Maven | 3.9.9 | `D:\apache-maven-3.9.9` |
| Node / npm | **22**(`v22.17.1`) | `D:\Program Files\nodejs` |
| MySQL | 8.4 client | `C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe`,**root / `123456`** |

> ⚠️ **两处历史遗留已修正**:旧版脚本与旧版本文档写的是 `C:\govai-tools\apps\` 下的 **JDK 17 / Node 20 / MySQL 8.0.29(root123)**,但 `C:\govai-tools` 在本机**不存在**,脚本一跑就报错。现已改为跟随 PATH。
> 若需要在 JDK 17 上跑(Spring Boot 3.2.5 官方支持 17–21),本机 `C:\Program Files\Microsoft\jdk-17.0.8.101-hotspot` 也装着,只是被 PATH 上的 JDK 22 遮蔽。

## 启动步骤

```bat
:: 0. 首次:初始化数据库
mysql -u root -p123456 --default-character-set=utf8mb4 < sql\init.sql

:: 1. 启动后端 (8083)
start-backend.bat
::    接口文档 http://localhost:8083/doc.html

:: 2. 启动前端 H5 (5174)
start-frontend.bat
::    浏览器 http://localhost:5174  (/api 已代理到 8083)
```

**改了后端代码必须重启**(无 devtools 热重载)。若 `mvn package` 报
`Error reading old mojo status ...\target\maven-status\...inputFiles.lst`(中文路径导致的文件损坏),
先 `rm -rf target/maven-status target/classes` 再重跑即可。

### 微信小程序构建

```bat
cd govai-citizen-ui
npm run build:mp-weixin
```

用微信开发者工具导入 `govai-citizen-ui/dist/build/mp-weixin`(appid 用测试号 `touristappid`)。
或直接用 HBuilderX 打开 `govai-citizen-ui-hx` 运行到小程序模拟器。

---

## 认证与联调状态

**当前为真实三端联调**(`application.yml` 里 `govai.mock-login: false`):

- **登录**:前端发 `{phone, code:"000000"}`,后端 `AuthService.ssoLogin` 经 Feign 调监管端
  `POST /api/v1/auth/sms-login`(按 `sys_user.phone` + 固定码,仅 CITIZEN)拿到统一 token **原样透传**;
  `JwtInterceptor` 按 `claims.userId` 查 `citizen_user.sys_user_id` 映射回本地 id,业务层零改动。
  该映射列由 `sql/upgrade_sso.sql` 添加。
- **审批**:提交后 `ApplyService.submit` 把 `matterCode` 当 `templateCode`,并把真实 `instanceId` 回写
  `approval_instance_id`;`ProgressService` 有 `instanceId` 时一律真调审批端。
- **办结通知**:审批端终态 → HTTP 回调群众端 `/api/v1/external/records/status` → `ExternalStatusService`
  幂等回写状态 + 写 APPROVAL 站内信 + 未读角标(该端点在 `WebMvcConfig` 放行,不走群众端 JWT)。
- **监管数据互通**:提交后经 `cit-view/approvals/report` 上报办件;用户资料经 `cit-view/credit` 补信用分。

**回退到单机演示**:把群众端 `application.yml` 的 `govai.mock-login` 改回 `true` 即可
(mock 分支全部保留:本地自签 JWT + mock 审批),监管端无副作用。

### 演示账号

| 用途 | 账号 | 说明 |
|---|---|---|
| 群众端 | `13800000020` / 验证码 `000000` | 王小明,信用 A/88 |
| 群众端 | `13800000021` / `000000` | 李华,信用 B/76 |
| 审批岗(联调用) | `approver1` / `decide1` / `issue1`,密码 `123456` | 受理初审 / 决定 / 办结 |

> 注意:审批端 `decide1`、`issue1` 登录监管端时**须省略 `loginType`**——监管端 `LOGIN_TYPE_ROLE` 对照表里没有 DECIDE/ISSUE。

---

## 功能模块

| 模块 | 说明 |
|---|---|
| 登录 | 手机号 + 固定验证码 → 监管端 SSO,自动注册 |
| 事项服务 | 分类、全文/模糊检索、办理指南/条件/材料/流程、详情 |
| 在线申报 | 表单 + 材料上传(本地 `uploads/`)→ 创建申报 → Feign 调审批端;实名资料自动预填、本地草稿自动保存 |
| 进度追踪 | Feign 查审批进度;时间线;终态可去评价;支持撤回(仅本地 PENDING 且未被审批端受理) |
| 消息中心 | 站内消息(办件/政策/系统)、未读角标、标记已读;办结实时通知 |
| 好差评 | 办结后评价(星级 + 文字 + 标签)、我的评价、评价统计 |
| 政策推荐 | 按实名画像(年龄档/实名状态/办件动态)自动匹配,含详情页与阅读即已读 |
| AI 助手 | Feign 调审批端 `/api/v1/ai/chat`;前端超时设 60s(推理模型耗时 20s+) |
| 个人中心 | 资料 + 信用分卡片(经 `cit-view/credit` 拉取) |

---

## 关键约定(全组一致)

- 返回格式统一 `Result<T>` / `PageResult<T>`(code: 200 / 400 / 401 / 403 / 500)
- **JWT 共享密钥:通过环境变量 `JWT_SECRET` 注入**(4 人一致)
- 跨服务数据一律通过 **Feign** 获取,**禁止跨 Schema 查表**
- 事项公开读接口 `GET /api/v1/matters/*` 放行(审批端契约),写操作需登录
- 列表页展示约定:不展示自增主键 id 列、业务码作首列、时间统一 `YYYY-MM-DD HH:mm`、一屏不横滚

## 第 6 周联调计划

- 删掉 Feign 的 `url` 属性,切 **Nacos** 服务发现
- 登录已提前切到监管端 SSO(见上);如届时改为一网通办统一入口,注意同步 `JWT` 载荷

## 已知问题

- 本机未装 Redis → 后端自动降级,缓存功能不启用,不影响主流程
- `start-*.bat` 输出为英文(纯 ASCII)。cmd 批处理在 UTF-8 中文 + LF 换行下会被解析错乱,故刻意不写中文注释
