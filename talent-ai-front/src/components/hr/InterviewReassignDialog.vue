<script setup lang="ts">
import { ref, watch } from 'vue'
import { UserRound, X } from 'lucide-vue-next'
import { fetchInterviewers, reassignInterview, type InterviewerOption } from '@/api/interview'
import { getErrorMessage } from '@/utils/validators'

const props = defineProps<{
  open: boolean
  interviewId: number | null
  candidateName?: string
  currentInterviewerName?: string
}>()

const emit = defineEmits<{
  close: []
  success: []
}>()

const saving = ref(false)
const loadingInterviewers = ref(false)
const formError = ref('')
const interviewers = ref<InterviewerOption[]>([])
const pickedInterviewerId = ref<number | ''>('')

watch(
  () => props.open,
  (open) => {
    if (!open) return
    formError.value = ''
    pickedInterviewerId.value = ''
    void loadInterviewers()
  },
)

async function loadInterviewers() {
  loadingInterviewers.value = true
  try {
    interviewers.value = await fetchInterviewers()
  } catch (e) {
    formError.value = getErrorMessage(e, '面试官列表加载失败')
  } finally {
    loadingInterviewers.value = false
  }
}

async function handleSubmit() {
  if (!props.interviewId || saving.value) return
  const interviewerId = Number(pickedInterviewerId.value)
  if (!Number.isFinite(interviewerId) || interviewerId <= 0) {
    formError.value = '请选择新的面试官'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    await reassignInterview(props.interviewId, interviewerId)
    emit('success')
    emit('close')
  } catch (e) {
    formError.value = getErrorMessage(e, '改派失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div
    v-if="open"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
    @click.self="emit('close')"
  >
    <div class="w-full max-w-md bg-card rounded-2xl shadow-card border border-border overflow-hidden">
      <div class="flex items-center justify-between px-5 py-4 border-b border-border">
        <div>
          <h2 class="text-base font-bold text-foreground">改派面试官</h2>
          <p v-if="candidateName" class="text-xs text-muted-foreground mt-0.5">
            {{ candidateName }}
            <span v-if="currentInterviewerName"> · 当前：{{ currentInterviewerName }}</span>
          </p>
        </div>
        <button type="button" class="p-1 rounded-lg hover:bg-muted" @click="emit('close')">
          <X :size="18" />
        </button>
      </div>

      <div class="p-5 space-y-4">
        <div>
          <label class="text-xs text-muted-foreground mb-1.5 block">新面试官</label>
          <div class="relative">
            <UserRound :size="14" class="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <select
              v-model="pickedInterviewerId"
              class="w-full pl-9 pr-3 py-2.5 rounded-xl bg-muted border border-transparent text-sm outline-none focus:border-brand-blue/40"
              :disabled="loadingInterviewers || saving"
            >
              <option value="">请选择面试官</option>
              <option v-for="item in interviewers" :key="item.id" :value="item.id">
                {{ item.nickname || item.account }}
              </option>
            </select>
          </div>
        </div>

        <p v-if="formError" class="text-xs text-red-600">{{ formError }}</p>

        <div class="flex gap-3 pt-1">
          <button
            type="button"
            class="flex-1 py-2.5 rounded-xl border border-border text-sm text-muted-foreground hover:bg-muted"
            :disabled="saving"
            @click="emit('close')"
          >
            取消
          </button>
          <button
            type="button"
            class="flex-1 py-2.5 rounded-xl gradient-blue text-white text-sm font-medium disabled:opacity-50"
            :disabled="saving || loadingInterviewers"
            @click="handleSubmit"
          >
            {{ saving ? '提交中...' : '确认改派' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
