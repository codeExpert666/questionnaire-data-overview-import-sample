<template>
  <div class="questionnaire-import-panel">
    <button type="button" :disabled="loading" @click="downloadTemplate">
      下载模板
    </button>

    <input
      ref="fileInput"
      type="file"
      accept=".xlsx"
      :disabled="loading"
      @change="handleFileChange"
    >

    <div v-if="result" class="success">
      导入成功：{{ result.questionnaireCount }} 份问卷，
      {{ result.opinionCount }} 条观点，
      {{ result.featureScoreCount }} 条特性评分。
    </div>

    <div v-if="errors.length" class="errors">
      <div v-for="(item, index) in errors" :key="index">
        <span v-if="item.rowNumber">第 {{ item.rowNumber }} 行</span>
        <span v-if="item.columnName">，{{ item.columnName }}</span>
        ：{{ item.message }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import {
  downloadQuestionnaireImportTemplate,
  importQuestionnaireExcel
} from './questionnaireOverviewApi'

const emit = defineEmits(['import-success'])
const loading = ref(false)
const result = ref(null)
const errors = ref([])
const fileInput = ref(null)

async function downloadTemplate() {
  loading.value = true
  errors.value = []
  try {
    await downloadQuestionnaireImportTemplate()
  } finally {
    loading.value = false
  }
}

async function handleFileChange(event) {
  const file = event.target.files?.[0]
  if (!file) return

  loading.value = true
  result.value = null
  errors.value = []
  try {
    result.value = await importQuestionnaireExcel(file)
    emit('import-success', result.value)
  } catch (error) {
    const body = error.response?.data
    errors.value = body?.errors?.length
      ? body.errors
      : [{ message: body?.message || '导入失败' }]
  } finally {
    loading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}
</script>
