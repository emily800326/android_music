package com.music.player.musicUtils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.music.player.bean.Song;

import static android.content.ContentValues.TAG;

/**
 * Music tool:Created by WJ on 2018/8/14.
 */
public class MusicUtils {
    /**
     * Search system music->back music list
     */

    public static List<Song> getMusicData(Context context) {
        List<Song> list = new ArrayList<>();
        // 從媒體櫃取得音樂檔案-->（MusicUtils）
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null,
                null, MediaStore.Audio.AudioColumns.IS_MUSIC);
        int songNum=0;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                songNum++;

                //取得SONG資訊
                Song Song = new Song();
                Song.song_Num=songNum;
                Song.song = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
                Song.singer = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                Song.path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                Song.duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                Song.size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));
                if (Song.size > 1000 * 800) {
                    // 以-切割取歌名歌手
                    if (Song.song.contains("-")) {
                        String[] str = Song.song.split("-");
                        Song.singer = str[0];
                        Song.song = str[1];
                    }
                    list.add(Song);
                }
//                Log.d("getsong", String.valueOf(Song.song));
            }
            // 釋放資源
            cursor.close();
        }

        return list;
    }

    /**
     *  get time format function
     */
    public static String formatTime(long time) {
        //format 倒數時間
        if (time / 1000 % 60 < 10) {
            return time / 1000 / 60 + ":0" + time / 1000 % 60;
        } else {
            return time / 1000 / 60 + ":" + time / 1000 % 60;
        }
    }
}