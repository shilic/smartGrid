# smart-grid

更聪明的表格——基于 Kotlin 反射与 Apache POI 的 Excel 双向映射库。将 Excel 表格解析为 Kotlin 数据对象。未来也会支持将对象写回 Excel。

## 环境依赖

- JDK 8+
- Kotlin 1.9+

## 引入依赖

```kotlin
// settings.gradle.kts
repositories {
    mavenCentral()
    maven("https://maven.pkg.github.com/shilic/smartGrid") {
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: ""
            password = System.getenv("GITHUB_TOKEN") ?: ""
        }
    }
}

// build.gradle.kts
dependencies {
    implementation("io.github.shilic:smart-grid:1.0.1")
}
```

## 使用说明

### 1. 定义数据模型

使用`GridSheetBind`注解标注这个数据来自于哪一个表格，使用`GridColumnBind`标注表格的列标题。

```kotlin
@GridSheetBind(sheetName = "Ecu Info",  pattern = "零部件信息|((Ecu|ecu|ECU)\\\\s*(Info|info|INFO))", gridSheetType = GridSheetType.Single)
class Ecu : IGridRowData {
    // ==================== 2. 零部件基础信息 ====================
    @GridColumnBind(headerText = "零部件名称", pattern = "零部件名称", valueType =  GridValueType.Text, keyword = true)
    var ecuName: String = ""
    @GridColumnBind(headerText = "零部件类型", pattern = "零部件类型",valueType =  GridValueType.Text)
    var ecuType: String = ""

    // ==================== 3. 零部件诊断ID信息 ====================
    @GridColumnBind("物理寻址", "物理寻址", GridValueType.HexNumber)
    var diagRequestId: UInt = 0u
    @GridColumnBind("物理响应", "物理响应", GridValueType.HexNumber)
    var diagResponseId: UInt = 0u
    @GridColumnBind("功能寻址", "功能寻址", GridValueType.HexNumber)
    var functionalRequestId: UInt = 0u
    @GridColumnBind("CAN帧类型", "CAN帧类型", GridValueType.Enum)
    var canFrameType: CanFrameType = CanFrameType.Default

    // 支持多表查询和嵌套解析
    @GridColumnBind(headerText = "DTCList", valueType = GridValueType.OtherSheet)
    var dtcDefineMap : MutableMap<String, Dtc> = mutableMapOf()
    @GridColumnBind(headerText = "EOLConfigDID", valueType = GridValueType.OtherSheet)
    var eolDidMap : MutableMap<String, EOLDid> = mutableMapOf()
}

@GridSheetBind(pattern = "DID表")
class Did: IGridRowData{
    @GridColumnBind(headerText = "DID号", pattern = "DID号", valueType = GridValueType.HexNumber, keyword = true)
    var didNumber: UShort = 0u

    @GridColumnBind(headerText = "DID名称", pattern = "DID名称", valueType = GridValueType.Text, uiIgnore = true)
    var didName: String = ""
    // 支持嵌套解析
    @GridColumnBind(headerText = "子数据名称", pattern = "子数据名称", valueType = GridValueType.SubSignal)
    var signalMap: MutableMap<String, DidSignal> = mutableMapOf()
}
```

### 2. 读取 Excel → 对象

```kotlin
val reader = GridReader(poiWorkbook("path/to/file.xlsx"))
val items: Map<String, Ecu> = reader.parse(Ecu::class)
```

### 3. 写入对象 → Excel

未来将会支持

## 支持的值类型

| 类型 | 说明 |
|------|------|
| `TEXT` | 普通文本 |
| `NUMBER` | 十进制数值 |
| `HexNumber` | 十六进制数值 |
| `ENUM_TYPE` | 枚举类型 |
| `Bool` | 布尔值 |
| `Strings` | 文本集合 |
| `ValueTable` | 值描述（键值对） |
| `SubSignal` | 嵌套子信号 |
| `SubStructure` | 嵌套子结构 |
| `OtherSheet` | 跨表格引用 |
| `SpecificSheet` | 特定表格引用 |
| `Custom` | 自定义适配器 |

## 目录结构

```
├── README.md
├── build.gradle.kts              // 构建配置
├── src
│   ├── main/kotlin
│   │   ├── core
│   │   │   ├── GridColumnBind.kt // @GridColumnBind 注解与元数据
│   │   │   ├── GridReader.kt     // 表格读取器
│   │   │   ├── GridWriter.kt     // 表格写入器（未实现）
│   │   │   ├── GridInterfaces.kt // 核心接口定义
│   │   │   ├── GridSheetBind.kt  // @GridSheetBind 注解
│   │   │   ├── GridSheetType.kt  // 表格类型枚举
│   │   │   ├── GridValueType.kt  // 13 种值类型实现
│   │   │   ├── IGridValueAdapter.kt // 值适配器接口
│   │   │   ├── poiWorkbook.kt    // Workbook 工厂
│   │   │   └── Ref.kt            // 可变引用包装
│   │   ├── exception
│   │   │   └── ExcelException.kt // 异常定义
│   │   └── utils
│   │       ├── ExcelConverter.kt // Excel 格式转换
│   │       ├── ExcelExtensions.kt// POI 扩展函数
│   │       ├── FileUtils.kt      // 文件工具
│   │       ├── TypeExtentions.kt // 类型扩展
│   │       └── tool.kt           // 反射工具
│   └── test/kotlin
│       └── demo
│           ├── MainTest.kt       // 端到端测试
│           └── dataModel         // 测试用数据模型
```

## 版本更新

### v1.0.0
- 首次发布，实现 Excel → Kotlin 对象的反射解析
- 支持 13 种值类型的双向解析
- 支持子信号、跨表格嵌套
- 新增：源码包与 Javadoc 包发布
- 新增：`IGridSpecificSheet` 接口支持跨表格引用
- 优化：`GridColumnBind` 新增 `order` 属性控制列排序

## 许可证

Apache License 2.0

## 作者

诚 — 985478238@qq.com
