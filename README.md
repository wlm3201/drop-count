# Drop Count

物品实体上方显示名称与数量的客户端 Fabric 模组。

同一物品多个实体合并计数，不同物品堆叠标签上下错开。

## 用法

默认 `O` 键开关，在「选项 → 控制 → Drop Count」里修改，切换时会提示当前状态。

## 构建

一份源码，每个游戏版本一个 Gradle 子项目，共用 `src/`：

```
src/                                共享源码
gradle/mod.gradle                   各版本共用的构建逻辑
versions/<游戏版本>/build.gradle     该版本的 MC / Fabric API / 版本范围
versions/<游戏版本>/src/             仅该版本需要的代码（目前只有 compat/Platform.java）
```

```bash
./gradlew build           # 构建所有版本，产物在 versions/*/build/libs/
./gradlew :v26_2:build    # 只构建一个版本
```

## 许可证

[CC0-1.0](LICENSE)。
