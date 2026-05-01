<template>
  <div class="course-detail-view">
    <LoadingSkeleton v-if="loading" :count="3" variant="block" />
    <template v-else>
      <div class="course-hero">
        <div class="hero-content">
          <div class="course-badge" :class="course.difficulty">{{ getDifficultyLabel(course.difficulty) }}</div>
          <h1 class="course-title">{{ course.title }}</h1>
          <p class="course-desc">{{ course.description }}</p>
          <div class="course-meta">
            <div class="meta-item">
              <span class="icon">📚</span>
              <span>{{ course.lessons }} 课时</span>
            </div>
            <div class="meta-item">
              <span class="icon">👥</span>
              <span>{{ course.students }} 人学习</span>
            </div>
            <div class="meta-item">
              <span class="icon">★</span>
              <span>{{ course.rating }} 评分</span>
            </div>
          </div>
          <div class="course-actions">
            <el-button type="primary" size="large" class="enroll-btn">
              {{ course.price === 0 ? '立即学习' : `¥${course.price} 立即购买` }}
            </el-button>
            <el-button size="large" class="bookmark-btn">
              ♥ 收藏
            </el-button>
          </div>
        </div>
        <div class="hero-cover">
          <img :src="course.cover" :alt="course.title" />
        </div>
      </div>

      <div class="course-content">
        <div class="content-main">
          <div class="section">
            <h2>课程介绍</h2>
            <div class="course-intro">
              <p>本课程专为编程初学者设计，通过生动有趣的案例，循序渐进地教授编程基础。</p>
              <p>你将学会：</p>
              <ul>
                <li>✓ 掌握基本的编程概念和思维方式</li>
                <li>✓ 熟练使用编程工具进行创作</li>
                <li>✓ 独立完成多个有趣的项目</li>
                <li>✓ 培养解决问题的能力</li>
              </ul>
            </div>
          </div>

          <div class="section">
            <h2>课程大纲</h2>
            <div class="course-chapters">
              <div v-for="(chapter, index) in course.chapters" :key="index" class="chapter-item">
                <div class="chapter-header">
                  <div class="chapter-number">第 {{ index + 1 }} 章</div>
                  <div class="chapter-title">{{ chapter.title }}</div>
                  <div class="chapter-lessons">{{ chapter.lessons.length }} 课时</div>
                </div>
                <div class="chapter-lessons-list">
                  <div v-for="(lesson, lessonIndex) in chapter.lessons" :key="lessonIndex" class="lesson-item">
                    <span class="lesson-icon">{{ lesson.completed ? '✅' : '⏯️' }}</span>
                    <span class="lesson-title">{{ lesson.title }}</span>
                    <span class="lesson-duration">{{ lesson.duration }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="content-sidebar">
          <div class="sidebar-card instructor-card">
            <h3>讲师</h3>
            <div class="instructor-info">
              <img :src="course.instructor.avatar" :alt="course.instructor.name" class="instructor-avatar" />
              <div class="instructor-details">
                <div class="instructor-name">{{ course.instructor.name }}</div>
                <div class="instructor-title">{{ course.instructor.title }}</div>
                <div class="instructor-stats">
                  <span>{{ course.instructor.courses }} 门课程</span>
                  <span>{{ course.instructor.totalStudents }} 名学生</span>
                </div>
              </div>
            </div>
          </div>

          <div class="sidebar-card outline-card">
            <h3>课程亮点</h3>
            <ul class="outline-list">
              <li>📹 高清视频讲解</li>
              <li>💻 在线编程练习</li>
              <li>📱 随时随地学习</li>
              <li>👨‍🏫 专业答疑服务</li>
              <li>🏆 完成证书</li>
            </ul>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)

const course = ref({
  id: 1,
  title: 'Scratch 入门到精通',
  description: '从零开始学习 Scratch 图形化编程，制作精彩小游戏，培养编程思维',
  category: '入门',
  difficulty: 'beginner',
  lessons: 24,
  students: 2345,
  rating: 5,
  price: 0,
  cover: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=scratch%20course%20cover%20colorful&image_size=square',
  instructor: {
    name: '张老师',
    title: '编程教育专家',
    avatar: 'https://trae-api-cn.mchost.guru/api/ide/v1/text-to-image?prompt=teacher%20avatar%20portrait&image_size=square',
    courses: 8,
    totalStudents: 12567
  },
  chapters: [
    {
      title: 'Scratch 基础入门',
      lessons: [
        { title: '认识 Scratch 界面', duration: '10分钟', completed: true },
        { title: '第一个程序：Hello World', duration: '15分钟', completed: true },
        { title: '角色移动与控制', duration: '20分钟', completed: false },
        { title: '舞台与背景设置', duration: '12分钟', completed: false }
      ]
    },
    {
      title: '编程基础概念',
      lessons: [
        { title: '事件与触发机制', duration: '18分钟', completed: false },
        { title: '变量与数据类型', duration: '22分钟', completed: false },
        { title: '条件判断语句', duration: '20分钟', completed: false },
        { title: '循环结构', duration: '18分钟', completed: false }
      ]
    },
    {
      title: '项目实战',
      lessons: [
        { title: '弹跳球小游戏', duration: '30分钟', completed: false },
        { title: '接苹果游戏', duration: '35分钟', completed: false },
        { title: '迷宫探险', duration: '40分钟', completed: false }
      ]
    }
  ]
})

