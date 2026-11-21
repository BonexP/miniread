package com.i.miniread.ui

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.i.miniread.R
import com.i.miniread.network.Category
import com.i.miniread.network.Entry
import com.i.miniread.network.Feed
import com.i.miniread.ui.theme.MinireadTheme
import com.i.miniread.util.ReaderTypography
import com.i.miniread.viewmodel.MinifluxViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 列表项与阅读面板的 UI 冒烟测试（第一张安全网）。
 *
 * 动机：全仓 241 例单测全是纯逻辑（`util/` + `network/`），**UI 层 0 覆盖**。
 * 因此任何视觉改动都是盲改——本文件存在的意义不是"测细致行为"，而是
 * **把三个列表屏与排版面板锚定住**：改共享组件（如 `MiniCard`）后，
 * 只要渲染或点击断了，这里会立刻失败。
 *
 * 设计原则：
 * - **只测冒烟**：能渲染、文本在、点击回传。**不断言颜色/像素**——
 *   颜色断言会与「E-Ink 恒黑白」和「standard 动态取色」两条路径打架。
 * - 用 `createComposeRule()` 而非 `createAndroidComposeRule<MainActivity>()`，
 *   避免拉起真实网络请求与 DataStore 初始化。
 * - `EntryItem` 签名要求真实 `MinifluxViewModel`（滑动标记已读用它），
 *   这里用 `ApplicationProvider` 构造一个空 VM——本测试不触发滑动，不会发请求。
 */
@RunWith(AndroidJUnit4::class)
class ListItemSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val ctx: Application
        get() = ApplicationProvider.getApplicationContext()

    // 测试数据用固定英文/中立串（不依赖系统语言），断言时直接按同一串查找。
    private val categoryTitle = "TestCategory"
    private val feedTitle = "TestFeed"
    private val entryTitle = "TestEntry"

    private val category = Category(id = 1, title = categoryTitle)
    private val feed = Feed(
        id = 1,
        title = feedTitle,
        site_url = "",
        feed_url = "",
        category = Category(id = 1, title = "c"),
        disabled = false
    )
    private val entry = Entry(
        id = 1,
        title = entryTitle,
        url = "",
        content = "content",
        published_at = "2026-01-01T00:00:00Z",
        status = "unread",
        feed_id = 1,
        starred = false,
        feed = feed
    )

    @Test
    fun categoryItem_rendersTitle_andClickFires() {
        var clicked = false
        composeTestRule.setContent {
            MinireadTheme {
                CategoryItem(
                    category = category,
                    unreadCount = 3,
                    onClick = { clicked = true },
                    onMarkAsRead = {},
                    onShowSubscriptions = {}
                )
            }
        }
        composeTestRule.onNodeWithText(categoryTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(categoryTitle).performClick()
        assertTrue("点击列表项应回调 onClick", clicked)
    }

    @Test
    fun feedItem_rendersTitle() {
        composeTestRule.setContent {
            MinireadTheme {
                FeedItem(
                    feed = feed,
                    unreadCount = 5,
                    isEditMode = false,
                    onClick = {},
                    onMarkAsRead = {}
                )
            }
        }
        composeTestRule.onNodeWithText(feedTitle).assertIsDisplayed()
    }

    @Test
    fun entryItem_rendersTitle() {
        val vm = MinifluxViewModel(
            ApplicationProvider.getApplicationContext<Application>()
        )
        composeTestRule.setContent {
            MinireadTheme {
                EntryItem(viewModel = vm, entry = entry, onClick = {})
            }
        }
        composeTestRule.onNodeWithText(entryTitle).assertIsDisplayed()
    }

    /**
     * 排版面板冒烟：六个段落的**可访问名**都在（符号字形对读屏无意义，
     * 用 `clearAndSetSemantics` 换成了字段名，见 §6 / 坑 11）。
     *
     * ⚠️ **不要写死中文/英文字面量**：仪器测试跑在设备的系统语言下
     * （本机模拟器是英文，实测语义树里是 `Font size` / `Line spacing` …）。
     * 断言文案一律用 `context.getString(R.string.xxx)` 取，与运行时语言一致。
     *
     * ⚠️ 用 `useUnmergedTree = true`：`Switch` 的 `contentDescription` 与
     * `TypographySection` 的符号节点同名，合并树里会出现 2 个同 description 节点
     * （曾报 "found 2 nodes"）。这里只验"存在"，取 first 即可。
     *
     * ⚠️ **不要**用 `assertIsDisplayed()`：它要求节点在可见视口内，而面板内容超过一屏
     * （内部 `verticalScroll`），且它会强制真实布局/绘制，在模拟器上易与 GC 抢渲染线程
     * （实测报 `ReferenceQueueDaemon ... HardwareRenderer` 超时 73s）。纯语义查询不触发绘制。
     */
    @Test
    fun readerPanel_rendersAllSixSections() {
        composeTestRule.setContent {
            MinireadTheme {
                ReaderTypographyPanel(
                    current = ReaderTypography.DEFAULT,
                    onChange = {},
                    onReset = {},
                    onClose = {}
                )
            }
        }
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        val labels = intArrayOf(
            R.string.reader_typography_font_size,
            R.string.reader_typography_line_height,
            R.string.reader_typography_width,
            R.string.reader_typography_paragraph_spacing,
            R.string.reader_typography_font_family,
            R.string.reader_typography_align
        )
        labels.forEach { id ->
            val label = ctx.getString(id)
            composeTestRule
                .onAllNodesWithContentDescription(label, useUnmergedTree = true)
                .onFirst()
                .assertExists()
        }
    }

    /**
     * 防 v4 回归（唯一能发现该 bug 的手段）：`Modifier.size(48.dp)` 把宽度锁死后，
     * M3 默认 `contentPadding`（horizontal 24dp×2）会让内容可用宽变成 **0**，
     * `+`/`−` 字形**压根不渲染**——真机只看到空框、点击却生效。
     *
     * 当时语义树里 `Text = "[+]"` 节点**不存在**，就是靠这一点定位的。
     *
     * 数量说明：**4 对**（字号 / 行距 / 行宽 / 段距）。v5 起字号与行宽都改用
     * `TypographyIndexStepper`，加上原有的行距 / 段距两个 `TypographyStepper`，
     * 共 4 个 `+` + 4 个 `−`。（字体族 / 对齐是 `Switch`，没有 `+`/`−`。）
     */
    @Test
    fun readerPanel_plusMinusGlyphsExist() {
        composeTestRule.setContent {
            MinireadTheme {
                ReaderTypographyPanel(
                    current = ReaderTypography.DEFAULT,
                    onChange = {},
                    onReset = {},
                    onClose = {}
                )
            }
        }
        composeTestRule.onAllNodesWithText("+").assertCountEquals(4)
        composeTestRule.onAllNodesWithText("−").assertCountEquals(4)
    }
}
