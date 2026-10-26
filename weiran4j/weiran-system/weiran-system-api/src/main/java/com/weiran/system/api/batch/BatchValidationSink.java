package com.weiran.system.api.batch;

@FunctionalInterface
public interface BatchValidationSink {
    void accept(BatchValidationItem item);
}
