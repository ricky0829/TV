---
name: fongmi-tv-upstream-sync
description: 把 FongMi/TV 上游更新同步到 ricky0829/TV 的 fongmi 分支，解决冲突、刷新预编译 media3 AAR、复核四项 TV 界面改动与 mpv 运动补偿兼容层，并触发 GitHub Actions 编译发布 APK。当用户提到同步上游、跟随上游更新、上游升级后重新打包时使用。
---

# FongMi TV 上游同步

把 `FongMi/TV` 的 `fongmi` 分支更新合并进本 fork，并重新产出可用 APK。

- 仓库：`ricky0829/TV`，分支 `fongmi`
- 本地：`d:\documents\trae\fongmi\TV-fongmi`
- Release：`aar`（预编译 AAR）、`5.8.7-N`（每次构建的版本化 APK，N 递增）

## 自动化链路

- `.github/workflows/sync-upstream.yml`：每天 03:23（北京时间）抓取上游并合并。冲突时创建 issue 并中止；成功则 push，并显式执行 `gh workflow run build-tv.yml --ref fongmi`。
- 注意：用仓库自带 `GITHUB_TOKEN` 推送不会触发 `push` 事件，所以同步工作流必须显式 dispatch 构建。
- **仓库必须开启 Issues**（fork 默认是关闭的）：`Create conflict issue` 与 `Verify custom patches` 都靠 `gh issue create` 报警。检查 `GET /repos/ricky0829/TV` 的 `has_issues`，为 false 时 `PATCH {"has_issues":true}`。
- 合并成功后、push 之前，同步工作流还会做两件事：
  1. `Sync version metadata`：从 `upstream/fongmi:app/build.gradle` 读取上游 `versionName` / `versionCode`，与 `gradle.properties` 的 `tvBaseVersion`、`app/build.gradle` 对比，不一致就自动改写并提交一个 chore commit。上游升级后序号自然从 1 重算。
  2. `Verify custom patches`：逐项 grep 本节「自定义改动清单」里的关键符号（选集按钮、竖排站点、多排选集、分辨率、左向焦点、版本化构建等）；缺失则创建 issue 并 `exit 1`，**中止推送与发布**，避免发出丢功能的版本。
- `.github/workflows/build-tv.yml`：从 Release `aar` 下载全部 `*.aar`，计算下一个版本号，构建 leanback APK，上传 artifact，并创建同名 tag / Release（两个 APK）。
- 手动同步：Actions 中运行 `Sync upstream`；或本地 `git fetch upstream fongmi && git merge upstream/fongmi`。

## 自定义改动清单（解决冲突时逐项确认）

1. 播放页标题下分辨率
   - `app/src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java`（`renderPlaybackMetadata`、`updateSubtitle`、`onSizeChanged`）
2. 控制栏“选集”按钮 + 播放中选集弹窗
   - `app/src/leanback/res/layout/view_control_vod_action.xml`（`episodes` 按钮与焦点环绕）
   - `VideoActivity.onEpisodes`、`EpisodeDialog.java`、`dialog_episode.xml`
   - `values/strings.xml`、`values-zh-rCN/strings.xml`、`values-zh-rTW/strings.xml` 的 `detail_episode`
3. 详情页多排选集
   - `app/src/leanback/res/layout/activity_video.xml`（`episode` 改为 `CustomRecyclerView`）
   - `EpisodeAdapter.java`（`grid(spanCount)` 与网格布局）、`adapter_episode_grid.xml`、`VideoActivity.getEpisodeSpan()`
4. 搜索页左侧竖排站点
   - `app/src/leanback/res/layout/activity_collect.xml`（左侧 `VerticalGridView`，id `sites`）
   - `CollectActivity`、`CollectAdapter`、`CollectFragment`（左右方向键焦点切换）
5. mpv 运动补偿兼容层
   - `MpvMotionSupport.java`：反射 `MpvPlayer.playbackProperties`，调用 `setStringOptionOrProperty` 设置 `interpolation`/`video-sync`/`tscale`，并联动窗口刷新率
   - `MpvUtil.addMotionOptions`（初始化时写入 mpv 选项，`MOTION_OPTIONS` 同步维护）
   - `MpvMotionSettingPanel.java`（面板状态与即时切换）
   - `app/proguard-rules.pro` 的 keep 规则（release 混淆后反射仍有效）
6. 片尾自动跳集（AAR 空壳兜底）
   - `app/src/main/java/com/fongmi/android/tv/player/exo/ExoPlayerEngine.java` 的
     `isEndingManaged()` 必须同时判断 `endingSource.isEndTrimActive()` 与
     `endingSource.getOriginalDurationMs() != C.TIME_UNSET`。
   - 原因：Release `aar` 的 `lib-exoplayer-release.aar` 中 `EndTrimMediaSource` 是
     占位实现——`prepareSource` / `createPeriod` 全部原样转发，不裁剪 Timeline，
     `getOriginalDurationMs()` 恒返回 `C.TIME_UNSET`。只看 `isEndTrimActive()` 会把
     `VodPlaybackController.skipEnding()` 的 Java 兜底短路，导致片尾时间点不跳下一集
     （只有文件自然播完才跳）。公开镜像 fork 的同一 AAR 哈希相同，无真实实现可用。
   - 若将来换成真正裁剪 Timeline 的 AAR，该判据会自动让引擎重新托管片尾。
   - `sync-upstream.yml` 的 `Verify custom patches` 已加对应 grep 校验。

