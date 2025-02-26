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
import android.graphics.PointF;
import android.widget.FrameLayout;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ScannerConstants {
    public static Bitmap selectedImageBitmap;
    public static ArrayList<FrameLayout> frameLayoutArray = new ArrayList<FrameLayout>(0);
    public static ArrayList<Bitmap> bitmaparray = new ArrayList<Bitmap>(0);
    public static ArrayList<Bitmap> bitmaparrayfinal= new ArrayList<Bitmap>(0);
    public static ArrayList< Map<Integer, PointF> > pointfArray = new ArrayList< Map<Integer, PointF> >(0);
    public static ArrayList< Map<Integer, PointF> > pointfArray0 = new ArrayList< Map<Integer, PointF> >(0);
    public static ArrayList< Map<Integer, PointF> > pointfArray90 = new ArrayList< Map<Integer, PointF> >(0);
    public static ArrayList< Map<Integer, PointF> > pointfArray180 = new ArrayList< Map<Integer, PointF> >(0);
    public static ArrayList< Map<Integer, PointF> > pointfArray270 = new ArrayList< Map<Integer, PointF> >(0);
    //    public static Bitmap selectedImageBitmap;
    public static AtomicReference<Integer> maxImgIdProcessed= new AtomicReference<Integer>(0);
    public static ArrayList<Bitmap> tempBitMapArray = new ArrayList<Bitmap>(0);
    public static Integer totalImageClicked = 0;
    public static String cropText="Crop",backText="Close",
            imageError="Image error.",
            cropError="Crop error";
    public static String cropColor="#6666ff",backColor="#ff0000",progressColor="#331199";
    public static boolean saveStorage=false;


    // layout parameters for showing Image for processing
    public static Integer height = 1440;
    public static Integer width = 1080;
    public static Integer paddingLeft = 60;
    public static Integer paddingTop = 10;

    //Image context
    public static Integer activeImageId = 0;
    public static Boolean isRotate = false;
    public static int containerId;
    public static ArrayList<Float> imageRatios = new ArrayList<Float>(0);

}
