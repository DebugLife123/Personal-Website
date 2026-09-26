import request from './request'

// 本地时区的 YYYY-MM-DD（不用 toISOString，避免 UTC 与本地日期错位）
const localToday = () => {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

// 记录页面访问（计数由服务端原子累加，前端只上报"是否今日首次访问"）
const recordPageView = async () => {
  try {
    const key = `visited_${localToday()}`
    const firstVisit = !localStorage.getItem(key)
    await request.post('/statistic/visit', { firstVisit })
    if (firstVisit) {
      localStorage.setItem(key, 'true')
    }
  } catch (error) {
    console.error('记录页面访问失败:', error)
  }
}

// 获取今日统计数据
const getTodayStatistic = async () => {
  try {
    const res = await request.get('/statistic/today')
    if (res.data.code === 200) {
      return res.data.data
    }
  } catch (error) {
    console.error('获取今日统计数据失败:', error)
  }
  return null
}

// 获取总统计数据
const getTotalStatistic = async () => {
  try {
    const res = await request.get('/statistic/total')
    if (res.data.code === 200) {
      return res.data.data
    }
  } catch (error) {
    console.error('获取总统计数据失败:', error)
  }
  return null
}

export {
  recordPageView,
  getTodayStatistic,
  getTotalStatistic
}
