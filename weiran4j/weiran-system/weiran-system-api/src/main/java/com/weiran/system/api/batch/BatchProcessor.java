package com.weiran.system.api.batch;

/** 业务模块实现此接口并注册为 Spring Bean；基座只负责任务编排。 */
public interface BatchProcessor {
    String kind();

    String extension();

    /** 创建任务时校验该处理器的业务权限；拒绝时抛业务异常。 */
    void authorize(long userId);

    default long maxBytes() {
        return 100L * 1024 * 1024;
    }

    void process(BatchSource source, BatchProgress progress) throws Exception;
}
