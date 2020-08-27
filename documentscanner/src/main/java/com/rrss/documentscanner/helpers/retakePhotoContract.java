package com.rrss.documentscanner.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rrss.documentscanner.ImageRetakeActivity;

public class retakePhotoContract extends ActivityResultContract<Integer,Bitmap>{

    @NonNull
    @Override
    public Intent createIntent(@NonNull Context context, Integer input) {
        Intent intent = new Intent(context, ImageRetakeActivity.class);
        intent.putExtra("itemId", input);
        return intent;
    }

    @Override
    public Bitmap parseResult(int resultCode, @Nullable Intent intent) {
        if(resultCode  != Activity.RESULT_OK)return null;
        else {
            byte[] byteArray = intent.getByteArrayExtra("imageBitmap");
            Bitmap imgBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            return imgBitmap;
        }
    }
}
