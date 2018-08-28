package com.music.player.server;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.IBinder;

import com.music.player.APPContext;
import com.music.player.Config.Constants;
import com.music.player.bean.Song;
import com.music.player.db.CollectDBHelper;
import com.music.player.musicUtils.MusicUtils;

import java.io.IOException;
import java.util.List;


/**
 * Created by wj on 2018/8/21.
 */

public class MusicServer extends Service implements MediaPlayer.OnCompletionListener {
    private MediaPlayer mPlayer;
    private PlayerReceiver mReceiver;
    private List<Song> mSongs;
    private Context mContext;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        initData();
    }


    private void initData() {
        mPlayer = APPContext.getPlager();
        mSongs = APPContext.getInstance().getSongs();
        mReceiver = new PlayerReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.CHANGE_MUSIC_SONG);
        filter.addAction(Constants.CHANGE_ALL_LIST);
        registerReceiver(mReceiver, filter);
        //撥放完成事件綁定監聽!
        mPlayer.setOnCompletionListener(this);
    }


    /**
     * 初始撥放器
     */
    private void startNewSong(Song song) {
        try {
            if(mPlayer !=null){
                mPlayer.stop();
            }
            mPlayer.reset();
            mPlayer.setDataSource(song.path);
            mPlayer.prepare();
            mPlayer.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 完成撥放 依據button 選擇下一首該如何撥放
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        sendBroadcast(new Intent(Constants.CHANGE_PLAYED));
//        nextSong();
    }

    private class PlayerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
//            Log.d(TAG, "onReceive: "+intent.getAction());
            if(Constants.CHANGE_MUSIC_SONG.equals(intent.getAction())) {
                Song song = (Song) intent.getSerializableExtra("song");
                if (song != null) {
                    startNewSong(song);
                }
            }else if (Constants.CHANGE_ALL_LIST.equals(intent.getAction())) {
                mSongs.clear();
                mSongs.addAll(MusicUtils.getMusicData(mContext));
            }else if (Constants.CHANGE_COLLECT_LIST.equals(intent.getAction())) {
                mSongs.clear();
                mSongs.addAll(CollectDBHelper.getInstance(mContext).getCollectSong(mContext));
            }
        }
    }

    @Override
    public void onDestroy() {
        mPlayer.stop();
        mPlayer.reset();
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
