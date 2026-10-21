import { Button, Space } from '@douyinfe/semi-ui';
import { Bold, Heading2, Italic, List, ListOrdered } from 'lucide-react';
import { useEffect } from 'react';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import './ConfigRichTextEditor.css';

export function ConfigRichTextEditor({ value, disabled, onChange }: { value: string; disabled?: boolean; onChange: (html: string) => void }) {
    const editor = useEditor({
        extensions: [StarterKit],
        content: value,
        editable: !disabled,
        onUpdate: ({ editor: current }) => onChange(current.getHTML()),
    });

    useEffect(() => {
        if (!editor) return;
        editor.setEditable(!disabled);
        if (editor.getHTML() !== value) editor.commands.setContent(value || '', { emitUpdate: false });
    }, [disabled, editor, value]);

    if (!editor) return null;

    return (
        <div style={{ border: '1px solid var(--semi-color-border)', borderRadius: 6, overflow: 'hidden' }}>
            <Space style={{ padding: 6, borderBottom: '1px solid var(--semi-color-border)', background: 'var(--semi-color-fill-0)' }}>
                <Button size="small" theme={editor.isActive('bold') ? 'solid' : 'borderless'} icon={<Bold size={14} />} disabled={disabled} aria-label="粗体" onClick={() => editor.chain().focus().toggleBold().run()} />
                <Button size="small" theme={editor.isActive('italic') ? 'solid' : 'borderless'} icon={<Italic size={14} />} disabled={disabled} aria-label="斜体" onClick={() => editor.chain().focus().toggleItalic().run()} />
                <Button size="small" theme={editor.isActive('heading', { level: 2 }) ? 'solid' : 'borderless'} icon={<Heading2 size={14} />} disabled={disabled} aria-label="二级标题" onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()} />
                <Button size="small" theme={editor.isActive('bulletList') ? 'solid' : 'borderless'} icon={<List size={14} />} disabled={disabled} aria-label="无序列表" onClick={() => editor.chain().focus().toggleBulletList().run()} />
                <Button size="small" theme={editor.isActive('orderedList') ? 'solid' : 'borderless'} icon={<ListOrdered size={14} />} disabled={disabled} aria-label="有序列表" onClick={() => editor.chain().focus().toggleOrderedList().run()} />
            </Space>
            <EditorContent className="config-rich-text" editor={editor} />
        </div>
    );
}
