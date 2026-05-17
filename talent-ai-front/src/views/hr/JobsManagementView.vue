<script setup lang="ts">
import { ref } from 'vue'
import {
  Plus,
  Search,
  Filter,
  ChevronDown,
  Users,
  Eye,
  Edit,
  Trash2,
  Sparkles,
  MapPin,
  Briefcase,
  CheckCircle,
  AlertCircle,
  XCircle,
} from 'lucide-vue-next'

const jobs = [
  { id: 1, title: '高级前端工程师', dept: '技术部', status: '招聘中', type: '全职', location: '北京', salary: '25-40K', headcount: 3, applied: 156, matched: 42, created: '2024-06-01', priority: '高' },
  { id: 2, title: '产品经理', dept: '产品部', status: '招聘中', type: '全职', location: '上海', salary: '20-35K', headcount: 2, applied: 89, matched: 28, created: '2024-06-03', priority: '中' },
  { id: 3, title: 'AI算法工程师', dept: '技术部', status: '招聘中', type: '全职', location: '北京', salary: '35-60K', headcount: 2, applied: 214, matched: 38, created: '2024-05-28', priority: '高' },
  { id: 4, title: 'UI/UX设计师', dept: '设计部', status: '暂停', type: '全职', location: '深圳', salary: '15-25K', headcount: 1, applied: 67, matched: 19, created: '2024-05-20', priority: '低' },
  { id: 5, title: '运营总监', dept: '运营部', status: '招聘中', type: '全职', location: '上海', salary: '30-50K', headcount: 1, applied: 43, matched: 12, created: '2024-06-08', priority: '高' },
  { id: 6, title: '数据分析师', dept: '技术部', status: '已完成', type: '全职', location: '北京', salary: '18-28K', headcount: 2, applied: 98, matched: 31, created: '2024-05-10', priority: '中' },
]

const statusMap: Record<string, { label: string; color: string; bg: string }> = {
  招聘中: { label: '招聘中', color: 'text-brand-green', bg: 'bg-green-50 border-green-200' },
  暂停: { label: '已暂停', color: 'text-brand-orange', bg: 'bg-orange-50 border-orange-200' },
  已完成: { label: '已完成', color: 'text-muted-foreground', bg: 'bg-muted border-border' },
}

const priorityMap: Record<string, string> = {
  高: 'text-brand-red bg-red-50 border-red-200',
  中: 'text-brand-orange bg-orange-50 border-orange-200',
  低: 'text-muted-foreground bg-muted border-border',
}

const drawerOpen = ref(false)
const selectedJob = ref(jobs[0])

function openDrawer(job: (typeof jobs)[0]) {
  selectedJob.value = job
  drawerOpen.value = true
}
</script>

