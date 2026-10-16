import { Button, Typography } from '@douyinfe/semi-ui';
import Image from '@tiptap/extension-image';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import {
    Bold,
    Heading2,
    ImagePlus,
    Italic,
    List,
    ListOrdered,
    Quote,
    Redo2,
    Underline,
    Undo2,
} from 'lucide-react';
import { useRef } from 'react';
import type { SiteConfigImageUpload } from '@/types/api';
import './SiteConfigRichEditor.css';

interface SiteConfigRichEditorProps {
    label: string;
    value: string;
    onChange: (value: string) => void;
    onUploadImage: (file: File) => Promise<SiteConfigImageUpload>;
    uploading: boolean;
}

export function SiteConfigRichEditor({ label, value, onChange, onUploadImage, uploading }: SiteConfigRichEditorProps) {
    const inputRef = useRef<HTMLInputElement>(null);
    const insertImages = async (files: File[]) => {
        const insertAt = editor?.state.selection.from;
        const uploads = await Promise.all(files.map((file) => onUploadImage(file)));
        if (!editor) return;
        editor
            .chain()
            .focus()
            .insertContentAt(
                insertAt ?? editor.state.selection.from,
                uploads.map((uploaded) => ({ type: 'image', attrs: { src: uploaded.url, alt: uploaded.name } })),
            )
            .run();
    };

    const editor = useEditor({
        extensions: [StarterKit, Image.configure({ allowBase64: false })],
        content: value,
        immediatelyRender: false,
        onUpdate: ({ editor: updatedEditor }) => onChange(updatedEditor.getHTML()),
        editorProps: {
            handlePaste: (_view, event) => {
                const imageFiles = Array.from(event.clipboardData?.items ?? [])
                    .filter((item) => item.kind === 'file' && item.type.startsWith('image/'))
                    .map((item) => item.getAsFile())
                    .filter((file): file is File => file !== null);
                if (imageFiles.length === 0) return false;
                event.preventDefault();
                void insertImages(imageFiles).catch(() => undefined);
                return true;
            },
            attributes: {
                'aria-label': label,
                class: 'site-config-rich-editor__content',
                role: 'textbox',
            },
        },
    });

    return (
        <div className="site-config-rich-editor">
            <Typography.Text strong className="site-config-rich-editor__label">
                {label}
            </Typography.Text>
            <div className="site-config-rich-editor__toolbar" role="toolbar" aria-label={`${label}格式工具`}>
                <Button htmlType="button" icon={<Bold size={16} />} aria-label="加粗" disabled={!editor} onClick={() => editor?.chain().focus().toggleBold().run()} />
                <Button htmlType="button" icon={<Italic size={16} />} aria-label="斜体" disabled={!editor} onClick={() => editor?.chain().focus().toggleItalic().run()} />
                <Button htmlType="button" icon={<Underline size={16} />} aria-label="下划线" disabled={!editor} onClick={() => editor?.chain().focus().toggleUnderline().run()} />
                <Button htmlType="button" icon={<Heading2 size={16} />} aria-label="二级标题" disabled={!editor} onClick={() => editor?.chain().focus().toggleHeading({ level: 2 }).run()} />
                <Button htmlType="button" icon={<List size={16} />} aria-label="无序列表" disabled={!editor} onClick={() => editor?.chain().focus().toggleBulletList().run()} />
                <Button htmlType="button" icon={<ListOrdered size={16} />} aria-label="有序列表" disabled={!editor} onClick={() => editor?.chain().focus().toggleOrderedList().run()} />
                <Button htmlType="button" icon={<Quote size={16} />} aria-label="引用" disabled={!editor} onClick={() => editor?.chain().focus().toggleBlockquote().run()} />
                <Button htmlType="button" icon={<Undo2 size={16} />} aria-label="撤销" disabled={!editor?.can().undo()} onClick={() => editor?.chain().focus().undo().run()} />
                <Button htmlType="button" icon={<Redo2 size={16} />} aria-label="重做" disabled={!editor?.can().redo()} onClick={() => editor?.chain().focus().redo().run()} />
                <Button
                    htmlType="button"
                    icon={<ImagePlus size={16} />}
                    aria-label="上传并插入图片"
                    loading={uploading}
                    disabled={!editor || uploading}
                    onClick={() => inputRef.current?.click()}
                />
                <input
                    ref={inputRef}
                    type="file"
                    accept="image/jpeg,image/png,image/gif,.jpg,.jpeg,.png,.gif"
                    hidden
                    aria-label={`${label}图片文件`}
                    onChange={(event) => {
                        const file = event.currentTarget.files?.[0];
                        event.currentTarget.value = '';
                        if (file) void insertImages([file]).catch(() => undefined);
                    }}
                />
            </div>
            <EditorContent editor={editor} />
        </div>
    );
}
