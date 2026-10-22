/**
 * 与后端契约（weiran4j/docs/01-架构与接口契约.md §4、§6）一一对应的类型。
 * 字段名不要自行改动；契约变了先改文档再改这里。
 */

/** 统一响应：code 为数字，0 表示成功 */
export interface ApiResponse<T> {
    code: number;
    message: string;
    data: T;
}

/** 失败响应体（契约 §4）：比成功体多 requestId，与响应头 X-Request-Id 一致；成功体没有该键 */
export type ApiErrorBody = { code: number; message: string; data: null; requestId?: string };

export interface PageResult<T> {
    list: T[];
    total: number;
    page: number;
    pageSize: number;
}

export interface PageQuery {
    page?: number;
    pageSize?: number;
}

export type Status = 'enabled' | 'disabled';
export type Gender = 'male' | 'female' | 'unknown';

/* ---------- 认证 ---------- */

export interface LoginRequest {
    username: string;
    password: string;
}

export interface PhoneLoginCodeRequest {
    phone: string;
}

export interface PhoneLoginRequest {
    phone: string;
    code: string;
}

/**
 * POST /api-backend/n/system/auth/login 的响应（契约 §6.1）。默认模式下令牌只走 HttpOnly Cookie，
 * `accessToken` 为 null 或缺省，前端两种都按「没有」处理、也不读它；`X-Auth-Mode: token` 时才有值（给非浏览器客户端）。
 */
export interface LoginResult {
    accessToken?: string | null;
    tokenType: string;
    expiresIn: number;
    userId: number;
}

export interface CurrentUserView {
    id: number;
    username: string;
    nickname: string;
    email: string | null;
    phone: string | null;
    avatar: string | null;
    gender: Gender | null;
    departmentId: number | null;
    departmentName: string | null;
    roles: string[];
    permissions: string[];
    /** false = 没有本地密码（外部身份自动开通的用户）：密码登录、锁屏校验一律 110101，不能改密码 */
    hasPassword: boolean;
}

export interface ProfileUpdateRequest {
    nickname: string;
    email?: string | null;
    phone?: string | null;
    avatar?: string | null;
    gender?: Gender | null;
}

export interface PasswordChangeRequest {
    oldPassword: string;
    newPassword: string;
}

/** PUT /api-backend/system/auth/updateFavoriteMenus：全量覆盖；重复 id 静默去重保序，去重后 ≤ 50 */
export interface FavoriteMenusSaveRequest {
    menuIds: number[];
}

/** POST /api-backend/system/auth/verifyPassword：锁屏解锁；错误返回 110101，不影响会话 */
export interface VerifyPasswordRequest {
    password: string;
}

/** POST /api-backend/system/auth/logout 的响应：会话来自配置了登出的外部身份提供方时为其登出地址，前端整页跳转；否则 null */
export interface LogoutResult {
    ssoLogoutUrl: string | null;
}

/* ---------- 外部身份（契约 §6.1、§6.2，D-015） ---------- */

export type ProviderType = 'oidc' | 'cas';

export interface ProviderView {
    id: string;
    type: ProviderType;
    name: string;
}

/** GET /api-backend/n/system/auth/listProviders（公开）：只含已启用的提供方 */
export interface ProvidersView {
    passwordLoginEnabled: boolean;
    phoneLoginEnabled?: boolean;
    providers: ProviderView[];
}

/** 已绑定的外部身份：本人 `/api-backend/system/auth/listCurrentIdentities`，管理员 `/api-backend/system/user/listIdentities/{id}` 结构相同 */
export interface UserIdentityView {
    id: number;
    provider: string;
    providerName: string;
    externalId: string;
    displayName: string | null;
    createdAt: string;
}

/** POST /api-backend/system/user/createIdentity/{id} */
export interface BindIdentityRequest {
    provider: string;
    externalId: string;
    displayName?: string;
}

/* ---------- 菜单 ---------- */

export type MenuType = 'directory' | 'menu' | 'button';

