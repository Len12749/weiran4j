import { Button, Typography } from '@douyinfe/semi-ui';
import Image from '@tiptap/extension-image';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { Bold, Heading2, ImagePlus, Italic, List, ListOrdered, Quote, Redo2, Underline, Undo2 } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Permission } from '@/components/Permission';
import { usePermission } from '@/hooks/usePermission';
import type { SystemMediaImageUpload } from '@/types/api';
import './SiteConfigRichEditor.css';

interface SiteConfigRichEditorProps {
    label: string;
    value: string;
    onChange: (value: string) => void;
    onUploadImage: (file: File) => Promise<SystemMediaImageUpload>;
    uploading: boolean;
}

export function SiteConfigRichEditor({ label, value, onChange, onUploadImage, uploading }: SiteConfigRichEditorProps) {
    const inputRef = useRef<HTMLInputElement>(null);
    const [pendingUploads, setPendingUploads] = useState(0);
    const { hasPermission } = usePermission();
    const editor = useEditor({
        extensions: [StarterKit, Image.configure({ allowBase64: false })],
        content: value,
        immediatelyRender: false,
        onUpdate: ({ editor: updatedEditor }) => onChange(updatedEditor.getHTML()),
        editorProps: {
            handlePaste: (_view, event) => {
                if (!hasPermission('system:media-library:upload') || pendingUploads > 0) return false;
                const files = Array.from(event.clipboardData?.items ?? [])
                    .filter((item) => item.kind === 'file' && item.type.startsWith('image/'))
                    .map((item) => item.getAsFile())
                    .filter((file): file is File => file !== null);
                if (!files.length) return false;
                event.preventDefault();
                void insertImages(files).catch(() => undefined);
                return true;
            },
            attributes: { 'aria-label': label, class: 'system-rich-editor__content', role: 'textbox' },
        },
    });

    useEffect(() => {
        editor?.setEditable(pendingUploads === 0);
    }, [editor, pendingUploads]);

    async function insertImages(files: File[]) {
        if (!editor) return;
        const insertAt = editor.state.selection.from;
        setPendingUploads((count) => count + files.length);
        try {
            const uploads: SystemMediaImageUpload[] = [];
            for (const file of files) {
                try {
                    uploads.push(await onUploadImage(file));
                } catch {
                    // Continue the batch so successful files can still be inserted in their selected order.
                } finally {
                    setPendingUploads((count) => count - 1);
                }
            }
            if (uploads.length) {
                editor
                    .chain()
                    .focus()
                    .insertContentAt(
                        insertAt,
                        uploads.map((uploaded) => ({ type: 'image', attrs: { src: uploaded.url, alt: uploaded.name } })),
                    )
                    .run();
            }
        } finally {
            setPendingUploads(0);
        }
    }

    return (
        <div className="system-rich-editor">
            <Typography.Text strong className="system-rich-editor__label">
                {label}
            </Typography.Text>
            <div className="system-rich-editor__toolbar" role="toolbar" aria-label={`${label}格式工具`}>
                <Button htmlType="button" icon={<Bold size={16} />} aria-label="加粗" disabled={!editor} onClick={() => editor?.chain().focus().toggleBold().run()} />
                <Button htmlType="button" icon={<Italic size={16} />} aria-label="斜体" disabled={!editor} onClick={() => editor?.chain().focus().toggleItalic().run()} />
                <Button htmlType="button" icon={<Underline size={16} />} aria-label="下划线" disabled={!editor} onClick={() => editor?.chain().focus().toggleUnderline().run()} />
                <Button htmlType="button" icon={<Heading2 size={16} />} aria-label="二级标题" disabled={!editor} onClick={() => editor?.chain().focus().toggleHeading({ level: 2 }).run()} />
                <Button htmlType="button" icon={<List size={16} />} aria-label="无序列表" disabled={!editor} onClick={() => editor?.chain().focus().toggleBulletList().run()} />
                <Button htmlType="button" icon={<ListOrdered size={16} />} aria-label="有序列表" disabled={!editor} onClick={() => editor?.chain().focus().toggleOrderedList().run()} />
                <Button htmlType="button" icon={<Quote size={16} />} aria-label="引用" disabled={!editor} onClick={() => editor?.chain().focus().toggleBlockquote().run()} />
                <Button htmlType="button" icon={<Undo2 size={16} />} aria-label="撤销" disabled={!editor?.can().undo()} onClick={() => editor?.chain().focus().undo().run()} />
                <Button htmlType="button" icon={<Redo2 size={16} />} aria-label="重做" disabled={!editor?.can().redo()} onClick={() => editor?.chain().focus().redo().run()} />
                <Permission code="system:media-library:upload">
                    <Button
                        htmlType="button"
                        icon={<ImagePlus size={16} />}
                        aria-label="上传并插入图片"
                        loading={uploading}
                        disabled={!editor || uploading || pendingUploads > 0}
                        onClick={() => inputRef.current?.click()}
                    />
                    <input
                        ref={inputRef}
                        type="file"
                        accept="image/jpeg,image/png,image/gif,.jpg,.jpeg,.png,.gif"
                        multiple
                        hidden
                        aria-label={`${label}图片文件`}
                        onChange={(event) => {
                            const files = Array.from(event.currentTarget.files ?? []);
                            event.currentTarget.value = '';
                            if (files.length) void insertImages(files).catch(() => undefined);
                        }}
                    />
                </Permission>
            </div>
            <EditorContent editor={editor} />
        </div>
    );
}
