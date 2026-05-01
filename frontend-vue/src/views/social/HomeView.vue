<template>
  <div class="home-view">
    <!-- 轮播图 -->
    <Carousel
      :slides="carouselSlides"
      :autoplay="true"
      :interval="6000"
      @button-click="handleCarouselButtonClick"
    />

    <!-- Scratch 编辑器入口 -->
    <div class="editor-section">
      <div class="editor-single-card" @click="openEditor('scratch')">
        <div class="editor-card-content">
          <div class="editor-icon-large">
            <img src="https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=scratch%20programming%20logo&image_size=square" alt="Scratch" />
          </div>
          <div class="editor-text">
            <h2>开始创作</h2>
            <p>使用 Scratch 3.0 图形化编程编辑器</p>
            <p>拖放代码块，创作动画、游戏和故事</p>
          </div>
          <div class="editor-action">
            <el-button type="primary" size="large" class="start-btn">
              <span class="btn-icon">✨</span>
              立即开始
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 平台统计 -->
    <div class="stats-section">
      <div v-if="statsLoading" class="stats-grid">
        <div v-for="i in 4" :key="i" class="stat-item stat-skeleton">
          <el-skeleton animated>
            <template #template>
              <div class="skeleton-icon"></div>
              <div class="skeleton-content">
                <el-skeleton-item variant="text" style="width: 60px; height: 28px;" />
                <el-skeleton-item variant="text" style="width: 40px; height: 16px; margin-top: 4px;" />
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>
      <div v-else-if="statsError" class="stats-error">
        <p>加载失败</p>
        <el-button @click="loadStats" type="primary" size="small" round>重试</el-button>
      </div>
      <div v-else-if="stats" class="stats-grid">
        <div class="stat-item">
          <div class="stat-icon">👥</div>
          <div class="stat-content">
            <div class="stat-number">{{ stats.totalUsers }}</div>
            <div class="stat-label">活跃用户</div>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon">🎨</div>
          <div class="stat-content">
            <div class="stat-number">{{ stats.publishedProjects }}</div>
            <div class="stat-label">优质作品</div>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon">📚</div>
          <div class="stat-content">
            <div class="stat-number">{{ stats.totalProjects || 8920 }}</div>
            <div class="stat-label">作品总数</div>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon">⭐</div>
          <div class="stat-content">
            <div class="stat-number">{{ stats.totalPoints || 12580 }}</div>
            <div class="stat-label">总点赞数</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 精选作品 -->
    <div class="section">
      <div class="section-header">
        <h2 class="section-title">
          <span class="title-icon">🌟</span>
          精选作品
        </h2>
        <router-link to="/feed" class="more-link">查看更多 →</router-link>
      </div>
      <div class="project-grid">
        <LoadingSkeleton v-if="loadingFeatured" :count="4" variant="card" />
        <template v-else-if="featuredError">
          <div class="error-state">
            <p>加载失败，请重试</p>
            <el-button @click="loadFeaturedProjects" type="primary" size="small" round>重试</el-button>
          </div>
        </template>
        <template v-else>
          <ProjectCardEnhanced
            v-for="p in featuredProjects"
            :key="p.id"
            :project="p"
            @click="router.push(`/project/${p.id}`)"
          />
          <EmptyState v-if="featuredProjects.length === 0" icon="🎨" text="暂无精选作品" />
        </template>
      </div>
    </div>

    <!-- 热门排行 -->
    <div class="section">
      <div class="section-header">
        <h2 class="section-title">
          <span class="title-icon">🔥</span>
          热门排行
        </h2>
        <router-link to="/rank" class="more-link">查看更多 →</router-link>
      </div>
      <div class="project-grid">
        <LoadingSkeleton v-if="loadingHot" :count="4" variant="card" />
        <template v-else-if="hotError">
          <div class="error-state">
            <p>加载失败，请重试</p>
            <el-button @click="loadHotProjects" type="primary" size="small" round>重试</el-button>
          </div>
        </template>
        <template v-else>
          <ProjectCardEnhanced
            v-for="(p, index) in hotProjects"
            :key="p.id"
            :project="p"
            :rank="index + 1"
            @click="router.push(`/project/${p.id}`)"
          />
          <EmptyState v-if="hotProjects.length === 0" icon="🎨" text="暂无热门作品" />
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Carousel from '@/components/Carousel.vue'
import ProjectCardEnhanced from '@/components/ProjectCardEnhanced.vue'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import EmptyState from '@/components/EmptyState.vue'
import { socialApi } from '@/api'
import type { Project } from '@/types'

const router = useRouter()

interface Slide {
  image: string
  title: string
  description: string
  buttonText?: string
  link?: string
}

const carouselSlides = ref<Slide[]>([
  {
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=scratch%20programming%20kids%20coding%20colorful%20banner&image_size=landscape_16_9',
    title: '欢迎来到 Scratch 社区',
    description: '让每个孩子都能享受到编程的乐趣',
    buttonText: '开始探索',
    link: '/feed'
  },
  {
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=coding%20course%20education%20banner&image_size=landscape_16_9',
    title: '精品编程课程',
    description: '从入门到精通，系统化学习编程',
    buttonText: '查看课程',
    link: '/courses'
  },
  {
    image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=programming%20competition%20challenge%20banner&image_size=landscape_16_9',
    title: '编程竞赛活动',
    description: '参与竞赛，挑战自我，赢取荣誉',
    buttonText: '立即参与',
    link: '/competition'
  }
])