export interface MenuNode {
    id: number;
    parentId: number;
    title: string;
    type: MenuType;
    path: string | null;
    component: string | null;
    icon: string | null;
    permission: string | null;
    sort: number;
    visible: boolean;
    keepAlive: boolean;
    isExternal: boolean;
    status: Status;
    children: MenuNode[];
}

export interface MenuSaveRequest {
    parentId: number;
    title: string;
    type: MenuType;
    path?: string | null;
    component?: string | null;
    icon?: string | null;
    permission?: string | null;
    sort?: number;
    visible?: boolean;
    keepAlive?: boolean;
    isExternal?: boolean;
    status?: Status;
}

/* ---------- 用户 ---------- */

export interface UserView {
    id: number;
    username: string;
    nickname: string;
    email: string | null;
    phone: string | null;
    avatar: string | null;
    gender: Gender | null;
    departmentId: number | null;
    departmentName: string | null;
    status: Status;
    isBuiltin: boolean;
    roleIds: number[];
    roleNames: string[];
    lastLoginAt: string | null;
    lastLoginIp: string | null;
    createdAt: string;
    updatedAt: string;
}

/** `GET /api-backend/system/user/list` 查询参数（契约 §6.2）：`keyword` 为三列模糊的快速搜索，其余为高级筛选的单字段条件 */
export interface UserQuery extends PageQuery {
    keyword?: string;
    status?: Status;
    departmentId?: number;
    /** 用户名，精确匹配 */
    username?: string;
    userId?: number;
    /** 手机号，精确匹配 */
    phone?: string;
    /** 邮箱，精确匹配 */
    email?: string;
    /** 拥有该角色 */
    roleId?: number;
    gender?: Gender;
    /** `yyyy-MM-dd HH:mm:ss`，闭区间 */
    createdStartTime?: string;
    createdEndTime?: string;
    /** `yyyy-MM-dd HH:mm:ss`，闭区间；传任一端即排除从未登录的用户 */
    lastLoginStartTime?: string;
    lastLoginEndTime?: string;
}

export interface UserCreateRequest {
    username: string;
    nickname: string;
    password: string;
    email?: string | null;
    phone?: string | null;
    gender?: Gender | null;
    departmentId?: number | null;
    status?: Status;
    roleIds: number[];
}

export type UserUpdateRequest = Omit<UserCreateRequest, 'username' | 'password'>;

/** PUT /api-backend/system/user/updateAccountField/{id}：管理员单字段设置账号标识 */
export type UserAccountField = 'username' | 'phone' | 'email';

export interface UserAccountFieldUpdateRequest {
    field: UserAccountField;
    value: string;
}

export interface UserOption {
    id: number;
    username: string;
    nickname: string;
}

/* ---------- 角色 ---------- */

export interface RoleView {
    id: number;
    name: string;
    code: string;
    description: string | null;
    sort: number;
    status: Status;
    isBuiltin: boolean;
    userCount: number;
    createdAt: string;
    updatedAt: string;
}

export interface RoleDetail extends RoleView {
    menuIds: number[];
}

export interface RoleQuery extends PageQuery {
    keyword?: string;
    status?: Status;
}

export interface RoleSaveRequest {
    name: string;
    code: string;
    description?: string | null;
    sort?: number;
    status?: Status;
}

export interface RoleOption {
    id: number;
    name: string;
    code: string;
}

/* ---------- 部门 ---------- */

export interface DepartmentNode {
    id: number;
    parentId: number;
    name: string;
    code: string;
    leaderId: number | null;
    leaderName: string | null;
    phone: string | null;
    sort: number;
    status: Status;
    createdAt: string;
    children: DepartmentNode[];
}

export interface DepartmentSaveRequest {
    parentId: number;
    name: string;
    code: string;
    leaderId?: number | null;
    phone?: string | null;
    sort?: number;
    status?: Status;
}

/* ---------- 行政区划 ---------- */

export type RegionLevel = 'province' | 'city' | 'county';

