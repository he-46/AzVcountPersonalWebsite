<template>
  <FrontLayout v-if="isFront">
    <router-view />
  </FrontLayout>
  <AdminLayout v-else>
    <router-view />
  </AdminLayout>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import FrontLayout from './layouts/FrontLayout.vue'
import AdminLayout from './layouts/AdminLayout.vue'

const route = useRoute()
const isFront = computed(() => route.meta.layout !== 'admin')
</script>
