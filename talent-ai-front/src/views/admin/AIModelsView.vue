<script setup lang="ts">
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/dialog/style/css'
import 'element-plus/es/components/form/style/css'
import 'element-plus/es/components/form-item/style/css'
import 'element-plus/es/components/input/style/css'
import 'element-plus/es/components/select/style/css'
import 'element-plus/es/components/option/style/css'
import { ref, computed, onMounted } from 'vue'
import type { EChartsOption } from 'echarts'
import {
  Bot, Zap, BarChart2, Activity, Settings, Sparkles, CheckCircle,
  AlertCircle, Play, Pause, Loader2, Pencil, Trash2, Plus,
} from 'lucide-vue-next'
import {
  ElMessage, ElMessageBox, ElDialog, ElForm, ElFormItem,
  ElInput, ElSelect, ElOption,
} from 'element-plus'
import {
  listAiModels, createAiModel, updateAiModel, deleteAiModel,
  updateAiModelStatus, fetchModelStats, fetchUsageTrend,
  fetchUsageDistribution, modelTypeLabel,
  MODEL_TYPE_OPTIONS, MODEL_STATUS,
  type AiModelItem, type ModelStats, type UsageTrendItem,
  type UsageDistItem, type AiModelSaveRequest,
} from '@/api/adminAiModel'
import { getErrorMessage } from '@/utils/validators'

/* =================================================================
 *  响应式状态
 * ================================================================= */

const loading = ref(false)
const modelList = ref<AiModelItem[]>([])
const stats = ref<ModelStats>({
  activeModels: 0, todayCalls: 0, totalTokens: 0,
  estimatedCost: 0, avgLatency: 0,
})
const trendData = ref<UsageTrendItem[]>([])
const distData = ref<UsageDistItem[]>([])

/* =================================================================
 *  统计卡片
 * ================================================================= */

function formatNumber(n: number): string {
  return n.toLocaleString('zh-CN')
}

const statCards = computed(() => [
  { label: '活跃模型', value: stats.value.activeModels, icon: Bot, cls: 'text-brand-blue bg-brand-tint' },
  { label: '今日调用量', value: formatNumber(stats.value.todayCalls), icon: Zap, cls: 'text-brand-purple bg-brand-tint-2' },
  { label: 'Token消耗量', value: formatNumber(stats.value.totalTokens), icon: BarChart2, cls: 'text-brand-orange bg-orange-50' },
  { label: '估算费用', value: `¥${stats.value.estimatedCost.toFixed(2)}`, icon: Activity, cls: 'text-brand-green bg-green-50' },
])

/* =================================================================
 *  图表配置
 * ================================================================= */

const trendOption = computed<EChartsOption>(() => ({
  grid: { left: 48, right: 24, top: 16, bottom: 28 },
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'var(--card)',
    borderColor: 'var(--border)',
    borderWidth: 1,
    textStyle: { fontSize: 12 },
  },
  xAxis: {
    type: 'category',
    data: trendData.value.map(d => d.date),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#64748b', fontSize: 11 },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#e2e8f0', type: 'dashed' } },
    axisLabel: { color: '#64748b', fontSize: 11 },
  },
  series: [
    {
      type: 'line',
      name: '调用量',
      data: trendData.value.map(d => d.calls),
      smooth: true,
      showSymbol: false,
      lineStyle: { width: 2, color: '#3d8b7a' },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(37,99,235,0.25)' },
            { offset: 1, color: 'rgba(37,99,235,0)' },
          ],
        },
      },
    },
  ],
}))

const distOption = computed<EChartsOption>(() => ({
  grid: { left: 48, right: 24, top: 16, bottom: 28 },
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'var(--card)',
    borderColor: 'var(--border)',
    borderWidth: 1,
    textStyle: { fontSize: 12 },
  },
  xAxis: {
    type: 'category',
    data: distData.value.map(d => d.name),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#64748b', fontSize: 11 },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#e2e8f0', type: 'dashed' } },
    axisLabel: { color: '#64748b', fontSize: 11 },
  },
  series: [
    {
      type: 'bar',
      name: '调用次数',
      data: distData.value.map(d => d.count),
      barWidth: '40%',
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#6366f1' },
            { offset: 1, color: '#818cf8' },
          ],
        },
      },
    },
  ],
}))

