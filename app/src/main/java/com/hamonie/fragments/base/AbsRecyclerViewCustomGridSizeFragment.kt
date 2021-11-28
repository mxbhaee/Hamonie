package com.hamonie.fragments.base

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.hamonie.R
import com.hamonie.util.RetroUtil
import com.google.android.material.transition.MaterialFade

abstract class AbsRecyclerViewCustomGridSizeFragment<A : RecyclerView.Adapter<*>, LM : RecyclerView.LayoutManager> :
    AbsRecyclerViewFragment<A, LM>() {

    private var gridSize: Int = 0
    private var sortOrder: String? = null
    private var currentLayoutRes: Int = 0
    private val isLandscape: Boolean
        get() = RetroUtil.isLandscape()

    val maxGridSize: Int
        get() = if (isLandscape) {
            resources.getInteger(R.integer.max_columns_land)
        } else {
            resources.getInteger(R.integer.max_columns)
        }

    fun itemLayoutRes(): Int {
        return if (getGridSize() > maxGridSizeForList) {
            loadLayoutRes()
        } else R.layout.item_list
    }

    fun setAndSaveLayoutRes(layoutRes: Int) {
        saveLayoutRes(layoutRes)
        invalidateAdapter()
    }

    private val maxGridSizeForList: Int
        get() = if (isLandscape) {
            resources.getInteger(R.integer.default_list_columns_land)
        } else resources.getInteger(R.integer.default_list_columns)

    fun getGridSize(): Int {
        if (gridSize == 0) {
            gridSize = if (isLandscape) {
                loadGridSizeLand()
            } else {
                loadGridSize()
            }
        }
        return gridSize
    }

    fun getSortOrder(): String? {
        if (sortOrder == null) {
            sortOrder = loadSortOrder()
        }
        return sortOrder
    }

    fun setAndSaveSortOrder(sortOrder: String) {
        this.sortOrder = sortOrder
        println(sortOrder)
        saveSortOrder(sortOrder)
        setSortOrder(sortOrder)
    }

    fun setAndSaveGridSize(gridSize: Int) {
        val oldLayoutRes = itemLayoutRes()
        this.gridSize = gridSize
        if (isLandscape) {
            saveGridSizeLand(gridSize)
        } else {
            saveGridSize(gridSize)
        }
        recyclerView.isVisible = false
        invalidateLayoutManager()
        // only recreate the adapter and layout manager if the layout currentLayoutRes has changed
        if (oldLayoutRes != itemLayoutRes()) {
            invalidateAdapter()
        } else {
            setGridSize(gridSize)
        }
        val transition = MaterialFade().apply {
            addTarget(recyclerView)
        }
        TransitionManager.beginDelayedTransition(container, transition)
        recyclerView.isVisible = true
    }

    protected abstract fun setGridSize(gridSize: Int)

    protected abstract fun setSortOrder(sortOrder: String)

    protected abstract fun loadSortOrder(): String

    protected abstract fun saveSortOrder(sortOrder: String)

    protected abstract fun loadGridSize(): Int

    protected abstract fun saveGridSize(gridColumns: Int)

    protected abstract fun loadGridSizeLand(): Int

    protected abstract fun saveGridSizeLand(gridColumns: Int)

    protected abstract fun loadLayoutRes(): Int

    protected abstract fun saveLayoutRes(layoutRes: Int)
}
