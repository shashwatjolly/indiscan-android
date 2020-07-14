package com.rrss.documentscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TestHorizontalActivity extends Activity {

    private LinearLayout linearLayout;
    private CustomHorizontalScrollView horizontalScrollView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int width = this.getWindow().getDecorView().getWidth();
        int height = this.getWindow().getDecorView().getHeight();
        horizontalScrollView = new CustomHorizontalScrollView(this, 50,
                width);
        setContentView(R.layout.trylayout);
        linearLayout = (LinearLayout) findViewById(R.id.layer);
        linearLayout.addView(horizontalScrollView);

        LinearLayout container = new LinearLayout(this);
        container.setLayoutParams(new LinearLayout.LayoutParams(width, height));
        // container.setHeight(height);

        TextView textView = new TextView(this);
        textView.setWidth(width);
        textView.setHeight(height);
        textView.setGravity(Gravity.CENTER);
        textView.setText("First  Screen");
        textView.setBackgroundColor(Color.CYAN);
        container.addView(textView);

        textView = new TextView(this);
        textView.setWidth(width);
        textView.setHeight(height);
        textView.setGravity(Gravity.CENTER);
        textView.setText("Second  Screen");
        textView.setBackgroundColor(Color.GREEN);
        container.addView(textView);

        textView = new TextView(this);
        textView.setWidth(width);
        textView.setHeight(height);
        textView.setGravity(Gravity.CENTER);
        textView.setText("Third  Screen");
        textView.setBackgroundColor(Color.RED);
        container.addView(textView);

        horizontalScrollView.addView(container);
    }

}