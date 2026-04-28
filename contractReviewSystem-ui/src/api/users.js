import request from './request'

export function createUser(data) {
  return request.post('/db/users', data)
}

export function listUsers() {
  return request.get('/db/users')
}

export function getUser(id) {
  return request.get(`/db/users/${id}`)
}
