package com.i.miniread.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * InterceptionDiagnosticsAnalyzer 纯 JVM 单元测试。
 *
 * 最重要的是前四组「口径一致性」用例：诊断层说「N 项有效」「已生效」，必须与真实解析/
 * 运行时判定完全一致 —— 这个页面存在的全部意义就是别对用户说谎。
 */
class InterceptionDiagnosticsTest {

    private fun raw(
        enabled: Boolean = false,
        domains: String = "",
        feedIds: String = "",
        refererRules: String = ""
    ) = InterceptionRawConfig(enabled, domains, feedIds, refererRules)

    // --- 口径一致性 ---

    @Test
    fun analyze_emptyConfig_isInactiveWithDisabledReason() {
        val diag = InterceptionDiagnosticsAnalyzer.analyze(raw())
        assertFalse(diag.isActive)
        assertEquals(listOf(InactiveReason.Disabled), diag.inactiveReasons)
        assertNull(diag.baseUrlMatches)
    }

    @Test
    fun analyze_switchOn_bothEmpty_reportsNoDomainsAndNoFeeds() {
        val diag = InterceptionDiagnosticsAnalyzer.analyze(raw(enabled = true))
        assertFalse(diag.isActive)
        assertEquals(
            listOf(InactiveReason.NoDomains, InactiveReason.NoFeedIds),
            diag.inactiveReasons
        )
    }

    @Test
    fun analyze_completeConfig_isActive() {
        val diag = InterceptionDiagnosticsAnalyzer.analyze(
            raw(enabled = true, domains = "example.com", feedIds = "26")
        )
        assertTrue(diag.isActive)
        assertTrue(diag.inactiveReasons.isEmpty())
    }

    @Test
    fun analyze_isActive_equalsSettingsIsActive() {
        val cases = listOf(
            raw(),
            raw(enabled = true),
            raw(enabled = true, domains = "example.com"),
            raw(enabled = true, feedIds = "26"),
            raw(enabled = true, domains = "example.com", feedIds = "26"),
            raw(enabled = false, domains = "example.com", feedIds = "26")
        )
        cases.forEach { cfg ->
            assertEquals(
                "mismatch for $cfg",
                InterceptionSettingsCodec.parse(cfg).isActive,
                InterceptionDiagnosticsAnalyzer.analyze(cfg).isActive
            )
        }
    }

    @Test
    fun analyze_validCounts_equalCodecParseSizes() {
        val cfg = raw(
            enabled = true,
            domains = "a.com, b.com , a.com",
            feedIds = "26, abc, 38",
            refererRules = "a.com=>https://a/;broken;b.com=>https://b/"
        )
        val diag = InterceptionDiagnosticsAnalyzer.analyze(cfg)
        assertEquals(InterceptionSettingsCodec.parseDomains(cfg.domains).size, diag.domains.validCount)
        assertEquals(InterceptionSettingsCodec.parseFeedIds(cfg.feedIds).size, diag.feedIds.validCount)
        assertEquals(
            InterceptionSettingsCodec.parseRefererRules(cfg.refererRules).size,
            diag.rules.validCount
        )
    }

    @Test
    fun analyze_droppedTotal_sumsAllFields() {
        val diag = InterceptionDiagnosticsAnalyzer.analyze(
            raw(domains = "a.com", feedIds = "26,abc", refererRules = "broken")
        )
        assertEquals(1, diag.feedIds.droppedCount)
        assertEquals(1, diag.rules.droppedCount)
        assertEquals(0, diag.domains.droppedCount)
        assertEquals(2, diag.droppedTotal)
    }

    // --- domains ---

    @Test
    fun domains_chineseComma_reportsWarning() {
        val report = InterceptionDiagnosticsAnalyzer.analyzeDomains("a.com，b.com")
        assertEquals(1, report.rawCount)
        assertTrue(report.warnings.any { it.code == IssueCode.ChineseSeparator })
        // 域名永不丢弃
        assertFalse(report.hasError)
        assertEquals(1, report.validCount)
    }

    @Test
    fun domains_withScheme_reportsWarning() {
        val report = InterceptionDiagnosticsAnalyzer.analyzeDomains("https://example.com")
        assertTrue(report.warnings.any { it.code == IssueCode.DomainHasScheme })
        assertTrue(report.warnings.any { it.code == IssueCode.DomainHasPath })
    }

    @Test
    fun domains_withPath_reportsWarning() {
        val report = InterceptionDiagnosticsAnalyzer.analyzeDomains("example.com/feed")
        assertTrue(report.warnings.any { it.code == IssueCode.DomainHasPath })
        assertFalse(report.warnings.any { it.code == IssueCode.DomainHasScheme })
    }

    @Test
    fun domains_doesNotDropNonEmptyEntries() {
        val report = InterceptionDiagnosticsAnalyzer.analyzeDomains("a.com, b.com")
        assertEquals(2, report.rawCount)
        assertEquals(2, report.validCount)
        assertFalse(report.hasError)
    }

    @Test
    fun domains_duplicate_reportsWarning() {
        val report = InterceptionDiagnosticsAnalyzer.analyzeDomains("a.com,a.com")
        assertEquals(2, report.rawCount)
        assertEquals(1, report.validCount)
        assertEquals(1, report.warnings.count { it.code == IssueCode.DuplicateItem })
    }

    // --- feedIds ---

