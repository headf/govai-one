# Design System: GovAI 智慧政务 · 群众端 (Citizen Mobile)

> Scope: uni-app (Vue3) 移动端 H5 / 微信小程序。单位体系为 `rpx`（1rpx = 屏宽/750），
> 令牌集中在 `App.vue` 全局非 scoped 样式。本文档是该端后续所有页面（含 Stitch 生成新屏）的
> **唯一设计事实来源**。

## 1. Visual Theme & Atmosphere

**可信、克制、清晰的政务服务台。** 像一间采光良好、秩序井然的政务大厅——干净、可预期、
不喧哗。视觉服务于"把事办成"，而非炫技。

- **Density: 4 / 10** — 日常应用均衡。信息成块、留白充足，公交/老年人也能一眼扫清。
- **Variance: 4 / 10** — 轻微非对称（Hero 左对齐、卡片错落间距），但**不做艺术化失衡**；
  政务场景中秩序感 > 惊喜感。
- **Motion: 4 / 10** — 克制的流体感。只保留必要的状态过渡与按压反馈；
  不使用持续跳动/循环动画（无障碍与"庄重"要求）。
- **语气**：温和、直白、以群众口吻陈述（"您有 N 条未读消息"），不用营销腔。

> 与通用 premium 审美的偏离（本项目有意为之）：
> 主色为**机构蓝** `#2563EB`（品牌/可访问性诉求，非"AI 霓虹蓝紫"）；中文正文使用
> 系统字体栈（PingFang / 华为 / 小米），**不引入 Geist/Inter 类拉丁展示体**——CJK 字形
> 覆盖与系统渲染质量优先。

## 2. Color Palette & Roles

**中性基座（绝对中性，冷灰，不混暖灰）**
- **Canvas Mist** `#F4F6FA` — 页面底色 (`--c-bg`)
- **Pure Surface** `#FFFFFF` — 卡片/容器填充
- **Ink Primary** `#1F2329` — 主文字 (`--t1`)，Off-Black（**禁用纯黑 #000000**）
- **Ink Secondary** `#4E5969` — 次级文字 / 说明 (`--t2`)
- **Ink Tertiary** `#86909C` — 辅助 / 元数据 / 占位 (`--t3`)
- **Ink Quaternary** `#C9CDD4` — 极弱提示 / 箭头 (`--t4`)
- **Hairline** `#E5E6EB` — 1px 结构分隔线 (`--c-div`)

**主色（单一强调色 · 机构蓝）**
- **Institutional Blue** `#2563EB` — 唯一强调色：主按钮、选中态、链接、进度当前点 (`--c-primary`)
- **Blue Lift** `#4A8DFF` — 悬停/渐变亮端 (`--c-primary-hi`)
- **Blue Deep** `#1E56E0` — 按下/深色端 (`--c-primary-deep`)
- **Hero Gradient** `linear-gradient(135deg, #4A8DFF, #2563EB)` — 顶部 Hero 与主按钮 (`--grad-primary`)
- **Blue Wash** `#EAF2FF` / **Softer Wash** `#F3F7FF` — 浅蓝底（chip、选中底） (`--c-soft` / `--c-soft2`)
- **Blue Hairline** `#D8E5FF` — 蓝色描边（ghost 按钮、搜索条） (`--c-line`)
- **Focus Ring** `rgba(37, 99, 235, 0.16)` — 时间线当前环 / 聚焦 (`--ring-primary`)

**语义色（收敛自全站，禁止漂移）**
- **Success / 办结绿** `#2FA566` — 统一成功色，全站仅此一种绿 (`--c-ok`)
- **Warning / 待处理** `#E6A23C`（文字态 `#B07A0E`） (`--c-warn`)
- **Danger / 驳回** `#FF3B30` + 底 `#FFECED` (`--c-danger` / `--c-danger-bg`)
- **Info / 进行中** `#4E8CF5` (`--c-info`)
- **Star / 评分金** `#F5A623` (`--c-gold` / `--c-star`)
- **WeChat Green** `#07C160` — 仅微信登录 (`--c-wechat`)

**质感**
- **Card Shadow** `0 8rpx 32rpx rgba(15,40,100,0.07)`（hover 加深至 `0 14rpx 40rpx / 0.12`） (`--shadow-card`)
- **Card Shadow Sm** `0 4rpx 16rpx rgba(15,40,100,0.05)` (`--shadow-card-sm`)
- **Button Glow** `0 8rpx 24rpx rgba(37,99,235,0.25)` — 柔和投影，**非霓虹外发光** (`--shadow-btn`)

> 约束：**强调色最多 1 个**（机构蓝）；饱和度以可访问性为准（本项目主蓝略高于 80%，
> 因政务品牌与对比度需要，且非霓虹用途）。**不引入第二强调色、不做紫/霓虹渐变。**

## 3. Typography Rules

- **正文 / 通用**：系统无衬线栈
  `-apple-system, "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif`
  （中文优先 PingFang；Android 回落系统黑体）。Relaxed leading（1.6~1.7），正文最大约 65 汉字/行。
- **层级**：靠**字重 + 颜色层级**建立，而非一味放大。
  - H1 页标题 44rpx / 700（Hero）
  - H2 区块标题 34rpx / 600（`.section-title`，左侧 8rpx 蓝色标条）
  - 卡片标题 30rpx / 500–600
  - 正文 28rpx / 400
  - 辅助/元数据 24rpx / `--t3`
  - 微标签/胶囊 22rpx
- **数字**：金额、编号、时长等**高密度数字使用等宽呈现**（`font-variant-numeric: tabular-nums`），
  避免跳动对齐问题。
