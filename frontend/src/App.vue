<template>
  <router-view />
  <BackToTop />
  <AiChat />
</template>

<script setup>
import { watch } from 'vue'
import BackToTop from '@/components/BackToTop.vue'
import AiChat from '@/components/AiChat.vue'
import { siteOwner, fetchSiteInfo } from '@/store/site'
import { setSiteName, getSiteName } from '@/utils/seo'
import '@/store/theme'

fetchSiteInfo()

// Sync site name → SEO + document title
watch(() => siteOwner.value.nickname, (name) => {
  setSiteName(name)
  const currentTitle = document.title
  const sName = getSiteName()
  if (currentTitle.includes(' — ')) {
    // Replace only the site name suffix, preserve the page title
    document.title = currentTitle.replace(/ — [^—]+$/, ` — ${sName}`)
  } else {
    // Bare site name (e.g. home page or admin)
    document.title = sName
  }
}, { immediate: true })

watch(() => siteOwner.value.favicon, (url) => {
  if (!url) return
  let link = document.getElementById('dynamic-favicon')
  if (!link) {
    link = document.createElement('link')
    link.id = 'dynamic-favicon'
    link.rel = 'icon'
    document.head.appendChild(link)
  }
  link.href = url
}, { immediate: true })
</script>
