package stellaris.modmaker.gui

import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.Callback
import stellaris.modmaker.*
import java.io.File
import com.sun.javafx.scene.control.skin.TableHeaderRow
import javafx.beans.binding.Bindings
import javafx.embed.swing.SwingFXUtils
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.util.StringConverter
import java.util.function.UnaryOperator
import javax.imageio.ImageIO


class Controller
{
    // Main controller and Mod Info tab
    
    @FXML
    lateinit private var tabPane: TabPane
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
    private var currentMod = Mod("")
    private var lastSaveLoc: File? = null
    
    private val fileChooser = FileChooser()
    private val imageFilter = FileChooser.ExtensionFilter("Image File", "*.jpg", "*.jpeg", "*.png")
    private val modMakerFilter = FileChooser.ExtensionFilter("Stellaris Mod Maker File", "*.smod")
    private val musicFilter = FileChooser.ExtensionFilter("Vorbis Audio File", "*.ogg")
    private val ddsFilter = FileChooser.ExtensionFilter("DirectDraw Surface File", "*.dds")
    
    fun initialize()
    {
        fileChooser.initialDirectory = modDirectory.parentFile
        
        invisibleTabs.putAll(tabPane.tabs.drop(1).associate {it.text to it})
        tabPane.tabs.remove(1, tabPane.tabs.size)
        
        invisibleTabs.keys.forEach {
            var menuItem = MenuItem(it)
            menuItem.onAction = EventHandler<ActionEvent> {evt -> addComponent(evt)}
            addMenu.items.add(menuItem)
    
            menuItem = MenuItem(it)
            menuItem.onAction = EventHandler<ActionEvent> {evt -> removeComponent(evt)}
            menuItem.isVisible = false
            removeMenu.items.add(menuItem)
        }
        
        // disable unimplemented mod types
        ModType.values().forEach {
            try
            {
                ModComponent.createComponent(it, Mod(""))
            }
            catch(error: NotImplementedError)
            {
                addMenu.items.find {item -> item.text.toUpperCase().replace(' ', '_') == it.toString()}!!.isDisable = true
            }
            finally {} // ignore other exceptions (They aren't important here)
        }
        
        imageBorderStyle = image.parent.style
    
        modName.textProperty().addListener({_, _, new -> currentMod.name = new})
        modFolder.textProperty().addListener({_, _, new -> currentMod.folderName = new})
        modID.textProperty().addListener({_, _, new -> currentMod.modID = new})
        supportedVersion.textProperty().addListener({_, _, new -> currentMod.supportedVersion = new})
        
        musicTabInit()
        loadingScreenTabInit()
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
                when(type)
                {
                    ModType.SPECIES -> TODO()
                    ModType.MUSIC -> musicListTable.items = (component as MusicComponent).songs
                    ModType.PORTRAITS -> TODO()
                    ModType.TRAITS -> TODO()
                    ModType.SYSTEM_INITIALIZERS -> TODO()
                    ModType.LOADING_SCREENS -> loadingScreenListView.items = (component as LoadingScreenComponent).loadingScreens
                    ModType.FLAGS -> TODO()
                    ModType.PRESCRIPTED_COUNTRIES -> TODO()
                    ModType.ASCENSION_PERKS -> TODO()
                    ModType.CIVICS -> TODO()
                    ModType.TECHNOLOGY -> TODO()
                    ModType.NAME_LISTS -> TODO()
                }
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
            addMenu.items.filter {ModType.valueOf(it.text.toUpperCase().replace(' ', '_')) in currentMod.modType}.forEach {it.fire()}
            currentMod.modComponents.forEach {
                when(it)
                {
                    is MusicComponent -> {
                        musicListTable.items = it.songs
                    }
                    is LoadingScreenComponent -> {
                        loadingScreenListView.items = it.loadingScreens
                        loadingScreenListView.items.forEach {loadingScreen ->
                            loadingScreen.fitHeightProperty().bind(loadingScreenListView.heightProperty().subtract(21))
                        }
                    }
                }
            }
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
    
    // ===========================================================================================================
    // Music tab
    // ===========================================================================================================
    
    @FXML
    lateinit private var musicListTable: TableView<MusicComponent.Song>
    @FXML
    lateinit private var musicLocationColumn: TableColumn<MusicComponent.Song, File>
    @FXML
    lateinit private var musicNameColumn: TableColumn<MusicComponent.Song, String>
    @FXML
    lateinit private var musicVolumeColumn: TableColumn<MusicComponent.Song, Double>
    
    private fun musicTabInit()
    {
        // prevents reordering of columns
        musicListTable.widthProperty().addListener({_, _, _ ->
            val header = musicListTable.lookup("TableHeaderRow") as TableHeaderRow
            header.reorderingProperty().addListener({_, _, _ ->
                header.isReordering = false
            })
        })
        musicListTable.selectionModel.selectionMode = SelectionMode.MULTIPLE
        
        musicLocationColumn.cellValueFactory = PropertyValueFactory("songLocation")
        musicNameColumn.cellValueFactory = PropertyValueFactory("songName")
        musicNameColumn.cellFactory = TextFieldTableCell.forTableColumn()
        musicNameColumn.setOnEditCommit {it.rowValue.songName = it.newValue}
        musicVolumeColumn.cellValueFactory = PropertyValueFactory("volume")
        musicVolumeColumn.cellFactory = Callback {DoubleEditingTableCell()}
        musicVolumeColumn.setOnEditCommit {it.rowValue.volume = it.newValue}
    }
    
    fun addSongs()
    {
        fileChooser.title = "Choose Songs"
        fileChooser.extensionFilters.clear()
        fileChooser.extensionFilters.add(musicFilter)
        fileChooser.showOpenMultipleDialog(window)?.let {
            musicListTable.items.addAll(it.map {MusicComponent.Song(it)})
        }
    }
    
    fun musicKeyReleased(evt: KeyEvent)
    {
        if(evt.code == KeyCode.DELETE)
            musicListTable.items.removeAll(musicListTable.selectionModel.selectedItems)
    }
    
    class DoubleEditingTableCell: TableCell<MusicComponent.Song, Double>()
    {
        private val textField = TextField()
        private val textFormatter: TextFormatter<Double>
        
        init
        {
            val filter = UnaryOperator<TextFormatter.Change?> {change ->
                var newValue = change!!.controlNewText
                val lastChar = if(newValue.isEmpty()) null else newValue.last().toLowerCase()
                if(lastChar == 'e')
                    newValue += '0'
                val num = newValue.toDoubleOrNull()
                if(newValue.isEmpty())
                    change
                else if(num == null || num < 0.0 || lastChar == 'd' || lastChar == 'f')
                    null
                else
                    change
            }
            val converter = object: StringConverter<Double>()
            {
                override fun fromString(string: String): Double
                {
                    @Suppress("NAME_SHADOWING")
                    var string = string
                    if(string.isNotEmpty() && string.last().toLowerCase() == 'e')
                        string += '0'
                    return string.toDoubleOrNull() ?: item
                }
                
                override fun toString(value: Double?): String
                {
                    return value?.toString() ?: "0.0"
                }
            }
            textFormatter = TextFormatter(converter, 0.0, filter)
            textField.textFormatter = textFormatter
            textField.addEventFilter(KeyEvent.KEY_RELEASED) {
                if(it.code == KeyCode.ESCAPE)
                    cancelEdit()
            }
            textField.onAction = EventHandler {commitEdit(converter.fromString(textField.text))}
            textProperty().bind(Bindings.`when`(emptyProperty()).then(null as String?).otherwise(itemProperty().asString()))
            graphic = textField
            contentDisplay = ContentDisplay.TEXT_ONLY
        }
    
        override fun updateItem(item: Double?, empty: Boolean)
        {
            super.updateItem(item, empty)
            contentDisplay = if(isEditing)
            {
                textField.requestFocus()
                textField.selectAll()
                ContentDisplay.GRAPHIC_ONLY
            }
            else
            {
                ContentDisplay.TEXT_ONLY
            }
        }
    
        override fun startEdit()
        {
            super.startEdit()
            textFormatter.value = item
            contentDisplay = ContentDisplay.GRAPHIC_ONLY
            textField.requestFocus()
            textField.selectAll()
        }
    
        override fun commitEdit(newValue: Double?)
        {
            super.commitEdit(newValue)
            contentDisplay = ContentDisplay.TEXT_ONLY
        }
    
        override fun cancelEdit()
        {
            super.cancelEdit()
            contentDisplay = ContentDisplay.TEXT_ONLY
        }
    }
    
    // ===========================================================================================================
    // Loading Screen tab
    // ===========================================================================================================
    
    @FXML
    lateinit private var loadingScreenListView: ListView<LoadingScreen>
    
    private fun loadingScreenTabInit()
    {
        loadingScreenListView.selectionModel.selectionMode = SelectionMode.MULTIPLE
    }
    
    fun addLoadingScreens()
    {
        fileChooser.title = "Choose Loading Screens"
        fileChooser.extensionFilters.clear()
        fileChooser.extensionFilters.add(ddsFilter)
        fileChooser.showOpenMultipleDialog(window)?.let {
            val imageViews = it.map {LoadingScreen(it)}
            imageViews.forEach {
                it.fitHeightProperty().bind(loadingScreenListView.heightProperty().subtract(21))
            }
            loadingScreenListView.items.addAll(imageViews)
        }
    }
    
    fun loadingScreenKeyReleased(evt: KeyEvent)
    {
        if(evt.code == KeyCode.DELETE)
        {
            loadingScreenListView.items.removeAll(loadingScreenListView.selectionModel.selectedItems)
        }
    }
    
    class LoadingScreen(val file: File): ImageView(SwingFXUtils.toFXImage(ImageIO.read(file), null))
    {
        init
        {
            isPreserveRatio = true
        }
    }
}