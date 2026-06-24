<script setup lang="ts">
import { Sparkles } from 'lucide-vue-next'
import type { CandidateHintApi } from '@/composables/useCandidateHint'

defineProps<{
  hint: CandidateHintApi
  aboveTabBar?: boolean
}>()
</script>

<template>
  <Transition
    enter-active-class="transition duration-200 ease-out"
    enter-from-class="opacity-0 translate-y-2"
    enter-to-class="opacity-100 translate-y-0"
    leave-active-class="transition duration-150 ease-in"
    leave-from-class="opacity-100 translate-y-0"
    leave-to-class="opacity-0 translate-y-2"
  >
    <div
      v-if="hint.visible"
      class="pointer-events-none fixed inset-x-0 z-[100] flex justify-center px-4"
      :class="aboveTabBar ? 'bottom-20' : 'bottom-6'"
      style="padding-bottom: env(safe-area-inset-bottom, 0px)"
      role="status"
      aria-live="polite"
    >
      <div
        class="max-w-sm rounded-2xl border border-brand-border bg-card/95 px-4 py-3 shadow-lg backdrop-blur-md flex items-start gap-2"
      >
        <Sparkles :size="16" class="text-brand-purple shrink-0 mt-0.5" />
        <p class="text-xs text-foreground leading-relaxed">{{ hint.hintText }}</p>
      </div>
    </div>
  </Transition>
</template>
