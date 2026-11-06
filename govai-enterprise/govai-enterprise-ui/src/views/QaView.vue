<template>
  <div class="qa-wrap">
    <el-card class="qa-head-card">
      <div class="head">
        <div>
          <p class="qa-sub">用大白话提问,基于事项库与政策知识库为您解答;点击右侧「历史记录」可切换回之前的对话窗口</p>
        </div>
        <div class="head-btns">
          <el-button type="primary" :icon="Plus" @click="newSession">新对话</el-button>
          <el-button type="primary" plain :icon="Clock" @click="openHistory">历史记录</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="chat-card">
      <div ref="chatBox" class="chat-box">
        <!-- 欢迎/引导 -->
        <div v-if="!messages.length" class="welcome">
          <el-icon :size="40" color="#2f6bff"><ChatDotRound /></el-icon>
          <p class="text-muted">您好,我是政务智能助手。您可以问我:</p>
          <div class="chips">
            <el-tag v-for="s in suggestions" :key="s" class="chip" @click="send(s)">{{ s }}</el-tag>
          </div>
        </div>

        <!-- 消息列表 -->
        <div v-for="(m, i) in messages" :key="i" :class="['msg-row', m.role]">
          <div class="avatar">
            <el-icon v-if="m.role === 'user'"><User /></el-icon>
            <el-icon v-else color="#2f6bff"><ChatDotRound /></el-icon>
          </div>
          <div class="bubble">
            <div class="msg-text">{{ m.text }}</div>

            <!-- 引用来源 -->
            <div v-if="m.role === 'ai' && m.sources?.length" class="src-block">
              <div class="src-title">引用来源</div>
              <div v-for="(s, si) in m.sources" :key="si" class="src-item">
                <el-tag size="small" :type="s.type === 'POLICY' ? 'warning' : 'success'" effect="plain">
                  {{ s.type === 'POLICY' ? '政策' : '事项' }}
                </el-tag>
                <span class="src-name">{{ s.title }}</span>
                <span v-if="s.snippet" class="text-muted src-snippet">{{ s.snippet }}</span>
              </div>
            </div>

            <!-- 关联事项(仅实时问答返回,历史会话恢复时不展示) -->
            <div v-if="m.role === 'ai' && m.relatedMatters?.length" class="rel-block">
              <span class="text-muted">相关事项:</span>
              <el-tag
                v-for="r in m.relatedMatters"
                :key="r.matterId"
                size="small"
                class="rel-tag"
                @click="router.push(`/matters/${r.matterId}`)"
              >
                {{ r.matterName }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="input-area">
        <div v-if="lastSuggested.length" class="chips suggest-line">
          <el-tag v-for="s in lastSuggested" :key="s" size="small" class="chip" @click="send(s)">{{ s }}</el-tag>
        </div>
        <div class="input-row">
          <el-input
            v-model="input"
            placeholder="请输入您的问题,如:食品经营许可证怎么办理?"
            clearable
            @keyup.enter="send()"
          />
          <el-button type="primary" :loading="asking" :disabled="!input.trim()" @click="send()">发送</el-button>
        </div>
      </div>
    </el-card>

    <!-- 历史记录抽屉(会话列表) -->
    <el-drawer v-model="historyDrawerVisible" title="历史对话窗口" size="460px">
      <div class="sess-wrap" v-loading="historyLoading">
        <div
          v-for="s in sessions"
          :key="s.id"
          :class="['sess-item', { active: s.id === currentSessionId }]"
          @click="switchSession(s.id)"
        >
          <div class="sess-main">
            <div class="sess-title">{{ s.title }}</div>
            <div class="sess-meta">
              <span>{{ fmtTime(s.lastTime) }}</span>
              <span>{{ s.messageCount }} 条消息</span>
            </div>
          </div>
          <el-button
            class="sess-del"
            type="danger"
            text
            :icon="Delete"
            size="small"
            @click.stop="removeSession(s.id)"
          />
        </div>
        <el-empty v-if="!sessions.length && !historyLoading" description="暂无历史对话" />
      </div>
      <div v-if="sessions.length" class="his-foot">
        <el-pagination
          v-model:current-page="historyQuery.page"
          v-model:page-size="historyQuery.size"
          :total="historyTotal"
          layout="total, prev, pager, next"
          small
          @change="loadSessions"
        />
        <el-button type="danger" plain size="small" @click="clearHistory">全部清空</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Plus, Delete } from '@element-plus/icons-vue'
import {
  qaAsk,
  qaSuggestions,
  createQaSession,
  listQaSessions,
  getQaSession,
  deleteQaSession,
  clearQaHistory
} from '@/api'
import type { QaSource, QaRelatedMatter, QaSession, QaSessionMessage } from '@/types'

interface Msg {
  role: 'user' | 'ai'
  text: string
  sources?: QaSource[]
  relatedMatters?: QaRelatedMatter[]
  suggested?: string[]
}

const router = useRouter()
const input = ref('')
const asking = ref(false)
const messages = ref<Msg[]>([])
const suggestions = ref<string[]>([])
const lastSuggested = ref<string[]>([])
const chatBox = ref<HTMLElement>()

// 当前会话
const currentSessionId = ref<number | undefined>(undefined)

