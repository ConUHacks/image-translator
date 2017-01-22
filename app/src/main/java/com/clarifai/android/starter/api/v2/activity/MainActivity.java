package com.clarifai.android.starter.api.v2.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.clarifai.android.starter.api.v2.R;

public final class MainActivity extends BaseActivity {

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override protected void onStart() {
        super.onStart();
    }

    protected void btnListener(View v){
        Intent intent = new Intent(this, RecognizeConceptsActivity.class);
        startActivity(intent);
    }

    @Override protected int layoutRes() { return R.layout.start_menu; }
}
