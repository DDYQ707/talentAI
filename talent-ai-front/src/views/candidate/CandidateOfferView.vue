<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Briefcase,
  Building2,
  Calendar,
  CheckCircle,
  DollarSign,
  FileText,
  Loader2,
  MailCheck,
  MailX,
  XCircle,
} from 'lucide-vue-next'
import {
  acceptOffer,
  fetchOfferByApplication,
  OFFER_STATUS,
  rejectOffer,
  type OfferDetailVO,
} from '@/api/offer'
import { getErrorMessage } from '@/utils/validators'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const submitting = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const offer = ref<OfferDetailVO | null>(null)

const applicationId = computed(() => {
  const id = Number(route.query.applicationId)
  return Number.isFinite(id) && id > 0 ? id : null
})

const canRespond = computed(() => offer.value?.status === OFFER_STATUS.ISSUED)
const isAccepted = computed(() => offer.value?.status === OFFER_STATUS.ACCEPTED)
const isDeclined = computed(() => offer.value?.status === OFFER_STATUS.DECLINED)

function formatSalary(amount?: number | null) {
  if (amount == null || amount <= 0) return '—'
  if (amount >= 1000) return `${Math.round(amount / 1000)}K`
  return String(amount)
}

function formatDate(dateStr?: string | null) {
  if (!dateStr) return '—'
  return dateStr.slice(0, 10)
}

async function loadOffer() {
  if (!applicationId.value) {
    errorMsg.value = '缺少投递 ID'
    loading.value = false
    return
  }
  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    offer.value = await fetchOfferByApplication(applicationId.value)
    if (!offer.value) {
      errorMsg.value = '暂未收到 Offer，请耐心等待 HR 发放'
    }
  } catch (e) {
    errorMsg.value = getErrorMessage(e, 'Offer 加载失败')
    offer.value = null
  } finally {
    loading.value = false
  }
}

async function handleAccept() {
  if (!offer.value || !canRespond.value || submitting.value) return
  if (!window.confirm('确认接受该 Offer？接受后将进入入职准备阶段。')) return
  submitting.value = true
  errorMsg.value = ''
  try {
    await acceptOffer(offer.value.id)
    successMsg.value = '您已成功接受 Offer'
    await loadOffer()
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '接受失败')
  } finally {
    submitting.value = false
  }
}

