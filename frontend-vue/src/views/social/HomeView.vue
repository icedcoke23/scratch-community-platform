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
    <div v-if="stats" class="stats-section">
      <div class="stats-grid">
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
            <div class="stat-number">{{ stats.totalCourses || 42 }}</div>
            <div class="stat-label">精品课程</div>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon">⭐</div>
          <div class="stat-content">
            <div class="stat-number">{{ stats.totalLikes || 12580 }}</div>
            <div class="stat-label">点赞总数</div>
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

    <!-- 推荐课程 -->
    <div class="section">
      <div class="section-header">
        <h2 class="section-title">
          <span class="title-icon">📚</span>
          推荐课程
        </h2>
        <router-link to="/courses" class="more-link">查看更多 →</router-link>
      </div>
      <div class="course-grid">
        <div v-for="course in recommendedCourses" :key="course.id" class="course-card" @click="router.push(`/course/${course.id}`)">
          <div class="course-cover">
            <img :src="course.cover" :alt="course.title" />
            <div class="course-tag">{{ course.category }}</div>
          </div>
          <div class="course-info">
            <h3>{{ course.title }}</h3>
            <p class="course-desc">{{ course.description }}</p>
            <div class="course-meta">
              <span class="course-lessons">{{ course.lessons }} 课时</span>
              <span class="course-students">{{ course.students }} 人学习</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 热门作品排行 -->
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

    <!-- 资讯动态 -->
    <div class="section">
      <div class="section-header">
        <h2 class="section-title">
          <span class="title-icon">📰</span>
          资讯动态
        </h2>
        <router-link to="/news" class="more-link">查看更多 →</router-link>
      </div>
      <div class="news-grid">
        <div v-for="news in newsList" :key="news.id" class="news-card" @click="router.push(`/news/${news.id}`)">
          <div class="news-cover">
            <img :src="news.cover" :alt="news.title" />
          </div>
          <div class="news-info">
            <div class="news-date">{{ news.date }}</div>
            <h3>{{ news.title }}</h3>
            <p class="news-desc">{{ news.description }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/composables/useI18n'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/composables/useToast'
import Carousel from '@/components/Carousel.vue'
import ProjectCardEnhanced from '@/components/ProjectCardEnhanced.vue'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import EmptyState from '@/components/EmptyState.vue'
import { socialApi } from '@/api'

const router = useRouter()
const { t } = useI18n()
const userStore = useUserStore()
const toast = useToast()

interface Slide {
  image: string
  title: string
  description: string
  buttonText?: string
  link?: string
}

// 轮播图数据
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

// 统计数据
const stats = ref({
  totalUsers: 12580,
  publishedProjects: 4589,
  totalProjects: 8920,
  totalCourses: 42,
  totalLikes: 12580
})

// 精选作品
const featuredProjects = ref<any[]>([])
const loadingFeatured = ref(true)

// 热门作品
const hotProjects = ref<any[]>([])
const loadingHot = ref(true)

// 推荐课程
const recommendedCourses = ref([
  {
    id: 1,
    title: 'Scratch 入门到精通',
    description: '从零开始学习 Scratch 图形化编程',
    category: '入门',
    lessons: 24,
    students: 2345,
    cover: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=scratch%20course%20cover%20colorful&image_size=square'
  },
  {
    id: 2,
    title: 'Python 编程基础',
    description: '学习 Python 语言和海龟绘图',
    category: '进阶',
    lessons: 32,
    students: 1876,
    cover: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=python%20course%20cover%20programming&image_size=square'
  },
  {
    id: 3,
    title: '游戏开发实战',
    description: '使用 Scratch 开发精彩小游戏',
    category: '实战',
    lessons: 16,
    students: 987,
    cover: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=game%20development%20course%20cover&image_size=square'
  },
  {
    id: 4,
    title: '动画制作教程',
    description: '学习使用 Scratch 制作精美动画',
    category: '创意',
    lessons: 12,
    students: 756,
    cover: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=animation%20course%20cover%20scratch&image_size=square'
  }
])

// 资讯列表
const newsList = ref([
  {
    id: 1,
    title: 'Scratch 社区 2.0 全新升级',
    description: '全新界面，更多功能，更优体验',
    date: '2024-01-15',
    cover: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=software%20upgrade%20announcement%20banner&image_size=square'
  },
  {
    id: 2,
    title: '寒假编程大赛开始报名',
    description: '参与比赛，赢取丰厚奖品',
    date: '2024-01-10',
    cover: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=programming%20competition%20announcement&image_size=square'
  },
  {
    id: 3,
    title: '优秀教师招募计划',
    description: '加入我们，共同推动编程教育',
    date: '2024-01-05',
    cover: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=teacher%20recruitment%20program&image_size=square'
  }
])

// 处理轮播图按钮点击
const handleCarouselButtonClick = (slide: Slide) => {
  if (slide.link) {
    router.push(slide.link)
  }
}

// 打开编辑器
const openEditor = (type: string) => {
  if (type === 'scratch') {
    router.push('/editor')
  }
}

// 加载精选作品
const loadFeaturedProjects = async () => {
  try {
    loadingFeatured.value = true
    const result = await socialApi.getFeed({ sort: 'featured', limit: 4 })
    if (result && result.records) {
      featuredProjects.value = result.records
    }
  } catch (e) {
    console.error('加载精选作品失败', e)
  } finally {
    loadingFeatured.value = false
  }
}

// 加载热门作品
const loadHotProjects = async () => {
  try {
    loadingHot.value = true
    const result = await socialApi.getFeed({ sort: 'hot', limit: 4 })
    if (result && result.records) {
      hotProjects.value = result.records
    }
  } catch (e) {
    console.error('加载热门作品失败', e)
  } finally {
    loadingHot.value = false
  }
}

onMounted(() => {
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

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.course-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
  }
}

.course-cover {
  position: relative;
  height: 180px;
  overflow: hidden;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.course-tag {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(102, 126, 234, 0.95);
  color: white;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.course-info {
  padding: 20px;
}

.course-info h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #1a1a2e;
}

.course-desc {
  margin: 0 0 12px 0;
  color: #64748b;
  font-size: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.course-meta {
  display: flex;
  gap: 16px;
  color: #94a3b8;
  font-size: 13px;
}

.news-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.news-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 10px 28px rgba(0, 0, 0, 0.1);
  }
}

.news-cover {
  width: 140px;
  flex-shrink: 0;
  overflow: hidden;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.news-info {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.news-date {
  color: #94a3b8;
  font-size: 12px;
  margin-bottom: 8px;
}

.news-info h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #1a1a2e;
}

.news-desc {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
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
  
  .course-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 16px;
  }
  
  .news-grid {
    grid-template-columns: 1fr;
  }
  
  .news-card {
    flex-direction: column;
  }
  
  .news-cover {
    width: 100%;
    height: 180px;
  }
}
</style>
