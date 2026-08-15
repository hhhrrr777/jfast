<script setup lang="ts">
import { computed, defineAsyncComponent, shallowRef, watchEffect, type Component } from 'vue'

/**
 * 静态隐式表单页路由(ADR-0003)的入口分发器:
 * 路径 /:module/:entity/form/:id? 解析 views/<module>/<entity>/form.vue,
 * 存在则渲染该表单页(系统管理表单页与实体建模生成的表单页共用此约定),
 * 不存在则显示占位(实体域尚未生成的空工程形态)。
 */

const formModules = import.meta.glob('@/views/*/*/form.vue')

interface RouteParams {
  module: string
  entity: string
  id?: string
}

const props = defineProps<RouteParams>()

const component = shallowRef<Component | null>(null)

watchEffect(() => {
  const key = `/src/views/${props.module}/${props.entity}/form.vue`
  const loader = formModules[key] as (() => Promise<unknown>) | undefined
  component.value = loader ? defineAsyncComponent(loader as () => Promise<{ default: Component }>) : null
})

const isEdit = computed(() => !!props.id)

const placeholderTitle = computed(() => `${isEdit.value ? '编辑' : '新增'}${props.entity}`)
</script>

<template>
  <component :is="component" v-if="component" />
  <el-card v-else>
    <h2>{{ placeholderTitle }}</h2>
    <p>模块: {{ module }}</p>
    <p>实体: {{ entity }}</p>
    <p>模式: {{ isEdit ? `编辑(id=${id})` : '新增' }}</p>
  </el-card>
</template>
