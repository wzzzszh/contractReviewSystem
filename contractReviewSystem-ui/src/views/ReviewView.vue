<template>
  <div class="review-grid">
    <el-card shadow="never" class="review-form-card">
      <template #header>
        <div class="card-header">
          <span>发起审查</span>
          <el-tag type="info">DOCX</el-tag>
        </div>
      </template>

      <el-form label-position="top">
        <el-form-item label="合同文件">
          <el-upload
            v-model:file-list="fileList"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".docx"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <div>拖入或点击选择 Word 合同</div>
          </el-upload>
        </el-form-item>

        <el-form-item label="审查视角">
          <el-segmented v-model="form.perspective" :options="perspectiveOptions" />
        </el-form-item>

        <el-form-item label="关注点">
          <el-input
            v-model.trim="form.modificationRequirement"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="例如：重点关注付款、违约责任、解除条款"
          />
        </el-form-item>

        <el-form-item label="输出路径">
          <el-input v-model.trim="form.outputPath" placeholder="为空时自动生成 -modified.docx" />
        </el-form-item>

        <el-progress v-if="uploadPercent > 0 && uploadPercent < 100" :percentage="uploadPercent" />

        <el-button
          type="primary"
          size="large"
          :icon="DocumentChecked"
          :loading="submitting"
          class="full-button"
          @click="submitReview"
        >
          生成审查结果
        </el-button>
      </el-form>
    </el-card>

    <div class="page-stack">
      <el-card shadow="never">
        <template #header>
          <div class="card-header">
            <span>本次文件</span>
            <div class="header-actions">
              <el-button v-if="uploadedRecord" text type="primary" :icon="Download" @click="download(uploadedRecord.filePath)">
                下载原文
              </el-button>
              <el-popconfirm title="只清空页面上的本次记录，不删除文件" @confirm="clearReview">
                <template #reference>
                  <el-button :icon="Delete" :disabled="submitting">清空本次记录</el-button>
                </template>
              </el-popconfirm>
            </div>
          </div>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="文件名">{{ uploadedRecord?.fileName || selectedFile?.name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="上传路径">{{ uploadedRecord?.filePath || '-' }}</el-descriptions-item>
          <el-descriptions-item label="修改稿">{{ result?.outputPath || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never" v-if="result">
        <template #header>
          <div class="card-header">
            <span>审查结果</span>
            <el-button type="success" :icon="Download" @click="download(result.outputPath)">下载修改稿</el-button>
          </div>
        </template>

        <div class="result-stats">
          <el-statistic title="已应用修改" :value="result.appliedOperationCount || 0" />
          <el-statistic title="跳过修改" :value="result.skippedOperationCount || 0" />
          <el-statistic title="审查视角" :value="result.perspective === 'PARTY_B' ? '乙方' : '甲方'" />
        </div>

        <el-alert v-if="result.warningMessage" type="warning" :title="result.warningMessage" show-icon :closable="false" />

        <el-tabs model-value="risk">
          <el-tab-pane label="风险审查" name="risk">
            <pre class="text-block">{{ result.riskReviewReport || '暂无内容' }}</pre>
          </el-tab-pane>
          <el-tab-pane label="修改要求" name="requirement">
            <pre class="text-block">{{ result.generatedModificationRequirement || '暂无内容' }}</pre>
          </el-tab-pane>
          <el-tab-pane label="执行信息" name="messages">
            <pre class="text-block">{{ executionText }}</pre>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'
import { Delete, DocumentChecked, Download } from '@element-plus/icons-vue'
import { modifyDocx } from '../api/docx'
import { createFileRecord, downloadFile, uploadForCurrentUser } from '../api/files'
import { showError } from '../api/request'
import { useAuthStore } from '../stores/auth'
import { useReviewStore } from '../stores/review'
import { fileNameFromPath } from '../utils/format'

const auth = useAuthStore()
const review = useReviewStore()
const { form, fileList, selectedFile, uploadedRecord, uploadPercent, submitting, result } = storeToRefs(review)

const perspectiveOptions = [
  { label: '甲方', value: 'PARTY_A' },
  { label: '乙方', value: 'PARTY_B' }
]

const executionText = computed(() => {
  const messages = result.value?.skippedOperationMessages || []
  return [
    result.value?.resultMessage,
    result.value?.patchPlanPath ? `补丁计划：${result.value.patchPlanPath}` : '',
    ...messages
  ].filter(Boolean).join('\n')
})

function handleFileChange(uploadFile, uploadFiles) {
  review.setSelectedFile(uploadFile, uploadFiles)
}

function handleFileRemove() {
  review.clearSelectedFile()
}

async function submitReview() {
  if (!selectedFile.value && !uploadedRecord.value) {
    ElMessage.warning('请选择合同文件')
    return
  }

  submitting.value = true
  try {
    if (!uploadedRecord.value) {
      uploadedRecord.value = await uploadForCurrentUser(selectedFile.value, (event) => {
        if (event.total) {
          uploadPercent.value = Math.round((event.loaded * 100) / event.total)
        }
      })
    }

    const response = await modifyDocx({
      inputPath: uploadedRecord.value.filePath,
      outputPath: form.value.outputPath || undefined,
      modificationRequirement: form.value.modificationRequirement || undefined,
      perspective: form.value.perspective
    })
    result.value = response

    if (response.outputPath && auth.user?.userId) {
      try {
        await createFileRecord({
          userId: auth.user.userId,
          fileName: fileNameFromPath(response.outputPath),
          filePath: response.outputPath,
          fileCategory: 'modified',
          sourceFileId: uploadedRecord.value.id
        })
      } catch (recordError) {
        ElMessage.warning(recordError?.message || '修改稿记录保存失败')
      }
    }

    ElMessage.success('审查完成')
  } catch (error) {
    showError(error, '审查失败')
  } finally {
    submitting.value = false
    uploadPercent.value = 0
  }
}

function clearReview() {
  review.reset()
  ElMessage.success('已清空本次记录')
}

async function download(path) {
  if (!path) return
  try {
    const blob = await downloadFile(path)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileNameFromPath(path)
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    showError(error, '下载失败')
  }
}
</script>
