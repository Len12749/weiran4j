import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { TextAvatar } from '../TextAvatar';

describe('TextAvatar', () => {
    it('没有设置头像时用默认头像图', () => {
        const { container } = render(<TextAvatar avatar={null} fallback="刘森" size="extra-small" />);
        const img = container.querySelector('img');
        expect(img).not.toBeNull();
        expect(img?.getAttribute('src')).toContain('avatar-default');
    });

    it('设置过文字头像时渲染文字与背景色', () => {
        const { container } = render(
            <TextAvatar avatar={{ text: '刘', background: '#123456' }} fallback="刘森" size="large" />,
        );
        expect(screen.getByText('刘')).toBeInTheDocument();
        expect(container.querySelector('img')).toBeNull();
    });
});
