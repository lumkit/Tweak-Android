package io.github.lumkit.tweak.common.shell.io

import java.io.File
import java.io.IOException

abstract class TweakFile : Comparator<TweakFile> {

    internal val _file: File

    constructor(path: String) {
        this._file = File(path)
    }
    constructor(file: TweakFile) {
        this._file = file._file
    }
    constructor(file: TweakFile, child: String) {
        this._file = File(file._file, child)
    }

    abstract val path: String
    val name: String
        get() = _file.name

    abstract suspend fun exists(): Boolean
    abstract suspend fun getParent(): String
    abstract suspend fun getParentFile(): TweakFile
    abstract suspend fun canRead(): Boolean
    abstract suspend fun canWrite(): Boolean
    abstract suspend fun isDirectory(): Boolean
    abstract suspend fun isFile(): Boolean
    abstract suspend fun lastModified(): Long
    abstract suspend fun length(): Long

    @Throws(IOException::class)
    abstract suspend fun createNewFile(): Boolean

    abstract suspend fun delete(): Boolean
    abstract suspend fun list(): Array<String>
    abstract suspend fun list(filter: (String) -> Boolean): Array<String>
    abstract suspend fun listFiles(): Array<TweakFile>
    abstract suspend fun listFiles(filter: (TweakFile) -> Boolean): Array<TweakFile>
    abstract suspend fun mkdirs(): Boolean
    abstract suspend fun renameTo(dest: String): Boolean
    override fun compare(o1: TweakFile, o2: TweakFile): Int = o1._file.compareTo(o2._file)
}