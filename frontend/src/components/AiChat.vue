<template>
  <div class="ai-chat-wrapper">
    <!-- Toggle button -->
    <button class="chat-fab" @click="toggleChat" :class="{ active: isOpen }">
      <svg v-if="!isOpen" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
      </svg>
      <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
        <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
      </svg>
    </button>

    <!-- Chat panel -->
    <transition name="panel-pop">
      <div v-if="isOpen" class="chat-panel">
        <!-- Header -->
        <div class="chat-header">
          <div class="header-info">
            <span class="header-dot"></span>
            <span class="header-title">小牛助手</span>
          </div>
          <div class="header-actions">
            <button class="header-btn" title="新对话" @click="createNewSession">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            </button>
            <button class="header-btn" :title="showSessionList ? '返回' : '历史'" @click="showSessionList = !showSessionList">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
                <template v-if="showSessionList">
                  <polyline points="15 18 9 12 15 6"/>
                </template>
                <template v-else>
                  <line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/>
                </template>
              </svg>
            </button>
          </div>
        </div>

        <!-- Session list -->
        <div v-if="showSessionList" class="session-list">
          <div v-if="sessions.length === 0" class="session-empty">
            <span class="empty-icon">💬</span>
            <span>暂无会话</span>
          </div>
          <div v-for="s in sessions" :key="s.sessionId" class="session-item" @click="loadSession(s)">
            <div class="session-info">
              <div class="session-title">{{ s.title }}</div>
              <div class="session-time">{{ formatTime(s.lastActiveTime) }}</div>
            </div>
            <button class="session-del" @click.stop="deleteSession(s.sessionId)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            </button>
          </div>
        </div>

        <!-- Chat messages -->
        <template v-else>
          <div class="chat-messages" ref="messagesRef">
            <div v-for="(msg, i) in messages" :key="i" :class="['msg', msg.role]">
              <div v-if="msg.role === 'assistant'" class="msg-avatar">🤖</div>
              <div class="msg-bubble">
                <div v-if="msg.role === 'assistant'" v-html="renderMarkdown(msg.content)" class="msg-text"></div>
                <div v-else class="msg-text">{{ msg.content }}</div>
              </div>
            </div>

            <!-- Loading skeleton -->
            <div v-if="loading" class="msg assistant">
              <div class="msg-avatar">🤖</div>
              <div class="msg-bubble msg-loading">
                <div class="typing-dots">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
          </div>

          <!-- Quick questions -->
          <div v-if="messages.length === 0 && !loading" class="quick-questions">
            <button v-for="q in suggestedQuestions" :key="q" class="quick-q" @click="sendMessage(q)">{{ q }}</button>
          </div>

          <!-- Input -->
          <div class="chat-input-area">
            <input
              v-model="inputText"
              placeholder="输入你的问题..."
              @keyup.enter="sendMessage()"
              :disabled="loading"
              class="chat-input"
            />
            <button class="send-btn" @click="sendMessage()" :disabled="loading || !inputText.trim()">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
              </svg>
            </button>
          </div>
        </template>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'

const isOpen = ref(false)
const inputText = ref('')
const loading = ref(false)
const messages = ref([])
const messagesRef = ref(null)
const showSessionList = ref(false)
const sessions = ref([])
const currentSessionId = ref('')

const suggestedQuestions = [
  '最近更新了什么？',
  '有没有 Spring Boot 的文章？',
  '介绍一下博主'
]

function getUserToken() {
  let token = localStorage.getItem('ai-chat-token')
  if (!token) {
    token = 'anon_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
    localStorage.setItem('ai-chat-token', token)
  }
  return token
}

function toggleChat() {
  isOpen.value = !isOpen.value
  if (isOpen.value && sessions.value.length === 0) loadSessions()
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  })
}

function formatTime(t) {
  if (!t) return ''
  const diff = Date.now() - new Date(t).getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return new Date(t).toLocaleDateString()
}

function renderMarkdown(text) {
  if (!text) return ''
  return text
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

// Session APIs
async function loadSessions() {
  try {
    const res = await fetch('/api/chat/sessions?userToken=' + getUserToken())
    sessions.value = await res.json()
  } catch (e) { console.error('加载会话失败', e) }
}

async function createNewSession() {
  try {
    const res = await fetch('/api/chat/sessions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userToken: getUserToken() })
    })
    const data = await res.json()
    currentSessionId.value = data.sessionId
    messages.value = []
    showSessionList.value = false
    loadSessions()
  } catch (e) { console.error('创建会话失败', e) }
}

async function loadSession(session) {
  currentSessionId.value = session.sessionId
  showSessionList.value = false
  try {
    const res = await fetch('/api/chat/sessions/' + session.sessionId + '/messages')
    const docs = await res.json()
    messages.value = docs.map(d => ({ role: d.role, content: d.content }))
    scrollToBottom()
  } catch (e) { console.error('加载消息失败', e) }
}