async function handleReject() {
  if (!offer.value || !canRespond.value || submitting.value) return
  if (!window.confirm('确认拒绝该 Offer？拒绝后本次录用流程将结束。')) return
  submitting.value = true
  errorMsg.value = ''
  try {
    await rejectOffer(offer.value.id)
    successMsg.value = '您已拒绝 Offer'
    await loadOffer()
  } catch (e) {
    errorMsg.value = getErrorMessage(e, '拒绝失败')
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.push('/candidate/applications')
}

onMounted(() => {
  void loadOffer()
})
</script>

<template>
  <div data-cmp="CandidateOffer" class="flex h-full flex-col bg-[#EBF4F0]">
    <div class="px-4 py-4 bg-card border-b border-border flex-shrink-0">
      <button type="button" class="flex items-center gap-1 text-xs text-muted-foreground mb-2" @click="goBack">
        <ArrowLeft :size="14" />
        返回投递记录
      </button>
      <h1 class="text-base font-bold text-foreground">Offer 确认</h1>
      <p v-if="offer" class="text-xs text-muted-foreground mt-0.5">{{ offer.jobTitle }} · {{ offer.offerNo }}</p>
    </div>

    <div class="flex-1 overflow-y-auto scrollbar-thin px-4 py-4 space-y-4">
      <p v-if="errorMsg" class="text-xs text-red-600 bg-red-50 border border-red-100 rounded-xl px-3 py-2">
        {{ errorMsg }}
      </p>
      <p v-if="successMsg" class="text-xs text-brand-green bg-green-50 border border-green-100 rounded-xl px-3 py-2">
        {{ successMsg }}
      </p>

      <div v-if="loading" class="text-center text-xs text-muted-foreground py-12">加载中...</div>

      <template v-else-if="offer">
        <div
          v-if="isAccepted"
          class="flex items-center gap-2 rounded-xl bg-green-50 border border-green-200 px-4 py-3 text-sm text-brand-green"
        >
          <MailCheck :size="18" />
          <span>您已接受此 Offer，请留意入职安排。</span>
        </div>
        <div
          v-else-if="isDeclined"
          class="flex items-center gap-2 rounded-xl bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-600"
        >
          <MailX :size="18" />
          <span>您已拒绝此 Offer。</span>
        </div>
        <div
          v-else-if="canRespond"
          class="flex items-center gap-2 rounded-xl bg-brand-tint border border-brand-border px-4 py-3 text-sm text-brand-blue"
        >
          <CheckCircle :size="18" />
          <span>HR 已发放 Offer，请在下方确认是否接受。</span>
        </div>
        <div
          v-else
          class="flex items-center gap-2 rounded-xl bg-muted/60 border border-border px-4 py-3 text-sm text-muted-foreground"
        >
          <FileText :size="18" />
          <span>当前状态：{{ offer.statusText }}</span>
        </div>

        <div class="bg-card rounded-xl border border-border shadow-card p-4 space-y-4">
          <div class="flex items-center gap-2">
            <Briefcase :size="16" class="text-brand-blue" />
            <h2 class="text-sm font-bold text-foreground">岗位信息</h2>
          </div>
          <div class="grid grid-cols-1 gap-3 text-sm">
            <div class="flex items-start gap-2">
              <Building2 :size="14" class="text-muted-foreground mt-0.5" />
              <div>
                <div class="text-muted-foreground text-xs">部门</div>
                <div class="font-medium">{{ offer.deptName || '—' }}</div>
              </div>
            </div>
            <div class="flex items-start gap-2">
              <FileText :size="14" class="text-muted-foreground mt-0.5" />
              <div>
                <div class="text-muted-foreground text-xs">职级</div>
                <div class="font-medium">{{ offer.positionLevel || '—' }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="bg-card rounded-xl border border-border shadow-card p-4 space-y-4">
          <div class="flex items-center gap-2">
            <DollarSign :size="16" class="text-brand-purple" />
            <h2 class="text-sm font-bold text-foreground">薪酬待遇</h2>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="rounded-xl bg-muted/50 px-3 py-2">
              <div class="text-xs text-muted-foreground">月薪</div>
              <div class="text-lg font-bold text-brand-purple">{{ formatSalary(offer.baseSalary) }}</div>
            </div>
            <div class="rounded-xl bg-muted/50 px-3 py-2">
              <div class="text-xs text-muted-foreground">年薪</div>
              <div class="text-lg font-bold text-brand-purple">{{ formatSalary(offer.annualSalary) }}</div>
            </div>
            <div v-if="offer.bonus" class="rounded-xl bg-muted/50 px-3 py-2 col-span-2">
              <div class="text-xs text-muted-foreground">奖金</div>
              <div class="text-sm font-semibold">{{ formatSalary(offer.bonus) }}</div>
            </div>
          </div>
          <p v-if="offer.salaryRemark" class="text-xs text-muted-foreground leading-relaxed">
            {{ offer.salaryRemark }}
          </p>
        </div>

        <div class="bg-card rounded-xl border border-border shadow-card p-4 space-y-3">
          <div class="flex items-center gap-2">
            <Calendar :size="16" class="text-brand-green" />
            <h2 class="text-sm font-bold text-foreground">入职安排</h2>
          </div>
          <div class="text-sm">
            <span class="text-muted-foreground">预计入职：</span>
            <span class="font-medium">{{ formatDate(offer.expectedOnboardDate) }}</span>
          </div>
          <div class="text-sm">
            <span class="text-muted-foreground">试用期：</span>
            <span class="font-medium">{{ offer.probationMonths ?? 3 }} 个月</span>
          </div>
          <p v-if="offer.remark" class="text-xs text-muted-foreground leading-relaxed rounded-lg bg-muted/40 px-3 py-2">
            {{ offer.remark }}
          </p>
        </div>

        <div v-if="canRespond" class="grid grid-cols-2 gap-3 pt-2">
          <button
            type="button"
            class="flex items-center justify-center gap-2 rounded-xl border border-red-200 bg-red-50 py-3 text-sm font-medium text-red-600 disabled:opacity-50"
            :disabled="submitting"
            @click="handleReject"
          >
            <XCircle :size="16" />
            拒绝 Offer
          </button>
          <button
            type="button"
            class="flex items-center justify-center gap-2 rounded-xl gradient-blue py-3 text-sm font-medium text-white disabled:opacity-50"
            :disabled="submitting"
            @click="handleAccept"
          >
            <Loader2 v-if="submitting" :size="16" class="animate-spin" />
            <CheckCircle v-else :size="16" />
            接受 Offer
          </button>
        </div>
      </template>
    </div>
  </div>
</template>
