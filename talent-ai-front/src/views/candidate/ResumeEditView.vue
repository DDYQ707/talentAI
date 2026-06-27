<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChevronLeft } from 'lucide-vue-next'
import OnlineResumeEditor from '@/components/candidate/OnlineResumeEditor.vue'

const router = useRouter()
const route = useRoute()

const editorRef = ref<InstanceType<typeof OnlineResumeEditor> | null>(null)
const resumeId = ref<number | null>(null)

onMounted(() => {
  const id = Number(route.query.id)
  resumeId.value = Number.isFinite(id) && id > 0 ? id : null
})

function goBack() {
  router.push('/candidate/resume')
}
</script>

<template>
  <div data-cmp="ResumeEdit" class="flex h-full flex-col bg-[#EBF4F0]">
    <div class="flex items-center gap-3 px-4 py-3 border-b border-border bg-card flex-shrink-0">
      <button type="button" class="p-1.5 rounded-lg hover:bg-muted" @click="goBack">
        <ChevronLeft :size="20" />
      </button>
      <span class="text-sm font-semibold text-foreground flex-1">编辑在线简历</span>
      <button
        type="button"
        class="text-xs text-brand-blue font-medium px-2 py-1"
        @click="editorRef?.handleSave?.()"
      >
        保存
      </button>
    </div>

    <div class="flex-1 overflow-y-auto scrollbar-thin px-4 py-4 pb-6">
      <OnlineResumeEditor
        ref="editorRef"
        :active="true"
        :initial-resume-id="resumeId"
        :focus-section="(route.query.section as 'education' | 'work' | 'projects' | 'skills' | 'certificates') || ''"
        @saved="goBack"
      />
    </div>
  </div>
</template>
