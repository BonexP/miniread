package com.i.miniread.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 定向拦截的运行时状态。
 *
 * - [settings]：解析后的生效配置（供 Compose 观察，配置变更后自动重组）
 * - [baseUrl]：当前 Miniflux 实例地址（判定目标域名用）
 *
 * 数据源是 [DataStoreManager]（DataStore 的 `base_url` 与 4 个 `intercept_*` key）：
 * - 应用启动时调用 [load]
 * - 登录 / 切换实例后调用 [refreshBaseUrl]
 * - 隐藏设置页保存后调用 [save]（或 [reset] 恢复中性化默认）
 *
 * 设计要点：所有读取都走内存快照，调用方（如 ArticleDetailScreen）**不再需要 runBlocking 读盘**。
 */
object InterceptionRuntime {

    private val _settings = MutableStateFlow(InterceptionSettings.EMPTY)
    val settings: StateFlow<InterceptionSettings> = _settings.asStateFlow()

    private val _baseUrl = MutableStateFlow("")
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    /** 当前生效配置快照（同步读取，不读盘）。 */
    fun currentSettings(): InterceptionSettings = _settings.value

    /** 当前 baseUrl 快照（同步读取，不读盘）。 */
    fun currentBaseUrl(): String = _baseUrl.value

    /** 从 DataStore 刷新内存快照（baseUrl + 4 个 intercept_* key）。 */
    suspend fun load() {
        _baseUrl.value = DataStoreManager.getBaseUrl()
        _settings.value = InterceptionSettingsCodec.parse(DataStoreManager.getInterceptionRaw())
    }

    /** 仅刷新 baseUrl（登录 / 切换实例后调用）。 */
    suspend fun refreshBaseUrl() {
        _baseUrl.value = DataStoreManager.getBaseUrl()
    }

    /** 保存配置：写 DataStore 并立即更新内存快照。 */
    suspend fun save(settings: InterceptionSettings) {
        DataStoreManager.saveInterceptionRaw(InterceptionSettingsCodec.format(settings))
        _settings.value = settings
    }

    /** 恢复中性化默认（关闭 + 全部为空）。 */
    suspend fun reset() = save(InterceptionSettings.EMPTY)

    /**
     * 判定某篇文章是否需要启用定向请求拦截（纯函数，便于单元测试）。
     *
     * 条件与原实现一致：总开关开启 **且** 命中任一目标域名 **且** 该文章所属 feedId 在白名单内。
     */
    fun isInterceptionActiveFor(
        settings: InterceptionSettings,
        baseUrl: String,
        feedId: Int?
    ): Boolean {
        if (!settings.enabled) return false
        if (settings.targetDomains.isEmpty()) return false
        if (feedId == null || feedId !in settings.feedIds) return false
        return InterceptionConfig.matchesAnyBaseUrl(baseUrl, settings.targetDomains)
    }

    /** 便捷重载：用当前内存快照判定。 */
    fun isInterceptionActiveFor(feedId: Int?): Boolean =
        isInterceptionActiveFor(_settings.value, _baseUrl.value, feedId)
}
