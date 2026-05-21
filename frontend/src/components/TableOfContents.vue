<template>
  <aside class="toc-sidebar" v-if="items.length > 0">
    <h4>目录</h4>
    <a
      v-for="item in items"
      :key="item.id"
      :class="['toc-link', 'level-' + item.level, { active: activeId === item.id }]"
      :href="'#' + item.id"
      :title="item.text"
      @click.prevent="scrollTo(item.id)"
    >
      {{ item.text }}
    </a>
  </aside>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'

const props = defineProps({
  containerSelector: { type: String, default: '.article-content' }
})

const items = ref([])
const activeId = ref('')

function buildToc() {
  const container = document.querySelector(props.containerSelector)
  if (!container) return
  const headings = container.querySelectorAll('h1, h2, h3')
  const list = []
  headings.forEach(h => {
    if (!h.id) {
      h.id = 'heading-' + Math.random().toString(36).substring(2, 8)
    }
    list.push({
      id: h.id,
      text: h.textContent,
      level: parseInt(h.tagName.charAt(1))
    })
  })
  items.value = list
}

function scrollTo(id) {
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
    activeId.value = id
  }
}

function onScroll() {
  const headings = items.value.map(i => document.getElementById(i.id)).filter(Boolean)
  for (let i = headings.length - 1; i >= 0; i--) {
    if (headings[i].getBoundingClientRect().top < 120) {
      activeId.value = items.value[i].id
      return
    }
  }
  activeId.value = ''
}

watch(() => props.containerSelector, () => {
  nextTick(buildToc)
})

onMounted(() => {
  nextTick(buildToc)
  window.addEventListener('scroll', onScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>
