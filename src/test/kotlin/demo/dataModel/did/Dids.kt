package demo.dataModel.did

import core.SheetBind
import core.SheetDataType

/**
 * 动态 (只读) DID
 * 通过不同的 @SheetBind 注解区分类型
 */
@SheetBind(sheetName = "DynamicData DID", pattern = "\\s*(Dynamic|dynamic|DYNAMIC)\\s*(Data|data|DATA)\\s*(Did|DID|did)\\s*", sheetDataType = SheetDataType.Dictionary)

class DynamicDataDid() : Did() {
}

/**
 * 下线检测DID
 */
@SheetBind(sheetName = "EOL Config DID", pattern = "\\s*(EOL|eol|Eol)\\s*(Config|config|CONFIG)\\s*(DID|did|Did)", sheetDataType = SheetDataType.Dictionary)
class EOLDid() : Did() {
}

/**
 * 静态 (储存) DID
 */
@SheetBind(sheetName = "StoredData DID", pattern = "\\s*(Stored|stored|STORED)\\s*(Data|data|DATA)\\s*(DID|Did|did)\\s*", sheetDataType = SheetDataType.Dictionary)
class StoredDataDid() : Did() {
}