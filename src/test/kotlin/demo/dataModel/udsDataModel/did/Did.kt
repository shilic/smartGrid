package demo.dataModel.udsDataModel.did

import io.github.shilic.smartGrid.core.GridColumnBind
import io.github.shilic.smartGrid.core.GridSheetBind
import io.github.shilic.smartGrid.core.GridSheetType
import io.github.shilic.smartGrid.core.GridValueType
import io.github.shilic.smartGrid.core.IGridRowData

/**
 * 普通 DID
 * 通过 @SheetBind 注解与表格解析器集成
 */
@GridSheetBind(sheetName = "Did", pattern = "\\s*(Did|DID|did)\\s*", gridSheetType = GridSheetType.Dictionary)
open class Did: IGridRowData {
    // 必选参数
    @GridColumnBind(headerText = "DID号", pattern = "DID号", valueType = GridValueType.HexNumber, keyword = true)
    var didNumber: UShort = 0u

    @GridColumnBind(headerText = "DID名称", pattern = "DID名称", valueType = GridValueType.Text, uiIgnore = true)
    var didName: String = ""

    @GridColumnBind(headerText = "DID描述", pattern = "DID描述", valueType = GridValueType.Text)
    var didDescription: String = ""

    @GridColumnBind(headerText = "字节长度", pattern = "字节长度", valueType = GridValueType.NumberType, uiIgnore = true)
    var sizeBytes: Int = 0

    @GridColumnBind(headerText = "排列格式", pattern = "排列格式", valueType = GridValueType.Enumeration, uiIgnore = true)
    var byteOrder: CanByteOrder = CanByteOrder.INTEL

    @GridColumnBind(headerText = "编码格式", pattern = "编码格式", valueType = GridValueType.Enumeration, uiIgnore = true)
    var didCodeFormat: DIDCodeFormat = DIDCodeFormat.UNSIGNED

    @GridColumnBind(headerText = "DID精度", pattern = "精度", valueType = GridValueType.NumberType, uiIgnore = true)
    var factor: Double = 1.0

    @GridColumnBind(headerText = "DID偏移量", pattern = "偏移量", valueType = GridValueType.NumberType, uiIgnore = true)
    var offset: Double = 0.0

    // 可选参数
    @GridColumnBind(headerText = "DID单位", pattern = "单位", valueType = GridValueType.Text, uiIgnore = true)
    var unit: String? = null


    @GridColumnBind(headerText = "值描述(读取)", pattern = "值描述", valueType = GridValueType.ValueTable, uiIgnore = true)
    var valueTable: MutableMap<Int, String> = mutableMapOf()

    @GridColumnBind(headerText = "存储位置", pattern = "存储位置", valueType = GridValueType.Enumeration, uiIgnore = true)
    var storagePosition: DidStoragePosition = DidStoragePosition.TBD

    @GridColumnBind(headerText = "访问权限", pattern = "访问权限", valueType = GridValueType.Enumeration)
    var didAccessPermission: DIDAccessPermission = DIDAccessPermission.READ_AND_WRITE

    @GridColumnBind(headerText = "27安全等级", pattern = "27安全等级", valueType = GridValueType.Enumeration, uiIgnore = true)
    var securityLevel: SecurityLevel27 = SecurityLevel27.LEVEL1

    // 会话权限
    @GridColumnBind(headerText = "Level1默认会话", pattern = "Level1默认会话", valueType = GridValueType.Enumeration, uiIgnore = true)
    var defaultSessionLevel1: DIDAccessPermission = DIDAccessPermission.READ_AND_WRITE

    @GridColumnBind(headerText = "Level1扩展会话", pattern = "Level1扩展会话", valueType = GridValueType.Enumeration, uiIgnore = true)
    var extendSessionLevel1: DIDAccessPermission = DIDAccessPermission.READ_AND_WRITE

    @GridColumnBind(headerText = "Level2默认会话", pattern = "Level2默认会话", valueType = GridValueType.Enumeration, uiIgnore = true)
    var defaultSessionLevel2: DIDAccessPermission = DIDAccessPermission.READ_AND_WRITE

    @GridColumnBind(headerText = "Level2编程会话", pattern = "Level2编程会话", valueType = GridValueType.Enumeration, uiIgnore = true)
    var programSessionLevel2: DIDAccessPermission = DIDAccessPermission.READ_AND_WRITE

    @GridColumnBind(headerText = "Level2扩展会话", pattern = "Level2扩展会话", valueType = GridValueType.Enumeration, uiIgnore = true)
    var extendSessionLevel2: DIDAccessPermission = DIDAccessPermission.READ_AND_WRITE

    // 子数据
    @GridColumnBind(headerText = "子数据名称", pattern = "子数据名称", valueType = GridValueType.SubSignal)
    var signalMap: MutableMap<String, DidSignal> = mutableMapOf()

    // 编码/解码状态
    @Transient
    var rawBytes: ByteArray = byteArrayOf(0)

    @Transient
    val didText: String = ""

    @Transient
    var phyValue: Double = 0.0
    // IGridData 接口实现
    override val gridKey: String get() = didNumber.toHexString()
    override var gridFather: String = ""
    override var gridRowIndex: Int? = null

    // IValueTable 接口实现

    var aValue: String
        get() = didText
        set(value) { /* 需要在子类中实现可变性 */ }

    // 操作符重载 - 提供类似C#索引器的功能
    operator fun get(signalName: String): DidSignal? = signalMap[signalName]

    // 扩展函数 - 将DID号转为十六进制字符串
    private fun UShort.toHexString(): String = "0x${toString(16).uppercase().padStart(4, '0')}"
}