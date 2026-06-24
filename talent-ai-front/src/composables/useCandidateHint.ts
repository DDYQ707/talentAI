import { inject, provide, ref, type InjectionKey, type Ref } from 'vue'

export interface CandidateHintApi {
  hintText: Ref<string>
  visible: Ref<boolean>
  showComingSoon: (featureName?: string) => void
}

const CandidateHintKey: InjectionKey<CandidateHintApi> = Symbol('candidateHint')

export function provideCandidateHint(): CandidateHintApi {
  const hintText = ref('')
  const visible = ref(false)
  let timer: ReturnType<typeof setTimeout> | undefined

  function showComingSoon(featureName?: string) {
    hintText.value = featureName ? `${featureName}即将上线，敬请期待` : '功能即将上线，敬请期待'
    visible.value = true
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      visible.value = false
    }, 2400)
  }

  const api: CandidateHintApi = { hintText, visible, showComingSoon }
  provide(CandidateHintKey, api)
  return api
}

export function useCandidateHint(): CandidateHintApi {
  const api = inject(CandidateHintKey)
  if (!api) {
    throw new Error('useCandidateHint must be used within MobileLayout')
  }
  return api
}
