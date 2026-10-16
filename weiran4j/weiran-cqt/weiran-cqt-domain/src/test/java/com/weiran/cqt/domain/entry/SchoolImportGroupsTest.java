package com.weiran.cqt.domain.entry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SchoolImportGroupsTest {

    @Test
    void assemblesConsecutiveTeamRowsAndFollowingIndividualEntry() {
        final List<SchoolImportGroup> groups = SchoolImportGroups.assemble(List.of(
                SchoolImportGroupsTest.row(3, SchoolImportEntryType.TEAM, 2, "戏剧", "表演", "春天"),
                SchoolImportGroupsTest.row(4, SchoolImportEntryType.TEAM, 2, "戏剧", "表演", "春天"),
                SchoolImportGroupsTest.row(5, SchoolImportEntryType.INDIVIDUAL, 1, "戏剧", "导演", "夏天")));

        assertThat(groups).hasSize(2);
        assertThat(groups.getFirst().isValid()).isTrue();
        assertThat(groups.getFirst().rows()).hasSize(2);
        assertThat(groups.getLast().isTeam()).isFalse();
    }

    @Test
    void rejectsTeamRowsWithDifferentTitleAndContinuesAtNextGroup() {
        final List<SchoolImportGroup> groups = SchoolImportGroups.assemble(List.of(
                SchoolImportGroupsTest.row(3, SchoolImportEntryType.TEAM, 2, "戏剧", "表演", "春天"),
                SchoolImportGroupsTest.row(4, SchoolImportEntryType.TEAM, 2, "戏剧", "表演", "秋天"),
                SchoolImportGroupsTest.row(5, SchoolImportEntryType.TEAM, 2, "戏剧", "表演", "冬天"),
                SchoolImportGroupsTest.row(6, SchoolImportEntryType.TEAM, 2, "戏剧", "表演", "冬天")));

        assertThat(groups).hasSize(2);
        assertThat(groups.getFirst().errorMessage()).contains("作品名称");
        assertThat(groups.getLast().isValid()).isTrue();
    }

    @Test
    void rejectsIncompleteTeam() {
        final List<SchoolImportGroup> groups = SchoolImportGroups.assemble(List.of(
                SchoolImportGroupsTest.row(3, SchoolImportEntryType.TEAM, 3, "戏剧", "表演", "春天"),
                SchoolImportGroupsTest.row(4, SchoolImportEntryType.TEAM, 3, "戏剧", "表演", "春天")));

        assertThat(groups)
                .singleElement()
                .extracting(SchoolImportGroup::errorMessage)
                .asString()
                .contains("不足");
    }

    @Test
    void rejectsTeamRowsWithDifferentCategoryMajorOrDeclaredCount() {
        final List<SchoolImportGroup> groups = SchoolImportGroups.assemble(List.of(
                SchoolImportGroupsTest.row(3, SchoolImportEntryType.TEAM, 2, "戏剧", "表演", "作品一"),
                SchoolImportGroupsTest.row(4, SchoolImportEntryType.TEAM, 3, "戏剧", "表演", "作品一"),
                SchoolImportGroupsTest.row(5, SchoolImportEntryType.TEAM, 2, "舞蹈", "表演", "作品二"),
                SchoolImportGroupsTest.row(6, SchoolImportEntryType.TEAM, 2, "舞蹈", "导演", "作品二")));

        assertThat(groups).hasSize(2);
        assertThat(groups.getFirst().errorMessage()).contains("申报人数");
        assertThat(groups.getLast().errorMessage()).contains("赛项、专业");
    }

    private static SchoolImportRow row(
            final int line,
            final SchoolImportEntryType type,
            final int count,
            final String category,
            final String major,
            final String title) {
        return new SchoolImportRow(
                line,
                type,
                count,
                "参赛人" + line,
                "身份证号",
                "11010119900101123" + line,
                "13800138000",
                "北京市",
                "测试学校",
                "高中组",
                category,
                major,
                title,
                null,
                "链接",
                "https://example.test/作品.pdf",
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
