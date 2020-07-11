package com.rrss.documentscanner;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
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

    private ArrayList<FrameLayout> holderImageCrop = new ArrayList<FrameLayout>(0);
    private ArrayList<ImageView> imageView = new ArrayList<ImageView>(0);
    private ArrayList<PolygonView> polygonView = new ArrayList<PolygonView>(0);
    private boolean isInverted;
    private ProgressBar progressBar;
    private ArrayList<Bitmap> cropImage = new ArrayList<Bitmap>(0);
    private int temp_id;
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
        Log.e("hewllo", "stage1");
        Log.e("hewllo", "stage1");
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
        Log.e("hello", "hereiam");
        Button btnImageCrop = findViewById(R.id.btnImageCrop);
        Button btnClose = findViewById(R.id.btnClose);
//        holderImageCrop = findViewById(R.id.holderImageCrop);
//        imageView = findViewById(R.id.imageView);
//        ImageView ivRotate = findViewById(R.id.ivRotate);
//        ImageView ivInvert = findViewById(R.id.ivInvert);
//        ImageView ivRebase = findViewById(R.id.ivRebase);
        btnImageCrop.setText(ScannerConstants.cropText);
        btnClose.setText(ScannerConstants.backText);
//        polygonView = findViewById(R.id.polygonView);
        LinearLayout layout = (LinearLayout)findViewById(R.id.showscrollimg);

        // top layer frame params
        FrameLayout.LayoutParams frparams1 = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        frparams1.width =950;
        frparams1.height = 950;
        frparams1.gravity = Gravity.CENTER;
        frparams1.setMargins(40,10,40,10);

        // second frame layer params
        FrameLayout.LayoutParams frparams2 = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

//        frparams2.width =950;
//        frparams2.height = 900;
        frparams2.gravity = Gravity.CENTER;
        frparams2.setMargins(40,10,40,10);

        // imageview params
        ViewGroup.LayoutParams imparam = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
//        imparam.width = 950;
//        imparam.height = 900;
        // polygon view params
        PolygonView.LayoutParams polyparams = new PolygonView.LayoutParams(
                PolygonView.LayoutParams.MATCH_PARENT,
                PolygonView.LayoutParams.MATCH_PARENT
        );

        LinearLayout ln = findViewById(R.id.showscrollimg);
        for(int i=0;i<ScannerConstants.bitmaparray.size();i++)
        {
            FrameLayout frame = new FrameLayout(this);
            frame.setLayoutParams(frparams1);
            temp_id = FrameLayout.generateViewId();
            frame.setId(temp_id);
            ln.addView(frame);
            Log.e("hello", "hereiamnow");
            FrameLayout holderImg = new FrameLayout(this);
            holderImg.setLayoutParams(frparams2);
            temp_id = FrameLayout.generateViewId();
            holderImg.setId(temp_id);
            frame.addView(holderImg);
            holderImageCrop.add(holderImg);
            Log.e("hello", "hereisdfsdam");

            ImageView imgv = new ImageView(this);
            imgv.setLayoutParams(imparam);
            holderImg.addView(imgv);
            imageView.add(imgv);

            PolygonView pv = new PolygonView(this);
            pv.setLayoutParams(polyparams);
            pv.setVisibility(View.INVISIBLE);
            frame.addView(pv);
            polygonView.add(pv);
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
//import androidx.appcompat.app.AppCompatActivity;
//
//import android.os.Bundle;
//
//public class ImageCropActivity extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_image_crop);
//    }
//}