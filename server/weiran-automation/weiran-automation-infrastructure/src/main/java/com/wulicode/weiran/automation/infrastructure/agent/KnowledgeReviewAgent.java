package com.wulicode.weiran.automation.infrastructure.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wulicode.weiran.automation.domain.task.ChangeRequestSummary;
import com.wulicode.weiran.automation.domain.task.ChangedFile;
import com.wulicode.weiran.automation.domain.task.ReviewAgent;
import com.wulicode.weiran.automation.domain.task.ReviewDraft;
import com.wulicode.weiran.automation.domain.task.ReviewFileDraft;
import com.wulicode.weiran.automation.domain.task.ReviewFileLevel;
import com.wulicode.weiran.automation.infrastructure.agent.OpenAiCompatibleClient.ChatResult;
import com.wulicode.weiran.automation.infrastructure.agent.OpenAiCompatibleClient.ToolCall;
import com.wulicode.weiran.automation.infrastructure.autoconfigure.AutomationProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 代码评审 Agent（ReAct）。
 *
 * <p>模型拿到「本次变更 diff + 任务目标」后，可以自己决定去读文件、搜调用链、看 git 历史、查知识库规范，
 * 循环几轮再给出结论。工具都是只读的，并且限制在本次同步下来的仓库目录内。
 */
