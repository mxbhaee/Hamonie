/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package com.hamonie.service;

import static android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;
import static androidx.media.MediaBrowserServiceCompat.BrowserRoot.EXTRA_RECENT;
import static org.koin.java.KoinJavaComponent.get;
import static com.hamonie.ConstantsKt.ALBUM_ART_ON_LOCK_SCREEN;
import static com.hamonie.ConstantsKt.BLURRED_ALBUM_ART;
import static com.hamonie.ConstantsKt.CLASSIC_NOTIFICATION;
import static com.hamonie.ConstantsKt.COLORED_NOTIFICATION;
import static com.hamonie.ConstantsKt.CROSS_FADE_DURATION;
import static com.hamonie.ConstantsKt.TOGGLE_HEADSET;
import static com.hamonie.service.AudioFader.startFadeAnimator;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.os.Binder;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import com.hamonie.R;
import com.hamonie.activities.LockScreenActivity;
import com.hamonie.appwidgets.AppWidgetBig;
import com.hamonie.appwidgets.AppWidgetCard;
import com.hamonie.appwidgets.AppWidgetClassic;
import com.hamonie.appwidgets.AppWidgetSmall;
import com.hamonie.appwidgets.AppWidgetText;
import com.hamonie.auto.AutoMediaIDHelper;
import com.hamonie.auto.AutoMusicProvider;
import com.hamonie.glide.BlurTransformation;
import com.hamonie.glide.GlideApp;
import com.hamonie.glide.RetroGlideExtension;
import com.hamonie.helper.MusicPlayerRemote;
import com.hamonie.helper.ShuffleHelper;
import com.hamonie.model.Song;
import com.hamonie.model.smartplaylist.AbsSmartPlaylist;
import com.hamonie.providers.HistoryStore;
import com.hamonie.providers.MusicPlaybackQueueStore;
import com.hamonie.providers.SongPlayCountStore;
import com.hamonie.service.notification.PlayingNotification;
import com.hamonie.service.notification.PlayingNotificationImpl;
import com.hamonie.service.notification.PlayingNotificationOreo;
import com.hamonie.service.playback.Playback;
import com.hamonie.util.MusicUtil;
import com.hamonie.util.PackageValidator;
import com.hamonie.util.PreferenceUtil;
import com.hamonie.util.RetroUtil;
import com.hamonie.volume.AudioVolumeObserver;
import com.hamonie.volume.OnAudioVolumeChangedListener;

/**
 * @author Karim Abou Zeid (kabouzeid), Andrew Neal
 */
public class MusicService extends MediaBrowserServiceCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener, Playback.PlaybackCallbacks, OnAudioVolumeChangedListener {

    public static final String TAG = MusicService.class.getSimpleName();
    public static final String RETRO_MUSIC_PACKAGE_NAME = "com.hamonie";
    public static final String MUSIC_PACKAGE_NAME = "com.android.music";
    public static final String ACTION_TOGGLE_PAUSE = RETRO_MUSIC_PACKAGE_NAME + ".togglepause";
    public static final String ACTION_PLAY = RETRO_MUSIC_PACKAGE_NAME + ".play";
    public static final String ACTION_PLAY_PLAYLIST = RETRO_MUSIC_PACKAGE_NAME + ".play.playlist";
    public static final String ACTION_PAUSE = RETRO_MUSIC_PACKAGE_NAME + ".pause";
    public static final String ACTION_STOP = RETRO_MUSIC_PACKAGE_NAME + ".stop";
    public static final String ACTION_SKIP = RETRO_MUSIC_PACKAGE_NAME + ".skip";
    public static final String ACTION_REWIND = RETRO_MUSIC_PACKAGE_NAME + ".rewind";
    public static final String ACTION_QUIT = RETRO_MUSIC_PACKAGE_NAME + ".quitservice";
    public static final String ACTION_PENDING_QUIT = RETRO_MUSIC_PACKAGE_NAME + ".pendingquitservice";
    public static final String INTENT_EXTRA_PLAYLIST =
            RETRO_MUSIC_PACKAGE_NAME + "intentextra.playlist";
    public static final String INTENT_EXTRA_SHUFFLE_MODE =
            RETRO_MUSIC_PACKAGE_NAME + ".intentextra.shufflemode";
    public static final String APP_WIDGET_UPDATE = RETRO_MUSIC_PACKAGE_NAME + ".appwidgetupdate";
    public static final String EXTRA_APP_WIDGET_NAME = RETRO_MUSIC_PACKAGE_NAME + "app_widget_name";
    // Do not change these three strings as it will break support with other apps (e.g. last.fm
    // scrobbling)
    public static final String META_CHANGED = RETRO_MUSIC_PACKAGE_NAME + ".metachanged";
    public static final String QUEUE_CHANGED = RETRO_MUSIC_PACKAGE_NAME + ".queuechanged";
    public static final String PLAY_STATE_CHANGED = RETRO_MUSIC_PACKAGE_NAME + ".playstatechanged";
    public static final String FAVORITE_STATE_CHANGED =
            RETRO_MUSIC_PACKAGE_NAME + "favoritestatechanged";
    public static final String REPEAT_MODE_CHANGED = RETRO_MUSIC_PACKAGE_NAME + ".repeatmodechanged";
    public static final String SHUFFLE_MODE_CHANGED =
            RETRO_MUSIC_PACKAGE_NAME + ".shufflemodechanged";
    public static final String MEDIA_STORE_CHANGED = RETRO_MUSIC_PACKAGE_NAME + ".mediastorechanged";
    public static final String CYCLE_REPEAT = RETRO_MUSIC_PACKAGE_NAME + ".cyclerepeat";
    public static final String TOGGLE_SHUFFLE = RETRO_MUSIC_PACKAGE_NAME + ".toggleshuffle";
    public static final String TOGGLE_FAVORITE = RETRO_MUSIC_PACKAGE_NAME + ".togglefavorite";
    public static final String SAVED_POSITION = "POSITION";
    public static final String SAVED_POSITION_IN_TRACK = "POSITION_IN_TRACK";
    public static final String SAVED_SHUFFLE_MODE = "SHUFFLE_MODE";
    public static final String SAVED_REPEAT_MODE = "REPEAT_MODE";
    public static final int RELEASE_WAKELOCK = 0;
    public static final int TRACK_ENDED = 1;
    public static final int TRACK_WENT_TO_NEXT = 2;
    public static final int PLAY_SONG = 3;
    public static final int PREPARE_NEXT = 4;
    public static final int SET_POSITION = 5;
    public static final int FOCUS_CHANGE = 6;
    public static final int DUCK = 7;
    public static final int UNDUCK = 8;
    public static final int RESTORE_QUEUES = 9;
    public static final int SHUFFLE_MODE_NONE = 0;
    public static final int SHUFFLE_MODE_SHUFFLE = 1;
    public static final int REPEAT_MODE_NONE = 0;
    public static final int REPEAT_MODE_ALL = 1;
    public static final int REPEAT_MODE_THIS = 2;
    public static final int SAVE_QUEUES = 0;
    private static final long MEDIA_SESSION_ACTIONS =
            PlaybackStateCompat.ACTION_PLAY
                    | PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_PLAY_PAUSE
                    | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    | PlaybackStateCompat.ACTION_STOP
                    | PlaybackStateCompat.ACTION_SEEK_TO;
    private final IBinder musicBind = new MusicBinder();
    public int nextPosition = -1;

