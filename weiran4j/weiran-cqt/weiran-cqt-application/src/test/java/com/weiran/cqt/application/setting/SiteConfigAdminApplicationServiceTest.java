package com.weiran.cqt.application.setting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.weiran.cqt.api.setting.SiteConfigAdminItem;
import com.weiran.cqt.api.setting.SiteConfigValue;
import com.weiran.cqt.domain.setting.SettingRepository;
import com.weiran.cqt.domain.setting.SiteConfig;
import com.weiran.framework.error.BizException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SiteConfigAdminApplicationServiceTest {

    @Test
    @DisplayName("管理读取返回原始内容并包含全部固定配置项")
    void returnsRawContentsForAllDefinitions() {
        final FakeSettingRepository repository = new FakeSettingRepository();
        repository.contents.put(2, "<p>关于我们</p>");
        repository.contents.put(71, "<p>用户协议</p>");

        final List<SiteConfigAdminItem> items = new SiteConfigAdminApplicationService(repository).getEditableConfig();

        assertThat(items).hasSize(22);
        assertThat(items)
                .filteredOn(item -> item.key().equals("guanyuwomen"))
                .singleElement()
                .extracting(SiteConfigAdminItem::contents)
                .isEqualTo("<p>关于我们</p>");
        assertThat(items)
                .filteredOn(item -> item.key().equals("user_agreement"))
                .singleElement()
                .extracting(SiteConfigAdminItem::ident)
                .isEqualTo(71);
    }

    @Test
    @DisplayName("全量保存分别写入独立 ident 且保留原始 HTML")
    void savesSeparateRawValues() {
        final FakeSettingRepository repository = new FakeSettingRepository();
        final List<SiteConfigValue> items = new ArrayList<>();
        SiteConfigAdminApplicationServiceTest.addAllDefinitions(items);
        items.removeIf(item -> item.key().equals("guanyuwomen") || item.key().equals("user_agreement"));
        items.add(new SiteConfigValue("guanyuwomen", "<p>关于我们</p>"));
        items.add(new SiteConfigValue("user_agreement", "<p>用户协议</p>"));

        new SiteConfigAdminApplicationService(repository).saveConfig(items);

        assertThat(repository.contents).containsEntry(2, "<p>关于我们</p>").containsEntry(71, "<p>用户协议</p>");
    }

    @Test
    @DisplayName("缺少配置键时拒绝部分更新")
    void rejectsPartialUpdate() {
        final FakeSettingRepository repository = new FakeSettingRepository();

        assertThatThrownBy(() -> new SiteConfigAdminApplicationService(repository)
                        .saveConfig(List.of(new SiteConfigValue("guanyuwomen", "about"))))
                .isInstanceOf(BizException.class);
        assertThat(repository.contents).isEmpty();
    }

    @Test
    @DisplayName("缺少配置列表或包含空配置项时返回参数错误")
    void rejectsMissingConfigItems() {
        final FakeSettingRepository repository = new FakeSettingRepository();
        final SiteConfigAdminApplicationService service = new SiteConfigAdminApplicationService(repository);

        assertThatThrownBy(() -> service.saveConfig(null)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> service.saveConfig(Arrays.asList((SiteConfigValue) null)))
                .isInstanceOf(BizException.class);
        assertThat(repository.contents).isEmpty();
    }

    private static void addAllDefinitions(final List<SiteConfigValue> items) {
        for (final var definition : SiteConfig.definitions()) {
            items.add(new SiteConfigValue(definition.key(), null));
        }
    }

    private static final class FakeSettingRepository implements SettingRepository {

        private final Map<Integer, @Nullable String> contents = new HashMap<>();

        @Override
        public Map<Integer, @Nullable String> findContentsByIdentRange(final int from, final int to) {
            return this.contents;
        }

        @Override
        public void saveContents(final int ident, final @Nullable String value) {
            this.contents.put(ident, value);
        }
    }
}