const getDifficultyLabel = (difficulty: string) => {
  const labels: Record<string, string> = {
    beginner: '入门',
    intermediate: '进阶',
    advanced: '高级'
  }
  return labels[difficulty] || difficulty
}

onMounted(() => {
  setTimeout(() => {
    loading.value = false
  }, 800)
})
</script>

<style scoped lang="less">
.course-detail-view {
  min-height: 100vh;
}

.course-hero {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60px 20px;
  display: flex;
  gap: 40px;
  max-width: 1200px;
  margin: 0 auto;
  align-items: center;
}

.hero-content {
  flex: 1;
  color: white;
}

.course-badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.2);

  &.beginner {
    background: rgba(72, 187, 120, 0.3);
  }

  &.intermediate {
    background: rgba(246, 173, 85, 0.3);
  }

  &.advanced {
    background: rgba(252, 129, 129, 0.3);
  }
}

.course-title {
  font-size: 36px;
  margin: 0 0 12px 0;
  font-weight: 700;
}

.course-desc {
  font-size: 18px;
  margin: 0 0 24px 0;
  opacity: 0.9;
  line-height: 1.6;
}

.course-meta {
  display: flex;
  gap: 32px;
  margin-bottom: 28px;

  .meta-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;

    .icon {
      font-size: 18px;
    }
  }
}

.course-actions {
  display: flex;
  gap: 16px;

  .enroll-btn {
    padding: 14px 32px;
    font-size: 16px;
    font-weight: 600;
    border-radius: 10px;
    background: #10b981;
    border: none;

    &:hover {
      background: #059669;
    }
  }

  .bookmark-btn {
    padding: 14px 24px;
    border-radius: 10px;
  }
}

.hero-cover {
  width: 400px;
  flex-shrink: 0;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);

  img {
    width: 100%;
    height: 300px;
    object-fit: cover;
  }
}

.course-content {
  display: flex;
  gap: 40px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
}

.content-main {
  flex: 1;
}

.content-sidebar {
  width: 340px;
  flex-shrink: 0;
}

.section {
  background: white;
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);

  h2 {
    font-size: 24px;
    margin: 0 0 20px 0;
    color: #1a1a2e;
  }
}

.course-intro {
  color: #64748b;
  line-height: 1.8;
  font-size: 15px;

  p {
    margin: 0 0 16px 0;
  }

  ul {
    margin: 16px 0;
    padding-left: 20px;

    li {
      margin-bottom: 8px;
    }
  }
}

.course-chapters {
  .chapter-item {
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    margin-bottom: 16px;
    overflow: hidden;
  }

  .chapter-header {
    background: #f8fafc;
    padding: 16px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .chapter-number {
      color: #667eea;
      font-weight: 600;
      font-size: 14px;
    }

    .chapter-title {
      flex: 1;
      margin: 0 16px;
      font-weight: 600;
      color: #1a1a2e;
    }

    .chapter-lessons {
      color: #94a3b8;
      font-size: 14px;
    }
  }

  .chapter-lessons-list {
    padding: 0;
  }

  .lesson-item {
    display: flex;
    align-items: center;
    padding: 14px 20px;
    border-top: 1px solid #f1f5f9;
    gap: 12px;
    cursor: pointer;
    transition: background 0.2s ease;

    &:hover {
      background: #f8fafc;
    }

    .lesson-icon {
      font-size: 18px;
    }

    .lesson-title {
      flex: 1;
      color: #1a1a2e;
    }

    .lesson-duration {
      color: #94a3b8;
      font-size: 14px;
    }
  }
}

.sidebar-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);

  h3 {
    margin: 0 0 16px 0;
    font-size: 18px;
    color: #1a1a2e;
  }
}

.instructor-info {
  display: flex;
  gap: 16px;
  align-items: center;
}

.instructor-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
}

.instructor-details {
  flex: 1;

  .instructor-name {
    font-weight: 600;
    color: #1a1a2e;
    margin-bottom: 4px;
  }

  .instructor-title {
    font-size: 14px;
    color: #64748b;
    margin-bottom: 8px;
  }

  .instructor-stats {
    display: flex;
    gap: 16px;
    font-size: 13px;
    color: #94a3b8;
  }
}

.outline-list {
  list-style: none;
  padding: 0;
  margin: 0;

  li {
    padding: 10px 0;
    color: #64748b;
    font-size: 15px;
  }
}

@media (max-width: 968px) {
  .course-hero {
    flex-direction: column;
    padding: 40px 20px;
  }

  .hero-cover {
    width: 100%;
    max-width: 400px;
  }

  .course-content {
    flex-direction: column;
  }

  .content-sidebar {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .course-title {
    font-size: 28px;
  }

  .course-actions {
    flex-direction: column;
  }
}
</style>
