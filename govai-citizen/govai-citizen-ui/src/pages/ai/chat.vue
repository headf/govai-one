<template>
  <view class="page">
    <scroll-view class="msgs" scroll-y :scroll-into-view="scrollInto">
      <view v-if="!msgs.length" class="welcome">
        <view class="w-avatar">智</view>
        <text class="t">您好，我是智慧政务AI助手</text>
        <text class="d">可以问我办事流程、所需材料、办理进度等问题</text>
        <view class="suggests">
          <view v-for="s in suggests" :key="s" class="sg" hover-class="u-hover" @click="send(s)">{{ s }}</view>
        </view>
      </view>
      <view v-for="(m, i) in msgs" :key="i" :id="'msg-' + i" class="msg-row" :class="m.role">
        <view v-if="m.role === 'ai'" class="avatar">智</view>
        <view class="bubble">
          <rich-text v-if="m.role === 'ai' && m.text !== '正在思考...'" class="rt" :nodes="mdHtml(m.text)"></rich-text>
          <text v-else-if="m.role === 'ai' && m.text === '正在思考...'" class="thinking">正在思考<span>.</span><span>.</span><span>.</span></text>
          <text v-else>{{ m.text }}</text>
          <view v-if="m.role === 'ai' && m.matter" class="sm-card" hover-class="u-hover" @click="goMatter(m.matter.id)">
            <view class="sm-line">
              <text class="sm-label">为您找到可办事项</text>
              <text class="sm-go">去申报 ›</text>
            </view>
            <text class="sm-name">{{ m.matter.matterName }}</text>
          </view>
        </view>
      </view>
      <view id="bottom"></view>
    </scroll-view>

    <view class="input-bar">
      <input class="ipt" v-model="draft" placeholder="请输入您的问题..." confirm-type="send" @confirm="onSend" />
      <view class="send-btn" hover-class="u-hover" @click="onSend">发送</view>
    </view>
  </view>
</template>

<script>
import { aiChat, fetchMatters } from '../../api/index.js'

