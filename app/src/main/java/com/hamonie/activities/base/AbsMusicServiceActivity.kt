package com.hamonie.activities.base

import android.Manifest
import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.lifecycle.lifecycleScope
import com.hamonie.R
import com.hamonie.db.toPlayCount
import com.hamonie.helper.MusicPlayerRemote
import com.hamonie.interfaces.IMusicServiceEventListener
import com.hamonie.repository.RealRepository
import com.hamonie.service.MusicService.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.lang.ref.WeakReference
import java.util.*

abstract class AbsMusicServiceActivity : AbsBaseActivity(), IMusicServiceEventListener {

    private val mMusicServiceEventListeners = ArrayList<IMusicServiceEventListener>()
    private val repository: RealRepository by inject()
    private var serviceToken: MusicPlayerRemote.ServiceToken? = null
    private var musicStateReceiver: MusicStateReceiver? = null
    private var receiverRegistered: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        serviceToken = MusicPlayerRemote.bindToService(this, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                this@AbsMusicServiceActivity.onServiceConnected()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                this@AbsMusicServiceActivity.onServiceDisconnected()
            }
        })

        setPermissionDeniedMessage(getString(R.string.permission_external_storage_denied))
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicPlayerRemote.unbindFromService(serviceToken)
        if (receiverRegistered) {
            unregisterReceiver(musicStateReceiver)
            receiverRegistered = false
        }
    }

    fun addMusicServiceEventListener(listenerI: IMusicServiceEventListener?) {
        if (listenerI != null) {
            mMusicServiceEventListeners.add(listenerI)
        }
    }

    fun removeMusicServiceEventListener(listenerI: IMusicServiceEventListener?) {
        if (listenerI != null) {
            mMusicServiceEventListeners.remove(listenerI)
        }
    }

    override fun onServiceConnected() {
        if (!receiverRegistered) {
            musicStateReceiver = MusicStateReceiver(this)

            val filter = IntentFilter()
            filter.addAction(PLAY_STATE_CHANGED)
            filter.addAction(SHUFFLE_MODE_CHANGED)
            filter.addAction(REPEAT_MODE_CHANGED)
            filter.addAction(META_CHANGED)
            filter.addAction(QUEUE_CHANGED)
            filter.addAction(MEDIA_STORE_CHANGED)
            filter.addAction(FAVORITE_STATE_CHANGED)

            registerReceiver(musicStateReceiver, filter)

            receiverRegistered = true
        }

        for (listener in mMusicServiceEventListeners) {
            listener.onServiceConnected()
        }
    }

    override fun onServiceDisconnected() {
        if (receiverRegistered) {
            unregisterReceiver(musicStateReceiver)
            receiverRegistered = false
        }

        for (listener in mMusicServiceEventListeners) {
            listener.onServiceDisconnected()
        }
    }

    override fun onPlayingMetaChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onPlayingMetaChanged()
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val entity = repository.songPresentInHistory(MusicPlayerRemote.currentSong)
            if (entity != null) {
                repository.updateHistorySong(MusicPlayerRemote.currentSong)
            } else {
                repository.addSongToHistory(MusicPlayerRemote.currentSong)
            }
            val songs = repository.checkSongExistInPlayCount(MusicPlayerRemote.currentSong.id)
            if (songs.isNotEmpty()) {
                repository.updateSongInPlayCount(songs.first().apply {
                    playCount += 1
                })
            } else {
                repository.insertSongInPlayCount(MusicPlayerRemote.currentSong.toPlayCount())
            }
        }
    }

    override fun onQueueChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onQueueChanged()
        }
    }

    override fun onPlayStateChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onPlayStateChanged()
        }
    }

    override fun onMediaStoreChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onMediaStoreChanged()
        }
    }

    override fun onRepeatModeChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onRepeatModeChanged()
        }
    }

    override fun onShuffleModeChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onShuffleModeChanged()
        }
    }

    override fun onFavoriteStateChanged() {
        for (listener in mMusicServiceEventListeners) {
            listener.onFavoriteStateChanged()
        }
    }

    override fun onHasPermissionsChanged(hasPermissions: Boolean) {
        super.onHasPermissionsChanged(hasPermissions)
        val intent = Intent(MEDIA_STORE_CHANGED)
        intent.putExtra(
            "from_permissions_changed",
            true
        ) // just in case we need to know this at some point
        sendBroadcast(intent)
        println("sendBroadcast $hasPermissions")
    }

    override fun getPermissionsToRequest(): Array<String> {
        return arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH
        )
    }

    private class MusicStateReceiver(activity: AbsMusicServiceActivity) : BroadcastReceiver() {

        private val reference: WeakReference<AbsMusicServiceActivity> = WeakReference(activity)

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val activity = reference.get()
            if (activity != null && action != null) {
                when (action) {
                    FAVORITE_STATE_CHANGED -> activity.onFavoriteStateChanged()
                    META_CHANGED -> activity.onPlayingMetaChanged()
                    QUEUE_CHANGED -> activity.onQueueChanged()
                    PLAY_STATE_CHANGED -> activity.onPlayStateChanged()
                    REPEAT_MODE_CHANGED -> activity.onRepeatModeChanged()
                    SHUFFLE_MODE_CHANGED -> activity.onShuffleModeChanged()
                    MEDIA_STORE_CHANGED -> activity.onMediaStoreChanged()
                }
            }
        }
    }

    companion object {
        val TAG: String = AbsMusicServiceActivity::class.java.simpleName
    }
}
