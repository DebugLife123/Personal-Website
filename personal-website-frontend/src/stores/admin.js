import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAdminStore = defineStore('admin', () => {
  const sidebarCollapsed = ref(false)
  const sidebarWidth = computed(() => (sidebarCollapsed.value ? '64px' : '220px'))

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return { sidebarCollapsed, sidebarWidth, toggleSidebar }
})
