<template>
  <nav class="mobile-nav hidden-desktop">
    <router-link
      v-for="link in links"
      :key="link.path"
      :to="link.path"
      class="mobile-nav-item"
      :class="{ active: currentPath === link.path }"
    >
      <span class="mobile-nav-icon">{{ link.icon }}</span>
      <span class="mobile-nav-label">{{ link.label }}</span>
    </router-link>
  </nav>
</template>

<script setup lang="ts">
defineProps<{
  links: Array<{ path: string; label: string; icon: string }>
  currentPath: string
}>()
</script>

<style scoped>
.mobile-nav {
  display: none;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: var(--card, #fff);
  border-top: 1px solid var(--border, #e2e8f0);
  z-index: 100;
  padding: 8px 0 env(safe-area-inset-bottom, 0);
  box-shadow: 0 -2px 12px rgba(0,0,0,0.06);
}

.mobile-nav {
  display: flex;
  justify-content: space-around;
  align-items: center;
}

.mobile-nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px 10px;
  font-size: 11px;
  color: var(--text2, #64748b);
  text-decoration: none;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  border-radius: 12px;
  min-width: 60px;
  position: relative;
}

.mobile-nav-item:active {
  transform: scale(0.92);
}

.mobile-nav-item.active {
  color: var(--primary, #3b82f6);
  background: var(--primary-bg, #eff6ff);
}

.mobile-nav-item.active::after {
  content: '';
  position: absolute;
  top: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  background: var(--primary, #3b82f6);
  border-radius: 2px;
}

.mobile-nav-icon {
  font-size: 24px;
  margin-bottom: 3px;
  transition: transform 0.2s ease;
}

.mobile-nav-item.active .mobile-nav-icon {
  transform: scale(1.15);
}

.mobile-nav-label {
  font-weight: 600;
  font-size: 11px;
  letter-spacing: 0.5px;
}

@media (max-width: 480px) {
  .mobile-nav-item {
    padding: 6px 6px;
    min-width: 52px;
  }
  .mobile-nav-icon { font-size: 22px; }
  .mobile-nav-label { font-size: 10px; }
}
</style>
