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

            任务：审查这次合并请求的变更，找出「有修复价值」的问题，写一条简短评论。

            判断标准（只报事实型，不报观点型）：
            - 只报告能指出明确错误后果的问题：参数个数或类型不匹配、变量/方法/属性不存在、空集合取键、
              异常未处理、数据会被截断或丢失、鉴权/签名缺失、越权、并发下会出错、明显违反团队硬性规范。
            - 不报告观点型问题：注释与实现不符、建议加重试、建议脱敏、日志级别或格式、命名、代码结构、
              可读性、重构建议、补充测试建议，一律不要输出。
            - 只报由本次变更引入或修改的代码引发的问题：判断依据是 diff 里的「+」行。对「+」行引用的外部方法/类
              可以用工具核实，但不要审查文件里未改动的历史代码，不要报历史遗留问题。
            - 说不清"会导致什么错误行为"的问题，不要报。
            - 问题数量不设上限，但每一条都必须能指出后果；没有值得改的就明说。
            - level 一律用 issue。

            工作方式：
            1. 先看 diff；对不确定的地方调用工具核实（read_file 看完整文件、grep 搜调用链、git_log 看历史、search_knowledge 查团队规范）。
            2. 结论必须基于证据，不要臆测；工具查不到就直说。
            3. 每条 comment 控制在一句话内（不超过 80 字），以句号结尾；最后一步不要再调用工具，直接输出 JSON：
            {"summary":"一句话结论","files":[{"path":"文件路径","level":"issue|suggest","comment":"一句话说明问题与改法"}]}
            """;

    private static final int MAX_ROUNDS = 6;

    /** 评论里最多列出的问题数：只做极端保护，不限制模型发现问题。 */
    private static final int MAX_ISSUES = 50;

    private static final int MAX_COMMENT_CHARS = 3000;

    private static final int MAX_DIFF_CHARS_PER_FILE = 6000;

    private final McpKnowledgeClient knowledge;

    private final OpenAiCompatibleClient llm;

    private final AutomationProperties properties;

    private final ObjectMapper mapper;

    /** 构造评审 Agent。 */
    public KnowledgeReviewAgent(
            final McpKnowledgeClient knowledge,
            final OpenAiCompatibleClient llm,
            final AutomationProperties properties,
            final ObjectMapper mapper) {
        this.knowledge = knowledge;
        this.llm = llm;
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    public ReviewDraft review(
            final ChangeRequestSummary summary, final List<ChangedFile> files, final Path repositoryPath) {
        final RepositoryToolbox toolbox = new RepositoryToolbox(repositoryPath);
        final List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", KnowledgeReviewAgent.SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", this.buildInitialPrompt(summary, files)));
        final List<Map<String, Object>> tools = KnowledgeReviewAgent.toolDefinitions();
        for (int round = 1; round <= KnowledgeReviewAgent.MAX_ROUNDS; round++) {
            final ChatResult result = this.llm.chatWithTools(messages, tools);
            if (result.toolCalls().isEmpty()) {
                return this.parse(result.content(), files);
            }
            messages.add(KnowledgeReviewAgent.assistantMessage(result));
            for (final ToolCall call : result.toolCalls()) {
                final String output = this.executeTool(call, toolbox);
                LOGGER.debug(
                        "Review agent tool {} args={} returned {} chars",
                        call.name(),
                        call.arguments(),
                        output.length());
                messages.add(Map.of("role", "tool", "tool_call_id", call.id(), "content", output));
            }
        }
        LOGGER.warn("Review agent used all {} tool rounds, forcing conclusion", KnowledgeReviewAgent.MAX_ROUNDS);
        messages.add(Map.of("role", "user", "content", "工具调用轮数已用完，请直接输出最终 JSON 结论，不要再调用工具。"));
        final ChatResult last = this.llm.chatWithTools(messages, List.of());
        return this.parse(last.content(), files);
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

    private ReviewDraft parse(final @Nullable String answer, final List<ChangedFile> files) {
        final String json = KnowledgeReviewAgent.extractJson(answer);
        try {
            final JsonNode root = this.mapper.readTree(json);
            final String summary = root.path("summary").asText("").strip();
            final List<ReviewFileDraft> drafts = new ArrayList<>();
            for (final JsonNode item : root.path("files")) {
                final String path = item.path("path").asText("").strip();
                if (path.isEmpty()) {
                    continue;
                }
                drafts.add(new ReviewFileDraft(
                        path,
                        ReviewFileLevel.of(item.path("level").asText("suggest")),
                        KnowledgeReviewAgent.textOrNull(item.path("comment")),
                        null));
            }
            // 只报事实型问题：非 issue 级别（观点型、建议型）一律不进评论。
            final List<ReviewFileDraft> issues = drafts.stream()
                    .filter(draft -> draft.level() == ReviewFileLevel.ISSUE)
                    .limit(KnowledgeReviewAgent.MAX_ISSUES)
                    .toList();
            final String conclusion = summary.isBlank() ? "未发现需要修改的问题" : summary;
            return new ReviewDraft(
                    KnowledgeReviewAgent.buildComment(conclusion, issues),
                    this.properties.review().llmModel(),
                    issues);
        } catch (final IOException ex) {
            LOGGER.warn("Model answer is not valid JSON, falling back to raw text: {}", ex.getMessage());
            return new ReviewDraft(
                    KnowledgeReviewAgent.clip(answer == null ? "" : answer, 500),
                    this.properties.review().llmModel(),
                    List.of());
        }
    }

    /** 组给云效看的评论：一句话结论 + 值得修的问题列表，轻量 Markdown。 */
    private static String buildComment(final String summary, final List<ReviewFileDraft> issues) {
        final String trimmed = summary.strip();
        final String headline;
        if (issues.isEmpty()) {
            headline = "未发现需要修改的问题。";
        } else if (trimmed.isEmpty() || trimmed.length() > 40 || trimmed.contains("：")) {
            headline = "发现 " + issues.size() + " 处值得修复的问题。";
        } else {
            headline = trimmed;
        }
        final StringBuilder comment = new StringBuilder(headline);
        if (!issues.isEmpty()) {
            comment.append("\n\n");
            for (final ReviewFileDraft draft : issues) {
                comment.append("- `").append(draft.path()).append('`');
                if (draft.commentText() != null && !draft.commentText().isBlank()) {
                    comment.append("：")
                            .append(KnowledgeReviewAgent.cleanSentence(KnowledgeReviewAgent.clipSentence(
                                    draft.commentText().strip(), 150)));
                }
                comment.append('\n');
            }
        }
        return KnowledgeReviewAgent.clip(comment.toString().strip(), MAX_COMMENT_CHARS);
    }

    /** 按句读截断，尽量不在句子中间切断。 */
    private static String clipSentence(final String value, final int max) {
        if (value.length() <= max) {
            return value;
        }
        for (final char ending : new char[] {'。', '；', '，'}) {
            final int cut = value.lastIndexOf(ending, max);
            if (cut > 20) {
                return value.substring(0, cut + 1);
            }
        }
        return value.substring(0, max - 1) + "。";
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
