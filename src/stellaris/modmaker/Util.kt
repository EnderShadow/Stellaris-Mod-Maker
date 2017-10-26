package stellaris.modmaker

import com.sun.javafx.PlatformUtil
import java.io.File

val modDirectory: File
    get() = when
    {
        PlatformUtil.isWindows() ->
        {
            val p = Runtime.getRuntime().exec("reg query \"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Shell Folders\" /v personal")
            p.waitFor()
            val inputStream = p.inputStream
            val documents = File(String(inputStream.readBytes(inputStream.available())).split(Regex("\\s{2,}"))[4])
            File(documents, "Paradox Interactive/Stellaris/mod")
        }
        PlatformUtil.isLinux() -> File("~/.local/share/Paradox Interactive/Stellaris/mod")
        PlatformUtil.isMac() -> File("~/Documents/Paradox Interactive/Stellaris/mod")
        else -> File("")
    }