package com.rrss.documentscanner;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.rrss.documentscanner.base.CropperErrorType;
import com.rrss.documentscanner.base.DocumentScanActivity;
import com.rrss.documentscanner.helpers.ScannerConstants;
import com.rrss.documentscanner.libraries.PolygonView;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ImageCropActivity extends DocumentScanActivity {

    private static ArrayList<FrameLayout> holderImageCrop = new ArrayList<FrameLayout>(0);
    private static ArrayList<ImageView> imageView = new ArrayList<ImageView>(0);
    private static ArrayList<PolygonView> polygonView = new ArrayList<PolygonView>(0);
    private static ArrayList<Bitmap> cropImage = new ArrayList<Bitmap>(0);
    protected static ArrayList<Integer> polygonViewId = new ArrayList<Integer>(0);
    private boolean isInverted;
    private ProgressBar progressBar;
    private int temp_id;
    private CustomHorizontalScrollView horizontalScrollView;
    private OnClickListener btnImageEnhanceClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(() -> {
                        cropImage = getCroppedImage();
                        if (cropImage == null)
                            return false;
//                        if (ScannerConstants.saveStorage)
//                            saveToInternalStorage(cropImage);
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                if (cropImage != null) {
                                    ScannerConstants.bitmaparrayfinal = cropImage;
                                    setResult(RESULT_OK);
                                    finish();
                                }
                            })
            );
        }
    };
//    private OnClickListener btnRebase = v -> {
//        cropImage = ScannerConstants.selectedImageBitmap.copy(ScannerConstants.selectedImageBitmap.getConfig(), true);
//        isInverted = false;
//        startCropping();
//    };
    private OnClickListener btnCloseClick = v -> finish();
