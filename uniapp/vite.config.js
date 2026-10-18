import {
	defineConfig
} from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
// import path from 'path' // 如果需要 path 模块

export default defineConfig({
	plugins: [uni()],
	css: {
		preprocessorOptions: {
			// 普通 H5 使用 Vite 5.4 的现代 Sass API；其他端和 uni_modules 编译链需另行迁移。
			// DCloud 当前声明 Vite 5.2.8 peer，5.4 作为兼容例外需核验 H5 构建及 CSS 差异。
			scss: {
				api: 'modern',
			},
			sass: {
				api: 'modern',
			},
		},
	},
	server: {
		host: '0.0.0.0',
		port: 5174,
		headers: {
			'Cache-Control': 'no-store, no-cache, must-revalidate, proxy-revalidate',
			'Pragma': 'no-cache',
			'Expires': '0'
		},
		// 核心配置：代理
		proxy: {
			// weiran-region 的公开行政区划查询使用框架 API 路径，不经过 portal 路由改写。
			'/api/regions/lookup': {
				target: 'http://127.0.0.1:3300',
				changeOrigin: true,
			},
			// 后端 mono4j（weiran4j bootRun 默认端口 3300）；uniapp 接口统一走 /api-web/**
			'/api-web': {
				target: 'http://127.0.0.1:3300',
				changeOrigin: true,
			},
		}
	}
})
