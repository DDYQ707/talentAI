<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Search,
  LayoutGrid,
  List,
  Sparkles,
  User,
  Star,
  Bookmark,
  Send,
} from 'lucide-vue-next'

const candidates = [
  { id: 1, name: '张三', title: '高级前端工程师', exp: '6年', edu: '本科·清华大学', match: 96, skills: ['React', 'Vue', 'TypeScript', 'Node.js'], status: '待初筛', starred: true, location: '北京', applied: '2024-06-12' },
  { id: 2, name: '李四', title: '产品经理', exp: '4年', edu: '硕士·北京大学', match: 88, skills: ['产品设计', '数据分析', 'Figma', 'SQL'], status: '面试中', starred: false, location: '上海', applied: '2024-06-11' },
  { id: 3, name: '王五', title: 'AI算法工程师', exp: '5年', edu: '博士·中科大', match: 94, skills: ['Python', 'PyTorch', 'LLM', 'RLHF'], status: '待初筛', starred: true, location: '北京', applied: '2024-06-13' },
  { id: 4, name: '赵六', title: '运营总监', exp: '8年', edu: '本科·复旦大学', match: 82, skills: ['增长运营', '用户运营', '数据分析', 'GMV'], status: '已录用', starred: false, location: '上海', applied: '2024-06-08' },
  { id: 5, name: '陈七', title: 'UI设计师', exp: '3年', edu: '本科·中央美院', match: 75, skills: ['Figma', 'Sketch', 'Motion', 'UI设计'], status: '已淘汰', starred: false, location: '深圳', applied: '2024-06-09' },
  { id: 6, name: '刘八', title: '后端工程师', exp: '4年', edu: '本科·华中科技', match: 91, skills: ['Java', 'Spring', 'MySQL', 'Redis'], status: '待初筛', starred: false, location: '武汉', applied: '2024-06-14' },
]

const statusStyles: Record<string, string> = {
  待初筛: 'bg-muted text-muted-foreground border-border',
  面试中: 'bg-orange-50 text-brand-orange border-orange-200',
  已录用: 'bg-green-50 text-brand-green border-green-200',
  已淘汰: 'bg-red-50 text-brand-red border-red-200',
}

const filterTags = ['全部', '待初筛', '面试中', '已录用', '已淘汰']

const viewMode = ref<'grid' | 'list'>('grid')
const activeFilter = ref('全部')

const filtered = computed(() =>
  activeFilter.value === '全部' ? candidates : candidates.filter((c) => c.status === activeFilter.value)
)
</script>

