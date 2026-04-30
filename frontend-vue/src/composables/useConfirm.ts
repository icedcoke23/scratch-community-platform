import { ref } from 'vue'
import { ElMessageBox } from 'element-plus'

export interface ConfirmOptions {
  title?: string
  message: string
  confirmText?: string
  cancelText?: string
  type?: 'warning' | 'info' | 'success' | 'error'
  dangerouslyUseHTMLString?: boolean
}

/**
 * 确认对话框 composable
 */
export function useConfirm() {
  const loading = ref(false)

  async function confirm(options: ConfirmOptions): Promise<boolean> {
    loading.value = true
    try {
      await ElMessageBox.confirm(
        options.message,
        options.title || '提示',
        {
          confirmButtonText: options.confirmText || '确定',
          cancelButtonText: options.cancelText || '取消',
          type: options.type || 'warning',
          dangerouslyUseHTMLString: options.dangerouslyUseHTMLString
        }
      )
      return true
    } catch {
      return false
    } finally {
      loading.value = false
    }
  }

  async function confirmDelete(itemName = '此项'): Promise<boolean> {
    return confirm({
      message: `确定删除${itemName}？此操作不可撤销。`,
      title: '删除确认',
      type: 'error',
      confirmText: '删除',
      cancelText: '取消'
    })
  }

  async function confirmAction(action: string): Promise<boolean> {
    return confirm({
      message: `确定${action}？`,
      title: '操作确认',
      type: 'warning'
    })
  }

  return {
    loading,
    confirm,
    confirmDelete,
    confirmAction
  }
}
