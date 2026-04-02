<template>
  <el-menu
    mode="horizontal"
    :ellipsis="false"
    router
    default-active="/"
    class="navbar-menu"
  >
    <el-menu-item index="/">首页</el-menu-item>
    <el-menu-item index="/resume">在线简历</el-menu-item>
    <el-menu-item index="/projects">项目展示</el-menu-item>
    <el-menu-item index="/message">留言板</el-menu-item>
  </el-menu>
</template>

<style scoped>
.navbar-menu {
  justify-content: center; /* 导航居中展示 */
  border-bottom: 1px solid #ddd;
}
</style>