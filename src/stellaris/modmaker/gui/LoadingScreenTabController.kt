package stellaris.modmaker.gui

import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.scene.control.ListView
import javafx.scene.control.MenuItem
import javafx.scene.control.SelectionMode
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import stellaris.modmaker.LoadingScreenComponent
import stellaris.modmaker.ModComponent
import java.io.File
import javax.imageio.ImageIO


class LoadingScreenTabController : TabController()
{
    @FXML
    lateinit private var loadingScreenListView: ListView<LoadingScreen>
    
    override fun initialize()
    {
        loadingScreenListView.selectionModel.selectionMode = SelectionMode.MULTIPLE
    }
    
    override fun tabMenuItems(): List<MenuItem>
    {
        val addLoadingScreensMenu = MenuItem("Add Loading Screens")
        addLoadingScreensMenu.setOnAction {addLoadingScreens()}
        return listOf(addLoadingScreensMenu)
    }
    
    override fun onModComponentAdded(component: ModComponent)
    {
        if(component is LoadingScreenComponent)
            loadingScreenListView.items = component.loadingScreens
    }
    
    override fun onModOpened(component: ModComponent)
    {
        if(component is LoadingScreenComponent)
        {
            loadingScreenListView.items = component.loadingScreens
            loadingScreenListView.items.forEach {loadingScreen ->
                loadingScreen.fitHeightProperty().bind(loadingScreenListView.heightProperty().subtract(21))
            }
        }
    }
    
    private fun addLoadingScreens()
    {
        rootController.fileChooser.title = "Choose Loading Screens"
        rootController.fileChooser.extensionFilters.clear()
        rootController.fileChooser.extensionFilters.add(rootController.ddsFilter)
        rootController.fileChooser.showOpenMultipleDialog(rootController.window)?.let {
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