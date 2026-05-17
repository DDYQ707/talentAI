<script setup lang="ts">
import { useId } from 'vue'

/** 小屏装饰：简化立方体（大屏右侧使用 public PNG） */
const uid = `lc${useId().replace(/[^a-zA-Z0-9]/g, '')}`
</script>

<template>
  <svg class="w-full h-auto" viewBox="0 0 320 200" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
      <linearGradient :id="`${uid}-mt`" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="#8fd4f0" />
        <stop offset="100%" stop-color="#4eb8e8" />
      </linearGradient>
      <linearGradient :id="`${uid}-ml`" x1="0%" y1="0%" x2="0%" y2="100%">
        <stop offset="0%" stop-color="#3d9ccc" />
        <stop offset="100%" stop-color="#2a7aad" />
      </linearGradient>
      <linearGradient :id="`${uid}-mr`" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" stop-color="#b8ecff" />
        <stop offset="100%" stop-color="#6ec8ef" />
      </linearGradient>
    </defs>
    <path d="M160 36 L238 88 L160 140 L82 88 Z" :fill="`url(#${uid}-mt)`" />
    <path d="M160 140 L238 88 L238 168 L160 220 Z" :fill="`url(#${uid}-ml)`" />
    <path d="M160 140 L82 88 L82 168 L160 220 Z" :fill="`url(#${uid}-mr)`" />
    <text x="160" y="108" text-anchor="middle" fill="white" font-size="22" font-weight="700">AI</text>
  </svg>
</template>
