package com.i.miniread.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * InterceptionRuntime 纯判定单元测试。
 *
 * 仅覆盖 [InterceptionRuntime.isInterceptionActiveFor]（纯函数）与
 * [InterceptionSettings.isActive] 语义；**不**触碰 load / save / reset
 * 等依赖 DataStore 的方法。
 *
 * 判定需同时满足：enabled == true、targetDomains 非空、feedId 非 null 且在
 * 白名单内、baseUrl 命中任一目标域名。
 */
class InterceptionRuntimeTest {

    private val target = "pi.lifeo3.icu"

    /** 一个"条件全配齐"的基准配置，各用例在此基础上破坏单个条件。 */
    private fun fullSettings(
        enabled: Boolean = true,
        targetDomains: List<String> = listOf(target),
        feedIds: Set<Int> = setOf(1)
    ): InterceptionSettings = InterceptionSettings(
        enabled = enabled,
        targetDomains = targetDomains,
        feedIds = feedIds,
        refererRules = emptyList()
    )

    // --- 单个条件不满足 ---

    @Test
    fun disabled_returnsFalse() {
        assertFalse(
            InterceptionRuntime.isInterceptionActiveFor(
                fullSettings(enabled = false),
                "https://$target",
                1
            )
        )
    }

    @Test
    fun emptyTargetDomains_returnsFalse() {
        assertFalse(
            InterceptionRuntime.isInterceptionActiveFor(
                fullSettings(targetDomains = emptyList()),
                "https://$target",
                1
            )
        )
    }

    @Test
    fun nullFeedId_returnsFalse() {
        assertFalse(
            InterceptionRuntime.isInterceptionActiveFor(fullSettings(), "https://$target", null)
        )
    }

    @Test
    fun feedIdNotInWhitelist_returnsFalse() {
        assertFalse(
            InterceptionRuntime.isInterceptionActiveFor(
                fullSettings(feedIds = setOf(1, 2)),
                "https://$target",
                99
            )
        )
    }

    @Test
    fun baseUrlNotMatching_returnsFalse() {
        assertFalse(
            InterceptionRuntime.isInterceptionActiveFor(fullSettings(), "https://example.com", 1)
        )
    }

    // --- 全部满足 ---

    @Test
    fun allConditionsMet_returnsTrue() {
        assertTrue(
            InterceptionRuntime.isInterceptionActiveFor(fullSettings(), "https://$target", 1)
        )
    }

    @Test
    fun multipleDomains_hitsSecond_returnsTrue() {
        assertTrue(
            InterceptionRuntime.isInterceptionActiveFor(
                fullSettings(targetDomains = listOf("other.com", target)),
                "https://$target",
                1
            )
        )
    }

    // --- InterceptionSettings.isActive ---

    @Test
    fun defaultSettings_isActiveFalse() {
        assertFalse(InterceptionSettings().isActive)
    }

    @Test
    fun emptySettings_isActiveFalse() {
        assertFalse(InterceptionSettings.EMPTY.isActive)
    }

    @Test
    fun enabledButNoDomains_isActiveFalse() {
        assertFalse(InterceptionSettings(enabled = true, targetDomains = emptyList()).isActive)
    }

    @Test
    fun enabledWithDomains_isActiveTrue() {
        assertTrue(InterceptionSettings(enabled = true, targetDomains = listOf(target)).isActive)
    }
}
