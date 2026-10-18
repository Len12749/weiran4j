import { Button, Image, ImagePreview, Spin, Typography, Upload } from '@douyinfe/semi-ui';
import { Eye, X } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import './SingleImageUpload.css';

interface SingleImageUploadProps {
    value: string;
    disabled?: boolean;
    onChange: (value: string) => void;
    upload: (file: File) => Promise<{ url: string }>;
}

/** A controlled single image field. Preview and removal are outside the upload trigger. */
export function SingleImageUpload({ value, onChange, upload, disabled = false }: SingleImageUploadProps) {
    const [uploading, setUploading] = useState(false);
    const [error, setError] = useState('');
    const [previewVisible, setPreviewVisible] = useState(false);
    const busy = useRef(false);
    const mounted = useRef(true);

    useEffect(() => {
        mounted.current = true;
        return () => { mounted.current = false; };
    }, []);

    return (
        <div className="single-image-upload">
            {value ? (
                <div className="single-image-upload__picture">
                    <Image src={value} alt="已上传图片" width={96} height={96} preview={false} imgStyle={{ objectFit: 'cover' }} />
                    <Button
                        className="single-image-upload__preview"
                        theme="solid"
                        size="small"
                        icon={<Eye size={14} />}
                        disabled={disabled || uploading}
                        aria-label="预览图片"
                        onClick={() => setPreviewVisible(true)}
                    />
                    <Button
                        className="single-image-upload__remove"
                        theme="solid"
                        type="danger"
                        size="small"
                        icon={<X size={14} />}
                        disabled={disabled || uploading}
                        aria-label="删除图片"
                        onClick={() => { if (disabled || busy.current) return; setPreviewVisible(false); setError(''); onChange(''); }}
                    />
                </div>
            ) : (
                <Upload
                    action=""
                    className="single-image-upload__uploader"
                    accept="image/jpeg,image/png,image/gif,.jpg,.jpeg,.png,.gif"
                    draggable
                    multiple={false}
                    limit={1}
                    fileList={[]}
                    showUploadList={false}
                    disabled={disabled || uploading}
                    onAcceptInvalid={() => setError('请选择 JPG、PNG 或 GIF 图片')}
                    onExceed={() => setError('每次只能上传一张图片')}
                    customRequest={async ({ fileInstance, onSuccess, onError }) => {
                        if (disabled || busy.current) { onError({ status: 409 }); return; }
                        busy.current = true;
                        setUploading(true);
                        setError('');
                        try {
                            const result = await upload(fileInstance);
                            if (!result.url) throw new Error('上传未返回图片地址');
                            if (mounted.current) { onSuccess(result); onChange(result.url); }
                        } catch {
                            if (mounted.current) { onError({ status: 500 }); setError('上传失败，请重试'); }
                        } finally {
                            busy.current = false;
                            if (mounted.current) setUploading(false);
                        }
                    }}
                >
                    <div className="single-image-upload__trigger" aria-label="上传图片" aria-busy={uploading}>
                        {uploading ? <Spin size="small" /> : <Typography.Text type="tertiary">上传</Typography.Text>}
                    </div>
                </Upload>
            )}
            {error && <Typography.Text type="danger" role="alert">{error}</Typography.Text>}
            {value && previewVisible && (
                <ImagePreview
                    key={value}
                    src={[value]}
                    visible
                    initialZoom={1}
                    onVisibleChange={setPreviewVisible}
                />
            )}
        </div>
    );
}
