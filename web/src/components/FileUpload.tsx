import { Button, ImagePreview, Toast } from '@douyinfe/semi-ui';
import { ArrowLeft, ArrowRight, Eye, FileIcon, FilePlus2, RotateCcw, Trash2 } from 'lucide-react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useUploadSystemFile } from '@/hooks/queries/system-media';
import './FileUpload.css';

const IMAGE_EXTENSIONS = new Set(['.jpg', '.jpeg', '.png', '.gif', '.webp']);
const FILE_EXTENSIONS = new Set([...IMAGE_EXTENSIONS, '.pdf', '.txt', '.csv', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx', '.zip']);
export const MAX_FILE_SIZE_MB = 10;
export const DEFAULT_MAX_FILES = 9;

export function isImageUrl(url: string): boolean {
    return IMAGE_EXTENSIONS.has(`.${url.split(/[?#]/)[0]?.split('.').pop()?.toLowerCase() ?? ''}`);
}

export function openFileUrl(url: string): void {
    try {
        const target = new URL(url, window.location.origin);
        if (!['http:', 'https:'].includes(target.protocol)) throw new Error('invalid protocol');
        window.open(target.href, '_blank', 'noopener,noreferrer');
    } catch {
        Toast.error('文件地址无效');
    }
}

export function fileUploadError(file: File): string | null {
    const extension = file.name.slice(file.name.lastIndexOf('.')).toLowerCase();
    if (!FILE_EXTENSIONS.has(extension)) return '不支持该文件类型';
    if (file.size === 0 || file.size > MAX_FILE_SIZE_MB * 1024 * 1024) return `文件须大于 0 且不超过 ${MAX_FILE_SIZE_MB} MB`;
    return null;
}

export function imageFileError(file: File): string | null {
    const extension = file.name.slice(file.name.lastIndexOf('.')).toLowerCase();
    if (!IMAGE_EXTENSIONS.has(extension) || file.size === 0 || file.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
        return '请选择不超过 10 MB 的 jpg、jpeg、png、gif 或 webp 图片';
    }
    return null;
}

type UploadFile = (file: File) => Promise<string | { url: string }>;

type FileUploadProps = {
    disabled?: boolean;
    onUpload?: UploadFile;
    onUploadingChange?: (uploading: boolean) => void;
    maxFiles?: number;
} & (
    | { multiple?: false; value: string; onChange: (value: string) => void }
    | { multiple: true; value: string[]; onChange: (value: string[]) => void }
);

/** 文件字段统一入口。默认上传到 system 资源库；业务可注入上传动作。 */
export function FileUpload(props: FileUploadProps) {
    const { disabled = false, onUpload, onUploadingChange, maxFiles = DEFAULT_MAX_FILES } = props;
    const upload = useUploadSystemFile();
    const inputRef = useRef<HTMLInputElement>(null);
    const busy = useRef(false);
    const dragIndexRef = useRef<number | null>(null);
    const mounted = useRef(true);
    const [uploading, setUploading] = useState(false);
    const [failed, setFailed] = useState<File[]>([]);
    const [previewIndex, setPreviewIndex] = useState<number | null>(null);
    const [dragIndex, setDragIndex] = useState<number | null>(null);
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

    const moveFile = (from: number, to: number) => {
        if (!props.multiple || disabled || uploading || from === to || to < 0 || to >= urlsRef.current.length) return;
        const next = [...urlsRef.current];
        const [moved] = next.splice(from, 1);
        if (!moved) return;
        next.splice(to, 0, moved);
        updateUrls(next);
    };

    const uploadFiles = async (files: File[]) => {
        if (disabled || busy.current || files.length === 0) return;
        busy.current = true;
        setUploading(true);
        onUploadingChange?.(true);
        const failures: File[] = [];
        for (const file of props.multiple ? files : files.slice(0, 1)) {
            if (props.multiple && urlsRef.current.length >= maxFiles) {
                Toast.warning(`最多上传 ${maxFiles} 个文件`);
                break;
            }
            const error = fileUploadError(file);
            if (error) {
                Toast.error(error);
                continue;
            }
            try {
                const result = onUpload ? await onUpload(file) : await upload.mutateAsync(file);
                const url = typeof result === 'string' ? result : result.url;
                if (!url) throw new Error('上传未返回文件地址');
                if (mounted.current) updateUrls(props.multiple ? [...urlsRef.current, url] : [url]);
            } catch {
                if (onUpload) Toast.error('文件上传失败，请重试');
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
        <div className="file-upload">
            <div className="file-upload__grid">
                {urls.map((url, index) => (
                    <div
                        className={`file-upload__item${dragIndex === index ? ' file-upload__item--dragging' : ''}`}
                        key={`${url}:${index}`}
                        draggable={props.multiple && !disabled && !uploading}
                        onDragStart={(event) => {
                            dragIndexRef.current = index;
                            setDragIndex(index);
                            event.dataTransfer.effectAllowed = 'move';
                            event.dataTransfer.setData('text/plain', String(index));
                        }}
                        onDragOver={(event) => {
                            event.preventDefault();
                            event.dataTransfer.dropEffect = dragIndexRef.current !== null ? 'move' : 'copy';
                        }}
                        onDrop={(event) => {
                            event.preventDefault();
                            if (dragIndexRef.current !== null) moveFile(dragIndexRef.current, index);
                            else void uploadFiles(Array.from(event.dataTransfer.files));
                            dragIndexRef.current = null;
                            setDragIndex(null);
                        }}
                        onDragEnd={() => { dragIndexRef.current = null; setDragIndex(null); }}
                        title={props.multiple ? '拖动文件可调整顺序' : undefined}
                    >
                        <div className="file-upload__frame">{isImageUrl(url)
                            ? <img src={url} alt={`已上传图片 ${index + 1}`} />
                            : <FileIcon size={30} aria-label={`文件 ${index + 1}`} />}</div>
                        <div className="file-upload__actions">
                            {props.multiple && <Button size="small" theme="borderless" icon={<ArrowLeft size={14} />} aria-label={`前移文件 ${index + 1}`} disabled={disabled || uploading || index === 0} onClick={() => moveFile(index, index - 1)} />}
                            <Button size="small" icon={<Eye size={14} />} aria-label={`${isImageUrl(url) ? '预览图片' : '浏览或下载文件'} ${index + 1}`} onClick={() => isImageUrl(url) ? setPreviewIndex(index) : openFileUrl(url)} />
                            <Button size="small" icon={<Trash2 size={14} />} aria-label={`移除文件 ${index + 1}`} disabled={disabled || uploading} onClick={() => updateUrls(urlsRef.current.filter((_, item) => item !== index))} />
                            {props.multiple && <Button size="small" theme="borderless" icon={<ArrowRight size={14} />} aria-label={`后移文件 ${index + 1}`} disabled={disabled || uploading || index === urls.length - 1} onClick={() => moveFile(index, index + 1)} />}
                        </div>
                    </div>
                ))}
                {(props.multiple ? urls.length < maxFiles : true) && <div
                    className="file-upload__dropzone"
                    role="button"
                    tabIndex={disabled || uploading ? -1 : 0}
                    aria-label={props.multiple ? '上传多个文件' : '上传文件'}
                    aria-disabled={disabled || uploading}
                    onClick={() => { if (!disabled && !uploading) inputRef.current?.click(); }}
                    onKeyDown={(event) => { if ((event.key === 'Enter' || event.key === ' ') && !disabled && !uploading) { event.preventDefault(); inputRef.current?.click(); } }}
                    onDragOver={(event) => { event.preventDefault(); event.dataTransfer.dropEffect = dragIndexRef.current !== null ? 'none' : 'copy'; }}
                    onDrop={(event) => { event.preventDefault(); if (dragIndexRef.current === null) void uploadFiles(Array.from(event.dataTransfer.files)); }}
                >
                    <FilePlus2 size={20} />
                    <span>{uploading ? '上传中…' : urls.length && !props.multiple ? '更换文件' : '上传文件'}</span>
                </div>}
            </div>
            {props.multiple && <small>每个文件不超过 {MAX_FILE_SIZE_MB} MB，最多 {maxFiles} 个（当前 {urls.length}/{maxFiles}）</small>}
            <input ref={inputRef} hidden type="file" accept={[...FILE_EXTENSIONS].join(',')} multiple={props.multiple} onChange={(event) => { void uploadFiles(Array.from(event.currentTarget.files ?? [])); event.currentTarget.value = ''; }} />
            {failed.length > 0 && (
                <Button size="small" icon={<RotateCcw size={14} />} disabled={disabled || uploading} onClick={() => void uploadFiles(failed)}>
                    重试失败的文件（{failed.length}）
                </Button>
            )}
            {previewIndex !== null && urls[previewIndex] && <ImagePreview src={urls.filter(isImageUrl)} visible currentIndex={urls.filter(isImageUrl).indexOf(urls[previewIndex])} initialZoom={1} onChange={(index) => setPreviewIndex(urls.indexOf(urls.filter(isImageUrl)[index] ?? ''))} onVisibleChange={(visible) => { if (!visible) setPreviewIndex(null); }} />}
        </div>
    );
}
