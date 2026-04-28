import axios from 'axios'
import { ElMessage } from 'element-plus'

const ACCESS_TOKEN_KEY = 'contract_review_access_token'
const REFRESH_TOKEN_KEY = 'contract_review_refresh_token'
const USER_KEY = 'contract_review_user'
const LEGACY_TOKEN_KEY = 'contract_review_token'

export const storage = {
  getToken() {
    return localStorage.getItem(ACCESS_TOKEN_KEY) || localStorage.getItem(LEGACY_TOKEN_KEY)
  },
  getRefreshToken() {
    return localStorage.getItem(REFRESH_TOKEN_KEY)
  },
  setSession(payload) {
    const accessToken = payload.accessToken || payload.token
    const session = { ...payload, token: accessToken }

    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
    localStorage.setItem(LEGACY_TOKEN_KEY, accessToken)
    if (payload.refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, payload.refreshToken)
    }
    localStorage.setItem(USER_KEY, JSON.stringify(session))
  },
  updateTokens(payload) {
    const current = this.getUser() || {}
    this.setSession({ ...current, ...payload })
  },
  getUser() {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  },
  clearSession() {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(LEGACY_TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 120000
})

let refreshPromise = null

request.interceptors.request.use((config) => {
  const token = storage.getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  async (response) => {
    const result = response.data
    if (result instanceof Blob) {
      return result
    }
    if (result && typeof result.code !== 'undefined') {
      if (result.code === 200) {
        return result.data
      }
      if (result.code === 401) {
        return refreshAndRetry(response.config)
      }
      return Promise.reject(new Error(result.message || '请求失败'))
    }
    return result
  },
  async (error) => {
    if (error.response?.status === 401) {
      return refreshAndRetry(error.config)
    }
    return Promise.reject(toRequestError(error))
  }
)

async function refreshAndRetry(originalConfig) {
  if (!originalConfig || originalConfig._retry || originalConfig.skipAuthRefresh) {
    return rejectAndRedirect()
  }

  const refreshToken = storage.getRefreshToken()
  if (!refreshToken) {
    return rejectAndRedirect()
  }

  originalConfig._retry = true
  const tokenPayload = await refreshAccessToken(refreshToken)
  originalConfig.headers = originalConfig.headers || {}
  originalConfig.headers.Authorization = `Bearer ${tokenPayload.accessToken}`
  return request(originalConfig)
}

async function refreshAccessToken(refreshToken) {
  // Only one refresh request is allowed at a time; concurrent 401s wait here.
  if (!refreshPromise) {
    refreshPromise = request
      .post('/auth/refresh', { refreshToken }, { skipAuthRefresh: true })
      .then((payload) => {
        storage.updateTokens(payload)
        return payload
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

function rejectAndRedirect() {
  storage.clearSession()
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
  return Promise.reject(new Error('登录已失效，请重新登录'))
}

function toRequestError(error) {
  const data = error.response?.data
  if (data?.message) {
    const requestError = new Error(data.message)
    requestError.code = data.code
    requestError.data = data.data
    return requestError
  }
  return error
}

export function showError(error, fallback = '操作失败') {
  ElMessage.error(error?.message || fallback)
}

export default request
