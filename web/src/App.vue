<template>
  <el-config-provider :locale="locale">
    <div class="app-container">
      <el-container>
        <el-aside width="200px">
          <div class="logo">终端监控</div>
          <el-menu
            :default-active="$route.path"
            router
            background-color="#304156"
            text-color="#bfcbd9"
            active-text-color="#409EFF"
          >
            <el-menu-item index="/dashboard">
              <el-icon><Odometer /></el-icon>
              <span>Dashboard</span>
            </el-menu-item>
            <el-menu-item index="/agents">
              <el-icon><Monitor /></el-icon>
              <span>Agent管理</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-container>
          <el-header>
            <div class="header-title">终端安全监控系统</div>
          </el-header>
          <el-main>
            <router-view />
          </el-main>
        </el-container>
      </el-container>
    </div>
  </el-config-provider>
</template>

<script setup>
import { ref } from 'vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

const locale = ref(zhCn)
</script>

<style scoped>
.app-container {
  height: 100vh;
}

.el-aside {
  background-color: #304156;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
}

.el-header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
}

.el-main {
  background: #f0f2f5;
}
</style>
