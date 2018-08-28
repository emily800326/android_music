package com.music.player.bean;

import java.io.Serializable;

/**
 * Created by WJ on 2018/8/10. * 放置音樂
 */
public class Song implements Serializable{
    /**
     * num
     */
    public int song_Num;
    /**
     * 歌手
     */
    public String singer;
    /**
     * 歌曲名
     */
    public String song;
    /**
     * 歌曲的地址
     */
    public String path;
    /**
     * 歌曲長度
     */
    public int duration;
    /**
     * 歌曲的大小
     */
    public long size;

}