// 历史会话列表
const historyDrawerVisible = ref(false)
const historyLoading = ref(false)
const sessions = ref<QaSession[]>([])
const historyTotal = ref(0)
const historyQuery = reactive({ page: 1, size: 20 })

function fmtTime(t?: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}

/** 打开历史抽屉 */
async function openHistory() {
  historyDrawerVisible.value = true
  await loadSessions()
}

/** 加载会话列表 */
async function loadSessions() {
  historyLoading.value = true
  try {
    const res = await listQaSessions({ ...historyQuery })
    sessions.value = res.list
    historyTotal.value = res.total
  } finally {
    historyLoading.value = false
  }
}

/** 新建对话 */
async function newSession() {
  const s = await createQaSession()
  currentSessionId.value = s.id
  messages.value = []
  lastSuggested.value = []
  // 新会话暂不放入列表(无消息);首条提问后由 loadSessions 拉回
  historyDrawerVisible.value = false
  scrollBottom()
}

/** 切换到某个历史会话(返回之前的对话窗口) */
async function switchSession(id: number) {
  try {
    const list: QaSessionMessage[] = await getQaSession(id)
    currentSessionId.value = id
    messages.value = []
    list.forEach((m) => {
      messages.value.push({ role: 'user', text: m.question })
      messages.value.push({ role: 'ai', text: m.answer, sources: m.sources })
    })
    lastSuggested.value = []
    historyDrawerVisible.value = false
    scrollBottom()
  } catch (e: any) {
    ElMessage.error('加载会话失败:' + (e.message || '请稍后重试'))
  }
}

/** 删除某个会话 */
async function removeSession(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该对话窗口吗?该会话的全部问答将被清除,操作不可恢复。', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await deleteQaSession(id)
  ElMessage.success('已删除')
  // 若删的是当前会话,则新建一个空会话作为当前
  if (currentSessionId.value === id) {
    await newSession()
  }
  await loadSessions()
}

/** 全部清空 */
async function clearHistory() {
  await ElMessageBox.confirm('确定清空全部历史对话吗?此操作不可恢复。', '清空确认', {
    type: 'warning',
    confirmButtonText: '清空',
    cancelButtonText: '取消'
  })
  await clearQaHistory()
  ElMessage.success('历史已清空')
  historyQuery.page = 1
  await loadSessions()
  await newSession()
}

/** 发送问题 */
async function send(question?: string) {
  const q = (question ?? input.value).trim()
  if (!q || asking.value) return
  // 无当前会话则自动新建一个
  if (!currentSessionId.value) {
    await newSession()
  }
  input.value = ''
  messages.value.push({ role: 'user', text: q })
  asking.value = true
  try {
    const resp = await qaAsk(q, currentSessionId.value)
    messages.value.push({
      role: 'ai',
      text: resp.answer,
      sources: resp.sources,
      relatedMatters: resp.relatedMatters,
      suggested: resp.suggestedQuestions
    })
    lastSuggested.value = resp.suggestedQuestions || []
    // 刷新会话列表(更新标题/消息数/最后活跃时间)
    await loadSessions()
  } catch (e: any) {
    messages.value.push({ role: 'ai', text: '抱歉,问答服务暂时不可用:' + (e.message || '请稍后重试') })
  } finally {
    asking.value = false
    scrollBottom()
  }
}

function scrollBottom() {
  nextTick(() => {
    if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
  })
}

onMounted(async () => {
  suggestions.value = await qaSuggestions()
  await loadSessions()
  // 若存在历史会话,默认加载最近一个;否则不预建(提问时自动新建)
  if (sessions.value.length) {
    await switchSession(sessions.value[0].id)
  }
})
</script>

<style scoped>
.qa-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 4px 0 24px;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.qa-sub {
  margin: 4px 0 0;
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.6;
}
.head-btns {
  display: flex;
  gap: 8px;
}
.chat-card {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 228px);
}
.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px;
}
.welcome {
  text-align: center;
  padding: 40px 0;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-top: 16px;
}
.chip {
  cursor: pointer;
}
.msg-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.msg-row.user {
  flex-direction: row-reverse;
}
.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}
.bubble {
  max-width: 72%;
  background: #f5f7fa;
  border-radius: 8px;
  padding: 10px 14px;
}
.msg-row.user .bubble {
  background: #eaf1ff;
}
.msg-text {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #303133;
}
.src-block {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #e0e3e9;
}
.src-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.src-item {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  font-size: 13px;
}
.src-name {
  color: #2f6bff;
}
.src-snippet {
  font-size: 12px;
}
.rel-block {
  margin-top: 10px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 13px;
}
.rel-tag {
  cursor: pointer;
}
.input-area {
  border-top: 1px solid #ebeef5;
  padding-top: 10px;
}
.suggest-line {
  margin-bottom: 8px;
}
.input-row {
  display: flex;
  gap: 8px;
}
.sess-wrap {
  min-height: 200px;
}
.sess-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}
.sess-item:hover {
  background: #f5f7fa;
}
.sess-item.active {
  background: #eaf1ff;
}
.sess-main {
  flex: 1;
  min-width: 0;
}
.sess-title {
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sess-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}
.sess-del {
  flex-shrink: 0;
}
.his-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  margin-top: 4px;
}
</style>