async function deleteSession(sessionId) {
  try {
    await fetch('/api/chat/sessions/' + sessionId, { method: 'DELETE' })
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = ''
      messages.value = []
    }
    loadSessions()
  } catch (e) { console.error('删除会话失败', e) }
}

// Send message
async function sendMessage(text) {
  const msg = typeof text === 'string' ? text : inputText.value.trim()
  if (!msg || loading.value) return
  inputText.value = ''

  if (!currentSessionId.value) {
    try {
      const res = await fetch('/api/chat/sessions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userToken: getUserToken() })
      })
      const data = await res.json()
      currentSessionId.value = data.sessionId
    } catch (e) { console.error('创建会话失败', e); return }
  }

  messages.value.push({ role: 'user', content: msg })
  loading.value = true
  scrollToBottom()

  let aiContent = ''
  messages.value.push({ role: 'assistant', content: '' })
  const aiMsg = messages.value[messages.value.length - 1]

  try {
    const response = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId: currentSessionId.value, userToken: getUserToken(), message: msg })
    })

    if (!response.ok) {
      aiMsg.content = '服务暂时不可用（' + response.status + '）'
      loading.value = false
      scrollToBottom()
      return
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      for (const line of lines) {
        const trimmed = line.trim()
        if (trimmed.startsWith('data:')) {
          const content = trimmed.substring(5).trimStart()
          if (content) {
            aiContent += content
            aiMsg.content = aiContent
            scrollToBottom()
          }
        }
      }
    }
    if (buffer.trim().startsWith('data:')) {
      const content = buffer.trim().substring(5).trimStart()
      if (content) {
        aiContent += content
        aiMsg.content = aiContent
      }
    }
  } catch (e) {
    aiMsg.content = '网络错误，请重试。'
  }

  loading.value = false
  scrollToBottom()
  loadSessions()
}

onMounted(() => { loadSessions() })
</script>

<style scoped>
.ai-chat-wrapper {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  font-family: var(--font-body, -apple-system, 'Segoe UI', sans-serif);
}

/* ============================================================
   FAB BUTTON
   ============================================================ */
.chat-fab {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: var(--indigo, #5b6abf);
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(91, 106, 191, 0.3);
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}

.chat-fab:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 28px rgba(91, 106, 191, 0.4);
}

.chat-fab.active {
  background: var(--gray-600, #555b6a);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

/* ============================================================
   PANEL
   ============================================================ */
.chat-panel {
  position: absolute;
  bottom: 64px;
  right: 0;
  width: 360px;
  height: 520px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(20px) saturate(1.5);
  -webkit-backdrop-filter: blur(20px) saturate(1.5);
  border-radius: 20px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.1), 0 0 0 1px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-pop-enter-active {
  transition: all 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

.panel-pop-leave-active {
  transition: all 0.2s ease-in;
}

.panel-pop-enter-from,
.panel-pop-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.95);
}

/* ============================================================
   HEADER
   ============================================================ */
.chat-header {
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.header-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 6px rgba(74, 222, 128, 0.4);
}

.header-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--gray-800, #252830);
}

.header-actions {
  display: flex;
  gap: 4px;
}

