package com.weiran.system.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weiran.system.domain.siteconfig.SiteConfigDefinition;
import com.weiran.system.domain.siteconfig.SiteConfigRepository;
import com.weiran.system.infrastructure.persistence.entity.SysSiteConfigDO;
import com.weiran.system.infrastructure.persistence.mapper.SysSiteConfigMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/** MyBatis 站点配置仓储。 */
@Repository
public class MybatisSiteConfigRepository implements SiteConfigRepository {

    private final SysSiteConfigMapper mapper;

    public MybatisSiteConfigRepository(final SysSiteConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Map<Integer, @Nullable String> findContents() {
        return this.toMap(this.mapper.selectList(new LambdaQueryWrapper<SysSiteConfigDO>()
                .orderByAsc(SysSiteConfigDO::getIdent)
                .orderByAsc(SysSiteConfigDO::getId)));
    }

    @Override
    public Map<Integer, @Nullable String> lockAndFindContents() {
        return this.toMap(this.mapper.lockAll());
    }

    @Override
    public void saveContents(final int ident, final @Nullable String contents) {
        final SysSiteConfigDO latest = this.mapper.selectOne(new LambdaQueryWrapper<SysSiteConfigDO>()
                .eq(SysSiteConfigDO::getIdent, ident)
                .orderByDesc(SysSiteConfigDO::getId)
                .last("limit 1"));
        if (latest == null) {
            final SysSiteConfigDO created = new SysSiteConfigDO();
            created.setIdent(ident);
            created.setConfigKey(SiteConfigDefinition.items().stream()
                    .filter(item -> item.ident() == ident)
                    .map(SiteConfigDefinition.Item::key)
                    .findFirst()
                    .orElseThrow());
            created.setContents(contents);
            created.setCreatedAt(Instant.now());
            created.setUpdatedAt(Instant.now());
            this.mapper.insert(created);
            return;
        }
        latest.setContents(contents);
        latest.setUpdatedAt(Instant.now());
        this.mapper.updateById(latest);
    }

    private Map<Integer, @Nullable String> toMap(final List<SysSiteConfigDO> records) {
        final Map<Integer, @Nullable String> result = new HashMap<>();
        for (final SysSiteConfigDO record : records) {
            result.put(record.getIdent(), record.getContents());
        }
        return result;
    }
}
