package com.example.editor.imageeditlibrary.editimage.view;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.editor.R;
import com.example.editor.imageeditlibrary.editimage.utils.RectUtil;


/**
 * @author panyi
 */
public class AddImageItem {
    private static final float MIN_SCALE = 0.15f;
    private static final int HELP_BOX_PAD = 25;

    private static final int BUTTON_WIDTH = Constants.STICKER_BTN_HALF_SIZE;

    public Bitmap bitmap;
    public Rect srcRect;// 原始图片坐标
    public RectF dstRect;// 绘制目标坐标
    private Rect helpToolsRect;
    public RectF deleteRect;// 删除按钮位置
    public RectF rotateRect;// 旋转按钮位置
    public RectF scaleRect;// 旋转按钮位置

    public RectF helpBox;
    public Matrix matrix;// 变化矩阵
    public float rotateAngle = 0;
    boolean isDrawHelpTool = false;
    private final Paint helpBoxPaint = new Paint();


    private float initWidth;// 加入屏幕时原始宽度

    private static Bitmap deleteBit;
    private static Bitmap rotateBit;
    private static Bitmap scaleBit;

    public RectF detectDeleteRect;
    public RectF detectRotateRect;
    public RectF detectScaleRect;


    public AddImageItem(Context context) {
        helpBoxPaint.setColor(ContextCompat.getColor(context, R.color.rect_frame));
        helpBoxPaint.setStyle(Style.STROKE);
        helpBoxPaint.setAntiAlias(true);
        helpBoxPaint.setStrokeWidth(4);

        // 导入工具按钮位图
        if (deleteBit == null) {
            deleteBit = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_delete);

        }

