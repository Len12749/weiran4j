/** Local request boundary: every transport and portal error settles the promise. */
export function schoolRequest(vm, { url, method = 'GET', data = {} }) {
    const token = uni.getStorageSync('loginInfo')?.access_token;
    if (!token) return Promise.reject(new Error('请先登录'));
    return new Promise((resolve, reject) => uni.request({
        url: `${vm.config?.baseURL || ''}${url.replace(/^\/api\//, '/api-web/')}`,
        method, data, timeout: 30000, header: { Authorization: `Bearer ${token}` },
        success(response) {
            const body = response.data;
            if (response.statusCode >= 200 && response.statusCode < 300 && body?.code === 200) resolve(response);
            else reject(new Error(body?.message || '请求失败，请重试'));
        },
        fail: () => reject(new Error('网络异常，请重试')),
    }));
}

export function importFinished(task) {
    return task.finished === true || ['COMPLETED', 'PARTIAL', 'FAILED'].includes(task.state) || [2, 3].includes(Number(task.status));
}
export function importStateLabel(task) {
    if (task.state === 'PARTIAL') return '部分成功';
    return ({ UPLOADING: '上传中', QUEUED: '待处理', PROCESSING: '处理中', COMPLETED: '完成', FAILED: '失败' })[task.state]
        || ({ 0: '待处理', 1: '处理中', 2: '完成', 3: '失败' })[task.status] || '待处理';
}
export function importRows(rows) {
    return Array.isArray(rows) ? rows.map((row) => ({ ...row, row_data: Array.isArray(row.row_data) ? row.row_data : [] })) : [];
}

/** One request at a time; stop/task switch invalidates both in-flight responses and timers. */
export function createImportPoller({ progress, result, onProgress, onResult, onError, schedule = setTimeout, cancel = clearTimeout, delay = 2000 }) {
    let version = 0;
    let timer = null;
    function stop() { version++; if (timer !== null) cancel(timer); timer = null; }
    function start(id) {
        stop();
        const active = version;
        const tick = async () => {
            try {
                const task = await progress(id);
                if (active !== version) return;
                onProgress(task);
                if (importFinished(task)) {
                    const details = await result(id);
                    if (active !== version) return;
                    if (importFinished(details)) onResult(details);
                    else { onProgress(details); timer = schedule(tick, delay); }
                } else timer = schedule(tick, delay);
            } catch (error) { if (active === version) onError(error); }
        };
        void tick();
    }
    return { start, stop };
}

/** Legacy API uses this marker for imports without a ZIP archive. */
export function importSessionId(zipId) { return zipId || '3session_id'; }
