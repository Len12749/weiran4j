/** Public content requests reject both HTTP and portal errors. */
export function contentGet(vm, resource, query = {}) {
    const search = Object.entries(query).filter(([, value]) => value !== undefined).map(([key, value]) => `${key}=${encodeURIComponent(value)}`).join('&');
    return new Promise((resolve, reject) => uni.request({
        url: `${vm.config?.baseURL || ''}/api-web/n/content/${resource}${search ? '?' + search : ''}`,
        method: 'GET',
        success: ({ statusCode, data }) => {
            if (statusCode >= 200 && statusCode < 300 && data?.code === 200 && data.data != null) {
                if (!resource.includes('/') && (!Array.isArray(data.data.list) || !Number.isFinite(data.data.total))) reject(new Error('内容列表格式异常'));
                else resolve(data.data);
            }
            else reject(new Error(data?.message || '内容不存在或暂时无法加载'));
        },
        fail: () => reject(new Error('网络异常，请重试')),
    }));
}

export async function contentAll(vm, resource, query = {}) {
    const rows = [];
    for (let page = 1; ; page++) {
        const result = await contentGet(vm, resource, { ...query, page, pageSize: 100 });
        if (!Array.isArray(result.list)) throw new Error('内容列表格式异常');
        rows.push(...result.list);
        if (rows.length >= result.total || !result.list.length) return rows;
    }
}

export function contentUrl(value) {
    const url = String(value || '').trim();
    return /^(https?:\/\/|\/(?!\/))/i.test(url) ? url : '';
}

export function expertView(item) {
    return { ...item, title: item.name, image: contentUrl(item.image), description: (item.content || '').replace(/<[^>]*>/g, ' ').replace(/&nbsp;/g, ' ').trim() };
}

export function downloadContent(url, name) {
    const address = contentUrl(url);
    if (!address) { uni.showToast({ title: '文件地址无效', icon: 'none' }); return; }
    // #ifdef H5
    const link = document.createElement('a');
    link.href = address;
    link.download = name || 'download';
    link.target = '_blank';
    link.rel = 'noopener';
    document.body.appendChild(link);
    link.click();
    link.remove();
    // #endif
    // #ifndef H5
    uni.downloadFile({ url: address, success: (res) => {
        if (res.statusCode === 200) uni.openDocument({ filePath: res.tempFilePath, showMenu: true, fail: () => uni.showToast({ title: '无法打开文件', icon: 'none' }) });
        else uni.showToast({ title: '下载失败', icon: 'none' });
    }, fail: () => uni.showToast({ title: '下载失败，请重试', icon: 'none' }) });
    // #endif
}
