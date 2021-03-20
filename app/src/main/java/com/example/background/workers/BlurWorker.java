package com.example.background.workers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.background.R;

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

        try {
        // original picture
        Bitmap picture = BitmapFactory.decodeResource(
                applicationContext.getResources(),
                R.drawable.test
        );

        // blurred picture
        Bitmap output = WorkerUtils.blurBitmap(picture,applicationContext);
        Uri outputUri = WorkerUtils.writeBitmapToFile(applicationContext,output);
        WorkerUtils.makeStatusNotification("Output is "
                + outputUri.toString(), applicationContext);

            // note: remember to return the result!
            return Result.success();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "Error applying blur", e);
            return Result.failure();
        }
    }
}
