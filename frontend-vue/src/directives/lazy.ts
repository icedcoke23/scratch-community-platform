import type { Directive, DirectiveBinding } from 'vue'

/**
 * 图片懒加载指令
 * 
 * 用法：
 * <img v-lazy="imageUrl" alt="..." />
 * <img v-lazy="{ src: imageUrl, placeholder: '/placeholder.png' }" alt="..." />
 */

interface LazyOptions {
  src: string
  placeholder?: string
  error?: string
}

const PLACEHOLDER = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2YzZjRmNiIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWNhM2FmIiBmb250LXNpemU9IjE0Ij5Mb2FkaW5nLi4uPC90ZXh0Pjwvc3ZnPg=='
const ERROR_PLACEHOLDER = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2YzZjRmNiIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjOWNhM2FmIiBmb250LXNpemU9IjEyIj7lm77niYcyPC90ZXh0Pjwvc3ZnPg=='

const observerMap = new WeakMap<HTMLImageElement, IntersectionObserver>()

function loadImage(el: HTMLImageElement, src: string, errorSrc?: string) {
  const img = new Image()
  img.onload = () => {
    el.src = src
    el.classList.add('lazy-loaded')
  }
  img.onerror = () => {
    el.src = errorSrc || ERROR_PLACEHOLDER
    el.classList.add('lazy-error')
  }
  img.src = src
}

function getOptions(binding: DirectiveBinding): LazyOptions {
  if (typeof binding.value === 'string') {
    return { src: binding.value }
  }
  return binding.value
}

export const vLazy: Directive<HTMLImageElement, string | LazyOptions> = {
  mounted(el, binding) {
    const options = getOptions(binding)
    
    // 设置占位图
    el.src = options.placeholder || PLACEHOLDER
    el.classList.add('lazy-img')

    // 如果浏览器支持 IntersectionObserver
    if ('IntersectionObserver' in window) {
      const observer = new IntersectionObserver(
        (entries) => {
          entries.forEach(entry => {
            if (entry.isIntersecting) {
              loadImage(el, options.src, options.error)
              observer.unobserve(el)
              observerMap.delete(el)
            }
          })
        },
        { rootMargin: '100px' }
      )
      observer.observe(el)
      observerMap.set(el, observer)
    } else {
      // 降级：直接加载
      loadImage(el, options.src, options.error)
    }
  },

  updated(el, binding) {
    const options = getOptions(binding)
    const currentSrc = el.getAttribute('src')
    
    // 如果 src 变化，重新加载
    if (currentSrc !== options.src && !currentSrc?.startsWith('data:')) {
      loadImage(el, options.src, options.error)
    }
  },

  unmounted(el) {
    // 清理 observer
    const observer = observerMap.get(el)
    if (observer) {
      observer.disconnect()
      observerMap.delete(el)
    }
  }
}
