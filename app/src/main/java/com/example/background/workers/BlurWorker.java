package com.example.background.workers;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.background.Constants;

import java.io.FileNotFoundException;

public class BlurWorker extends Worker {

    public BlurWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private static final String TAG = BlurWorker.class.getSimpleName();

    @NonNull
    @Override
    public Result doWork() {

        Context applicationContext = getApplicationContext();
        WorkerUtils.makeStatusNotification("Blurring Image", applicationContext);
        WorkerUtils.sleep();

        // note: 获取setInputData中存入的data
        String resourceUri = getInputData().getString(Constants.KEY_IMAGE_URI);

        try {
            // original picture
//        Bitmap picture = BitmapFactory.decodeResource(
//                applicationContext.getResources(),
//                R.drawable.test
//        );

            // 用传入的uri替换默认图片
            // check empty
            if (TextUtils.isEmpty(resourceUri)) {
                Log.e(TAG, "Invalid inout uri");
                throw new IllegalArgumentException("Invalid inout uri");
            }

            ContentResolver resolver = applicationContext.getContentResolver();
            Bitmap picture = BitmapFactory.decodeStream(
                    resolver.openInputStream(Uri.parse(resourceUri))
            );

            // blur picture
            Bitmap output = WorkerUtils.blurBitmap(picture, applicationContext);


            Uri outputUri = WorkerUtils.writeBitmapToFile(applicationContext, output);
            WorkerUtils.makeStatusNotification("Output is "
                    + outputUri.toString(), applicationContext);

            Data outputData = new Data.Builder()
                    .putString(Constants.KEY_IMAGE_URI, outputUri.toString())
                    .build();

            // note: remember to return the result!
            return Result.success(outputData);

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Error applying blur", e);
            return Result.failure();
        }
    }
}
