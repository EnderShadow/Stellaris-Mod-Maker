package stellaris.modmaker.gui

import javafx.scene.control.MenuItem
import stellaris.modmaker.ModComponent

abstract class TabController
{
    lateinit var rootController: Controller
    
    abstract fun initialize()
    
    /**
     * Returns a list of menu items that go in the Tab menu
     */
    abstract fun tabMenuItems(): List<MenuItem>
    
    /**
     * This is called whenever a component is added to a mod
     */
    abstract fun onModComponentAdded(component: ModComponent)
    
    /**
     * This is called whenever a mod is loaded for each component in the mod.
     */
    abstract fun onModOpened(component: ModComponent)
}