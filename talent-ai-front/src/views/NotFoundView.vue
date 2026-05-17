<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { onMounted } from 'vue'

const route = useRoute()
const router = useRouter()

onMounted(() => {
  console.error('404 Error: User attempted to access non-existent route:', route.path)
})
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-gray-100">
    <div class="text-center">
      <h1 class="mb-4 text-4xl font-bold">404</h1>
      <p class="mb-4 text-xl text-gray-600">页面不存在</p>
      <button type="button" class="text-[#3d8b7a] underline hover:text-[#2d6b5e]" @click="router.push('/login')">
        返回登录
      </button>
    </div>
  </div>
</template>
