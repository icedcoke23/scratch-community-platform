<template>
  <nav class="breadcrumb" v-if="items.length > 1">
    <template v-for="(item, index) in items" :key="index">
      <router-link
        v-if="item.path && index < items.length - 1"
        :to="item.path"
        class="breadcrumb-link"
      >
        {{ item.label }}
      </router-link>
      <span v-else-if="index < items.length - 1" class="breadcrumb-link" @click="item.action?.()">
        {{ item.label }}
      </span>
      <span v-else class="breadcrumb-current">{{ item.label }}</span>
      <span v-if="index < items.length - 1" class="breadcrumb-separator">/</span>
    </template>
  </nav>
</template>

<script setup lang="ts">
export interface BreadcrumbItem {
  label: string
  path?: string
  action?: () => void
}

defineProps<{
  items: BreadcrumbItem[]
}>()
</script>

<style scoped>
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.breadcrumb-link {
  color: var(--text2);
  text-decoration: none;
  cursor: pointer;
  transition: color 0.15s;
}

.breadcrumb-link:hover {
  color: var(--primary);
}

.breadcrumb-separator {
  color: var(--text2);
  opacity: 0.5;
  user-select: none;
}

.breadcrumb-current {
  color: var(--text);
  font-weight: 500;
}

@media (max-width: 480px) {
  .breadcrumb {
    font-size: 12px;
    margin-bottom: 12px;
  }
}
</style>
