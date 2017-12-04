package stellaris.modmaker.gui

import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.stage.FileChooser
import javafx.stage.Stage
import stellaris.modmaker.*
import java.io.File
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.input.*


class Controller
{
    // Controller for the window and the Mod Info tab
    
    @FXML
    lateinit private var tabPane: TabPane
    @FXML
    lateinit private var tabMenu: Menu
    @FXML
    lateinit private var addMenu: Menu
    @FXML
    lateinit private var removeMenu: Menu
    @FXML
    lateinit private var modName: TextField
    @FXML
    lateinit private var modFolder: TextField
    @FXML
    lateinit private var modID: TextField
    @FXML
    lateinit private var supportedVersion: TextField
    @FXML
    lateinit private var image: ImageView
    
    lateinit var window: Stage
    lateinit private var imageBorderStyle: String
    
    private val invisibleTabs = mutableMapOf<String, Tab>()
    private val tabControllers = mutableMapOf<String, TabController>()
    private var currentMod = Mod("")
    private var lastSaveLoc: File? = null
    
    val fileChooser = FileChooser()
    val imageFilter = FileChooser.ExtensionFilter("Image File", "*.jpg", "*.jpeg", "*.png")
    val modMakerFilter = FileChooser.ExtensionFilter("Stellaris Mod Maker File", "*.smod")
    val musicFilter = FileChooser.ExtensionFilter("Vorbis Audio File", "*.ogg")
    val ddsFilter = FileChooser.ExtensionFilter("DirectDraw Surface File", "*.dds")
    
    fun initialize()
    {
        fileChooser.initialDirectory = modDirectory.parentFile
        
        imageBorderStyle = image.parent.style
    
        modName.textProperty().addListener({_, _, new -> currentMod.name = new})
        modFolder.textProperty().addListener({_, _, new -> currentMod.folderName = new})
        modID.textProperty().addListener({_, _, new -> currentMod.modID = new})
        supportedVersion.textProperty().addListener({_, _, new -> currentMod.supportedVersion = new})
        
        tabPane.selectionModel.selectedItemProperty().addListener({_, old, new ->
            if(old != new)
            {
                tabMenu.items.clear()
                if(new.text != "Mod Info")
                    tabMenu.items.addAll(menuForTab(new.text))
            }
        })
    }
    
    fun registerTab(fxmlPath: String, modType: ModType)
    {
        val loader = FXMLLoader(javaClass.getResource(fxmlPath))
        val tabContent = loader.load<Parent>()
        val controller = loader.getController<TabController>()
        controller.rootController = this
        val tab = Tab(modType.name.toLowerCase().replace('_', ' ').capitalizeEachWord(), tabContent)
        tabControllers.put(tab.text, controller)
        
        invisibleTabs.put(tab.text, tab)
        
        var menuItem = MenuItem(tab.text)
        menuItem.onAction = EventHandler<ActionEvent> {evt -> addComponent(evt)}
        addMenu.items.add(menuItem)
        
        menuItem = MenuItem(tab.text)
        menuItem.onAction = EventHandler<ActionEvent> {evt -> removeComponent(evt)}
        menuItem.isVisible = false
        removeMenu.items.add(menuItem)
    }
    
    private fun menuForTab(name: String): List<MenuItem>
    {
        return tabControllers[name]!!.tabMenuItems()
    }
    
    fun createMod() = currentMod.createMod()
    
    private fun addComponent(evt: ActionEvent)
    {
        val name = (evt.source as MenuItem).text
        if(name in invisibleTabs)
        {
            tabPane.tabs.add(invisibleTabs.remove(name))
            addMenu.items.find {it.text == name}?.isVisible = false
            removeMenu.items.find {it.text == name}?.isVisible = true
            if (addMenu.items.none {it.isVisible})
                addMenu.isDisable = true
            removeMenu.isDisable = false
            
            val type = ModType.valueOf(name.toUpperCase().replace(' ', '_'))
            if(currentMod.modType.add(type))
            {
                val component = ModComponent.createComponent(type, currentMod)
                currentMod.modComponents.add(component)
                tabControllers[name]!!.onModComponentAdded(component)
            }
        }
    }
    
    private fun removeComponent(evt: ActionEvent)
    {
        val name = (evt.source as MenuItem).text
        val tab = tabPane.tabs.find {it.text == name}
        if(tab != null)
        {
            tabPane.tabs.remove(tab)
            invisibleTabs[name] = tab
            addMenu.items.find {it.text == name}?.isVisible = true
            removeMenu.items.find {it.text == name}?.isVisible = false
            if (removeMenu.items.none {it.isVisible})
                removeMenu.isDisable = true
            addMenu.isDisable = false
            
            val type = ModType.valueOf(name.toUpperCase().replace(' ', '_'))
            currentMod.modType.remove(type)
            currentMod.modComponents.find {it.type == type}?.deleteModComponent(File(modDirectory, currentMod.folderName))
        }
    }
    
    fun selectModImage(evt: MouseEvent)
    {
        if(evt.button == MouseButton.PRIMARY)
        {
            fileChooser.title = "Select Image"
            fileChooser.extensionFilters.clear()
            fileChooser.extensionFilters.add(imageFilter)
            fileChooser.showOpenDialog(window)?.let {
                currentMod.image = it
                image.image = Image(it.toURI().toString())
                image.parent.style = ""
            }
        }
    }
    
    fun open()
    {
        fileChooser.title = "Open Mod File"
        fileChooser.extensionFilters.clear()
        fileChooser.extensionFilters.add(modMakerFilter)
        fileChooser.showOpenDialog(window)?.let {
            // Clean up open tabs
            currentMod = Mod("") // prevents directories from the old mod being deleted
            removeMenu.items.filter {it.isVisible}.forEach {it.fire()}
            
            // Load mod from file
            currentMod = Mod.loadMod(it)
            lastSaveLoc = it
            image.parent.style = currentMod.image?.let {imageFile ->
                image.image = Image(imageFile.toURI().toString())
                ""
            } ?: imageBorderStyle
            modName.text = currentMod.name
            modFolder.text = if(currentMod.folderSet) currentMod.folderName else ""
            modID.text = currentMod.modID
            supportedVersion.text = currentMod.supportedVersion
            
            // Add tabs that the mod uses
            addMenu.items.filter {ModType.valueOf(it.text.toUpperCase().replace(' ', '_')) in currentMod.modType}.forEach {it.fire()}
            
            // Tells each tab controller that a mod is being opened
            currentMod.modComponents.forEach {component -> tabPane.tabs.drop(1).forEach {tabControllers[it.text]!!.onModOpened(component)}}
        }
    }
    
    fun save()
    {
        lastSaveLoc?.let {currentMod.saveMod(it)} ?: saveAs()
    }
    
    fun saveAs()
    {
        fileChooser.title = "Save Mod File"
        fileChooser.extensionFilters.clear()
        fileChooser.extensionFilters.add(modMakerFilter)
        fileChooser.showSaveDialog(window)?.let {
            currentMod.saveMod(it)
            lastSaveLoc = it
        }
    }
    
    fun closeMod()
    {
        currentMod = Mod("")
        lastSaveLoc = null
        image.image = null
        image.parent.style = imageBorderStyle
        modName.text = ""
        modFolder.text = ""
        modID.text = ""
        supportedVersion.text = ""
        removeMenu.items.filter {it.isVisible}.forEach {it.fire()}
    }
    
    fun quit()
    {
        window.close()
    }
}