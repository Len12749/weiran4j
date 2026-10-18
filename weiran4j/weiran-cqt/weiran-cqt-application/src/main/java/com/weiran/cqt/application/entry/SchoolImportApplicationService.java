package com.weiran.cqt.application.entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiran.cqt.api.entry.SchoolImportService;
import com.weiran.cqt.api.entry.SchoolImportTaskView;
import com.weiran.cqt.domain.account.Account;
import com.weiran.cqt.domain.account.AccountRepository;
import com.weiran.cqt.domain.account.AccountTypes;
import com.weiran.cqt.domain.competition.Competition;
import com.weiran.cqt.domain.competition.CompetitionRepository;
import com.weiran.cqt.domain.entry.EntryRepository;
import com.weiran.cqt.domain.entry.MyEntryRow;
import com.weiran.cqt.domain.entry.SchoolImportGroup;
import com.weiran.cqt.domain.entry.SchoolImportGroups;
import com.weiran.cqt.domain.entry.SchoolImportRow;
import com.weiran.cqt.domain.entry.SchoolImportRowResult;
import com.weiran.cqt.domain.entry.SchoolImportTask;
import com.weiran.cqt.domain.entry.SchoolImportTaskRepository;
import com.weiran.cqt.domain.entry.SchoolImportTaskStatus;
import com.weiran.cqt.domain.file.FileStorage;
import com.weiran.cqt.domain.file.UploadedFiles;
import com.weiran.framework.error.BizException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;

/** 学校作品导入、任务读取与异步处理用例。 */
@Slf4j
public class SchoolImportApplicationService implements SchoolImportService {

    private static final String NO_ZIP_SESSION = "3session_id";

    private static final TypeReference<Map<String, Object>> DISPLAY = new TypeReference<>() {};

    private final AccountRepository accountRepository;

    private final CompetitionRepository competitionRepository;

    private final EntryRepository entryRepository;

    private final SchoolImportTaskRepository taskRepository;

    private final SchoolImportEntryWriter entryWriter;

    private final ObjectProvider<FileStorage> storages;

    private final ObjectMapper objectMapper;

    private final Clock clock;

    private final String keyPrefix;

    private final SchoolImportSpreadsheetParser spreadsheetParser = new SchoolImportSpreadsheetParser();

