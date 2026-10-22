import { Button, ImagePreview, Space, Toast, Typography } from '@douyinfe/semi-ui';
import { Eye, ImagePlus, RotateCcw, Trash2 } from 'lucide-react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useUploadSystemImage } from '@/hooks/queries/system-media';
import './ImageUpload.css';

const IMAGE_EXTENSIONS = new Set(['.jpg', '.jpeg', '.png', '.gif']);

export function imageFileError(file: File): string | null {
    const extension = file.name.slice(file.name.lastIndexOf('.')).toLowerCase();
    if (!IMAGE_EXTENSIONS.has(extension) || file.size === 0 || file.size > 10 * 1024 * 1024) {
        return '请选择不超过 10 MB 的 jpg、jpeg、png 或 gif 图片';
    }
    return null;
}

type UploadImage = (file: File) => Promise<string | { url: string }>;

type ImageUploadProps = {
    disabled?: boolean;
    onUpload?: UploadImage;
    onUploadingChange?: (uploading: boolean) => void;
} & (
    | { multiple?: false; value: string; onChange: (value: string) => void }
    | { multiple: true; value: string[]; onChange: (value: string[]) => void }
);

/** 图片字段统一入口。默认上传到 system 媒体库；业务可注入上传动作。 */
export function ImageUpload(props: ImageUploadProps) {
    const { disabled = false, onUpload, onUploadingChange } = props;
    const upload = useUploadSystemImage();
    const inputRef = useRef<HTMLInputElement>(null);
    const busy = useRef(false);
    const mounted = useRef(true);
    const [uploading, setUploading] = useState(false);
    const [failed, setFailed] = useState<File[]>([]);
    const [previewIndex, setPreviewIndex] = useState<number | null>(null);
    const urls = useMemo(() => props.multiple ? props.value : props.value ? [props.value] : [], [props.multiple, props.value]);
    const urlsRef = useRef(urls);

    useEffect(() => { urlsRef.current = urls; }, [urls]);

    useEffect(() => {
        mounted.current = true;
        return () => { mounted.current = false; };
    }, []);

    const updateUrls = (next: string[]) => {
        urlsRef.current = next;
        if (props.multiple) props.onChange(next);
        else props.onChange(next[0] ?? '');
    };

    const uploadFiles = async (files: File[]) => {
        if (disabled || busy.current || files.length === 0) return;
        busy.current = true;
        setUploading(true);
        onUploadingChange?.(true);
        const failures: File[] = [];
        for (const file of props.multiple ? files : files.slice(0, 1)) {
            const error = imageFileError(file);
            if (error) {
                Toast.error(error);
                continue;
            }
            try {
                const result = onUpload ? await onUpload(file) : await upload.mutateAsync(file);
                const url = typeof result === 'string' ? result : result.url;
                if (!url) throw new Error('上传未返回图片地址');
                if (mounted.current) updateUrls(props.multiple ? [...urlsRef.current, url] : [url]);
            } catch {
                if (onUpload) Toast.error('图片上传失败，请重试');
                failures.push(file);
            }
        }
        busy.current = false;
        if (mounted.current) {
            setFailed(failures);
            setUploading(false);
            onUploadingChange?.(false);
        }
    };

    return (
        <div className="image-upload">
            <div className="image-upload__list">
                {urls.map((url, index) => (
                    <div className="image-upload__item" key={`${url}:${index}`}>
                        <img src={url} alt={`已上传图片 ${index + 1}`} />
                        <Space spacing={4}>
                            <Button size="small" icon={<Eye size={14} />} aria-label={`预览图片 ${index + 1}`} onClick={() => setPreviewIndex(index)} />
                            <Button size="small" icon={<Trash2 size={14} />} aria-label={`移除图片 ${index + 1}`} disabled={disabled || uploading} onClick={() => updateUrls(urlsRef.current.filter((_, item) => item !== index))} />
                        </Space>
                    </div>
                ))}
            </div>
            <div
                className="image-upload__dropzone"
                role="button"
                tabIndex={disabled || uploading ? -1 : 0}
                aria-label={props.multiple ? '上传多张图片' : '上传图片'}
                aria-disabled={disabled || uploading}
                onClick={() => { if (!disabled && !uploading) inputRef.current?.click(); }}
                onKeyDown={(event) => { if ((event.key === 'Enter' || event.key === ' ') && !disabled && !uploading) { event.preventDefault(); inputRef.current?.click(); } }}
                onDragOver={(event) => event.preventDefault()}
                onDrop={(event) => { event.preventDefault(); void uploadFiles(Array.from(event.dataTransfer.files)); }}
            >
                <ImagePlus size={20} />
                <Typography.Text>{uploading ? '上传中…' : props.multiple ? '点击或拖放图片，可选多张' : urls.length ? '点击或拖放图片以更换' : '点击或拖放图片'}</Typography.Text>
            </div>
            <input ref={inputRef} hidden type="file" accept=".jpg,.jpeg,.png,.gif" multiple={props.multiple} onChange={(event) => { void uploadFiles(Array.from(event.currentTarget.files ?? [])); event.currentTarget.value = ''; }} />
            {failed.length > 0 && (
                <Button size="small" icon={<RotateCcw size={14} />} disabled={disabled || uploading} onClick={() => void uploadFiles(failed)}>
                    重试失败的图片（{failed.length}）
                </Button>
            )}
            {previewIndex !== null && urls[previewIndex] && <ImagePreview src={urls} visible currentIndex={previewIndex} onVisibleChange={(visible) => { if (!visible) setPreviewIndex(null); }} />}
        </div>
    );
}
