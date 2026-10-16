package com.weiran.cqt.domain.entry;

import java.util.ArrayList;
import java.util.List;

/** 将模板数据行按个人作品或连续 N 位团体成员分组。 */
public final class SchoolImportGroups {

    private SchoolImportGroups() {}

    /**
     * 将连续有效数据行组为作品。团体首行申报 N 人时消费随后 N 条数据行；不完整或字段不一致时产出错误组。
     *
     * @param sourceRows 模板中的非空数据行，保持原顺序
     * @return 含可导入作品或逐组错误的分组结果
     */
    public static List<SchoolImportGroup> assemble(final List<SchoolImportRow> sourceRows) {
        final List<SchoolImportGroup> groups = new ArrayList<>();
        int index = 0;
        while (index < sourceRows.size()) {
            final SchoolImportRow first = sourceRows.get(index);
            if (first.entryType() == SchoolImportEntryType.INDIVIDUAL) {
                groups.add(new SchoolImportGroup(
                        first.sourceRow(),
                        first.sourceRow(),
                        List.of(first),
                        first.validationError() != null
                                ? first.validationError()
                                : first.declaredGroupSize() == 1 ? null : "个人作品申报人数必须为1"));
                index++;
                continue;
            }
            final int count = first.declaredGroupSize();
            if (count < 2) {
                groups.add(new SchoolImportGroup(first.sourceRow(), first.sourceRow(), List.of(first), "团体人数必须至少为2人"));
                index++;
                continue;
            }
            final int end = Math.min(sourceRows.size(), index + count);
            final List<SchoolImportRow> members = List.copyOf(sourceRows.subList(index, end));
            String error = first.validationError();
            if (error == null && end - index != count) {
                error = "团体申报人数为" + count + "人，但后续成员行不足";
            }
            if (error == null) {
                for (int memberIndex = index; memberIndex < end; memberIndex++) {
                    final SchoolImportRow member = sourceRows.get(memberIndex);
                    if (member.validationError() != null) {
                        error = member.validationError();
                        break;
                    }
                    if (member.entryType() != SchoolImportEntryType.TEAM
                            || member.declaredGroupSize() != count
                            || !SchoolImportGroups.same(first.category(), member.category())
                            || !SchoolImportGroups.same(first.major(), member.major())
                            || !SchoolImportGroups.same(first.title(), member.title())) {
                        error = "团体成员行" + member.sourceRow() + "的作品类型、赛项、专业、作品名称或申报人数与首行不一致";
                        break;
                    }
                }
            }
            groups.add(
                    new SchoolImportGroup(first.sourceRow(), members.getLast().sourceRow(), members, error));
            index = end;
        }
        return List.copyOf(groups);
    }

    private static boolean same(final String first, final String other) {
        return SchoolImportGroups.normalize(first).equals(SchoolImportGroups.normalize(other));
    }

    private static String normalize(final String value) {
        return EntryText.normalize(value);
    }
}
