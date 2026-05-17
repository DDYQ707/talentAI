<script setup lang="ts">
import {
  Plus,
  Search,
  User,
  CheckCircle,
  Clock,
  XCircle,
  Send,
  FileText,
  Sparkles,
  TrendingUp,
  Edit,
  Eye,
} from 'lucide-vue-next'

const offers = [
  { id: 1, candidate: '张三', job: '高级前端工程师', dept: '技术部', salary: '35K', bonus: '4薪', startDate: '2024-07-15', status: '已发送', date: '2024-06-10', approver: '技术总监' },
  { id: 2, candidate: '赵六', job: '运营总监', dept: '运营部', salary: '45K', bonus: '6薪', startDate: '2024-07-01', status: '已接受', date: '2024-06-05', approver: 'CEO' },
  { id: 3, candidate: '李四', job: '产品经理', dept: '产品部', salary: '28K', bonus: '4薪', startDate: '2024-07-20', status: '审批中', date: '2024-06-13', approver: '产品VP' },
  { id: 4, candidate: '孙九', job: '算法工程师', dept: '技术部', salary: '50K', bonus: '6薪', startDate: '2024-08-01', status: '已拒绝', date: '2024-06-01', approver: 'CTO' },
  { id: 5, candidate: '刘八', job: '后端工程师', dept: '技术部', salary: '25K', bonus: '3薪', startDate: '2024-07-22', status: '草稿', date: '2024-06-14', approver: '技术经理' },
]

const statusStyles: Record<string, { cls: string; icon: typeof Send }> = {
  已发送: { cls: 'bg-brand-tint text-brand-blue border-brand-border', icon: Send },
  已接受: { cls: 'bg-green-50 text-brand-green border-green-200', icon: CheckCircle },
  审批中: { cls: 'bg-orange-50 text-brand-orange border-orange-200', icon: Clock },
  已拒绝: { cls: 'bg-red-50 text-brand-red border-red-200', icon: XCircle },
  草稿: { cls: 'bg-muted text-muted-foreground border-border', icon: FileText },
}

const stats = [
  { label: '本月发放', value: 5, change: '+2', color: 'text-brand-blue' },
  { label: '已接受', value: 1, change: '20%', color: 'text-brand-green' },
  { label: '审批中', value: 1, change: '', color: 'text-brand-orange' },
  { label: '已拒绝', value: 1, change: '20%', color: 'text-brand-red' },
]
</script>