         if (rotateBit == null) {
             rotateBit = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_rotate);
        }

        if (scaleBit == null) {
            scaleBit = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_scale);
        }// end if
    }

    public void init(Bitmap addBit, View parentView) {
        this.bitmap = addBit;
        this.srcRect = new Rect(0, 0, addBit.getWidth(), addBit.getHeight());
        int bitWidth = Math.min(addBit.getWidth(), parentView.getWidth() >> 1);
        int bitHeight = (int) bitWidth * addBit.getHeight() / addBit.getWidth();
        int left = (parentView.getWidth() >> 1) - (bitWidth >> 1);
        int top = (parentView.getHeight() >> 1) - (bitHeight >> 1);
        this.dstRect = new RectF(left, top, left + bitWidth, top + bitHeight);

        this.matrix = new Matrix();
        this.matrix.postTranslate(this.dstRect.left, this.dstRect.top);
        this.matrix.postScale((float) bitWidth / addBit.getWidth(),
                (float) bitHeight / addBit.getHeight(), this.dstRect.left,
                this.dstRect.top);
        initWidth = this.dstRect.width();// 记录原始宽度
        // item.matrix.setScale((float)bitWidth/addBit.getWidth(),
        // (float)bitHeight/addBit.getHeight());
        this.isDrawHelpTool = true;
        this.helpBox = new RectF(this.dstRect);
        updateHelpBoxRect();

        helpToolsRect = new Rect(0, 0, deleteBit.getWidth(), deleteBit.getHeight());

        deleteRect = new RectF(helpBox.left - BUTTON_WIDTH, helpBox.top
                - BUTTON_WIDTH, helpBox.left + BUTTON_WIDTH, helpBox.top
                + BUTTON_WIDTH);

        rotateRect = new RectF(helpBox.left - BUTTON_WIDTH, helpBox.bottom
                - BUTTON_WIDTH, helpBox.left + BUTTON_WIDTH, helpBox.bottom
                + BUTTON_WIDTH);

        scaleRect = new RectF(helpBox.right - BUTTON_WIDTH, helpBox.bottom
                - BUTTON_WIDTH, helpBox.right + BUTTON_WIDTH, helpBox.bottom
                + BUTTON_WIDTH);


        detectDeleteRect = new RectF(deleteRect);
        detectRotateRect = new RectF(rotateRect);
        detectScaleRect = new RectF(scaleRect);
    }

    private void updateHelpBoxRect() {
        this.helpBox.left -= HELP_BOX_PAD;
        this.helpBox.right += HELP_BOX_PAD;
        this.helpBox.top -= HELP_BOX_PAD;
        this.helpBox.bottom += HELP_BOX_PAD;
    }

    /**
     * 位置更新
     *
     * @param dx
     * @param dy
     */
    public void updatePos(final float dx, final float dy) {
        this.matrix.postTranslate(dx, dy);// 记录到矩阵中

        dstRect.offset(dx, dy);

        // 工具按钮随之移动
        helpBox.offset(dx, dy);
        deleteRect.offset(dx, dy);
        rotateRect.offset(dx,dy);
        scaleRect.offset(dx, dy);

        this.detectDeleteRect.offset(dx, dy);
        this.detectRotateRect.offset(dx, dy);
        this.detectScaleRect.offset(dx, dy);
    }

    public void updateRotate(final float oldx, final float oldy,
                                     final float dx, final float dy) {
        float c_x = dstRect.centerX();
        float c_y = dstRect.centerY();

        float x = this.detectScaleRect.centerX();
        float y = this.detectScaleRect.centerY();

        // float x = oldx;
        // float y = oldy;

        float n_x = x + dx;
        float n_y = y + dy;

        float xa = x - c_x;
        float ya = y - c_y;

        float xb = n_x - c_x;
        float yb = n_y - c_y;

        float srcLen = (float) Math.sqrt(xa * xa + ya * ya);
        float curLen = (float) Math.sqrt(xb * xb + yb * yb);

        // 重新计算工具箱坐标
        helpBox.set(dstRect);
        updateHelpBoxRect();// 重新计算

        deleteRect.offsetTo(helpBox.left - BUTTON_WIDTH, helpBox.top
                - BUTTON_WIDTH);

        rotateRect.offsetTo(helpBox.left - BUTTON_WIDTH,helpBox.bottom
                - BUTTON_WIDTH);


        scaleRect.offsetTo(helpBox.right - BUTTON_WIDTH, helpBox.bottom
                - BUTTON_WIDTH);

        detectDeleteRect.offsetTo(helpBox.left - BUTTON_WIDTH, helpBox.top
                - BUTTON_WIDTH);

        detectRotateRect.offsetTo(helpBox.left - BUTTON_WIDTH, helpBox.bottom
                - BUTTON_WIDTH);

        detectScaleRect.offsetTo(helpBox.right - BUTTON_WIDTH, helpBox.bottom
                - BUTTON_WIDTH);

        double cos = (xa * xb + ya * yb) / (srcLen * curLen);
        if (cos > 1 || cos < -1)
            return;
        float angle = (float) Math.toDegrees(Math.acos(cos));
        // System.out.println("angle--->" + angle);

        // 定理
        float calMatrix = xa * yb - xb * ya;// 行列式计算 确定转动方向

        int flag = calMatrix > 0 ? 1 : -1;
        angle = flag * angle;

        // System.out.println("angle--->" + angle);
        rotateAngle += angle;
        this.matrix.postRotate(angle, this.dstRect.centerX(),
                this.dstRect.centerY());

        RectUtil.rotateRect(this.detectDeleteRect, this.dstRect.centerX(),
                this.dstRect.centerY(), rotateAngle);

        RectUtil.rotateRect(this.detectRotateRect, this.dstRect.centerX(),
                this.dstRect.centerY(), rotateAngle);


        Log.d("旋转","======>"+detectRotateRect);

        RectUtil.rotateRect(this.detectScaleRect, this.dstRect.centerX(),
                this.dstRect.centerY(), rotateAngle);
    }


    public void updateScale(final float oldx, final float oldy,
                             final float dx, final float dy) {
        float c_x = dstRect.centerX();
        float c_y = dstRect.centerY();

        float x = this.detectScaleRect.centerX();
        float y = this.detectScaleRect.centerY();

        // float x = oldx;
        // float y = oldy;

        float n_x = x + dx;
        float n_y = y + dy;

        float xa = x - c_x;
        float ya = y - c_y;

        float xb = n_x - c_x;
        float yb = n_y - c_y;

        float srcLen = (float) Math.sqrt(xa * xa + ya * ya);
        float curLen = (float) Math.sqrt(xb * xb + yb * yb);

        // System.out.println("srcLen--->" + srcLen + "   curLen---->" +
        // curLen);

        float scale = curLen / srcLen;// 计算缩放比

        float newWidth = dstRect.width() * scale;
        if (newWidth / initWidth < MIN_SCALE) {// 最小缩放值检测
            return;
        }

        this.matrix.postScale(scale, scale, this.dstRect.centerX(),
                this.dstRect.centerY());// 存入scale矩阵
        // this.matrix.postRotate(5, this.dstRect.centerX(),
        // this.dstRect.centerY());
        RectUtil.scaleRect(this.dstRect, scale);// 缩放目标矩形

        // 重新计算工具箱坐标
        helpBox.set(dstRect);
        updateHelpBoxRect();// 重新计算

        deleteRect.offsetTo(helpBox.left - BUTTON_WIDTH, helpBox.top
                - BUTTON_WIDTH);

        rotateRect.offsetTo(helpBox.left - BUTTON_WIDTH,helpBox.bottom
                - BUTTON_WIDTH);


        scaleRect.offsetTo(helpBox.right - BUTTON_WIDTH, helpBox.bottom
                - BUTTON_WIDTH);

        detectDeleteRect.offsetTo(helpBox.left - BUTTON_WIDTH, helpBox.top
                - BUTTON_WIDTH);

        detectRotateRect.offsetTo(helpBox.left - BUTTON_WIDTH, helpBox.bottom
                - BUTTON_WIDTH);

        detectScaleRect.offsetTo(helpBox.right - BUTTON_WIDTH, helpBox.bottom
                - BUTTON_WIDTH);

        RectUtil.rotateRect(this.detectDeleteRect, this.dstRect.centerX(),
                this.dstRect.centerY(), rotateAngle);

        RectUtil.rotateRect(this.detectRotateRect, this.dstRect.centerX(),
                this.dstRect.centerY(), rotateAngle);


        Log.d("旋转","======>"+detectRotateRect);

        RectUtil.rotateRect(this.detectScaleRect, this.dstRect.centerX(),
                this.dstRect.centerY(), rotateAngle);
    }




    public void draw(Canvas canvas) {
        canvas.drawBitmap(this.bitmap, this.matrix, null);// 贴图元素绘制
        if (this.isDrawHelpTool) {// 绘制辅助工具线
            canvas.save();
            canvas.rotate(rotateAngle, helpBox.centerX(), helpBox.centerY());
            canvas.drawRect(helpBox,  helpBoxPaint);
            // 绘制工具按钮
            canvas.drawBitmap(deleteBit, helpToolsRect, deleteRect, null);

            Log.d("drawBitmap","s=s==s=s=s==s=s=s=s==deleteRect:"+deleteRect +" rotateRect:"+ rotateRect+" scaleRect:"+scaleRect);
            canvas.drawBitmap(rotateBit, helpToolsRect, rotateRect, null);

            canvas.drawBitmap(scaleBit, helpToolsRect, scaleRect, null);
            canvas.restore();
            // canvas.drawRect(deleteRect, dstPaint);
             //canvas.drawRect(rotateRect, dstPaint);

            //debug
//             canvas.drawRect(detectRotateRect, debugPaint);
//             canvas.drawRect(detectDeleteRect, debugPaint);
//             canvas.drawRect(helpBox , debugPaint);
        }// end if

        // detectRotateRect
    }
}// end class
