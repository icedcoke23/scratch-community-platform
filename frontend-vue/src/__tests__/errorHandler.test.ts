import { describe, it, expect, beforeEach } from 'vitest'
import { addError, removeError, clearErrors, useErrors } from '@/utils/errorHandler'

describe('errorHandler', () => {
  beforeEach(() => {
    clearErrors()
  })

  describe('addError', () => {
    it('添加错误后列表增加', () => {
      addError('network', '网络错误')
      const { errors } = useErrors()
      expect(errors.value.length).toBe(1)
      expect(errors.value[0].type).toBe('network')
      expect(errors.value[0].message).toBe('网络错误')
    })

    it('不同类型错误都添加', () => {
      addError('network', '错误A')
      addError('auth', '错误B')
      addError('server', '错误C')
      const { errors } = useErrors()
      expect(errors.value.length).toBe(3)
    })

    it('重复消息在 3 秒内去重', () => {
      addError('network', '重复错误')
      addError('network', '重复错误')
      addError('network', '重复错误')
      const { errors } = useErrors()
      expect(errors.value.length).toBe(1)
    })

    it('超过最大数量移除最旧的', () => {
      for (let i = 0; i < 15; i++) {
        addError('network', `唯一错误 ${i}`)
      }
      const { errors } = useErrors()
      expect(errors.value.length).toBeLessThanOrEqual(10)
    })

    it('返回错误信息对象', () => {
      const result = addError('auth', '认证失败')
      expect(result).toBeDefined()
      expect(result.type).toBe('auth')
      expect(result.id).toBeGreaterThan(0)
    })
  })

  describe('removeError', () => {
    it('移除指定错误', () => {
      const error = addError('network', '要移除的')
      addError('auth', '保留的')
      removeError(error.id)
      const { errors } = useErrors()
      expect(errors.value.length).toBe(1)
      expect(errors.value[0].type).toBe('auth')
    })

    it('移除不存在的 ID 不报错', () => {
      expect(() => removeError(999)).not.toThrow()
    })
  })

  describe('clearErrors', () => {
    it('清除所有错误', () => {
      addError('network', '错误1')
      addError('auth', '错误2')
      clearErrors()
      const { errors } = useErrors()
      expect(errors.value.length).toBe(0)
    })

    it('清除后去重缓存也重置', () => {
      addError('network', '测试消息')
      clearErrors()
      addError('network', '测试消息') // 应该被添加，因为缓存已清除
      const { errors } = useErrors()
      expect(errors.value.length).toBe(1)
    })
  })

  describe('useErrors', () => {
    it('返回响应式错误列表', () => {
      const { errors } = useErrors()
      expect(errors.value).toBeDefined()
      expect(Array.isArray(errors.value)).toBe(true)
    })
  })
})
