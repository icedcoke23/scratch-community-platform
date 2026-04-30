import { ref, computed } from 'vue'

export type Locale = 'zh-CN' | 'en'

const STORAGE_KEY = 'locale'

/** 当前语言 */
export const locale = ref<Locale>('zh-CN')

/** 翻译字典 */
const messages: Record<Locale, Record<string, string>> = {
  'zh-CN': {
    // 通用
    'common.loading': '加载中...',
    'common.empty': '暂无数据',
    'common.save': '保存',
    'common.cancel': '取消',
    'common.confirm': '确定',
    'common.delete': '删除',
    'common.edit': '编辑',
    'common.create': '创建',
    'common.search': '搜索',
    'common.back': '返回',
    'common.more': '加载更多',
    'common.submit': '提交',
    'common.publish': '发布',
    'common.export': '导出',
    'common.import': '导入',
    'common.share': '分享',
    'common.copy': '复制',
    'common.copied': '已复制',
    'common.status': '状态',

    // 导航
    'nav.feed': '社区',
    'nav.problems': '题库',
    'nav.competition': '竞赛',
    'nav.rank': '排行榜',
    'nav.class': '班级',
    'nav.points': '积分',
    'nav.homework': '作业',
    'nav.analytics': '学情',
    'nav.admin': '管理',
    'nav.notifications': '通知',
    'nav.search': '搜索',

    // 认证
    'auth.login': '登录',
    'auth.register': '注册',
    'auth.logout': '退出',
    'auth.username': '用户名',
    'auth.password': '密码',
    'auth.nickname': '昵称',
    'auth.email': '邮箱',
    'auth.role': '角色',
    'auth.student': '学生',
    'auth.teacher': '教师',
    'auth.loginSuccess': '登录成功',
    'auth.logoutSuccess': '已退出',
    'auth.expired': '登录已过期，请重新登录',

    // 社区
    'feed.title': '社区作品',
    'feed.latest': '最新',
    'feed.hot': '最热',
    'feed.empty': '还没有作品，快来发布第一个吧！',

    // 项目
    'project.detail': '项目详情',
    'project.like': '点赞',
    'project.comment': '评论',
    'project.remix': 'Remix',
    'project.aiReview': 'AI 点评',
    'project.share': '分享',
    'project.viewCount': '浏览',
    'project.published': '已发布',
    'project.draft': '草稿',

    // 编辑器
    'editor.title': '项目标题',
    'editor.save': '保存',
    'editor.saved': '已保存',
    'editor.unsaved': '未保存',
    'editor.publish': '发布',
    'editor.export': '导出',
    'editor.import': '导入',
    'editor.editMode': '编辑中',
    'editor.previewMode': '预览中',
    'editor.fullscreen': '全屏',
    'editor.loading': '正在加载 Scratch 编辑器...',
    'editor.loadingHint': '首次加载可能需要 10-20 秒',
    'editor.projectInfo': '项目信息',
    'editor.description': '项目描述',
    'editor.tags': '标签',
    'editor.tagsPlaceholder': '用逗号分隔，如：动画,游戏,入门',
    'editor.blockCount': '积木数',
    'editor.complexity': '复杂度',
    'editor.published': '已发布',
    'editor.draft': '草稿',
    'editor.createdAt': '创建时间',
    'editor.saveChanges': '保存修改',
    'editor.exportSb3': '导出 SB3 文件',
    'editor.exportImage': '导出封面图',
    'editor.exportingSb3': '正在导出 SB3 文件...',
    'editor.imageExportWip': '封面图导出功能开发中',
    'editor.projectCreated': '项目已创建',
    'editor.createFailed': '创建项目失败',
    'editor.savedMsg': '已保存',
    'editor.saveFailed': '保存失败',
    'editor.publishConfirm': '确定发布项目？发布后社区用户可以看到。',
    'editor.publishTitle': '发布项目',
    'editor.publishedMsg': '已发布',
    'editor.publishFailed': '发布失败',
    'editor.importSuccess': 'SB3 文件导入成功',
    'editor.importFailed': '导入失败',
    'editor.importSb3Failed': '导入 SB3 失败',
    'editor.projectNotReady': '项目未就绪',
    'editor.unsavedLeave': '有未保存的修改，确定离开？',
    'editor.leave': '离开',
    'editor.loadFailed': '加载项目失败',
    'editor.importedProject': '导入的项目',

    // 积分
    'points.title': '积分中心',
    'points.total': '总积分',
    'points.checkin': '每日签到 (+5 积分)',
    'points.checkinSuccess': '签到成功！',
    'points.checkedIn': '今日已签到',
    'points.rules': '积分规则',
    'points.ranking': '积分排行榜',
    'points.history': '积分记录',

    // 竞赛
    'competition.title': '竞赛中心',
    'competition.create': '创建竞赛',
    'competition.register': '报名参加',
    'competition.registered': '已报名',
    'competition.ranking': '实时排名',
    'competition.status.draft': '草稿',
    'competition.status.running': '进行中',
    'competition.status.published': '报名中',
    'competition.status.ended': '已结束',
    'competition.type.timed': '限时赛',
    'competition.type.rated': '排名赛',
    'competition.problems': '{count} 题',
    'competition.participants': '{count} 人',
    'competition.totalScore': '满分 {score}',
    'competition.remaining': '剩余 {h}h {m}m',
    'competition.createTitle': '标题',
    'competition.createDesc': '描述',
    'competition.createType': '类型',
    'competition.createStart': '开始时间',
    'competition.createEnd': '结束时间',
    'competition.createProblems': '题目 ID',
    'competition.createProblemsHint': '逗号分隔，如 1,2,3',
    'competition.fillAll': '请填写所有必填项',
    'competition.createSuccess': '创建成功',

    // 题库
    'problems.title': '题库',
    'problems.all': '全部',
    'problems.choice': '选择题',
    'problems.trueFalse': '判断题',
    'problems.scratch': 'Scratch 编程',
    'problems.difficulty': '难度',
    'problems.easy': '简单',
    'problems.medium': '中等',
    'problems.hard': '困难',
    'problems.submissions': '{count} 次提交',
    'problems.acceptRate': '通过率 {rate}%',
    'problems.submit': '提交答案',
    'problems.submitted': '已提交',
    'problems.result': '判题结果',
    'problems.ac': '通过',
    'problems.wa': '答案错误',
    'problems.tle': '超时',
    'problems.re': '运行错误',
    'problems.pending': '等待判题',
    'problems.empty': '暂无题目',

    // 作业
    'homework.title': '作业中心',
    'homework.empty': '暂无作业',
    'homework.deadline': '截止时间',
    'homework.submitted': '已提交',
    'homework.graded': '已批改',
    'homework.pending': '待提交',
    'homework.overdue': '已逾期',
    'homework.submit': '提交作业',
    'homework.score': '得分',
    'homework.comment': '教师评语',

    // 学情分析
    'analytics.title': '学情分析',
    'analytics.selectClass': '选择班级',
    'analytics.avgScore': '平均分',
    'analytics.submitRate': '提交率',
    'analytics.gradedRate': '批改率',

    // 404
    'notFound.desc': '页面走丢了，可能被 Scratch 小猫带跑了',
    'notFound.backHome': '回到首页',
    'notFound.goBack': '返回上页',

    // 通知
    'notification.title': '通知中心',
    'notification.empty': '暂无通知',
    'notification.markRead': '已读',
    'notification.markAllRead': '全部已读',

    // 管理
    'admin.title': '管理后台',
    'admin.totalUsers': '总用户',
    'admin.todayNew': '今日新增',
    'admin.totalProjects': '总项目',
    'admin.totalProblems': '总题目',
    'admin.totalSubmissions': '总提交',
    'admin.userManage': '用户管理',

    // 错误
    'error.network': '网络错误，请检查连接',
    'error.timeout': '请求超时，请检查网络',
    'error.server': '服务器错误，请稍后重试',
    'error.rateLimit': '请求过于频繁，请稍后再试',
    'error.unknown': '未知错误',

    // 设置
    'settings.title': '个人设置',
    'settings.profile': '基本信息',
    'settings.appearance': '外观设置',
    'settings.notifications': '通知设置',
    'settings.security': '账号安全',
    'settings.data': '数据管理',
    'settings.nickname': '昵称',
    'settings.email': '邮箱',
    'settings.bio': '个人简介',
    'settings.theme': '主题',
    'settings.language': '语言',
    'settings.light': '浅色',
    'settings.dark': '深色',
    'settings.auto': '跟随系统',
    'settings.changePassword': '修改密码',
    'settings.exportData': '导出数据',

    // 成就
    'achievements.title': '成就系统',
    'achievements.unlocked': '已解锁',
    'achievements.total': '总成就',
    'achievements.progress': '完成度',
    'achievements.all': '全部',
    'achievements.create': '创作',
    'achievements.social': '社交',
    'achievements.learn': '学习',
    'achievements.special': '特殊',

    // 面包屑
    'breadcrumb.home': '首页',

    // 虚拟列表
    'virtualList.loading': '加载中...',
    'virtualList.noMore': '没有更多了',

    // 确认对话框
    'confirm.title': '提示',
    'confirm.ok': '确定',
    'confirm.cancel': '取消',
    'confirm.delete': '删除确认',
    'confirm.deleteMessage': '确定删除？此操作不可撤销。',

    // 消息提示
    'toast.saved': '已保存',
    'toast.deleted': '已删除',
    'toast.created': '已创建',
    'toast.copied': '已复制到剪贴板',
  },

  'en': {
    // Common
    'common.loading': 'Loading...',
    'common.empty': 'No data',
    'common.save': 'Save',
    'common.cancel': 'Cancel',
    'common.confirm': 'Confirm',
    'common.delete': 'Delete',
    'common.edit': 'Edit',
    'common.create': 'Create',
    'common.search': 'Search',
    'common.back': 'Back',
    'common.more': 'Load more',
    'common.submit': 'Submit',
    'common.publish': 'Publish',
    'common.export': 'Export',
    'common.import': 'Import',
    'common.share': 'Share',
    'common.copy': 'Copy',
    'common.copied': 'Copied',
    'common.status': 'Status',

    // Navigation
    'nav.feed': 'Feed',
    'nav.problems': 'Problems',
    'nav.competition': 'Competition',
    'nav.rank': 'Ranking',
    'nav.class': 'Classes',
    'nav.points': 'Points',
    'nav.homework': 'Homework',
    'nav.analytics': 'Analytics',
    'nav.admin': 'Admin',
    'nav.notifications': 'Notifications',
    'nav.search': 'Search',

    // Auth
    'auth.login': 'Login',
    'auth.register': 'Register',
    'auth.logout': 'Logout',
    'auth.username': 'Username',
    'auth.password': 'Password',
    'auth.nickname': 'Nickname',
    'auth.email': 'Email',
    'auth.role': 'Role',
    'auth.student': 'Student',
    'auth.teacher': 'Teacher',
    'auth.loginSuccess': 'Login successful',
    'auth.logoutSuccess': 'Logged out',
    'auth.expired': 'Session expired, please login again',

    // Feed
    'feed.title': 'Community Projects',
    'feed.latest': 'Latest',
    'feed.hot': 'Popular',
    'feed.empty': 'No projects yet. Be the first to share!',

    // Project
    'project.detail': 'Project Details',
    'project.like': 'Like',
    'project.comment': 'Comment',
    'project.remix': 'Remix',
    'project.aiReview': 'AI Review',
    'project.share': 'Share',
    'project.viewCount': 'Views',
    'project.published': 'Published',
    'project.draft': 'Draft',

    // Editor
    'editor.title': 'Project Title',
    'editor.save': 'Save',
    'editor.saved': 'Saved',
    'editor.unsaved': 'Unsaved',
    'editor.publish': 'Publish',
    'editor.export': 'Export',
    'editor.import': 'Import',
    'editor.editMode': 'Editing',
    'editor.previewMode': 'Preview',
    'editor.fullscreen': 'Fullscreen',
    'editor.loading': 'Loading Scratch Editor...',
    'editor.loadingHint': 'First load may take 10-20 seconds',
    'editor.projectInfo': 'Project Info',
    'editor.description': 'Description',
    'editor.tags': 'Tags',
    'editor.tagsPlaceholder': 'Comma separated, e.g.: animation,game,beginner',
    'editor.blockCount': 'Block Count',
    'editor.complexity': 'Complexity',
    'editor.published': 'Published',
    'editor.draft': 'Draft',
    'editor.createdAt': 'Created At',
    'editor.saveChanges': 'Save Changes',
    'editor.exportSb3': 'Export SB3 File',
    'editor.exportImage': 'Export Cover Image',
    'editor.exportingSb3': 'Exporting SB3 file...',
    'editor.imageExportWip': 'Cover image export coming soon',
    'editor.projectCreated': 'Project created',
    'editor.createFailed': 'Failed to create project',
    'editor.savedMsg': 'Saved',
    'editor.saveFailed': 'Save failed',
    'editor.publishConfirm': 'Publish this project? It will be visible to the community.',
    'editor.publishTitle': 'Publish Project',
    'editor.publishedMsg': 'Published',
    'editor.publishFailed': 'Publish failed',
    'editor.importSuccess': 'SB3 file imported successfully',
    'editor.importFailed': 'Import failed',
    'editor.importSb3Failed': 'Failed to import SB3',
    'editor.projectNotReady': 'Project not ready',
    'editor.unsavedLeave': 'You have unsaved changes. Leave anyway?',
    'editor.leave': 'Leave',
    'editor.loadFailed': 'Failed to load project',
    'editor.importedProject': 'Imported Project',

    // Points
    'points.title': 'Points Center',
    'points.total': 'Total Points',
    'points.checkin': 'Daily Check-in (+5 points)',
    'points.checkinSuccess': 'Check-in successful!',
    'points.checkedIn': 'Already checked in today',
    'points.rules': 'Points Rules',
    'points.ranking': 'Points Ranking',
    'points.history': 'Points History',

    // Competition
    'competition.title': 'Competition Center',
    'competition.create': 'Create Competition',
    'competition.register': 'Register',
    'competition.registered': 'Registered',
    'competition.ranking': 'Live Ranking',
    'competition.status.draft': 'Draft',
    'competition.status.running': 'Running',
    'competition.status.published': 'Open',
    'competition.status.ended': 'Ended',
    'competition.type.timed': 'Timed',
    'competition.type.rated': 'Rated',
    'competition.problems': '{count} problems',
    'competition.participants': '{count} participants',
    'competition.totalScore': 'Total: {score}',
    'competition.remaining': '{h}h {m}m left',
    'competition.createTitle': 'Title',
    'competition.createDesc': 'Description',
    'competition.createType': 'Type',
    'competition.createStart': 'Start Time',
    'competition.createEnd': 'End Time',
    'competition.createProblems': 'Problem IDs',
    'competition.createProblemsHint': 'Comma separated, e.g. 1,2,3',
    'competition.fillAll': 'Please fill all required fields',
    'competition.createSuccess': 'Created successfully',

    // Problems
    'problems.title': 'Problems',
    'problems.all': 'All',
    'problems.choice': 'Choice',
    'problems.trueFalse': 'True/False',
    'problems.scratch': 'Scratch',
    'problems.difficulty': 'Difficulty',
    'problems.easy': 'Easy',
    'problems.medium': 'Medium',
    'problems.hard': 'Hard',
    'problems.submissions': '{count} submissions',
    'problems.acceptRate': 'Accept rate {rate}%',
    'problems.submit': 'Submit',
    'problems.submitted': 'Submitted',
    'problems.result': 'Result',
    'problems.ac': 'Accepted',
    'problems.wa': 'Wrong Answer',
    'problems.tle': 'Time Limit',
    'problems.re': 'Runtime Error',
    'problems.pending': 'Pending',
    'problems.empty': 'No problems',

    // Homework
    'homework.title': 'Homework',
    'homework.empty': 'No homework',
    'homework.deadline': 'Deadline',
    'homework.submitted': 'Submitted',
    'homework.graded': 'Graded',
    'homework.pending': 'Pending',
    'homework.overdue': 'Overdue',
    'homework.submit': 'Submit',
    'homework.score': 'Score',
    'homework.comment': 'Teacher Comment',

    // Analytics
    'analytics.title': 'Analytics',
    'analytics.selectClass': 'Select Class',
    'analytics.avgScore': 'Avg Score',
    'analytics.submitRate': 'Submit Rate',
    'analytics.gradedRate': 'Graded Rate',

    // 404
    'notFound.desc': 'Page not found. Maybe the Scratch cat took it away!',
    'notFound.backHome': 'Back to Home',
    'notFound.goBack': 'Go Back',

    // Notifications
    'notification.title': 'Notifications',
    'notification.empty': 'No notifications',
    'notification.markRead': 'Read',
    'notification.markAllRead': 'Mark all read',

    // Admin
    'admin.title': 'Admin Dashboard',
    'admin.totalUsers': 'Total Users',
    'admin.todayNew': 'Today New',
    'admin.totalProjects': 'Total Projects',
    'admin.totalProblems': 'Total Problems',
    'admin.totalSubmissions': 'Total Submissions',
    'admin.userManage': 'User Management',

    // Errors
    'error.network': 'Network error, please check your connection',
    'error.timeout': 'Request timeout, please check your network',
    'error.server': 'Server error, please try again later',
    'error.rateLimit': 'Too many requests, please slow down',
    'error.unknown': 'Unknown error',

    // Settings
    'settings.title': 'Settings',
    'settings.profile': 'Profile',
    'settings.appearance': 'Appearance',
    'settings.notifications': 'Notifications',
    'settings.security': 'Security',
    'settings.data': 'Data Management',
    'settings.nickname': 'Nickname',
    'settings.email': 'Email',
    'settings.bio': 'Bio',
    'settings.theme': 'Theme',
    'settings.language': 'Language',
    'settings.light': 'Light',
    'settings.dark': 'Dark',
    'settings.auto': 'Auto',
    'settings.changePassword': 'Change Password',
    'settings.exportData': 'Export Data',

    // Achievements
    'achievements.title': 'Achievements',
    'achievements.unlocked': 'Unlocked',
    'achievements.total': 'Total',
    'achievements.progress': 'Progress',
    'achievements.all': 'All',
    'achievements.create': 'Create',
    'achievements.social': 'Social',
    'achievements.learn': 'Learn',
    'achievements.special': 'Special',

    // Breadcrumb
    'breadcrumb.home': 'Home',

    // Virtual list
    'virtualList.loading': 'Loading...',
    'virtualList.noMore': 'No more items',

    // Confirm dialog
    'confirm.title': 'Confirm',
    'confirm.ok': 'OK',
    'confirm.cancel': 'Cancel',
    'confirm.delete': 'Delete Confirmation',
    'confirm.deleteMessage': 'Are you sure? This cannot be undone.',

    // Toast messages
    'toast.saved': 'Saved',
    'toast.deleted': 'Deleted',
    'toast.created': 'Created',
    'toast.copied': 'Copied to clipboard',
  }
}

/**
 * 国际化 composable
 */
export function useI18n() {
  /** 翻译函数 */
  function t(key: string, params?: Record<string, string | number>): string {
    let msg = messages[locale.value]?.[key] || messages['zh-CN']?.[key] || key
    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        msg = msg.replace(`{${k}}`, String(v))
      })
    }
    return msg
  }

  /** 切换语言 */
  function setLocale(l: Locale) {
    locale.value = l
    localStorage.setItem(STORAGE_KEY, l)
    document.documentElement.setAttribute('lang', l)
  }

  /** 切换到下一个语言 */
  function toggleLocale() {
    setLocale(locale.value === 'zh-CN' ? 'en' : 'zh-CN')
  }

  /** 初始化 */
  function initLocale() {
    const stored = localStorage.getItem(STORAGE_KEY) as Locale | null
    if (stored && ['zh-CN', 'en'].includes(stored)) {
      locale.value = stored
    }
    document.documentElement.setAttribute('lang', locale.value)
  }

  return {
    t,
    locale,
    setLocale,
    toggleLocale,
    initLocale
  }
}
