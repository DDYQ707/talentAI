<script setup lang="ts">
import { ref } from 'vue'
import { Search, Filter, User, Bookmark, Sparkles, Brain, MapPin, Briefcase, Tag, Plus, ChevronDown, Zap } from 'lucide-vue-next'

const talents = [
  { id: 1, name: '张工程', title: '高级前端工程师', exp: '7年', edu: '本科·清华', company: '字节跳动', location: '北京', match: 96, tags: ['React专家', '开源贡献者', '面试体验佳'], status: '在职-观望', saved: true },
  { id: 2, name: '李架构', title: '系统架构师', exp: '9年', edu: '硕士·复旦', company: '阿里巴巴', location: '杭州', match: 91, tags: ['分布式系统', '高并发', '团队领导力'], status: '主动求职', saved: true },
  { id: 3, name: '王算法', title: 'AI算法专家', exp: '6年', edu: '博士·中科大', company: '百度', location: '北京', match: 94, tags: ['LLM', '计算机视觉', '论文作者'], status: '被动求职', saved: false },
  { id: 4, name: '赵产品', title: '产品总监', exp: '8年', edu: 'MBA·北大', company: '美团', location: '北京', match: 88, tags: ['0-1产品', '增长黑客', '数据驱动'], status: '在职-观望', saved: false },
  { id: 5, name: '陈运营', title: '增长运营总监', exp: '7年', edu: '本科·武大', company: '滴滴', location: '上海', match: 85, tags: ['GMV增长', '私域运营', 'KOL合作'], status: '主动求职', saved: true },
  { id: 6, name: '刘设计', title: '体验设计总监', exp: '8年', edu: '硕士·中央美院', company: '腾讯', location: '深圳', match: 82, tags: ['用户体验', '设计系统', '品牌设计'], status: '被动求职', saved: false },
]

const statusColors: Record<string, string> = {
  主动求职: 'bg-green-50 text-brand-green border-green-200',
  被动求职: 'bg-brand-tint text-brand-blue border-brand-border',
  在职观望: 'bg-orange-50 text-brand-orange border-orange-200',
  '在职-观望': 'bg-orange-50 text-brand-orange border-orange-200',
}

const categories = [
  { label: '全部人才', count: 1248 },
  { label: '技术类', count: 486 },
  { label: '产品类', count: 213 },
  { label: '运营类', count: 178 },
  { label: '设计类', count: 156 },
  { label: '管理类', count: 215 },
]

const searchVal = ref('')
const activeCategory = ref('全部人才')
</script>

