package utils

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

// ======================== String 扩展 ========================
/**
 * 获取文件扩展名（不带点）
 * 示例: "file.txt" -> "txt"
 */
val String.fileExtension: String get() = File(this).extension.ifBlank { "" }
/**
 * 获取文件扩展名（带点）
 * 示例: "file.txt" -> ".txt"
 */
val String.fileExtensionWithDot: String get() = fileExtension.takeIf { it.isNotBlank() }?.let { ".$it" } ?: ""
/**
 * 获取文件名（包含扩展名）
 * 示例: "/path/to/file.txt" -> "file.txt"
 */
val String.fileName: String get() = File(this).name
/**
 * 获取文件名（不包含扩展名）
 * 示例: "/path/to/file.txt" -> "file"
 */
val String.fileNameWithoutExtension: String get() = File(this).nameWithoutExtension
/**
 * 获取父目录路径
 * 示例: "/path/to/file.txt" -> "/path/to"
 */
val String.parentPath: String get() = File(this).parent ?: ""
/**
 * 获取规范化的路径
 */
val String.normalizedPath: String get() = File(this).canonicalPath

// ======================== 基本检查 ========================
/**
 * 扩展属性版本
 */
val String.absolutePath: String get() =  File(this).absolutePath
val String.exists: Boolean get() = File(this).exists()
val String.isFile: Boolean get() = File(this).isFile
val String.isDirectory: Boolean get() = File(this).isDirectory
val String.isSymlink: Boolean get() = try {
    Files.isSymbolicLink(Paths.get(this))
} catch (e: Exception) {
    false
}