# GovAI One 智慧政务系统

企业实训项目 · 第 12 组。四个端并行开发,共用同一个 MySQL 实例(不同 Schema),跨端数据一律走 HTTP/Feign,
**禁止跨 Schema 直接查表**。

---

## 四个子工程

| 成员 | 负责端 | 目录 | 仓库名 | 后端 | 前端 | Schema |
|---|---|---|---|---|---|---|
| 张梦凡 | 群众端 | `govai-citizen/` | govai-citizen | 8083 | 5174 | `govai_citizen` |
| 张可 | 审批员端 | `govai-approval/` | govai-approval | 8082 | 5173 | `govai_approval` |
| 郝锦瑞 | 企业端 | `govai-enterprise/` | govai-enterprise | 8084 | 5175 | `govai_enterprise` |
| 杨星宇 | 监管端 | `govai-supervision/` | govai-supervision | 8081 | 5176 | `govai_supervision` |

### 各端一句话

- **群众端**(8083/5174)— 事项查询、在线申报、进度追踪、好差评、政策推荐、AI 助手。uni-app 双端(微信小程序 + H5)
- **审批员端**(8082/5173)— 规则引擎 + AI 审核意见,按真实政务流程分**三岗位**办理:受理初审 → 决定 → 办结出证
- **企业端**(8084/5175)— 企业门户:事项大厅、智能办事规划、在线申报、证照空间(生命周期管理)、政策、OCR 拍照回填
- **监管端**(8081/5176)— 数据大屏、审批数据实时监控、风险预警、双随机抽查、信用档案与修复、RBAC 权限、操作日志

## 端口分配总表

| 端口 | 服务 |
|---|---|
| 8081 | 监管端后端(`govai-supervision-server`,Spring Boot) |
| 8082 | 审批端后端(`govai-approval`,Spring Boot) |
| 8083 | 群众端后端(`govai-citizen`,Spring Boot) |
| 8084 | 企业端后端(`govai-enterprise`,Spring Boot) |
| 5173 | 审批端 UI(Vue3 + Vite) |
| 5174 | 群众端 H5(uni-app,`npm run dev:h5`) |
| 5175 | 企业端 UI(Vue3 + Vite) |
| 5176 | 监管端 UI(Vue3 + Vite) |

另有 `nginx/nginx.conf` 作为反向代理参考配置(**注意其端口语义已过时**,见下方「已知过时项」)。

---

## 目录导航

```
企业实训-智能政务系统/
├─ README.md          ← 本文件
├─ 文档/              统一文档入口(需求 / 设计 / 答辩 / 原型 / 过程稿)
├─ scripts/           工具与运维脚本(生成文档 / 修复PPT / 运维)
├─ logs/              运行日志(所有服务输出统一写在这里)
├─ backups/           数据库备份 SQL(改动前快照)
├─ archive/           归档(重复压缩包、erl_crash.dump、旧版本)
├─ nginx/             反向代理配置参考
├─ .idea/             IDE 工程(含 MySQL 数据源连接,勿删)
├─ govai-approval/         审批端
├─ govai-citizen/          群众端
├─ govai-enterprise/       企业端
└─ govai-supervision/      监管端
```

| 目录 | 放什么 | 备注 |
|---|---|---|
| `文档/答辩/` | 结题答辩 PPT、逐页讲稿、分人讲稿(4)、术语手册、功能实现说明、贡献度表 | **答辩直接取用** |
| `文档/过程稿/` | 通俗版讲稿、PPT 提纲等中间产物 | 可清理 |
| `文档/需求` · `文档/设计` | 需求规约/矩阵、骨架文档、分工方案、模块图 | 正式交付物 |
| `scripts/` | 文档生成脚本(`gen_*.py`)、PPT 修复脚本(`fix_*.py`)、运维脚本 | 改文档内容后可重跑 |
| `logs/` | 各端控制台输出 | 所有服务统一写这里 |
| `backups/` | `mysqldump` 的改动前快照 | 一次性,勿当作可恢复的正式备份 |
| `archive/` | 归档的重复压缩包、崩溃转储、旧版本 | 可清理,但含唯一副本请注意 |