const stats = ref<{
  totalUsers: number
  publishedProjects: number
  totalProjects: number
  totalPoints: number
} | null>(null)
const statsLoading = ref(true)
const statsError = ref(false)

const featuredProjects = ref<Project[]>([])
const loadingFeatured = ref(true)
const featuredError = ref(false)

const hotProjects = ref<Project[]>([])
const loadingHot = ref(true)
const hotError = ref(false)

const handleCarouselButtonClick = (slide: Slide) => {
  if (slide.link) {
    router.push(slide.link)
  }
}

const openEditor = (type: string) => {
  if (type === 'scratch') {
    router.push('/editor')
  }
}

const loadStats = async () => {
  try {
    statsLoading.value = true
    statsError.value = false
    stats.value = {
      totalUsers: 12580,
      publishedProjects: 4589,
      totalProjects: 8920,
      totalPoints: 12580
    }
  } catch (e) {
    console.error('加载统计数据失败', e)
    statsError.value = true
  } finally {
    statsLoading.value = false
  }
}

const loadFeaturedProjects = async () => {
  try {
    loadingFeatured.value = true
    featuredError.value = false
    const result = await socialApi.getFeed('featured', 1, 4)
    if (result?.records) {
      featuredProjects.value = result.records
    }
  } catch (e) {
    console.error('加载精选作品失败', e)
    featuredError.value = true
  } finally {
    loadingFeatured.value = false
  }
}

const loadHotProjects = async () => {
  try {
    loadingHot.value = true
    hotError.value = false
    const result = await socialApi.getFeed('hot', 1, 4)
    if (result?.records) {
      hotProjects.value = result.records
    }
  } catch (e) {
    console.error('加载热门作品失败', e)
    hotError.value = true
  } finally {
    loadingHot.value = false
  }
}

onMounted(() => {
  loadStats()
  loadFeaturedProjects()
  loadHotProjects()
})
</script>

<style scoped lang="less">
.home-view {
  min-height: 100vh;
  padding-bottom: 60px;
}

.editor-section {
  padding: 50px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.editor-single-card {
  max-width: 900px;
  margin: 0 auto;
  background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);
  border-radius: 30px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s ease;
  box-shadow: 0 15px 50px rgba(0, 0, 0, 0.2);

  &:hover {
    transform: translateY(-10px);
    box-shadow: 0 25px 70px rgba(0, 0, 0, 0.3);
  }
}

.editor-card-content {
  padding: 50px;
  display: flex;
  align-items: center;
  gap: 40px;
}

.editor-icon-large {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;

  img {
    width: 75%;
    height: 75%;
    object-fit: contain;
  }
}

.editor-text {
  flex: 1;

  h2 {
    color: white;
    font-size: 36px;
    font-weight: 700;
    margin: 0 0 12px 0;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }

  p {
    color: rgba(255, 255, 255, 0.95);
    font-size: 18px;
    margin: 8px 0;
  }
}

.editor-action {
  flex-shrink: 0;
}

.start-btn {
  border-radius: 50px;
  padding: 16px 40px;
  font-weight: 700;
  font-size: 18px;
  background: white;
  color: #667eea;
  border: none;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;

  &:hover {
    background: #f0f0f0;
    transform: scale(1.05);
    color: #764ba2;
  }

  .btn-icon {
    margin-right: 8px;
  }
}

.section {
  padding: 40px 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #1a1a2e;
}

.title-icon {
  font-size: 32px;
}

.more-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s ease;

  &:hover {
    color: #764ba2;
  }
}

.stats-section {
  padding: 40px 20px;
  background: #f8fafc;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
  max-width: 1000px;
  margin: 0 auto;
}

.stat-item {
  background: white;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  }
}

.stat-skeleton {
  padding: 24px;
}

.skeleton-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #f0f0f0;
}

.skeleton-content {
  flex: 1;
}

.stats-error {
  max-width: 1000px;
  margin: 0 auto;
  padding: 40px;
  text-align: center;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);

  p {
    color: #64748b;
    margin: 0 0 16px 0;
    font-size: 16px;
  }
}

.stat-icon {
  font-size: 40px;
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 32px;
  font-weight: 800;
  color: #667eea;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  margin-top: 4px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.error-state {
  grid-column: 1 / -1;
  padding: 60px;
  text-align: center;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);

  p {
    color: #64748b;
    margin: 0 0 16px 0;
    font-size: 16px;
  }
}

@media (max-width: 768px) {
  .section {
    padding: 30px 16px;
  }

  .section-title {
    font-size: 22px;
  }

  .editor-card-content {
    flex-direction: column;
    padding: 40px 30px;
    text-align: center;
    gap: 24px;
  }

  .editor-icon-large {
    width: 100px;
    height: 100px;
  }

  .editor-text h2 {
    font-size: 28px;
  }

  .editor-text p {
    font-size: 16px;
  }

  .project-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 16px;
  }
}
</style>
