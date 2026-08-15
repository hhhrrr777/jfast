<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteUser,
  listAllRoles,
  listUsers,
  type RoleVO,
  type UserQuery,
  type UserVO
} from '@/api/system'

/**
 * 用户管理列表页:搜索区 + 表格 + 分页;操作按钮经 v-hasPermi 裁剪;
 * 新增/编辑跳独立表单页(ADR-0003 静态隐式路由 /system/user/form/:id?)。
 */

const router = useRouter()

const loading = ref(false)
const total = ref(0)
const rows = ref<UserVO[]>([])
const roleOptions = ref<RoleVO[]>([])
const selected = ref<UserVO[]>([])

const query = reactive<UserQuery>({
  pageNum: 1,
  pageSize: 10,
  userName: '',
  nickName: '',
  status: ''
})

async function load() {
  loading.value = true
  try {
    const res = await listUsers(query)
    total.value = res.data.total
    rows.value = res.data.rows
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function loadRoles() {
  try {
    const res = await listAllRoles()
    roleOptions.value = res.data.data
  } catch {
    // 角色下拉加载失败不阻塞列表
  }
}

loadRoles()

function roleNames(row: UserVO): string {
  if (!row.roleIds || row.roleIds.length === 0) return '—'
  return row.roleIds
    .map((id) => roleOptions.value.find((r) => r.roleId === id)?.roleName)
    .filter(Boolean)
    .join('、')
}

function search() {
  query.pageNum = 1
  load()
}

function resetQuery() {
  query.userName = ''
  query.nickName = ''
  query.status = ''
  search()
}

function toAdd() {
  router.push('/system/user/form')
}

function toEdit(row: UserVO) {
  router.push(`/system/user/form/${row.userId}`)
}

async function handleDelete(rows: UserVO[]) {
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${rows.length} 个用户?`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteUser(rows.map((r) => r.userId))
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function handleResetPwd(row: UserVO) {
  let password: string
  try {
    const result = await ElMessageBox.prompt(`重置用户「${row.userName}」的密码`, '重置密码', {
      inputPattern: /^.{5,20}$/,
      inputErrorMessage: '密码长度必须在 5 到 20 个字符之间'
    })
    password = result.value
  } catch {
    return
  }
  try {
    const { resetPassword } = await import('@/api/system')
    await resetPassword(row.userId, password)
    ElMessage.success('重置成功')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '重置失败')
  }
}
</script>

<template>
  <el-card>
    <el-form inline :model="query" class="search-bar" @submit.prevent>
      <el-form-item label="账号">
        <el-input v-model="query.userName" placeholder="登录账号" clearable @keyup.enter="search" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="query.nickName" placeholder="昵称" clearable @keyup.enter="search" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button v-hasPermi="['system:user:add']" type="primary" @click="toAdd">新增</el-button>
      <el-button
        v-hasPermi="['system:user:remove']"
        type="danger"
        :disabled="selected.length === 0"
        @click="handleDelete(selected)"
      >
        批量删除
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="rows"
      row-key="userId"
      @selection-change="(vals: UserVO[]) => (selected = vals)"
    >
      <el-table-column type="selection" width="48" />
      <el-table-column prop="userId" label="ID" width="70" />
      <el-table-column prop="userName" label="登录账号" min-width="120" />
      <el-table-column prop="nickName" label="昵称" min-width="120" />
      <el-table-column label="角色" min-width="160">
        <template #default="{ row }">{{ roleNames(row) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'danger'">
            {{ row.status === '0' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="loginIp" label="最后登录IP" width="130" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button v-hasPermi="['system:user:edit']" link type="primary" @click="toEdit(row)">编辑</el-button>
          <el-button v-hasPermi="['system:user:resetPwd']" link type="warning" @click="handleResetPwd(row)">
            重置密码
          </el-button>
          <el-button v-hasPermi="['system:user:remove']" link type="danger" @click="handleDelete([row])">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      :page-sizes="[10, 20, 50]"
      class="pager"
      @current-change="load"
      @size-change="search"
    />
  </el-card>
</template>

<style scoped>
.search-bar {
  margin-bottom: 4px;
}

.toolbar {
  margin-bottom: 12px;
}

.pager {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
