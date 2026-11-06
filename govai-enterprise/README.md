# GovAI One 企业端(govai-enterprise)

负责人:**郝锦瑞** | 后端 8084 | 前端 5175 | 数据库 `govai_enterprise` | 特色依赖 **PaddleOCR**

面向法人的办事服务门户:企业登录 → 事项申报 → 智能审批进度 → 电子证照 → 政策免申即享。

## 技术栈
- 后端:Spring Boot 3.2.5 + MyBatis-Plus + JWT + Knife4j + OpenFeign(Java 17)
- 前端:Vue 3 + TypeScript + Element Plus + Pinia + Vite(端口 5175)
- 数据库:MySQL 8.0(`govai_enterprise`,9 张表)
- OCR:PaddleOCR 2.8.1(Python 3.11 独立服务)

## 目录结构
```
govai-enterprise/
├── govai-enterprise-server/   # 后端
├── govai-enterprise-ui/       # 前端(Vue3)
├── ocr-service/               # PaddleOCR 识别服务(Python)
└── sql/init.sql               # 建库建表 + 种子数据
```

## 环境准备
1. MySQL 8.0(本机 127.0.0.1:3306,root/123456)
2. Java 17+、Maven 3.9+、Node 18+
3. Python 3.11(OCR 用,`py -3.11`)

## 启动步骤(按顺序)

### 1. 建库
```bash
mysql -uroot -p123456 -h127.0.0.1 --default-character-set=utf8mb4 < sql/init.sql
```

### 2. 启动 OCR 服务(Python 3.11,端口 8090)
```bash
# 首次需安装依赖
py -3.11 -m pip install paddlepaddle==2.6.2 paddleocr==2.8.1 flask

cd ocr-service
py -3.11 ocr_service.py
```
首次运行会自动下载 PaddleOCR 模型(约 20MB)到 `~/.paddleocr`。

### 3. 启动后端(端口 8084)
```bash
cd govai-enterprise-server
mvn spring-boot:run
```
API 文档:http://localhost:8084/doc.html

### 4. 启动前端(端口 5175)
```bash
cd govai-enterprise-ui
npm install
npm run dev
```
访问 http://localhost:5175

## 演示账号
| 账号 | 密码 | 角色 | 企业 |
|---|---|---|---|
| ent_admin | 123456 | 管理员 | 东软智膳餐饮管理有限公司 |
| ent_operator | 123456 | 经办人 | 东软智膳餐饮管理有限公司 |
| yunqi_admin | 123456 | 管理员 | 云启科技有限公司 |

## 功能清单
- 企业认证中心(登录/账号)
- 法人事项大厅(检索/分类/详情)
- 智能申报(动态表单 + **OCR 拍照回填** + 材料上传 + 免证办带证)
- 合规预检(名称/信用代码/法人/证照/材料五维检查)
- 我的申报(列表 + 审批进度时间轴)
- 企业电子证照(免证办数据源 + 到期提醒)
- 政策服务(匹配 + 免申即享确认)
- **OCR 能力接口**(契约接口8:`POST /api/v1/ocr/recognize`,供审批端调用)

## 跨服务契约(第 6 周联调)
- 调审批端(张可 8082):`POST /approval/submit`、`GET /approval/progress/{id}`(开发期本地模拟降级)
- 对外提供:`POST /ocr/recognize`(企业端特色依赖)
- 统一登录(SSO):开发期本地签发同密钥 JWT,联调期切监管端 `/auth/login`

## 环境差异备忘
- 本机 MySQL 密码为 `123456`(骨架文档写 root123,以实际为准)
- 本机无 Docker/Redis;Redis 未启动,代码已做降级处理
- OCR 依赖 Python 3.11(Paddle 对 3.13 支持不完整)
