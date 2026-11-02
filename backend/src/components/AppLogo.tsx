import logoUrl from '@/assets/app-logo.png';
import './AppLogo.css';

interface AppLogoProps {
    size?: number;
    /** solid：浅色界面；glass：主色底上的品牌栏（登录页）。图片自带色彩，两者只差一层柔光 */
    variant?: 'solid' | 'glass';
    className?: string;
}

/** 应用标志：品牌图片（src/assets/app-logo.png，透明底），尺寸由 size 控制 */
export function AppLogo({ size = 28, variant = 'solid', className }: AppLogoProps) {
    const cls = ['app-logo', `app-logo--${variant}`, className].filter(Boolean).join(' ');
    return (
        <span className={cls} aria-hidden="true" style={{ width: size, height: size }}>
            <img className="app-logo__image" src={logoUrl} alt="" draggable={false} />
        </span>
    );
}
