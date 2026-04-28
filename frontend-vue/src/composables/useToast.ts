import { ElMessage, ElNotification } from 'element-plus'

/**
 * 全局消息提示 composable
 * 统一的消息风格和快捷方法
 */
export function useToast() {
  /** 成功提示 */
  function success(message: string, duration = 3000) {
    ElMessage.success({ message, duration, showClose: true })
  }

  /** 错误提示 */
  function error(message: string, duration = 5000) {
    ElMessage.error({ message, duration, showClose: true })
  }

  /** 警告提示 */
  function warning(message: string, duration = 4000) {
    ElMessage.warning({ message, duration, showClose: true })
  }

  /** 信息提示 */
  function info(message: string, duration = 3000) {
    ElMessage.info({ message, duration, showClose: true })
  }

  /** 通知（右上角卡片） */
  function notify(options: {
    title: string
    message: string
    type?: 'success' | 'warning' | 'info' | 'error'
    duration?: number
  }) {
    ElNotification({
      title: options.title,
      message: options.message,
      type: options.type || 'info',
      duration: options.duration || 4500
    })
  }

  /** 成功通知 */
  function notifySuccess(title: string, message: string) {
    notify({ title, message, type: 'success' })
  }

  /** 错误通知 */
  function notifyError(title: string, message: string) {
    notify({ title, message, type: 'error', duration: 6000 })
  }

  /** 操作成功快捷提示 */
  function saved(name = '修改') {
    success(`${name}已保存`)
  }

  function deleted(name = '内容') {
    success(`${name}已删除`)
  }

  function created(name = '项目') {
    success(`${name}已创建`)
  }

  function copied() {
    success('已复制到剪贴板')
  }

  return {
    success,
    error,
    warning,
    info,
    notify,
    notifySuccess,
    notifyError,
    saved,
    deleted,
    created,
    copied
  }
}
