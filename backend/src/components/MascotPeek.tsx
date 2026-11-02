import { useEffect, useRef } from 'react';
import mascotUrl from '@/assets/mascot-peek.png';
import './MascotPeek.css';

/**
 * 眼白与瞳孔在图片里的位置，均按「占图片宽度的比例」记（量自 1222×820 的原图）：
 * 眼白中心 (690, 372.5)、半径 209.5，瞳孔直径约为眼白直径的 55%。
 */
const EYE = { cx: 0.5646, cy: 0.4543, r: 0.1714, pupil: 0.55 } as const;

/** 跟随的时间常数，越小越跟手 */
const FOLLOW_MS = 70;

/** 光标离眼睛多远算「推满」 */
const REACH_PX = 380;

interface MascotPeekProps {
    className?: string;
}

/**
 * 趴在登录卡片上沿的氪金兽：透明底贴图，黑色瞳孔是手搓的，
 * 跟着鼠标在眼白范围内转。纯装饰，不接收指针事件、不进无障碍树。
 */
export function MascotPeek({ className }: MascotPeekProps) {
    const rootRef = useRef<HTMLDivElement>(null);
    const pupilRef = useRef<HTMLSpanElement>(null);

    useEffect(() => {
        const root = rootRef.current;
        const pupil = pupilRef.current;
        if (!root || !pupil) return undefined;

        // 触屏、或用户要求减少动态效果时，瞳孔保持正中
        const canHover = window.matchMedia('(hover: hover)').matches;
        const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        if (!canHover || reduceMotion) return undefined;

        let curX = 0;
        let curY = 0;
        let wantX = 0;
        let wantY = 0;
        let raf = 0;
        let lastT = 0;
        let pointer: { x: number; y: number } | null = null;
        // 布局信息缓存：指针事件频率可能上千赫兹，不能每个事件都读一次 getBoundingClientRect
        let box: DOMRect | null = null;

        /** 瞳孔圆心最多能偏多远：眼白半径减瞳孔半径，再留一点边 */
        const limit = (width: number) => Math.max(0, width * EYE.r * (1 - EYE.pupil) - width * 0.008);

        const paint = () => {
            pupil.style.transform = `translate(-50%, -50%) translate3d(${curX.toFixed(2)}px, ${curY.toFixed(2)}px, 0)`;
        };

        const frame = (t: number) => {
            const dt = lastT ? Math.min(64, t - lastT) : 16;
            lastT = t;

            // 目标值每帧只算一次：跟的是「这一帧的光标位置」，不是每个事件
            if (pointer && box && box.width) {
                const dx = pointer.x - (box.left + box.width * EYE.cx);
                const dy = pointer.y - (box.top + box.height * EYE.cy);
                const d = Math.hypot(dx, dy) || 1;
                // 越远推得越满，离得近时收窄，看起来才是「盯着你看」
                const reach = limit(box.width) * Math.min(1, d / REACH_PX);
                wantX = (dx / d) * reach;
                wantY = (dy / d) * reach;
            } else {
                wantX = 0;
                wantY = 0;
            }

            const k = 1 - Math.exp(-dt / FOLLOW_MS); // 帧率无关：120Hz 屏同样是按真实时间缓动
            curX += (wantX - curX) * k;
            curY += (wantY - curY) * k;
            if (Math.abs(wantX - curX) < 0.05) curX = wantX;
            if (Math.abs(wantY - curY) < 0.05) curY = wantY;

            paint();

            if (curX === wantX && curY === wantY) {
                raf = 0;
                lastT = 0;
                return;
            }
            raf = requestAnimationFrame(frame);
        };

        const kick = () => {
            if (raf) return;
            if (!box) box = root.getBoundingClientRect();
            raf = requestAnimationFrame(frame);
        };

        const invalidate = () => {
            box = null;
            if (pointer) kick();
        };

        const onPointerMove = (e: PointerEvent) => {
            pointer = { x: e.clientX, y: e.clientY };
            kick();
        };
        const onPointerLeave = () => {
            pointer = null;
            kick();
        };

        window.addEventListener('pointermove', onPointerMove, { passive: true });
        window.addEventListener('blur', onPointerLeave);
        window.addEventListener('resize', invalidate);
        // 右侧是可滚动容器，滚动后缓存的位置就失效了
        window.addEventListener('scroll', invalidate, { passive: true, capture: true });
        document.addEventListener('mouseleave', onPointerLeave);
        paint();

        return () => {
            window.removeEventListener('pointermove', onPointerMove);
            window.removeEventListener('blur', onPointerLeave);
            window.removeEventListener('resize', invalidate);
            window.removeEventListener('scroll', invalidate, { capture: true });
            document.removeEventListener('mouseleave', onPointerLeave);
            cancelAnimationFrame(raf);
        };
    }, []);

    return (
        <div className={['mascot-peek', className].filter(Boolean).join(' ')} ref={rootRef} aria-hidden="true">
            <img className="mascot-peek__body" src={mascotUrl} alt="" draggable={false} />
            <span className="mascot-peek__pupil" ref={pupilRef} />
        </div>
    );
}
