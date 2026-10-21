package com.i.miniread.util

/** 三个可编辑字段。 */
enum class DiagnosticField { Domains, FeedIds, RefererRules }

/**
 * 输入问题的分类。
 *
 * `errors` = 这条内容**会被解析器丢弃**，写了等于没写；
 * `warnings` = 内容会被保留，但大概率不是用户想要的效果。
 */
enum class IssueCode {
    /** 出现中文逗号 / 分号等全角标点，不参与切分。 */
    ChineseSeparator,

    /** 域名里带了 `://`，`contains` 永远匹配不到。 */
    DomainHasScheme,

    /** 域名里带了 `/` 或 `?`，只填主机或主机片段。 */
    DomainHasPath,

    /** 订阅源编号不是数字，会被丢弃。 */
    FeedIdNotNumber,

    /** 编号 ≤ 0，能解析但不可能命中任何订阅源。 */
    FeedIdNotPositive,

    /** 规则里没有 `=>`，会被丢弃。 */
    RuleMissingArrow,

    /** 规则以 `=>` 开头，左侧为空，会被丢弃。 */
    RuleArrowAtStart,

    /** `=>` 某一侧为空，会被丢弃。 */
    RuleEmptySide,

    /** 出现多个 `=>`，右侧会被当成 Referer 原文（含多余的箭头）。 */
    RuleMultipleArrows,

    /** 右侧不是 http(s) 开头的完整地址。 */
    RuleRefererNotUrl,

    /** 左侧关键字里带 `://`，对 host 做 contains 永远不命中。 */
    RulePatternHasScheme,

    /** 前面的规则已覆盖同一关键字，这条永远不会生效。 */
    RuleShadowed,

    /** 重复条目。 */
    DuplicateItem
}

/**
 * @param raw 用户原文（已 trim），用于在提示里回显「是哪一条」
 * @param detail 补充信息，例如遮蔽它的前一条关键字
 */
data class Issue(
    val field: DiagnosticField,
    val code: IssueCode,
    val raw: String,
    val detail: String = ""
)

/**
 * 单个字段的诊断结果。
 *
 * @param rawCount 切分后的非空条目数
 * @param validCount 真正参与匹配的条目数（必须等于对应 `parseXxx` 的结果长度）
 * @param errors 会被丢弃的条目
 * @param warnings 保留但可能不符合预期的条目
 */
data class FieldReport(
    val field: DiagnosticField,
    val rawCount: Int,
    val validCount: Int,
    val errors: List<Issue> = emptyList(),
    val warnings: List<Issue> = emptyList()
) {
    val droppedCount: Int get() = errors.size
    val hasError: Boolean get() = errors.isNotEmpty()
    val hasWarning: Boolean get() = warnings.isNotEmpty()
}

/** 配置已保存但不会拦截任何文章的原因。 */
enum class InactiveReason { Disabled, NoDomains, NoFeedIds }

/**
 * @param baseUrlMatches null = 调用方没提供 baseUrl，不做判断
 */
data class InterceptionDiagnostics(
    val domains: FieldReport,
    val feedIds: FieldReport,
    val rules: FieldReport,
    val isActive: Boolean,
    val inactiveReasons: List<InactiveReason>,
    val baseUrlMatches: Boolean?
) {
    val droppedTotal: Int
        get() = domains.droppedCount + feedIds.droppedCount + rules.droppedCount

    val warningTotal: Int
        get() = domains.warnings.size + feedIds.warnings.size + rules.warnings.size
}

/**
 * 把「用户输入」翻译成「会发生什么」。
 *
 * 红线（改动前必读）：
 * 1. 条目切分只能走 [InterceptionSettingsCodec.splitEntries]，**不许**自己写 `split(",")`；
 * 2. [FieldReport.validCount] 必须等于对应 `parseDomains` / `parseFeedIds` / `parseRefererRules`
 *    的结果长度，否则 UI 会告诉用户「X 项有效」而实际不是；
 * 3. [InterceptionDiagnostics.isActive] 必须等于 `InterceptionSettings.isActive`；
 * 4. 无 IO、无 Android 依赖 —— 纯 JVM 单测锁定上述三条。
 */
object InterceptionDiagnosticsAnalyzer {

    private val CHINESE_SEPARATORS = charArrayOf('，', '；', '、', '。', '：')

    private fun containsChineseSeparator(text: String): Boolean =
        text.any { it in CHINESE_SEPARATORS }

    fun analyze(raw: InterceptionRawConfig, baseUrl: String? = null): InterceptionDiagnostics {
        val domains = analyzeDomains(raw.domains)
        val feedIds = analyzeFeedIds(raw.feedIds)
        val rules = analyzeRefererRules(raw.refererRules)
        val settings = InterceptionSettingsCodec.parse(raw)

        // 关着的时候只说「开关没开」，不再逐条数落缺失项，避免一次弹三条噪音。
        val reasons: List<InactiveReason> = if (!raw.enabled) {
            listOf(InactiveReason.Disabled)
        } else {
            buildList {
                if (settings.targetDomains.isEmpty()) add(InactiveReason.NoDomains)
                if (settings.feedIds.isEmpty()) add(InactiveReason.NoFeedIds)
            }
        }

        return InterceptionDiagnostics(
            domains = domains,
            feedIds = feedIds,
            rules = rules,
            isActive = settings.isActive,
            inactiveReasons = reasons,
            baseUrlMatches = baseUrl
                ?.takeIf { it.isNotBlank() }
                ?.let { InterceptionConfig.matchesAnyBaseUrl(it, settings.targetDomains) }
        )
    }

