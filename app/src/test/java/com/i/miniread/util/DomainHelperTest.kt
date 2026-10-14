package com.i.miniread.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DomainHelper 纯 JVM 单元测试。
 *
 * 只测显式传参的 3 参重载 [DomainHelper.isTargetDomain]，等价于
 * `settings.isActive && matchesAnyBaseUrl(baseUrl, settings.targetDomains)`；
 * 其中 `isActive` 还要求 feed 白名单非空（见 [InterceptionSettings.isActive]）。
 *
 * 注意：不调用无参 / 单参重载——它们读取 [InterceptionRuntime] 的内存快照，
 * 会引入对运行时状态的隐式依赖。
 */
class DomainHelperTest {

    private val target = "pi.lifeo3.icu"

    /** 基准配置：开关开、白名单非空 → `isActive == true`，各用例只破坏单个条件。 */
    private fun activeSettings(
        targetDomains: List<String> = listOf(target)
    ): InterceptionSettings = InterceptionSettings(
        enabled = true,
        targetDomains = targetDomains,
        feedIds = setOf(1)
    )

    @Test
    fun disabled_isNotTargetDomain() {
        val settings = activeSettings().copy(enabled = false)
        assertFalse(DomainHelper.isTargetDomain(settings, "https://$target"))
    }

    @Test
    fun emptyDomains_isNotTargetDomain() {
        val settings = activeSettings(targetDomains = emptyList())
        assertFalse(DomainHelper.isTargetDomain(settings, "https://$target"))
    }

    @Test
    fun emptyFeedIds_isNotTargetDomain() {
        // 白名单为空 = 没有任何 feed 会被拦截 → 不算生效（与运行时判定一致）
        val settings = activeSettings().copy(feedIds = emptySet())
        assertFalse(DomainHelper.isTargetDomain(settings, "https://$target"))
    }

    @Test
    fun matchingBaseUrl_isTargetDomain() {
        val settings = activeSettings()
        assertTrue(DomainHelper.isTargetDomain(settings, "https://$target"))
    }

    @Test
    fun nonMatchingBaseUrl_isNotTargetDomain() {
        val settings = activeSettings()
        assertFalse(DomainHelper.isTargetDomain(settings, "https://example.com"))
    }

    @Test
    fun multipleDomains_anyMatch_isTargetDomain() {
        val settings = activeSettings(targetDomains = listOf("other.com", target))
        assertTrue(DomainHelper.isTargetDomain(settings, "https://$target"))
    }

    @Test
    fun emptySettings_isNotTargetDomain() {
        assertFalse(DomainHelper.isTargetDomain(InterceptionSettings.EMPTY, "https://$target"))
    }
}
