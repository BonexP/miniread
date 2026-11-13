# 版本控制工作流：命令与回退（AGENTS.md §10 展开）

> **本文是 [AGENTS.md](../AGENTS.md) §10 的展开**，不是第二份权威指令。冲突时**一律以 AGENTS.md 为准**。
> AGENTS.md 的 §10.1 五条硬性纪律（尤其 eol-conversion 配置、tag 归 git、`jj` 改写 `@` 后 `git switch main`）**不搬到这里**，必须留在唯一权威文件内，注入截断也切不掉。
> 本文属 **L2 参考**，记录 2026-10-16~18 的实测数据；命令与路径以当前环境为准，执行前先核。
> 目录：命令速查 / 回退 jj / 其他已核实的坑。

---

## 1. 背景（AGENTS.md §10 摘要）

本仓库自 **2026-10-16** 起以 **colocated 模式**同时使用 git 与 **jj（Jujutsu 0.46.0）**：`.jj/` 与 `.git/` 同级共存，两类命令都可用。引入前已按「换行符 / 性能 / 发布链路 / 回退」四道闸门实测，结论为**适合本项目**（数据见 `.workbuddy/reports/jj-pilot-report-2026-10-16.html`，本机私有，不随仓库分发）。

**为什么本仓库能用**：jj 官方**不支持**的 submodule / Git LFS / git hooks / `.gitattributes` / partial clone / `git worktree`，本仓库**一个都没用到**。⚠️ 将来若引入其中任意一项，需重新评估 jj 的适用性。

---

## 2. 常用命令（原文 §10.2）

| 目的 | 命令 |
| --- | --- |
| 状态 / 日志 / diff | `jj st` / `jj log` / `jj diff` |
| 提交（无暂存区，改完即自动快照） | `jj commit -m "..."`（等价 describe + new） |
| 开新工作（≈ 建分支） | `jj new main -m "意图"` |
| 撤销上一步 / 跳到任意历史状态 | `jj undo` / `jj op log` / `jj op restore <op-id>` |
| 发版打 tag（附注，git） | `git tag -a vX -m "MiniRead X.Y.Z — 要点1、要点2"` → `git push origin vX` |
| 移动 tag 让 CI 重跑（git） | `git tag -f -a vX -m "..."` → `git push origin --delete vX` → `git push origin vX` |
| 让 jj 看见新 tag | `jj git fetch`（`tracked` 仍可能为 `false`，正常） |
| 同步远端（≈ git pull） | `jj git fetch` → `jj rebase -d main` |

> tag 操作**不要**用 `jj tag set`（只能轻量 tag，会降级附注）；`jj tag set` 若真要用于非发布场景，默认**拒绝移动**已有 tag，必须显式加 `--allow-move`。
> **推送远程、打 / 移动 / 删除 tag、创建 Release 属公开不可逆动作，每次均需用户明确授权**（AGENTS.md §9 / §10.1 第 5 条）。

---

## 3. 回退 jj（完全退回纯 git，原文 §10.3）

```bash
rm -rf .jj
git for-each-ref --format='%(refname)' refs/jj | while read -r r; do git update-ref -d "$r"; done
git switch main
```

三行缺一不可：第二行清除实测 **82 个** `refs/jj/*` 残留引用（49 个 `keep` + 33 个 `remote-tags`），第三行修掉 detached HEAD。执行后 **git 侧**的 refs / index / `git status` / git 本地配置 / 文件哈希与引入前基线**完全一致**（已实测）。

