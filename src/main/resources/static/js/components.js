/**
 * 公共组件
 */
const { ref, computed, onMounted, watch, h } = Vue;
const { ElMessage, ElMessageBox } = ElementPlus;

// 布局组件
const Layout = {
  template: `
    <el-container class="layout-container">
      <el-header class="layout-header">
        <div class="logo" @click="$router.push('/')">KoalaWiki</div>
        <el-menu mode="horizontal" :router="true" :default-active="$route.path" class="header-menu">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/warehouses">仓库</el-menu-item>
          <el-menu-item index="/domains">领域管理</el-menu-item>
          <el-menu-item index="/agents">Agents</el-menu-item>
        </el-menu>
      </el-header>
      <el-main class="layout-main">
        <router-view></router-view>
      </el-main>
    </el-container>
  `
};

// Markdown渲染组件
const MarkdownViewer = {
  props: ['content'],
  template: `<div class="markdown-body" v-html="renderedContent"></div>`,
  setup(props) {
    const renderedContent = computed(() => {
      if (!props.content) return '';
      marked.setOptions({
        highlight: (code, lang) => {
          if (lang && hljs.getLanguage(lang)) {
            return hljs.highlight(code, { language: lang }).value;
          }
          return code;
        }
      });
      return marked.parse(props.content);
    });
    return { renderedContent };
  }
};
