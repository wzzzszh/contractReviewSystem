import request from './request'

export function getHealth() {
  return request.get('/monitor/health')
}

export function getSystemInfo() {
  return request.get('/monitor/info')
}
