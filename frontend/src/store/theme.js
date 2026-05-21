import { ref, watch } from 'vue'

const stored = localStorage.getItem('theme')
const isDark = ref(stored === 'dark')

function apply() {
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}

apply()

watch(isDark, apply)

export function useTheme() {
  function toggle() {
    isDark.value = !isDark.value
  }
  return { isDark, toggle }
}
