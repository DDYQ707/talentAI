<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Search, User, Clock, Video, MapPin, ChevronLeft, ChevronRight, Sparkles } from 'lucide-vue-next'

const interviews = [
  { id: 1, candidate: '张三', job: '高级前端工程师', round: '技术面试', interviewer: '李工(技术总监)', time: '2024-06-14 14:00', type: '视频面试', status: '待进行', score: 0 },
  { id: 2, candidate: '王五', job: 'AI算法工程师', round: 'HR初面', interviewer: '张招聘(HR)', time: '2024-06-14 10:00', type: '现场面试', status: '已完成', score: 88 },
  { id: 3, candidate: '李四', job: '产品经理', round: '终面', interviewer: '陈总监', time: '2024-06-15 15:00', type: '视频面试', status: '待进行', score: 0 },
  { id: 4, candidate: '赵六', job: '运营总监', round: '总裁面', interviewer: '王CEO', time: '2024-06-13 11:00', type: '现场面试', status: '已完成', score: 92 },
  { id: 5, candidate: '陈七', job: 'UI设计师', round: '作品评审', interviewer: '刘设计总监', time: '2024-06-16 14:30', type: '线上评审', status: '待安排', score: 0 },
  { id: 6, candidate: '刘八', job: '后端工程师', round: '技术面试', interviewer: '王技术', time: '2024-06-17 10:00', type: '视频面试', status: '待安排', score: 0 },
]

const statusStyles: Record<string, { label: string; cls: string }> = {
  待进行: { label: '待进行', cls: 'bg-brand-tint text-brand-blue border-brand-border' },
  已完成: { label: '已完成', cls: 'bg-green-50 text-brand-green border-green-200' },
  待安排: { label: '待安排', cls: 'bg-orange-50 text-brand-orange border-orange-200' },
  已取消: { label: '已取消', cls: 'bg-muted text-muted-foreground border-border' },
}

const calendarDays = [
  { date: 10, count: 1 },
  { date: 11, count: 0 },
  { date: 12, count: 2 },
  { date: 13, count: 3 },
  { date: 14, count: 2 },
  { date: 15, count: 1 },
  { date: 16, count: 1 },
  { date: 17, count: 2 },
  { date: 18, count: 0 },
  { date: 19, count: 3 },
  { date: 20, count: 1 },
]

const view = ref<'list' | 'calendar'>('list')
</script>

