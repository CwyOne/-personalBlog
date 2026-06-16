import { ref, computed } from 'vue'

function getStoredUser() {
  try { return JSON.parse(sessionStorage.getItem('user') || '{}') }
  catch { return {} }
}

const currentUser = ref(getStoredUser())
const isLoggedIn = ref(!!sessionStorage.getItem('token'))

export function setUser(user) {
  currentUser.value = user
  isLoggedIn.value = true
  sessionStorage.setItem('user', JSON.stringify(user))
}

export function updateUser(patch) {
  currentUser.value = { ...currentUser.value, ...patch }
  sessionStorage.setItem('user', JSON.stringify(currentUser.value))
}

export function clearUser() {
  currentUser.value = {}
  isLoggedIn.value = false
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('user')
}

export const isAdmin = computed(() => currentUser.value.role === 'admin')

export { currentUser, isLoggedIn }
