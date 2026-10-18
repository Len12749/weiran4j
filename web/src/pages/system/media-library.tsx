import { Button, Card, Input, Modal, Pagination, Spin, Toast, Typography } from '@douyinfe/semi-ui';
import { Copy, ImagePlus, RefreshCw, RotateCcw, Search as SearchIcon, Trash2 } from 'lucide-react';
import { useRef, useState } from 'react';
import { PageContainer } from '@/components/PageContainer';
import { Permission } from '@/components/Permission';
import { SearchToolbar } from '@/components/SearchToolbar';
import { useDeleteSystemMediaImage, useSystemMediaImages, useUploadSystemImage } from '@/hooks/queries/system-media';
import { useTableDefaults } from '@/hooks/useTableDefaults';
import { ApiError } from '@/utils/request';
import type { SystemMediaImageView } from '@/types/api';
import './media-library.css';

interface UploadItem {
    key: string;
    file: File;
    state: 'uploading' | 'success' | 'error';
    error: string | undefined;
}

const MAX_IMAGE_SIZE = 10 * 1024 * 1024;
const IMAGE_EXTENSIONS = new Set(['.jpg', '.jpeg', '.png', '.gif']);

function validateImage(file: File): string | null {
    const extension = file.name.slice(file.name.lastIndexOf('.')).toLowerCase();
    if (!IMAGE_EXTENSIONS.has(extension)) return '只支持 jpg、jpeg、png、gif 图片';
    if (file.size > MAX_IMAGE_SIZE) return '图片不能超过 10 MB';
    if (file.size === 0) return '图片文件为空';
    return null;
}

function isReferenceConflict(error: unknown): error is ApiError {
    return error instanceof ApiError && (error.status === 409 || (error.code >= 40900 && error.code < 41000));
}