    @Test
    fun feedIds_mixedTextAndNumbers_reportsDroppedRaw() {
        val report = InterceptionDiagnosticsAnalyzer.analyzeFeedIds("26,abc,38")
        assertEquals(3, report.rawCount)
        assertEquals(2, report.validCount)
        assertEquals(1, report.droppedCount)
        assertEquals("abc", report.errors.first().raw)
        assertEquals(IssueCode.FeedIdNotNumber, report.errors.first().code)
    }

    @Test
    fun feedIds_chineseComma_reportsNotNumberAndWarning() {
        val report = InterceptionDiagnosticsAnalyzer.analyzeFeedIds("26，38")
        assertEquals(1, report.rawCount)
        assertEquals(0, report.validCount)
        assertEquals(IssueCode.FeedIdNotNumber, report.errors.first().code)
        assertTrue(report.warnings.any { it.code == IssueCode.ChineseSeparator })
    }

    @Test
    fun feedIds_duplicateIds_warnsOnce() {
        val report = InterceptionDiagnosticsAnalyzer.analyzeFeedIds("26,26")
        assertEquals(2, report.rawCount)
        assertEquals(1, report.validCount)
        assertEquals(1, report.warnings.count { it.code == IssueCode.DuplicateItem })
    }

    @Test
    fun feedIds_zeroOrNegative_reportsWarning() {
        val report = InterceptionDiagnosticsAnalyzer.analyzeFeedIds("0,-1")
        assertEquals(2, report.warnings.count { it.code == IssueCode.FeedIdNotPositive })
        assertEquals(2, report.validCount)
        assertFalse(report.hasError)
    }

    // --- referer rules ---

    @Test
    fun rules_missingArrow_dropped() {
        val report = InterceptionDiagnosticsAnalyzer.analyzeRefererRules("example.com")
        assertEquals(1, report.droppedCount)
        assertEquals(IssueCode.RuleMissingArrow, report.errors.first().code)
    }

    @Test
    fun rules_arrowAtStart_dropped() {
        val report = InterceptionDiagnosticsAnalyzer.analyzeRefererRules("=>https://example.com/")
        assertEquals(IssueCode.RuleArrowAtStart, report.errors.first().code)
    }

    @Test
    fun rules_emptyReferer_dropped() {
        val report = InterceptionDiagnosticsAnalyzer.analyzeRefererRules("a.com=>")
        assertEquals(IssueCode.RuleEmptySide, report.errors.first().code)
    }

    @Test
    fun rules_multipleArrows_reportsWarning() {
        val report = InterceptionDiagnosticsAnalyzer.analyzeRefererRules("a.com=>https://a.com/=>x")
        assertTrue(report.warnings.any { it.code == IssueCode.RuleMultipleArrows })
        assertFalse(report.hasError)
    }

    @Test
    fun rules_refererWithoutScheme_reportsWarning() {
        val report = InterceptionDiagnosticsAnalyzer.analyzeRefererRules("a.com=>a.com/")
        assertTrue(report.warnings.any { it.code == IssueCode.RuleRefererNotUrl })
        assertFalse(report.hasError)
    }

    @Test
    fun rules_patternWithScheme_reportsWarning() {
        val report = InterceptionDiagnosticsAnalyzer.analyzeRefererRules("https://a.com=>https://a.com/")
        assertTrue(report.warnings.any { it.code == IssueCode.RulePatternHasScheme })
    }

    @Test
    fun rules_shadowedPattern_reportsWarningWithDetail() {
        val report = InterceptionDiagnosticsAnalyzer.analyzeRefererRules(
            "a.com=>https://a.com/;cdn.a.com=>https://cdn.a.com/"
        )
        val shadowed = report.warnings.firstOrNull { it.code == IssueCode.RuleShadowed }
        assertTrue(shadowed != null)
        assertEquals("a.com", shadowed?.detail)
        // 仍然会被解析（顺序敏感、先匹配者生效），所以 validCount 是 2
        assertEquals(2, report.validCount)
    }

    @Test
    fun rules_orderPreserved_validCountEqualsParsed() {
        val text = "b.com=>https://b/;a.com=>https://a/"
        val report = InterceptionDiagnosticsAnalyzer.analyzeRefererRules(text)
        assertEquals(2, report.validCount)
        assertEquals("b.com", InterceptionSettingsCodec.parseRefererRules(text).first().first)
    }

    // --- baseUrl ---

    @Test
    fun analyze_baseUrlMismatch_setsFalse() {
        val diag = InterceptionDiagnosticsAnalyzer.analyze(
            raw(enabled = true, domains = "example.com", feedIds = "26"),
            baseUrl = "https://other.example"
        )
        assertEquals(false, diag.baseUrlMatches)
    }

    @Test
    fun analyze_baseUrlMatch_setsTrue() {
        val diag = InterceptionDiagnosticsAnalyzer.analyze(
            raw(enabled = true, domains = "example.com", feedIds = "26"),
            baseUrl = "https://example.com/v1"
        )
        assertEquals(true, diag.baseUrlMatches)
    }

    @Test
    fun analyze_blankBaseUrl_setsNull() {
        val diag = InterceptionDiagnosticsAnalyzer.analyze(
            raw(enabled = true, domains = "example.com", feedIds = "26"),
            baseUrl = "   "
        )
        assertNull(diag.baseUrlMatches)
    }
}
