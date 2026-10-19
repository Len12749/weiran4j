package com.weiran.system.application.media;

import com.weiran.framework.error.BizException;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import com.weiran.system.api.media.ImageUploadView;
import com.weiran.system.api.media.MediaService;
import com.weiran.system.api.media.MediaView;
import com.weiran.system.domain.media.MediaAsset;
import com.weiran.system.domain.media.MediaRepository;
import com.weiran.system.domain.storage.FileStorage;
import com.weiran.system.domain.storage.FileStorage.StoredFile;
import com.weiran.system.domain.storage.FileStorageException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.time.Instant;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 图片上传、查询和引用保护删除用例。 */
public class MediaApplicationService implements MediaService {

    private static final long MAX_SIZE = 10L * 1024L * 1024L;

    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif"};

    private final MediaRepository mediaRepository;
    private final FileStorage fileStorage;
    private final MediaAssetDeletionService deletionService;

    public MediaApplicationService(
            final MediaRepository mediaRepository,
            final FileStorage fileStorage,
            final MediaAssetDeletionService deletionService) {
        this.mediaRepository = mediaRepository;
        this.fileStorage = fileStorage;
        this.deletionService = deletionService;
    }

    @Override
    @Transactional
    public ImageUploadView upload(
            final String originalName, final InputStream content, final long size, final long uploadedBy)
            throws IOException {
        final StoredFile stored;
        try (PushbackInputStream checked = new PushbackInputStream(content, 8)) {
            if (size == 0 || size > MediaApplicationService.MAX_SIZE) {
                throw BizException.badRequest("图片不能为空且不得超过 10 MB");
            }
            final String extension = MediaApplicationService.extension(originalName);
            if (!MediaApplicationService.allowed(extension)) {
                throw BizException.badRequest("仅支持 jpg、jpeg、png、gif 图片");
            }
            final byte[] header = checked.readNBytes(8);
            checked.unread(header);
            if (!MediaApplicationService.matchesImageSignature(extension, header)) {
                throw BizException.badRequest("图片文件内容与扩展名不匹配");
            }
            stored = this.fileStorage.store(originalName, checked, size);
        } catch (final IOException | RuntimeException ex) {
            if (ex instanceof BizException businessException) {
                throw businessException;
            }
            if (ex instanceof FileStorageException storageException) {
                if ("文件存储未配置".equals(storageException.getMessage())) {
                    throw BizException.serviceUnavailable("文件存储未配置");
                }
                throw new BizException(com.weiran.framework.error.CommonErrors.INTERNAL_ERROR, "图片上传失败，请稍后重试");
            }
            throw new BizException(com.weiran.framework.error.CommonErrors.INTERNAL_ERROR, ex);
        }
        try {
            this.mediaRepository.save(new MediaAsset(
                    0L, originalName, stored.url(), stored.objectName(), stored.size(), uploadedBy, Instant.now()));
        } catch (final RuntimeException ex) {
            try {
                this.fileStorage.delete(stored.objectName());
            } catch (final RuntimeException cleanupFailure) {
                ex.addSuppressed(cleanupFailure);
            }
            throw ex;
        }
        return new ImageUploadView(stored.url(), originalName, stored.objectName(), stored.size());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MediaView> page(final @Nullable String keyword, final PageQuery pageQuery) {
        final PageResult<MediaAsset> result = this.mediaRepository.page(keyword, pageQuery);
        return new PageResult<>(
                result.list().stream().map(MediaApplicationService::toView).toList(),
                result.total(),
                result.page(),
                result.pageSize());
    }

    @Override
    @Transactional
    public void delete(final long id) {
        final MediaAsset asset = this.mediaRepository.findById(id).orElseThrow(() -> BizException.notFound("图片不存在"));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                MediaApplicationService.this.fileStorage.delete(asset.objectName());
                MediaApplicationService.this.deletionService.deleteRecord(id);
            }
        });
    }

    private static MediaView toView(final MediaAsset asset) {
        return new MediaView(asset.id(), asset.originalName(), asset.url(), asset.size(), asset.createdAt());
    }

    private static boolean allowed(final String extension) {
        for (final String allowed : MediaApplicationService.ALLOWED_EXTENSIONS) {
            if (allowed.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesImageSignature(final String extension, final byte[] header) {
        return switch (extension) {
            case "jpg", "jpeg" ->
                header.length >= 3 && header[0] == (byte) 0xff && header[1] == (byte) 0xd8 && header[2] == (byte) 0xff;
            case "png" ->
                header.length >= 8
                        && header[0] == (byte) 0x89
                        && header[1] == 0x50
                        && header[2] == 0x4e
                        && header[3] == 0x47
                        && header[4] == 0x0d
                        && header[5] == 0x0a
                        && header[6] == 0x1a
                        && header[7] == 0x0a;
            case "gif" ->
                header.length >= 6
                        && header[0] == 'G'
                        && header[1] == 'I'
                        && header[2] == 'F'
                        && header[3] == '8'
                        && (header[4] == '7' || header[4] == '9')
                        && header[5] == 'a';
            default -> false;
        };
    }

    private static String extension(final String originalName) {
        final int dot = originalName.lastIndexOf('.');
        return dot < 0 ? "" : originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
