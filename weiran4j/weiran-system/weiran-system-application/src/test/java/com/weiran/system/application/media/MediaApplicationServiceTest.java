package com.weiran.system.application.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weiran.framework.error.BizException;
import com.weiran.system.domain.media.MediaRepository;
import com.weiran.system.domain.siteconfig.SiteConfigRepository;
import com.weiran.system.domain.storage.FileStorage;
import com.weiran.system.domain.storage.FileStorageException;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MediaApplicationServiceTest {

    @Test
    @DisplayName("存储未配置时上传返回 503")
    void returnsServiceUnavailableWhenStorageIsDisabled() {
        final MediaRepository mediaRepository = mock(MediaRepository.class);
        final SiteConfigRepository siteConfigRepository = mock(SiteConfigRepository.class);
        final FileStorage fileStorage = mock(FileStorage.class);
        when(fileStorage.store(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyLong()))
                .thenThrow(new FileStorageException("文件存储未配置"));
        final MediaApplicationService service = new MediaApplicationService(
                mediaRepository, siteConfigRepository, fileStorage, new MediaAssetDeletionService(mediaRepository));

        assertThatThrownBy(() -> service.upload(
                        "image.png",
                        new ByteArrayInputStream(new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}),
                        8,
                        1L))
                .isInstanceOf(BizException.class)
                .satisfies(exception -> assertThat(
                                ((BizException) exception).getErrorCode().code())
                        .isEqualTo(50300));
    }
}
