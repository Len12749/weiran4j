/** 运行时标题：由「系统配置」的 system.general.title 覆盖，构建时变量只作兜底 */
let runtimeAppTitle: string | undefined;

export const config = {
    /**
     * 接口前缀；开发环境走 vite 代理，留空即可。
     * 只能是路径前缀（如 `/admin-api`），不能是 `http(s)://` 绝对地址：前后端必须同源部署
     * （认证 Cookie SameSite=Strict、后端无 CORS），`vite.config.ts` 遇到绝对地址会直接报错。
     */
    apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '',
    get appTitle(): string {
        return runtimeAppTitle ?? (import.meta.env.VITE_APP_TITLE || 'Weiran Admin');
    },
};

/**
 * 用系统配置覆盖站点标题，改配置后刷新页面即生效，不必重新构建。
 * 空值忽略，避免配置被清空时标题变成空白。
 */
export function setAppTitle(title?: string | null): void {
    const next = title?.trim();
    if (next) runtimeAppTitle = next;
}

/** 登录后的默认落地页（种子菜单 id=1） */
export const HOME_PATH = '/dashboard';
