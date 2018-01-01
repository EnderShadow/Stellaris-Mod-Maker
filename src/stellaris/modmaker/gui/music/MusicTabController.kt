package stellaris.modmaker.gui.music

import com.sun.javafx.scene.control.skin.TableHeaderRow
import javafx.beans.binding.Bindings
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.util.Callback
import javafx.util.StringConverter
import stellaris.modmaker.ModComponent
import stellaris.modmaker.gui.TabController
import java.io.File
import java.util.function.UnaryOperator


class MusicTabController: TabController()
{
    @FXML
    lateinit private var musicListTable: TableView<MusicComponent.Song>
    @FXML
    lateinit private var musicLocationColumn: TableColumn<MusicComponent.Song, File>
    @FXML
    lateinit private var musicNameColumn: TableColumn<MusicComponent.Song, String>
    @FXML
    lateinit private var musicVolumeColumn: TableColumn<MusicComponent.Song, Double>
    
    override fun initialize()
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
    
    override fun tabMenuItems(): List<MenuItem>
    {
        val addSongsMenu = MenuItem("Add Songs")
        addSongsMenu.setOnAction {addSongs()}
        return listOf(addSongsMenu)
    }
    
    override fun onModComponentAdded(component: ModComponent)
    {
        if(component is MusicComponent)
            musicListTable.items = component.songs
    }
    
    override fun onModOpened(component: ModComponent)
    {
        if(component is MusicComponent)
            musicListTable.items = component.songs
    }
    
    private fun addSongs()
    {
        rootController.fileChooser.title = "Choose Songs"
        rootController.fileChooser.extensionFilters.clear()
        rootController.fileChooser.extensionFilters.add(rootController.musicFilter)
        rootController.fileChooser.showOpenMultipleDialog(rootController.window)?.let {
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
}