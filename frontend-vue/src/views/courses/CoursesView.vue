<template>
  <div class="courses-view">
    <div class="page-header">
      <h1>精品课程</h1>
      <p>从零开始，轻松学习编程</p>
    </div>

    <div class="filter-section">
      <div class="filter-tabs">
        <el-radio-group v-model="currentCategory" size="large" class="category-radio">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="beginner">入门</el-radio-button>
          <el-radio-button value="intermediate">进阶</el-radio-button>
          <el-radio-button value="advanced">高级</el-radio-button>
        </el-radio-group>
      </div>
      <div class="filter-sort">
        <el-select v-model="sortBy" placeholder="排序" size="large">
          <el-option label="最新" value="new" />
          <el-option label="热门" value="popular" />
          <el-option label="好评" value="rating" />
        </el-select>
      </div>
    </div>

    <div class="courses-grid">
      <LoadingSkeleton v-if="loading" :count="6" variant="card" />
      <template v-else>
        <div
          v-for="course in filteredCourses"
          :key="course.id"
          class="course-card"
          @click="router.push(`/course/${course.id}`)"
        >
          <div class="course-cover">
            <img :src="course.cover" :alt="course.title" />
            <div class="course-badge" :class="course.difficulty">{{ getDifficultyLabel(course.difficulty) }}</div>
          </div>
          <div class="course-content">
            <div class="course-meta">
              <span class="course-category">{{ course.category }}</span>
              <div class="course-rating">
                <span v-for="i in 5" :key="i" :class="{ active: i <= course.rating }">★</span>
                <span class="rating-count">{{ course.students }}</span>
              </div>
            </div>
            <h3 class="course-title">{{ course.title }}</h3>
            <p class="course-desc">{{ course.description }}</p>
            <div class="course-footer">
              <div class="course-stats">
                <span class="stat-item">
                  <span class="icon">📚</span>
                  {{ course.lessons }} 课时
                </span>
                <span class="stat-item">
                  <span class="icon">👥</span>
                  {{ course.students }} 人学习
                </span>
              </div>
              <div v-if="course.price === 0" class="course-price free">免费</div>
              <div v-else class="course-price">¥{{ course.price }}</div>
            </div>
          </div>
        </div>
        <EmptyState v-if="filteredCourses.length === 0" icon="📚" text="暂无课程" />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()

const currentCategory = ref('')
const sortBy = ref('popular')
const loading = ref(true)

const courses = ref([
  {
    id: 1,
    title: 'Scratch 入门到精通',
    description: '从零开始学习 Scratch 图形化编程，制作精彩小游戏',
    category: '入门',
    difficulty: 'beginner',
    lessons: 24,
    students: 2345,
    rating: 5,
    price: 0,
    cover: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=scratch%20course%20cover%20colorful&image_size=square'
  },
  {
    id: 2,
    title: 'Python 编程基础',
    description: '学习 Python 语言和海龟绘图，掌握编程基础',
    category: '进阶',
    difficulty: 'intermediate',
    lessons: 32,
    students: 1876,
    rating: 4,
    price: 99,
    cover: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=python%20course%20cover%20programming&image_size=square'
  },
  {
    id: 3,
    title: '游戏开发实战',
    description: '使用 Scratch 开发精彩小游戏，提升编程能力',
    category: '实战',
    difficulty: 'advanced',
    lessons: 16,
    students: 987,
    rating: 5,
    price: 199,
    cover: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=game%20development%20course%20cover&image_size=square'
  },
  {
    id: 4,
    title: '动画制作教程',
    description: '学习使用 Scratch 制作精美动画，培养创造力',
    category: '创意',
    difficulty: 'beginner',
    lessons: 12,
    students: 756,
    rating: 4,
    price: 0,
    cover: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=animation%20course%20cover%20scratch&image_size=square'
  },
  {
    id: 5,
    title: 'Web 前端开发',
    description: '学习 HTML、CSS、JavaScript，开发精美网页',
    category: '进阶',
    difficulty: 'intermediate',
    lessons: 48,
    students: 1234,
    rating: 5,
    price: 299,
    cover: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=web%20development%20course%20cover&image_size=square'
  },
  {
    id: 6,
    title: '人工智能入门',
    description: '了解人工智能基础知识，体验 AI 魅力',
    category: '高级',
    difficulty: 'advanced',
    lessons: 20,
    students: 567,
    rating: 4,
    price: 399,
    cover: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=artificial%20intelligence%20course%20cover&image_size=square'
  }
])

const getDifficultyLabel = (difficulty: string) => {
  const labels: Record<string, string> = {
    beginner: '入门',
    intermediate: '进阶',
    advanced: '高级'
  }
  return labels[difficulty] || difficulty
}

const filteredCourses = computed(() => {
  let result = [...courses.value]
  
  if (currentCategory.value) {
    result = result.filter(c => c.difficulty === currentCategory.value)
  }
  
  if (sortBy.value === 'popular') {
    result.sort((a, b) => b.students - a.students)
  } else if (sortBy.value === 'rating') {
    result.sort((a, b) => b.rating - a.rating)
  }
  
  return result
})

onMounted(() => {
  setTimeout(() => {
    loading.value = false
  }, 800)
})
</script>

<style scoped lang="less">
.courses-view {
  min-height: 100vh;
  padding: 40px 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;

  h1 {
    font-size: 36px;
    margin: 0 0 12px 0;
    color: #1a1a2e;
  }

  p {
    font-size: 18px;
    color: #64748b;
    margin: 0;
  }
}

.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  max-width: 1200px;
  margin-left: auto;
  margin-right: auto;
  flex-wrap: wrap;
  gap: 16px;
}

.courses-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  max-width: 1400px;
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
    transform: translateY(-8px);
    box-shadow: 0 16px 48px rgba(0, 0, 0, 0.1);
  }
}

.course-cover {
  position: relative;
  height: 200px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.course-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: white;

  &.beginner {
    background: linear-gradient(135deg, #48bb78 0%, #38a169 100%);
  }

  &.intermediate {
    background: linear-gradient(135deg, #f6ad55 0%, #ed8936 100%);
  }

  &.advanced {
    background: linear-gradient(135deg, #fc8181 0%, #f56565 100%);
  }
}

.course-content {
  padding: 20px;
}

.course-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.course-category {
  font-size: 13px;
  color: #667eea;
  font-weight: 600;
}

.course-rating {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;

  span {
    color: #cbd5e1;

    &.active {
      color: #fbbf24;
    }
  }

  .rating-count {
    color: #94a3b8;
    font-size: 12px;
    margin-left: 4px;
  }
}

.course-title {
  font-size: 18px;
  margin: 0 0 8px 0;
  color: #1a1a2e;
}

.course-desc {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 16px 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.course-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.course-stats {
  display: flex;
  gap: 16px;
  color: #94a3b8;
  font-size: 13px;

  .stat-item {
    display: flex;
    align-items: center;
    gap: 4px;

    .icon {
      font-size: 14px;
    }
  }
}

.course-price {
  font-size: 20px;
  font-weight: 700;
  color: #667eea;

  &.free {
    color: #48bb78;
  }
}

@media (max-width: 768px) {
  .courses-view {
    padding: 24px 16px;
  }

  .page-header h1 {
    font-size: 28px;
  }

  .courses-grid {
    grid-template-columns: 1fr;
  }
}
</style>