    /** 构造学校导入服务。 */
    public SchoolImportApplicationService(
            final AccountRepository accountRepository,
            final CompetitionRepository competitionRepository,
            final EntryRepository entryRepository,
            final SchoolImportTaskRepository taskRepository,
            final SchoolImportEntryWriter entryWriter,
            final ObjectProvider<FileStorage> storages,
            final ObjectMapper objectMapper,
            final Clock clock,
            @Value("${weiran.cqt.storage.key-prefix:cqt/}") final String keyPrefix) {
        this.accountRepository = accountRepository;
        this.competitionRepository = competitionRepository;
        this.entryRepository = entryRepository;
        this.taskRepository = taskRepository;
        this.entryWriter = entryWriter;
        this.storages = storages;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public byte[] templateWorkbook() {
        try {
            return SchoolImportSpreadsheetParser.blankTemplate();
        } catch (final IOException ex) {
            throw new IllegalStateException("报名模板生成失败", ex);
        }
    }

    @Override
    public boolean checkSignup(final long accountId, final @Nullable Long competitionId) {
        this.requireSchool(accountId);
        final LocalDateTime now = LocalDateTime.now(this.clock);
        this.taskRepository.expireUploading(now);
        return this.taskRepository.hasPending(accountId, competitionId, now);
    }

    @Override
    public List<Map<String, @Nullable Object>> schoolProducts(
            final long accountId, final @Nullable Long competitionId) {
        this.requireSchool(accountId);
        final List<Map<String, @Nullable Object>> result = new ArrayList<>();
        for (final MyEntryRow row : this.entryRepository.findBySchoolImport(accountId, competitionId)) {
            final Map<String, @Nullable Object> value = new LinkedHashMap<>();
            value.put("productid", row.entryId());
            value.put("id", row.competitionId());
            value.put("title", row.title());
            value.put("status", row.status());
            value.put("legacy_status", row.legacyStatus());
            value.put("firstcatid", row.firstCategoryLegacyId());
            value.put("secondcatid", row.secondCategoryLegacyId());
            value.put("regionsid", row.regionId());
            value.put("purl", row.attachmentUrl());
            value.put("teachername", row.teacherNames());
            value.put("created_at", row.createdAt());
            value.put("source_database", row.sourceDatabase());
            value.put("competition_name", row.competitionName());
            value.put("competition_id", row.competitionId());
            value.put("teantype", row.team() ? 1 : 0);
            value.put("username", row.leaderName());
            value.put("phone", row.leaderPhone());
            result.add(value);
        }
        return List.copyOf(result);
    }

    @Override
    public String registerZip(final long accountId, final String url, final @Nullable String name) {
        final Account account = this.requireSchool(accountId);
        final String storedName = name == null ? url.substring(url.lastIndexOf('/') + 1) : name;
        if (!UploadedFiles.extensionOf(storedName).filter(".zip"::equals).isPresent()) {
            throw BizException.badRequest("作品压缩包必须是.zip文件");
        }
        this.validateOwnedObject(url, accountId);
        final LocalDateTime now = LocalDateTime.now(this.clock);
        final String id = UUID.randomUUID().toString();
        this.taskRepository.create(new SchoolImportTask(
                id,
                accountId,
                account.getSchoolId(),
                null,
                url,
                storedName,
                null,
                null,
                null,
                SchoolImportTaskStatus.UPLOADING,
                0,
                0,
                0,
                0,
                null,
                now.plusHours(2),
                0,
                now,
                now,
                null));
        return id;
    }

    @Override
    public String registerExcel(
            final long accountId, final String sessionId, final String url, final @Nullable String name) {
        final Account account = this.requireSchool(accountId);
        final String storedName = name == null ? SchoolImportApplicationService.filenameFromUrl(url) : name;
        if (UploadedFiles.extensionOf(storedName)
                .filter(extension -> extension.equals(".xls") || extension.equals(".xlsx"))
                .isEmpty()) {
            throw BizException.badRequest("报名表必须是.xls或.xlsx文件");
        }
        this.validateOwnedObject(url, accountId);
        final LocalDateTime now = LocalDateTime.now(this.clock);
        if (SchoolImportApplicationService.NO_ZIP_SESSION.equals(sessionId)) {
            final String id = UUID.randomUUID().toString();
            this.taskRepository.create(new SchoolImportTask(
                    id,
                    accountId,
                    account.getSchoolId(),
                    null,
                    null,
                    null,
                    url,
                    storedName,
                    null,
                    SchoolImportTaskStatus.UPLOADING,
                    0,
                    0,
                    0,
                    0,
                    null,
                    now.plusHours(2),
                    0,
                    now,
                    now,
                    null));
            return id;
        }
        if (this.taskRepository.findOwned(sessionId, accountId).isEmpty()
                || !this.taskRepository.updateUpload(sessionId, accountId, null, null, url, storedName, now)) {
            throw BizException.badRequest("上传会话不存在、已失效或不属于当前学校");
        }
        return sessionId;
    }

    @Override
    public String confirm(
            final long accountId,
            final long competitionId,
            final String sessionId,
            final String excelId,
            final @Nullable String importType,
            final @Nullable String zipName,
            final @Nullable String excelName) {
        this.requireSchool(accountId);
        final String id = SchoolImportApplicationService.NO_ZIP_SESSION.equals(sessionId) ? excelId : sessionId;
        final SchoolImportTask task =
                this.taskRepository.findOwned(id, accountId).orElseThrow(() -> BizException.notFound("上传会话不存在"));
        if (!SchoolImportApplicationService.NO_ZIP_SESSION.equals(sessionId) && !id.equals(excelId)) {
            throw BizException.badRequest("Excel 与 ZIP 上传会话不匹配");
        }
        final Competition competition =
                this.competitionRepository.findById(competitionId).orElseThrow(() -> BizException.notFound("赛事不存在"));
        competition.requireOpenForSignup(LocalDateTime.now(this.clock));
        if (task.status() != SchoolImportTaskStatus.UPLOADING) {
            if (Objects.equals(task.competitionId(), competitionId)) {
                return task.id();
            }
            throw BizException.badRequest("该上传会话已用于其它赛事");
        }
        if (task.excelUrl() == null) {
            throw BizException.badRequest("请先上传报名Excel文件");
        }
        final LocalDateTime now = LocalDateTime.now(this.clock);
        this.taskRepository.updateUpload(
                id, accountId, null, zipName, task.excelUrl(), excelName == null ? task.excelName() : excelName, now);
        if (!this.taskRepository.queue(id, accountId, competitionId, importType == null ? "" : importType, now)) {
            throw BizException.badRequest("上传会话不能提交，请重新上传文件");
        }
        return id;
    }

    @Override
    public List<SchoolImportTaskView> tasks(final long accountId, final long competitionId) {
        this.requireSchool(accountId);
        return this.taskRepository.listOwned(accountId, competitionId).stream()
                .map(task -> this.view(task, List.of()))
                .toList();
    }

    @Override
    public SchoolImportTaskView detail(final long accountId, final long competitionId, final String taskId) {
        this.requireSchool(accountId);
        final SchoolImportTask task = this.taskRepository
                .findOwned(taskId, accountId)
                .filter(value -> Objects.equals(value.competitionId(), competitionId))
                .orElseThrow(() -> BizException.notFound("导入记录不存在"));
        return this.view(
                task,
                this.taskRepository.rows(taskId).stream().map(this::resultView).toList());
    }

    @Override
    public SchoolImportTaskView summary(final long accountId, final String taskId) {
        return this.view(this.ownedTask(accountId, taskId), List.of());
    }

    @Override
    public SchoolImportTaskView detailById(final long accountId, final String taskId) {
        final SchoolImportTask task = this.ownedTask(accountId, taskId);
        return this.view(
                task,
                this.taskRepository.rows(task.id()).stream()
                        .map(this::resultView)
                        .toList());
    }

    private SchoolImportTask ownedTask(final long accountId, final String taskId) {
        this.requireSchool(accountId);
        return this.taskRepository.findOwned(taskId, accountId).orElseThrow(() -> BizException.notFound("导入记录不存在"));
    }

    /** 领取并处理一个可恢复任务；由定时 worker 调用。 */
    public boolean processNext() {
        final LocalDateTime now = LocalDateTime.now(this.clock);
        this.taskRepository.expireUploading(now);
        final SchoolImportTask task = this.taskRepository
                .claimNext(now, now.plus(Duration.ofMinutes(15)))
                .orElse(null);
        if (task == null) {
            return false;
        }
        try {
            this.process(task);
        } catch (final RuntimeException ex) {
            SchoolImportApplicationService.log.warn(
                    "学校导入任务暂时失败 task={} attempt={} error={}",
                    task.id(),
                    task.attemptCount(),
                    ex.getClass().getSimpleName());
            if (task.attemptCount() >= 5) {
                this.recordTaskFailure(task, "导入任务多次重试仍失败，请检查上传文件后重新提交");
            }
        }
        return true;
    }

    private void process(final SchoolImportTask task) {
        if (task.competitionId() == null || task.excelUrl() == null) {
            this.recordTaskFailure(task, "导入任务缺少赛事或Excel文件");
            return;
        }
        final FileStorage storage = this.storages.getIfAvailable();
        if (storage == null) {
            throw BizException.badRequest("文件存储未配置");
        }
        final List<SchoolImportRow> parsedRows;
        try (InputStream input = storage.openOwnedObject(task.excelUrl(), task.accountId())) {
            parsedRows = this.spreadsheetParser.parse(input);
        } catch (final IOException | IllegalArgumentException ex) {
            this.recordTaskFailure(task, "报名Excel文件无法解析，请使用下载的模板检查文件格式");
            return;
        }
        if (parsedRows.isEmpty()) {
            this.recordTaskFailure(task, "报名表没有可导入的作品行");
            return;
        }
        final List<SchoolImportGroup> groups = SchoolImportGroups.assemble(parsedRows);
        final SchoolImportArchiveIndex archive;
        if (task.zipUrl() == null) {
            archive = null;
        } else {
            try (InputStream input = storage.openOwnedObject(Objects.requireNonNull(task.zipUrl()), task.accountId())) {
                archive = SchoolImportArchiveIndex.scan(input);
            } catch (final IOException | IllegalArgumentException ex) {
                this.recordTaskFailure(task, "作品ZIP文件损坏或包含不安全路径/超限内容");
                return;
            }
        }
        final Map<Integer, String> attachmentErrors = new HashMap<>();
        final Set<String> requestedAttachments = new HashSet<>();
        for (final SchoolImportGroup group : groups) {
            final SchoolImportRow first = group.rows().getFirst();
            if (!first.attachmentMode().contains("文件")) {
                continue;
            }
            if (archive == null) {
                attachmentErrors.put(group.firstSourceRow(), "附件为文件时必须上传作品 ZIP 包");
                continue;
            }
            final String matchError = archive.matchError(first.attachmentValue());
            if (matchError != null) {
                attachmentErrors.put(group.firstSourceRow(), matchError);
                continue;
            }
            if (UploadedFiles.extensionOf(first.attachmentValue()).isEmpty()) {
                attachmentErrors.put(group.firstSourceRow(), "ZIP 中的附件文件类型不受支持");
                continue;
            }
            requestedAttachments.add(SchoolImportArchiveIndex.fileKey(first.attachmentValue()));
        }
        final Map<String, SchoolImportArchiveIndex.ImportedAttachment> extractedAttachments;
        if (archive == null || requestedAttachments.isEmpty()) {
            extractedAttachments = Map.of();
        } else {
            try (InputStream input = storage.openOwnedObject(Objects.requireNonNull(task.zipUrl()), task.accountId())) {
                extractedAttachments = SchoolImportArchiveIndex.extractRequested(
                        input, requestedAttachments, storage, task.accountId(), this.keyPrefix, this.clock);
            } catch (final IOException | IllegalArgumentException ex) {
                this.recordTaskFailure(task, "作品 ZIP 附件无法安全提取，请检查压缩包后重新上传");
                return;
            }
        }
        final LocalDateTime now = LocalDateTime.now(this.clock);
        final List<SchoolImportRowResult> initialRows = new ArrayList<>();
        for (final SchoolImportGroup group : groups) {
            final String error =
                    group.errorMessage() == null ? attachmentErrors.get(group.firstSourceRow()) : group.errorMessage();
            for (final SchoolImportRow row : group.rows()) {
                initialRows.add(new SchoolImportRowResult(
                        0,
                        task.id(),
                        row.sourceRow(),
                        group.lastSourceRow(),
                        null,
                        null,
                        error == null ? "PENDING" : "FAILED",
                        this.displayJson(row, extractedAttachments),
                        error));
            }
        }
        this.taskRepository.insertRows(task.id(), initialRows, parsedRows.size(), now);
        final Map<Integer, SchoolImportRowResult> outcomesBySourceRow = new HashMap<>();
        for (final SchoolImportRowResult result : this.taskRepository.rows(task.id())) {
            outcomesBySourceRow.put(result.sourceRow(), result);
        }
        for (final SchoolImportGroup group : groups) {
            final SchoolImportRowResult firstResult = outcomesBySourceRow.get(group.firstSourceRow());
            if (firstResult == null || !"PENDING".equals(firstResult.status())) {
                continue;
            }
            final long entryId;
            final String fingerprint = SchoolImportApplicationService.fingerprint(task, group);
            try {
                final SchoolImportRow first = group.rows().getFirst();
                final String attachmentError = attachmentErrors.get(group.firstSourceRow());
                if (attachmentError != null) {
                    throw BizException.badRequest(attachmentError);
                }
                final SchoolImportArchiveIndex.ImportedAttachment attachment =
                        first.attachmentMode().contains("文件")
                                ? extractedAttachments.get(SchoolImportArchiveIndex.fileKey(first.attachmentValue()))
                                : null;
                if (first.attachmentMode().contains("文件") && attachment == null) {
                    throw BizException.badRequest("ZIP 中未找到作品附件文件");
                }
                entryId = this.entryWriter.create(
                        task.accountId(),
                        task.competitionId(),
                        group,
                        attachment == null ? first.attachmentValue() : attachment.url(),
                        attachment == null ? null : attachment.name(),
                        fingerprint);
            } catch (final BizException ex) {
                if (ex.getErrorCode().httpStatus() >= 500) {
                    throw ex;
                }
                this.taskRepository.markFailure(
                        task.id(),
                        group.firstSourceRow(),
                        group.lastSourceRow(),
                        SchoolImportApplicationService.safeMessage(ex),
                        LocalDateTime.now(this.clock));
                continue;
            }
            this.taskRepository.markSuccess(
                    task.id(),
                    group.firstSourceRow(),
                    group.lastSourceRow(),
                    fingerprint,
                    entryId,
                    LocalDateTime.now(this.clock));
        }
        final List<SchoolImportRowResult> outcomes = this.taskRepository.rows(task.id());
        if (outcomes.stream().anyMatch(row -> "PENDING".equals(row.status()))) {
            throw new IllegalStateException("导入源行尚未全部处理");
        }
        final long failures =
                outcomes.stream().filter(row -> "FAILED".equals(row.status())).count();
        final long successes =
                outcomes.stream().filter(row -> "SUCCESS".equals(row.status())).count();
        final SchoolImportTaskStatus status = failures == 0
                ? SchoolImportTaskStatus.COMPLETED
                : successes == 0 ? SchoolImportTaskStatus.FAILED : SchoolImportTaskStatus.PARTIAL;
        this.taskRepository.finish(task.id(), status, LocalDateTime.now(this.clock));
    }

    private void recordTaskFailure(final SchoolImportTask task, final String message) {
        final LocalDateTime now = LocalDateTime.now(this.clock);
        // Preserve known source rows; a task failure is not a fabricated Excel row.
        for (final SchoolImportRowResult row : this.taskRepository.rows(task.id())) {
            if ("PENDING".equals(row.status())) {
                this.taskRepository.markFailure(task.id(), row.sourceRow(), row.sourceRow(), message, now);
            }
        }
        final boolean hasSuccess =
                this.taskRepository.rows(task.id()).stream().anyMatch(row -> "SUCCESS".equals(row.status()));
        this.taskRepository.fail(
                task.id(), hasSuccess ? SchoolImportTaskStatus.PARTIAL : SchoolImportTaskStatus.FAILED, message, now);
    }

    private SchoolImportTaskView view(final SchoolImportTask task, final List<Map<String, @Nullable Object>> results) {
        return new SchoolImportTaskView(
                task.id(),
                Objects.requireNonNullElse(task.competitionId(), 0L),
                task.status().name(),
                task.zipUrl(),
                task.zipName(),
                task.excelUrl(),
                task.excelName(),
                task.totalRows(),
                task.processedRows(),
                task.successCount(),
                task.failureCount(),
                task.createdAt(),
                task.updatedAt(),
                results,
                task.errorMessage());
    }

    private Map<String, @Nullable Object> resultView(final SchoolImportRowResult result) {
        final Map<String, @Nullable Object> value = this.readDisplay(result.displayJson());
        value.put("row_number", result.sourceRow());
        value.put(
                "status",
                switch (result.status()) {
                    case "SUCCESS" -> 1;
                    case "FAILED" -> 2;
                    default -> 0;
                });
        value.put("error", result.errorMessage());
        value.put("error_msg", result.errorMessage());
        value.put("product_id", result.entryId());
        value.put("col_a", result.sourceRow());
        value.put("col_o", value.get("attachment"));
        value.put("col_s", value.get("attachment_url"));
        return value;
    }

    private Map<String, @Nullable Object> readDisplay(final @Nullable String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return new LinkedHashMap<>(this.objectMapper.readValue(json, SchoolImportApplicationService.DISPLAY));
        } catch (final JsonProcessingException ex) {
            return new LinkedHashMap<>();
        }
    }

