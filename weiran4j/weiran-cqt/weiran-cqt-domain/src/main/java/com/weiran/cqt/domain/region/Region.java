package com.weiran.cqt.domain.region;

import org.jspecify.annotations.Nullable;

/**
 * 地区快照，ID 来自行政区划表 {@code region}。
 *
 * @param id 地区 ID
 * @param parentId 上级地区 ID（0 为顶级）
 * @param name 名称
 * @param code 编码
 */
public record Region(
        long id, long parentId, String name, @Nullable String code) {}