//    private OnClickListener btnInvertColor = new OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            showProgressBar();
//            disposable.add(
//                    Observable.fromCallable(() -> {
//                        invertColor();
//                        return false;
//                    })
//                            .subscribeOn(Schedulers.io())
//                            .observeOn(AndroidSchedulers.mainThread())
//                            .subscribe((result) -> {
//                                hideProgressBar();
//                                Bitmap scaledBitmap = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
//                                imageView.setImageBitmap(scaledBitmap);
//                            })
//            );
//        }
//    };
//    private OnClickListener onRotateClick = new OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            showProgressBar();
//            disposable.add(
//                    Observable.fromCallable(() -> {
//                        if (isInverted)
//                            invertColor();
//                        cropImage = rotateBitmap(cropImage, 90);
//                        return false;
//                    })
//                            .subscribeOn(Schedulers.io())
//                            .observeOn(AndroidSchedulers.mainThread())
//                            .subscribe((result) -> {
//                                hideProgressBar();
//                                startCropping();
//                            })
//            );
//        }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);
        cropImage = ScannerConstants.bitmaparray;
        isInverted = false;
        if (ScannerConstants.bitmaparray != null)
            initView();
        else {
            Toast.makeText(this, ScannerConstants.imageError, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected ArrayList<FrameLayout> getHolderImageCrop() {
        return holderImageCrop;
    }

    @Override
    protected ArrayList<ImageView> getImageView() {
        return imageView;
    }

    @Override
    protected ArrayList<PolygonView> getPolygonView() {
        return polygonView;
    }

    @Override
    protected void showProgressBar() {
        RelativeLayout rlContainer = findViewById(R.id.rlContainer);
        setViewInteract(rlContainer, false);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void hideProgressBar() {
        RelativeLayout rlContainer = findViewById(R.id.rlContainer);
        setViewInteract(rlContainer, true);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void showError(CropperErrorType errorType) {
        switch (errorType) {
            case CROP_ERROR:
                Toast.makeText(this, ScannerConstants.cropError, Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    protected ArrayList<Bitmap> getBitmapImage() {
        return cropImage;
    }

    private void setViewInteract(View view, boolean canDo) {
        view.setEnabled(canDo);
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setViewInteract(((ViewGroup) view).getChildAt(i), canDo);
            }
        }
    }

    private void initView() {
        Button btnImageCrop = findViewById(R.id.btnImageCrop);
        Button btnClose = findViewById(R.id.btnClose);
//        ImageView ivRotate = findViewById(R.id.ivRotate);
//        ImageView ivInvert = findViewById(R.id.ivInvert);
//        ImageView ivRebase = findViewById(R.id.ivRebase);
        btnImageCrop.setText(ScannerConstants.cropText);
        btnClose.setText(ScannerConstants.backText);
        LinearLayout layout = (LinearLayout)findViewById(R.id.layoutProcessActivity);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        // adding customHorizontalScrollView
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        Log.e("hello", String.valueOf(width)+"aeff"+String.valueOf(width));
        horizontalScrollView = new CustomHorizontalScrollView(this, ScannerConstants.bitmaparray.size(), width);
        layout.addView(horizontalScrollView);

        //parameter for linear layout
        LinearLayout.LayoutParams topLinearLayoutParam = new LinearLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        topLinearLayoutParam.width = width;
        topLinearLayoutParam.height = 1200;

        //adding linear layout inside horizontal scroll above
        LinearLayout linearLayoutTop = new LinearLayout(this);
        linearLayoutTop.setLayoutParams(topLinearLayoutParam);
        int idx = LinearLayout.generateViewId();
        linearLayoutTop.setId(idx);
        horizontalScrollView.addView(linearLayoutTop);

        // top layer frame params
        FrameLayout.LayoutParams parentFrameParam = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        parentFrameParam.width = width;
        parentFrameParam.height = 1200;
        parentFrameParam.gravity = Gravity.CENTER;
//        parentFrameParam.setMargins(40,10,40,10);

        // second frame layer params
        FrameLayout.LayoutParams childFrameParam = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        childFrameParam.gravity = Gravity.CENTER;
        childFrameParam.setMargins(60,10,60,10);

        // imageview params
        ViewGroup.LayoutParams imageViewParam = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        // polygon view params
        PolygonView.LayoutParams polygonViewParams = new PolygonView.LayoutParams(
                PolygonView.LayoutParams.MATCH_PARENT,
                PolygonView.LayoutParams.MATCH_PARENT
        );
        polygonViewParams.setMargins(60,10,60,10);

        LinearLayout layoutProcessActivty2 = findViewById(idx);
        for(int i=0;i<ScannerConstants.bitmaparray.size();i++) {
            FrameLayout frameClickedImg = new FrameLayout(this);
            frameClickedImg.setLayoutParams(parentFrameParam);
            temp_id = FrameLayout.generateViewId();
            frameClickedImg.setId(temp_id);
            layoutProcessActivty2.addView(frameClickedImg);

            FrameLayout holderClickedImg = new FrameLayout(this);
            holderClickedImg.setLayoutParams(childFrameParam);
            temp_id = FrameLayout.generateViewId();
            holderClickedImg.setId(temp_id);
            frameClickedImg.addView(holderClickedImg);
            holderImageCrop.add(holderClickedImg);

            ImageView imgClickedView = new ImageView(this);
            imgClickedView.setLayoutParams(imageViewParam);
            holderClickedImg.addView(imgClickedView);
            imageView.add(imgClickedView);

            PolygonView cropHandles = new PolygonView(this);
            cropHandles.setLayoutParams(polygonViewParams);
            cropHandles.setVisibility(View.INVISIBLE);
            temp_id = PolygonView.generateViewId();
            cropHandles.setId(temp_id);
            polygonViewId.add(temp_id);
            frameClickedImg.addView(cropHandles);
            polygonView.add(cropHandles);
        }


        progressBar = findViewById(R.id.progressBar);
        if (progressBar.getIndeterminateDrawable() != null && ScannerConstants.progressColor != null)
            progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor(ScannerConstants.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);
        else if (progressBar.getProgressDrawable() != null && ScannerConstants.progressColor != null)
            progressBar.getProgressDrawable().setColorFilter(Color.parseColor(ScannerConstants.progressColor), android.graphics.PorterDuff.Mode.MULTIPLY);
        btnImageCrop.setBackgroundColor(Color.parseColor(ScannerConstants.cropColor));
        btnClose.setBackgroundColor(Color.parseColor(ScannerConstants.backColor));
        btnImageCrop.setOnClickListener(btnImageEnhanceClick);
        btnClose.setOnClickListener(btnCloseClick);
//        ivRotate.setOnClickListener(onRotateClick);
//        ivInvert.setOnClickListener(btnInvertColor);
//        ivRebase.setOnClickListener(btnRebase);
        startCropping();
    }

//    private void invertColor() {
//        if (!isInverted) {
//            Bitmap bmpMonochrome = Bitmap.createBitmap(cropImage.getWidth(), cropImage.getHeight(), Bitmap.Config.ARGB_8888);
//            Canvas canvas = new Canvas(bmpMonochrome);
//            ColorMatrix ma = new ColorMatrix();
//            ma.setSaturation(0);
//            Paint paint = new Paint();
//            paint.setColorFilter(new ColorMatrixColorFilter(ma));
//            canvas.drawBitmap(cropImage, 0, 0, paint);
//            cropImage = bmpMonochrome.copy(bmpMonochrome.getConfig(), true);
//        } else {
//            cropImage = cropImage.copy(cropImage.getConfig(), true);
//        }
//        isInverted = !isInverted;
//    }

    private String saveToInternalStorage(Bitmap bitmapImage) {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "cropped_" + timeStamp + ".png";
        File mypath = new File(directory, imageFileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }
}