import request from './request'

// 记录页面访问
const recordPageView = async () => {
  try {
    // 检查是否已经记录过今日访问
    const today = new Date().toISOString().split('T')[0]
    const hasVisited = localStorage.getItem(`visited_${today}`)
    
    if (!hasVisited) {
      // 新访客
      await request.post('/statistic/update', {
        pageViews: 1,
        uniqueVisitors: 1
      })
      localStorage.setItem(`visited_${today}`, 'true')
    } else {
      // 重复访问
      await request.post('/statistic/update', {
        pageViews: 1
      })
    }
  } catch (error) {
    console.error('记录页面访问失败:', error)
  }
}

// 记录文章阅读
const recordArticleRead = async () => {
  try {
    await request.post('/statistic/update', {
      articleReads: 1
    })
  } catch (error) {
    console.error('记录文章阅读失败:', error)
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
  recordArticleRead,
  getTodayStatistic,
  getTotalStatistic
}