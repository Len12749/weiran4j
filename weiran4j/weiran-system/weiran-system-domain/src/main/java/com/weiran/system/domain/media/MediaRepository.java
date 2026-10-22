package com.weiran.system.domain.media;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** 媒体资产仓储端口。 */
public interface MediaRepository {

    MediaAsset save(MediaAsset asset);

    PageResult<MediaAsset> page(@Nullable String keyword, @Nullable String category, PageQuery pageQuery);

    default PageResult<MediaAsset> page(final @Nullable String keyword, final PageQuery pageQuery) {
        return this.page(keyword, null, pageQuery);
    }

    Optional<MediaAsset> findById(long id);

    void deleteById(long id);
}
