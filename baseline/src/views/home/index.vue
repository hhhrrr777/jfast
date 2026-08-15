<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function openDemoForm() {
  router.push('/business/demo/form')
}
</script>

<template>
  <div class="page-center">
    <el-result title="jfast-baseline" sub-title="前端骨架已就绪">
      <template #extra>
        <el-button type="primary" @click="openDemoForm">打开示例表单页</el-button>
      </template>
    </el-result>
  </div>
</template>
