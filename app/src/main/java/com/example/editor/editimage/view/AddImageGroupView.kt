package com.example.editor.editimage.view;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.editor.R;
import com.example.editor.XLogger;
import com.example.editor.editimage.utils.RectUtil;

import java.util.LinkedHashMap;



public class AddImageGroupView extends View {
    private static final int STATUS_IDLE = 0;
    private static int STATUS_MOVE = 1;// 移动状态
    private static int STATUS_DELETE = 2;// 删除状态
    private static int STATUS_SCALE = 3;// 图片旋转状态
    private static int STATUS_ROTATE = 4;// 图片缩放模式

    private int imageCount;// 已加入照片的数量
    private int currentStatus;// 当前状态
    private AddImageItem currentItem;// 当前操作的贴图数据
    private float oldx, oldy;

    private RectF shaderRec = new RectF();
    private final Paint shaderPaint = new Paint();

    private LinkedHashMap<Integer, AddImageItem> mImageQueue = new LinkedHashMap<>();// 存贮每层贴图数据

    private Point mPoint = new Point(0 , 0);

    public AddImageGroupView(Context context) {
        super(context);
        init(context);
    }

    public AddImageGroupView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AddImageGroupView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        currentStatus = STATUS_IDLE;
    }



    Path screenPath = new Path();
    Path cropPath = new Path();
    Path combinedPath = new Path();
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

    public void addBitImage(final Bitmap addBit) {
        XLogger.d(" addBitImage getWidth:"+ addBit.getWidth()+ " getHeight:"+ addBit.getHeight()+ "currentItem:"+currentItem);
        AddImageItem addImageItem = new AddImageItem(this.getContext());
        addImageItem.init(addBit, this);
        if (currentItem != null) {
            currentItem.isDrawHelpTool = false;
        }
        imageCount = imageCount +1;
        mImageQueue.put(imageCount, addImageItem);
        invalidate();// 重绘视图
    }


    /**
     * 绘制客户页面
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        // System.out.println("on draw!!~");
        for (Integer id : mImageQueue.keySet()) {
            AddImageItem item = mImageQueue.get(id);
            if (item!=null)
                item.draw(canvas);
        }

        combinedPath.op(screenPath, cropPath, Path.Op.DIFFERENCE);
        //绘制遮蔽层
        canvas.drawPath(combinedPath,shaderPaint);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // System.out.println(w + "   " + h + "    " + oldw + "   " + oldh);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean ret = super.onTouchEvent(event);// 是否向下传递事件标志 true为消耗

        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                int deleteId = -1;
                for (Integer id : mImageQueue.keySet()) {
                    AddImageItem item = mImageQueue.get(id);
                    if (item==null) break;
                    if (item.detectDeleteRect.contains(x, y)) {// 删除模式
                        // ret = true;
                        deleteId = id;
                        currentStatus = STATUS_DELETE;
                    }else if (item.detectRotateRect.contains(x, y)){
                        ret = true;
                        if (currentItem != null) {
                            currentItem.isDrawHelpTool = false;
                        }
                        currentItem = item;
                        currentItem.isDrawHelpTool = true;
                        currentStatus = STATUS_ROTATE;
                        oldx = x;
                        oldy = y;
                    } else if (item.detectScaleRect.contains(x, y)) {// 点击了旋转按钮
                        ret = true;
                        if (currentItem != null) {
                            currentItem.isDrawHelpTool = false;
                        }
                        currentItem = item;
                        currentItem.isDrawHelpTool = true;
                        currentStatus = STATUS_SCALE;
                        oldx = x;
                        oldy = y;
                    } else if (detectInItemContent(item , x , y)) {// 移动模式
                        // 被选中一张贴图
                        ret = true;
                        if (currentItem != null) {
                            currentItem.isDrawHelpTool = false;
                        }
                        currentItem = item;
                        currentItem.isDrawHelpTool = true;
                        currentStatus = STATUS_MOVE;
                        oldx = x;
                        oldy = y;
                    }
                    // end if
                }// end for each

                if (!ret && currentItem != null && currentStatus == STATUS_IDLE) {// 没有贴图被选择
                    currentItem.isDrawHelpTool = false;
                    currentItem = null;
                    invalidate();
                }

                if (deleteId > 0 && currentStatus == STATUS_DELETE) {// 删除选定贴图
                    mImageQueue.remove(deleteId);
                    currentStatus = STATUS_IDLE;// 返回空闲状态
                    invalidate();
                }// end if

                break;
            case MotionEvent.ACTION_MOVE:
                ret = true;
                if (currentStatus == STATUS_MOVE) {// 移动贴图
                    float dx = x - oldx;
                    float dy = y - oldy;
                    if (currentItem != null) {
                        currentItem.updatePos(dx, dy);
                        invalidate();
                    }// end if
                    oldx = x;
                    oldy = y;
                } else if (currentStatus == STATUS_SCALE) {// 旋转 缩放图片操作
                    // System.out.println("旋转");
                    float dx = x - oldx;
                    float dy = y - oldy;
                    if (currentItem != null) {
                        currentItem.updateScale(oldx, oldy, dx, dy);// 旋转
                        invalidate();
                    }// end if
                    oldx = x;
                    oldy = y;
                }else if (currentStatus == STATUS_ROTATE){
                    float dx = x - oldx;
                    float dy = y - oldy;
                    if (currentItem != null) {
                        currentItem.updateRotate(oldx, oldy, dx, dy);// 旋转
                        invalidate();
                    }// end if
                    oldx = x;
                    oldy = y;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                ret = false;
                currentStatus = STATUS_IDLE;
                break;
        }// end switch
        return ret;
    }

    /**
     * 判定点击点是否在内容范围之内  需考虑旋转
     * @param item
     * @param x
     * @param y
     * @return
     */
    private boolean detectInItemContent(AddImageItem item , float x , float y){
        //reset
        mPoint.set((int)x , (int)y);
        //旋转点击点
        RectUtil.rotatePoint(mPoint , item.helpBox.centerX() , item.helpBox.centerY() , -item.rotateAngle);
        return item.helpBox.contains(mPoint.x, mPoint.y);
    }

    public LinkedHashMap<Integer, AddImageItem> getImageQueue() {
        return mImageQueue;
    }

    public void clear() {
        mImageQueue.clear();
        this.invalidate();
    }
}
