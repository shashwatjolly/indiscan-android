/*
 * *
 *  * Created by Ali YÜCE on 3/2/20 11:18 PM
 *  * https://github.com/mayuce/
 *  * Copyright (c) 2020 . All rights reserved.
 *  * Last modified 3/2/20 11:10 PM
 *
 */

package com.rrss.documentscanner.helpers;

import android.graphics.Bitmap;

import java.util.ArrayList;

public class ScannerConstants {
    public static Bitmap selectedImageBitmap;
    public static ArrayList<Bitmap> bitmaparray = new ArrayList<Bitmap>(0);
    public static ArrayList<Bitmap> bitmaparrayfinal= new ArrayList<Bitmap>(0);

//    public static Bitmap selectedImageBitmap;

    public static String cropText="Crop",backText="Close",
            imageError="Image error.",
            cropError="Crop error";
    public static String cropColor="#6666ff",backColor="#ff0000",progressColor="#331199";
    public static boolean saveStorage=false;
}
