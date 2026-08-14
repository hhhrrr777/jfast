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
  return `${'${'}action}${'${'}props.entity}`
})
</script>

<template>
  <div class="entity-form">
    <#if conditions.systemAdmin>
    <el-page-header :title="title" @back="$router.back()" />
    <#else>
    <h2>{{ title }}</h2>
    </#if>

    <el-card class="form-card">
      <p>模块: {{ module }}</p>
      <p>实体: {{ entity }}</p>
      <p>模式: {{ isEdit ? `编辑(id=${'${'}id})` : '新增' }}</p>

      <#if conditions.systemAdmin>
      <el-form label-width="80px">
        <el-form-item label="占位字段">
          <el-input placeholder="完整后台表单占位" />
        </el-form-item>
      </el-form>
      </#if>
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
