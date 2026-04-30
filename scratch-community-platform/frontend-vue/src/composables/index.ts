export function createLogger(name: string) {
  const prefix = `[${name}]`
  return {
    log: (...args: any[]) => console.log(prefix, ...args),
    warn: (...args: any[]) => console.warn(prefix, ...args),
    error: (...args: any[]) => console.error(prefix, ...args)
  }
}

export function useI18n() {
  return {
    t: (key: string) => {
      const messages: Record<string, string> = {
        'common.back': '返回',
        'common.cancel': '取消',
        'common.status': '状态',
        'editor.title': '项目标题',
        'editor.unsaved': '未保存',
        'editor.saved': '已保存',
        'editor.save': '保存',
        'editor.publish': '发布',
        'editor.import': '导入',
        'editor.export': '导出',
        'editor.exportSb3': '导出SB3',
        'editor.editMode': '编辑模式',
        'editor.previewMode': '预览模式',
        'editor.projectInfo': '项目信息',
        'editor.description': '项目描述',
        'editor.tags': '标签',
        'editor.blockCount': '积木数',
        'editor.complexity': '复杂度',
        'editor.createdAt': '创建时间',
        'editor.published': '已发布',
        'editor.draft': '草稿',
        'editor.saveChanges': '保存修改',
        'editor.loading': '正在加载编辑器...',
        'editor.loadingHint': '首次加载可能需要 3-5 秒',
        'editor.importedProject': '导入的项目',
        'editor.importSuccess': '项目导入成功',
        'editor.importFailed': '导入失败',
        'editor.importSb3Failed': '导入SB3文件失败',
        'editor.projectNotReady': '项目尚未准备好',
        'editor.savedMsg': '保存成功',
        'editor.saveFailed': '保存失败',
        'editor.createFailed': '创建失败',
        'editor.projectCreated': '项目创建成功',
        'editor.publishConfirm': '确定要发布这个项目吗？',
        'editor.publishTitle': '发布确认',
        'editor.publishedMsg': '发布成功',
        'editor.publishFailed': '发布失败',
        'editor.unsavedLeave': '项目尚未保存，确定要离开吗？',
        'editor.imageExportWip': '图片导出功能开发中',
        'editor.exportingSb3': '正在导出 SB3...',
        'editor.loadFailed': '加载项目失败',
        'confirm.title': '确认'
      }
      return messages[key] || key
    }
  }
}