- **禁用**：`Inter`（拉丁展示位）；通用衬线体（`Times New Roman`/`Georgia`/`Garamond`）；
  任何衬线体用于本端（移动政务 UI）。
- **字距**：仅 Hero 大标题可 `letter-spacing: 2rpx`；正文不得加宽字距。

## 4. Component Stylings

- **Buttons（主/次/软/危险）**
  - 形状：圆角胶囊 `999rpx`；高度由页面控制（`padding: 24~26rpx`）
  - 主按钮：`--grad-primary` 填充 + 白色字 + 柔和投影
  - 次按钮（ghost）：白底 + `--c-line` 描边 + 蓝字
  - 软按钮：`--c-soft` 底 + 蓝字；危险：`--c-danger`
  - 交互：按下 `scale(0.98)` + `brightness(0.96)`（`0.12s`）；**无外发光、无自定义光标**
- **Cards**
  - 圆角 `20rpx`；内边距 `24rpx`；投影 `--shadow-card`
  - **仅当层级需要抬升时使用**；hover 上浮 `translateY(-2rpx)` + 阴影加深
  - 高密度/纯列表场景改用 `1rpx` 分隔线（`.list-item`）或负空间，不堆卡片
- **Inputs / Forms**
  - 标签在左（`.form-lbl` 170rpx）或上方；占位 `--t4`
  - 聚焦：容器外发光环（`search-pill:focus-within`）或 `--ring-primary`
  - 不用浮动标签；错误提示在使用位置就近显示
- **状态胶囊 `.status-pill`**
  - `sp-ok / sp-warn / sp-danger / sp-info / sp-hold`，22rpx，`999rpx` 圆角
- **段选 `.seg`**：`--c-soft2` 底轨 + 白色激活片（`.seg-on`，蓝字 + 微投影）
- **Time Line（进度）**：`tl-ok` 绿点 / `tl-cur` 蓝点 + `--ring-primary` 环 / `tl-wait` 灰点；
  已完成连线转绿（`.line.ok`）
- **Loaders**：**骨架屏 `.sk-line`**（1.4s shimmer，尺寸贴合真实布局）；**禁用通用转圈 spinner**（除全局"提交中"轻量 loading）
- **Empty States**：组合式空态 `.empty-box`（图标圆 + 文案 + 可选 CTA 按钮），非一句"暂无数据"
- **Icons**：优先文字/符号（`›`、`×`）；**不使用 emoji**

## 5. Layout Principles

- **移动优先单列**：主内容单列纵向流；分类/快捷用 2 列网格（`grid-template-columns: 1fr 1fr`）
- **禁止横向溢出**：`scroll-view scroll-x` 用于分类横滑，页面本身不横滚
- **禁止元素重叠**：每个元素占据独立空间；**不用负边距制造"压盖"**（本端曾因负边距导致提示条与卡片重叠，已改为正常流式间距）
- **Hero**：左对齐（不用居中 Hero）；渐变底 + 右上/右下径向光斑装饰（`::before/::after`，`pointer-events:none`）
- **间距哲学**：`20rpx` 为基本节奏（卡片间距/页边距）；区块标题上下 `28rpx 4rpx 16rpx`
- **触控目标**：可点元素高度 ≥ 88rpx（约 44px）
- **单位**：一律 `rpx`；不得用 `px` 硬编码（`transform`/`box-shadow` 模糊值可用 rpx）

## 6. Motion & Interaction

- **过渡**：交互态统一 `0.12s~0.18s ease`（卡片 `.18s`、按钮 `.12s`、段选 `.18s`）
- **按压反馈 `.u-hover`**：`scale(0.97)` + `opacity 0.9`（配合 `hover-class` 施加于可点 view）
- **唯一"持续"动画**：骨架 shimmer（`sk-shine 1.4s infinite`）。
  **其余一律不做无限循环动画**（政务庄重 + 无障碍 + 省电）；列表不做瀑布式 stagger。
- **性能**：只动画 `transform` / `opacity`，不动画 `top/left/width/height`

## 7. Anti-Patterns（严禁）

- 禁止 emoji（UI 内任何位置）
- 禁止纯黑 `#000000`（用 `--t1 #1F2329`）
- 禁止霓虹/外发光阴影、禁止紫/霓虹渐变、禁止第二个强调色
- 禁止 `Inter` 及通用衬线体；本端禁用任何衬线体
- 禁止元素重叠 / 负边距压盖 / 绝对定位堆叠内容
- 禁止"三张等宽卡片一排"通用 Feature 布局（用 2 列网格 / 横滑 / 列表）
- 禁止居中 Hero（本端低方差但需左对齐）
- 禁止通用转圈 spinner（用骨架屏）；禁止空态只写"暂无数据"
- 禁止 AI 文案套话（"赋能/一站式/无缝/极致/Next-Gen/ Elevate"）与"滚下去看看"类填充文案、滚动箭头/跳动箭头
- 禁止假大整数（`99.99%`、`50%`）、禁止占位假名（`John Doe`/`Acme`/`Nexus`）
- 禁止破坏的图片外链；占位图用 `picsum.photos` 或内联 SVG
- 禁止跨端强调色漂移（成功绿全站只有 `#2FA566` 一种）
- 禁止为动画而动画：不得添加与本端 Motion 规则冲突的循环/弹跳

---

### 附：Stitch 生成新屏时的用法建议
- 屏宽基准 750（rpx）；所有尺寸用 `rpx` 描述，颜色引用上方语义名 + `#hex`。
- 每屏结构建议：`Hero(左对齐渐变) → 主卡片/列表 → 空态或骨架`；单强调色、单主 CTA。
- 中文文案遵循群众口吻、动词开头、拒绝营销腔；数字用 tabular。
- 需要图标时优先符号/描边线条，不引入 emoji 与花哨图标集。
