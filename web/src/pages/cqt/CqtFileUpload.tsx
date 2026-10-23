import { Button, ImagePreview, Toast } from '@douyinfe/semi-ui';
import { Download, Eye, FilePlus2, Play, Trash2 } from 'lucide-react';
import { useRef, useState } from 'react';
import { FileTypeIcon, isPdfFile, isVideoFile } from '@/components/FileTypeIcon';
import { isImageUrl } from '@/components/FileUpload';
import { PdfPreview } from '@/components/PdfPreview';
import { VideoPreview } from '@/components/VideoPreview';
import { openFileUrl } from '@/lib/file-url';
import '@/components/FileUpload.css';

type CqtFileUploadProps = {
    value: string;
    disabled?: boolean;
    allowedExtensions: readonly string[];
    maxFileSizeMB?: number;
    onUpload: (file: File) => Promise<string | { url: string }>;
    onChange: (url: string) => void;
    onUploadingChange?: (uploading: boolean) => void;
};

/** 常青藤业务上传：沿用系统上传组件的样式，按业务接口校验文件类型与容量。 */
export function CqtFileUpload({ value, disabled = false, allowedExtensions, maxFileSizeMB = 10, onUpload, onChange, onUploadingChange }: CqtFileUploadProps) {
    const inputRef = useRef<HTMLInputElement>(null);
    const busy = useRef(false);
    const [uploading, setUploading] = useState(false);
    const [imageVisible, setImageVisible] = useState(false);
    const [pdfVisible, setPdfVisible] = useState(false);
    const [videoVisible, setVideoVisible] = useState(false);

    const uploadFile = async (file: File | undefined) => {
        if (!file || disabled || busy.current) return;
        const extension = file.name.slice(file.name.lastIndexOf('.')).toLowerCase();
        if (!allowedExtensions.includes(extension)) {
            Toast.error('不支持该文件类型');
            return;
        }
        if (file.size === 0 || file.size > maxFileSizeMB * 1024 * 1024) {
            Toast.error(`文件须大于 0 且不超过 ${maxFileSizeMB} MB`);
            return;
        }
        busy.current = true;
        setUploading(true);
        onUploadingChange?.(true);
        try {
            const result = await onUpload(file);
            const url = typeof result === 'string' ? result : result.url;
            if (!url) throw new Error('上传未返回文件地址');
            onChange(url);
        } catch {
            Toast.error('文件上传失败，请重试');
        } finally {
            busy.current = false;
            setUploading(false);
            onUploadingChange?.(false);
        }
    };

    const preview = () => {
        if (isImageUrl(value)) setImageVisible(true);
        else if (isPdfFile(value)) setPdfVisible(true);
        else if (isVideoFile(value)) setVideoVisible(true);
        else openFileUrl(value);
    };

    return <div className="file-upload">
        <div className="file-upload__grid">
            {value && <div className="file-upload__item">
                <div className="file-upload__frame">{isImageUrl(value)
                    ? <img src={value} alt="已上传文件" />
                    : <FileTypeIcon name={value} />}</div>
                <div className="file-upload__actions">
                    <Button size="small" icon={isImageUrl(value) || isPdfFile(value) ? <Eye size={14} /> : isVideoFile(value) ? <Play size={14} /> : <Download size={14} />} aria-label="预览或下载文件" onClick={preview} />
                    <Button size="small" type="danger" theme="borderless" icon={<Trash2 size={14} />} aria-label="移除文件" disabled={disabled || uploading} onClick={() => onChange('')} />
                </div>
            </div>}
            <div className="file-upload__dropzone" role="button" tabIndex={disabled || uploading ? -1 : 0} aria-label={value ? '更换文件' : '上传文件'} aria-disabled={disabled || uploading}
                onClick={() => { if (!disabled && !uploading) inputRef.current?.click(); }}
                onKeyDown={(event) => { if ((event.key === 'Enter' || event.key === ' ') && !disabled && !uploading) { event.preventDefault(); inputRef.current?.click(); } }}
                onDragOver={(event) => { event.preventDefault(); event.dataTransfer.dropEffect = 'copy'; }}
                onDrop={(event) => { event.preventDefault(); void uploadFile(event.dataTransfer.files[0]); }}>
                <FilePlus2 size={20} /><span>{uploading ? '上传中…' : value ? '更换文件' : '上传文件'}</span>
            </div>
        </div>
        <input ref={inputRef} hidden type="file" accept={allowedExtensions.join(',')} onChange={(event) => { void uploadFile(event.currentTarget.files?.[0]); event.currentTarget.value = ''; }} />
        {value && <ImagePreview src={[value]} visible={imageVisible} onVisibleChange={setImageVisible} />}
        <PdfPreview url={pdfVisible ? value : null} title="PDF 预览" onClose={() => setPdfVisible(false)} />
        <VideoPreview url={videoVisible ? value : null} title="视频预览" onClose={() => setVideoVisible(false)} />
    </div>;
}
