import { onUnmounted } from 'vue'

export const MATCH_POLL_INTERVAL_MS = 4000
export const MATCH_POLL_MAX_ATTEMPTS = 15

/** 轮询匹配分；pollFn 返回 true 表示可停止；onExhausted 在达到最大次数仍未完成时回调 */
export function useMatchScorePoll(
  pollFn: () => Promise<boolean>,
  intervalMs = MATCH_POLL_INTERVAL_MS,
  maxAttempts = MATCH_POLL_MAX_ATTEMPTS,
  onExhausted?: () => void,
) {
  let timer: ReturnType<typeof setInterval> | undefined
  let attempts = 0
  let stopped = true

  function stop() {
    stopped = true
    if (timer) {
      clearInterval(timer)
      timer = undefined
    }
  }

  async function tick() {
    if (stopped) return
    attempts += 1
    try {
      const done = await pollFn()
      if (done) {
        stop()
        return
      }
      if (attempts >= maxAttempts) {
        onExhausted?.()
        stop()
      }
    } catch {
      if (attempts >= maxAttempts) {
        stop()
      }
    }
  }

  function start() {
    stop()
    stopped = false
    attempts = 0
    void tick()
    timer = setInterval(() => void tick(), intervalMs)
  }

  onUnmounted(stop)

  return { start, stop }
}
