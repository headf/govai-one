<template>
  <div class="msg-page">
    <div class="msg-head">
      <el-alert
        title="消息中心:办结结果、退回补正、证照换发等业务提醒在此聚合,未读将标记高亮。"
        type="info"
        :closable="false"
        show-icon
      />
      <div class="msg-bar">
        <div class="msg-type-tabs">
          <el-radio-group v-model="type" @change="load">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="APPROVAL">办件通知</el-radio-button>
            <el-radio-button label="SYSTEM">系统</el-radio-button>
          </el-radio-group>
        </div>
        <el-badge :value="unread" :hidden="unread === 0" class="unread-badge">
          <el-button type="primary" plain @click="readAll">全部已读</el-button>
        </el-badge>
      </div>
    </div>

    <div v-loading="loading" class="msg-list">
      <el-card
        v-for="m in list"
        :key="m.id"
        class="msg-card"
        :class="{ unread: m.isRead === 0 }"
        shadow="hover"
      >
        <div class="msg-row">
          <el-tag size="small" :type="typeTag(m.msgType)">{{ typeName(m.msgType) }}</el-tag>
          <span class="msg-title" :class="{ bold: m.isRead === 0 }">{{ m.title }}</span>
          <span class="msg-time">{{ m.createTime?.replace('T', ' ') }}</span>
          <el-button v-if="m.isRead === 0" size="small" link type="primary" @click="markRead(m)">
            标为已读
          </el-button>
        </div>
        <div class="msg-content">{{ m.content }}</div>
      </el-card>
      <el-empty v-if="!list.length" description="暂无消息" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listEntMessages,
  entUnreadCount,
  markEntMessageRead,
  readAllEntMessages
} from '@/api'
import type { EntMessage } from '@/types'

const list = ref<EntMessage[]>([])
const loading = ref(false)
const type = ref('')
const unread = ref(0)

function typeName(t?: string) {
  return { APPROVAL: '办件通知', SYSTEM: '系统消息', POLICY: '政策推送' }[t || ''] || t || '通知'
}
function typeTag(t?: string) {
  return t === 'APPROVAL' ? 'primary' : 'info'
}

async function refreshUnread() {
  unread.value = await entUnreadCount()
}

async function load() {
  loading.value = true
  try {
    list.value = await listEntMessages(type.value || undefined)
  } finally {
    loading.value = false
  }
  refreshUnread()
}

async function markRead(m: EntMessage) {
  await markEntMessageRead(m.id)
  m.isRead = 1
  refreshUnread()
}

async function readAll() {
  await readAllEntMessages()
  ElMessage.success('已全部标记已读')
  load()
}

onMounted(load)
</script>

<style scoped>
.msg-page {
  display: grid;
  gap: 16px;
}
.msg-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
}
.msg-card {
  margin-bottom: 12px;
  border-left: 3px solid transparent;
}
.msg-card.unread {
  border-left-color: #2f6bff;
  background: #f7faff;
}
.msg-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.msg-title {
  flex: 1;
  color: #606266;
}
.msg-title.bold {
  color: #303133;
  font-weight: 600;
}
.msg-time {
  color: #909399;
  font-size: 12px;
}
.msg-content {
  margin-top: 8px;
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
}
</style>
