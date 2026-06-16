import { onMounted, onUnmounted } from 'vue'

/**
 * Composable for scroll-triggered reveal animations.
 * Elements with class `.reveal-up` get `.visible` added when they enter the viewport.
 */
export function useRevealOnScroll() {
  function onScroll() {
    document.querySelectorAll('.reveal-up:not(.visible)').forEach(el => {
      if (el.getBoundingClientRect().top < window.innerHeight * 0.88) {
        el.classList.add('visible')
      }
    })
  }

  onMounted(() => {
    window.addEventListener('scroll', onScroll, { passive: true })
    // Trigger reveals for elements already in viewport
    requestAnimationFrame(onScroll)
  })

  onUnmounted(() => {
    window.removeEventListener('scroll', onScroll)
  })
}
