package stellaris.modmaker.gui.loading_screen

import javafx.collections.FXCollections
import json.JSONArray
import json.JSONObject
import stellaris.modmaker.Mod
import stellaris.modmaker.ModComponent
import stellaris.modmaker.ModType
import java.io.File

class LoadingScreenComponent(private val mod: Mod): ModComponent
{
    companion object
    {
        fun fromJSON(mod: Mod, obj: JSONObject): LoadingScreenComponent
        {
            val loadingScreenComponent = LoadingScreenComponent(mod)
            loadingScreenComponent.loadingScreens.addAll(obj.jsonArray("loadingScreens").map {LoadingScreenTabController.LoadingScreen(File(it as String))})
            return loadingScreenComponent
        }
    }
    
    override val type = ModType.LOADING_SCREENS
    
    val loadingScreens = FXCollections.observableArrayList<LoadingScreenTabController.LoadingScreen>()!!
    
    private val path = "gfx/loadingscreens"
    
    override fun toJSON(): JSONObject
    {
        val component = JSONObject()
        component["type"] = type.toString()
        component["loadingScreens"] = JSONArray(loadingScreens.map {it.file.path})
        return component
    }
    
    override fun createModComponent(modLoc: File)
    {
        val loadingScreenLoc = File(modLoc, path)
        loadingScreenLoc.deleteRecursively()
        loadingScreenLoc.mkdirs()
        
        loadingScreens.forEachIndexed {index, loadingScreen ->  loadingScreen.file.copyTo(File(loadingScreenLoc, "${mod.folderName}_load_${index + 1}.${loadingScreen.file.extension}"))}
    }
    
    override fun deleteModComponent(modLoc: File)
    {
        File(modLoc, "gfx/loadingscreens").deleteRecursively()
    }
}