<template>
  <div data-cmp="TalentPool" class="flex h-full" style="height: calc(100vh - 64px)">
    <div class="w-56 flex-shrink-0 border-r border-border bg-card flex flex-col p-4">
      <div class="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">人才分类</div>
      <button
        v-for="c in categories"
        :key="c.label"
        type="button"
        class="w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs mb-1 transition-colors"
        :class="activeCategory === c.label ? 'bg-brand-blue text-white' : 'hover:bg-muted text-muted-foreground'"
        @click="activeCategory = c.label"
      >
        <span>{{ c.label }}</span>
        <span class="text-xs" :class="activeCategory === c.label ? 'text-white/90' : 'text-muted-foreground'">{{ c.count }}</span>
      </button>
      <div class="mt-4 pt-4 border-t border-border">
        <div class="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">标签筛选</div>
        <div v-for="tag in ['React专家', '分布式系统', 'LLM', '高并发', '0-1产品']" :key="tag" class="flex items-center gap-2 px-3 py-1.5 rounded-lg hover:bg-muted cursor-pointer">
          <Tag :size="11" class="text-brand-purple" />
          <span class="text-xs text-muted-foreground">{{ tag }}</span>
        </div>
      </div>
    </div>

    <div class="flex-1 flex flex-col min-w-0">
      <div class="px-6 py-4 border-b border-border flex items-center gap-3">
        <div class="flex items-center gap-2 bg-muted rounded-lg px-3 py-2 flex-1 max-w-lg">
          <Search :size="14" class="text-muted-foreground" />
          <input
            v-model="searchVal"
            class="bg-transparent text-sm outline-none flex-1 placeholder:text-muted-foreground"
            placeholder="AI语义搜索：会Python且了解金融行业的5年以上工程师..."
          />
        </div>
        <button type="button" class="flex items-center gap-1.5 px-4 py-2 rounded-lg gradient-purple text-white text-sm">
          <Sparkles :size="14" />
          <span>智能搜索</span>
        </button>
        <button type="button" class="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-border text-xs text-muted-foreground">
          <Filter :size="14" />
          <span>高级筛选</span>
        </button>
        <div class="ml-auto text-xs text-muted-foreground">共 {{ talents.length }} 位人才</div>
      </div>

      <div class="px-6 py-2 border-b border-border bg-muted/30 flex items-center gap-3">
        <span class="text-xs text-muted-foreground">排序：</span>
        <button v-for="s in ['匹配度', '最近添加', '活跃度']" :key="s" type="button" class="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground px-2 py-1 rounded">
          <span>{{ s }}</span>
          <ChevronDown :size="10" />
        </button>
        <div class="ml-auto flex items-center gap-2">
          <button type="button" class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-accent text-brand-purple text-xs border border-brand-border/50">
            <Zap :size="12" />
            <span>AI批量分析</span>
          </button>
          <button type="button" class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-border text-xs text-muted-foreground">
            <Plus :size="12" />
            <span>添加人才</span>
          </button>
        </div>
      </div>

      <div class="flex-1 overflow-y-auto scrollbar-thin p-6">
        <div class="flex flex-col gap-3">
          <div
            v-for="t in talents"
            :key="t.id"
            class="bg-card shadow-card hover:shadow-panel transition-shadow border border-border hover:border-brand-blue/20 cursor-pointer"
          >
            <div class="flex items-center gap-4 p-4">
              <div class="w-12 h-12 rounded-full gradient-blue-purple flex items-center justify-center flex-shrink-0">
                <User :size="20" class="text-white" />
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-3 mb-1">
                  <span class="text-sm font-bold text-foreground">{{ t.name }}</span>
                  <span class="text-xs text-muted-foreground">{{ t.title }}</span>
                  <span
                    :class="[
                      'text-xs px-2 py-0.5 rounded-full border',
                      statusColors[t.status] || 'bg-muted text-muted-foreground border-border',
                    ]"
                  >
                    {{ t.status }}
                  </span>
                </div>
                <div class="flex items-center gap-4 text-xs text-muted-foreground mb-2">
                  <span class="flex items-center gap-1"><Briefcase :size="10" />{{ t.company }}</span>
                  <span class="flex items-center gap-1"><MapPin :size="10" />{{ t.location }}</span>
                  <span>{{ t.exp }} · {{ t.edu }}</span>
                </div>
                <div class="flex flex-wrap gap-1">
                  <span v-for="tag in t.tags" :key="tag" class="text-xs px-2 py-0.5 rounded-full bg-accent text-accent-foreground">{{ tag }}</span>
                </div>
              </div>
              <div class="flex items-center gap-4 flex-shrink-0">
                <div class="text-center">
                  <div class="text-xl font-black text-brand-purple">{{ t.match }}%</div>
                  <div class="text-xs text-muted-foreground">匹配度</div>
                </div>
                <div class="flex flex-col gap-1">
                  <button type="button" :class="['p-2 rounded-lg hover:bg-muted', t.saved ? 'text-brand-orange' : 'text-muted-foreground']">
                    <Bookmark :size="16" :class="t.saved ? 'fill-brand-orange' : ''" />
                  </button>
                  <button type="button" class="p-2 rounded-lg hover:bg-muted text-muted-foreground hover:text-brand-purple">
                    <Brain :size="16" />
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
