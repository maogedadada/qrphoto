package com.zbar.lib;

/**
 * desc:
 * author：zy
 * date:2018/4/16
 * time:14:59
 */

public class ZbarManager {
    static {
        System.loadLibrary("zbar");
	}
    public native String decode(byte[] data, int width, int height, boolean isCrop, int x, int y, int cwidth, int cheight);
}
