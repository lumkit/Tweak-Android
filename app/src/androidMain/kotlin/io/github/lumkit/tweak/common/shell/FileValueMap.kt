package io.github.lumkit.tweak.common.shell

import io.github.lumkit.tweak.common.shell.provide.ReusableShells

object FileValueMap {
    suspend fun mapFileValue(dir: String): HashMap<String, String> {
        val hashMap = HashMap<String, String>()

        val cmds = StringBuilder("if [[ -e $dir ]]; then\n")
        cmds.append("cd $dir\n")
        cmds.append("for item in `ls`\n" +
                "do\n" +
                "\tif [[ -f \$item ]] then\n" +
                "\t\techo \"\$item:`cat \$item`\"\n" +
                "\t\techo '----'\n" +
                "\tfi\n" +
                "done\n")
        cmds.append("\nfi\n")
        val content = ReusableShells.execSync(cmds.toString())
        if (content != "error") {
            val rows = content.split("----")
            for (row in rows) {
                val item = row.trim()
                if (item.isNotEmpty() && item.indexOf(":") > -1) {
                    val end = item.indexOf(":")
                    val name = item.substring(0, end)
                    if (end < item.length - 1) {
                        val value = item.substring(end + 1)
                        hashMap[name] = value
                    } else {
                        hashMap[name] = ""
                    }
                }
            }
        }
        return hashMap
    }
}