export default function SystemMediaLibraryPage() {
    const inputRef = useRef<HTMLInputElement>(null);
    const { pageSize: initialPageSize, pageSizeOpts } = useTableDefaults();
    const [draft, setDraft] = useState('');
    const [keyword, setKeyword] = useState('');
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(initialPageSize);
    const [uploads, setUploads] = useState<UploadItem[]>([]);
    const [preview, setPreview] = useState<SystemMediaImageView | null>(null);
    const [deleting, setDeleting] = useState<SystemMediaImageView | null>(null);
    const query = useSystemMediaImages({ page, pageSize, ...(keyword ? { keyword } : {}) });
    const upload = useUploadSystemImage();
    const remove = useDeleteSystemMediaImage();

    const setItem = (key: string, update: Partial<UploadItem>) => {
        setUploads((current) => current.map((item) => (item.key === key ? { ...item, ...update } : item)));
    };

    const uploadOne = async (item: UploadItem) => {
        setItem(item.key, { state: 'uploading', error: undefined });
        const validationError = validateImage(item.file);
        if (validationError) {
            setItem(item.key, { state: 'error', error: validationError });
            return;
        }
        try {
            await upload.mutateAsync(item.file);
            setItem(item.key, { state: 'success', error: undefined });
        } catch (error) {
            setItem(item.key, { state: 'error', error: error instanceof Error ? error.message : '上传失败' });
        }
    };

    const chooseFiles = (files: File[]) => {
        const selected = files.map((file, index) => ({
            key: `${Date.now()}-${index}-${file.name}-${file.lastModified}`,
            file,
            state: 'uploading' as const,
            error: undefined,
        }));
        setUploads((current) => [...selected, ...current]);
        void (async () => {
            for (const item of selected) await uploadOne(item);
        })();
    };

    const search = () => {
        setKeyword(draft.trim());
        setPage(1);
    };

    const resetSearch = () => {
        setDraft('');
        setKeyword('');
        setPage(1);
    };

    const copyUrl = async (url: string) => {
        try {
            await navigator.clipboard.writeText(url);
            Toast.success('图片链接已复制');
        } catch {
            Toast.error('复制失败，请检查浏览器剪贴板权限');
        }
    };

    const confirmDelete = async () => {
        if (!deleting) return;
        try {
            await remove.mutateAsync(deleting.id);
            Toast.success('图片已删除');
            setDeleting(null);
        } catch (error) {
            if (isReferenceConflict(error)) {
                Toast.error('图片仍被站点配置引用，无法删除');
            } else if (error instanceof Error) {
                Toast.error(error.message);
            } else {
                Toast.error('删除失败');
            }
        }
    };

    return (
        <PageContainer title="图片库">
            <SearchToolbar
                onSearch={search}
                onReset={resetSearch}
                onRefresh={() => void query.refetch()}
                refreshing={query.isFetching}
                conditions={keyword ? [{ key: 'keyword', label: '文件名', value: keyword, onRemove: resetSearch }] : []}
                actions={
                    <Permission code="system:media-library:upload">
                        <Button type="primary" theme="solid" icon={<ImagePlus size={16} />} onClick={() => inputRef.current?.click()}>
                            批量上传
                        </Button>
                    </Permission>
                }
            >
                <Input
                    prefix={<SearchIcon size={14} />}
                    placeholder="按图片名称搜索"
                    value={draft}
                    onChange={setDraft}
                    onEnterPress={search}
                    showClear
                    style={{ width: 240 }}
                />
            </SearchToolbar>
            <input
                ref={inputRef}
                type="file"
                accept="image/jpeg,image/png,image/gif,.jpg,.jpeg,.png,.gif"
                multiple
                hidden
                aria-label="选择图片"
                onChange={(event) => {
                    const files = Array.from(event.currentTarget.files ?? []);
                    event.currentTarget.value = '';
                    if (files.length) chooseFiles(files);
                }}
            />

            {uploads.length > 0 && (
                <section className="media-library__uploads" aria-label="上传结果">
                    <div className="media-library__uploads-title">本次上传</div>
                    {uploads.map((item) => (
                        <div className="media-library__upload-row" key={item.key}>
                            <span className="media-library__upload-name" title={item.file.name}>{item.file.name}</span>
                            {item.state === 'uploading' ? (
                                <Typography.Text type="tertiary">上传中</Typography.Text>
                            ) : item.state === 'success' ? (
                                <Typography.Text type="success">已上传</Typography.Text>
                            ) : (
                                <>
                                    <Typography.Text type="danger">{item.error}</Typography.Text>
                                    <Button size="small" icon={<RefreshCw size={14} />} onClick={() => void uploadOne(item)}>重试</Button>
                                </>
                            )}
                        </div>
                    ))}
                </section>
            )}

            {query.isFetching && !query.data ? (
                <div className="media-library__loading"><Spin /></div>
            ) : query.isError && !query.data ? (
                <div className="media-library__error">
                    <Typography.Text type="danger">图片列表加载失败</Typography.Text>
                    <Button icon={<RotateCcw size={14} />} onClick={() => void query.refetch()}>重试</Button>
                </div>
            ) : (
                <>
                    {query.data?.list.length ? (
                        <div className="media-library__grid">
                            {query.data.list.map((image) => (
                                <Card key={image.id} className="media-library__card" bodyStyle={{ padding: 12 }}>
                                    <button className="media-library__preview-button" type="button" onClick={() => setPreview(image)} aria-label={`预览 ${image.originalName}`}>
                                        <img src={image.url} alt={image.originalName} loading="lazy" />
                                    </button>
                                    <Typography.Text ellipsis={{ showTooltip: true }} className="media-library__name">{image.originalName}</Typography.Text>
                                    <Typography.Text type="tertiary" size="small">{formatBytes(image.size)} · {image.createdAt}</Typography.Text>
                                    <div className="media-library__actions">
                                        <Button size="small" icon={<Copy size={14} />} onClick={() => void copyUrl(image.url)}>复制链接</Button>
                                        <Permission code="system:media-library:delete">
                                            <Button size="small" type="danger" theme="borderless" icon={<Trash2 size={14} />} aria-label={`删除 ${image.originalName}`} onClick={() => setDeleting(image)} />
                                        </Permission>
                                    </div>
                                </Card>
                            ))}
                        </div>
                    ) : (
                        <div className="media-library__empty">{keyword ? '没有找到匹配的图片' : '还没有图片，上传后会显示在这里'}</div>
                    )}
                    <div className="media-library__pagination">
                        <Pagination
                            currentPage={page}
                            pageSize={pageSize}
                            total={query.data?.total ?? 0}
                            pageSizeOpts={pageSizeOpts}
                            onPageChange={setPage}
                            onPageSizeChange={(size) => {
                                setPageSize(size);
                                setPage(1);
                            }}
                        />
                    </div>
                </>
            )}

            <Modal visible={preview !== null} title={preview?.originalName ?? '图片预览'} footer={null} onCancel={() => setPreview(null)}>
                {preview && <img className="media-library__preview-image" src={preview.url} alt={preview.originalName} />}
            </Modal>
            <Modal
                visible={deleting !== null}
                title="删除图片"
                onCancel={() => setDeleting(null)}
                onOk={() => confirmDelete()}
                okText="删除"
                okButtonProps={{ type: 'danger', loading: remove.isPending }}
                cancelText="取消"
            >
                <Typography.Paragraph>
                    确定删除「{deleting?.originalName}」？仍被站点配置引用的图片无法删除。
                </Typography.Paragraph>
            </Modal>
        </PageContainer>
    );
}

function formatBytes(size: number): string {
    if (size < 1024) return `${size} B`;
    if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}
