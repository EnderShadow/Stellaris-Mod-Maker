package stellaris.modmaker.gui.music

import javafx.collections.FXCollections
import json.JSONArray
import json.JSONObject
import stellaris.modmaker.Mod
import stellaris.modmaker.ModComponent
import stellaris.modmaker.ModType
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

class MusicComponent(private val mod: Mod): ModComponent
{
    companion object
    {
        fun fromJSON(mod: Mod, obj: JSONObject): MusicComponent
        {
            val musicComponent = MusicComponent(mod)
            obj.jsonArray("songs").forEach {it as JSONObject
                val location = File(it.string("location"))
                val name = it.string("name")!!
                val volume = it.number("volume") as Double
                musicComponent.songs.add(Song(location, name, volume))
            }
            return musicComponent
        }
    }
    
    override val type = ModType.MUSIC
    
    val songs = FXCollections.observableArrayList<Song>()!!
    
    private val path get() = "music/${mod.folderName}"
    
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