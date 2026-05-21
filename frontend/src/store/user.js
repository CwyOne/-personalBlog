import { ref, computed } from 'vue'

function getStoredUser() {
  try { return JSON.parse(localStorage.getItem('user') || '{}') }
  catch { return {} }
}

const currentUser = ref(getStoredUser())
const isLoggedIn = ref(!!localStorage.getItem('token'))

export function setUser(user) {
  currentUser.value = user
  isLoggedIn.value = true
  localStorage.setItem('user', JSON.stringify(user))
}

export function updateUser(patch) {
  currentUser.value = { ...currentUser.value, ...patch }
  localStorage.setItem('user', JSON.stringify(currentUser.value))
}

export function clearUser() {
  currentUser.value = {}
  isLoggedIn.value = false
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

export const isAdmin = computed(() => currentUser.value.role === 'admin')

export { currentUser, isLoggedIn }
