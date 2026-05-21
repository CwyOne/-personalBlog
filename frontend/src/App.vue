<template>
  <router-view />
  <BackToTop />
</template>

<script setup>
import { watch } from 'vue'
import BackToTop from '@/components/BackToTop.vue'
import { siteOwner, fetchSiteInfo } from '@/store/site'
import '@/store/theme'

fetchSiteInfo()

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
