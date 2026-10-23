package com.weiran.system.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import com.weiran.framework.persistence.Likes;
import com.weiran.framework.persistence.MybatisPages;
import com.weiran.framework.status.EnableStatus;
import com.weiran.system.domain.auth.AuthState;
import com.weiran.system.domain.user.Gender;
import com.weiran.system.domain.user.User;
import com.weiran.system.domain.user.UserAccountField;
import com.weiran.system.domain.user.UserCriteria;
import com.weiran.system.domain.user.UserRepository;
import com.weiran.system.infrastructure.persistence.entity.SysUserBackendDO;
import com.weiran.system.infrastructure.persistence.entity.SysUserDO;
import com.weiran.system.infrastructure.persistence.entity.SysUserRoleDO;
import com.weiran.system.infrastructure.persistence.mapper.SysUserBackendMapper;
import com.weiran.system.infrastructure.persistence.mapper.SysUserMapper;
import com.weiran.system.infrastructure.persistence.mapper.SysUserRoleMapper;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/** {@link UserRepository} 的 MyBatis-Plus 实现。 */
public class MybatisUserRepository implements UserRepository {

    private static final String BACKEND = "backend";

    /**
     * 显式把 updated_at 赋回原值，抵消列定义上的 {@code on update current_timestamp}。
     *
     * <p>用于登录信息的定向更新。界面偏好与收藏菜单现位于无审计时间列的后台扩展表。
     */
    private static final String KEEP_UPDATED_AT = "updated_at = updated_at";

    private static final TypeReference<List<Long>> LONG_LIST = new TypeReference<>() {};

    private final SysUserMapper userMapper;

    private final SysUserBackendMapper backendMapper;

    private final SysUserRoleMapper userRoleMapper;

    private final ObjectMapper objectMapper;

