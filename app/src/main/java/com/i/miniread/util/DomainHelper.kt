package com.i.miniread.util

/**
 * 判断「当前实例是否命中已配置的目标域名」。
 *
 * v1.0.0 起改为读取 [InterceptionRuntime] 的**内存快照**，
 * 不再用 `runBlocking` 同步读 DataStore（原实现在组合期会造成主线程阻塞）。
 */
object DomainHelper {

    /**
     * 显式传入配置与 baseUrl 的重载（纯判定，便于调用方与单测使用）。
     */
    fun isTargetDomain(settings: InterceptionSettings, baseUrl: String): Boolean {
        if (!settings.isActive) return false
        return InterceptionConfig.matchesAnyBaseUrl(baseUrl, settings.targetDomains)
    }

    /** 用当前内存快照判定。 */
    fun isTargetDomain(): Boolean =
        isTargetDomain(InterceptionRuntime.currentSettings(), InterceptionRuntime.currentBaseUrl())

    /** 用当前配置 + 指定 baseUrl 判定。 */
    fun isTargetDomain(baseUrl: String): Boolean =
        isTargetDomain(InterceptionRuntime.currentSettings(), baseUrl)
}
