package com.hamonie.cast


import android.view.Menu
import com.hamonie.R

import com.google.android.gms.cast.framework.CastButtonFactory

import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity


class ExpandedControlsActivity : ExpandedControllerActivity() {
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_cast, menu)
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.action_cast)
        return true
    }
}