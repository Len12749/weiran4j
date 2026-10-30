// 必须最先导入：Semi 的 Toast / Modal 等命令式 API 在 React 19 下依赖这个适配
import '@douyinfe/semi-ui/react19-adapter';
import { LocaleProvider } from '@douyinfe/semi-ui';
import zh_CN from '@douyinfe/semi-ui/lib/es/locale/source/zh_CN';
import { QueryClientProvider } from '@tanstack/react-query';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { App } from './App';
import { config, setAppTitle } from './config';
import { queryClient } from './lib/query';
import { PreferencesProvider } from './hooks/PreferencesProvider';
import { bootstrapTheme } from './lib/preferences-storage';
import { ThemeProvider } from './providers/ThemeProvider';
import { http } from './utils/request';
import './styles/global.css';

dayjs.locale('zh-cn');

/** 站点标题的配置键，值在「系统配置」里维护 */
const APP_TITLE_CONFIG_KEY = 'system.general.title';

/**
 * 启动时读平台配置的标题，覆盖构建时的 VITE_APP_TITLE。
 * 800ms 超时：同源本地请求正常几十毫秒，超时就直接用兜底标题，不拖住首屏。
 */
async function bootstrapAppTitle(): Promise<void> {
    try {
        const data = await http.get<{ configValue?: string }>(
            `/api-backend/n/system/config/getPublic/${APP_TITLE_CONFIG_KEY}`,
            { silent: true, signal: AbortSignal.timeout(800) },
        );
        setAppTitle(data?.configValue);
    } catch {
        // 配置读不到（后端未就绪、键被删）时沿用兜底标题
    }
}

async function bootstrap(): Promise<void> {
    await bootstrapAppTitle();
    document.title = config.appTitle;
    // 挂载前同步应用已存主题（偏好里的明暗 + 主色；顺带迁移旧版 weiran_theme），避免深色用户首屏闪白
    bootstrapTheme();

    const container = document.getElementById('root');
    if (!container) {
        throw new Error('找不到 #root 挂载点');
    }

    createRoot(container).render(
        <StrictMode>
            <QueryClientProvider client={queryClient}>
                <LocaleProvider locale={zh_CN}>
                    <PreferencesProvider>
                        <ThemeProvider>
                            <BrowserRouter>
                                <App />
                            </BrowserRouter>
                        </ThemeProvider>
                    </PreferencesProvider>
                </LocaleProvider>
            </QueryClientProvider>
        </StrictMode>,
    );
}

void bootstrap();
