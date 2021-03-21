/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background;

import android.app.Application;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.background.workers.BlurWorker;
import com.example.background.workers.CleanupWorker;
import com.example.background.workers.SaveImageToFileWorker;

import java.util.List;

import static com.example.background.Constants.KEY_IMAGE_URI;

public class BlurViewModel extends AndroidViewModel {

    private Uri mImageUri;
    private WorkManager mWorkManager;

    //note: 使用livedata获取并储存被Tag标记过的workInfo
    private LiveData<List<WorkInfo>> mWorkInfo;

    private Uri mFinalOutputUri;

    public BlurViewModel(@NonNull Application application) {
        super(application);
        mWorkManager = WorkManager.getInstance(application);
        mWorkInfo = mWorkManager.getWorkInfosByTagLiveData(Constants.TAG_OUTPUT);
    }

    /**
     * 向View层传递workInfo
     *
     * @return mWorkInfo 存储图片work的workInfo
     */
    public LiveData<List<WorkInfo>> getWorkInfo(){
        return mWorkInfo;
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     *
     * @param blurLevel The amount to blur the image
     */
    void applyBlur(int blurLevel) {
//        mWorkManager.enqueue(OneTimeWorkRequest.from(BlurWorker.class));

        //note: 写法2
//        mWorkManager.enqueue(new OneTimeWorkRequest.Builder(BlurWorker.class)
//                .setInputData(createInputDataForUri())
//                .build());

        //note: add constraints
        Constraints constraints = new Constraints.Builder()
                .setRequiresCharging(true)
                .build();

        //note: 连锁work
        WorkContinuation continuation = mWorkManager.beginUniqueWork(Constants.IMAGE_MANIPULATION_WORK_NAME
                , ExistingWorkPolicy.REPLACE //以新work替代旧work
                , new OneTimeWorkRequest.Builder(CleanupWorker.class).build()
        );

        for (int i = 0; i < blurLevel; i++) {
            //note: 记得赋值啊
            OneTimeWorkRequest.Builder blurBuilder = new OneTimeWorkRequest.Builder(BlurWorker.class);

            if (i == 0) {
                blurBuilder.setInputData(createInputDataForUri());
            }

            continuation = continuation.then(blurBuilder.build());
        }


        continuation = continuation.then(new OneTimeWorkRequest.Builder(SaveImageToFileWorker.class)
                .setConstraints(constraints)
                .addTag(Constants.TAG_OUTPUT)
                .build());
        continuation.enqueue();
    }

    private Uri uriOrNull(String uriString) {
        if (!TextUtils.isEmpty(uriString)) {
            return Uri.parse(uriString);
        }
        return null;
    }

    /**
     * Setters
     */
    void setImageUri(String uri) {
        mImageUri = uriOrNull(uri);
    }

    /**
     * Getters
     */
    Uri getImageUri() {
        return mImageUri;
    }


    private Data createInputDataForUri() {
        Data.Builder builder = new Data.Builder();
        if (mImageUri != null) {
            builder.putString(KEY_IMAGE_URI, mImageUri.toString());
        }

        return builder.build();
    }

    public Uri getFinalOutputUri() {
        return mFinalOutputUri;
    }

    public void setFinalOutputUri(String OutputUri) {
        this.mFinalOutputUri = uriOrNull(OutputUri);
    }

    public void cancelWork(){
        mWorkManager.cancelUniqueWork(Constants.IMAGE_MANIPULATION_WORK_NAME);
    }

}