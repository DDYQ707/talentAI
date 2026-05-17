<script setup lang="ts">
import { ref } from 'vue'
import {
  Bot,
  Send,
  User,
  Sparkles,
  Briefcase,
  Zap,
  Plus,
  Search,
  ThumbsUp,
  ThumbsDown,
  Copy,
  RefreshCw,
} from 'lucide-vue-next'

const historyItems = [
  { id: 1, title: '查找Python高级工程师', time: '10分钟前', active: true },
  { id: 2, title: '分析本月招聘漏斗数据', time: '1小时前', active: false },
  { id: 3, title: '生成产品经理JD', time: '3小时前', active: false },
  { id: 4, title: '候选人背景调查', time: '昨天', active: false },
  { id: 5, title: '批量筛选前端简历', time: '昨天', active: false },
  { id: 6, title: '面试问题生成', time: '2天前', active: false },
]

const quickCommands = [
  { icon: Search, label: '智能搜索候选人', cmd: '帮我找一位5年以上经验的Python后端工程师，熟悉分布式系统' },
  { icon: Briefcase, label: '生成岗位JD', cmd: '帮我写一份高级产品经理的职位描述' },
  { icon: Sparkles, label: '简历打分分析', cmd: '分析这份简历与前端工程师岗位的匹配度' },
  { icon: Zap, label: '招聘策略建议', cmd: '我们的面试通过率只有20%，如何提升？' },
]

const messages = [
  { role: 'user' as const, content: '帮我找5年以上Python经验的高级工程师，要熟悉分布式系统和微服务架构' },
  {
    role: 'ai' as const,
    content: '根据您的要求，我在人才库中找到了 **18位** 高匹配度候选人。以下是TOP 3推荐：',
    candidates: [
      { name: '张工程', exp: '7年经验', match: 96, skills: ['Python', '分布式', 'K8s', 'Go'], status: '待入职' },
      { name: '李架构', exp: '6年经验', match: 91, skills: ['Python', '微服务', 'Redis', 'Docker'], status: '在职-考虑' },
      { name: '王后端', exp: '5年经验', match: 87, skills: ['Python', 'Django', 'Kafka', 'AWS'], status: '求职中' },
    ],
  },
  { role: 'user' as const, content: '张工程的背景怎么样？详细分析一下' },
  {
    role: 'ai' as const,
    content:
      '**张工程 - 深度分析报告**\n\n该候选人综合评分96分，属于强烈推荐级别。主要优势在于：\n\n✅ **技术深度**：7年Python经验，主导过日均千万级请求的分布式系统\n✅ **架构能力**：设计过3套微服务架构，性能提升40%+\n✅ **稳定性高**：近3份工作平均在职2.5年，无频繁跳槽迹象\n\n⚠️ **风险提示**：当前薪资期望比市场高15%，谈判空间有限',
  },
]

const candidateResults = [
  { name: '张工程', match: 96, exp: '7年', current: '阿里巴巴', skills: ['Python', '分布式', 'K8s'] },
  { name: '李架构', match: 91, exp: '6年', current: '字节跳动', skills: ['Python', '微服务', 'Redis'] },
  { name: '王后端', match: 87, exp: '5年', current: '腾讯', skills: ['Python', 'Django', 'Kafka'] },
  { name: '陈系统', match: 84, exp: '5年', current: '美团', skills: ['Python', 'Go', 'MySQL'] },
]

const input = ref('')

function applyQuick(cmd: string) {
  input.value = cmd
}
</script>