    public boolean pendingQuit = false;

    @Nullable
    public Playback playback;

    private PackageValidator mPackageValidator;

    private final AutoMusicProvider mMusicProvider = get(AutoMusicProvider.class);

    public boolean trackEndedByCrossfade = false;

    public int position = -1;

    private final AppWidgetBig appWidgetBig = AppWidgetBig.Companion.getInstance();

    private final AppWidgetCard appWidgetCard = AppWidgetCard.Companion.getInstance();

    private final AppWidgetClassic appWidgetClassic = AppWidgetClassic.Companion.getInstance();

    private final AppWidgetSmall appWidgetSmall = AppWidgetSmall.Companion.getInstance();

    private final AppWidgetText appWidgetText = AppWidgetText.Companion.getInstance();

    private final BroadcastReceiver widgetIntentReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, final Intent intent) {
                    final String command = intent.getStringExtra(EXTRA_APP_WIDGET_NAME);
                    final int[] ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                    if (command != null) {
                        switch (command) {
                            case AppWidgetClassic.NAME: {
                                appWidgetClassic.performUpdate(MusicService.this, ids);
                                break;
                            }
                            case AppWidgetSmall.NAME: {
                                appWidgetSmall.performUpdate(MusicService.this, ids);
                                break;
                            }
                            case AppWidgetBig.NAME: {
                                appWidgetBig.performUpdate(MusicService.this, ids);
                                break;
                            }
                            case AppWidgetCard.NAME: {
                                appWidgetCard.performUpdate(MusicService.this, ids);
                                break;
                            }
                            case AppWidgetText.NAME: {
                                appWidgetText.performUpdate(MusicService.this, ids);
                                break;
                            }
                        }
                    }
                }
            };
    private AudioManager audioManager;
    private final IntentFilter becomingNoisyReceiverIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private boolean becomingNoisyReceiverRegistered;
    private final IntentFilter bluetoothConnectedIntentFilter =
            new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
    private boolean bluetoothConnectedRegistered = false;
    private final IntentFilter headsetReceiverIntentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
    private boolean headsetReceiverRegistered = false;
    private MediaSessionCompat mediaSession;
    private ContentObserver mediaStoreObserver;
    private HandlerThread musicPlayerHandlerThread;
    private boolean notHandledMetaChangedForCurrentTrack;
    private List<Song> originalPlayingQueue = new ArrayList<>();
    private List<Song> playingQueue = new ArrayList<>();
    private boolean pausedByTransientLossOfFocus;
    private AudioVolumeObserver audioVolumeObserver = null;

    private final BroadcastReceiver becomingNoisyReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, @NonNull Intent intent) {
                    if (intent.getAction() != null
                            && intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                        pause();
                    }
                }
            };

    private PlaybackHandler playerHandler;

    private final AudioManager.OnAudioFocusChangeListener audioFocusListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(final int focusChange) {
                    playerHandler.obtainMessage(FOCUS_CHANGE, focusChange, 0).sendToTarget();
                }
            };

    private PlayingNotification playingNotification;
    private final BroadcastReceiver updateFavoriteReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, final Intent intent) {
                    updateNotification();
                }
            };
    private final BroadcastReceiver lockScreenReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (PreferenceUtil.INSTANCE.isLockScreen() && isPlaying()) {
                        Intent lockIntent = new Intent(context, LockScreenActivity.class);
                        lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(lockIntent);
                    }
                }
            };
    private QueueSaveHandler queueSaveHandler;
    private HandlerThread queueSaveHandlerThread;
    private boolean queuesRestored;
    private int repeatMode;
    private int shuffleMode;
    private final SongPlayCountHelper songPlayCountHelper = new SongPlayCountHelper();
    private final BroadcastReceiver bluetoothReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, final Intent intent) {
                    String action = intent.getAction();
                    if (action != null) {
                        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)
                                && PreferenceUtil.INSTANCE.isBluetoothSpeaker()) {
                            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                                if (getAudioManager().getDevices(AudioManager.GET_DEVICES_OUTPUTS).length > 0) {
                                    play();
                                }
                            } else {
                                if (getAudioManager().isBluetoothA2dpOn()) {
                                    play();
                                }
                            }
                        }
                    }
                }
            };
    private final PhoneStateListener phoneStateListener =
            new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    switch (state) {
                        case TelephonyManager.CALL_STATE_IDLE:
                            // Not in call: Play music
                            play();
                            break;
                        case TelephonyManager.CALL_STATE_RINGING:
                        case TelephonyManager.CALL_STATE_OFFHOOK:
                            // A call is dialing, active or on hold
                            pause();
                            break;
                        default:
                    }
                    super.onCallStateChanged(state, incomingNumber);
                }
            };
    private final BroadcastReceiver headsetReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action != null) {
                        if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                            int state = intent.getIntExtra("state", -1);
                            switch (state) {
                                case 0:
                                    pause();
                                    break;
                                case 1:
                                    play();
                                    break;
                            }
                        }
                    }
                }
            };
    private ThrottledSeekHandler throttledSeekHandler;
    private Handler uiThreadHandler;
    private PowerManager.WakeLock wakeLock;

    private static Bitmap copy(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.RGB_565;
        }
        try {
            return bitmap.copy(config, false);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getTrackUri(@NonNull Song song) {
        return MusicUtil.INSTANCE.getSongFileUri(song.getId()).toString();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        }
        wakeLock.setReferenceCounted(false);

        musicPlayerHandlerThread = new HandlerThread("PlaybackHandler");
        musicPlayerHandlerThread.start();
        playerHandler = new PlaybackHandler(this, musicPlayerHandlerThread.getLooper());

        // Set MultiPlayer when crossfade duration is 0 i.e. off
        if (PreferenceUtil.INSTANCE.getCrossFadeDuration() == 0) {
            playback = new MultiPlayer(this);
        } else {
            playback = new CrossFadePlayer(this);
        }

        playback.setCallbacks(this);

        setupMediaSession();

        // queue saving needs to run on a separate thread so that it doesn't block the playback handler
        // events
        queueSaveHandlerThread =
                new HandlerThread("QueueSaveHandler", Process.THREAD_PRIORITY_BACKGROUND);
        queueSaveHandlerThread.start();
        queueSaveHandler = new QueueSaveHandler(this, queueSaveHandlerThread.getLooper());

        uiThreadHandler = new Handler();

        registerReceiver(widgetIntentReceiver, new IntentFilter(APP_WIDGET_UPDATE));
        registerReceiver(updateFavoriteReceiver, new IntentFilter(FAVORITE_STATE_CHANGED));
        registerReceiver(lockScreenReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        initNotification();

        mediaStoreObserver = new MediaStoreObserver(this, playerHandler);
        throttledSeekHandler = new ThrottledSeekHandler(this, playerHandler);
        getContentResolver()
                .registerContentObserver(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaStoreObserver);
        getContentResolver()
                .registerContentObserver(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, true, mediaStoreObserver);
        getContentResolver()
                .registerContentObserver(
                        MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, true, mediaStoreObserver);
        getContentResolver()
                .registerContentObserver(
                        MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, true, mediaStoreObserver);
        getContentResolver()
                .registerContentObserver(
                        MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, true, mediaStoreObserver);

        getContentResolver()
                .registerContentObserver(
                        MediaStore.Audio.Media.INTERNAL_CONTENT_URI, true, mediaStoreObserver);
        getContentResolver()
                .registerContentObserver(
                        MediaStore.Audio.Albums.INTERNAL_CONTENT_URI, true, mediaStoreObserver);
        getContentResolver()
                .registerContentObserver(
                        MediaStore.Audio.Artists.INTERNAL_CONTENT_URI, true, mediaStoreObserver);
        getContentResolver()
                .registerContentObserver(
                        MediaStore.Audio.Genres.INTERNAL_CONTENT_URI, true, mediaStoreObserver);
        getContentResolver()
                .registerContentObserver(
                        MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI, true, mediaStoreObserver);

        audioVolumeObserver = new AudioVolumeObserver(this);
        audioVolumeObserver.register(AudioManager.STREAM_MUSIC, this);

        PreferenceUtil.INSTANCE.registerOnSharedPreferenceChangedListener(this);

        restoreState();

        sendBroadcast(new Intent("com.hamonie.RETRO_MUSIC_SERVICE_CREATED"));

        registerHeadsetEvents();
        registerBluetoothConnected();

        mPackageValidator = new PackageValidator(this, R.xml.allowed_media_browser_callers);
        mMusicProvider.setMusicService(this);
        setSessionToken(mediaSession.getSessionToken());
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(widgetIntentReceiver);
        unregisterReceiver(updateFavoriteReceiver);
        unregisterReceiver(lockScreenReceiver);
        if (becomingNoisyReceiverRegistered) {
            unregisterReceiver(becomingNoisyReceiver);
            becomingNoisyReceiverRegistered = false;
        }
        if (headsetReceiverRegistered) {
            unregisterReceiver(headsetReceiver);
            headsetReceiverRegistered = false;
        }
        if (bluetoothConnectedRegistered) {
            unregisterReceiver(bluetoothReceiver);
            bluetoothConnectedRegistered = false;
        }
        mediaSession.setActive(false);
        quit();
        releaseResources();
        getContentResolver().unregisterContentObserver(mediaStoreObserver);
        PreferenceUtil.INSTANCE.unregisterOnSharedPreferenceChangedListener(this);
        wakeLock.release();

        sendBroadcast(new Intent("com.hamonie.RETRO_MUSIC_SERVICE_DESTROYED"));
    }

    public void acquireWakeLock(long milli) {
        wakeLock.acquire(milli);
    }

    boolean pausedByZeroVolume;

    @Override
    public void onAudioVolumeChanged(int currentVolume, int maxVolume) {
        if (PreferenceUtil.INSTANCE.isPauseOnZeroVolume()) {
            if (isPlaying() && currentVolume < 1) {
                pause();
                System.out.println("Paused");
                pausedByZeroVolume = true;
            } else if (pausedByZeroVolume && currentVolume >= 1) {
                System.out.println("Played");
                play();
                pausedByZeroVolume = false;
            }
        }
    }

    public void addSong(int position, Song song) {
        playingQueue.add(position, song);
        originalPlayingQueue.add(position, song);
        notifyChange(QUEUE_CHANGED);
    }

    public void addSong(Song song) {
        playingQueue.add(song);
        originalPlayingQueue.add(song);
        notifyChange(QUEUE_CHANGED);
    }

    public void addSongs(int position, List<Song> songs) {
        playingQueue.addAll(position, songs);
        originalPlayingQueue.addAll(position, songs);
        notifyChange(QUEUE_CHANGED);
    }

    public void addSongs(List<Song> songs) {
        playingQueue.addAll(songs);
        originalPlayingQueue.addAll(songs);
        notifyChange(QUEUE_CHANGED);
    }

    public void back(boolean force) {
        if (getSongProgressMillis() > 2000) {
            seek(0);
        } else {
            playPreviousSong(force);
        }
    }

    public void clearQueue() {
        playingQueue.clear();
        originalPlayingQueue.clear();

        setPosition(-1);
        notifyChange(QUEUE_CHANGED);
    }

    public void cycleRepeatMode() {
        switch (getRepeatMode()) {
            case REPEAT_MODE_NONE:
                setRepeatMode(REPEAT_MODE_ALL);
                break;
            case REPEAT_MODE_ALL:
                setRepeatMode(REPEAT_MODE_THIS);
                break;
            default:
                setRepeatMode(REPEAT_MODE_NONE);
                break;
        }
    }

    public int getAudioSessionId() {
        if (playback != null) {
            return playback.getAudioSessionId();
        }
        return -1;
    }

    @NonNull
    public Song getCurrentSong() {
        return getSongAt(getPosition());
    }

    public Song getNextSong() {
        if (isLastTrack() && getRepeatMode() == REPEAT_MODE_NONE) {
            return null;
        } else {
            return getSongAt(getNextPosition(false));
        }
    }

    @NonNull
    public MediaSessionCompat getMediaSession() {
        return mediaSession;
    }

    public int getNextPosition(boolean force) {
        int position = getPosition() + 1;
        switch (getRepeatMode()) {
            case REPEAT_MODE_ALL:
                if (isLastTrack()) {
                    position = 0;
                }
                break;
            case REPEAT_MODE_THIS:
                if (force) {
                    if (isLastTrack()) {
                        position = 0;
                    }
                } else {
                    position -= 1;
                }
                break;
            default:
            case REPEAT_MODE_NONE:
                if (isLastTrack()) {
                    position -= 1;
                }
                break;
        }
        return position;
    }

    @Nullable
    public List<Song> getPlayingQueue() {
        return playingQueue;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(final int position) {
        // handle this on the handlers thread to avoid blocking the ui thread
        playerHandler.removeMessages(SET_POSITION);
        playerHandler.obtainMessage(SET_POSITION, position, 0).sendToTarget();
    }

    public int getPreviousPosition(boolean force) {
        int newPosition = getPosition() - 1;
        switch (repeatMode) {
            case REPEAT_MODE_ALL:
                if (newPosition < 0) {
                    if (getPlayingQueue() != null) {
                        newPosition = getPlayingQueue().size() - 1;
                    }
                }
                break;
            case REPEAT_MODE_THIS:
                if (force) {
                    if (newPosition < 0) {
                        if (getPlayingQueue() != null) {
                            newPosition = getPlayingQueue().size() - 1;
                        }
                    }
                } else {
                    newPosition = getPosition();
                }
                break;
            default:
            case REPEAT_MODE_NONE:
                if (newPosition < 0) {
                    newPosition = 0;
                }
                break;
        }
        return newPosition;
    }

    public long getQueueDurationMillis(int position) {
        long duration = 0;
        for (int i = position + 1; i < playingQueue.size(); i++) {
            duration += playingQueue.get(i).getDuration();
        }
        return duration;
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatMode(final int repeatMode) {
        switch (repeatMode) {
            case REPEAT_MODE_NONE:
            case REPEAT_MODE_ALL:
            case REPEAT_MODE_THIS:
                this.repeatMode = repeatMode;
                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putInt(SAVED_REPEAT_MODE, repeatMode)
                        .apply();
                prepareNext();
                handleAndSendChangeInternal(REPEAT_MODE_CHANGED);
                break;
        }
    }

    public int getShuffleMode() {
        return shuffleMode;
    }

    public void setShuffleMode(final int shuffleMode) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putInt(SAVED_SHUFFLE_MODE, shuffleMode)
                .apply();
        switch (shuffleMode) {
            case SHUFFLE_MODE_SHUFFLE:
                this.shuffleMode = shuffleMode;
                if (this.getPlayingQueue() != null) {
                    ShuffleHelper.INSTANCE.makeShuffleList(this.getPlayingQueue(), getPosition());
                }
                position = 0;
                break;
            case SHUFFLE_MODE_NONE:
                this.shuffleMode = shuffleMode;
                long currentSongId = Objects.requireNonNull(getCurrentSong()).getId();
                playingQueue = new ArrayList<>(originalPlayingQueue);
                int newPosition = 0;
                if (getPlayingQueue() != null) {
                    for (Song song : getPlayingQueue()) {
                        if (song.getId() == currentSongId) {
                            newPosition = getPlayingQueue().indexOf(song);
                        }
                    }
                }
                position = newPosition;
                break;
        }
        handleAndSendChangeInternal(SHUFFLE_MODE_CHANGED);
        notifyChange(QUEUE_CHANGED);
    }

    @NonNull
    public Song getSongAt(int position) {
        if (position >= 0 && getPlayingQueue() != null && position < getPlayingQueue().size()) {
            return getPlayingQueue().get(position);
        } else {
            return Song.Companion.getEmptySong();
        }
    }

    public int getSongDurationMillis() {
        if (playback != null) {
            return playback.duration();
        }
        return -1;
    }

    public int getSongProgressMillis() {
        if (playback != null) {
            return playback.position();
        }
        return -1;
    }

    public void handleAndSendChangeInternal(@NonNull final String what) {
        handleChangeInternal(what);
        sendChangeInternal(what);
    }

    public void initNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !PreferenceUtil.INSTANCE.isClassicNotification()) {
            playingNotification = new PlayingNotificationImpl();
        } else {
            playingNotification = new PlayingNotificationOreo();
        }
        playingNotification.init(this);
    }

    public boolean isLastTrack() {
        if (getPlayingQueue() != null) {
            return getPosition() == getPlayingQueue().size() - 1;
        }
        return false;
    }

    public boolean isPausedByTransientLossOfFocus() {
        return pausedByTransientLossOfFocus;
    }

    public void setPausedByTransientLossOfFocus(boolean pausedByTransientLossOfFocus) {
        this.pausedByTransientLossOfFocus = pausedByTransientLossOfFocus;
    }

    public boolean isPlaying() {
        return playback != null && playback.isPlaying();
    }

    public void moveSong(int from, int to) {
        if (from == to) {
            return;
        }
        final int currentPosition = getPosition();
        Song songToMove = playingQueue.remove(from);
        playingQueue.add(to, songToMove);
        if (getShuffleMode() == SHUFFLE_MODE_NONE) {
            Song tmpSong = originalPlayingQueue.remove(from);
            originalPlayingQueue.add(to, tmpSong);
        }
        if (from > currentPosition && to <= currentPosition) {
            position = currentPosition + 1;
        } else if (from < currentPosition && to >= currentPosition) {
            position = currentPosition - 1;
        } else if (from == currentPosition) {
            position = to;
        }
        notifyChange(QUEUE_CHANGED);
    }

    public void notifyChange(@NonNull final String what) {
        handleAndSendChangeInternal(what);
        sendPublicIntent(what);
    }

    @NonNull
    @Override
    public IBinder onBind(Intent intent) {
        // For Android auto, need to call super, or onGetRoot won't be called.
        if (intent != null && "android.media.browse.MediaBrowserService".equals(intent.getAction())) {
            return super.onBind(intent);
        }
        return musicBind;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {


        // Check origin to ensure we're not allowing any arbitrary app to browse app contents
        if (!mPackageValidator.isKnownCaller(clientPackageName, clientUid)) {
            // Request from an untrusted package: return an empty browser root
            return new BrowserRoot(AutoMediaIDHelper.MEDIA_ID_EMPTY_ROOT, null);
        } else {
            /**
             * By default return the browsable root. Treat the EXTRA_RECENT flag as a special case
             * and return the recent root instead.
             */
            boolean isRecentRequest = false;
            if (rootHints != null) {
                isRecentRequest = rootHints.getBoolean(EXTRA_RECENT);
            }
            String browserRootPath;
            if (isRecentRequest) {
                browserRootPath = AutoMediaIDHelper.RECENT_ROOT;
            } else {
                browserRootPath = AutoMediaIDHelper.MEDIA_ID_ROOT;
            }
            return new BrowserRoot(browserRootPath, null);
        }
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result) {
        if (parentId.equals(AutoMediaIDHelper.RECENT_ROOT)) {
            Song song = getCurrentSong();
            MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(String.valueOf(song.getId()))
                            .setTitle(song.getTitle())
                            .setSubtitle(song.getArtistName())
                            .setIconUri(MusicUtil.getMediaStoreAlbumCoverUri(song.getAlbumId()))
                            .build(), FLAG_PLAYABLE
            );
            result.sendResult(Collections.singletonList(mediaItem));
        } else {
            result.sendResult(mMusicProvider.getChildren(parentId, getResources()));
        }
    }

    @Override
    public void onSharedPreferenceChanged(
            @NonNull SharedPreferences sharedPreferences, @NonNull String key) {
        switch (key) {
            case CROSS_FADE_DURATION:
                int progress = getSongProgressMillis();
                boolean wasPlaying = isPlaying();
                /* Switch to MultiPlayer if Crossfade duration is 0 and
                Playback is not an instance of MultiPlayer */
                if (playback != null)
                  playback.setCrossFadeDuration(PreferenceUtil.INSTANCE.getCrossFadeDuration());
                if (!(playback instanceof MultiPlayer) && PreferenceUtil.INSTANCE.getCrossFadeDuration() == 0) {
                    if (playback != null) {
                        playback.release();
                    }
                    playback = null;
                    playback = new MultiPlayer(this);
                    playback.setCallbacks(this);
                    if (openTrackAndPrepareNextAt(position)) {
                        seek(progress);
                        if (wasPlaying) {
                            play();
                        }
                    }
                }
                /* Switch to CrossFadePlayer if Crossfade duration is greater than 0 and
                Playback is not an instance of CrossFadePlayer */
                else if (!(playback instanceof CrossFadePlayer) && PreferenceUtil.INSTANCE.getCrossFadeDuration() > 0) {
                    if (playback != null) {
                        playback.release();
                    }
                    playback = null;
                    playback = new CrossFadePlayer(this);
                    playback.setCallbacks(this);
                    if (openTrackAndPrepareNextAt(position)) {
                        seek(progress);
                        if (wasPlaying) {
                            play();
                        }
                    }
                }
                break;
            case ALBUM_ART_ON_LOCK_SCREEN:
            case BLURRED_ALBUM_ART:
                updateMediaSessionMetaData();
                break;
            case COLORED_NOTIFICATION:
                updateNotification();
                break;
            case CLASSIC_NOTIFICATION:
                initNotification();
                updateNotification();
                break;
            case TOGGLE_HEADSET:
                registerHeadsetEvents();
                break;
        }
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            restoreQueuesAndPositionIfNecessary();
            String action = intent.getAction();
            switch (action) {
                case ACTION_TOGGLE_PAUSE:
                    if (isPlaying()) {
                        pause();
                    } else {
                        play();
                    }
                    break;
                case ACTION_PAUSE:
                    pause();
                    break;
                case ACTION_PLAY:
                    play();
                    break;
                case ACTION_PLAY_PLAYLIST:
                    playFromPlaylist(intent);
                    break;
                case ACTION_REWIND:
                    back(true);
                    break;
                case ACTION_SKIP:
                    playNextSong(true);
                    break;
                case ACTION_STOP:
                case ACTION_QUIT:
                    pendingQuit = false;
                    quit();
                    break;
                case ACTION_PENDING_QUIT:
                    pendingQuit = true;
                    break;
                case TOGGLE_FAVORITE:
                    MusicUtil.INSTANCE.toggleFavorite(getApplicationContext(), getCurrentSong());
                    break;
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onTrackEnded() {
        acquireWakeLock(30000);
        playerHandler.sendEmptyMessage(TRACK_ENDED);
    }

    @Override
    public void onTrackEndedWithCrossfade() {
        trackEndedByCrossfade = true;
        acquireWakeLock(30000);
        playerHandler.sendEmptyMessage(TRACK_ENDED);
    }

    @Override
    public void onTrackWentToNext() {
        playerHandler.sendEmptyMessage(TRACK_WENT_TO_NEXT);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (!isPlaying()) {
            stopSelf();
        }
        return true;
    }

    public void openQueue(
            @Nullable final List<Song> playingQueue,
            final int startPosition,
            final boolean startPlaying) {
        if (playingQueue != null
                && !playingQueue.isEmpty()
                && startPosition >= 0
                && startPosition < playingQueue.size()) {
            // it is important to copy the playing queue here first as we might add/remove songs later
            originalPlayingQueue = new ArrayList<>(playingQueue);
            this.playingQueue = new ArrayList<>(originalPlayingQueue);

            int position = startPosition;
            if (shuffleMode == SHUFFLE_MODE_SHUFFLE) {
                ShuffleHelper.INSTANCE.makeShuffleList(this.playingQueue, startPosition);
                position = 0;
            }
            if (startPlaying) {
                playSongAt(position);
            } else {
                setPosition(position);
            }
            notifyChange(QUEUE_CHANGED);
        }
    }

    public boolean openTrackAndPrepareNextAt(int position) {
        synchronized (this) {
            this.position = position;
            boolean prepared = openCurrent();
            if (prepared) {
                prepareNextImpl();
            }
            notifyChange(META_CHANGED);
            notHandledMetaChangedForCurrentTrack = false;
            return prepared;
        }
    }

    public void pause() {
        pausedByTransientLossOfFocus = false;
        if (playback != null && playback.isPlaying()) {
            startFadeAnimator(playback, false, () -> {
                //Code to run when Animator Ends
                if (playback != null) {
                    playback.pause();
                }
                notifyChange(PLAY_STATE_CHANGED);
            });
        }
    }

    public void forcePause() {
        pausedByTransientLossOfFocus = false;
        if (playback != null && playback.isPlaying()) {
            playback.pause();
            notifyChange(PLAY_STATE_CHANGED);
        }
    }

    public void play() {
        synchronized (this) {
            if (requestFocus()) {
                if (playback != null && !playback.isPlaying()) {
                    if (!playback.isInitialized()) {
                        playSongAt(getPosition());
                    } else {
                        //Don't Start playing when it's casting
                        if (MusicPlayerRemote.INSTANCE.isCasting()) {
                            return;
                        }
                        startFadeAnimator(playback, true, () -> {
                            // Code when Animator Ends
                            if (!becomingNoisyReceiverRegistered) {
                                registerReceiver(becomingNoisyReceiver, becomingNoisyReceiverIntentFilter);
                                becomingNoisyReceiverRegistered = true;
                            }
                            if (notHandledMetaChangedForCurrentTrack) {
                                handleChangeInternal(META_CHANGED);
                                notHandledMetaChangedForCurrentTrack = false;
                            }

                            // fixes a bug where the volume would stay ducked because the
                            // AudioManager.AUDIOFOCUS_GAIN event is not sent
                            playerHandler.removeMessages(DUCK);
                            playerHandler.sendEmptyMessage(UNDUCK);
                        });
                        //Start Playback with Animator
                        playback.start();
                        notifyChange(PLAY_STATE_CHANGED);
                    }
                }
            } else {
                Toast.makeText(
                        this, getResources().getString(R.string.audio_focus_denied), Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    public void playNextSong(boolean force) {
        playSongAt(getNextPosition(force));
    }

    public void playPreviousSong(boolean force) {
        playSongAt(getPreviousPosition(force));
    }

    public void playSongAt(final int position) {
        // handle this on the handlers thread to avoid blocking the ui thread
        playerHandler.removeMessages(PLAY_SONG);
        playerHandler.obtainMessage(PLAY_SONG, position, 0).sendToTarget();
    }

    public void playSongAtImpl(int position) {
        if (!trackEndedByCrossfade) {
            // This is only imp if we are using crossfade
            if (playback instanceof CrossFadePlayer) {
                ((CrossFadePlayer) playback).sourceChangedByUser();
            }
        } else {
            trackEndedByCrossfade = false;
        }
        if (openTrackAndPrepareNextAt(position)) {
            play();
        } else {
            Toast.makeText(this, getResources().getString(R.string.unplayable_file), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void playSongs(ArrayList<Song> songs, int shuffleMode) {
        if (songs != null && !songs.isEmpty()) {
            if (shuffleMode == SHUFFLE_MODE_SHUFFLE) {
                int startPosition = new Random().nextInt(songs.size());
                openQueue(songs, startPosition, false);
                setShuffleMode(shuffleMode);
            } else {
                openQueue(songs, 0, false);
            }
            play();
        } else {
            Toast.makeText(getApplicationContext(), R.string.playlist_is_empty, Toast.LENGTH_LONG).show();
        }
    }

    public void prepareNextImpl() {
        synchronized (this) {
            try {
                int nextPosition = getNextPosition(false);
                if (playback != null) {
                    playback.setNextDataSource(getTrackUri(Objects.requireNonNull(getSongAt(nextPosition))));
                }
                this.nextPosition = nextPosition;
            } catch (Exception e) {
            }
        }
    }

    public void quit() {
        pause();
        playingNotification.stop();

        closeAudioEffectSession();
        getAudioManager().abandonAudioFocus(audioFocusListener);
        stopSelf();
    }

    public void releaseWakeLock() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public void removeSong(int position) {
        if (getShuffleMode() == SHUFFLE_MODE_NONE) {
            playingQueue.remove(position);
            originalPlayingQueue.remove(position);
        } else {
            originalPlayingQueue.remove(playingQueue.remove(position));
        }

        rePosition(position);

        notifyChange(QUEUE_CHANGED);
    }

    public void removeSongImpl(@NonNull Song song) {
        for (int i = 0; i < playingQueue.size(); i++) {
            if (playingQueue.get(i).getId() == song.getId()) {
                playingQueue.remove(i);
                rePosition(i);
            }
        }
        for (int i = 0; i < originalPlayingQueue.size(); i++) {
            if (originalPlayingQueue.get(i).getId() == song.getId()) {
                originalPlayingQueue.remove(i);
            }
        }
    }

    public void removeSong(@NonNull Song song) {
        removeSongImpl(song);
        notifyChange(QUEUE_CHANGED);
    }

    public void removeSongs(@NonNull List<Song> songs) {
        for (Song song : songs) {
            removeSongImpl(song);
        }
        notifyChange(QUEUE_CHANGED);
    }

    public synchronized void restoreQueuesAndPositionIfNecessary() {
        if (!queuesRestored && playingQueue.isEmpty()) {
            List<Song> restoredQueue = MusicPlaybackQueueStore.getInstance(this).getSavedPlayingQueue();
            List<Song> restoredOriginalQueue =
                    MusicPlaybackQueueStore.getInstance(this).getSavedOriginalPlayingQueue();
            int restoredPosition =
                    PreferenceManager.getDefaultSharedPreferences(this).getInt(SAVED_POSITION, -1);
            int restoredPositionInTrack =
                    PreferenceManager.getDefaultSharedPreferences(this).getInt(SAVED_POSITION_IN_TRACK, -1);

            if (restoredQueue.size() > 0
                    && restoredQueue.size() == restoredOriginalQueue.size()
                    && restoredPosition != -1) {
                this.originalPlayingQueue = restoredOriginalQueue;
                this.playingQueue = restoredQueue;

                position = restoredPosition;
                openCurrent();
                prepareNext();

                if (restoredPositionInTrack > 0) {
                    seek(restoredPositionInTrack);
                }

                notHandledMetaChangedForCurrentTrack = true;
                sendChangeInternal(META_CHANGED);
                sendChangeInternal(QUEUE_CHANGED);
            }
        }
        queuesRestored = true;
    }

    public void runOnUiThread(Runnable runnable) {
        uiThreadHandler.post(runnable);
    }

    public void savePositionInTrack() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putInt(SAVED_POSITION_IN_TRACK, getSongProgressMillis())
                .apply();
    }

    public void saveQueuesImpl() {
        MusicPlaybackQueueStore.getInstance(this).saveQueues(playingQueue, originalPlayingQueue);
    }

    public void saveState() {
        saveQueues();
        savePosition();
        savePositionInTrack();
    }

    public int seek(int millis) {
        synchronized (this) {
            try {
                int newPosition = 0;
                if (playback != null) {
                    newPosition = playback.seek(millis);
                }
                throttledSeekHandler.notifySeek();
                return newPosition;
            } catch (Exception e) {
                return -1;
            }
        }
    }

    // to let other apps know whats playing. i.E. last.fm (scrobbling) or musixmatch
    public void sendPublicIntent(@NonNull final String what) {
        final Intent intent = new Intent(what.replace(RETRO_MUSIC_PACKAGE_NAME, MUSIC_PACKAGE_NAME));

        final Song song = getCurrentSong();

        if (song != null) {
            intent.putExtra("id", song.getId());
            intent.putExtra("artist", song.getArtistName());
            intent.putExtra("album", song.getAlbumName());
            intent.putExtra("track", song.getTitle());
            intent.putExtra("duration", song.getDuration());
            intent.putExtra("position", (long) getSongProgressMillis());
            intent.putExtra("playing", isPlaying());
            intent.putExtra("scrobbling_source", RETRO_MUSIC_PACKAGE_NAME);
            sendStickyBroadcast(intent);
        }
    }

    public void toggleShuffle() {
        if (getShuffleMode() == SHUFFLE_MODE_NONE) {
            setShuffleMode(SHUFFLE_MODE_SHUFFLE);
        } else {
            setShuffleMode(SHUFFLE_MODE_NONE);
        }
    }

    public void updateMediaSessionPlaybackState() {
        PlaybackStateCompat.Builder stateBuilder =
                new PlaybackStateCompat.Builder()
                        .setActions(MEDIA_SESSION_ACTIONS)
                        .setState(
                                isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                                getSongProgressMillis(),
                                1);

        setCustomAction(stateBuilder);

        mediaSession.setPlaybackState(stateBuilder.build());
    }

    public void updateNotification() {
        if (playingNotification != null && getCurrentSong().getId() != -1) {
            playingNotification.update();
        }
    }

    public void updateMediaSessionMetaData() {
        Log.i(TAG, "onResourceReady: ");
        final Song song = getCurrentSong();

        if (song.getId() == -1) {
            mediaSession.setMetadata(null);
            return;
        }

        final MediaMetadataCompat.Builder metaData = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getArtistName())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, song.getArtistName())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.getAlbumName())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getTitle())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.getDuration())
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, getPosition() + 1)
                .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, song.getYear())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            metaData.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, getPlayingQueue().size());
        }

        if (PreferenceUtil.INSTANCE.isAlbumArtOnLockScreen()) {
            final Point screenSize = RetroUtil.getScreenSize(MusicService.this);
            final RequestBuilder<Bitmap> request = GlideApp.with(MusicService.this).asBitmap().songCoverOptions(song).load(RetroGlideExtension.INSTANCE.getSongModel(song));
            if (PreferenceUtil.INSTANCE.isBlurredAlbumArt()) {
                request.transform(new BlurTransformation.Builder(MusicService.this).build());
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    request.into(new SimpleTarget<Bitmap>(screenSize.x, screenSize.y) {
                        @Override
                        public void onLoadFailed(Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            mediaSession.setMetadata(metaData.build());
                        }

                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                            metaData.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, copy(resource));
                            mediaSession.setMetadata(metaData.build());
                        }
                    });
                }
            });
        } else {
            mediaSession.setMetadata(metaData.build());
        }
    }

    private void closeAudioEffectSession() {
        final Intent audioEffectsIntent =
                new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        if (playback != null) {
            audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, playback.getAudioSessionId());
        }
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(audioEffectsIntent);
    }

    private AudioManager getAudioManager() {
        if (audioManager == null) {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
        return audioManager;
    }

    private void handleChangeInternal(@NonNull final String what) {
        switch (what) {
            case PLAY_STATE_CHANGED:
                updateNotification();
                updateMediaSessionPlaybackState();
                final boolean isPlaying = isPlaying();
                if (!isPlaying && getSongProgressMillis() > 0) {
                    savePositionInTrack();
                }
                songPlayCountHelper.notifyPlayStateChanged(isPlaying);
                break;
            case FAVORITE_STATE_CHANGED:
            case META_CHANGED:
                updateNotification();
                updateMediaSessionMetaData();
                updateMediaSessionPlaybackState();
                savePosition();
                savePositionInTrack();
                final Song currentSong = getCurrentSong();
                HistoryStore.getInstance(this).addSongId(currentSong.getId());
                if (songPlayCountHelper.shouldBumpPlayCount()) {
                    SongPlayCountStore.getInstance(this).bumpPlayCount(songPlayCountHelper.getSong().getId());
                }
                songPlayCountHelper.notifySongChanged(currentSong);
                break;
            case QUEUE_CHANGED:
                updateMediaSessionMetaData(); // because playing queue size might have changed
                saveState();
                if (playingQueue.size() > 0) {
                    prepareNext();
                } else {
                    playingNotification.stop();
                }
                break;
        }
    }

    private boolean openCurrent() {
        synchronized (this) {
            try {
                if (playback != null) {
                    return playback.setDataSource(getTrackUri(Objects.requireNonNull(getCurrentSong())));
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    private void playFromPlaylist(Intent intent) {
        AbsSmartPlaylist playlist = intent.getParcelableExtra(INTENT_EXTRA_PLAYLIST);
        int shuffleMode = intent.getIntExtra(INTENT_EXTRA_SHUFFLE_MODE, getShuffleMode());
        if (playlist != null) {
            List<Song> playlistSongs = playlist.songs();
            if (!playlistSongs.isEmpty()) {
                if (shuffleMode == SHUFFLE_MODE_SHUFFLE) {
                    int startPosition = new Random().nextInt(playlistSongs.size());
                    openQueue(playlistSongs, startPosition, true);
                    setShuffleMode(shuffleMode);
                } else {
                    openQueue(playlistSongs, 0, true);
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.playlist_is_empty, Toast.LENGTH_LONG)
                        .show();
            }
        } else {
            Toast.makeText(getApplicationContext(), R.string.playlist_is_empty, Toast.LENGTH_LONG).show();
        }
    }

    private void prepareNext() {
        playerHandler.removeMessages(PREPARE_NEXT);
        playerHandler.obtainMessage(PREPARE_NEXT).sendToTarget();
    }

    private void rePosition(int deletedPosition) {
        int currentPosition = getPosition();
        if (deletedPosition < currentPosition) {
            position = currentPosition - 1;
        } else if (deletedPosition == currentPosition) {
            if (playingQueue.size() > deletedPosition) {
                setPosition(position);
            } else {
                setPosition(position - 1);
            }
        }
    }

    private void registerBluetoothConnected() {
        Log.i(TAG, "registerBluetoothConnected: ");
        if (!bluetoothConnectedRegistered) {
            registerReceiver(bluetoothReceiver, bluetoothConnectedIntentFilter);
            bluetoothConnectedRegistered = true;
        }
    }

    private void registerHeadsetEvents() {
        if (!headsetReceiverRegistered && PreferenceUtil.INSTANCE.isHeadsetPlugged()) {
            registerReceiver(headsetReceiver, headsetReceiverIntentFilter);
            headsetReceiverRegistered = true;
        }
    }

    private void releaseResources() {
        playerHandler.removeCallbacksAndMessages(null);
        musicPlayerHandlerThread.quitSafely();
        queueSaveHandler.removeCallbacksAndMessages(null);
        queueSaveHandlerThread.quitSafely();
        if (playback != null) {
            playback.release();
        }
        playback = null;
        mediaSession.release();
    }

    private boolean requestFocus() {
        return (getAudioManager()
                .requestAudioFocus(
                        audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
                == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    }

    private void restoreState() {
        shuffleMode = PreferenceManager.getDefaultSharedPreferences(this).getInt(SAVED_SHUFFLE_MODE, 0);
        repeatMode = PreferenceManager.getDefaultSharedPreferences(this).getInt(SAVED_REPEAT_MODE, 0);
        handleAndSendChangeInternal(SHUFFLE_MODE_CHANGED);
        handleAndSendChangeInternal(REPEAT_MODE_CHANGED);

        playerHandler.removeMessages(RESTORE_QUEUES);
        playerHandler.sendEmptyMessage(RESTORE_QUEUES);
    }

    private void savePosition() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putInt(SAVED_POSITION, getPosition())
                .apply();
    }

    private void saveQueues() {
        queueSaveHandler.removeMessages(SAVE_QUEUES);
        queueSaveHandler.sendEmptyMessage(SAVE_QUEUES);
    }

    private void sendChangeInternal(final String what) {
        sendBroadcast(new Intent(what));
        appWidgetBig.notifyChange(this, what);
        appWidgetClassic.notifyChange(this, what);
        appWidgetSmall.notifyChange(this, what);
        appWidgetCard.notifyChange(this, what);
        appWidgetText.notifyChange(this, what);
    }

    private void setCustomAction(PlaybackStateCompat.Builder stateBuilder) {
        int repeatIcon = R.drawable.ic_repeat; // REPEAT_MODE_NONE
        if (getRepeatMode() == REPEAT_MODE_THIS) {
            repeatIcon = R.drawable.ic_repeat_one;
        } else if (getRepeatMode() == REPEAT_MODE_ALL) {
            repeatIcon = R.drawable.ic_repeat_white_circle;
        }
        stateBuilder.addCustomAction(
                new PlaybackStateCompat.CustomAction.Builder(
                        CYCLE_REPEAT, getString(R.string.action_cycle_repeat), repeatIcon)
                        .build());

        final int shuffleIcon =
                getShuffleMode() == SHUFFLE_MODE_NONE
                        ? R.drawable.ic_shuffle_off_circled
                        : R.drawable.ic_shuffle_on_circled;
        stateBuilder.addCustomAction(
                new PlaybackStateCompat.CustomAction.Builder(
                        TOGGLE_SHUFFLE, getString(R.string.action_toggle_shuffle), shuffleIcon)
                        .build());

        final int favoriteIcon =
                MusicUtil.INSTANCE.isFavorite(getApplicationContext(), getCurrentSong())
                        ? R.drawable.ic_favorite
                        : R.drawable.ic_favorite_border;
        stateBuilder.addCustomAction(
                new PlaybackStateCompat.CustomAction.Builder(
                        TOGGLE_FAVORITE, getString(R.string.action_toggle_favorite), favoriteIcon)
                        .build());
    }

    private void setupMediaSession() {
        ComponentName mediaButtonReceiverComponentName =
                new ComponentName(getApplicationContext(), MediaButtonIntentReceiver.class);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mediaButtonReceiverComponentName);

        PendingIntent mediaButtonReceiverPendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            mediaButtonReceiverPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, PendingIntent.FLAG_MUTABLE);
        } else {
            mediaButtonReceiverPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);
        }

        mediaSession = new MediaSessionCompat(
                this,
                "RetroMusicPlayer",
                mediaButtonReceiverComponentName,
                mediaButtonReceiverPendingIntent);
        MediaSessionCallback mediasessionCallback =
                new MediaSessionCallback(getApplicationContext(), this);
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                        | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(mediasessionCallback);
        mediaSession.setActive(true);
        mediaSession.setMediaButtonReceiver(mediaButtonReceiverPendingIntent);
    }

    public class MusicBinder extends Binder {

        @NonNull
        public MusicService getService() {
            return MusicService.this;
        }
    }
}