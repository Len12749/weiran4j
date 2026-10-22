package com.weiran.system.application.media;

import com.weiran.framework.error.BizException;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import com.weiran.platform.domain.config.ConfigRepository;
import com.weiran.platform.domain.config.SystemConfig;
import com.weiran.system.api.media.FileUploadView;
import com.weiran.system.api.media.MediaService;
import com.weiran.system.api.media.MediaView;
import com.weiran.system.domain.media.MediaAsset;
import com.weiran.system.domain.media.MediaFileTypes;
import com.weiran.system.domain.media.MediaRepository;
import com.weiran.system.domain.storage.FileStorage;
import com.weiran.system.domain.storage.FileStorage.StoredFile;
import com.weiran.system.domain.storage.FileStorageException;
import com.weiran.system.domain.user.UserRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 文件上传、查询和引用保护删除用例。 */
public class MediaApplicationService implements MediaService {

    private static final long MAX_SIZE = 10L * 1024L * 1024L;

    private final MediaRepository mediaRepository;
    private final FileStorage fileStorage;
    private final MediaAssetDeletionService deletionService;
    private final @Nullable ConfigRepository configRepository;
    private final @Nullable UserRepository userRepository;

    @Autowired
    public MediaApplicationService(
            final MediaRepository mediaRepository,
            final FileStorage fileStorage,
            final MediaAssetDeletionService deletionService,
            final ConfigRepository configRepository,
            final UserRepository userRepository) {
        this.mediaRepository = mediaRepository;
        this.fileStorage = fileStorage;
        this.deletionService = deletionService;
        this.configRepository = configRepository;
        this.userRepository = userRepository;
    }

    /** 供无需配置仓储的轻量使用者使用。 */
    public MediaApplicationService(
            final MediaRepository mediaRepository,
            final FileStorage fileStorage,
            final MediaAssetDeletionService deletionService) {
        this.mediaRepository = mediaRepository;
        this.fileStorage = fileStorage;
        this.deletionService = deletionService;
        this.configRepository = null;
        this.userRepository = null;
    }

    @Override
    @Transactional
    public FileUploadView upload(
            final String originalName, final InputStream content, final long size, final long uploadedBy)
            throws IOException {
        final StoredFile stored;
        try (PushbackInputStream checked = new PushbackInputStream(content, 12)) {
            if (size == 0 || size > MediaApplicationService.MAX_SIZE) {
                throw BizException.badRequest("文件不能为空且不得超过 10 MB");
            }
            final String extension = MediaFileTypes.extension(originalName);
            if (!MediaFileTypes.allowed(extension)) {
                throw BizException.badRequest("不支持该文件类型");
            }
            final byte[] header = checked.readNBytes(12);
            checked.unread(header);
            if (MediaFileTypes.image(extension) && !MediaApplicationService.matchesImageSignature(extension, header)) {
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
                throw new BizException(com.weiran.framework.error.CommonErrors.INTERNAL_ERROR, "文件上传失败，请稍后重试");
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
        return new FileUploadView(stored.url(), originalName, stored.objectName(), stored.size());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MediaView> page(
            final @Nullable String keyword, final @Nullable String category, final PageQuery pageQuery) {
        final PageResult<MediaAsset> result = this.mediaRepository.page(keyword, category, pageQuery);
        final List<SystemConfig> configs = this.configRepository == null ? List.of() : this.configRepository.findAll();
        final Map<String, List<Long>> avatarReferences = this.userRepository == null
                ? Map.of()
                : this.userRepository.findAvatarReferences(
                        result.list().stream().map(MediaAsset::url).toList());
        return new PageResult<>(
                result.list().stream()
                        .map(asset -> MediaApplicationService.toView(asset, configs, avatarReferences))
                        .toList(),
                result.total(),
                result.page(),
                result.pageSize());
    }

    @Override
    @Transactional
    public void delete(final long id) {
        final MediaAsset asset = this.mediaRepository.findById(id).orElseThrow(() -> BizException.notFound("文件不存在"));
        final List<SystemConfig> configs = this.configRepository == null ? List.of() : this.configRepository.findAll();
        final Map<String, List<Long>> avatarReferences =
                this.userRepository == null ? Map.of() : this.userRepository.findAvatarReferences(List.of(asset.url()));
        if (!MediaApplicationService.references(asset, configs, avatarReferences)
                .isEmpty()) {
            throw BizException.conflict("文件仍被使用，请先移除引用");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                MediaApplicationService.this.fileStorage.delete(asset.objectName());
                MediaApplicationService.this.deletionService.deleteRecord(id);
            }
        });
    }

    private static MediaView toView(
            final MediaAsset asset, final List<SystemConfig> configs, final Map<String, List<Long>> avatarReferences) {
        return new MediaView(
                asset.id(),
                asset.originalName(),
                asset.url(),
                asset.size(),
                asset.createdAt(),
                MediaFileTypes.category(asset.originalName()),
                MediaApplicationService.references(asset, configs, avatarReferences));
    }

    private static List<String> references(
            final MediaAsset asset, final List<SystemConfig> configs, final Map<String, List<Long>> avatarReferences) {
        final List<String> configReferences = configs.stream()
                .filter(config -> config.getConfigValue().contains(asset.url()))
                .map(config -> "系统配置：" + config.getConfigKey())
                .toList();
        final List<String> userReferences = avatarReferences.getOrDefault(asset.url(), List.of()).stream()
                .map(id -> "用户头像：#" + id)
                .toList();
        return java.util.stream.Stream.concat(configReferences.stream(), userReferences.stream())
                .toList();
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
            case "webp" ->
                header.length >= 12
                        && header[0] == 'R'
                        && header[1] == 'I'
                        && header[2] == 'F'
                        && header[3] == 'F'
                        && header[8] == 'W'
                        && header[9] == 'E'
                        && header[10] == 'B'
                        && header[11] == 'P';
            default -> false;
        };
    }
}
