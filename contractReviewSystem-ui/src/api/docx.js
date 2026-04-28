import request from './request'

export function modifyDocx(data) {
  return request.post('/docx-agent/modify', data)
}
