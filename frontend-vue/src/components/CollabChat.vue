<template>
  <div class="collab-chat" :class="{ visible }">
    <div class="chat-header">
      <span>💬 协作聊天</span>
      <button class="close-btn" @click="$emit('close')">✕</button>
    </div>

    <div class="chat-messages" ref="messagesRef">
      <div
        v-for="(msg, i) in messages"
        :key="i"
        class="chat-msg"
        :class="{ own: msg.userId === currentUserId }"
      >
        <span class="msg-author">{{ msg.nickname }}</span>
        <span class="msg-text">{{ msg.message }}</span>
        <span class="msg-time">{{ formatTime(msg.timestamp) }}</span>
      </div>
      <div v-if="messages.length === 0" class="chat-empty">
        暂无消息，说点什么吧~
      </div>
    </div>

    <div class="chat-input">
      <input
        v-model="inputText"
        @keydown.enter="send"
        placeholder="输入消息..."
        maxlength="500"
      />
      <button @click="send" :disabled="!inputText.trim()">发送</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, computed } from 'vue'
import { useUserStore } from '@/stores/user'

const props = defineProps<{
  visible: boolean
  messages: Array<{ userId: number; nickname: string; message: string; timestamp: number }>
}>()

const emit = defineEmits<{
  send: [message: string]
  close: []
}>()

const userStore = useUserStore()
const currentUserId = computed(() => userStore.user?.id)
const inputText = ref('')
const messagesRef = ref<HTMLElement>()

function send() {
  const text = inputText.value.trim()
  if (text) {
    emit('send', text)
    inputText.value = ''
  }
}

function formatTime(ts: number) {
  const d = new Date(ts)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

// 自动滚动到底部
watch(() => props.messages.length, async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
})
</script>

<style scoped>
.collab-chat {
  display: none;
  flex-direction: column;
  width: 280px;
  border-left: 1px solid var(--border-color, #eee);
  background: var(--bg-primary, #fff);
}

.collab-chat.visible {
  display: flex;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.6rem 0.8rem;
  border-bottom: 1px solid var(--border-color, #eee);
  font-size: 0.85rem;
  font-weight: 600;
}

.close-btn {
  border: none;
  background: none;
  cursor: pointer;
  font-size: 1rem;
  color: #999;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 0.6rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.chat-msg {
  display: flex;
  flex-direction: column;
  max-width: 85%;
}

.chat-msg.own {
  align-self: flex-end;
  text-align: right;
}

.msg-author {
  font-size: 0.7rem;
  color: #999;
  margin-bottom: 0.1rem;
}

.msg-text {
  padding: 0.4rem 0.6rem;
  background: #f0f0f0;
  border-radius: 8px;
  font-size: 0.85rem;
  word-break: break-word;
}

.chat-msg.own .msg-text {
  background: #409eff;
  color: #fff;
}

.msg-time {
  font-size: 0.65rem;
  color: #ccc;
  margin-top: 0.1rem;
}

.chat-empty {
  text-align: center;
  color: #ccc;
  font-size: 0.8rem;
  padding: 2rem 0;
}

.chat-input {
  display: flex;
  gap: 0.4rem;
  padding: 0.6rem;
  border-top: 1px solid var(--border-color, #eee);
}

.chat-input input {
  flex: 1;
  padding: 0.4rem 0.6rem;
  border: 1px solid var(--border-color, #ddd);
  border-radius: 4px;
  font-size: 0.85rem;
  outline: none;
}

.chat-input input:focus {
  border-color: #409eff;
}

.chat-input button {
  padding: 0.4rem 0.8rem;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
}

.chat-input button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