    private String displayJson(
            final SchoolImportRow row, final Map<String, SchoolImportArchiveIndex.ImportedAttachment> attachments) {
        final SchoolImportArchiveIndex.ImportedAttachment extracted =
                row.attachmentMode().contains("文件")
                        ? attachments.get(SchoolImportArchiveIndex.fileKey(row.attachmentValue()))
                        : null;
        final String attachmentName =
                extracted == null ? SchoolImportApplicationService.baseName(row.attachmentValue()) : extracted.name();
        final List<Object> rowData = new ArrayList<>();
        rowData.add(row.sourceRow());
        rowData.add(row.entryType() == com.weiran.cqt.domain.entry.SchoolImportEntryType.TEAM ? "团体" : "个人");
        rowData.add(row.declaredGroupSize());
        rowData.add(row.name());
        rowData.add(row.credentialNo());
        rowData.add(row.region());
        rowData.add(row.school());
        rowData.add(row.phone());
        rowData.add(row.group());
        rowData.add(row.category());
        rowData.add(row.major());
        rowData.add(row.title());
        rowData.add(row.description());
        rowData.add(row.attachmentMode());
        rowData.add(attachmentName);
        rowData.add(row.teacher1());
        rowData.add(row.teacher1CredentialNo());
        rowData.add(row.teacher2());
        rowData.add(row.teacher2CredentialNo());
        rowData.add(row.channel());
        final Map<String, Object> display = new LinkedHashMap<>();
        display.put("row_data", rowData);
        display.put("row_number", row.sourceRow());
        display.put("col_b", row.entryType() == com.weiran.cqt.domain.entry.SchoolImportEntryType.TEAM ? "团体" : "个人");
        display.put("col_d", row.name());
        display.put("col_f", row.credentialNo());
        display.put("col_g", row.phone());
        display.put("col_o", attachmentName);
        display.put("attachment", attachmentName);
        display.put("attachment_url", extracted == null ? row.attachmentValue() : extracted.url());
        return this.json(display);
    }

