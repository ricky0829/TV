Fork of https://github.com/FongMi/TV.

本分支仅供 TV 版使用，解决了以下问题：
1. 播放页标题下恢复分辨率显示
2. 播放控制栏恢复“选集”按钮
3. 详情页选集改为多排
4. 搜索结果页站点改为左侧竖排

## 版本与发布规则

版本号格式：`<上游版本>-<序号>`，序号为自然数，从 1 起，**每成功构建一次递增 1**。
例如上游 5.8.7 → `5.8.7-1`、`5.8.7-2`、`5.8.7-3` ……

每次 push 到 `fongmi` 触发的成功构建会：

1. 读取 `gradle.properties` 的 `tvBaseVersion`，扫描远端已有的 `5.8.7-*` tag 取最大序号 +1；
2. 把该版本号注入 `versionName`（`versionCode` 保持上游的 587 不变），用仓库持久签名构建；
3. **创建同名 tag 与 Release，tag 指向当次 commit**，附 `leanback-arm64_v8a.apk` 与
   `leanback-armeabi_v7a.apk` 两个资产；
4. 同步更新滚动别名 `leanback-latest` 的两个资产与说明，固定下载链接保持不变。

使用约定：

- 追溯/回滚具体版本用 `5.8.7-N`；只想拿最新构建用 `leanback-latest`。
- 上游升级（例如 5.8.8）时，只改 `gradle.properties` 的 `tvBaseVersion`，序号自动从 1 重新开始。
- 已发布的历史版本不覆盖；每次修改都是一个新的、可回溯的 Release。
