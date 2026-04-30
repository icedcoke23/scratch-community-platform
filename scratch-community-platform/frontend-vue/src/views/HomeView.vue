<template>
  <div class="home">
    <header class="home-header">
      <h1>🎨 Scratch 社区平台</h1>
      <p class="subtitle">基于 TurboWarp 的 Scratch 创作与分享社区</p>
    </header>
    <div class="home-content">
      <div class="card">
        <h2>开始创作</h2>
        <p>点击下方按钮创建你的第一个 Scratch 项目</p>
        <router-link to="/editor/new">
          <button class="btn primary">创建新项目</button>
        </router-link>
      </div>
      <div class="card">
        <h2>功能特色</h2>
        <ul class="feature-list">
          <li>🚀 基于 TurboWarp 高性能编辑器</li>
          <li>📦 本地 iframe wrapper 集成</li>
          <li>📱 响应式设计</li>
          <li>💾 项目保存与管理</li>
          <li>🎮 项目预览与全屏</li>
        </ul>
      </div>
    </div>
    <footer class="home-footer">
      <p>Powered by TurboWarp &amp; Vue 3</p>
    </footer>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.home-header {
  padding: 60px 20px;
  text-align: center;
  color: white;
}

.home-header h1 {
  font-size: 48px;
  margin-bottom: 12px;
}

.subtitle {
  font-size: 18px;
  opacity: 0.9;
}

.home-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.card {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}

.card h2 {
  margin-bottom: 12px;
  color: #1e293b;
}

.card p {
  color: #64748b;
  margin-bottom: 20px;
}

.btn {
  padding: 12px 24px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.btn.primary {
  background: #3B82F6;
  color: white;
}

.btn.primary:hover {
  background: #2563eb;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.feature-list {
  list-style: none;
  color: #64748b;
}

.feature-list li {
  padding: 8px 0;
  font-size: 15px;
}

.home-footer {
  text-align: center;
  padding: 40px;
  color: rgba(255,255,255,0.8);
  margin-top: 20px;
}

@media (max-width: 768px) {
  .home-content {
    grid-template-columns: 1fr;
  }
  .home-header h1 {
    font-size: 32px;
  }
}
</style>