public class KnowledgeReviewAgent implements ReviewAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeReviewAgent.class);

    private static final String SYSTEM_PROMPT = """
            你是研发团队的代码评审助手，可以调用工具查看当前代码仓库、git 历史与团队知识库。

            工作顺序：
            1. 先了解这个代码库的技术栈与分层，必要时看 git 历史确认改动背景。
            2. 去知识库检索这个技术栈的开发规范。
            3. 看本次变更的 diff，只挑下面几类问题：
               - 会导致报错或运行失败：参数不匹配、方法/类/属性不存在、空集合取键、异常未处理、非法状态码；
               - 会导致数据错误或丢失：事务边界、时区、截断、并发写；
               - 会导致安全问题：越权、签名或密钥缺失、注入；
               - 明显违反团队硬性规范。
               不要报：命名、注释、格式、重构建议、边界情况、"建议确认"、你已经排除掉的怀疑。
            4. 最多列 10 条，按严重程度排序；每条一句话，说清"问题 + 会导致什么"。
               输出 JSON：{"findings":[{"path":"文件路径","comment":"..."}]}
               没有就输出 {"findings":[]}；只输出 JSON，不要写分析过程和客套话。

            工具：read_file 读文件、grep 搜代码、list_files 看目录、git_log / git_show 看历史、search_knowledge 查规范。
            """;

    /** 轮数不设限，这里只是防止模型无限循环的极端保护。 */
    private static final int MAX_ROUNDS = 200;

    /** 评论里最多列出的问题数。 */
    private static final int MAX_ISSUES = 10;

    private static final int MAX_COMMENT_CHARS = 20_000;

    private static final int MAX_DIFF_CHARS_PER_FILE = 6000;

    private final McpKnowledgeClient knowledge;

    private final OpenAiCompatibleClient llm;

    private final AutomationProperties properties;

    private final ObjectMapper mapper;

    private final ExecutorService toolExecutor;

    /** 构造评审 Agent。 */
    public KnowledgeReviewAgent(
            final McpKnowledgeClient knowledge,
            final OpenAiCompatibleClient llm,
            final AutomationProperties properties,
            final ObjectMapper mapper,
            final ExecutorService toolExecutor) {
        this.knowledge = knowledge;
        this.llm = llm;
        this.properties = properties;
        this.mapper = mapper;
        this.toolExecutor = toolExecutor;
    }

    @Override
    public ReviewDraft review(
            final ChangeRequestSummary summary, final List<ChangedFile> files, final Path repositoryPath) {
        final RepositoryToolbox toolbox = new RepositoryToolbox(repositoryPath);
        final List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", this.systemPrompt()));
        messages.add(Map.of("role", "user", "content", this.buildInitialPrompt(summary, files)));
        final List<Map<String, Object>> tools = KnowledgeReviewAgent.toolDefinitions();
        int round = 0;
        while (true) {
            round++;
            final ChatResult result = this.llm.chatWithTools(messages, tools);
            if (result.toolCalls().isEmpty()) {
                return this.parse(result.content());
            }
            if (round > KnowledgeReviewAgent.MAX_ROUNDS) {
                LOGGER.warn("Review agent exceeded {} rounds, forcing conclusion", KnowledgeReviewAgent.MAX_ROUNDS);
                messages.add(Map.of("role", "user", "content", "请直接输出最终 JSON 结论，不要再调用工具。"));
                final ChatResult last = this.llm.chatWithTools(messages, List.of(), true);
                return this.parse(last.content());
            }
            messages.add(KnowledgeReviewAgent.assistantMessage(result));
            // 同一轮的多个工具并发执行，结果按原顺序回填
            final List<ToolCall> calls = result.toolCalls();
            final List<CompletableFuture<String>> futures = new ArrayList<>();
            for (final ToolCall call : calls) {
                futures.add(CompletableFuture.supplyAsync(() -> this.executeTool(call, toolbox), this.toolExecutor));
            }
            for (int index = 0; index < calls.size(); index++) {
                final ToolCall call = calls.get(index);
                final String output = futures.get(index).join();
                LOGGER.debug(
                        "Review agent tool {} args={} returned {} chars",
                        call.name(),
                        call.arguments(),
                        output.length());
                messages.add(Map.of("role", "tool", "tool_call_id", call.id(), "content", output));
            }
        }
    }

    /** 系统提示词：优先用配置（便于不改代码调整），留空回落到内置默认。 */
    private String systemPrompt() {
        final String configured = this.properties.review().systemPrompt();
        return configured == null || configured.isBlank() ? KnowledgeReviewAgent.SYSTEM_PROMPT : configured;
    }

    private String buildInitialPrompt(final ChangeRequestSummary summary, final List<ChangedFile> files) {
        final StringBuilder prompt = new StringBuilder()
                .append("合并请求：")
                .append(summary.title())
                .append('\n')
                .append("分支：")
                .append(summary.sourceBranch())
                .append(" → ")
                .append(summary.targetBranch())
                .append('\n');
        if (summary.webUrl() != null) {
            prompt.append("链接：").append(summary.webUrl()).append('\n');
        }
        prompt.append("变更文件：").append(files.size()).append(" 个\n");
        for (final ChangedFile file : files) {
            prompt.append("\n### ")
                    .append(file.path())
                    .append("  (+")
                    .append(file.addLines())
                    .append(" -")
                    .append(file.delLines())
                    .append(")\n```diff\n")
                    .append(KnowledgeReviewAgent.clip(file.diff(), KnowledgeReviewAgent.MAX_DIFF_CHARS_PER_FILE))
                    .append("\n```\n");
        }
        prompt.append("\n请按任务要求审查；需要核实上下文时调用工具，最后输出 JSON。");
        return prompt.toString();
    }

    /** 执行一次工具调用；任何异常都转成文本结果，避免中断循环。 */
    private String executeTool(final ToolCall call, final RepositoryToolbox toolbox) {
        try {
            final JsonNode args = this.mapper.readTree(call.arguments().isBlank() ? "{}" : call.arguments());
            return switch (call.name()) {
                case "read_file" -> toolbox.readFile(args.path("path").asText(""));
                case "grep" ->
                    toolbox.grep(args.path("pattern").asText(""), KnowledgeReviewAgent.textOrNull(args.path("path")));
                case "list_files" -> toolbox.listFiles(KnowledgeReviewAgent.textOrNull(args.path("path")));
                case "git_log" ->
                    toolbox.gitLog(
                            KnowledgeReviewAgent.textOrNull(args.path("path")),
                            args.path("limit").asInt(20));
                case "git_show" -> toolbox.gitShow(args.path("sha").asText(""));
                case "search_knowledge" ->
                    this.knowledge.search(
                            args.path("query").asText(""),
                            args.path("limit")
                                    .asInt(Math.max(this.properties.review().retrieveLimit(), 1)));
                default -> "未知工具：" + call.name();
            };
        } catch (final IOException | RuntimeException ex) {
            return "工具执行失败：" + ex.getMessage();
        }
    }

    private static Map<String, Object> assistantMessage(final ChatResult result) {
        final List<Map<String, Object>> calls = result.toolCalls().stream()
                .map(call -> Map.of(
                        "id",
                        call.id(),
                        "type",
                        "function",
                        "function",
                        Map.of("name", call.name(), "arguments", call.arguments())))
                .toList();
        final Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant");
        message.put("content", result.content());
        message.put("tool_calls", calls);
        return message;
    }

    private ReviewDraft parse(final @Nullable String answer) {
        LOGGER.debug("Review agent final answer: {}", KnowledgeReviewAgent.clip(answer, 800));
        final String json = KnowledgeReviewAgent.extractJson(answer);
        try {
            final JsonNode root = this.mapper.readTree(json);
            final List<ReviewFileDraft> drafts = new ArrayList<>();
            for (final JsonNode item : root.path("findings")) {
                final String path = item.path("path").asText("").strip();
                if (path.isEmpty()) {
                    continue;
                }
                drafts.add(new ReviewFileDraft(
                        path, ReviewFileLevel.ISSUE, KnowledgeReviewAgent.textOrNull(item.path("comment")), null));
            }
            final List<ReviewFileDraft> issues =
                    drafts.stream().limit(KnowledgeReviewAgent.MAX_ISSUES).toList();
            return new ReviewDraft(
                    KnowledgeReviewAgent.buildComment(issues),
                    this.properties.review().llmModel(),
                    issues);
        } catch (final IOException ex) {
            LOGGER.warn("Model answer is not valid JSON, salvaging findings: {}", ex.getMessage());
            final List<ReviewFileDraft> salvaged = KnowledgeReviewAgent.salvage(answer);
            if (salvaged.isEmpty()) {
                return new ReviewDraft(
                        KnowledgeReviewAgent.clip(answer == null ? "" : answer, 500),
                        this.properties.review().llmModel(),
                        List.of());
            }
            return new ReviewDraft(
                    KnowledgeReviewAgent.buildComment(salvaged),
                    this.properties.review().llmModel(),
                    salvaged);
        }
    }

    /** JSON 被截断时，尽量把已经完整的 path/comment 对捞出来，避免把原始 JSON 当评论。 */
    private static List<ReviewFileDraft> salvage(final @Nullable String answer) {
        final List<ReviewFileDraft> found = new ArrayList<>();
        if (answer == null || answer.isBlank()) {
            return found;
        }
        final Pattern pattern =
                Pattern.compile("\\{\\s*\"path\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"comment\"\\s*:\\s*\"([^\"]*)\"");
        final Matcher matcher = pattern.matcher(answer);
        while (matcher.find() && found.size() < MAX_ISSUES) {
            found.add(new ReviewFileDraft(matcher.group(1), ReviewFileLevel.ISSUE, matcher.group(2), null));
        }
        return found;
    }

    /** 组给云效看的评论：标题由平台固定，下面只列问题，轻量 Markdown。 */
    private static String buildComment(final List<ReviewFileDraft> issues) {
        if (issues.isEmpty()) {
            return "未发现需要修改的问题。";
        }
        final StringBuilder comment =
                new StringBuilder("发现 ").append(issues.size()).append(" 处问题，建议修复后再合并。");
        comment.append("\n\n");
        for (final ReviewFileDraft draft : issues) {
            comment.append("- `").append(draft.path()).append('`');
            if (draft.commentText() != null && !draft.commentText().isBlank()) {
                comment.append("：")
                        .append(KnowledgeReviewAgent.cleanSentence(
                                draft.commentText().strip()));
            }
            comment.append('\n');
        }
        return KnowledgeReviewAgent.clip(comment.toString().strip(), KnowledgeReviewAgent.MAX_COMMENT_CHARS);
    }

    /** 统一收尾：去掉省略号与悬空的分号、逗号，保证每条意见以句号结束。 */
    private static String cleanSentence(final String value) {
        String trimmed = value.strip();
        while (!trimmed.isEmpty()
                && (trimmed.endsWith("…")
                        || trimmed.endsWith("；")
                        || trimmed.endsWith("，")
                        || trimmed.endsWith("、")
                        || trimmed.endsWith(":"))) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).strip();
        }
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.endsWith("。") || trimmed.endsWith("！") || trimmed.endsWith("？") ? trimmed : trimmed + "。";
    }

    /** 从模型输出里取出 JSON 主体，兼容 ```json 代码块。 */
    private static String extractJson(final @Nullable String answer) {
        if (answer == null) {
            return "{}";
        }
        final int start = answer.indexOf('{');
        final int end = answer.lastIndexOf('}');
        return start >= 0 && end > start ? answer.substring(start, end + 1) : "{}";
    }

    private static @Nullable String textOrNull(final JsonNode node) {
        return node.isMissingNode() || node.isNull() || node.asText().isBlank()
                ? null
                : node.asText().strip();
    }

    private static String clip(final @Nullable String value, final int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    /** 暴露给模型的工具定义（OpenAI tools 格式）。 */
    private static List<Map<String, Object>> toolDefinitions() {
        final List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(KnowledgeReviewAgent.tool(
                "read_file",
                "读取当前代码仓库中某个文件的完整内容。用于确认函数签名、调用方、配置项等上下文。",
                KnowledgeReviewAgent.objectSchema(
                        Map.of("path", KnowledgeReviewAgent.stringProperty("仓库内相对路径")), List.of("path"))));
        tools.add(KnowledgeReviewAgent.tool(
                "grep",
                "用 git grep 在仓库里按正则搜索代码，返回 文件:行号:内容。用于查找调用方、常量、相似实现。",
                KnowledgeReviewAgent.objectSchema(
                        Map.of(
                                "pattern", KnowledgeReviewAgent.stringProperty("POSIX 正则"),
                                "path", KnowledgeReviewAgent.stringProperty("可选：限定目录或文件")),
                        List.of("pattern"))));
        tools.add(KnowledgeReviewAgent.tool(
                "list_files",
                "列出仓库中某个目录下的文件（最多两层）。",
                KnowledgeReviewAgent.objectSchema(
                        Map.of("path", KnowledgeReviewAgent.stringProperty("仓库内相对目录，留空为根目录")), List.of())));
        tools.add(KnowledgeReviewAgent.tool(
                "git_log",
                "查看最近的提交记录，可限定到某个文件或目录。用于判断这段代码的历史与修复意图。",
                KnowledgeReviewAgent.objectSchema(
                        Map.of(
                                "path", KnowledgeReviewAgent.stringProperty("可选：文件或目录"),
                                "limit", KnowledgeReviewAgent.numberProperty("条数，默认 20，最多 50")),
                        List.of())));
        tools.add(KnowledgeReviewAgent.tool(
                "git_show",
                "查看某个提交的改动概要。",
                KnowledgeReviewAgent.objectSchema(
                        Map.of("sha", KnowledgeReviewAgent.stringProperty("提交 SHA")), List.of("sha"))));
        tools.add(KnowledgeReviewAgent.tool(
                "search_knowledge",
                "检索团队知识库（研发规范、流程、业务说明），返回相关文档片段。",
                KnowledgeReviewAgent.objectSchema(
                        Map.of(
                                "query", KnowledgeReviewAgent.stringProperty("检索问题或关键词"),
                                "limit", KnowledgeReviewAgent.numberProperty("返回片段数，默认 6")),
                        List.of("query"))));
        return tools;
    }

    private static Map<String, Object> objectSchema(final Map<String, Object> properties, final List<String> required) {
        final Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private static Map<String, Object> stringProperty(final String description) {
        return Map.of("type", "string", "description", description);
    }

    private static Map<String, Object> numberProperty(final String description) {
        return Map.of("type", "number", "description", description);
    }

    private static Map<String, Object> tool(
            final String name, final String description, final Map<String, Object> parameters) {
        return Map.of(
                "type",
                "function",
                "function",
                Map.of("name", name, "description", description, "parameters", parameters));
    }
}
