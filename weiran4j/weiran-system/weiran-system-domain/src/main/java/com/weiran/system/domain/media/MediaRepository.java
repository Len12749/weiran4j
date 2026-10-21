package com.weiran.system.domain.media;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** 媒体资产仓储端口。 */
public interface MediaRepository {

    MediaAsset save(MediaAsset asset);

    PageResult<MediaAsset> page(@Nullable String keyword, PageQuery pageQuery);

    Optional<MediaAsset> findById(long id);

    void deleteById(long id);
}
