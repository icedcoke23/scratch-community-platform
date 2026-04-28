import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ProjectCard from '@/components/ProjectCard.vue'
import LoadingSkeleton from '@/components/LoadingSkeleton.vue'
import EmptyState from '@/components/EmptyState.vue'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import type { Project } from '@/types'

const mockProject: Project = {
  id: 1,
  userId: 1,
  username: 'testuser',
  nickname: '测试用户',
  title: '我的 Scratch 项目',
  description: '这是一个测试项目的描述',
  coverUrl: '',
  status: 'published',
  blockCount: 10,
  likeCount: 5,
  commentCount: 3,
  viewCount: 100,
  tags: '动画,游戏',
  createdAt: '2026-04-24T12:00:00'
}

describe('ProjectCard', () => {
  it('渲染项目标题', () => {
    const wrapper = mount(ProjectCard, { props: { project: mockProject } })
    expect(wrapper.find('.card-title').text()).toBe('我的 Scratch 项目')
  })

  it('渲染项目描述（截断）', () => {
    const wrapper = mount(ProjectCard, { props: { project: mockProject } })
    expect(wrapper.find('.card-desc').text()).toContain('测试项目')
  })

  it('长描述应被截断到 100 字符', () => {
    const longDescProject = {
      ...mockProject,
      description: 'A'.repeat(150)
    }
    const wrapper = mount(ProjectCard, { props: { project: longDescProject } })
    const desc = wrapper.find('.card-desc').text()
    expect(desc.length).toBeLessThanOrEqual(103) // 100 + '...'
    expect(desc).toContain('...')
  })

  it('无描述时不应渲染描述区域', () => {
    const noDescProject = { ...mockProject, description: undefined }
    const wrapper = mount(ProjectCard, { props: { project: noDescProject } })
    expect(wrapper.find('.card-desc').exists()).toBe(false)
  })

  it('渲染点赞/评论/浏览数', () => {
    const wrapper = mount(ProjectCard, { props: { project: mockProject } })
    const stats = wrapper.find('.meta-stats').text()
    expect(stats).toContain('5')  // likeCount
    expect(stats).toContain('3')  // commentCount
    expect(stats).toContain('100') // viewCount
  })

  it('渲染作者头像首字母', () => {
    const wrapper = mount(ProjectCard, { props: { project: mockProject } })
    expect(wrapper.find('.avatar-sm').text()).toBe('测')
  })

  it('渲染标签', () => {
    const wrapper = mount(ProjectCard, { props: { project: mockProject } })
    const tags = wrapper.findAll('.tag')
    expect(tags).toHaveLength(2)
    expect(tags[0].text()).toBe('动画')
    expect(tags[1].text()).toBe('游戏')
  })

  it('无标签时不应渲染标签区域', () => {
    const noTagsProject = { ...mockProject, tags: undefined }
    const wrapper = mount(ProjectCard, { props: { project: noTagsProject } })
    expect(wrapper.find('.card-tags').exists()).toBe(false)
  })

  it('无封面时显示占位图', () => {
    const wrapper = mount(ProjectCard, { props: { project: mockProject } })
    expect(wrapper.find('.card-cover-placeholder').exists()).toBe(true)
    expect(wrapper.find('.card-cover').exists()).toBe(false)
  })

  it('有封面时显示图片', () => {
    const withCover = { ...mockProject, coverUrl: 'https://example.com/cover.png' }
    const wrapper = mount(ProjectCard, { props: { project: withCover } })
    expect(wrapper.find('.card-cover').exists()).toBe(true)
    expect(wrapper.find('.card-cover-placeholder').exists()).toBe(false)
  })

  it('点击应触发 click 事件', async () => {
    const wrapper = mount(ProjectCard, { props: { project: mockProject } })
    await wrapper.find('.project-card').trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })
})

describe('LoadingSkeleton', () => {
  it('渲染默认数量', () => {
    const wrapper = mount(LoadingSkeleton)
    expect(wrapper.find('.skeleton-container').exists()).toBe(true)
    // 默认 count=6，应有 6 个 skeleton-item
    const items = wrapper.findAll('.skeleton-item')
    expect(items.length).toBe(6)
  })

  it('渲染指定数量', () => {
    const wrapper = mount(LoadingSkeleton, { props: { count: 3 } })
    const items = wrapper.findAll('.skeleton-item')
    expect(items.length).toBeGreaterThanOrEqual(1)
  })
})

describe('EmptyState', () => {
  it('渲染默认内容', () => {
    const wrapper = mount(EmptyState)
    expect(wrapper.text()).toContain('暂无数据')
  })

  it('渲染自定义内容', () => {
    const wrapper = mount(EmptyState, {
      props: { icon: '🎨', text: '暂无作品' }
    })
    expect(wrapper.text()).toContain('🎨')
    expect(wrapper.text()).toContain('暂无作品')
  })

  it('支持默认 slot', () => {
    const wrapper = mount(EmptyState, {
      slots: { default: '<button>自定义操作</button>' }
    })
    expect(wrapper.find('button').exists()).toBe(true)
  })
})

describe('ErrorBoundary', () => {
  it('渲染默认 slot 内容', () => {
    const wrapper = mount(ErrorBoundary, {
      slots: { default: '<div class="child">子组件</div>' }
    })
    expect(wrapper.find('.child').exists()).toBe(true)
  })

  it('无错误时不显示错误 UI', () => {
    const wrapper = mount(ErrorBoundary, {
      slots: { default: '<div>正常内容</div>' }
    })
    expect(wrapper.text()).toContain('正常内容')
  })
})
