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
4. Release 说明只记录 commit、构建时间（**北京时间 UTC+8**）、版本号与资产清单，
   不写签名信息。

使用约定：

- 仓库**不维护滚动别名**，只有版本化 Release。下载地址用具体版本号：
  `https://github.com/ricky0829/TV/releases/download/5.8.7-N/leanback-arm64_v8a.apk`。
- 追溯/回滚只需选对应的 `5.8.7-N`。
- 上游升级（例如 5.8.8）时，只改 `gradle.properties` 的 `tvBaseVersion`，序号自动从 1 重新开始。
- 已发布的历史版本不覆盖；每次修改都是一个新的、可回溯的 Release。