<template>
  <div data-cmp="AIAssistant" class="flex h-full" style="height: calc(100vh - 64px)">
    <div class="w-56 flex-shrink-0 border-r border-border bg-card flex flex-col">
      <div class="p-4 border-b border-border">
        <button type="button" class="w-full flex items-center justify-center gap-2 py-2.5 rounded-control gradient-blue text-white text-sm font-medium">
          <Plus :size="14" />
          <span>新建对话</span>
        </button>
      </div>
      <div class="flex-1 overflow-y-auto scrollbar-thin p-2">
        <div class="text-xs text-muted-foreground px-2 py-2 uppercase tracking-wider">历史记录</div>
        <div
          v-for="item in historyItems"
          :key="item.id"
          class="px-3 py-2.5 rounded-xl cursor-pointer mb-1"
          :class="item.active ? 'bg-accent border border-brand-purple/20' : 'hover:bg-muted'"
        >
          <div :class="['text-xs font-medium truncate', item.active ? 'text-brand-purple' : 'text-foreground']">
            {{ item.title }}
          </div>
          <div class="text-xs text-muted-foreground mt-0.5">{{ item.time }}</div>
        </div>
      </div>
    </div>

    <div class="flex-1 flex flex-col min-w-0">
      <div class="px-4 py-3 border-b border-border bg-muted/50 flex items-center gap-2 overflow-x-auto">
        <span class="text-xs text-muted-foreground flex-shrink-0">快捷指令：</span>
        <button
          v-for="cmd in quickCommands"
          :key="cmd.label"
          type="button"
          class="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-card border border-border text-xs text-foreground hover:border-brand-purple/40 hover:bg-accent transition-colors flex-shrink-0"
          @click="applyQuick(cmd.cmd)"
        >
          <component :is="cmd.icon" :size="12" class="text-brand-purple" />
          <span>{{ cmd.label }}</span>
        </button>
      </div>

      <div class="flex-1 overflow-y-auto scrollbar-thin p-6 space-y-6">
        <div v-for="(msg, i) in messages" :key="i" class="flex gap-3" :class="msg.role === 'user' ? 'justify-end' : 'justify-start'">
          <template v-if="msg.role === 'ai'">
            <div class="w-8 h-8 rounded-xl gradient-purple flex items-center justify-center flex-shrink-0 mt-1">
              <Bot :size="14" class="text-white" />
            </div>
            <div class="max-w-lg">
              <div class="bg-card rounded-2xl rounded-tl-sm px-4 py-3 text-sm text-foreground shadow-card">
                <div class="whitespace-pre-line leading-relaxed">{{ msg.content }}</div>
              </div>
              <div v-if="'candidates' in msg && msg.candidates" class="mt-3 space-y-2">
                <div
                  v-for="c in msg.candidates"
                  :key="c.name"
                  class="bg-card rounded-xl p-3 shadow-card border border-border"
                >
                  <div class="flex items-center justify-between mb-2">
                    <div class="flex items-center gap-2">
                      <div class="w-7 h-7 rounded-full gradient-blue flex items-center justify-center">
                        <User :size="12" class="text-white" />
                      </div>
                      <span class="text-sm font-medium text-foreground">{{ c.name }}</span>
                      <span class="text-xs text-muted-foreground">{{ c.exp }}</span>
                    </div>
                    <div class="flex items-center gap-2">
                      <div class="text-xs px-2 py-0.5 rounded-full bg-green-50 text-brand-green border border-green-200">
                        {{ c.status }}
                      </div>
                      <div class="text-sm font-bold text-brand-purple">{{ c.match }}%</div>
                    </div>
                  </div>
                  <div class="flex flex-wrap gap-1">
                    <span v-for="s in c.skills" :key="s" class="text-xs px-2 py-0.5 rounded-full bg-accent text-accent-foreground">
                      {{ s }}
                    </span>
                  </div>
                </div>
              </div>
              <div class="flex items-center gap-2 mt-2">
                <button type="button" class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground transition-colors">
                  <ThumbsUp :size="12" />
                </button>
                <button type="button" class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground transition-colors">
                  <ThumbsDown :size="12" />
                </button>
                <button type="button" class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground transition-colors">
                  <Copy :size="12" />
                </button>
                <button type="button" class="p-1.5 rounded-lg hover:bg-muted text-muted-foreground hover:text-foreground transition-colors">
                  <RefreshCw :size="12" />
                </button>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="max-w-lg order-first flex gap-3 justify-end items-start">
              <div class="gradient-blue text-white rounded-2xl rounded-tr-sm px-4 py-3 text-sm">{{ msg.content }}</div>
              <div class="w-8 h-8 rounded-control gradient-blue flex items-center justify-center flex-shrink-0 mt-1">
                <User :size="14" class="text-white" />
              </div>
            </div>
          </template>
        </div>
      </div>

      <div class="p-4 border-t border-border">
        <div class="flex items-end gap-3 bg-card rounded-2xl border border-border p-3 shadow-card">
          <div class="flex-1">
            <textarea
              v-model="input"
              rows="2"
              class="w-full bg-transparent text-sm outline-none resize-none text-foreground placeholder:text-muted-foreground"
              placeholder="向AI助手发送指令，例如：帮我找5年经验的产品经理..."
            />
          </div>
          <button type="button" class="w-9 h-9 rounded-xl gradient-purple flex items-center justify-center flex-shrink-0">
            <Send :size="16" class="text-white" />
          </button>
        </div>
        <div class="text-xs text-muted-foreground text-center mt-2">AI助手基于真实数据分析，建议结合人工判断</div>
      </div>
    </div>

    <div class="w-72 flex-shrink-0 border-l border-border bg-card flex flex-col">
      <div class="p-4 border-b border-border">
        <div class="flex items-center gap-2">
          <Sparkles :size="16" class="text-brand-purple" />
          <span class="text-sm font-semibold text-foreground">推荐候选人</span>
          <span class="ml-auto text-xs text-muted-foreground">共18人</span>
        </div>
      </div>
      <div class="flex-1 overflow-y-auto scrollbar-thin p-3 space-y-3">
        <div v-for="c in candidateResults" :key="c.name" class="bg-muted rounded-xl p-3 hover:bg-accent transition-colors cursor-pointer">
          <div class="flex items-center gap-2 mb-2">
            <div class="w-8 h-8 rounded-full gradient-blue-purple flex items-center justify-center flex-shrink-0">
              <User :size="12" class="text-white" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="text-xs font-semibold text-foreground">{{ c.name }}</div>
              <div class="text-xs text-muted-foreground truncate">{{ c.current }} · {{ c.exp }}</div>
            </div>
            <div class="text-sm font-bold text-brand-purple">{{ c.match }}%</div>
          </div>
          <div class="flex flex-wrap gap-1">
            <span v-for="s in c.skills" :key="s" class="text-xs px-1.5 py-0.5 rounded bg-card text-muted-foreground">{{ s }}</span>
          </div>
        </div>
      </div>
      <div class="p-3 border-t border-border">
        <button type="button" class="w-full py-2 rounded-xl border border-brand-blue text-brand-blue text-xs font-medium hover:bg-brand-tint transition-colors">
          查看全部候选人
        </button>
      </div>
    </div>
  </div>
</template>
