package com.example.workitchat;


import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.support.v7.widget.Toolbar;

import com.squareup.picasso.Picasso;

/**
 * Simple Activity for showing an image in full size. Also, there's a zoom-in option.
 * IMPORTANT: the image uri should be sent to this activity as extra of the intent.
 *
 */
public class FullScreenImage extends AppCompatActivity {

    ImageView mImageView;
    private ScaleGestureDetector mScaleGestureDetector;
    private float mScaleFactor = 1.0f;
    private String mImage;
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        mProgress = new ProgressDialog(this);
        mProgress.setTitle("Uploading Image");
        mProgress.setMessage("Wait a minute...");
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();

        mImage = getIntent().getStringExtra("imageUri");

        mImageView = (ImageView) findViewById(R.id.full_screen_image);
        Picasso.get().load(mImage).placeholder(R.drawable.avatar).into(mImageView);

        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        mProgress.dismiss();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        return true;
    }


    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector){
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mScaleFactor = Math.max(0.1f,
                    Math.min(mScaleFactor, 10.0f));
            mImageView.setScaleX(mScaleFactor);
            mImageView.setScaleY(mScaleFactor);
            return true;
        }
    }
}
