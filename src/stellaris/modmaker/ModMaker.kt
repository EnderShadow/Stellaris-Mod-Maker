package stellaris.modmaker

import javafx.application.Application
import javafx.collections.FXCollections
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import json.JSONArray
import json.JSONObject
import json.JSONParser
import stellaris.modmaker.gui.Controller
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*

fun main(args: Array<String>)
{
    Application.launch(ModMaker::class.java, *args)
}

class ModMaker: Application()
{
    override fun start(primaryStage: Stage)
    {
        val loader = FXMLLoader(javaClass.getResource("gui/ModMaker.fxml"))
        val root = loader.load<Parent>()
        val controller = loader.getController<Controller>()
        controller.window = primaryStage
        primaryStage.title = "Stellaris Mod Maker"
        primaryStage.scene = Scene(root)
        primaryStage.show()
    }
}

enum class ModType(val tag: String)
{
    SPECIES("Species"),
    MUSIC("Music"),
    PORTRAITS("Graphics"),
    TRAITS("Traits"),
    SYSTEM_INITIALIZERS("Galaxy Generation"),
    GRAPHICS("Graphics"),
    FLAGS("Graphics"),
    PRESCRIPTED_COUNTRIES("Galaxy Generation"),
    ASCENSION_PERKS("Government"),
    CIVICS("Government")
}

class Mod(var name: String, var modID: String = "", val modType: MutableSet<ModType> = mutableSetOf(), val uniqueID: UUID = UUID.randomUUID())
{
    companion object
    {
        fun loadMod(saveLoc: File): Mod
        {
            val jsonData = JSONParser.parse(saveLoc) as JSONObject
            val name = jsonData.string("name")!!
            val folderName = jsonData.string("folderName")
            val modID = jsonData.string("modID")!!
            val uniqueID = UUID.fromString(jsonData.string("UUID"))
            val image = if(!jsonData.isNull("image")) File(jsonData.string("image")) else null
            val supportedVersion = jsonData.string("supported version")!!
            val modType = jsonData.jsonArray("types").map {ModType.valueOf(it as String)}.toMutableSet()
            
            val mod = Mod(name, modID, modType, uniqueID)
            mod.image = image
            mod.supportedVersion = supportedVersion
            mod.folderName = folderName
            
            // load modID from stellaris mod file if it exists
            if(mod.modID.isEmpty())
            {
                val modFile = File(modDirectory, "${mod.folderName}.mod")
                if(modFile.exists())
                {
                    val newModID = modFile.readLines().find {it.startsWith("remote_file_id")} ?: ""
                    if(newModID.isNotEmpty())
                        mod.modID = newModID.substringBeforeLast("\"").substringAfterLast("\"")
                }
            }
            
            val components = jsonData.jsonArray("components").map {ModComponent.fromJSON(mod, it as JSONObject)}
            mod.modComponents.addAll(components)
            
            return mod
        }
    }
    
    var folderSet = false
    var folderName: String? = null
        set(value)
        {
            field = if(value == null || value.isEmpty()) null else value
            folderSet = field?.let {true} ?: false
        }
        get()
        {
            return field?.let {field} ?: name.toLowerCase().replace(" ", "")
        }
    var image: File? = null
    var supportedVersion = ""
    val modComponents = mutableListOf<ModComponent>()
    
