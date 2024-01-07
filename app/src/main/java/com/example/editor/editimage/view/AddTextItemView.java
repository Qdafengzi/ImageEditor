package com.example.editor.editimage.view;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.editor.R;
import com.example.editor.editimage.utils.ListUtil;
import com.example.editor.editimage.utils.RectUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class AddTextItemView extends View {
    public static final float TEXT_SIZE_DEFAULT = 80;
    public static final int PADDING = 32;
    //public static final int PADDING = 0;

    public static final int TEXT_TOP_PADDING = 10;

    //public static final int CHAR_MIN_HEIGHT = 60;


    //private String mText;
    private TextPaint mPaint = new TextPaint();
    private Paint debugPaint = new Paint();
    private Paint mHelpPaint = new Paint();

    private Rect mTextRect = new Rect();// warp text rect record
    private RectF mHelpBoxRect = new RectF();
    private Rect mDeleteRect = new Rect();//删除按钮位置
    private Rect mRotateRect = new Rect();//旋转按钮位置
    private Rect mScaleRect = new Rect();//旋转按钮位置

    private RectF mDeleteDstRect = new RectF();
    private RectF mRotateDstRect = new RectF();
    private RectF mScaleDstRect = new RectF();

    private Bitmap mDeleteBitmap;
    private Bitmap mRotateBitmap;
    private Bitmap mScaleBitmap;

    private int mCurrentMode = IDLE_MODE;
    //控件的几种模式
    private static final int IDLE_MODE = 2;//正常
    private static final int MOVE_MODE = 3;//移动模式
    private static final int SCALE_MODE = 4;//旋转模式
    private static final int DELETE_MODE = 5;//删除模式
    private static final int ROTATE_MODE = 6;//旋转模式

//    private EditText mEditText;//输入控件

    public int layout_x = 0;
    public int layout_y = 0;

    private float last_x = 0;
    private float last_y = 0;

    public float mRotateAngle = 0;
    public float mScale = 1;
    private boolean isInitLayout = true;

    private boolean isShowHelpBox = true;
    private final Paint shaderPaint = new Paint();


    private boolean mAutoNewLine = false;//是否需要自动换行
    private List<String> mTextContents = new ArrayList<String>(2);//存放所写的文字内容
    private String mText;

    private Point mPoint = new Point(0, 0);

    public AddTextItemView(Context context) {
        super(context);
        initView(context);
    }

    public AddTextItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public AddTextItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public void setEditText(EditText textView) {
//        this.mEditText = textView;
    }

    private void initView(Context context) {
        shaderPaint.setColor(ContextCompat.getColor(this.getContext(), R.color.shader));
        debugPaint.setColor(Color.parseColor("#66ff0000"));

        mDeleteBitmap = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_delete);

        mRotateBitmap = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_rotate);

        mScaleBitmap = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_scale);

        mDeleteRect.set(0, 0, mDeleteBitmap.getWidth(), mDeleteBitmap.getHeight());
        mRotateRect.set(0, 0, mRotateBitmap.getWidth(), mRotateBitmap.getHeight());
        mScaleRect.set(0, 0, mScaleBitmap.getWidth(), mScaleBitmap.getHeight());

        mDeleteDstRect = new RectF(0, 0, Constants.IMAGE_BTN_HALF_SIZE << 1, Constants.IMAGE_BTN_HALF_SIZE << 1);
        mRotateDstRect = new RectF(0, 0, Constants.IMAGE_BTN_HALF_SIZE << 1, Constants.IMAGE_BTN_HALF_SIZE << 1);
        mScaleDstRect = new RectF(0, 0, Constants.IMAGE_BTN_HALF_SIZE << 1, Constants.IMAGE_BTN_HALF_SIZE << 1);

        mPaint.setColor(Color.WHITE);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setTextSize(TEXT_SIZE_DEFAULT);
        mPaint.setAntiAlias(true);
        mPaint.setTextAlign(Paint.Align.LEFT);

        mHelpPaint.setColor(ContextCompat.getColor(context, R.color.rect_frame));
        mHelpPaint.setStyle(Paint.Style.STROKE);
        mHelpPaint.setAntiAlias(true);
        mHelpPaint.setStrokeWidth(4);
    }




    Path screenPath = new Path();
    Path cropPath = new Path();
    Path combinedPath = new Path();
    private RectF shaderRec = new RectF();

    public void setRootImageRect(RectF bitmapRect, int width, int height){
        shaderPaint.setColor(ContextCompat.getColor(this.getContext(), R.color.white));
        shaderRec = bitmapRect;
        screenPath.addRect(new RectF(0f, 0f,width, height), Path.Direction.CCW);
        RectF cropRect = new RectF(shaderRec.left,
                shaderRec.top,
                shaderRec.right,
                shaderRec.bottom);
        cropPath.addRect(cropRect, Path.Direction.CW);
    }


    TextView example = new TextView(getContext());
    public void setText(String text) {
        this.mText = text;
        example.setText(text);
        example.setTextSize(TEXT_SIZE_DEFAULT);

        invalidate();
    }

    public void setTextColor(int newColor) {
        mPaint.setColor(newColor);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (isInitLayout) {
            isInitLayout = false;
            resetView();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (TextUtils.isEmpty(mText))
            return;

        parseText();
        drawContent(canvas);


        //绘制遮蔽层
        combinedPath.op(screenPath, cropPath, Path.Op.DIFFERENCE);
        canvas.drawPath(combinedPath,shaderPaint);
    }

    protected void parseText() {
        if (TextUtils.isEmpty(mText))
            return;
        mTextContents.clear();
        String[] splits = mText.split("\n");
        //end for each
        mTextContents.addAll(Arrays.asList(splits));
    }

    private void drawContent(Canvas canvas) {
        drawText(canvas);
        //draw x and rotate button
        int offsetValue = ((int) mDeleteDstRect.width()) >> 1;

        mDeleteDstRect.offsetTo(mHelpBoxRect.left - offsetValue, mHelpBoxRect.top - offsetValue);
        mRotateDstRect.offsetTo(mHelpBoxRect.left - offsetValue, mHelpBoxRect.bottom - offsetValue);
        mScaleDstRect.offsetTo(mHelpBoxRect.right - offsetValue, mHelpBoxRect.bottom - offsetValue);

        RectUtil.rotateRect(mDeleteDstRect, mHelpBoxRect.centerX(),
                mHelpBoxRect.centerY(), mRotateAngle);

        RectUtil.rotateRect(mRotateDstRect, mHelpBoxRect.centerX(),
                mHelpBoxRect.centerY(), mRotateAngle);
        RectUtil.rotateRect(mScaleDstRect, mHelpBoxRect.centerX(),
                mHelpBoxRect.centerY(), mRotateAngle);

        if (!isShowHelpBox) {
            return;
        }

        canvas.save();
        canvas.rotate(mRotateAngle, mHelpBoxRect.centerX(), mHelpBoxRect.centerY());
        canvas.drawRect(mHelpBoxRect, mHelpPaint);
        canvas.restore();

        canvas.drawBitmap(mDeleteBitmap, mDeleteRect, mDeleteDstRect, null);
        canvas.drawBitmap(mRotateBitmap, mRotateRect, mRotateDstRect, null);
        canvas.drawBitmap(mScaleBitmap, mScaleRect, mScaleDstRect, null);

        //debug
//        canvas.drawRect(mRotateDstRect, debugPaint);
//        canvas.drawRect(mDeleteDstRect, debugPaint);
//        canvas.drawRect(mHelpBoxRect, debugPaint);
    }

    private void drawText(Canvas canvas) {
        drawText(canvas, layout_x, layout_y, mScale, mRotateAngle);
    }

    public Rect getTextBounds(Paint paint, String text) {
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return bounds;
    }

    public void drawText(Canvas canvas, int _x, int _y, float scale, float rotate) {
        if (ListUtil.isEmpty(mTextContents))
            return;

        int x = _x;
        int y = _y;

        int textHeight = 0;
        //clear
        mTextRect.setEmpty();
        Rect tempRect = new Rect();
        Paint.FontMetricsInt fontMetrics = mPaint.getFontMetricsInt();
        int charMinHeight = Math.abs(fontMetrics.top) + Math.abs(fontMetrics.bottom);//字体高度
//        int charMinHeight =Math.abs(fontMetrics.bottom-fontMetrics.top);//字体高度
        Rect textBounds = getTextBounds(mPaint, mText);
        int width= textBounds.width();
        int height= textBounds.height();






        textHeight = charMinHeight;

        int totalHeight = 0;
        int maxWidth = 0;
        //System.out.println("top = "+fontMetrics.top +"   bottom = "+fontMetrics.bottom);
        for (int i = 0; i < mTextContents.size(); i++) {
            String text = mTextContents.get(i);
            mPaint.getTextBounds(text, 0, text.length(), tempRect);
            //System.out.println(i + " ---> " + tempRect.height());
            //text_height = Math.max(charMinHeight, tempRect.height());
            if (tempRect.height() <= 0) {//处理此行文字为空的情况
                tempRect.set(0, 0, 0, textHeight);
            }

            totalHeight+= textHeight;
            if (tempRect.width()>maxWidth){
                maxWidth = tempRect.width();
            }

            RectUtil.rectAddV(mTextRect, tempRect, 0, charMinHeight);
        }

        x -= maxWidth / 2;

        y = y -  totalHeight / 2;

        mTextRect.offset(x, y);

//        mHelpBoxRect.set(
//                mTextRect.left - PADDING,
//                mTextRect.top - PADDING,
//                mTextRect.right + PADDING,
//                mTextRect.bottom + PADDING
//        );

        mHelpBoxRect.set(
                mTextRect.left - PADDING,
                mTextRect.top - PADDING,
                mTextRect.right + PADDING,
                mTextRect.bottom
        );


        RectUtil.scaleRect(mHelpBoxRect, scale);

        canvas.save();
        canvas.scale(scale, scale, mHelpBoxRect.centerX(), mHelpBoxRect.centerY());
        canvas.rotate(rotate, mHelpBoxRect.centerX(), mHelpBoxRect.centerY());



        //canvas.drawRect(mTextRect, debugPaint);
        //float left = mHelpBoxRect.left - mTextRect.left;
        //float right = mHelpBoxRect.right - mTextRect.right;

        //System.out.println("left = "+left +"   right = "+right);
//        int draw_text_y = y + (textHeight >> 1) + PADDING;
        int draw_text_y = y + (textHeight >> 1);
        for (int i = 0; i < mTextContents.size(); i++) {
            canvas.drawText(mTextContents.get(i), x, draw_text_y, mPaint);
            draw_text_y += textHeight;
        }

//        canvas.drawCircle(canvas.getWidth(),canvas.getHeight(),20f,mPaint);
//        canvas.drawLine(canvas.getWidth()/2,0,canvas.getWidth()/2,canvas.getHeight(),mPaint);
//        canvas.drawLine(0,canvas.getHeight()/2,canvas.getWidth(),canvas.getHeight()/2,mPaint);

        canvas.restore();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean ret = super.onTouchEvent(event);// 是否向下传递事件标志 true为消耗

        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mDeleteDstRect.contains(x, y)) {// 删除模式
                    isShowHelpBox = true;
                    mCurrentMode = DELETE_MODE;
                } else if (mScaleDstRect.contains(x, y)) {// 旋转按钮
                    isShowHelpBox = true;
                    mCurrentMode = SCALE_MODE;
                    last_x = mScaleDstRect.centerX();
                    last_y = mScaleDstRect.centerY();
                    ret = true;
                } else if (mRotateDstRect.contains(x, y)) {// 旋转按钮
                    isShowHelpBox = true;
                    mCurrentMode = ROTATE_MODE;
                    last_x = mRotateDstRect.centerX();
                    last_y = mRotateDstRect.centerY();
                    ret = true;
                } else if (detectInHelpBox(x, y)) {// 移动模式
                    isShowHelpBox = true;
                    mCurrentMode = MOVE_MODE;
                    last_x = x;
                    last_y = y;
                    ret = true;
                } else {
                    isShowHelpBox = false;
                    invalidate();
                }// end if

                if (mCurrentMode == DELETE_MODE) {
                    // 删除选定贴图
                    mCurrentMode = IDLE_MODE;
                    // 返回空闲状态
                    clearTextContent();
                    invalidate();
                }// end if
                break;
            case MotionEvent.ACTION_MOVE:
                ret = true;
                if (mCurrentMode == MOVE_MODE) {
                    // 移动贴图
                    float dx = x - last_x;
                    float dy = y - last_y;
                    layout_x += dx;
                    layout_y += dy;
                    invalidate();
                    last_x = x;
                    last_y = y;
                } else if (mCurrentMode == SCALE_MODE) {
                    // 旋转 缩放文字操作
                    float dx = x - last_x;
                    float dy = y - last_y;
                    updateScale(dx, dy);
                    invalidate();
                    last_x = x;
                    last_y = y;
                } else if (mCurrentMode == ROTATE_MODE) {
                    // 旋转 缩放文字操作
                    float dx = x - last_x;
                    float dy = y - last_y;
                    updateRotate(dx, dy);
                    invalidate();
                    last_x = x;
                    last_y = y;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                ret = false;
                mCurrentMode = IDLE_MODE;
                break;
        }

        return ret;
    }

    /**
     * 考虑旋转情况下 点击点是否在内容矩形内
     *
     * @param x
     * @param y
     * @return
     */
    private boolean detectInHelpBox(float x, float y) {
        //mRotateAngle
        mPoint.set((int) x, (int) y);
        //旋转点击点
        RectUtil.rotatePoint(mPoint, mHelpBoxRect.centerX(), mHelpBoxRect.centerY(), - mRotateAngle);
        return mHelpBoxRect.contains(mPoint.x, mPoint.y);
    }

    public void clearTextContent() {
//        if (mEditText != null) {
//            mEditText.setText(null);
//        }
        //setText(null);
        setText("");
    }


    /**
     * 旋转 缩放 更新
     *
     * @param dx
     * @param dy
     */
    public void updateRotateAndScale(final float dx, final float dy) {
        float c_x = mHelpBoxRect.centerX();
        float c_y = mHelpBoxRect.centerY();

        float x = mScaleDstRect.centerX();
        float y = mScaleDstRect.centerY();

        float n_x = x + dx;
        float n_y = y + dy;

        float xa = x - c_x;
        float ya = y - c_y;

        float xb = n_x - c_x;
        float yb = n_y - c_y;

        float srcLen = (float) Math.sqrt(xa * xa + ya * ya);
        float curLen = (float) Math.sqrt(xb * xb + yb * yb);

        float scale = curLen / srcLen;// 计算缩放比

        mScale *= scale;
        float newWidth = mHelpBoxRect.width() * mScale;

        if (newWidth < 70) {
            mScale /= scale;
            return;
        }

        double cos = (xa * xb + ya * yb) / (srcLen * curLen);
        if (cos > 1 || cos < -1)
            return;
        float angle = (float) Math.toDegrees(Math.acos(cos));
        float calMatrix = xa * yb - xb * ya;// 行列式计算 确定转动方向

        int flag = calMatrix > 0 ? 1 : -1;
        angle = flag * angle;

        mRotateAngle += angle;
    }

    public void updateScale(final float dx, final float dy) {
        float c_x = mHelpBoxRect.centerX();
        float c_y = mHelpBoxRect.centerY();

        float x = mScaleDstRect.centerX();
        float y = mScaleDstRect.centerY();

        float n_x = x + dx;
        float n_y = y + dy;

        float xa = x - c_x;
        float ya = y - c_y;

        float xb = n_x - c_x;
        float yb = n_y - c_y;

        float srcLen = (float) Math.sqrt(xa * xa + ya * ya);
        float curLen = (float) Math.sqrt(xb * xb + yb * yb);

        float scale = curLen / srcLen;// 计算缩放比

        mScale *= scale;
        float newWidth = mHelpBoxRect.width() * mScale;

        if (newWidth < 70) {
            mScale /= scale;
        }
    }


    public void updateRotate(final float dx, final float dy) {
        float c_x = mHelpBoxRect.centerX();
        float c_y = mHelpBoxRect.centerY();

        float x = mScaleDstRect.centerX();
        float y = mScaleDstRect.centerY();

        float n_x = x + dx;
        float n_y = y + dy;

        float xa = x - c_x;
        float ya = y - c_y;

        float xb = n_x - c_x;
        float yb = n_y - c_y;

        float srcLen = (float) Math.sqrt(xa * xa + ya * ya);
        float curLen = (float) Math.sqrt(xb * xb + yb * yb);

        double cos = (xa * xb + ya * yb) / (srcLen * curLen);
        if (cos > 1 || cos < -1)
            return;
        float angle = (float) Math.toDegrees(Math.acos(cos));
        float calMatrix = xa * yb - xb * ya;// 行列式计算 确定转动方向

        int flag = calMatrix > 0 ? 1 : -1;
        angle = flag * angle;

        mRotateAngle += angle;
    }

    public void resetView() {
        layout_x = getMeasuredWidth() / 2;

        layout_y = getMeasuredWidth() / 2;
        mRotateAngle = 0;
        mScale = 1;
        mTextContents.clear();
    }

    public float getScale() {
        return mScale;
    }

    public float getRotateAngle() {
        return mRotateAngle;
    }

    public boolean isAutoNewLine() {
        return mAutoNewLine;
    }

    public void setAutoNewline(boolean isAuto) {
        if (mAutoNewLine != isAuto) {
            mAutoNewLine = isAuto;
            postInvalidate();
        }
    }


}//end class