    fun analyzeDomains(raw: String?): FieldReport {
        val entries = InterceptionSettingsCodec.splitEntries(raw)
        val valid = InterceptionSettingsCodec.parseDomains(raw)
        val warnings = mutableListOf<Issue>()
        val seen = mutableSetOf<String>()

        entries.forEach { entry ->
            if (containsChineseSeparator(entry)) {
                warnings += Issue(DiagnosticField.Domains, IssueCode.ChineseSeparator, entry)
            }
            if (entry.contains("://")) {
                warnings += Issue(DiagnosticField.Domains, IssueCode.DomainHasScheme, entry)
            }
            if (entry.contains('/') || entry.contains('?')) {
                warnings += Issue(DiagnosticField.Domains, IssueCode.DomainHasPath, entry)
            }
            if (!seen.add(entry)) {
                warnings += Issue(DiagnosticField.Domains, IssueCode.DuplicateItem, entry)
            }
        }

        // 域名永远不会被丢弃（空项在切分时已过滤），errors 恒空。
        return FieldReport(DiagnosticField.Domains, entries.size, valid.size, emptyList(), warnings)
    }

    fun analyzeFeedIds(raw: String?): FieldReport {
        val entries = InterceptionSettingsCodec.splitEntries(raw)
        val valid = InterceptionSettingsCodec.parseFeedIds(raw)
        val errors = mutableListOf<Issue>()
        val warnings = mutableListOf<Issue>()
        val seen = mutableSetOf<Int>()

        entries.forEach { entry ->
            val id = entry.toIntOrNull()
            if (id == null) {
                if (containsChineseSeparator(entry)) {
                    warnings += Issue(DiagnosticField.FeedIds, IssueCode.ChineseSeparator, entry)
                }
                errors += Issue(DiagnosticField.FeedIds, IssueCode.FeedIdNotNumber, entry)
                return@forEach
            }
            if (id <= 0) {
                warnings += Issue(DiagnosticField.FeedIds, IssueCode.FeedIdNotPositive, entry)
            }
            if (!seen.add(id)) {
                warnings += Issue(DiagnosticField.FeedIds, IssueCode.DuplicateItem, entry)
            }
        }

        return FieldReport(DiagnosticField.FeedIds, entries.size, valid.size, errors, warnings)
    }

    fun analyzeRefererRules(raw: String?): FieldReport {
        val entries = InterceptionSettingsCodec.splitEntries(raw)
        val valid = InterceptionSettingsCodec.parseRefererRules(raw)
        val errors = mutableListOf<Issue>()
        val warnings = mutableListOf<Issue>()
        val seenPatterns = mutableListOf<String>()
        val arrow = InterceptionSettingsCodec.RULE_ARROW

        entries.forEach { entry ->
            val idx = entry.indexOf(arrow)
            if (idx < 0) {
                errors += Issue(DiagnosticField.RefererRules, IssueCode.RuleMissingArrow, entry)
                return@forEach
            }
            if (idx == 0) {
                errors += Issue(DiagnosticField.RefererRules, IssueCode.RuleArrowAtStart, entry)
                return@forEach
            }
            val pattern = entry.substring(0, idx).trim()
            val referer = entry.substring(idx + arrow.length).trim()
            if (pattern.isEmpty() || referer.isEmpty()) {
                errors += Issue(DiagnosticField.RefererRules, IssueCode.RuleEmptySide, entry)
                return@forEach
            }

            if (entry.indexOf(arrow, idx + arrow.length) >= 0) {
                warnings += Issue(DiagnosticField.RefererRules, IssueCode.RuleMultipleArrows, entry)
            }
            if (!referer.startsWith("http://") && !referer.startsWith("https://")) {
                warnings += Issue(DiagnosticField.RefererRules, IssueCode.RuleRefererNotUrl, entry)
            }
            if (pattern.contains("://")) {
                warnings += Issue(DiagnosticField.RefererRules, IssueCode.RulePatternHasScheme, entry)
            }
            // 前面的关键字是这条的子串 → 任何能命中这条的 host 都会先命中前面那条，这条永不生效。
            val shadow = seenPatterns.firstOrNull { pattern.contains(it) }
            if (shadow != null) {
                warnings += Issue(DiagnosticField.RefererRules, IssueCode.RuleShadowed, entry, shadow)
            }
            seenPatterns += pattern
        }

        return FieldReport(DiagnosticField.RefererRules, entries.size, valid.size, errors, warnings)
    }
}