export interface RegionView {
    id: number;
    code: string;
    name: string;
    level: RegionLevel;
    parentCode: string | null;
    sort: number;
    status: Status;
    createdAt: string;
    updatedAt: string;
}

export interface RegionNode extends RegionView {
    children: RegionNode[];
}

export interface RegionQuery {
    keyword?: string;
    level?: RegionLevel;
    status?: Status;
}

export interface RegionSaveRequest {
    code: string;
    name: string;
    level: RegionLevel;
    parentCode?: string | null;
    sort?: number;
    status?: Status;
}

/* ---------- 字典 ---------- */

export interface DictView {
    id: number;
    name: string;
    code: string;
    description: string | null;
    status: Status;
    isBuiltin: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface DictQuery extends PageQuery {
    keyword?: string;
    status?: Status;
}

export interface DictSaveRequest {
    name: string;
    code: string;
    description?: string | null;
    status?: Status;
}

export interface DictItemView {
    id: number;
    /** HTTP compatibility field; persisted as sys_dict_item.group_id. */
    dictId: number;
    label: string;
    value: string;
    color: string | null;
    sort: number;
    status: Status;
    remark: string | null;
}

export interface DictItemSaveRequest {
    label: string;
    value: string;
    color?: string | null;
    sort?: number;
    status?: Status;
    remark?: string | null;
}

/* ---------- 系统配置 ---------- */

export type ConfigType = 'string' | 'number' | 'boolean' | 'json';
export type ConfigFieldType = 'input' | 'textarea' | 'number' | 'switch' | 'select' | 'json' | 'file' | 'image' | 'richtext';

export interface ConfigGroupView {
    groupKey: string;
    key: string;
    label: string;
    isBuiltin: boolean;
    canDelete: boolean;
    createdAt: string | null;
}

export interface ConfigGroupSaveRequest {
    groupKey?: 'config_system' | 'config_sms';
    key: string;
    label: string;
}

export interface ConfigGroupUpdateRequest {
    label: string;
}

export interface ConfigView {
    id: number;
    configKey: string;
    configValue: string;
    configType: ConfigType;
    groupKey: string;
    fieldType: ConfigFieldType;
    fieldOptions: string | null;
    required: boolean;
    description: string | null;
    isBuiltin: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface ConfigQuery extends PageQuery {
    keyword?: string;
}

export interface ConfigSaveRequest {
    configKey: string;
    configValue: string;
    configType: ConfigType;
    groupKey: string;
    fieldType: ConfigFieldType;
    fieldOptions?: string | null;
    required: boolean;
    description?: string | null;
}

/* ---------- 系统媒体库 ---------- */

export interface SystemMediaFileUpload {
    url: string;
    name: string;
    path: string;
    size: number;
}

export interface SystemMediaFileView {
    id: number;
    originalName: string;
    url: string;
    size: number;
    createdAt: string;
    category: 'image' | 'document' | 'archive';
    references: string[];
}

export interface SystemMediaFileQuery extends PageQuery {
    keyword?: string;
    category?: 'image' | 'document' | 'archive';
}

/* ---------- 日志 ---------- */

export interface LoginLogView {
    id: number;
    userId: number | null;
    username: string;
    ip: string | null;
    browser: string | null;
    os: string | null;
    userAgent: string | null;
    eventType: 'login' | 'logout';
    status: 'success' | 'fail';
    message: string | null;
    createdAt: string;
}

export interface LoginLogQuery extends PageQuery {
    username?: string;
    status?: 'success' | 'fail';
    eventType?: 'login' | 'logout';
    startTime?: string;
    endTime?: string;
}

/** 契约 §6.9 未逐字段列出，按 §5 sys_operation_log 列的 camelCase 形式 */
export interface OperationLogView {
    id: number;
    userId: number | null;
    username: string | null;
    module: string;
    description: string;
    method: string;
    path: string;
    requestBody?: string | null;
    responseCode: number;
    success: boolean;
    errorMessage: string | null;
    durationMs: number;
    ip: string | null;
    userAgent: string | null;
    createdAt: string;
}

export interface OperationLogQuery extends PageQuery {
    username?: string;
    module?: string;
    success?: boolean;
    startTime?: string;
    endTime?: string;
}

export interface IdResult {
    id: number;
}

// 下游赛事管理契约
export interface SiteConfigAdminItem {
    key: string;
    label: string;
    ident: number;
    contents: string | null;
}

export interface SiteConfigAdminResponse {
    items: SiteConfigAdminItem[];
}

export interface SiteConfigSaveRequest {
    items: Array<Pick<SiteConfigAdminItem, 'key' | 'contents'>>;
}

export interface SiteConfigImageUpload {
    url: string;
    name: string;
    path: string;
    size: number;
}

/* ---------- CQT 大赛动态 ---------- */

export interface CqtNewsView {
    id: number;
    title: string;
    image: string;
    showOnHome: boolean;
    sort: number;
    content: string;
    createdAt: string;
    updatedAt: string;
}

export interface CqtNewsPage {
    list: CqtNewsView[];
    total: number;
    page: number;
    pageSize: number;
}

export interface CqtNewsQuery extends PageQuery {
    keyword?: string;
}

export interface CqtNewsSaveRequest {
    title: string;
    image: string;
    showOnHome: boolean;
    sort: number;
    content: string;
}

export type CqtNewsImageUpload = SiteConfigImageUpload;

/* ---------- 日志 ---------- */


export interface CqtExpertSaveRequest {
    name: string; image: string; content: string; showOnHome: boolean; enabled: boolean; sort: number;
}
export interface CqtExpertView extends CqtExpertSaveRequest { id: number; createdAt: string; updatedAt: string }
export type CqtShowcaseCategory = 'SCRIPT' | 'PERFORMANCE';
export interface CqtShowcaseSaveRequest {
    title: string; image: string; category: CqtShowcaseCategory; author: string; teacher: string; school: string;
    description: string; content: string; attachmentUrl: string; attachmentName: string; videoUrl: string; enabled: boolean; sort: number;
}
export interface CqtShowcaseView extends CqtShowcaseSaveRequest { id: number; createdAt: string; updatedAt: string }
export type CqtResourceCategory = 'SCRIPT_PAPER' | 'DRAMA_MATERIAL';
export interface CqtResourceSaveRequest {
    title: string; category: CqtResourceCategory; fileUrl: string; fileName: string; content: string; enabled: boolean; sort: number;
}
export interface CqtResourceView extends CqtResourceSaveRequest { id: number; createdAt: string; updatedAt: string }
export interface CqtContentQuery extends PageQuery { keyword?: string; category?: string; enabled?: boolean }

/* CQT 评奖（冻结契约） */
export interface CqtAwardRule {
    id: number; revisionId: number; phaseId: number; targetType: 'ENTRY' | 'PARTICIPANT'; firstCategoryId: number; secondCategoryId: number | null;
    groupName: string | null; awardName: string; minScore: number; maxScore: number; enabled: boolean; revision: number; version: number; createdAt: string;
}
export interface CqtAwardRow {
    targetId: number; entryId: number; participantId: number | null; targetType: 'ENTRY' | 'PARTICIPANT'; name: string; title: string;
    score: number | null; finalVersion: number; ruleId: number | null; ruleRevision: number | null; awardName: string | null; status: string; reason: string | null;
}
export interface CqtAwardBatch {
    id: number; phaseId: number; status: string; inputVersion: number; previewId: string; supersedesBatchId: number | null;
    reason: string | null; version: number; createdBy: number; createdAt: string; updatedAt: string; rows: CqtAwardRow[];
}

/* CQT 评审实体与页面投影 */
export interface CqtReviewEntry { id: number; competitionId: number; title: string; source: string; scoringScope: string; stageScope: string; status: string; legacyStatus: number; version: number; firstCategoryId: number; secondCategoryId: number | null; regionId: number | null; groupName: string; attachmentUrl: string; description: string; decision?: string; }
export interface CqtReviewTarget { id: number; entryId: number; participantId: number | null; competitionId: number; source: string; targetType: string; name: string; title: string; firstCategoryId: number; secondCategoryId: number | null; regionId: number | null; groupName: string; stageScope: string; status: string; entryStatus: string; legacyStatus: number; version: number; eligible?: boolean; blockedReason?: string; assignedCount?: number; targetId?: number }
export interface CqtReviewRule { id: number; phaseId: number; revision: number; minScore: number; maxScore: number; minimumReviewers: number; passingScore: number | null }
export interface CqtReviewDimension { id: number; ruleId: number; firstCategoryId: number; name: string; description: string; minScore: number; maxScore: number; sort: number }
export interface CqtReviewPhase { id: number; competitionId: number; code: string; state: string; version: number; inputVersion: number; currentRuleId?: number; rule?: CqtReviewRule }
export interface CqtReviewerScope { id: number; userId: number; competitionId: number; phaseId: number; firstCategoryId: number; secondCategoryId: number | null; regionId: number | null; enabled: boolean; version: number }
export interface CqtReviewAssignment { id: number; phaseId: number; targetId: number; reviewerId: number; state: string; version: number; target?: CqtReviewTarget; title?: string; name?: string; source?: string; targetType?: string; scoreStatus?: string }
export interface CqtReviewFinal { id: number; phaseId: number; targetId: number; finalScore: number | null; averageScore: number | null; validReviewerCount: number; state: string; version: number; snapshotId: number; target?: CqtReviewTarget; decision?: { decision: string; version: number } }
export interface CqtReviewQualification { qualification?: CqtReviewQualification; sourceTarget?: CqtReviewTarget; blockedReason?: string; id: number; sourcePhaseId: number; sourceTargetId: number; finalVersion: number; nationalTargetId: number | null; status: string; version: number; reason: string }
export interface CqtScoreRevision { id: number; scoreId: number; revision: number; total: number | null; comment: string; status: string; ruleId: number; operatorId: number; createdAt: string }
export interface CqtReviewTaskDetail { assignment: CqtReviewAssignment; target: CqtReviewTarget; entry: CqtReviewEntry; rule: CqtReviewRule; dimensions: CqtReviewDimension[]; score: { id: number; version: number; status: string } | null; current: CqtScoreRevision | null; dimensionScores: { dimensionId: number; score: number | null; comment: string }[]; history: CqtScoreRevision[]; canSubmit?: boolean; blockedReason?: string }

export interface CqtReviewOption { id: number; name: string; parentId: number | null }
export interface CqtReviewOptions {
    competitions: CqtReviewOption[]; categories: CqtReviewOption[]; regions: CqtReviewOption[];
    users: { id: number; username: string; nickname: string }[]; phases: CqtReviewPhase[];
}
export interface CqtReviewPreview {
    previewToken: string; expiresAt: string; inputVersion: number; summary: string | Record<string, unknown>;
    rows: Record<string, unknown>[]; blockedRows: Record<string, unknown>[];
}

export type CqtCompetitionMode = 'SHARED' | 'SEPARATE';
export type CqtCompetitionStatus = 0 | 1 | 2;
export interface CqtCompetitionBody {
    name: string; edition: string; year: number; description: string | null; nationalEntryMode: CqtCompetitionMode;
    registrationStart: string | null; registrationEnd: string | null; provinceReviewStart: string | null; provinceReviewEnd: string | null;
    nationalReviewStart: string | null; nationalReviewEnd: string | null; resultPublishTime: string | null;
    provinceCertificateTemplate: string | null; nationalCertificateTemplate: string | null; teacherOrgCertificateTemplate: string | null;
}
export interface CqtCompetitionAdminView extends Omit<CqtCompetitionBody, 'edition' | 'year'> {
    id: number; legacyId: number | null; edition: string | null; year: number | null; status: CqtCompetitionStatus; version: number; createdAt: string | null; updatedAt: string | null;
}
export interface CqtCompetitionQuery { page: number; pageSize: number; keyword?: string; year?: number | undefined; status?: CqtCompetitionStatus | undefined }
