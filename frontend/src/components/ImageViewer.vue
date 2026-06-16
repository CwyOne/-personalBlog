<template>
  <!-- 此组件无视觉输出，仅提供图片查看器功能 -->
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import Viewer from 'viewerjs'
import 'viewerjs/dist/viewer.css'

const props = defineProps({
  /** 容器选择器，用于查找需要绑定查看器的图片 */
  containerSelector: {
    type: String,
    default: '.article-content'
  },
  /** 是否启用查看器 */
  enabled: {
    type: Boolean,
    default: true
  }
})

const viewer = ref(null)

/**
 * 初始化查看器：查找容器内所有图片，绑定 viewerjs
 */
function initViewer() {
  // 销毁旧实例
  destroyViewer()

  if (!props.enabled) return

  const container = document.querySelector(props.containerSelector)
  if (!container) return

  // 为所有图片添加 cursor: zoom-in 样式提示
  const images = container.querySelectorAll('img')
  images.forEach(img => {
    img.style.cursor = 'zoom-in'
  })

  // 创建 viewerjs 实例
  viewer.value = new Viewer(container, {
    // 不自动显示，等待用户点击
    inline: false,
    // 按钮配置：关闭、缩放、旋转
    button: true,
    navbar: false,
    title: false,
    toolbar: {
      zoomIn: 1,
      zoomOut: 1,
      oneToOne: 1,
      reset: 1,
      rotateLeft: 1,
      rotateRight: 1,
      flipHorizontal: 1,
      flipVertical: 1
    },
    // 可移动
    movable: true,
    // 缩放比例
    zoomRatio: 0.2,
    // 最小/最大缩放
    minScale: 0.1,
    maxScale: 10,
    // 点击图片关闭（非空白处）
    hide: () => {
      // 查看器关闭后的回调
    },
    // 查看器显示时的回调
    show: () => {
      // 添加暗黑模式适配
      adaptTheme()
    }
  })
}

/**
 * 销毁查看器实例
 */
function destroyViewer() {
  if (viewer.value) {
    viewer.value.destroy()
    viewer.value = null
  }
}

/**
 * 适配当前主题（亮色/暗黑模式）
 */
function adaptTheme() {
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark'
  const viewerContainer = document.querySelector('.viewer-container')
  if (viewerContainer) {
    if (isDark) {
      viewerContainer.style.background = 'rgba(24, 26, 32, 0.92)'
    } else {
      viewerContainer.style.background = 'rgba(248, 249, 252, 0.92)'
    }
  }
}

/**
 * 监听内容变化，重新初始化查看器
 */
function observeContent() {
  const container = document.querySelector(props.containerSelector)
  if (!container) return

  const observer = new MutationObserver(() => {
    // 内容变化时重新初始化
    nextTick(() => {
      initViewer()
    })
  })

  observer.observe(container, {
    childList: true,
    subtree: true
  })

  return observer
}

let mutationObserver = null

onMounted(() => {
  // 延迟初始化，等待文章内容渲染完成
  setTimeout(() => {
    initViewer()
    mutationObserver = observeContent()
  }, 500)
})

onUnmounted(() => {
  destroyViewer()
  if (mutationObserver) {
    mutationObserver.disconnect()
  }
})

// 监听容器选择器变化，重新初始化
watch(() => props.containerSelector, () => {
  nextTick(() => initViewer())
})

// 暴露方法供父组件调用
defineExpose({
  initViewer,
  destroyViewer
})
</script>

<style scoped>
/* 查看器样式通过 viewerjs 自带 CSS 控制，此处仅做主题适配 */
:root[data-theme="dark"] :deep(.viewer-container) {
  background: rgba(24, 26, 32, 0.92) !important;
}

:root[data-theme="dark"] :deep(.viewer-navbar) {
  background: rgba(24, 26, 32, 0.8);
}

:root[data-theme="dark"] :deep(.viewer-title) {
  color: #e5e7ed;
}
</style>
