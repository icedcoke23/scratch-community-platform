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
      <span>{{ link.label }}</span>
    </router-link>
  </nav>
</template>

<script setup lang="ts">
defineProps<{
  links: Array<{ path: string; label: string; icon: string }>
  currentPath: string
}>()
</script>
