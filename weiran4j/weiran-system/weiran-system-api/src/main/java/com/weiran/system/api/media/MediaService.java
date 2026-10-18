package com.weiran.system.api.media;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.io.IOException;
import java.io.InputStream;
import org.jspecify.annotations.Nullable;

/** 图片资产管理服务。 */
public interface MediaService {

    ImageUploadView upload(String originalName, InputStream content, long size, long uploadedBy) throws IOException;

    PageResult<MediaView> page(@Nullable String keyword, PageQuery pageQuery);

    void delete(long id);
}