    private static String baseName(final String value) {
        final String normalized = value.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private String json(final Map<String, ?> value) {
        try {
            return this.objectMapper.writeValueAsString(value);
        } catch (final JsonProcessingException ex) {
            throw new IllegalStateException("导入结果序列化失败", ex);
        }
    }

    private void validateOwnedObject(final String url, final long accountId) {
        final FileStorage storage = this.storages.getIfAvailable();
        if (storage == null) {
            throw BizException.badRequest("文件存储未配置");
        }
        try (InputStream input = storage.openOwnedObject(url, accountId)) {
            // 打开并关闭输入流以验证 URL、归属与对象存在，不读取大文件。
            input.readNBytes(0);
        } catch (final IOException ex) {
            throw BizException.badRequest("上传文件不可读取");
        }
    }

    private Account requireSchool(final long accountId) {
        final Account account =
                this.accountRepository.findById(accountId).orElseThrow(() -> BizException.notFound("当前用户不存在"));
        if (!Objects.equals(account.getUserType(), AccountTypes.SCHOOL)) {
            throw BizException.badRequest("仅学校账号可使用批量导入");
        }
        return account;
    }

    private static String fingerprint(final SchoolImportTask task, final SchoolImportGroup group) {
        try {
            final byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest((task.id() + ":" + task.competitionId() + ":" + group.firstSourceRow())
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (final java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    private static String safeMessage(final RuntimeException ex) {
        final String message = ex instanceof BizException ? ex.getMessage() : null;
        if (message == null || message.isBlank()) {
            return "作品行校验或写入失败";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private static String filenameFromUrl(final String url) {
        final String path = url.split("[?#]", 2)[0];
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
