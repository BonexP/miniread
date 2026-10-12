package com.i.miniread.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DomainHelper 纯 JVM 单元测试。
 *
 * 只测显式传参的 3 参重载 [DomainHelper.isTargetDomain]，等价于
 * `settings.isActive && matchesAnyBaseUrl(baseUrl, settings.targetDomains)`。
 *
 * 注意：不调用无参 / 单参重载——它们读取 [InterceptionRuntime] 的内存快照，
 * 会引入对运行时状态的隐式依赖。
 */
class DomainHelperTest {

    private val target = "pi.lifeo3.icu"

    @Test
    fun disabled_isNotTargetDomain() {
        val settings = InterceptionSettings(enabled = false, targetDomains = listOf(target))
        assertFalse(DomainHelper.isTargetDomain(settings, "https://$target"))
    }

    @Test
    fun emptyDomains_isNotTargetDomain() {
        val settings = InterceptionSettings(enabled = true, targetDomains = emptyList())
        assertFalse(DomainHelper.isTargetDomain(settings, "https://$target"))
    }

    @Test
    fun matchingBaseUrl_isTargetDomain() {
        val settings = InterceptionSettings(enabled = true, targetDomains = listOf(target))
        assertTrue(DomainHelper.isTargetDomain(settings, "https://$target"))
    }

    @Test
    fun nonMatchingBaseUrl_isNotTargetDomain() {
        val settings = InterceptionSettings(enabled = true, targetDomains = listOf(target))
        assertFalse(DomainHelper.isTargetDomain(settings, "https://example.com"))
    }

    @Test
    fun multipleDomains_anyMatch_isTargetDomain() {
        val settings = InterceptionSettings(
            enabled = true,
            targetDomains = listOf("other.com", target)
        )
        assertTrue(DomainHelper.isTargetDomain(settings, "https://$target"))
    }

    @Test
    fun emptySettings_isNotTargetDomain() {
        assertFalse(DomainHelper.isTargetDomain(InterceptionSettings.EMPTY, "https://$target"))
    }
}
