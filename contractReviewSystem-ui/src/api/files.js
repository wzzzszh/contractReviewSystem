import request from './request'

export function uploadForCurrentUser(file, onUploadProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/file/upload/current-user', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress
  })
}

export function listFilesByUser(userId) {
  return request.get(`/db/files/user/${userId}`)
}

export function listFilesBySource(sourceFileId) {
  return request.get(`/db/files/source/${sourceFileId}`)
}

export function createFileRecord(data) {
  return request.post('/db/files', data)
}

export function deletePhysicalFile(path) {
  return request.delete('/file/delete', { params: { path } })
}

export function downloadFile(path) {
  return request.get('/file/download', {
    params: { path },
    responseType: 'blob'
  })
}