## 同步步骤

1. 运行 `Sync upstream`；若为绿且触发构建，检查 `Build Leanback APK` 结果即可。
2. 若出现冲突 issue：本地合并后按上面清单逐项保留自定义行为，再 push 回 `fongmi`。
3. 合并后若 `gradle/libs.versions.toml` 变化，按下一节确认 AAR 是否要刷新。

## 刷新预编译 AAR

1. 通过 `https://api.github.com/repos/FongMi/TV/forks?sort=newest` / `sort=stargazers` 找提交了 `app/libs/lib-*.aar` 的镜像 fork，下载全部 `*.aar`。
2. 校验关键符号：
   - `lib-common-release.aar` 含 `androidx/media3/common/util/TrackSelectionUtil`
   - `lib-mpvplayer-release.aar` 含 `setAudioFilterListener`
   - 新增 API 是否存在，例如 `addMotionInterpolationListener`、`setMotionInterpolationFps`
3. 上传到本仓库 Release `aar`（同名资产覆盖），再触发构建。
4. 若新 AAR 提供了原生运动补偿 API，可移除 `MpvMotionSupport` 的反射路径，改用原生方法。

## 签名

- CI 优先使用仓库 Secrets：`SIGNING_KEYSTORE`（keystore 的 Base64）与 `SIGNING_STORE_PASSWORD`；缺失时才临时生成并给出警告。
- 固定签名后，自建版本之间可直接覆盖升级；从更早的临时签名版本切换仍需卸载一次，官方版始终需要先卸载。
- 本地备份：`d:\documents\trae\fongmi\signing\fongmi-tv-release.jks`（alias `tv`）。丢失后无法再签出可覆盖升级的版本。
- 验证方式：`keytool -list -v` 得到的证书 SHA256 应与 APK 签名块中的证书 SHA256 一致。

## 版本与发布

- 版本号规则：`<上游版本>-<自然数>`，序号从 1 起，**每成功构建一次递增 1**，
  例 `5.8.7-1`、`5.8.7-2`、`5.8.7-3`……；上游升级后序号自动从 1 重算。
- 单一来源：`gradle.properties` 的 `tvBaseVersion`；`app/build.gradle` 的
  `versionName` 优先取 CI 注入的 `TV_VERSION_NAME`，本地构建回落到 `tvBaseVersion`。
  `versionCode` 跟随上游（当前 587），由同步工作流自动维护，保证同签名可直接覆盖安装。
- `build-tv.yml` 的 `Compute version` 用 `git ls-remote --tags` 找最大序号 +1；
  `Publish release` 会 `gh release create "$VERSION" --target "$GITHUB_SHA"` 创建
  **新 tag 与新 Release**（tag 指向当次 commit），附两个 ABI 的 APK。
- 仓库**不维护 `leanback-latest` 之类的滚动别名**：那种做法会让 tag / target_commitish /
  published_at 一直停在首次创建的 commit，页面看起来没有更新，也无法回溯版本。
  已发布的历史版本不覆盖，下载链接一律写具体版本号 `releases/download/5.8.7-N/...`。
- Release 说明必须包含**本次变动内容**：`Publish release` 用 `gh api
  repos/$GITHUB_REPOSITORY/compare/$PREV...$GITHUB_SHA` 取相对上一个版本 tag 的
  提交列表（最多 20 条，格式 `- 提交标题 (短 SHA)`），并附
  `https://github.com/$GITHUB_REPOSITORY/compare/$PREV...$GITHUB_SHA` 差异链接作为兜底；
  首个版本没有 PREV，改为附 commit 链接。`Compute version` 同时输出 `prev`。
- 其余说明字段：commit、构建时间（**北京时间 UTC+8**）、versionName/versionCode、
  资产清单；**不要写签名信息**（签名细节放在本文件的「签名」章节，不进发布说明）。
- 上游升级时 `sync-upstream.yml` 的 `Sync version metadata` 会自动读取上游
  `versionName` / `versionCode` 并更新上述两处，**不要手工补号**。

## 验证

- `Build Leanback APK` 为绿，出现新的 `5.8.7-N` Release：tag 指向当次 commit，
  两个 APK 资产齐全，说明里的构建时间为北京时间。
- 安装后检查：标题下分辨率、控制栏选集按钮、详情页多排选集、搜索页左侧竖排站点、mpv 播放设置中的运动补偿。
- 运动补偿前提：设备支持 Vulkan 1.2；面板显示的目标帧率不高于实际刷新率。
- 可在 APK 的 dex 中检查 `playbackProperties`、`setStringOptionOrProperty` 是否存在，确认混淆保留生效。

## 常见失败

- `platforms;android-37` 找不到：runner 预装的是 `android-37.0/37.1/37.2`，工作流已软链为 `android-37`；不要降级 compileSdk（依赖要求 37）。
- `verifyLeanbackReleaseMpvNative` 失败：AAR 的 `libmpv.so` 未使用统一 `libffmpeg.so`，应换 AAR；临时 `-x` 只能用于排查，不要用于发布。
- `isMpvInitialized()` 非 public：外部包只能用 `MpvPlayer.isAvailable()`。
- 反射在 release 版失效：检查 `proguard-rules.pro` 的 keep 规则是否被上游覆盖。
- 同步后 push 未触发构建：执行 `gh workflow run build-tv.yml --ref fongmi`。