---

## 推荐启动顺序

监管端是**统一凭证主库**(`sys_user`)与 SSO 登录入口,其他三端登录都依赖它,因此:

```
1. MySQL (3306, root/123456)  ← 必须先起
2. 监管端后端 8081            ← 凭证主库 + SSO,其他端登录依赖它
3. 监管端 UI   5176
4. 其余三端后端(8082 / 8083 / 8084)与前端(5173 / 5174 / 5175)按需启动
```

各端启动方式见各自 README / 源码根目录。群众端已提供 `start-backend.bat` 与 `start-frontend.bat`。

**运行环境**:本机 java / mvn / node 均在 PATH 上,实际版本为 **JDK 22 · Maven 3.9.9 · Node 22**;
MySQL 3306 为 8.4.9,口令 **`root/123456`**。
> ⚠️ 骨架文档与部分子工程 README 里写的 `root/root123` 是**他人机器/旧环境的值**,本机是 `123456`。
> 另:`C:\govai-tools\apps\...` 这套工具链路径在本机**不存在**(部分旧脚本/文档仍引用它,已失效)。

---

## 已知过时项(整理时确认,未擅自改动)

1. **`nginx/nginx.conf` 的端口语义停留在旧骨架规划** —— 它把 8081 标注为「底座+数据服务」、8083 标注为「政策解读服务」,
   而实际 8081 是监管端、8083 是群众端;并且完全没有监管端/群众端的前端路由。改动需配合实际部署,故仅在此标注。
2. **`govai-supervision/HELP.md`** 是 Spring Initializr 模板残留,链接指向 Spring Boot 4.1.1 文档,而 `pom.xml` 实为 3.2.5。
3. **`govai-approval/govai-approval-ui/README.md`** 是 `create-vue` 脚手架默认模板,零项目信息。
4. **根目录不是 git 仓库**。仅两个子目录有 `.git`(见下)。

## 版本控制现状

| 目录 | 是仓库 | 提交数 | 未提交 |
|---|---|---|---|
| `govai-enterprise/` | ✅ | 3 | **121** |
| `govai-supervision/` | ✅ | **0**(整个监管端未入库) | 12 |
| `govai-approval/` | ❌ | — | — |
| `govai-citizen/` | ❌ | — | — |

> **建议尽快给两个仓库各提一次 commit。** 企业端第 4 周之后的全部工作(121 项)尚未入库,
> 监管端则零版本保护;审批端与群众端完全没有版本控制,任何误删都不可恢复。
> 根目录已预置 `.gitignore`,为将来在根执行 `git init` 兜底。

## 全组技术栈

**后端(统一)**:Spring Boot 3.2.5 · MyBatis-Plus 3.5.7 · MySQL 8.0 · Redis 7 · OpenFeign · RabbitMQ · JWT(jjwt 0.12.5) · Knife4j 4.4.0 · Hutool 5.8.26 · Lombok · LangChain4j(接 DeepSeek 大模型)

> 服务寻址采用**固定地址直连**(本地部署)；Nacos 2.2.3 为后续演进方向,**当前未启用**。

**前端**:群众端 uni-app + Vue3(uni-ui/uView);其余三端 Vue3 + TypeScript + Element Plus(监管端另加 ECharts + WebSocket 大屏)

**跨端约定**:统一返回 `Result<T>` / `PageResult<T>`(200/400/401/403/500);JWT 共享密钥通过环境变量 `JWT_SECRET` 注入(四端必须一致)

---

*整理于 2026-09-21：四端目录统一为 `govai-<角色>` 命名（原 `智慧政务系统-群众端`→`govai-citizen`、`Supervision`→`govai-supervision`、`GovAi/govai-enterprise`→`govai-enterprise`）；新增 `文档/答辩/` 与 `scripts/`；所有服务日志统一输出到 `logs/`。*
