export function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}

export function formatBytes(value) {
  if (!Number.isFinite(value) || value <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let size = value
  let index = 0
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024
    index += 1
  }
  return `${size.toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}

export function fileNameFromPath(path) {
  if (!path) return ''
  const normalized = String(path).replaceAll('\\', '/')
  return normalized.split('/').pop()
}