<template>
  <div data-cmp="OfferManagement" class="p-6 space-y-5" style="height: calc(100vh - 64px); overflow-y: auto">
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-xl font-bold text-foreground">Offer管理</h1>
        <p class="text-sm text-muted-foreground mt-0.5">管理所有候选人的Offer发放与审批</p>
      </div>
      <button type="button" class="flex items-center gap-2 px-4 py-2.5 rounded-control gradient-blue text-white text-sm font-medium">
        <Plus :size="16" />
        <span>创建Offer</span>
      </button>
    </div>

    <div class="flex gap-4">
      <div v-for="s in stats" :key="s.label" class="flex-1 bg-card rounded-xl p-4 shadow-card">
        <div :class="['text-2xl font-bold', s.color]">{{ s.value }}</div>
        <div class="text-xs text-muted-foreground mt-1">{{ s.label }}</div>
        <div v-if="s.change" class="text-xs text-muted-foreground mt-1">{{ s.change }} 接受率</div>
      </div>
    </div>

    <div class="flex items-center gap-3">
      <div class="flex items-center gap-2 bg-card rounded-lg px-3 py-2 border border-border flex-1 max-w-sm">
        <Search :size="14" class="text-muted-foreground" />
        <input class="bg-transparent text-sm outline-none flex-1 placeholder:text-muted-foreground" placeholder="搜索候选人或岗位" />
      </div>
      <button type="button" class="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-border text-xs text-muted-foreground hover:text-foreground">
        全部状态
      </button>
      <div class="ml-auto flex items-center gap-2">
        <button type="button" class="flex items-center gap-1.5 px-3 py-2 rounded-lg gradient-purple text-white text-xs">
          <Sparkles :size="12" />
          <span>AI生成Offer</span>
        </button>
      </div>
    </div>

    <div class="bg-card shadow-card overflow-hidden">
      <table class="w-full">
        <thead>
          <tr class="border-b border-border bg-muted/50">
            <th v-for="h in ['候选人', '应聘岗位', '部门', '薪资', '年终奖', '入职日期', '审批人', '状态', '操作']" :key="h" class="text-left text-xs font-medium text-muted-foreground px-4 py-3">
              {{ h }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="offer in offers" :key="offer.id" class="border-b border-border hover:bg-muted/30 transition-colors">
            <td class="px-4 py-3">
              <div class="flex items-center gap-2">
                <div class="w-7 h-7 rounded-full gradient-blue flex items-center justify-center">
                  <User :size="12" class="text-white" />
                </div>
                <div>
                  <div class="text-sm font-medium text-foreground">{{ offer.candidate }}</div>
                  <div class="text-xs text-muted-foreground">{{ offer.date }}</div>
                </div>
              </div>
            </td>
            <td class="px-4 py-3 text-sm text-muted-foreground">{{ offer.job }}</td>
            <td class="px-4 py-3 text-sm text-muted-foreground">{{ offer.dept }}</td>
            <td class="px-4 py-3">
              <span class="text-sm font-bold text-brand-blue">{{ offer.salary }}</span>
            </td>
            <td class="px-4 py-3 text-sm text-muted-foreground">{{ offer.bonus }}</td>
            <td class="px-4 py-3 text-sm text-muted-foreground">{{ offer.startDate }}</td>
            <td class="px-4 py-3 text-sm text-muted-foreground">{{ offer.approver }}</td>
            <td class="px-4 py-3">
              <div class="flex items-center gap-1.5">
                <component :is="statusStyles[offer.status].icon" :size="12" :class="statusStyles[offer.status].cls.split(' ')[1]" />
                <span :class="['text-xs px-2 py-0.5 rounded-full border', statusStyles[offer.status].cls]">{{ offer.status }}</span>
              </div>
            </td>
            <td class="px-4 py-3">
              <div class="flex items-center gap-1">
                <button type="button" class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground">
                  <Eye :size="14" />
                </button>
                <button type="button" class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground">
                  <Edit :size="14" />
                </button>
                <button type="button" class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground">
                  <Send :size="14" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="bg-card shadow-card p-5">
      <div class="flex items-center gap-2 mb-4">
        <div class="w-7 h-7 rounded-lg gradient-purple flex items-center justify-center">
          <Sparkles :size="14" class="text-white" />
        </div>
        <h3 class="text-sm font-semibold text-foreground">AI Offer助手</h3>
      </div>
      <div class="flex gap-4">
        <div
          v-for="item in [
            { title: '薪资建议', desc: '基于市场数据，自动生成最具竞争力的薪资方案', action: '生成建议' },
            { title: 'Offer信件生成', desc: 'AI一键生成个性化Offer信件，提升候选人体验', action: '立即生成' },
            { title: '风险预测', desc: '分析候选人接受/拒绝概率，提前做好备选方案', action: '查看分析' },
          ]"
          :key="item.title"
          class="flex-1 bg-accent rounded-xl p-4 border border-brand-border/50"
        >
          <div class="flex items-center gap-2 mb-2">
            <TrendingUp :size="14" class="text-brand-purple" />
            <span class="text-xs font-semibold text-foreground">{{ item.title }}</span>
          </div>
          <p class="text-xs text-muted-foreground leading-relaxed mb-3">{{ item.desc }}</p>
          <button type="button" class="text-xs text-brand-purple font-medium hover:underline">{{ item.action }} →</button>
        </div>
      </div>
    </div>
  </div>
</template>