.header-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: transparent;
  border: none;
  color: var(--gray-400, #a8adb8);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.header-btn:hover {
  background: rgba(0, 0, 0, 0.04);
  color: var(--gray-700, #3a3f4b);
}

/* ============================================================
   SESSION LIST
   ============================================================ */
.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  gap: 8px;
  color: var(--gray-400, #a8adb8);
  font-size: 14px;
}

.empty-icon { font-size: 28px; }

.session-item {
  display: flex;
  align-items: center;
  padding: 12px 14px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.15s;
  gap: 10px;
}

.session-item:hover { background: rgba(0, 0, 0, 0.03); }

.session-info { flex: 1; min-width: 0; }

.session-title {
  font-size: 13px;
  color: var(--gray-700, #3a3f4b);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
}

.session-time {
  font-size: 11px;
  color: var(--gray-400, #a8adb8);
  margin-top: 2px;
}

.session-del {
  background: none;
  border: none;
  color: var(--gray-300, #d1d5de);
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.2s;
  flex-shrink: 0;
}

.session-del:hover { color: #f56c6c; background: rgba(245, 108, 108, 0.06); }

/* ============================================================
   MESSAGES
   ============================================================ */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.msg {
  display: flex;
  gap: 8px;
  max-width: 88%;
  animation: msgIn 0.3s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes msgIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.msg.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.msg.assistant { align-self: flex-start; }

.msg-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--gray-100, #f4f5f7);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  flex-shrink: 0;
}

.msg-bubble {
  padding: 10px 14px;
  font-size: 13.5px;
  line-height: 1.65;
  word-break: break-word;
}

/* Irregular bubble shapes */
.msg.user .msg-bubble {
  background: var(--indigo, #5b6abf);
  color: #fff;
  border-radius: 16px 4px 16px 16px;
}

.msg.assistant .msg-bubble {
  background: rgba(0, 0, 0, 0.03);
  color: var(--gray-700, #3a3f4b);
  border-radius: 4px 16px 16px 16px;
}

.msg-text a { color: inherit; text-decoration: underline; text-underline-offset: 2px; }
.msg.user .msg-text a { color: rgba(255, 255, 255, 0.9); }
.msg-text strong { font-weight: 600; }

/* Loading dots */
.msg-loading { padding: 12px 18px; }

.typing-dots {
  display: flex;
  gap: 5px;
  align-items: center;
}

.typing-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--gray-400, #a8adb8);
  animation: dotBounce 1.2s ease-in-out infinite;
}

.typing-dots span:nth-child(2) { animation-delay: 0.15s; }
.typing-dots span:nth-child(3) { animation-delay: 0.3s; }

@keyframes dotBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-5px); opacity: 1; }
}

/* ============================================================
   QUICK QUESTIONS
   ============================================================ */
.quick-questions {
  padding: 8px 16px 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.quick-q {
  padding: 6px 14px;
  background: rgba(91, 106, 191, 0.06);
  border: 1px solid rgba(91, 106, 191, 0.15);
  border-radius: 100px;
  font-size: 12px;
  font-family: inherit;
  color: var(--indigo, #5b6abf);
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.quick-q:hover {
  background: rgba(91, 106, 191, 0.12);
  border-color: rgba(91, 106, 191, 0.25);
}

/* ============================================================
   INPUT
   ============================================================ */
.chat-input-area {
  padding: 12px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  display: flex;
  gap: 8px;
  align-items: center;
}

.chat-input {
  flex: 1;
  border: 1.5px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 13.5px;
  font-family: inherit;
  color: var(--gray-700, #3a3f4b);
  background: rgba(0, 0, 0, 0.02);
  outline: none;
  transition: all 0.2s;
}

.chat-input::placeholder { color: var(--gray-400, #a8adb8); }
.chat-input:focus { border-color: rgba(91, 106, 191, 0.4); background: #fff; }

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--indigo, #5b6abf);
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) { background: var(--indigo-soft, #7884c9); }
.send-btn:disabled { opacity: 0.35; cursor: not-allowed; }

/* ============================================================
   DARK MODE
   ============================================================ */
:root[data-theme="dark"] .chat-panel {
  background: rgba(30, 32, 40, 0.88);
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.04);
}

:root[data-theme="dark"] .chat-header { border-bottom-color: rgba(255, 255, 255, 0.04); }
:root[data-theme="dark"] .header-title { color: var(--gray-800, #f0f1f4); }
:root[data-theme="dark"] .header-btn:hover { background: rgba(255, 255, 255, 0.06); }

:root[data-theme="dark"] .session-item:hover { background: rgba(255, 255, 255, 0.04); }
:root[data-theme="dark"] .session-title { color: var(--gray-700, #e5e7ed); }

:root[data-theme="dark"] .msg.assistant .msg-bubble { background: rgba(255, 255, 255, 0.06); color: var(--gray-700, #e5e7ed); }
:root[data-theme="dark"] .msg-avatar { background: rgba(255, 255, 255, 0.06); }

:root[data-theme="dark"] .chat-input {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
  color: var(--gray-700, #e5e7ed);
}

:root[data-theme="dark"] .chat-input:focus { background: rgba(255, 255, 255, 0.08); }
:root[data-theme="dark"] .chat-input-area { border-top-color: rgba(255, 255, 255, 0.04); }

:root[data-theme="dark"] .quick-q {
  background: rgba(91, 106, 191, 0.08);
  border-color: rgba(91, 106, 191, 0.2);
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 480px) {
  .ai-chat-wrapper { bottom: 16px; right: 16px; }

  .chat-panel {
    width: calc(100vw - 32px);
    height: 70vh;
    max-height: 520px;
    right: -8px;
    bottom: 58px;
  }

  .chat-fab { width: 46px; height: 46px; }
}

/* Scrollbar */
.chat-messages::-webkit-scrollbar,
.session-list::-webkit-scrollbar { width: 4px; }
.chat-messages::-webkit-scrollbar-thumb,
.session-list::-webkit-scrollbar-thumb { background: rgba(0, 0, 0, 0.08); border-radius: 2px; }
</style>
