package demo.dataModel.udsDataModel.did

import io.github.shilic.smartGrid.core.GridSheetBind
import io.github.shilic.smartGrid.core.GridSheetType

/**
 * 动态 (只读) DID
 * 通过不同的 @SheetBind 注解区分类型
 */
@GridSheetBind(sheetName = "DynamicData DID", pattern = "\\s*(Dynamic|dynamic|DYNAMIC)\\s*(Data|data|DATA)\\s*(Did|DID|did)\\s*", gridSheetType = GridSheetType.Dictionary)

class DynamicDataDid() : Did() {
}

/**
 * 下线检测DID
 */
@GridSheetBind(sheetName = "EOL Config DID", pattern = "\\s*(EOL|eol|Eol)\\s*(Config|config|CONFIG)\\s*(DID|did|Did)", gridSheetType = GridSheetType.Dictionary)
class EOLDid() : Did() {
}

/**
 * 静态 (储存) DID
 */
@GridSheetBind(sheetName = "StoredData DID", pattern = "\\s*(Stored|stored|STORED)\\s*(Data|data|DATA)\\s*(DID|Did|did)\\s*", gridSheetType = GridSheetType.Dictionary)
class StoredDataDid() : Did() {
}