⚠️ **但「完全一致」仅限 git 侧**：`%APPDATA%\jj\repos\<config-id>\`（本仓库为 `56a476adacc04af8cabf`，即 `.jj/repo/config-id` 的内容）不会随 `.jj` 删除而消失，其中的 `config.toml` 仍留在磁盘上。它不再被任何仓库读取，但会永久占位（本机现有 5 个，其中 4 个是试点实验遗留）。确认不再使用 jj 后可定向清理：

```bash
ls "$APPDATA/jj/repos/"                          # 先确认目录（Git Bash 下 $APPDATA 可用）
rm -rf "$APPDATA/jj/repos/56a476adacc04af8cabf"  # 只删本仓库这一份；其余按需
```

清理陈旧分支的安全做法（**先校验已合并，再删**）：

```bash
git fetch --all --prune
git rev-list --count origin/main..<branch>   # 必须为 0 才可删
git branch -d <branch>                       # 用 -d 而非 -D
```

> ⚠️ 以上命令会**删除 `.jj/` 与本机 jj 配置目录**，属于不可逆操作。执行前须按 AGENTS.md §7 确认 `.jj/` 的纪律，并取得用户明确授权。

---

## 4. 其它已核实的坑（原文 §10.4）

1. **本地 tag 与远端不一致**时，`git fetch --tags` 会报 `would clobber existing tag` 并跳过该 tag。确认以远端为准后 `git tag -f <tag> <远端 commit>` 对齐即可（本地旧对象不会丢失）。
2. **colocated 下不要用 git 做提交 / 改写历史 / 切工作副本**（会造成 divergent change）。提交一律走 jj；git 只用于只读查看与 tag / 分支清理。
3. **性能预期**：jj 每条命令都会走一次工作区快照，`jj st` 实测约 **991ms** vs `git status` 约 748ms（工作区含 625MB / 7240 个已忽略的构建文件）。属预期行为，不是故障。

---

## 5. eol-conversion 完整诊断（AGENTS.md §10.1 第 1 条的展开，纪律本身不搬）

本机 `core.autocrlf=true`（磁盘 CRLF / 仓库 LF），而仓库级 `working-copy.eol-conversion` **默认 `none`**：缺失时 `jj st` 会把 **115 个文件**全部判为已修改（实测 21628 增 / 21628 删，纯行尾差异），且 `gradlew` 有被写成 CRLF 的风险（会破坏 Linux CI）。

配置**不在 `.jj/` 内**：jj 0.38 起仓库级配置因安全原因移出仓库，实际存于 `%APPDATA%\jj\repos\<config-id>\config.toml`，`<config-id>` 即 `.jj/repo/config-id` 的内容（本仓库当前为 `56a476adacc04af8cabf`）。自查两条：

```bash
jj config path --repo          # 应打印上述路径
jj config list --repo          # 应含 working-copy.eol-conversion = "input-output"
```

改配置用 `jj config edit --repo`，**不要**去 `.jj/repo/` 下找 `config.toml`（那里没有）。

**重建 `.jj` 会生成新的 config-id，旧配置目录随即成为孤儿、新仓库读不到它 —— 配置必然丢失**；换机器同理。因此必须重设并强制重读一次：

```bash
jj config set --repo working-copy.eol-conversion input-output
jj restore        # 仅需一次，强制 jj 重读工作副本
```

修复后磁盘字节**零改动**（115 个 CRLF 文件与 `gradlew` 哈希均保持不变）。**不要**试图用 `git add --renormalize` 或新增 `.gitattributes` 绕过这个问题（jj 不读 `.gitattributes`）。

---

## 6. tag 的既有常态（AGENTS.md §10.1 第 2/4 条的展开）

实测（2026-10-18）`jj tag list -T 'name ++ " " ++ tracked'` 显示 `v1.0.0`~`v1.0.3` **全部 `tracked=false`** —— jj 从未跟踪过这些远端的附注 tag，这是既有常态。因此：

- 建 / 移 / 删 / 推 tag 一律用 git；jj 侧只需在推送后 `jj git fetch` 让 tag 可见（`tracked` 仍可能为 `false`，无需处理）。
- 若坚持用 jj 推 tag，未先 fetch 会失败：`Warning: ... refs/tags/vX (reason: stale info)` → `Error: Failed to push some bookmarks`；`--force` **不是**合法参数。
- 不对称提示：**bookmark 重写后会自动 force-push，tag 不会**。
- `git tag -a` / `-d` / `-f` / `push` 是唯一路径（本仓库 `v1.0.0`~`v1.1.0` 均为附注 tag）。**禁止**用 `jj tag set` 动发布 tag —— 它只能产出轻量 tag，会把附注降级为普通 commit 引用；也**避免** `jj git push --all`（会把 `tracked=false` 的历史 tag 一并纳入候选）。发布链路本身不依赖附注 tag（`release.yml` 只用 `github.ref_name`，见 AGENTS.md §9），附注仅用于把发布说明写进注记。

## 7. jj 改写 `@` 与 git HEAD（AGENTS.md §10.1 第 3 条的展开）

`jj commit` / `jj edit` 等改写 `@` 时会把 git 的 HEAD 置于 detached 状态 → 此时既有的 `git fetch && git merge --ff-only origin/main` 会**静默失效**（只移动游离的 HEAD，`refs/heads/main` 不前进）。

**凡要执行写操作的 git 命令，先 `git switch main`**；`git log/diff/show/status` 等只读命令不受影响。

实测（2026-10-17）：`git switch main` 之后**不会**被再次 detach —— 连跑 `jj st` 与 `jj log`，HEAD 仍稳定在 `refs/heads/main`（`@` 的父提交与 main 指向一致时尤其如此）。所以「用了 jj 就永远不能跑 git 写命令」是误解，正确的说法是「**每次 jj 改写 `@` 之后重新挂回一次**」。
