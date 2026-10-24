package com.weiran.system.api.batch;

/** 处理器在每个项目完成后提交进度。 */
public interface BatchProgress {
    long jobId();

    /** 预检期间记录一项错误；同一行或文件只记录一条。 */
    void precheckFailure(long itemNo, String itemName, String message);

    /** 预检结束后设置总数；存在预检错误时停止进入处理阶段。 */
    void total(long value);

    void success(long itemNo, String itemName);

    void failure(long itemNo, String itemName, String message);

    boolean cancelled();
}
