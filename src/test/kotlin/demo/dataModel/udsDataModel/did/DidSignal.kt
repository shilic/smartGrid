package demo.dataModel.udsDataModel.did

import io.github.shilic.smartGrid.core.GridColumnBind
import io.github.shilic.smartGrid.core.GridSheetBind
import io.github.shilic.smartGrid.core.GridSheetType
import io.github.shilic.smartGrid.core.GridValueType
import io.github.shilic.smartGrid.core.IMutableGridRowData
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * DID 子信号 - 使用数据类
 * 现在所有类型都使用枚举，代码更简洁
 */
@GridSheetBind(gridSheetType = GridSheetType.SubSignal)
class DidSignal: IMutableGridRowData {
    // 必选参数
    @GridColumnBind(headerText = "子数据名称", pattern = "子数据名称", valueType = GridValueType.Text, uiIgnore = true, keyword = true)
    var signalName: String = ""

    @GridColumnBind(headerText = "子数据描述", pattern = "子数据描述", valueType = GridValueType.Text)
    var signalDescription: String = ""

    @GridColumnBind(headerText = "子数据起始位", pattern = "子数据起始位", valueType = GridValueType.NumberType, uiIgnore = true)
    var startBit: Int = 0

    @GridColumnBind(headerText = "子数据长度", pattern = "子数据长度", valueType = GridValueType.NumberType, uiIgnore = true)
    var bitLength: Int = 0

    @GridColumnBind(headerText = "子数据排列格式", pattern = "排列格式", valueType = GridValueType.Enumeration, uiIgnore = true)
    var byteOrder: CanByteOrder = CanByteOrder.INTEL

    @GridColumnBind(headerText = "子数据编码格式", pattern = "编码格式", valueType = GridValueType.Enumeration, uiIgnore = true)
    var didCodeFormat: DIDCodeFormat = DIDCodeFormat.UNSIGNED

    @GridColumnBind(headerText = "子数据精度", pattern = "精度", valueType = GridValueType.NumberType, uiIgnore = true)
    var factor: Double = 1.0

    @GridColumnBind(headerText = "子数据偏移量", pattern = "偏移量", valueType = GridValueType.NumberType, uiIgnore = true)
    var offset: Double = 0.0

    // 可选参数
    @GridColumnBind(headerText = "子数据单位", pattern = "单位", valueType = GridValueType.Text, uiIgnore = true)
    var unit: String? = null

    @GridColumnBind(headerText = "子数据值描述", pattern = "值描述", valueType = GridValueType.ValueTable, uiIgnore = true)
    var valueTable: MutableMap<Int, String> = mutableMapOf()
    // 计算属性 - 使用表达式体函数
    val sizeBytes: Int get() = (bitLength + 7) / 8

    // 编码/解码时的变量 - 使用委托属性进行线程安全访问
    @Transient
    private var _rawBytes: ByteArray = byteArrayOf(0)

    @Transient
    private var _didText: String = ""

    @Transient
    private var _phyValue: Double = 0.0

    // 线程安全访问 - 使用读写锁
    @Transient
    private val lock = ReentrantReadWriteLock()

    var rawBytes: ByteArray
        get() = lock.read { _rawBytes }
        set(value) = lock.write { _rawBytes = value }

    var didText: String
        get() = lock.read { _didText }
        set(value) = lock.write { _didText = value }

    var phyValue: Double
        get() = lock.read { _phyValue }
        set(value) = lock.write { _phyValue = value }

    // IGridData 接口实现
    override val gridKey: String get() = signalName
    override var gridFather: String = ""
    override var gridRowIndex: Int? = null

    override fun toString(): String = "[子数据, 名称: $signalName, 描述: $signalDescription]"
}