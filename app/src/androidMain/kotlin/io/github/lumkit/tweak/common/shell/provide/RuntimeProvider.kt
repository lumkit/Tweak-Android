package io.github.lumkit.tweak.common.shell.provide

object RuntimeProvider {
    private var workPath: String = ""
    private var defaultWorkPath: String = ""

    fun setWorkPath(path: String) {
        workPath = path
    }

    private fun smartWorkPath(): String? {
        if (workPath.isNotBlank()) {
            if (defaultWorkPath.isBlank()) {
                defaultWorkPath = try {
                    Runtime.getRuntime().exec("sh").let {
                        it.outputStream.use { os ->
                            os.write("echo \$PATH".toByteArray())
                            os.flush()
                        }

                        it.inputStream.use { input ->
                            val cache = ByteArray(16384)
                            val len = input.read(cache)
                            String(cache, 0, len).trim().ifBlank {
                                throw RuntimeException("未能获取到\$PATH参数")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    "/sbin:/system/sbin:/system/bin:/system/xbin:/odm/bin:/vendor/bin:/vendor/xbin"
                }
            }

            return "PATH=$defaultWorkPath:$workPath"
        }
        return null
    }

    fun display() {
        println(smartWorkPath())
    }

    /**
     * 获取Process
     */
    fun getProcess(run: String, redirectErrorStream: Boolean = false): Process {
        val path = smartWorkPath()
        val process = ProcessBuilder()
            .command(run)
            .redirectErrorStream(redirectErrorStream)
            .start()
        if (path != null) {
            val outputStream = process.outputStream
            outputStream.write("export ".toByteArray())
            outputStream.write(path.toByteArray())
            outputStream.write("\n".toByteArray())
            outputStream.flush()
        }
        return process
    }
}