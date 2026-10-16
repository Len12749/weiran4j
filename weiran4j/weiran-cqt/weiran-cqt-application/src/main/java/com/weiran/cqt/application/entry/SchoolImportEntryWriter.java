package com.weiran.cqt.application.entry;

import com.weiran.cqt.domain.account.Account;
import com.weiran.cqt.domain.account.AccountRepository;
import com.weiran.cqt.domain.account.AccountTypes;
import com.weiran.cqt.domain.account.CredentialType;
import com.weiran.cqt.domain.account.Credentials;
import com.weiran.cqt.domain.competition.Category;
import com.weiran.cqt.domain.competition.Competition;
import com.weiran.cqt.domain.competition.CompetitionRepository;
import com.weiran.cqt.domain.competition.EntryStage;
import com.weiran.cqt.domain.competition.Group;
import com.weiran.cqt.domain.entry.Attachment;
import com.weiran.cqt.domain.entry.DuplicateParticipationException;
import com.weiran.cqt.domain.entry.EntryRepository;
import com.weiran.cqt.domain.entry.EntryText;
import com.weiran.cqt.domain.entry.NewEntry;
import com.weiran.cqt.domain.entry.Participant;
import com.weiran.cqt.domain.entry.ParticipationConflict;
import com.weiran.cqt.domain.entry.SchoolImportGroup;
import com.weiran.cqt.domain.entry.SchoolImportRow;
import com.weiran.cqt.domain.entry.SequenceGenerator;
import com.weiran.cqt.domain.file.FileStorage;
import com.weiran.cqt.domain.region.Region;
import com.weiran.cqt.domain.region.RegionRepository;
import com.weiran.framework.error.BizException;
import com.weiran.framework.error.CommonErrors;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;

/** 在独立事务中创建一件学校作品及其参赛成员。 */
public class SchoolImportEntryWriter {

    private static final String ENTRY_SEQUENCE = "fastapi_signup_entry";

    private static final String PERSON_SEQUENCE = "fastapi_signup_person";

    private final AccountRepository accountRepository;

    private final CompetitionRepository competitionRepository;

    private final RegionRepository regionRepository;

    private final EntryRepository entryRepository;

    private final SequenceGenerator sequenceGenerator;

    private final ObjectProvider<FileStorage> storages;

    private final Clock clock;

    /** 构造作品写入用例。 */
    public SchoolImportEntryWriter(
            final AccountRepository accountRepository,
            final CompetitionRepository competitionRepository,
            final RegionRepository regionRepository,
            final EntryRepository entryRepository,
            final SequenceGenerator sequenceGenerator,
            final ObjectProvider<FileStorage> storages,
            final Clock clock) {
        this.accountRepository = accountRepository;
        this.competitionRepository = competitionRepository;
        this.regionRepository = regionRepository;
        this.entryRepository = entryRepository;
        this.sequenceGenerator = sequenceGenerator;
        this.storages = storages;
        this.clock = clock;
    }

