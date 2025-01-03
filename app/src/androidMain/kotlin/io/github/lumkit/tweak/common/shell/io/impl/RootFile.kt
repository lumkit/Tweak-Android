package io.github.lumkit.tweak.common.shell.io.impl

import io.github.lumkit.tweak.common.shell.io.TweakFile
import io.github.lumkit.tweak.common.shell.provide.ReusableShells
import io.github.lumkit.tweak.common.util.firstLine
import java.io.File

class RootFile : TweakFile {

    constructor(path: String) : super(path)
    constructor(file: TweakFile) : super(file)
    constructor(file: TweakFile, child: String) : super(file, child)

    override val path: String
        get() = _file.path

    override suspend fun exists(): Boolean =
        ReusableShells.execSync("[ -e \"${path}\" ] && echo 1 || echo 0").firstLine().firstLine() == "1"

    override suspend fun getParent(): String = _file.parent ?: ""

    override suspend fun getParentFile(): TweakFile = RootFile(getParent())

    override suspend fun canRead(): Boolean =
        ReusableShells.execSync("[ -r \"${path}\" ] && echo 1 || echo 0").firstLine().firstLine() == "1"

    override suspend fun canWrite(): Boolean =
        ReusableShells.execSync("[ -w \"${path}\" ] && echo 1 || echo 0").firstLine() == "1"

    override suspend fun isDirectory(): Boolean =
        ReusableShells.execSync("[ -d \"${path}\" ] && echo 1 || echo 0").firstLine() == "1"

    override suspend fun isFile(): Boolean =
        ReusableShells.execSync("[ -f \"${path}\" ] && echo 1 || echo 0").firstLine() == "1"

    override suspend fun lastModified(): Long = try {
        ReusableShells.execSync("stat -c '%Y' \"${path}\"").firstLine().toLong()
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }

    override suspend fun length(): Long = try {
        ReusableShells.execSync("stat -c '%s' \"${path}\"").firstLine().toLong()
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }

    override suspend fun createNewFile(): Boolean =
        ReusableShells.execSync("[ ! -e \"${path}\" ] && echo -n > \"${path}\" && echo 1 || echo 0").firstLine() == "1"

    override suspend fun delete(): Boolean =
        ReusableShells.execSync("(rm -rf \"${path}\" || rmdir -f \"${path}\") && echo 1 || echo 0").firstLine() == "1"

    override suspend fun list(): Array<String> {
        if (!isDirectory())
            return arrayOf()
        val cmd = "ls -a \"${path}\""

        val list = ArrayList(ReusableShells.execSync(cmd).split("\n"))
        val iterator = list.listIterator()

        while (iterator.hasNext()) {
            val name: String = iterator.next()
            if (name == "." || name == ".." || name.isEmpty() || name.isBlank()) {
                iterator.remove()
            }
        }

        return list.map { "${path}/${it}" }.toTypedArray()
    }

    override suspend fun list(filter: (String) -> Boolean): Array<String> = list().filter { filter(it) }.toTypedArray()

    override suspend fun listFiles(): Array<TweakFile> = list().map { RootFile(it) }.toTypedArray()

    override suspend fun listFiles(filter: (TweakFile) -> Boolean): Array<TweakFile> = listFiles().filter { filter(it) }.toTypedArray()

    override suspend fun mkdirs(): Boolean = ReusableShells.execSync("mkdir -p \"${path}\" && echo 1 || echo 0").firstLine() == "1"

    override suspend fun renameTo(dest: String): Boolean {
        val cmd = "mv -f \"${path}\" \"${(if (_file.parentFile == null) File(dest) else File(_file.parentFile, dest)).absolutePath}\" && echo 1 || echo 0"
        return ReusableShells.execSync(cmd).firstLine() == "1"
    }

    suspend fun clear(): Boolean {
        val cmd = "(echo -n > \"${path}\") && echo 1 || echo 0"
        return ReusableShells.execSync(cmd).firstLine() == "1"
    }
}