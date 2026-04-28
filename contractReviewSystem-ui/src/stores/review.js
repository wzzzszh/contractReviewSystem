import { defineStore } from 'pinia'

function createDefaultForm() {
  return {
    perspective: 'PARTY_A',
    modificationRequirement: '',
    outputPath: ''
  }
}

export const useReviewStore = defineStore('review', {
  state: () => ({
    form: createDefaultForm(),
    fileList: [],
    selectedFile: null,
    uploadedRecord: null,
    uploadPercent: 0,
    submitting: false,
    result: null
  }),
  actions: {
    setSelectedFile(uploadFile, uploadFiles) {
      this.fileList = uploadFiles.slice(-1)
      this.selectedFile = uploadFile.raw
      this.uploadedRecord = null
      this.uploadPercent = 0
      this.result = null
    },
    clearSelectedFile() {
      this.fileList = []
      this.selectedFile = null
      this.uploadedRecord = null
      this.uploadPercent = 0
      this.result = null
    },
    reset() {
      this.form = createDefaultForm()
      this.fileList = []
      this.selectedFile = null
      this.uploadedRecord = null
      this.uploadPercent = 0
      this.submitting = false
      this.result = null
    }
  }
})
