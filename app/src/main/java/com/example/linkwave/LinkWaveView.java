package com.example.linkwave;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 水波纹扩散的view
 * <p>
 * Created by yangzhizhong
 */
public class LinkWaveView extends View {
    private Paint paint;
    private int mLinkColor = Color.RED;//环颜色
    private int maxDiameter = 128;//波纹圆环波动范围
    private int mAlpha = 128;//初始透明度
    private boolean isStarting = false;//运行状态
    private int mCentreViewWidth = 180;//中心图片直径
    private int diaMeter = 0;//初始圆直径
    private int delay;//刷新间隔
    private int mLinkCount;//圆环数目
    private List<LinkCircle> startWidthList = new ArrayList<>();//圆环列表容器

    private Handler handler = new Handler();

    public LinkWaveView(Context context) {
        this(context, null);
    }

    public LinkWaveView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinkWaveView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context, attrs, defStyle);
        initData();
    }

    /**
     * 获取xml参数
     *
     * @param context  context
     * @param attrs    attrs
     * @param defStyle defStyle
     */
    private void initView(Context context, AttributeSet attrs, int defStyle) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.LinkWaveView,
                defStyle, 0);
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.LinkWaveView_mWaveSpeed) {
                int speed = a.getInteger(attr, 40);
                handleDelay(speed);
            } else if (attr == R.styleable.LinkWaveView_mLinkColor) {
                mLinkColor = a.getColor(attr, Color.WHITE);
            } else if (attr == R.styleable.LinkWaveView_mLinkCount) {
                setLinkCount(a.getInteger(attr, 5));
            } else if (attr == R.styleable.LinkWaveView_mIsStarting) {
                isStarting = a.getBoolean(attr, false);
            } else if (attr == R.styleable.LinkWaveView_mCentreViewWidth) {
                mCentreViewWidth = a.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 90, getResources().getDisplayMetrics()));
            }
        }

        a.recycle();
    }

    private void initData() {
        maxDiameter = maxDiameter + mCentreViewWidth / 2;
        diaMeter = diaMeter + mCentreViewWidth / 2;
        paint = new Paint();
        paint.setColor(mLinkColor);// 此处颜色可以改为自己喜欢的
        paint.setAntiAlias(true);//消除锯齿
        paint.setStrokeWidth(6f);
        initList();
        start();
    }

    /**
     * 初始化圆环和透明度
     */
    private void initList() {
        LinkCircle linkCircle = new LinkCircle();
        linkCircle.setDiameter(diaMeter);
        linkCircle.setAlpha(mAlpha);
        startWidthList.add(linkCircle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            height = getPaddingTop() + getPaddingBottom() + maxDiameter * 2;
        }

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else {
            width = getPaddingLeft() + getPaddingRight() + maxDiameter * 2;
        }
        setMeasuredDimension(width, height);
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        setBackgroundColor(Color.TRANSPARENT);// 颜色：完全透明
        // 依次绘制 同心圆
        for (int i = 0; i < startWidthList.size(); i++) {
            int alpha = startWidthList.get(i).getAlpha();
            int startWidth = startWidthList.get(i).getDiameter();
            paint.setAlpha(alpha);// 设置透明度
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, startWidth, paint);
            // 同心圆扩散
            if (isStarting && alpha > 0 && startWidth < maxDiameter) {
                LinkCircle linkCircle = new LinkCircle();
                linkCircle.setDiameter(startWidth + 1);
                linkCircle.setAlpha(alpha - 1);
                startWidthList.set(i, linkCircle);
            }
        }
        if (isStarting && startWidthList.get(startWidthList.size() - 1).getDiameter() == (maxDiameter / mLinkCount + diaMeter)) {
            LinkCircle linkCircle = new LinkCircle();
            linkCircle.setDiameter(diaMeter);
            linkCircle.setAlpha(mAlpha);
            startWidthList.add(linkCircle);
        }
        // 同心圆数量达到限制，删除最外层圆
        if (isStarting && startWidthList.size() == mLinkCount) {
            startWidthList.remove(0);
        }
    }


    /** 开始 */
    public void start() {
        isStarting = true;
        doInvalidate();
    }

    /**
     * 控制刷新
     * 采用单线程池管理，避免OOM
     */
    private void doInvalidate() {
        if (!isStarting) {
            return;
        }
        postInvalidate();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doInvalidate();
            }
        }, delay);
    }

    /** 暂停 */
    public void stop() {
        isStarting = false;
    }

    /**
     * 获取运行状态
     *
     * @return isStarting 是否运行
     */
    public boolean isStarting() {
        return isStarting;
    }

    /**
     * 设置扩散速度(0~100)
     *
     * @param speed 速度
     */
    public void handleDelay(int speed) {
        if (speed > 99) {
            speed = 99;
        } else if (speed < 1) {
            speed = 1;
        }
        delay = 100 - speed;
    }

    /**
     * 获取扩散速度
     *
     * @return speed 扩散速度
     */
    public int getWaveSpeed() {
        return 100 - delay;
    }

    /**
     * 获取当前圆环数目（1 ~ 10）
     *
     * @return mLinkCount
     */
    public int getLinkCount() {
        return mLinkCount;
    }

    /**
     * 设置圆环数目
     *
     * @param count 圆环数目
     */
    public void setLinkCount(int count) {
        mLinkCount = count;
        if (mLinkCount > 10) {
            mLinkCount = 10;
        } else if (mLinkCount < 1) {
            mLinkCount = 1;
        }
    }

    /**
     * 获取圆环颜色
     *
     * @return mLinkColor
     */
    public int getLinkColor() {
        return mLinkColor;
    }

    /**
     * 设置圆环颜色
     *
     * @param color 圆环颜色
     */
    public void setLinkColor(int color) {
        mLinkColor = color;
    }

    /**
     * 获取中心视图尺寸
     *
     * @return mCentreViewWidth
     */
    public int getCentreViewWidth() {
        return mCentreViewWidth;
    }

    /**
     * 设置中心视图尺寸
     *
     * @param width 尺寸
     */
    public void setCentreViewWidth(int width) {
        mCentreViewWidth = width;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isStarting = false;
    }

    private class LinkCircle {
        private int diameter;
        private int alpha;

        int getDiameter() {
            return diameter;
        }

        void setDiameter(int diameter) {
            this.diameter = diameter;
        }

        public int getAlpha() {
            return alpha;
        }

        public void setAlpha(int alpha) {
            this.alpha = alpha;
        }
    }
}
