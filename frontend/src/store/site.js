import { ref } from 'vue'
import { getAbout } from '@/utils/api'

const siteOwner = ref({
  nickname: '子墨',
  avatar: '',
  email: '',
  createTime: '',
  bio: '',
  techStack: '[]',
  github: '',
  twitter: '',
  publicEmail: '',
  favicon: '',
  loginBg: ''
})

let fetched = false

export function fetchSiteInfo() {
  if (fetched) return Promise.resolve(siteOwner.value)
  return getAbout().then(res => {
    siteOwner.value = res.data
    fetched = true
    return siteOwner.value
  }).catch(() => {
    // Keep defaults, don't mark fetched so we can retry
    return siteOwner.value
  })
}

export function updateSiteInfo(data) {
  siteOwner.value = { ...siteOwner.value, ...data }
}

export function refreshSiteInfo() {
  fetched = false
  return fetchSiteInfo()
}

export { siteOwner }