<template>
  <div data-cmp="ResumeManagement" class="flex h-full" style="height: calc(100vh - 64px)">
    <div class="w-56 flex-shrink-0 border-r border-border bg-card p-4 overflow-y-auto scrollbar-thin">
      <div class="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">筛选条件</div>
      <div class="mb-4">
        <div class="text-xs font-medium text-foreground mb-2">候选状态</div>
        <button
          v-for="tag in filterTags"
          :key="tag"
          type="button"
          class="w-full text-left text-xs px-3 py-2 rounded-lg mb-1 transition-colors"
          :class="activeFilter === tag ? 'bg-brand-blue text-white' : 'hover:bg-muted text-muted-foreground'"
          @click="activeFilter = tag"
        >
          {{ tag }}
        </button>
      </div>
      <div class="mb-4">
        <div class="text-xs font-medium text-foreground mb-2">匹配度</div>
        <label v-for="r in ['90分以上', '80-90分', '70-80分', '70分以下']" :key="r" class="flex items-center gap-2 text-xs text-muted-foreground py-1.5 cursor-pointer hover:text-foreground">
          <div class="w-3.5 h-3.5 rounded border border-border" />
          <span>{{ r }}</span>
        </label>
      </div>
      <div class="mb-4">
        <div class="text-xs font-medium text-foreground mb-2">工作城市</div>
        <label v-for="city in ['北京', '上海', '深圳', '武汉', '杭州']" :key="city" class="flex items-center gap-2 text-xs text-muted-foreground py-1.5 cursor-pointer hover:text-foreground">
          <div class="w-3.5 h-3.5 rounded border border-border" />
          <span>{{ city }}</span>
        </label>
      </div>
      <div class="mb-4">
        <div class="text-xs font-medium text-foreground mb-2">学历要求</div>
        <label v-for="e in ['博士', '硕士', '本科', '大专']" :key="e" class="flex items-center gap-2 text-xs text-muted-foreground py-1.5 cursor-pointer hover:text-foreground">
          <div class="w-3.5 h-3.5 rounded border border-border" />
          <span>{{ e }}</span>
        </label>
      </div>
    </div>

    <div class="flex-1 flex flex-col min-w-0 overflow-hidden">
      <div class="px-6 py-4 border-b border-border flex items-center gap-3">
        <div class="flex items-center gap-2 bg-muted rounded-lg px-3 py-2 flex-1 max-w-md">
          <Search :size="14" class="text-muted-foreground" />
          <input class="bg-transparent text-sm outline-none flex-1 placeholder:text-muted-foreground" placeholder="搜索候选人姓名、技能、公司..." />
        </div>
        <button type="button" class="flex items-center gap-1.5 px-3 py-2 rounded-lg gradient-purple text-white text-sm">
          <Sparkles :size="14" />
          <span>AI语义搜索</span>
        </button>
        <div class="ml-auto flex items-center gap-2">
          <div class="text-xs text-muted-foreground">共 {{ filtered.length }} 人</div>
          <button type="button" class="p-2 rounded-lg" :class="viewMode === 'grid' ? 'bg-brand-blue text-white' : 'hover:bg-muted text-muted-foreground'" @click="viewMode = 'grid'">
            <LayoutGrid :size="16" />
          </button>
          <button type="button" class="p-2 rounded-lg" :class="viewMode === 'list' ? 'bg-brand-blue text-white' : 'hover:bg-muted text-muted-foreground'" @click="viewMode = 'list'">
            <List :size="16" />
          </button>
        </div>
      </div>

      <div class="px-6 py-2 border-b border-border bg-muted/30 flex items-center gap-3">
        <label class="flex items-center gap-2 cursor-pointer">
          <div class="w-4 h-4 rounded border-2 border-border" />
          <span class="text-xs text-muted-foreground">全选</span>
        </label>
        <div class="h-3 w-px bg-border" />
        <button v-for="a in ['批量标星', '批量导出', '批量移动']" :key="a" type="button" class="text-xs text-muted-foreground hover:text-foreground px-2 py-1 rounded hover:bg-muted">
          {{ a }}
        </button>
      </div>

      <div class="flex-1 overflow-auto scrollbar-thin p-6">
        <div :class="viewMode === 'grid' ? 'flex flex-wrap gap-4' : 'flex flex-col gap-3'">
          <div
            v-for="c in filtered"
            :key="c.id"
            class="bg-card shadow-card hover:shadow-panel transition-shadow cursor-pointer border border-border hover:border-brand-blue/30"
            :class="viewMode === 'grid' ? 'w-64 flex-shrink-0' : 'w-full'"
          >
            <div class="p-4">
              <div class="flex items-start justify-between mb-3">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-full gradient-blue-purple flex items-center justify-center flex-shrink-0">
                    <User :size="16" class="text-white" />
                  </div>
                  <div>
                    <div class="text-sm font-semibold text-foreground">{{ c.name }}</div>
                    <div class="text-xs text-muted-foreground">{{ c.title }}</div>
                  </div>
                </div>
                <button type="button" :class="c.starred ? 'text-brand-orange' : 'text-muted-foreground hover:text-brand-orange'">
                  <Star :size="16" :class="c.starred ? 'fill-brand-orange' : ''" />
                </button>
              </div>
              <div class="flex items-center gap-3 text-xs text-muted-foreground mb-3">
                <span>{{ c.exp }}</span>
                <span>·</span>
                <span>{{ c.edu }}</span>
              </div>
              <div class="flex items-center justify-between mb-3">
                <span class="text-xs text-muted-foreground">AI匹配度</span>
                <div class="flex items-center gap-2">
                  <div class="h-1.5 w-20 bg-muted rounded-full overflow-hidden">
                    <div
                      class="h-full rounded-full"
                      :style="{
                        width: `${c.match}%`,
                        background: c.match >= 90 ? 'var(--brand-purple)' : c.match >= 80 ? 'var(--brand-blue)' : '#94A3B8',
                      }"
                    />
                  </div>
                  <span
                    :class="[
                      'text-xs font-bold',
                      c.match >= 90 ? 'text-brand-purple' : c.match >= 80 ? 'text-brand-blue' : 'text-muted-foreground',
                    ]"
                  >
                    {{ c.match }}%
                  </span>
                </div>
              </div>
              <div class="flex flex-wrap gap-1 mb-3">
                <span v-for="s in c.skills.slice(0, 3)" :key="s" class="text-xs px-2 py-0.5 rounded-full bg-accent text-accent-foreground">{{ s }}</span>
                <span v-if="c.skills.length > 3" class="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground">+{{ c.skills.length - 3 }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span :class="['text-xs px-2 py-0.5 rounded-full border', statusStyles[c.status]]">{{ c.status }}</span>
                <div class="flex items-center gap-1">
                  <button type="button" class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground">
                    <Bookmark :size="12" />
                  </button>
                  <button type="button" class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground">
                    <Send :size="12" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
