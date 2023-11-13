package com.example.mycamerax;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


public class ImageUtils {
    public static byte[] generateNV21Data(Image $this$generateNV21Data) throws IllegalStateException, NullPointerException {
        Rect crop = $this$generateNV21Data.getCropRect();
        int format = $this$generateNV21Data.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = $this$generateNV21Data.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        Image.Plane var10000 = planes[0];
        byte[] rowData = new byte[var10000.getRowStride()];
        int channelOffset = 0;
        int outputStride = 1;
        int i = 0;

        for(int var11 = planes.length; i < var11; ++i) {
            switch(i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height + 1;
                    outputStride = 2;
                    break;
                case 2:
                    channelOffset = width * height;
                    outputStride = 2;
            }

            var10000 = planes[i];
            ByteBuffer buffer = var10000.getBuffer();
            var10000 = planes[i];
            int rowStride = var10000.getRowStride();
            var10000 = planes[i];
            int pixelStride = var10000.getPixelStride();
            int shift = i == 0 ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            int row = 0;

            for(int var19 = h; row < var19; ++row) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, w);
                    channelOffset += w;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    int col = 0;

                    for(int var22 = w; col < var22; ++col) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }

                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }

        return data;
    }

    public static byte[] convertYUV420ToNV21_ALL_PLANES(Image imgYUV420) {

        byte[] rez = null;

        try {
            ByteBuffer buffer0 = imgYUV420.getPlanes()[0].getBuffer();
            ByteBuffer buffer1 = imgYUV420.getPlanes()[1].getBuffer();
            ByteBuffer buffer2 = imgYUV420.getPlanes()[2].getBuffer();

            // actually here should be something like each second byte
            // however I simply get the last byte of buffer 2 and the entire buffer 1
            int buffer0_size = buffer0.remaining();
            int buffer1_size = buffer1.remaining(); // / 2 + 1;
            int buffer2_size = 1;//buffer2.remaining(); // / 2 + 1;

            byte[] buffer0_byte = new byte[buffer0_size];
            byte[] buffer1_byte = new byte[buffer1_size];
            byte[] buffer2_byte = new byte[buffer2_size];

            buffer0.get(buffer0_byte, 0, buffer0_size);
            buffer1.get(buffer1_byte, 0, buffer1_size);
            buffer2.get(buffer2_byte, buffer2_size-1, buffer2_size);


            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                // swap 1 and 2 as blue and red colors are swapped
                outputStream.write(buffer0_byte);
                outputStream.write(buffer2_byte);
                outputStream.write(buffer1_byte);
            } catch (IOException e) {
                e.printStackTrace();
            }

            rez = outputStream.toByteArray();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        return rez;
    }
}

