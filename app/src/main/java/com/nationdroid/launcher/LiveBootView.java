package com.nationdroid.launcher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Foreground Home welcome only; never replaces Android's system boot screen. */
public final class LiveBootView extends FrameLayout {
    private final ImageView logo;
    private ValueAnimator motion;
    public LiveBootView(Context context,String greeting){
        super(context);setBackgroundColor(0xff020b1c);setClickable(true);setFocusable(true);
        setContentDescription("NationDroid live welcome. Created by ope-opa-nation. Tap to continue.");
        LinearLayout stack=new LinearLayout(context);stack.setOrientation(LinearLayout.VERTICAL);stack.setGravity(Gravity.CENTER);stack.setPadding(dp(20),dp(16),dp(20),dp(16));
        addView(stack,new FrameLayout.LayoutParams(-1,-1));
        logo=new ImageView(context);logo.setImageResource(R.drawable.nationdroid_orb);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        stack.addView(logo,new LinearLayout.LayoutParams(-1,0,1));
        TextView credit=label("Created by ope-opa-nation",17,0xff73dfff);credit.setTypeface(null,Typeface.BOLD);stack.addView(credit);
        TextView message=label(greeting,16,0xffe9f3ff);message.setPadding(0,dp(16),0,dp(12));stack.addView(message);
        stack.addView(label("Tap anywhere to continue",12,0xff9caec7));
    }
    private TextView label(String value,int size,int color){TextView v=new TextView(getContext());v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER);return v;}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();
        if(android.os.Build.VERSION.SDK_INT>=26&&!ValueAnimator.areAnimatorsEnabled())return;
        motion=ValueAnimator.ofFloat(0f,1f);motion.setDuration(2400);motion.setRepeatCount(ValueAnimator.INFINITE);motion.setRepeatMode(ValueAnimator.REVERSE);motion.setInterpolator(new AccelerateDecelerateInterpolator());
        motion.addUpdateListener(a->{float t=(float)a.getAnimatedValue();logo.setScaleX(.95f+.05f*t);logo.setScaleY(.95f+.05f*t);logo.setTranslationY(dp(-6)*t);logo.setAlpha(.83f+.17f*t);});motion.start();
    }
    @Override protected void onDetachedFromWindow(){if(motion!=null){motion.cancel();motion.removeAllUpdateListeners();motion=null;}super.onDetachedFromWindow();}
}
