package com.i.miniread.ui

import androidx.annotation.StringRes
import com.i.miniread.R
import com.i.miniread.util.InactiveReason
import com.i.miniread.util.IssueCode

/**
 * 帮助页的小节表。**只放资源 id**，文案全部在 `values` / `values-zh` 的 `strings.xml` 里。
 *
 * `id` 是路由参数 `?section=` 的取值，必须是稳定的 ASCII 短串 —— 设置页每个字段的「?」
 * 靠它直达对应小节，改 id 等于改外部契约。
 */
enum class HelpSection(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    @StringRes val exampleRes: Int? = null
) {
    Overview(
        "overview",
        R.string.settings_help_section_overview_title,
        R.string.settings_help_section_overview_body
    ),
    When(
        "when",
        R.string.settings_help_section_when_title,
        R.string.settings_help_section_when_body
    ),
    Conditions(
        "conditions",
        R.string.settings_help_section_conditions_title,
        R.string.settings_help_section_conditions_body
    ),
    Domains(
        "domains",
        R.string.settings_help_section_domains_title,
        R.string.settings_help_section_domains_body,
        R.string.settings_help_section_domains_example
    ),
    FeedIds(
        "feedids",
        R.string.settings_help_section_feedids_title,
        R.string.settings_help_section_feedids_body,
        R.string.settings_help_section_feedids_example
    ),
    Referer(
        "referer",
        R.string.settings_help_section_referer_title,
        R.string.settings_help_section_referer_body,
        R.string.settings_help_section_referer_example
    ),
    Effect(
        "effect",
        R.string.settings_help_section_effect_title,
        R.string.settings_help_section_effect_body
    ),
    Backup(
        "backup",
        R.string.settings_help_section_backup_title,
        R.string.settings_help_section_backup_body,
        R.string.settings_help_section_backup_example
    ),
    Mistakes(
        "mistakes",
        R.string.settings_help_section_mistakes_title,
        R.string.settings_help_section_mistakes_body
    ),
    Privacy(
        "privacy",
        R.string.settings_help_section_privacy_title,
        R.string.settings_help_section_privacy_body
    );

    companion object {
        /** 未知或空 id 返回 null（帮助页停在顶部，不滚动）。 */
        fun fromId(id: String?): HelpSection? = values().firstOrNull { it.id == id }
    }
}

/** 诊断问题 → 用户可读文案。放在 UI 层，`util/` 不引用 `R`。 */
@StringRes
fun IssueCode.messageRes(): Int = when (this) {
    IssueCode.ChineseSeparator -> R.string.settings_hidden_issue_chinese_separator
    IssueCode.DomainHasScheme -> R.string.settings_hidden_issue_domain_scheme
    IssueCode.DomainHasPath -> R.string.settings_hidden_issue_domain_path
    IssueCode.FeedIdNotNumber -> R.string.settings_hidden_issue_feed_not_number
    IssueCode.FeedIdNotPositive -> R.string.settings_hidden_issue_feed_not_positive
    IssueCode.RuleMissingArrow -> R.string.settings_hidden_issue_rule_arrow
    IssueCode.RuleArrowAtStart -> R.string.settings_hidden_issue_rule_empty_side
    IssueCode.RuleEmptySide -> R.string.settings_hidden_issue_rule_empty_side
    IssueCode.RuleMultipleArrows -> R.string.settings_hidden_issue_rule_multiple_arrows
    IssueCode.RuleRefererNotUrl -> R.string.settings_hidden_issue_rule_referer_url
    IssueCode.RulePatternHasScheme -> R.string.settings_hidden_issue_rule_pattern_scheme
    IssueCode.RuleShadowed -> R.string.settings_hidden_issue_rule_shadowed
    IssueCode.DuplicateItem -> R.string.settings_hidden_issue_duplicate
}

/** 未生效原因 → 用户可读文案。 */
@StringRes
fun InactiveReason.messageRes(): Int = when (this) {
    InactiveReason.Disabled -> R.string.settings_hidden_reason_disabled
    InactiveReason.NoDomains -> R.string.settings_hidden_reason_no_domains
    InactiveReason.NoFeedIds -> R.string.settings_hidden_reason_no_feeds
}
