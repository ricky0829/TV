---
name: fongmi-tv-upstream-sync
description: 把 FongMi/TV 上游更新同步到 ricky0829/TV 的 fongmi 分支，解决冲突、刷新预编译 media3 AAR、复核四项 TV 界面改动与 mpv 运动补偿兼容层，并触发 GitHub Actions 编译发布 APK。当用户提到同步上游、跟随上游更新、上游升级后重新打包时使用。
---

# FongMi TV 上游同步

把 `FongMi/TV` 的 `fongmi` 分支更新合并进本 fork，并重新产出可用 APK。

- 仓库：`ricky0829/TV`，分支 `fongmi`
- 本地：`d:\documents\trae\fongmi\TV-fongmi`
- Release：`aar`（预编译 AAR）、`leanback-latest`（两个 APK）

## 自动化链路

- `.github/workflows/sync-upstream.yml`：每天 03:23（北京时间）抓取上游并合并。冲突时创建 issue 并中止；成功则 push，并显式执行 `gh workflow run build-tv.yml --ref fongmi`。
- 注意：用仓库自带 `GITHUB_TOKEN` 推送不会触发 `push` 事件，所以同步工作流必须显式 dispatch 构建。
- `.github/workflows/build-tv.yml`：从 Release `aar` 下载全部 `*.aar`，构建 leanback APK，上传 artifact，并覆盖 Release `leanback-latest` 的两个 APK。
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

## 验证

- `Build Leanback APK` 为绿，Release `leanback-latest` 两个 APK 更新时间变化。
- 安装后检查：标题下分辨率、控制栏选集按钮、详情页多排选集、搜索页左侧竖排站点、mpv 播放设置中的运动补偿。
- 运动补偿前提：设备支持 Vulkan 1.2；面板显示的目标帧率不高于实际刷新率。
- 可在 APK 的 dex 中检查 `playbackProperties`、`setStringOptionOrProperty` 是否存在，确认混淆保留生效。

## 常见失败

- `platforms;android-37` 找不到：runner 预装的是 `android-37.0/37.1/37.2`，工作流已软链为 `android-37`；不要降级 compileSdk（依赖要求 37）。
- `verifyLeanbackReleaseMpvNative` 失败：AAR 的 `libmpv.so` 未使用统一 `libffmpeg.so`，应换 AAR；临时 `-x` 只能用于排查，不要用于发布。
- `isMpvInitialized()` 非 public：外部包只能用 `MpvPlayer.isAvailable()`。
- 反射在 release 版失效：检查 `proguard-rules.pro` 的 keep 规则是否被上游覆盖。
- 同步后 push 未触发构建：执行 `gh workflow run build-tv.yml --ref fongmi`。
