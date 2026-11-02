import { Avatar } from '@douyinfe/semi-ui';
import defaultAvatarUrl from '@/assets/avatar-default.png';
import type { UserAvatar } from '@/types/api';

interface Props {
    avatar: UserAvatar | null | undefined;
    fallback: string;
    size: 'extra-small' | 'large';
    className?: string;
}

/** 用户头像：设置过文字头像时渲染文字与背景色，没有设置时用默认头像图。 */
export function TextAvatar({ avatar, fallback, size, className }: Props) {
    if (!avatar) {
        // 默认头像是透明底 PNG，不再画圆形底色，让它直接落在页面背景上
        return (
            <Avatar
                size={size}
                className={className}
                src={defaultAvatarUrl}
                style={{ backgroundColor: 'transparent', border: 'none' }}
            />
        );
    }
    return (
        <Avatar size={size} className={className} style={{ backgroundColor: avatar.background }}>
            {avatar.text || Array.from(fallback)[0] || '?'}
        </Avatar>
    );
}