<template>
  <div data-cmp="JobsManagement" class="flex h-full" style="height: calc(100vh - 64px)">
    <div class="flex-1 p-6 flex flex-col gap-4 min-w-0 overflow-auto scrollbar-thin">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-xl font-bold text-foreground">岗位管理</h1>
          <p class="text-sm text-muted-foreground mt-0.5">共 {{ jobs.length }} 个岗位 · AI智能管理</p>
        </div>
        <button type="button" class="flex items-center gap-2 px-4 py-2.5 rounded-control gradient-blue text-white text-sm font-medium">
          <Plus :size="16" />
          <span>新建岗位</span>
        </button>
      </div>

      <div class="flex gap-4">
        <div
          v-for="s in [
            { label: '招聘中', value: 4, icon: CheckCircle, color: 'text-brand-green', bg: 'bg-green-50' },
            { label: '总投递', value: 667, icon: Users, color: 'text-brand-blue', bg: 'bg-brand-tint' },
            { label: 'AI匹配', value: 170, icon: Sparkles, color: 'text-brand-purple', bg: 'bg-brand-tint-2' },
            { label: '已暂停', value: 1, icon: AlertCircle, color: 'text-brand-orange', bg: 'bg-orange-50' },
          ]"
          :key="s.label"
          class="flex-1 bg-card rounded-xl p-4 shadow-card flex items-center gap-3"
        >
          <div :class="['w-9 h-9 rounded-xl flex items-center justify-center', s.bg]">
            <component :is="s.icon" :size="18" :class="s.color" />
          </div>
          <div>
            <div class="text-lg font-bold text-foreground">{{ s.value }}</div>
            <div class="text-xs text-muted-foreground">{{ s.label }}</div>
          </div>
        </div>
      </div>

      <div class="flex items-center gap-3 bg-card rounded-xl p-3 shadow-card">
        <div class="flex items-center gap-2 bg-muted rounded-lg px-3 py-2 flex-1 max-w-xs">
          <Search :size="14" class="text-muted-foreground" />
          <input class="bg-transparent text-sm outline-none flex-1 placeholder:text-muted-foreground" placeholder="搜索岗位名称" />
        </div>
        <button
          v-for="f in ['全部状态', '全部部门', '全部城市']"
          :key="f"
          type="button"
          class="flex items-center gap-1.5 px-3 py-2 rounded-lg bg-muted text-sm text-muted-foreground hover:text-foreground border border-border"
        >
          <span>{{ f }}</span>
          <ChevronDown :size="14" />
        </button>
        <button type="button" class="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-border text-sm text-muted-foreground hover:text-foreground ml-auto">
          <Filter :size="14" />
          <span>高级筛选</span>
        </button>
      </div>

      <div class="bg-card rounded-xl shadow-card overflow-hidden">
        <table class="w-full">
          <thead>
            <tr class="border-b border-border bg-muted/50">
              <th v-for="h in ['岗位名称', '部门', '状态', '优先级', '薪资范围', '投递/匹配', '发布时间', '操作']" :key="h" class="text-left text-xs font-medium text-muted-foreground px-4 py-3">
                {{ h }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="job in jobs" :key="job.id" class="border-b border-border hover:bg-muted/30 transition-colors">
              <td class="px-4 py-3">
                <div class="flex items-center gap-3">
                  <div class="w-8 h-8 rounded-lg gradient-blue flex items-center justify-center">
                    <Briefcase :size="14" class="text-white" />
                  </div>
                  <div>
                    <div class="text-sm font-medium text-foreground">{{ job.title }}</div>
                    <div class="flex items-center gap-1 text-xs text-muted-foreground mt-0.5">
                      <MapPin :size="10" />
                      <span>{{ job.location }}</span>
                    </div>
                  </div>
                </div>
              </td>
              <td class="px-4 py-3 text-sm text-muted-foreground">{{ job.dept }}</td>
              <td class="px-4 py-3">
                <span :class="['text-xs px-2 py-1 rounded-full border', statusMap[job.status].bg, statusMap[job.status].color]">
                  {{ statusMap[job.status].label }}
                </span>
              </td>
              <td class="px-4 py-3">
                <span :class="['text-xs px-2 py-1 rounded-full border', priorityMap[job.priority]]">{{ job.priority }}优先</span>
              </td>
              <td class="px-4 py-3 text-sm font-medium text-foreground">{{ job.salary }}</td>
              <td class="px-4 py-3">
                <div class="flex items-center gap-2">
                  <span class="text-sm text-foreground">{{ job.applied }}</span>
                  <span class="text-muted-foreground">/</span>
                  <span class="text-sm text-brand-purple font-medium">{{ job.matched }}</span>
                </div>
                <div class="mt-1 h-1 bg-muted rounded-full overflow-hidden w-16">
                  <div
                    class="h-full rounded-full bg-brand-purple"
                    :style="{ width: `${Math.min((job.matched / job.applied) * 100, 100)}%` }"
                  />
                </div>
              </td>
              <td class="px-4 py-3 text-xs text-muted-foreground">{{ job.created }}</td>
              <td class="px-4 py-3">
                <div class="flex items-center gap-1">
                  <button type="button" class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground" @click="openDrawer(job)">
                    <Eye :size="14" />
                  </button>
                  <button type="button" class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground">
                    <Edit :size="14" />
                  </button>
                  <button type="button" class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-brand-red">
                    <Trash2 :size="14" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div
      class="flex-shrink-0 border-l border-border bg-card transition-all duration-300 overflow-auto scrollbar-thin"
      :class="drawerOpen ? 'w-80' : 'w-0 overflow-hidden'"
    >
      <div class="p-5" style="width: 320px">
        <div class="flex items-center justify-between mb-5">
          <span class="text-sm font-semibold text-foreground">岗位画像分析</span>
          <button type="button" class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground" @click="drawerOpen = false">
            <XCircle :size="16" />
          </button>
        </div>
        <div class="bg-muted rounded-xl p-4 mb-4">
          <div class="text-base font-bold text-foreground mb-1">{{ selectedJob.title }}</div>
          <div class="text-sm text-muted-foreground mb-2">{{ selectedJob.dept }} · {{ selectedJob.location }}</div>
          <div class="text-lg font-bold text-brand-blue">{{ selectedJob.salary }}</div>
        </div>
        <div class="flex gap-3 mb-4">
          <div v-for="s in [{ label: '总投递', value: selectedJob.applied, color: 'text-brand-blue' }, { label: 'AI匹配', value: selectedJob.matched, color: 'text-brand-purple' }, { label: '招聘中', value: selectedJob.headcount, color: 'text-brand-green' }]" :key="s.label" class="flex-1 bg-muted rounded-xl p-3 text-center">
            <div :class="['text-lg font-bold', s.color]">{{ s.value }}</div>
            <div class="text-xs text-muted-foreground">{{ s.label }}</div>
          </div>
        </div>
        <div class="bg-accent rounded-xl p-4 border border-brand-border/50">
          <div class="flex items-center gap-2 mb-3">
            <Sparkles :size="14" class="text-brand-purple" />
            <span class="text-xs font-semibold text-brand-purple">AI岗位画像</span>
          </div>
          <div class="space-y-2 text-xs text-muted-foreground">
            <div class="flex justify-between">
              <span>岗位热度</span>
              <span class="text-brand-orange font-medium">🔥 高热度</span>
            </div>
            <div class="flex justify-between">
              <span>竞争系数</span>
              <span class="text-brand-red font-medium">1:{{ Math.round(selectedJob.applied / selectedJob.headcount) }}</span>
            </div>
            <div class="flex justify-between">
              <span>匹配率</span>
              <span class="text-brand-green font-medium">{{ Math.round((selectedJob.matched / selectedJob.applied) * 100) }}%</span>
            </div>
            <div class="flex justify-between">
              <span>预计周期</span>
              <span class="text-foreground font-medium">12-18天</span>
            </div>
          </div>
          <div class="mt-3 pt-3 border-t border-brand-border/50 text-xs text-muted-foreground leading-relaxed">
            💡 该岗位投递量充足，建议提高AI筛选阈值到85分以上，可节省40%人工筛选时间
          </div>
        </div>
        <div class="mt-4 flex flex-col gap-2">
          <button type="button" class="w-full py-2.5 rounded-control gradient-blue text-white text-sm font-medium">查看所有候选人</button>
          <button type="button" class="w-full py-2.5 rounded-xl border border-border text-sm text-foreground hover:bg-muted">编辑岗位信息</button>
        </div>
      </div>
    </div>
  </div>
</template>