<template>
  <div data-cmp="InterviewManagement" class="flex h-full" style="height: calc(100vh - 64px)">
    <div class="flex-1 p-6 flex flex-col gap-4 overflow-auto scrollbar-thin min-w-0">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-xl font-bold text-foreground">面试管理</h1>
          <p class="text-sm text-muted-foreground mt-0.5">今日 2 场待进行 · 本周 8 场</p>
        </div>
        <div class="flex items-center gap-3">
          <div class="flex bg-muted rounded-lg overflow-hidden border border-border">
            <button
              type="button"
              class="px-4 py-2 text-xs font-medium transition-colors"
              :class="view === 'list' ? 'bg-brand-blue text-white' : 'text-muted-foreground hover:text-foreground'"
              @click="view = 'list'"
            >
              列表视图
            </button>
            <button
              type="button"
              class="px-4 py-2 text-xs font-medium transition-colors"
              :class="view === 'calendar' ? 'bg-brand-blue text-white' : 'text-muted-foreground hover:text-foreground'"
              @click="view = 'calendar'"
            >
              日历视图
            </button>
          </div>
          <button type="button" class="flex items-center gap-2 px-4 py-2.5 rounded-control gradient-blue text-white text-sm font-medium">
            <Plus :size="16" />
            <span>安排面试</span>
          </button>
        </div>
      </div>

      <div class="flex gap-4">
        <div v-for="s in [{ label: '今日面试', value: 2, color: 'text-brand-blue' }, { label: '本周总计', value: 8, color: 'text-brand-purple' }, { label: '已完成', value: 2, color: 'text-brand-green' }, { label: '待安排', value: 2, color: 'text-brand-orange' }]" :key="s.label" class="flex-1 bg-card rounded-xl p-4 shadow-card">
          <div :class="['text-2xl font-bold', s.color]">{{ s.value }}</div>
          <div class="text-xs text-muted-foreground mt-1">{{ s.label }}</div>
        </div>
      </div>

      <div class="flex items-center gap-3">
        <div class="flex items-center gap-2 bg-card rounded-lg px-3 py-2 border border-border flex-1 max-w-sm">
          <Search :size="14" class="text-muted-foreground" />
          <input class="bg-transparent text-sm outline-none flex-1 placeholder:text-muted-foreground" placeholder="搜索候选人或岗位" />
        </div>
        <button v-for="f in ['全部状态', '全部轮次', '全部方式']" :key="f" type="button" class="flex items-center gap-1.5 px-3 py-2 rounded-lg bg-card border border-border text-xs text-muted-foreground">
          <span>{{ f }}</span>
        </button>
      </div>

      <div v-show="view === 'list'">
        <div class="bg-card shadow-card overflow-hidden">
          <table class="w-full">
            <thead>
              <tr class="border-b border-border bg-muted/50">
                <th v-for="h in ['候选人', '应聘岗位', '面试轮次', '面试官', '时间', '方式', '状态', '评分']" :key="h" class="text-left text-xs font-medium text-muted-foreground px-4 py-3">
                  {{ h }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="iv in interviews" :key="iv.id" class="border-b border-border hover:bg-muted/30 transition-colors cursor-pointer">
                <td class="px-4 py-3">
                  <div class="flex items-center gap-2">
                    <div class="w-7 h-7 rounded-full gradient-blue flex items-center justify-center">
                      <User :size="12" class="text-white" />
                    </div>
                    <span class="text-sm font-medium text-foreground">{{ iv.candidate }}</span>
                  </div>
                </td>
                <td class="px-4 py-3 text-sm text-muted-foreground">{{ iv.job }}</td>
                <td class="px-4 py-3">
                  <span class="text-xs px-2 py-1 rounded-full bg-accent text-accent-foreground">{{ iv.round }}</span>
                </td>
                <td class="px-4 py-3 text-sm text-muted-foreground">{{ iv.interviewer }}</td>
                <td class="px-4 py-3">
                  <div class="flex items-center gap-1 text-xs text-foreground">
                    <Clock :size="11" class="text-muted-foreground" />
                    <span>{{ iv.time }}</span>
                  </div>
                </td>
                <td class="px-4 py-3">
                  <div class="flex items-center gap-1 text-xs text-muted-foreground">
                    <Video v-if="iv.type === '视频面试'" :size="12" />
                    <MapPin v-else :size="12" />
                    <span>{{ iv.type }}</span>
                  </div>
                </td>
                <td class="px-4 py-3">
                  <span :class="['text-xs px-2 py-1 rounded-full border', statusStyles[iv.status].cls]">{{ statusStyles[iv.status].label }}</span>
                </td>
                <td class="px-4 py-3">
                  <div v-if="iv.score > 0" class="flex items-center gap-1">
                    <span class="text-sm font-bold text-brand-purple">{{ iv.score }}</span>
                    <span class="text-xs text-muted-foreground">分</span>
                  </div>
                  <span v-else class="text-xs text-muted-foreground">—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-show="view === 'calendar'">
        <div class="bg-card shadow-card p-5">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-sm font-semibold text-foreground">2024年6月</h3>
            <div class="flex items-center gap-2">
              <button type="button" class="p-1.5 rounded-lg hover:bg-muted">
                <ChevronLeft :size="14" />
              </button>
              <button type="button" class="p-1.5 rounded-lg hover:bg-muted">
                <ChevronRight :size="14" />
              </button>
            </div>
          </div>
          <div class="flex gap-2 overflow-x-auto pb-2">
            <div
              v-for="d in calendarDays"
              :key="d.date"
              class="flex-shrink-0 w-16 rounded-xl p-3 text-center cursor-pointer border transition-colors"
              :class="d.date === 14 ? 'gradient-blue border-transparent text-white' : 'border-border hover:border-brand-blue/40 hover:bg-muted'"
            >
              <div :class="['text-xs mb-1', d.date === 14 ? 'text-white/90' : 'text-muted-foreground']">6月</div>
              <div :class="['text-lg font-bold', d.date === 14 ? 'text-white' : 'text-foreground']">{{ d.date }}</div>
              <div v-if="d.count > 0" :class="['text-xs mt-1', d.date === 14 ? 'text-white/90' : 'text-brand-blue']">{{ d.count }}场</div>
              <div v-else :class="['text-xs mt-1', d.date === 14 ? 'text-white/70' : 'text-muted-foreground']">—</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="w-72 flex-shrink-0 border-l border-border bg-card p-5 flex flex-col gap-4 overflow-y-auto scrollbar-thin">
      <div class="flex items-center gap-2">
        <Sparkles :size="16" class="text-brand-purple" />
        <span class="text-sm font-semibold text-foreground">AI面试助手</span>
      </div>
      <div class="bg-accent rounded-xl p-4 border border-brand-border/50">
        <div class="text-xs font-semibold text-brand-purple mb-3">今日面试摘要</div>
        <div class="space-y-3">
          <div v-for="iv in interviews.filter((x) => x.status !== '待安排').slice(0, 3)" :key="iv.id" class="border-b border-brand-border/50 pb-3 last:border-0 last:pb-0">
            <div class="flex items-center justify-between mb-1">
              <span class="text-xs font-medium text-foreground">{{ iv.candidate }}</span>
              <span class="text-xs" :class="statusStyles[iv.status].cls.split(' ')[1]">{{ iv.status }}</span>
            </div>
            <div class="text-xs text-muted-foreground">{{ iv.job }} · {{ iv.round }}</div>
            <div class="text-xs text-muted-foreground">{{ iv.time.split(' ')[1] }}</div>
          </div>
        </div>
      </div>
      <div class="bg-card rounded-xl p-4 border border-border">
        <div class="text-xs font-semibold text-foreground mb-3">💡 AI建议</div>
        <div class="space-y-2 text-xs text-muted-foreground">
          <div class="leading-relaxed">张三今日技术面，建议重点考查React性能优化和团队协作经验</div>
          <div class="leading-relaxed">本周面试完成率75%，建议及时跟进未出席候选人</div>
        </div>
      </div>
    </div>
  </div>
</template>