export default {
  data() {
    return {
      draft: '',
      msgs: [],
      sending: false,
      scrollInto: '',
      suggests: ['户口迁移需要哪些材料?', '社保关系怎么转移?', '医保报销流程是什么?']
    }
  },
  methods: {
    // 轻量 markdown → 富文本: 标题/列表/加粗转成结构化 HTML(rich-text 渲染), 避免星号井号露出来
    mdHtml(text) {
      const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      const inline = (s) => esc(s)
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        .replace(/(^|[^*])\*([^*]+?)\*/g, '$1<em>$2</em>')
      const PS = 'font-size:15px;line-height:1.7;margin:2px 0 5px;'
      const out = []
      String(text || '').replace(/\r/g, '').split('\n').forEach((raw) => {
        const t = raw.trim()
        if (!t) { out.push('<p style="' + PS + 'height:2px;">&nbsp;</p>'); return }
        const h = t.match(/^(#{1,3})\s+(.*)$/)
        if (h) { out.push('<p style="' + PS + 'font-weight:700;font-size:16px;">' + inline(h[2]) + '</p>'); return }
        const ul = t.match(/^[-*•]\s+(.*)$/)
        if (ul) { out.push('<p style="' + PS + '">•&nbsp;' + inline(ul[1]) + '</p>'); return }
        const ol = t.match(/^(\d+)[.、]\s*(.*)$/)
        if (ol) { out.push('<p style="' + PS + '">' + ol[1] + '.&nbsp;' + inline(ol[2]) + '</p>'); return }
        out.push('<p style="' + PS + '">' + inline(t) + '</p>')
      })
      return out.join('\n')
    },
    send(text) {
      this.draft = text
      this.onSend()
    },
    async onSend() {
      const text = this.draft.trim()
      if (!text || this.sending) return
      // 携带最近对话作为上下文, 让 AI 能理解前文(伪多轮)
      const hist = this.msgs
        .filter((m) => m.role === 'user' || (m.role === 'ai' && m.text !== '正在思考...'))
        .slice(-6)
        .map((m) => (m.role === 'user' ? '用户' : '助手') + '：' + m.text)
        .join('\n')
      const prompt = hist ? hist + '\n\n请结合以上对话回答我最新问题：' + text : text
      this.msgs.push({ role: 'user', text })
      this.draft = ''
      this.sending = true
      this.scrollToBottom()
      this.msgs.push({ role: 'ai', text: '正在思考...' })
      this.scrollToBottom()
      try {
        const data = await aiChat(prompt)
        const lastAi = this.msgs[this.msgs.length - 1]
        lastAi.text = data.response
        // 若问题命中事项, 在回答下给出直达办事卡
        if (!lastAi.matter) {
          lastAi.matter = await this.matchMatter(text)
        }
      } catch (e) {
        this.msgs[this.msgs.length - 1].text = '抱歉, 我暂时无法回答, 请稍后再试。'
      } finally {
        this.sending = false
        this.scrollToBottom()
      }
    },
    async matchMatter(q) {
      const kw = (q || '').replace(/[？?。.!！,\s]/g, '').slice(0, 12)
      if (!kw) return null
      try {
        const data = await fetchMatters({ page: 1, size: 1, keyword: kw })
        if (data.list && data.list.length) return data.list[0]
      } catch (e) { /* ignore */ }
      return null
    },
    goMatter(id) {
      uni.navigateTo({ url: '/pages/matter/detail?id=' + id })
    },
    scrollToBottom() {
      this.$nextTick(() => {
        this.scrollInto = 'bottom'
      })
    }
  }
}
</script>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
}
.msgs {
  flex: 1;
  padding: 20rpx;
  box-sizing: border-box;
}
.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 70rpx 40rpx 0;
}
.w-avatar {
  width: 128rpx;
  height: 128rpx;
  border-radius: 32rpx;
  background: var(--grad-primary);
  color: #fff;
  font-size: 64rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-btn);
}
.t {
  font-size: 32rpx;
  font-weight: 600;
  color: var(--t1);
  margin-top: 30rpx;
}
.d {
  font-size: 24rpx;
  color: var(--t3);
  margin-top: 14rpx;
  text-align: center;
}
.suggests {
  display: flex;
  flex-direction: column;
  margin-top: 44rpx;
  width: 100%;
}
.sg {
  background: var(--c-soft2);
  border: 1rpx solid var(--c-line);
  color: var(--c-primary);
  padding: 22rpx 30rpx;
  border-radius: 24rpx;
  font-size: 24rpx;
  margin: 10rpx 0;
  text-align: center;
}
.msg-row {
  display: flex;
  margin: 18rpx 0;
  align-items: flex-start;
}
.msg-row.user {
  justify-content: flex-end;
}
.avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 24rpx;
  background: var(--grad-primary);
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  flex-shrink: 0;
}
.bubble {
  max-width: 76%;
  padding: 20rpx 26rpx;
  font-size: 28rpx;
  line-height: 1.6;
  word-break: break-all;
}
.msg-row.ai .bubble {
  background: #fff;
  color: var(--t1);
  border-radius: 12rpx 22rpx 22rpx 22rpx;
  box-shadow: var(--shadow-card);
}
.rt {
  display: block;
  overflow-wrap: break-word;
  word-break: break-word;
}
.sm-card {
  margin-top: 18rpx;
  background: var(--c-soft2);
  border: 1rpx solid var(--c-line);
  border-radius: 24rpx;
  padding: 18rpx 22rpx;
}
.sm-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.sm-label {
  font-size: 24rpx;
  color: var(--c-primary);
}
.sm-go {
  font-size: 24rpx;
  color: var(--c-primary);
  font-weight: 600;
}
.sm-name {
  display: block;
  font-size: 28rpx;
  color: var(--t1);
  font-weight: 600;
  margin-top: 10rpx;
}
.msg-row.user .bubble {
  background: var(--grad-primary);
  color: #fff;
  border-radius: 24rpx 6rpx 22rpx 22rpx;
}
.input-bar {
  display: flex;
  align-items: center;
  padding: 16rpx 20rpx;
  background: #fff;
  border-top: 1rpx solid var(--c-div);
}
.ipt {
  flex: 1;
  background: var(--c-bg);
  border-radius: var(--r-pill);
  padding: 18rpx 28rpx;
  font-size: 28rpx;
  color: var(--t1);
}
.send-btn {
  background: var(--grad-primary);
  color: #fff;
  padding: 18rpx 36rpx;
  border-radius: var(--r-pill);
  font-size: 28rpx;
  margin-left: 16rpx;
  flex-shrink: 0;
}
</style>
