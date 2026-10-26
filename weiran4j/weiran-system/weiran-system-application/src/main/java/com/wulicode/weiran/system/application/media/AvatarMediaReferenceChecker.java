package com.wulicode.weiran.system.application.media;

import com.wulicode.weiran.system.api.media.MediaReferenceChecker;
import com.wulicode.weiran.system.api.media.MediaReferenceLocation;
import com.wulicode.weiran.system.domain.user.UserRepository;
import java.util.Collection;
import java.util.List;

/** 兼容历史用户头像引用的实时检查。 */
public class AvatarMediaReferenceChecker implements MediaReferenceChecker {

    private final UserRepository users;

    public AvatarMediaReferenceChecker(final UserRepository users) {
        this.users = users;
    }

    @Override
    public List<MediaReferenceLocation> findReferences(final Collection<String> urls) {
        if (urls.isEmpty()) {
            return List.of();
        }
        return this.users.findAvatarReferences(urls).entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(id -> new MediaReferenceLocation(
                                entry.getKey(), "user_avatar", String.valueOf(id), "avatar", "用户头像：#" + id)))
                .toList();
    }
}
