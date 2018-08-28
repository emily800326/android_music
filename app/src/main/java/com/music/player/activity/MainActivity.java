package com.music.player.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.music.player.APPContext;
import com.music.player.Config.Constants;
import com.music.player.R;
import com.music.player.adapter.MainMusicAdapter;
import com.music.player.base.BaseActivity;
import com.music.player.bean.Song;
import com.music.player.db.CollectDBHelper;
import com.music.player.musicUtils.MusicUtils;
import com.music.player.server.MusicServer;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;

public class MainActivity extends BaseActivity implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener,
        MainMusicAdapter.OnMusicItemListener {


    Intent intent = getIntent();
    private ImageView mMainIconIv;
    private ImageView mExitIv;
    private TextView mMusicName;
    private TextView mMusicAuthor;
    private SeekBar mSeekBar;
    private TextView mTimeTvFirst;
    private TextView mTimeTv;

    private ImageView mStopIv;
    private ImageView mLastIv;
    private ImageView mNextIv;
    private CheckBox mRepeatIv;
    private CheckBox mShuffleIv;
    private CheckBox mStateCb;

    private ImageView mCollectLl;
    private RecyclerView mRecycler;

    private MainMusicAdapter mAdapter;
    private List<Song> mSongs;
    private MediaPlayer mPlayer;

    private Timer mTimer;
    private TimerTask mTimerTask;

    private MainHandler mHandler;
    private MainReceiver mMainReceiver;
    private final int UP_TIME = 2;



    @Override
    public void setContentView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
    }

    @Override
    public void findView() {
        mMainIconIv = (ImageView) findViewById(R.id.activity_main_iv);
        mExitIv = (ImageView) findViewById(R.id.activity_main_exit_iv);
        mMusicName = (TextView) findViewById(R.id.activity_main_music_name);
        mMusicAuthor = (TextView) findViewById(R.id.activity_main_music_author);
        mSeekBar = (SeekBar) findViewById(R.id.activity_main_music_seek);
        mTimeTvFirst= (TextView) findViewById(R.id.activity_main_music_start);
        mTimeTv = (TextView) findViewById(R.id.activity_main_music_time);
        mStopIv = (ImageView) findViewById(R.id.activity_main_stop);
        mLastIv = (ImageView) findViewById(R.id.activity_main_last_iv);
        mRepeatIv=(CheckBox) findViewById(R.id.activity_main_repeat);
        mShuffleIv=(CheckBox) findViewById(R.id.activity_main_shuffle);
        mNextIv = (ImageView) findViewById(R.id.activity_main_next_iv);
        mStateCb = (CheckBox) findViewById(R.id.activity_main_state_iv);
        mCollectLl = (ImageView) findViewById(R.id.activity_main_collect_ll);
        mRecycler = (RecyclerView) findViewById(R.id.activity_main_recycler);

    }

    @Override
    public void initView() {
        mPlayer = APPContext.getPlager();
        mStateCb.setChecked(mPlayer.isPlaying());
    }

    private void setSongView(Song song) {
        if (song != null) {
            mMusicName.setText(song.song);
            mMusicAuthor.setText(song.singer);
            mTimeTvFirst.setText(MusicUtils.formatTime(mPlayer.getCurrentPosition()));
            mTimeTv.setText(MusicUtils.formatTime(song.duration));
        }
    }


    @Override
    public void initData() {

        mMainReceiver = new MainReceiver();
        IntentFilter filter = new IntentFilter();

        filter.addAction(Constants.CHANGE_MUSIC_UP_MUSIC_DATA);
        filter.addAction(Constants.CHANGE_PLAYED);

        registerReceiver(mMainReceiver, filter);
        mHandler = new MainHandler();

        mSongs = MusicUtils.getMusicData(mContext);
//        Log.d(TAG, "initData---> "+mSongs);
        mAdapter = new MainMusicAdapter(this, mSongs, this);

//        recycleview 設定布局 LinearLayout
        mRecycler.setLayoutManager(new LinearLayoutManager(mContext));
        mRecycler.setAdapter(mAdapter);


        Song song = APPContext.getInstance().getSong();
        setSongView(song);
        if (mPlayer.isPlaying()) {
            mSeekBar.setMax(song.duration);
            mSeekBar.setProgress(mPlayer.getCurrentPosition());
            recordProgress();
        }
    }


    @Override
    public void initListener() {
        mExitIv.setOnClickListener(this);
        mStopIv.setOnClickListener(this);
        mLastIv.setOnClickListener(this);
        mNextIv.setOnClickListener(this);
        mRepeatIv.setOnCheckedChangeListener(this);
        mShuffleIv.setOnCheckedChangeListener(this);
        mCollectLl.setOnClickListener(this);
        mStateCb.setOnCheckedChangeListener(this);
        mSeekBar.setOnSeekBarChangeListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.activity_main_stop:
                stopSong();
                break;
            case R.id.activity_main_last_iv:
                lastSong();
                break;
            case R.id.activity_main_next_iv:
                nextSong();
                break;
            case R.id.activity_main_exit_iv:
                stopService(new Intent(this, MusicServer.class));
                System.exit(0);
                break;
            case R.id.activity_main_collect_ll:
                startActivityForResult(new Intent(this, CollectActivity.class), 1);
                break;
        }
    }
    /**
     * 停止這首
     */
    private void stopSong() {
        if(mPlayer!=null){
            //mediaplayer 是MediaPlayer的 instance
            mPlayer.stop();
            try{
                //stop後重新撥放的話要進入prepared狀態
                mPlayer.prepare();
                //將撥放時間重置
                mPlayer.seekTo(0);
                mRepeatIv.setChecked(false);
                mShuffleIv.setChecked(false);

            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
    /**
     * 上一首
     */
    private void lastSong() {
        if (mSongs.size() > 0) {
            //歌曲數
            Song song = APPContext.getInstance().getSong();
            int lastPosition = -1;
            if (song != null) {
                for (Song so : mSongs) {
                    lastPosition++;
                    if (so.path.equals(song.path)) {
                        if (lastPosition == 0) {
                            lastPosition = mSongs.size() - 1;
                        } else {
                            lastPosition--;
                        }
                        break;
                    }
                }
            } else {
                lastPosition++;
            }
            Song lastSong = mSongs.get(lastPosition);
            sendBroadcastSong(lastSong);
        }
    }

    /**
     * 下一首
     */
    private void nextSong() {
        if(mShuffleIv.isChecked()){
            Log.d(TAG, "nextSong  ----clicked shuffle");
            shuffleSong();
        }else if(mRepeatIv.isChecked()){
            Log.d(TAG, "nextSong  ----clicked repeat");
            repeatSong();
        }else{
            if (mSongs.size() > 0) {
                Song song = APPContext.getInstance().getSong();
//                Log.d(TAG, "nextSong  b:"+mSongs);
                int lastPosition = -1;
                if (song != null) {
                    for (Song so : mSongs) {
//                        Log.d(TAG, "nextSong  c:"+so);
                        lastPosition++;
                        if (so.path.equals(song.path)) {
                            if (lastPosition == mSongs.size() - 1) {
                                lastPosition = 0;
                            } else {
                                lastPosition++;
                            }
                            break;
                        }
                    }
                } else {
                    lastPosition++;
                }
                Song lastSong = mSongs.get(lastPosition);
                sendBroadcastSong(lastSong);
            }
        }


    }
    /**
     * 重複此首
     */
    private void repeatSong() {

        Song song = APPContext.getInstance().getSong();
        int lastPosition =song.song_Num;
        Song lastSong = mSongs.get(lastPosition-1);
        sendBroadcastSong(lastSong);

    }
    /**
     * 隨機撥放
     */
    private void shuffleSong() {
        if(mRepeatIv.isChecked()){
            Song song = APPContext.getInstance().getSong();
            int lastPosition =song.song_Num;
            Song lastSong = mSongs.get(lastPosition-1);
            sendBroadcastSong(lastSong);
        }else{
            if (mSongs.size() > 0) {
                Song song = APPContext.getInstance().getSong();
                int lastPosition = new Random().nextInt(mSongs.size());


                if (song != null) {
                    for (Song so : mSongs) {
                        if (so.path.equals(song.path)) {
                            if (lastPosition == mSongs.size() - 1) {
                                lastPosition = 0;
                            } else {
                                lastPosition++;
                            }
                            break;
                        }
                    }

                } else {
                    lastPosition++;
                }

                Song lastSong = mSongs.get(lastPosition);
                sendBroadcastSong(lastSong);
            }

        }

    }
    private void sendBroadcastSong(Song song) {
        sendBroadcast(new Intent(Constants.CHANGE_ALL_LIST));
        Intent intent = new Intent(Constants.CHANGE_MUSIC_SONG);
        intent.putExtra("song", song);
        Log.d(TAG, "sendBroadcastSong:  "+intent);
        sendBroadcast(intent);
        APPContext.getInstance().setSong(song);
        setSongView(song);
        mSeekBar.setMax(song.duration);
        mSeekBar.setProgress(0);
        mAdapter.notifyDataSetChanged();
        mStateCb.setChecked(true);
        recordProgress();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        Log.d(TAG, "onChecked: " + buttonView);

        switch (buttonView.getId()) {
            case R.id.activity_main_state_iv:
//                Log.d(TAG, "onCheckedChanged: in main state");
                if (isChecked) {
//                    Log.d(TAG, "play: in main state");
                    if (mSongs.size() > 0) {
                        Song song = APPContext.getInstance().getSong();
                        if (song != null) {
                            mPlayer.start();
                            recordProgress();
                        } else {
                            song = mSongs.get(0);
                            sendBroadcastSong(song);
                        }
                    }
                } else {
//                    Log.d(TAG, "pause: in main state");
                    cancelTimer();
                    mPlayer.pause();
                }
                break;

            case R.id.activity_main_repeat:
//                Intent intent =  new Intent(Constants.SET_REPEAT); //設定廣播識別碼
//                if( mRepeatIv.isChecked()==true){
//                    intent.putExtra("repeatValue","RepeatOne"); //設定廣播夾帶參數
//                }else{
//                    intent.putExtra("repeatValue","N"); //設定廣播夾帶參數
//                }
//                sendBroadcast(intent);
                Log.d(TAG, "sendBroad____________repeat");

                break;

            case R.id.activity_main_shuffle:
//                Intent intent2 =  new Intent(Constants.SET_RANDOM); //設定廣播識別碼
//                if( mShuffleIv.isChecked()==true){
//                    intent2.putExtra("randomValue","Random"); //設定廣播夾帶參數
//                }else{
//                    intent2.putExtra("randomValue","N"); //設定廣播夾帶參數
//                }
//                sendBroadcast(intent2);
                Log.d(TAG, "sendBroad____________shuffle");

                break;

        }

    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if(Build.VERSION.SDK_INT>=23){
            //權限查取
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},123);
                return;
            }
        }

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mPlayer.seekTo(seekBar.getProgress());
        mHandler.sendEmptyMessage(UP_TIME);
    }

    @Override
    public void onMusicClick(int position, Song song) {
        Song oldSong = APPContext.getInstance().getSong();
        if (oldSong == null || !oldSong.path.equals(song.path)) {
            sendBroadcastSong(song);
        }
    }

    @Override
    public void onCollectClick(int position, Song song, boolean isCheck) {
        if (isCheck) {
            CollectDBHelper.getInstance(mContext).insertSong(song);
        } else {
            CollectDBHelper.getInstance(mContext).deleteSong(song);
        }
    }

    private void recordProgress() {
        cancelTimer();
        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(UP_TIME);
            }
        };
        mTimer.schedule(mTimerTask, 0, 1000);
    }

    private void cancelTimer() {
        if (mTimer != null) {
            mTimer.cancel();
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
        }
    }

    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UP_TIME:
                    Song song = APPContext.getInstance().getSong();
                    mSeekBar.setProgress(mPlayer.getCurrentPosition());
                    mTimeTvFirst.setText(MusicUtils.formatTime(mPlayer.getCurrentPosition()));
                    mTimeTv.setText(MusicUtils.formatTime(song.duration));
                    break;
            }

        }
    }

    private class MainReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.CHANGE_MUSIC_UP_MUSIC_DATA.equals(intent.getAction())) {
                Song song = APPContext.getInstance().getSong();
                setSongView(song);
                mStateCb.setChecked(true);
                mAdapter.notifyDataSetChanged();
                recordProgress();
            } else if (Constants.CHANGE_PLAYED.equals(intent.getAction())) {
                mStateCb.setChecked(false);
                nextSong();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Song song = APPContext.getInstance().getSong();
        setSongView(song);
        if (mPlayer.isPlaying()) {
            mStateCb.setChecked(true);
            mSeekBar.setMax(mPlayer.getDuration());
            mSeekBar.setProgress(mPlayer.getCurrentPosition());
            recordProgress();
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mMainReceiver);
        super.onDestroy();
    }


}