    /** 构造仓储。 */
    public MybatisUserRepository(
            final SysUserMapper userMapper,
            final SysUserBackendMapper backendMapper,
            final SysUserRoleMapper userRoleMapper,
            final ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.backendMapper = backendMapper;
        this.userRoleMapper = userRoleMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<User> findById(final long id) {
        return Optional.ofNullable(this.userMapper.selectById(id))
                .filter(row -> MybatisUserRepository.BACKEND.equals(row.getType()))
                .map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(final String username) {
        return Optional.ofNullable(this.userMapper.selectOne(Wrappers.lambdaQuery(SysUserDO.class)
                        .eq(SysUserDO::getType, MybatisUserRepository.BACKEND)
                        .eq(SysUserDO::getUsername, username)))
                .map(this::toDomain);
    }

    @Override
    public Optional<User> findByPhone(final String phone) {
        return Optional.ofNullable(this.userMapper.selectOne(Wrappers.lambdaQuery(SysUserDO.class)
                        .eq(SysUserDO::getType, MybatisUserRepository.BACKEND)
                        .eq(SysUserDO::getPhone, phone)))
                .map(this::toDomain);
    }

    @Override
    public Map<String, List<Long>> findAvatarReferences(final Collection<String> urls) {
        if (urls.isEmpty()) {
            return Map.of();
        }
        return this.backendMapper
                .selectList(Wrappers.lambdaQuery(SysUserBackendDO.class)
                        .select(SysUserBackendDO::getUserId, SysUserBackendDO::getAvatar)
                        .in(SysUserBackendDO::getAvatar, urls))
                .stream()
                .collect(Collectors.groupingBy(
                        SysUserBackendDO::getAvatar,
                        Collectors.mapping(SysUserBackendDO::getUserId, Collectors.toList())));
    }

    @Override
    public boolean existsByUsername(final String username) {
        return this.userMapper.exists(Wrappers.lambdaQuery(SysUserDO.class).eq(SysUserDO::getUsername, username));
    }

    @Override
    public boolean existsByEmail(final String email) {
        return this.userMapper.exists(Wrappers.lambdaQuery(SysUserDO.class).eq(SysUserDO::getEmail, email));
    }

    @Override
    public boolean existsByEmail(final String email, final long excludedUserId) {
        return this.userMapper.exists(Wrappers.lambdaQuery(SysUserDO.class)
                .eq(SysUserDO::getEmail, email)
                .ne(SysUserDO::getId, excludedUserId));
    }

    @Override
    public boolean existsByPhone(final String phone) {
        return this.userMapper.exists(Wrappers.lambdaQuery(SysUserDO.class).eq(SysUserDO::getPhone, phone));
    }

    @Override
    public boolean existsByPhone(final String phone, final long excludedUserId) {
        return this.userMapper.exists(Wrappers.lambdaQuery(SysUserDO.class)
                .eq(SysUserDO::getPhone, phone)
                .ne(SysUserDO::getId, excludedUserId));
    }

    @Override
    public void updateAccountField(final long id, final UserAccountField field, final @Nullable String value) {
        final SysUserDO row = new SysUserDO();
        row.setId(id);
        final var update = Wrappers.lambdaUpdate(SysUserDO.class).eq(SysUserDO::getId, id);
        switch (field) {
            case USERNAME -> update.set(SysUserDO::getUsername, value);
            case PHONE -> update.set(SysUserDO::getPhone, value);
            case EMAIL -> update.set(SysUserDO::getEmail, value);
        }
        this.userMapper.update(row, update);
    }

    @Override
    public long insert(final User user) {
        final SysUserDO row = MybatisUserRepository.toDataObject(user);
        this.userMapper.insert(row);
        final SysUserBackendDO backend = new SysUserBackendDO();
        backend.setUserId(row.getId());
        backend.setAvatar(user.getAvatar());
        backend.setGender(user.getGender().value());
        this.backendMapper.insert(backend);
        return row.getId();
    }

    @Override
    public void updateProfile(final User user) {
        final SysUserDO row = new SysUserDO();
        row.setId(user.requireId());
        this.userMapper.update(
                row,
                Wrappers.lambdaUpdate(SysUserDO.class)
                        .set(SysUserDO::getNickname, user.getNickname())
                        .set(SysUserDO::getEmail, user.getEmail())
                        .set(SysUserDO::getPhone, user.getPhone())
                        .eq(SysUserDO::getId, user.requireId()));
        this.backendMapper.update(Wrappers.lambdaUpdate(SysUserBackendDO.class)
                .set(SysUserBackendDO::getAvatar, user.getAvatar())
                .set(SysUserBackendDO::getGender, user.getGender().value())
                .eq(SysUserBackendDO::getUserId, user.requireId()));
    }

    @Override
    public void updateAccount(final User user) {
        // 传入只带 ID 的实体是为了触发审计字段填充（updated_at / updated_by）；实体其余字段为空不参与 SET。
        final SysUserDO row = new SysUserDO();
        row.setId(user.requireId());
        this.userMapper.update(
                row,
                Wrappers.lambdaUpdate(SysUserDO.class)
                        .set(SysUserDO::getNickname, user.getNickname())
                        .set(SysUserDO::getEmail, user.getEmail())
                        .set(SysUserDO::getPhone, user.getPhone())
                        .set(SysUserDO::getStatus, user.getStatus().value())
                        .eq(SysUserDO::getId, user.requireId()));
        this.backendMapper.update(Wrappers.lambdaUpdate(SysUserBackendDO.class)
                .set(SysUserBackendDO::getGender, user.getGender().value())
                .eq(SysUserBackendDO::getUserId, user.requireId()));
    }

    @Override
    public void recordLogin(final long id, final String ip, final LocalDateTime at) {
        this.userMapper.update(Wrappers.lambdaUpdate(SysUserDO.class)
                .set(SysUserDO::getLastLoginAt, at)
                .set(SysUserDO::getLastLoginIp, ip)
                .setSql(MybatisUserRepository.KEEP_UPDATED_AT)
                .eq(SysUserDO::getId, id));
    }

    @Override
    public void changePassword(final long id, final String passwordHash, final LocalDateTime at) {
        this.userMapper.update(Wrappers.lambdaUpdate(SysUserDO.class)
                .set(SysUserDO::getPassword, passwordHash)
                .set(SysUserDO::getPasswordUpdatedAt, at)
                .setSql("token_version = token_version + 1")
                .eq(SysUserDO::getId, id));
    }

    @Override
    public void revokeTokens(final long id) {
        this.userMapper.update(Wrappers.lambdaUpdate(SysUserDO.class)
                .setSql("token_version = token_version + 1")
                .eq(SysUserDO::getId, id));
    }

    @Override
    public Optional<AuthState> findAuthState(final long id) {
        // 每个已认证请求都会走到这里：只按主键取类型、令牌版本与状态，不加载整行。
        return Optional.ofNullable(this.userMapper.selectOne(Wrappers.lambdaQuery(SysUserDO.class)
                        .select(SysUserDO::getId, SysUserDO::getType, SysUserDO::getTokenVersion, SysUserDO::getStatus)
                        .eq(SysUserDO::getId, id)))
                .filter(row -> MybatisUserRepository.BACKEND.equals(row.getType()))
                .map(row -> new AuthState(
                        row.getTokenVersion() == null ? 0 : row.getTokenVersion(),
                        EnableStatus.of(row.getStatus()) == EnableStatus.ENABLED));
    }

    @Override
    public Optional<String> findPreferences(final long id) {
        return Optional.ofNullable(this.backendMapper.selectOne(Wrappers.lambdaQuery(SysUserBackendDO.class)
                        .select(SysUserBackendDO::getUserId, SysUserBackendDO::getPreferences)
                        .eq(SysUserBackendDO::getUserId, id)))
                .map(SysUserBackendDO::getPreferences);
    }

    @Override
    public void updatePreferences(final long id, final String preferencesJson) {
        // 后台界面偏好只更新扩展表，不刷新基础账号的 updated_at。
        this.backendMapper.update(Wrappers.lambdaUpdate(SysUserBackendDO.class)
                .set(SysUserBackendDO::getPreferences, preferencesJson)
                .eq(SysUserBackendDO::getUserId, id));
    }

    @Override
    public List<Long> findFavoriteMenuIds(final long id) {
        return Optional.ofNullable(this.backendMapper.selectOne(Wrappers.lambdaQuery(SysUserBackendDO.class)
                        .select(SysUserBackendDO::getUserId, SysUserBackendDO::getFavoriteMenus)
                        .eq(SysUserBackendDO::getUserId, id)))
                .map(SysUserBackendDO::getFavoriteMenus)
                .map(json -> this.parseMenuIds(id, json))
                .orElse(List.of());
    }

    @Override
    public void updateFavoriteMenuIds(final long id, final List<Long> menuIds) {
        final String json;
        try {
            json = this.objectMapper.writeValueAsString(menuIds);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("收藏菜单序列化失败", ex);
        }
        this.backendMapper.update(Wrappers.lambdaUpdate(SysUserBackendDO.class)
                .set(SysUserBackendDO::getFavoriteMenus, json)
                .eq(SysUserBackendDO::getUserId, id));
    }

    @Override
    public void deleteById(final long id) {
        this.userRoleMapper.delete(Wrappers.lambdaQuery(SysUserRoleDO.class).eq(SysUserRoleDO::getUserId, id));
        this.backendMapper.deleteById(id);
        this.userMapper.deleteById(id);
    }

    @Override
    public PageResult<User> page(final UserCriteria criteria, final PageQuery pageQuery) {
        final String keyword = criteria.keyword();
        final EnableStatus status = criteria.status();
        final Gender gender = criteria.gender();
        final Long roleId = criteria.roleId();
        final LambdaQueryWrapper<SysUserDO> wrapper = Wrappers.lambdaQuery(SysUserDO.class)
                .and(
                        keyword != null,
                        w -> w.like(SysUserDO::getUsername, Likes.escape(keyword))
                                .or()
                                .like(SysUserDO::getNickname, Likes.escape(keyword))
                                .or()
                                .like(SysUserDO::getPhone, Likes.escape(keyword)))
                .eq(status != null, SysUserDO::getStatus, status == null ? null : status.value())
                .eq(criteria.username() != null, SysUserDO::getUsername, criteria.username())
                .eq(criteria.userId() != null, SysUserDO::getId, criteria.userId())
                .eq(criteria.phone() != null, SysUserDO::getPhone, criteria.phone())
                .eq(criteria.email() != null, SysUserDO::getEmail, criteria.email())
                .eq(SysUserDO::getType, MybatisUserRepository.BACKEND)
                .ge(criteria.createdFrom() != null, SysUserDO::getCreatedAt, criteria.createdFrom())
                .le(criteria.createdTo() != null, SysUserDO::getCreatedAt, criteria.createdTo())
                // 对可空列的比较天然排除 NULL：传了登录时间任一端，从未登录的用户即被排除
                .ge(criteria.lastLoginFrom() != null, SysUserDO::getLastLoginAt, criteria.lastLoginFrom())
                .le(criteria.lastLoginTo() != null, SysUserDO::getLastLoginAt, criteria.lastLoginTo())
                // 子查询而非 join：一人多角色也只出一行，total 正确；role_id 走参数绑定
                .apply(roleId != null, "id in (select user_id from sys_user_role where role_id = {0})", roleId)
                .apply(
                        gender != null,
                        "id in (select user_id from sys_user_backend where gender = {0})",
                        gender == null ? null : gender.value())
                .orderByDesc(SysUserDO::getId);
        final var rows = this.userMapper.selectPage(MybatisPages.of(pageQuery), wrapper);
        final Map<Long, SysUserBackendDO> profiles = this.findBackendProfiles(
                rows.getRecords().stream().map(SysUserDO::getId).toList());
        return MybatisPages.toResult(rows, pageQuery, row -> this.toDomain(row, profiles.get(row.getId())));
    }

    @Override
    public List<User> findEnabled() {
        final List<SysUserDO> rows = this.userMapper.selectList(Wrappers.lambdaQuery(SysUserDO.class)
                .eq(SysUserDO::getType, MybatisUserRepository.BACKEND)
                .eq(SysUserDO::getStatus, EnableStatus.ENABLED.value())
                .orderByAsc(SysUserDO::getId));
        final Map<Long, SysUserBackendDO> profiles =
                this.findBackendProfiles(rows.stream().map(SysUserDO::getId).toList());
        return rows.stream()
                .map(row -> this.toDomain(row, profiles.get(row.getId())))
                .toList();
    }

    @Override
    public List<User> findByIds(final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        final List<SysUserDO> rows = this.userMapper.selectByIds(ids).stream()
                .filter(row -> MybatisUserRepository.BACKEND.equals(row.getType()))
                .toList();
        final Map<Long, SysUserBackendDO> profiles =
                this.findBackendProfiles(rows.stream().map(SysUserDO::getId).toList());
        return rows.stream()
                .map(row -> this.toDomain(row, profiles.get(row.getId())))
                .toList();
    }

    @Override
    public Set<Long> findRoleIds(final long userId) {
        return this.userRoleMapper
                .selectList(Wrappers.lambdaQuery(SysUserRoleDO.class).eq(SysUserRoleDO::getUserId, userId))
                .stream()
                .map(SysUserRoleDO::getRoleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Map<Long, Set<Long>> findRoleIdsByUserIds(final Collection<Long> userIds) {
        final Map<Long, Set<Long>> result = new HashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }
        this.userRoleMapper
                .selectList(Wrappers.lambdaQuery(SysUserRoleDO.class).in(SysUserRoleDO::getUserId, userIds))
                .forEach(link -> result.computeIfAbsent(link.getUserId(), key -> new LinkedHashSet<>())
                        .add(link.getRoleId()));
        return result;
    }

    @Override
    public void replaceRoles(final long userId, final Collection<Long> roleIds) {
        this.userRoleMapper.delete(Wrappers.lambdaQuery(SysUserRoleDO.class).eq(SysUserRoleDO::getUserId, userId));
        for (final Long roleId : new LinkedHashSet<>(roleIds)) {
            final SysUserRoleDO link = new SysUserRoleDO();
            link.setUserId(userId);
            link.setRoleId(roleId);
            this.userRoleMapper.insert(link);
        }
    }

    private List<Long> parseMenuIds(final long userId, final String json) {
        try {
            return List.copyOf(this.objectMapper.readValue(json, MybatisUserRepository.LONG_LIST));
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("sys_user_backend.favorite_menus 不是合法的 ID 数组，userId=" + userId, ex);
        }
    }

    private User toDomain(final SysUserDO row) {
        return this.toDomain(row, this.backendMapper.selectById(row.getId()));
    }

    private User toDomain(final SysUserDO row, final @Nullable SysUserBackendDO backend) {
        if (backend == null) {
            throw new IllegalStateException("后台用户缺少 sys_user_backend 资料，userId=" + row.getId());
        }
        return User.builder()
                .id(row.getId())
                .username(row.getUsername())
                .nickname(row.getNickname())
                .passwordHash(row.getPassword())
                .email(row.getEmail())
                .phone(row.getPhone())
                .avatar(backend.getAvatar())
                .gender(Gender.of(backend.getGender()))
                .status(EnableStatus.of(row.getStatus()))
                .tokenVersion(row.getTokenVersion() == null ? 0 : row.getTokenVersion())
                .lastLoginAt(row.getLastLoginAt())
                .lastLoginIp(row.getLastLoginIp())
                .passwordUpdatedAt(row.getPasswordUpdatedAt())
                .builtin(Boolean.TRUE.equals(row.getIsBuiltin()))
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .build();
    }

    private static SysUserDO toDataObject(final User user) {
        final SysUserDO row = new SysUserDO();
        row.setId(user.getId());
        row.setUsername(user.getUsername());
        row.setNickname(user.getNickname());
        row.setPassword(user.getPasswordHash());
        row.setEmail(user.getEmail());
        row.setPhone(user.getPhone());
        row.setType(MybatisUserRepository.BACKEND);
        row.setStatus(user.getStatus().value());
        row.setTokenVersion(user.getTokenVersion());
        row.setLastLoginAt(user.getLastLoginAt());
        row.setLastLoginIp(user.getLastLoginIp());
        row.setPasswordUpdatedAt(user.getPasswordUpdatedAt());
        row.setIsBuiltin(user.isBuiltin());
        return row;
    }

    private Map<Long, SysUserBackendDO> findBackendProfiles(final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return this.backendMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(SysUserBackendDO::getUserId, profile -> profile));
    }
}
