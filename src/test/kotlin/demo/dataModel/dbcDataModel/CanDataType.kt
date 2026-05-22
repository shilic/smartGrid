package demo.dataModel.dbcDataModel

import io.github.shilic.smartGrid.core.GridColumnBind

enum class CanDataType {
    @GridColumnBind(headerText = "Unsigned", pattern = "Hex|hex|HEX|无符号|((Hex|hex|HEX)?\\s*[(（]?\\s*(Unsigned|unsigned|UNSIGNED)\\s*[)）]?)")
    UNSIGNED,
    @GridColumnBind(headerText = "Singed", pattern = "有符号|(?<!(Un|un|UN))Signed|(?<!(Un|un|UN))signed|(?<!(Un|un|UN))SIGNED")
    SIGNED
}