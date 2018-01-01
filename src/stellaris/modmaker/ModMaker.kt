package stellaris.modmaker

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import json.JSONArray
import json.JSONObject
import json.JSONParser
import stellaris.modmaker.gui.Controller
import stellaris.modmaker.gui.loading_screen.LoadingScreenComponent
import stellaris.modmaker.gui.music.MusicComponent
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.reflect.KClass
import kotlin.reflect.full.*

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
        
        registerTab(controller, ModType.MUSIC, "music/MusicTab.fxml", MusicComponent::class)
        registerTab(controller, ModType.LOADING_SCREENS, "loading_screen/LoadingScreenTab.fxml", LoadingScreenComponent::class)
        
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
    LOADING_SCREENS("Graphics"),
    FLAGS("Graphics"),
    PRESCRIPTED_COUNTRIES("Galaxy Generation"),
    ASCENSION_PERKS("Government"),
    CIVICS("Government"),
    TECHNOLOGY("Tech"),
    NAME_LISTS("Species")
}

class Mod(var name: String, var modID: String = "", val modType: MutableSet<ModType> = mutableSetOf())
{
    companion object
    {
        fun loadMod(saveLoc: File): Mod
        {
            val jsonData = JSONParser.parse(saveLoc) as JSONObject
            val name = jsonData.string("name")!!
            val folderName = jsonData.string("folderName")
            val modID = jsonData.string("modID")!!
            val image = if(!jsonData.isNull("image")) File(jsonData.string("image")) else null
            val supportedVersion = jsonData.string("supported version")!!
            val modType = jsonData.jsonArray("types").map {ModType.valueOf(it as String)}.toMutableSet()
            
            val mod = Mod(name, modID, modType)
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

fun registerTab(controller: Controller, modType: ModType, fxmlFile: String, componentClass: KClass<out ModComponent>)
{
    controller.registerTab(fxmlFile, modType)
    ModComponent.registerComponent(modType, componentClass)
}

interface ModComponent
{
    companion object
    {
        private data class ComponentFunctions(val create: (Mod) -> ModComponent, val fromJSON: (Mod, JSONObject) -> ModComponent)
        
        private val componentMap = mutableMapOf<ModType, ComponentFunctions>()
        
        fun registerComponent(modType: ModType, componentClass: KClass<out ModComponent>)
        {
            val reflectedConstructor = componentClass.primaryConstructor!!
            val createFunc = {mod: Mod -> reflectedConstructor.call(mod)}
            
            val reflectedFunction = componentClass.companionObject!!.functions.find {it.name == "fromJSON"}!!
            val companionObj = componentClass.companionObjectInstance
            val fromJSONFunc = {mod: Mod, obj: JSONObject -> reflectedFunction.call(companionObj, mod, obj) as ModComponent}
            
            componentMap[modType] = ComponentFunctions(createFunc, fromJSONFunc)
        }
        
        fun createComponent(type: ModType, mod: Mod) = componentMap[type]!!.create(mod)
        
        fun fromJSON(mod: Mod, obj: JSONObject): ModComponent
        {
            val type = ModType.valueOf(obj.string("type")!!)
            return componentMap[type]!!.fromJSON(mod, obj)
        }
    }
    
    val type: ModType
    fun toJSON(): JSONObject
    fun createModComponent(modLoc: File)
    fun deleteModComponent(modLoc: File)
}