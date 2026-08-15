<script setup lang="ts">
import { computed } from 'vue'

interface RouteParams {
  module: string
  entity: string
  id?: string
}

const props = defineProps<RouteParams>()

const isEdit = computed(() => !!props.id)
const title = computed(() => {
  const action = isEdit.value ? '编辑' : '新增'
  return `${action}${props.entity}`
})
</script>

<template>
  <div class="entity-form">
    <h2>{{ title }}</h2>

    <el-card class="form-card">
      <p>模块: {{ module }}</p>
      <p>实体: {{ entity }}</p>
      <p>模式: {{ isEdit ? `编辑(id=${id})` : '新增' }}</p>

    </el-card>
  </div>
</template>

<style scoped>
.entity-form {
  max-width: 800px;
}

.form-card {
  margin-top: 16px;
}
</style>
