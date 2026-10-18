package com.weiran.cqt.api.news;

import org.jspecify.annotations.NullMarked;

/** 新增或更新大赛动态的字段。 */
@NullMarked
public record NewsCommand(String title, String image, boolean showOnHome, int sort, String content) {}
