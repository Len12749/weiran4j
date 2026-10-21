import { defineConfig } from 'vitepress'

export default defineConfig({
  base: '/weiran4j/',
  lang: 'zh-CN',
  title: 'weiran4j',
  description: '后台管理框架的使用指南与架构说明',
  cleanUrls: true,
  themeConfig: {
    nav: [
      { text: '使用指南', link: '/usage' },
      { text: '架构说明', link: '/architecture' },
      { text: '配置边界', link: '/configuration' },
    ],
    sidebar: [
      {
        text: '项目文档',
        items: [
          { text: '首页', link: '/' },
          { text: '使用指南', link: '/usage' },
          { text: '架构说明', link: '/architecture' },
          { text: '配置边界', link: '/configuration' },
        ],
      },
    ],
    search: {
      provider: 'local',
    },
    footer: {
      message: 'Java 21 · React · Spring Boot',
    },
  },
})