    /** 校验并原子写入一件作品；已存在相同指纹时返回原作品 id。 */
    @Transactional
    public long create(
            final long accountId,
            final long competitionId,
            final SchoolImportGroup group,
            final @Nullable String attachmentUrl,
            final @Nullable String attachmentName,
            final String fingerprint) {
        final Long existing =
                this.entryRepository.findImportedEntry(fingerprint).orElse(null);
        if (existing != null) {
            return existing;
        }
        final Account account =
                this.accountRepository.findById(accountId).orElseThrow(() -> BizException.notFound("当前学校账号不存在"));
        if (!Objects.equals(account.getUserType(), AccountTypes.SCHOOL)) {
            throw BizException.badRequest("仅学校账号可导入作品");
        }
        final LocalDateTime now = LocalDateTime.now(this.clock);
        final Competition competition =
                this.competitionRepository.findById(competitionId).orElseThrow(() -> BizException.notFound("赛事不存在"));
        competition.requireOpenForSignup(now);
        final SchoolImportRow first = group.rows().getFirst();
        final String title = EntryText.normalize(first.title());
        if (title.isEmpty()) {
            throw BizException.badRequest("第" + first.sourceRow() + "行作品名称不能为空");
        }
        if (EntryText.normalize(first.category()).isEmpty()) {
            throw BizException.badRequest("第" + first.sourceRow() + "行赛项不能为空");
        }
        if (EntryText.normalize(first.major()).isEmpty()) {
            throw BizException.badRequest("第" + first.sourceRow() + "行专业不能为空");
        }
        final Category category = SchoolImportEntryWriter.resolveCategory(
                this.competitionRepository.findEnabledChildren(0), first.category(), "赛项");
        final Category major = SchoolImportEntryWriter.resolveCategory(
                this.competitionRepository.findEnabledChildren(category.legacyId()), first.major(), "专业");
        final List<Participant> participants = this.participants(group, account, now);
        final Set<String> configuredGroups =
                this.competitionRepository
                        .findEnabledGroups(competitionId, category.legacyId(), major.legacyId())
                        .stream()
                        .map(Group::name)
                        .collect(Collectors.toSet());
        for (int index = 0; index < participants.size(); index++) {
            final Participant participant = participants.get(index);
            if (participant.group().isBlank() || !configuredGroups.contains(participant.group())) {
                throw BizException.badRequest("第" + (index + 1) + "位成员“" + participant.name() + "”的组别未在当前赛事和赛项配置中");
            }
        }
        final FileStorage storage = this.storages.getIfAvailable();
        final Attachment attachment = SchoolImportEntryWriter.attachment(
                first,
                attachmentUrl,
                attachmentName,
                category.attachmentRequired() || major.attachmentRequired(),
                storage);
        final EntryStage stage = competition.portalSignupStage();
        for (int index = 0; index < participants.size(); index++) {
            final Participant participant = participants.get(index);
            final ParticipationConflict conflict = this.entryRepository
                    .findConflict(
                            competition.id(),
                            category.legacyId(),
                            participant.credentialType(),
                            participant.idCard(),
                            stage)
                    .orElse(null);
            if (conflict != null) {
                throw new BizException(
                        CommonErrors.CONFLICT, "第" + (index + 1) + "位成员“" + participant.name() + "”已在本届同一赛项报名");
            }
        }
        final long sequence = this.sequenceGenerator.next(SchoolImportEntryWriter.ENTRY_SEQUENCE);
        final String entryNo = String.format(java.util.Locale.ROOT, "WEB-%d-%010d", competition.id(), sequence);
        final long entryId = this.entryRepository.insertImportedEntry(
                new NewEntry(
                        entryNo,
                        2,
                        sequence,
                        competition.id(),
                        competition.legacyId(),
                        stage,
                        category.legacyId(),
                        major.legacyId(),
                        SchoolImportEntryWriter.regionId(this.regionRepository.findAll(), first.region()),
                        SchoolImportEntryWriter.numeric(account.getSchoolId()),
                        title,
                        SchoolImportEntryWriter.nullable(first.description()),
                        attachment,
                        SchoolImportEntryWriter.teacherNames(first),
                        EntryText.normalize(first.major()),
                        participants.getFirst().group(),
                        group.isTeam(),
                        participants.size(),
                        now),
                fingerprint);
        for (int index = 0; index < participants.size(); index++) {
            final Participant participant = participants.get(index);
            final long personSequence = this.sequenceGenerator.next(SchoolImportEntryWriter.PERSON_SEQUENCE);
            final long participantId = this.entryRepository.insertParticipant(
                    entryId, participant, AccountTypes.SOURCE_ZHONGXI, null, personSequence, index == 0, index, now);
            try {
                this.entryRepository.insertParticipationKey(
                        competition.id(), stage, category.legacyId(), participant, entryId, participantId);
            } catch (final DuplicateParticipationException ex) {
                throw new BizException(CommonErrors.CONFLICT, "“" + participant.name() + "”已在本届同一赛项报名，请勿重复报名");
            }
        }
        this.entryRepository.insertEntryTarget(entryId);
        return entryId;
    }

