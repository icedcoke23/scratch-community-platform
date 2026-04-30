<template>
  <div class="carousel-container" ref="carouselRef">
    <div class="carousel-track" :style="{ transform: `translateX(-${currentIndex * 100}%)` }">
      <div
        v-for="(slide, index) in slides"
        :key="index"
        class="carousel-slide"
      >
        <img :src="slide.image" :alt="slide.title" class="carousel-image">
        <div class="carousel-overlay">
          <h2 class="carousel-title">{{ slide.title }}</h2>
          <p class="carousel-description">{{ slide.description }}</p>
          <el-button v-if="slide.buttonText" type="primary" size="large" @click="handleButtonClick(slide)">
            {{ slide.buttonText }}
          </el-button>
        </div>
      </div>
    </div>
    
    <button class="carousel-btn prev-btn" @click="prev" :disabled="currentIndex === 0">
      <span class="btn-icon">‹</span>
    </button>
    <button class="carousel-btn next-btn" @click="next" :disabled="currentIndex === slides.length - 1">
      <span class="btn-icon">›</span>
    </button>
    
    <div class="carousel-dots">
      <button
        v-for="(slide, index) in slides"
        :key="index"
        class="carousel-dot"
        :class="{ active: index === currentIndex }"
        @click="goTo(index)"
        :aria-label="`跳转到第 ${index + 1} 张`"
      ></button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

defineOptions({ name: 'Carousel' })

interface Slide {
  image: string
  title: string
  description: string
  buttonText?: string
  link?: string
}

const props = defineProps<{
  slides: Slide[]
  autoplay?: boolean
  interval?: number
}>()

const emit = defineEmits<{
  (e: 'slide-change', index: number): void
  (e: 'button-click', slide: Slide): void
}>()

const currentIndex = ref(0)
let autoplayTimer: number | null = null

function next() {
  if (currentIndex.value < props.slides.length - 1) {
    currentIndex.value++
    emit('slide-change', currentIndex.value)
  }
}

function prev() {
  if (currentIndex.value > 0) {
    currentIndex.value--
    emit('slide-change', currentIndex.value)
  }
}

function goTo(index: number) {
  currentIndex.value = index
  emit('slide-change', currentIndex.value)
}

function handleButtonClick(slide: Slide) {
  emit('button-click', slide)
  if (slide.link) {
    window.open(slide.link, '_blank')
  }
}

function startAutoplay() {
  if (props.autoplay && props.slides.length > 1) {
    autoplayTimer = window.setInterval(() => {
      if (currentIndex.value < props.slides.length - 1) {
        currentIndex.value++
      } else {
        currentIndex.value = 0
      }
      emit('slide-change', currentIndex.value)
    }, props.interval || 5000)
  }
}

function stopAutoplay() {
  if (autoplayTimer) {
    clearInterval(autoplayTimer)
    autoplayTimer = null
  }
}

onMounted(() => {
  startAutoplay()
})

onUnmounted(() => {
  stopAutoplay()
})
</script>

<style scoped>
.carousel-container {
  position: relative;
  width: 100%;
  height: 400px;
  overflow: hidden;
  border-radius: 20px;
  margin-bottom: 32px;
  box-shadow: 0 8px 32px rgba(59, 130, 246, 0.15);
}

.carousel-track {
  display: flex;
  height: 100%;
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.carousel-slide {
  min-width: 100%;
  height: 100%;
  position: relative;
}

.carousel-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.carousel-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to right, rgba(0, 0, 0, 0.6) 0%, rgba(0, 0, 0, 0.3) 50%, transparent 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px;
  color: white;
}

.carousel-title {
  font-size: 36px;
  font-weight: 800;
  margin-bottom: 16px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.carousel-description {
  font-size: 18px;
  margin-bottom: 24px;
  max-width: 500px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
  line-height: 1.6;
}

.carousel-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.9);
  color: var(--primary);
  font-size: 32px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 10;
}

.carousel-btn:hover:not(:disabled) {
  background: white;
  transform: translateY(-50%) scale(1.1);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.carousel-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.prev-btn {
  left: 20px;
}

.next-btn {
  right: 20px;
}

.btn-icon {
  line-height: 1;
}

.carousel-dots {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 12px;
  z-index: 10;
}

.carousel-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.carousel-dot:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: scale(1.2);
}

.carousel-dot.active {
  background: white;
  width: 36px;
  border-radius: 6px;
}

@media (max-width: 768px) {
  .carousel-container {
    height: 280px;
    border-radius: 16px;
  }
  
  .carousel-overlay {
    padding: 24px;
  }
  
  .carousel-title {
    font-size: 24px;
  }
  
  .carousel-description {
    font-size: 14px;
  }
  
  .carousel-btn {
    width: 44px;
    height: 44px;
    font-size: 24px;
  }
  
  .prev-btn {
    left: 12px;
  }
  
  .next-btn {
    right: 12px;
  }
}

@media (max-width: 480px) {
  .carousel-container {
    height: 220px;
    border-radius: 12px;
  }
  
  .carousel-overlay {
    padding: 16px;
  }
  
  .carousel-title {
    font-size: 18px;
    margin-bottom: 8px;
  }
  
  .carousel-description {
    font-size: 12px;
    margin-bottom: 12px;
  }
}
</style>
