package com.weiran.system.api.batch;

import java.io.IOException;
import java.io.InputStream;

/** 每次调用都打开新的输入流，供处理器扫描和处理。 */
@FunctionalInterface
public interface BatchSource {
    InputStream open() throws IOException;
}
