import { ref, type Ref } from 'vue'

export interface CandidateHintApi {
  hintText: Ref<string>
  visible: Ref<boolean>
  showComingSoon: (featureName?: string) => void
}

/** 占位功能提示（已移除悬浮 Toast，保留空实现供页面调用） */
export function useCandidateHint(): CandidateHintApi {
  return {
    hintText: ref(''),
    visible: ref(false),
    showComingSoon: () => {},
  }
}
