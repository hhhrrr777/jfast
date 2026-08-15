import type { Directive, DirectiveBinding } from 'vue'
import { hasPermi } from '@/store/permission'

/**
 * 按钮级权限指令:v-hasPermi="['system:user:add']" 或 v-hasPermi="'system:user:add'"。
 * 无权限则直接移除元素(不渲染);仅做展示裁剪,安全边界在后端注解。
 */
export const hasPermiDirective: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    if (!binding.value) return
    if (!hasPermi(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  }
}
