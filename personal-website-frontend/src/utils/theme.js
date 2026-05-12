// 主题管理工具

// 检查本地存储中的主题设置
const getTheme = () => {
  return localStorage.getItem('theme') || 'light'
}

// 设置主题
const setTheme = (theme) => {
  localStorage.setItem('theme', theme)
  document.documentElement.setAttribute('data-theme', theme)
  document.body.style.backgroundColor = theme === 'dark' ? '#09090c' : '#f8f9fa'
  if (theme === 'dark') {
    document.documentElement.classList.add('dark')
  } else {
    document.documentElement.classList.remove('dark')
  }
}

// 切换主题
const toggleTheme = () => {
  const currentTheme = getTheme()
  const newTheme = currentTheme === 'light' ? 'dark' : 'light'
  setTheme(newTheme)
  return newTheme
}

// 初始化主题
const initTheme = () => {
  const theme = getTheme()
  setTheme(theme)
}

export {
  getTheme,
  setTheme,
  toggleTheme,
  initTheme
}