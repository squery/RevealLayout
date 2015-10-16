package com.telephone.squery.reveallayout.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import java.util.ArrayList;

/**
 * Created by ZhangYan[Squery] on 2015/10/15.
 */

/**
 * 这个类主要是  完成5.0以上的那种点击控件的水波纹效果
 * 任何放入内部的clickable元素都具有波纹效果，当它被点击的时候，
 * 为了性能，尽量不要在内部放入复杂的元素
 * 我们写成专门的类 这样就可以不去重复的去重写 各种view了 只需要用这个布局就行
 * <p/>
 * 之所以采用了LinearLayout而不是RelativeLayout 是因为水波纹要频繁的调用onLayout
 * RelativeLayout内部重绘要做很多事，所以为了性能还是选LinearLayout
 */
public class RevealLayout extends LinearLayout implements Runnable {

    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int mTargetWidth;
    private int mTargetHeight;

    private int mMinBetweenWidthAndHeight;
    private int mMaxBetweenWidthAndHeight;

    private int mMaxRevealRadius;
    private int mRevealRadiusGap;
    private int mRevealRadius = 0;
    private float mCenterX;
    private float mCenterY;
    private int[] mLocationInScreen = new int[2];

    private boolean mShouldDoAnimation = false;
    private boolean mIsPressed = false;

    private int INVALIDATE_DURATION = 40;
    private View mTouchTarget;

    private DispatchUpTouchEventRunnable mDispatchUpTouchEventRunnable
            = new DispatchUpTouchEventRunnable();

    public RevealLayout(Context context) {
        super(context);
        init();
    }

    public RevealLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public RevealLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 得到我们点击的view
     *
     * @param view 正常的ViewLayout 注意是容器类组件
     * @param x    view的长度的范围
     * @param y    viewde宽度的范围
     * @return 返回的view
     */
    private View getTouchTarget(View view, int x, int y) {

        View target = null;
        ArrayList<View> touchables = view.getTouchables();
        for (View touchable : touchables) {
            if (isTouchPointInView(touchable, x, y)) {
                target = touchable;
                break;
            }
        }
        return target;
    }

    /**
     * 判断该view 是不是被点击了
     */
    private boolean isTouchPointInView(View view, int x, int y) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getMeasuredWidth();
        int bottom = top + view.getMeasuredHeight();
        if (view.isClickable() && x >= left && x <= right
                && y >= top && y <= bottom) {

            return true;
        }
        return false;
    }

    /**
     * 重写这个方法就是为了确定用户点击到了那个View上
     *
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        int rawX = (int) ev.getRawX();
        int rawY = (int) ev.getRawY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                View touchTarget = getTouchTarget(this, rawX, rawY);
                if (touchTarget != null && touchTarget.isClickable() && touchTarget.isEnabled()) {
                    mTouchTarget = touchTarget;
                    initParametersForChild(ev, mTouchTarget);
                    postInvalidateDelayed(INVALIDATE_DURATION);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mIsPressed = false;
                postInvalidateDelayed(INVALIDATE_DURATION);
                break;
            case MotionEvent.ACTION_UP:
                mIsPressed = false;
                postInvalidateDelayed(INVALIDATE_DURATION);
                mDispatchUpTouchEventRunnable.event = ev;
                postDelayed(mDispatchUpTouchEventRunnable, 400);
                return true;
        }

        // 注意虽然我们重写了这个方法 但是clcik功能是在父类中实现的所以要调用父类的方法
        return super.dispatchTouchEvent(ev);
    }

    private void initParametersForChild(MotionEvent event, View view) {
        mCenterX = event.getX();
        mCenterY = event.getY();
        mTargetWidth = view.getMeasuredWidth();
        mTargetHeight = view.getMeasuredHeight();
        mMinBetweenWidthAndHeight = Math.min(mTargetWidth, mTargetHeight);
        mMaxBetweenWidthAndHeight = Math.max(mTargetWidth, mTargetHeight);
        mRevealRadius = 0;
        mShouldDoAnimation = true;
        mIsPressed = true;
        mRevealRadiusGap = mMinBetweenWidthAndHeight / 8;

        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int left = location[0] - mLocationInScreen[0];
        int transformedCenterX = (int) mCenterX - left;
        mMaxRevealRadius = Math.max(transformedCenterX, mTargetWidth - transformedCenterX);
    }

    /**
     * 绘制水波纹
     *
     * @param canvas
     */
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (!mShouldDoAnimation || mTargetWidth <= 0 || mTouchTarget == null) {
            return;
        }

        if (mRevealRadius > mMinBetweenWidthAndHeight / 2) {
            mRevealRadius += mRevealRadiusGap * 4;
        } else {
            mRevealRadius += mRevealRadiusGap;
        }
        this.getLocationOnScreen(mLocationInScreen);
        int[] location = new int[2];
        mTouchTarget.getLocationOnScreen(location);
        int left = location[0] - mLocationInScreen[0];
        int top = location[1] - mLocationInScreen[1];
        int right = left + mTouchTarget.getMeasuredWidth();
        int bottom = top + mTouchTarget.getMeasuredHeight();

        canvas.save();
        canvas.clipRect(left, top, right, bottom);
        canvas.drawCircle(mCenterX, mCenterY, mRevealRadius, mPaint);
        canvas.restore();

        if (mRevealRadius <= mMaxRevealRadius) {
            postInvalidateDelayed(INVALIDATE_DURATION, left, top, right, bottom);
        } else if (!mIsPressed) {
            mShouldDoAnimation = false;
            postInvalidateDelayed(INVALIDATE_DURATION, left, top, right, bottom);
        }
    }


    @Override
    public void run() {
        super.performClick();
    }

    private class DispatchUpTouchEventRunnable implements Runnable {
        public MotionEvent event;

        @Override
        public void run() {

            if (mTouchTarget == null || !mTouchTarget.isEnabled()) {
                return;
            }
            if (isTouchPointInView(mTouchTarget, (int) event.getRawX(), (int) event.getRawY())) {

                mTouchTarget.performClick();
            }
        }
    }

    @Override
    public boolean performClick() {
        postDelayed(this, 400);
        return true;
    }

    private void init() {
        setWillNotDraw(false);
        mPaint.setColor(Color.parseColor("#1b000000"));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        this.getLocationOnScreen(mLocationInScreen);
    }
}
