import { Button, Space, Toast } from '@douyinfe/semi-ui';
import { ImagePlus, Trash2 } from 'lucide-react';
import { useRef, useState } from 'react';
import { useUploadSystemImage } from '@/hooks/queries/system-media';

const IMAGE_EXTENSIONS = new Set(['.jpg', '.jpeg', '.png', '.gif']);

export function ConfigImageUpload({
    value,
    disabled,
    onChange,
    onUpload,
}: {
    value: string;
    disabled?: boolean;
    onChange: (url: string) => void;
    onUpload?: (file: File) => Promise<string>;
}) {
    const inputRef = useRef<HTMLInputElement>(null);
    const upload = useUploadSystemImage();
    const [imageUrl, setImageUrl] = useState(value);
    const [isUploading, setIsUploading] = useState(false);

    const chooseImage = async (file: File | undefined) => {
        if (!file) return;
        const extension = file.name.slice(file.name.lastIndexOf('.')).toLowerCase();
        if (!IMAGE_EXTENSIONS.has(extension) || file.size === 0 || file.size > 10 * 1024 * 1024) {
            Toast.error('请选择不超过 10 MB 的 jpg、jpeg、png 或 gif 图片');
            return;
        }
        setIsUploading(true);
        try {
            const url = onUpload ? await onUpload(file) : (await upload.mutateAsync(file)).url;
            setImageUrl(url);
            onChange(url);
        } catch {
            if (onUpload) Toast.error('图片读取失败');
            // 默认上传的请求层负责显示错误。
        } finally {
            setIsUploading(false);
        }
    };

    return (
        <Space vertical align="start" spacing={8}>
            {imageUrl ? <img src={imageUrl} alt="配置图片预览" style={{ display: 'block', maxWidth: 240, maxHeight: 160, objectFit: 'contain' }} /> : null}
            <Space>
                <input
                    ref={inputRef}
                    type="file"
                    accept=".jpg,.jpeg,.png,.gif"
                    hidden
                    onChange={(event) => {
                        void chooseImage(event.currentTarget.files?.[0]);
                        event.currentTarget.value = '';
                    }}
                />
                <Button icon={<ImagePlus size={14} />} disabled={disabled || isUploading || upload.isPending} loading={isUploading || upload.isPending} onClick={() => inputRef.current?.click()}>
                    {imageUrl ? '更换图片' : '上传图片'}
                </Button>
                {imageUrl && <Button icon={<Trash2 size={14} />} disabled={disabled} onClick={() => { setImageUrl(''); onChange(''); }}>移除</Button>}
            </Space>
        </Space>
    );
}