/* =================================================================
 *  数据加载
 * ================================================================= */

async function loadModels() {
  loading.value = true
  try {
    const page = await listAiModels({ page: 1, size: 100 })
    modelList.value = page.records ?? []
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '加载模型列表失败'))
    modelList.value = []
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  loading.value = true
  try {
    const [pageData, statsData, trend, dist] = await Promise.all([
      listAiModels({ page: 1, size: 100 }),
      fetchModelStats(),
      fetchUsageTrend(),
      fetchUsageDistribution(),
    ])
    modelList.value = pageData.records ?? []
    stats.value = statsData
    trendData.value = trend ?? []
    distData.value = dist ?? []
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '加载数据失败'))
  } finally {
    loading.value = false
  }
}

/* =================================================================
 *  状态切换
 * ================================================================= */

async function handleToggleStatus(model: AiModelItem) {
  const newStatus = model.status === MODEL_STATUS.RUNNING ? 2 : 1
  const actionLabel = newStatus === 1 ? '启动' : '暂停'
  try {
    await updateAiModelStatus(model.id, newStatus as 1 | 2)
    model.status = newStatus
    ElMessage.success(`已${actionLabel}模型「${model.modelName}」`)
  } catch (e) {
    ElMessage.error(getErrorMessage(e, `${actionLabel}失败`))
  }
}

/* =================================================================
 *  删除
 * ================================================================= */

