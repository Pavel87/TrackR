package com.pacmac.trackr;

import android.graphics.Bitmap;

/**
 * Created by pacmac on 2017-08-14.
 */

public class ImageItem {
    private Bitmap image;
    private int id;

    public ImageItem(Bitmap image, int id) {
        this.image = image;
        this.id = id;
    }

    public Bitmap getImage() {
        return image;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}