    private List<Participant> participants(
            final SchoolImportGroup group, final Account account, final LocalDateTime now) {
        final List<Participant> participants = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (final SchoolImportRow row : group.rows()) {
            final String name = EntryText.normalize(row.name());
            final String school = EntryText.normalize(row.school());
            if (name.isEmpty()) {
                throw BizException.badRequest("第" + row.sourceRow() + "行姓名不能为空");
            }
            if (school.isEmpty()) {
                throw BizException.badRequest("第" + row.sourceRow() + "行所在学校不能为空");
            }
            if (!EntryText.normalize(account.getSchool()).equals(school)) {
                throw BizException.badRequest("第" + row.sourceRow() + "行所在学校与当前学校账号不一致");
            }
            final CredentialType type = CredentialType.parse(row.credentialType());
            final String credential = Credentials.requireValid(type, row.credentialNo(), now.toLocalDate());
            if (!seen.add(type.name() + ":" + credential)) {
                throw BizException.badRequest("第" + row.sourceRow() + "行证件号重复");
            }
            String phone = EntryText.normalize(row.phone());
            if (!phone.isEmpty() && !phone.matches("1[3-9]\\d{9}")) {
                throw BizException.badRequest("第" + row.sourceRow() + "行联系方式格式不正确");
            }
            participants.add(new Participant(
                    name,
                    type,
                    credential,
                    phone.isEmpty() ? null : phone,
                    school,
                    null,
                    EntryText.normalize(row.group())));
        }
        if (participants.size() != (group.isTeam() ? group.rows().getFirst().declaredGroupSize() : 1)) {
            throw BizException.badRequest("作品申报人数与成员行数不一致");
        }
        return List.copyOf(participants);
    }

    private static Category resolveCategory(final List<Category> values, final String name, final String label) {
        final List<Category> matches = values.stream()
                .filter(Category::isEnabled)
                .filter(value -> EntryText.normalize(value.name()).equals(EntryText.normalize(name)))
                .toList();
        if (matches.size() != 1) {
            throw BizException.badRequest("第" + label + "“" + name + "”不存在、未启用或名称不唯一");
        }
        return matches.getFirst();
    }

    private static Attachment attachment(
            final SchoolImportRow row,
            final @Nullable String attachmentUrl,
            final @Nullable String attachmentName,
            final boolean required,
            final @Nullable FileStorage storage) {
        if (row.attachmentMode().contains("文件")) {
            if (attachmentUrl == null || attachmentName == null) {
                throw BizException.badRequest("ZIP 中未找到作品附件文件");
            }
            return Attachment.parse(
                    "FILE",
                    attachmentUrl,
                    attachmentName,
                    required,
                    url -> storage != null && storage.isStoredUrl(url));
        }
        return Attachment.parse(row.attachmentMode(), row.attachmentValue(), null, required, url -> false);
    }

    private static @Nullable Long regionId(final List<Region> regions, final String name) {
        final String normalized = EntryText.normalize(name);
        if (normalized.isEmpty()) {
            return null;
        }
        return regions.stream()
                .filter(region -> EntryText.normalize(region.name()).equals(normalized))
                .map(Region::legacyId)
                .findFirst()
                .orElse(null);
    }

    private static @Nullable Long numeric(final @Nullable String value) {
        if (value == null
                || value.isBlank()
                || value.length() >= 19
                || !value.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return Long.valueOf(value);
    }

    private static @Nullable String nullable(final @Nullable String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static @Nullable String teacherNames(final SchoolImportRow row) {
        final String first = EntryText.normalize(row.teacher1());
        final String second = EntryText.normalize(row.teacher2());
        final String names = String.join(
                "、",
                List.of(first, second).stream()
                        .filter(value -> !value.isBlank())
                        .toList());
        return names.isBlank() ? null : names;
    }
}