async function handleDelete(model: AiModelItem) {
  try {
    await ElMessageBox.confirm(
      `确定要删除模型「${model.modelName}」吗？此操作不可撤销。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await deleteAiModel(model.id)
    modelList.value = modelList.value.filter(m => m.id !== model.id)
    ElMessage.success('已删除')
    // 重新加载统计
    fetchModelStats().then(s => { stats.value = s }).catch(() => {})
  } catch (e) {
    ElMessage.error(getErrorMessage(e, '删除失败'))
  }
}

/* =================================================================
 *  新增 / 编辑弹窗
 * ================================================================= */

const dialogVisible = ref(false)
const dialogTitle = ref('接入新模型')
const editingId = ref<number | null>(null)
const formSubmitting = ref(false)

const emptyForm = (): AiModelSaveRequest => ({
  modelCode: '',
  modelName: '',
  modelType: 1,
  purpose: '',
  apiEndpoint: '',
  promptTemplate: '',
  status: 1,
})

const form = ref<AiModelSaveRequest>(emptyForm())

function openCreate() {
  editingId.value = null
  dialogTitle.value = '接入新模型'
  form.value = emptyForm()
  dialogVisible.value = true
}

function openEdit(model: AiModelItem) {
  editingId.value = model.id
  dialogTitle.value = '编辑模型'
  form.value = {
    modelCode: model.modelCode,
    modelName: model.modelName,
    modelType: model.modelType,
    purpose: model.purpose ?? '',
    apiEndpoint: model.apiEndpoint ?? '',
    promptTemplate: model.promptTemplate ?? '',
    status: model.status,
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.modelCode.trim() || !form.value.modelName.trim()) {
    ElMessage.warning('模型编码和模型名称为必填项')
    return
  }
  formSubmitting.value = true
  try {
    if (editingId.value !== null) {
      await updateAiModel(editingId.value, form.value)
      ElMessage.success('模型已更新')
    } else {
      await createAiModel(form.value)
      ElMessage.success('模型已创建')
    }
    dialogVisible.value = false
    await loadModels()
    // 刷新统计
    fetchModelStats().then(s => { stats.value = s }).catch(() => {})
  } catch (e) {
    ElMessage.error(getErrorMessage(e, editingId.value ? '更新失败' : '创建失败'))
  } finally {
    formSubmitting.value = false
  }
}

/* =================================================================
 *  工具函数
 * ================================================================= */

function formatTime(value?: string): string {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

/* =================================================================
 *  生命周期
 * ================================================================= */

onMounted(() => {
  loadAll()
})
</script>

<template>
  <div data-cmp="AIModels" class="p-8 h-full overflow-y-auto scrollbar-thin">
    <div class="max-w-6xl mx-auto">
      <!-- 页面头部 -->
      <div class="flex items-center justify-between mb-8">
        <div>
          <h1 class="text-2xl font-black text-foreground">AI模型管理</h1>
          <p class="text-muted-foreground mt-1">管理和监控系统中所有AI模型的运行状态</p>
        </div>
        <div class="flex gap-3">
          <button
            type="button"
            class="flex items-center gap-2 px-4 py-2 border border-border rounded-xl text-sm text-muted-foreground hover:bg-muted"
          >
            <Settings :size="14" />
            <span>全局配置</span>
          </button>
          <button
            type="button"
            class="flex items-center gap-2 px-4 py-2 gradient-blue rounded-xl text-white text-sm font-medium shadow-custom"
            @click="openCreate"
          >
            <Bot :size="14" />
            <span>接入新模型</span>
          </button>
        </div>
      </div>

      <!-- Loading 状态 -->
      <div v-if="loading && modelList.length === 0" class="flex items-center justify-center py-24 text-muted-foreground">
        <Loader2 :size="20" class="animate-spin text-brand-blue" />
        <span class="ml-2 text-sm">加载中…</span>
      </div>

      <template v-else>
        <!-- 统计卡片 -->
        <div class="flex gap-4 mb-8">
          <div
            v-for="s in statCards"
            :key="s.label"
            class="flex-1 bg-card p-5 shadow-card border border-border"
          >
            <div :class="['w-10 h-10 rounded-xl flex items-center justify-center mb-3', s.cls]">
              <component :is="s.icon" :size="18" />
            </div>
            <div class="text-2xl font-black text-foreground">{{ s.value }}</div>
            <div class="text-sm text-muted-foreground mt-1">{{ s.label }}</div>
          </div>
        </div>

        <!-- 图表区域 -->
        <div class="flex gap-4 mb-6">
          <!-- 近7日调用趋势 -->
          <div class="flex-1 bg-card p-5 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-5">
              <BarChart2 :size="15" class="text-brand-blue" />
              <span class="text-sm font-bold text-foreground">近7日 AI调用趋势</span>
            </div>
            <VChart :option="trendOption" autoresize style="height: 180px" />
          </div>
          <!-- 调用分布 -->
          <div class="flex-1 bg-card p-5 shadow-card border border-border">
            <div class="flex items-center gap-2 mb-5">
              <Activity :size="15" class="text-brand-purple" />
              <span class="text-sm font-bold text-foreground">模型调用分布</span>
            </div>
            <VChart :option="distOption" autoresize style="height: 180px" />
          </div>
        </div>

        <!-- 模型列表 -->
        <div class="bg-card shadow-card border border-border overflow-hidden">
          <div class="px-5 py-4 border-b border-border flex items-center gap-2">
            <Bot :size="15" class="text-brand-purple" />
            <span class="text-sm font-bold text-foreground">模型列表</span>
            <span class="text-xs text-muted-foreground ml-1">({{ modelList.length }})</span>
          </div>

          <!-- 空状态 -->
          <div v-if="modelList.length === 0" class="flex flex-col items-center justify-center py-16 text-muted-foreground">
            <Bot :size="36" class="mb-3 opacity-30" />
            <p class="text-sm">暂无模型数据</p>
          </div>

          <!-- 表格 -->
          <div v-else class="overflow-x-auto">
            <table class="w-full">
              <thead>
                <tr class="border-b border-border">
                  <th
                    v-for="h in ['模型名称', '类型', '使用场景', '状态', '创建时间', '操作']"
                    :key="h"
                    class="px-4 py-3 text-left text-xs text-muted-foreground font-medium"
                  >
                    {{ h }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="m in modelList"
                  :key="m.id"
                  class="border-b border-border last:border-0 hover:bg-muted/50 transition-colors"
                >
                  <!-- 模型名称 -->
                  <td class="px-4 py-3">
                    <div class="flex items-center gap-2">
                      <div class="w-7 h-7 rounded-lg gradient-purple flex items-center justify-center">
                        <Sparkles :size="12" class="text-white" />
                      </div>
                      <div>
                        <span class="text-sm font-medium text-foreground">{{ m.modelName }}</span>
                        <div class="text-xs text-muted-foreground">{{ m.modelCode }}</div>
                      </div>
                    </div>
                  </td>
                  <!-- 类型 -->
                  <td class="px-4 py-3 text-xs text-muted-foreground">{{ modelTypeLabel(m.modelType) }}</td>
                  <!-- 使用场景 -->
                  <td class="px-4 py-3 text-xs text-muted-foreground max-w-36 truncate">{{ m.purpose || '—' }}</td>
                  <!-- 状态 -->
                  <td class="px-4 py-3">
                    <span
                      :class="[
                        'flex items-center gap-1 text-xs font-medium',
                        m.status === 1 ? 'text-brand-green' : 'text-muted-foreground',
                      ]"
                    >
                      <CheckCircle v-if="m.status === 1" :size="11" />
                      <AlertCircle v-else :size="11" />
                      {{ m.status === 1 ? '运行中' : '暂停' }}
                    </span>
                  </td>
                  <!-- 创建时间 -->
                  <td class="px-4 py-3 text-xs text-muted-foreground">{{ formatTime(m.createdAt) }}</td>
                  <!-- 操作 -->
                  <td class="px-4 py-3">
                    <div class="flex items-center gap-1">
                      <!-- 暂停 / 启动 -->
                      <button
                        v-if="m.status === 1"
                        type="button"
                        class="p-1.5 rounded-lg bg-orange-50 hover:bg-orange-100"
                        title="暂停"
                        @click="handleToggleStatus(m)"
                      >
                        <Pause :size="13" class="text-brand-orange" />
                      </button>
                      <button
                        v-else
                        type="button"
                        class="p-1.5 rounded-lg bg-green-50 hover:bg-green-100"
                        title="启动"
                        @click="handleToggleStatus(m)"
                      >
                        <Play :size="13" class="text-brand-green" />
                      </button>
                      <!-- 编辑 -->
                      <button
                        type="button"
                        class="p-1.5 rounded-lg hover:bg-muted"
                        title="编辑"
                        @click="openEdit(m)"
                      >
                        <Pencil :size="13" class="text-muted-foreground" />
                      </button>
                      <!-- 删除 -->
                      <button
                        type="button"
                        class="p-1.5 rounded-lg hover:bg-red-50 hover:text-brand-red"
                        title="删除"
                        @click="handleDelete(m)"
                      >
                        <Trash2 :size="13" class="text-muted-foreground" />
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </template>
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <ElDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <ElForm label-width="100px" :model="form" class="pr-4">
        <ElFormItem label="模型编码" required>
          <ElInput v-model="form.modelCode" placeholder="如 qwen-max" />
        </ElFormItem>
        <ElFormItem label="模型名称" required>
          <ElInput v-model="form.modelName" placeholder="如 通义千问-Max" />
        </ElFormItem>
        <ElFormItem label="模型类型">
          <ElSelect v-model="form.modelType" style="width: 100%">
            <ElOption
              v-for="opt in MODEL_TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </ElSelect>
        </ElFormItem>
        <ElFormItem label="使用场景">
          <ElInput v-model="form.purpose" placeholder="简述模型用途" />
        </ElFormItem>
        <ElFormItem label="API端点">
          <ElInput v-model="form.apiEndpoint" placeholder="https://..." />
        </ElFormItem>
        <ElFormItem label="Prompt模板">
          <ElInput
            v-model="form.promptTemplate"
            type="textarea"
            :rows="4"
            placeholder="输入提示词模板…"
          />
        </ElFormItem>
        <ElFormItem label="状态">
          <ElSelect v-model="form.status" style="width: 100%">
            <ElOption label="运行中" :value="1" />
            <ElOption label="暂停" :value="2" />
          </ElSelect>
        </ElFormItem>
      </ElForm>
      <template #footer>
        <div class="flex justify-end gap-3">
          <button
            type="button"
            class="px-4 py-2 border border-border rounded-xl text-sm text-muted-foreground hover:bg-muted"
            @click="dialogVisible = false"
          >
            取消
          </button>
          <button
            type="button"
            class="flex items-center gap-2 px-4 py-2 gradient-blue rounded-xl text-white text-sm font-medium shadow-custom disabled:opacity-50"
            :disabled="formSubmitting"
            @click="handleSubmit"
          >
            <Loader2 v-if="formSubmitting" :size="14" class="animate-spin" />
            <Plus v-else :size="14" />
            <span>{{ editingId !== null ? '保存' : '创建' }}</span>
          </button>
        </div>
      </template>
    </ElDialog>
  </div>
</template>
