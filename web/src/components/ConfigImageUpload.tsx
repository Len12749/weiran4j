import { Button, Space, Toast } from '@douyinfe/semi-ui';
import { ImagePlus, Trash2 } from 'lucide-react';
import { useRef, useState } from 'react';
import { useUploadSystemImage } from '@/hooks/queries/system-media';

const IMAGE_EXTENSIONS = new Set(['.jpg', '.jpeg', '.png', '.gif']);

export function ConfigImageUpload({ value, disabled, onChange }: { value: string; disabled?: boolean; onChange: (url: string) => void }) {
    const inputRef = useRef<HTMLInputElement>(null);
    const upload = useUploadSystemImage();
    const [imageUrl, setImageUrl] = useState(value);

    const chooseImage = async (file: File | undefined) => {
        if (!file) return;
        const extension = file.name.slice(file.name.lastIndexOf('.')).toLowerCase();
        if (!IMAGE_EXTENSIONS.has(extension) || file.size === 0 || file.size > 10 * 1024 * 1024) {
            Toast.error('请选择不超过 10 MB 的 jpg、jpeg、png 或 gif 图片');
            return;
        }
        try {
            const result = await upload.mutateAsync(file);
            setImageUrl(result.url);
            onChange(result.url);
        } catch {
            // 请求层负责显示上传错误。
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
                <Button icon={<ImagePlus size={14} />} disabled={disabled || upload.isPending} loading={upload.isPending} onClick={() => inputRef.current?.click()}>
                    {imageUrl ? '更换图片' : '上传图片'}
                </Button>
                {imageUrl && <Button icon={<Trash2 size={14} />} disabled={disabled} onClick={() => { setImageUrl(''); onChange(''); }}>移除</Button>}
            </Space>
        </Space>
    );
}