    fun saveMod(saveLoc: File)
    {
        val mod = JSONObject()
        mod["name"] = name
        mod["folderName"] = if(folderSet) folderName else null
        mod["modID"] = modID
        mod["UUID"] = uniqueID.toString()
        mod["image"] = image?.path
        mod["supported version"] = supportedVersion
        mod["types"] = JSONArray(modType.toList().map {it.toString()})
        mod["components"] = JSONArray(modComponents.map {it.toJSON()})
        Files.write(saveLoc.toPath(), listOf(mod.toString()), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
    }
    
    fun createMod()
    {
        val modLoc = File(modDirectory, folderName)
        modLoc.mkdirs()
        
        image?.copyTo(File(modLoc, "cover.${image?.extension}"), true)
        
        val descriptor = File(modLoc, "descriptor.mod")
        descriptor.createNewFile()
        
        // write data to descriptor file
        var text = StringBuilder()
        text.append("name=\"$name\"\n")
        text.append("path=\"mod/$folderName\"\n")
        text.append("tags={\n")
        modType.map {it.tag}.toSortedSet().forEach {text.append("\t\"$it\"\n")}
        text.append("}\n")
        if(image != null)
            text.append("picture=\"cover.${image?.extension}\"\n")
        text.append("supported_version=\"$supportedVersion\"\n")
        var bos = BufferedOutputStream(FileOutputStream(descriptor))
        bos.write(text.toString().toByteArray())
        bos.close()
        
        val modFile = File(modDirectory, "$folderName.mod")
        if(modID.isEmpty() && modFile.exists())
        {
            modID = modFile.readLines().find {it.startsWith("remote_file_id")} ?: ""
            if(modID.isNotEmpty())
                modID = modID.substringBeforeLast("\"").substringAfterLast("\"")
        }
    
        // write data to mod file
        text = StringBuilder()
        text.append("name=\"$name\"\n")
        text.append("path=\"mod/$folderName\"\n")
        text.append("tags={\n")
        modType.map {it.tag}.toSortedSet().forEach {text.append("\t\"$it\"\n")}
        text.append("}\n")
        if(image != null)
            text.append("picture=\"cover.${image?.extension}\"\n")
        if(modID.isNotEmpty())
            text.append("remote_file_id=\"$modID\"\n")
        text.append("supported_version=\"$supportedVersion\"\n")
        bos = BufferedOutputStream(FileOutputStream(modFile))
        bos.write(text.toString().toByteArray())
        bos.close()
        
        modComponents.forEach {it.createModComponent(modLoc)}
    }
}

interface ModComponent
{
    companion object
    {
        fun createComponent(type: ModType, mod: Mod): ModComponent
        {
            return when(type)
            {
                ModType.SPECIES -> TODO()
                ModType.MUSIC -> MusicComponent(mod)
                ModType.PORTRAITS -> TODO()
                ModType.TRAITS -> TODO()
                ModType.SYSTEM_INITIALIZERS -> TODO()
                ModType.GRAPHICS -> TODO()
                ModType.FLAGS -> TODO()
                ModType.PRESCRIPTED_COUNTRIES -> TODO()
                ModType.ASCENSION_PERKS -> TODO()
                ModType.CIVICS -> TODO()
            }
        }
        
        fun fromJSON(mod: Mod, obj: JSONObject): ModComponent
        {
            val type = ModType.valueOf(obj.string("type")!!)
            return when(type)
            {
    
                ModType.SPECIES -> TODO()
                ModType.MUSIC -> {
                    val musicComponent = MusicComponent(mod)
                    obj.jsonArray("songs").forEach { it as JSONObject
                        val location = File(it.string("location"))
                        val name = it.string("name")!!
                        val volume = it.number("volume") as Double
                        musicComponent.songs.add(MusicComponent.Song(location, name, volume))
                    }
                    musicComponent
                }
                ModType.PORTRAITS -> TODO()
                ModType.TRAITS -> TODO()
                ModType.SYSTEM_INITIALIZERS -> TODO()
                ModType.GRAPHICS -> TODO()
                ModType.FLAGS -> TODO()
                ModType.PRESCRIPTED_COUNTRIES -> TODO()
                ModType.ASCENSION_PERKS -> TODO()
                ModType.CIVICS -> TODO()
            }
        }
    }
    
    val type: ModType
    fun toJSON(): JSONObject
    fun createModComponent(modLoc: File)
    fun deleteModComponent(modLoc: File)
}

class MusicComponent(val mod: Mod): ModComponent
{
    override val type = ModType.MUSIC
    
    val songs = FXCollections.observableArrayList<Song>()
    
    val path get() = "music/${mod.folderName}"
    
    data class Song(val songLocation: File, var songName: String = songLocation.nameWithoutExtension, var volume: Double = 0.5)
    
    override fun toJSON(): JSONObject
    {
        val component = JSONObject()
        component["type"] = type.toString()
        component["songs"] = JSONArray(songs.map {JSONObject(mapOf("location" to it.songLocation.path, "name" to it.songName, "volume" to it.volume))})
        return component
    }
    
    override fun createModComponent(modLoc: File)
    {
        val musicLoc = File(modLoc, path)
        File(modLoc, "music").deleteRecursively()
        musicLoc.mkdirs()
        
        songs.map {it.songLocation}.forEach {it.copyTo(File(musicLoc, it.name))}
        
        val assetFile = File(modLoc, "music/${mod.folderName}.asset")
        assetFile.createNewFile()
        val songFile = File(modLoc, "music/${mod.folderName}.txt")
        songFile.createNewFile()
        
        val assetWriter = BufferedOutputStream(FileOutputStream(assetFile))
        val songWriter = BufferedOutputStream(FileOutputStream(songFile))
        for(song in songs)
        {
            assetWriter.write("music = {\n".toByteArray())
            assetWriter.write("\tname = \"${song.songName}\"\n".toByteArray())
            assetWriter.write("\tfile = \"${mod.folderName}/${song.songLocation.name}\"\n".toByteArray())
            assetWriter.write("\tvolume = ${song.volume}\n".toByteArray())
            assetWriter.write("}\n\n".toByteArray())
    
            songWriter.write("song = {\n".toByteArray())
            songWriter.write("\tname = \"${song.songName}\"\n".toByteArray())
            songWriter.write("}\n\n".toByteArray())
        }
        assetWriter.close()
        songWriter.close()
    }
    
    override fun deleteModComponent(modLoc: File)
    {
        File(modLoc, "music").deleteRecursively()
    }
}