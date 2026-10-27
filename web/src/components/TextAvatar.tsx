import { Avatar } from '@douyinfe/semi-ui';
import type { UserAvatar } from '@/types/api';

interface Props {
    avatar: UserAvatar | null | undefined;
    fallback: string;
    size: 'extra-small' | 'large';
    className?: string;
}

/** 用户头像只渲染文字和背景色，不读取图片地址。 */
export function TextAvatar({ avatar, fallback, size, className }: Props) {
    return (
        <Avatar size={size} className={className} style={{ backgroundColor: avatar?.background ?? '#0064FA' }}>
            {avatar?.text || Array.from(fallback)[0] || '?'}
        </Avatar>
    );
}
