package com.gear2cam.official;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.util.Base64;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.gear2cam.official.R;
import com.gear2cam.official.services.CameraProviderService;
import com.parse.ParseFacebookUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by varun on 16/5/14.
 */
public class FacebookHelper {

    public interface FacebookHelperCallback
    {
        public void onFacebookSaveImage(boolean success);
    };

    public static String getWatchPreview(String filePath) {
        try {
            InputStream in = new FileInputStream(filePath);
            // decode image size (decode metadata only, not the whole image)
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, options);
            in.close();
            in = null;

            // save width and height
            int inWidth = options.outWidth;
            int inHeight = options.outHeight;

            int dstWidth;
            int dstHeight;

            if(inWidth >= inHeight) {
                dstWidth = 320;
                dstHeight = (int) ((float) 320 * (float) inHeight / (float) inWidth);
            }
            else {
                dstWidth = 200;
                dstHeight = (int) ((float) 200 * (float) inHeight / (float) inWidth);
            }

            // decode full image pre-resized
            in = new FileInputStream(filePath);
            options = new BitmapFactory.Options();
            // calc rought re-size (this is no exact resize)
            options.inSampleSize = Math.max(inWidth/dstWidth, inHeight/dstHeight);
            // decode full image
            Bitmap roughBitmap = BitmapFactory.decodeStream(in, null, options);

            int rotate = 0;

            String porientation = "LANDSCAPE";

            File imageFile = new File(filePath);
            ExifInterface exif = new ExifInterface(
                    imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    porientation = "PORTRAIT";
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    porientation = "LANDSCAPE";
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    porientation = "PORTRAIT";
                    break;
            }

            Matrix matrix = new Matrix();
            matrix.postRotate(rotate);
            roughBitmap = Bitmap.createBitmap(roughBitmap , 0, 0, roughBitmap.getWidth(), roughBitmap.getHeight(), matrix, true);


            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            roughBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
            byte[] byteArray = stream.toByteArray();

            stream.close();
            roughBitmap.recycle();

            return porientation + "," + Base64.encodeToString(byteArray, Base64.NO_WRAP);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }


    private static String createResizedImage(String filePath) {
        if(!filePath.endsWith(".jpg")) {
            return null;
        }
        String thumbnailPath = filePath.substring(0, filePath.length() - 4);
        thumbnailPath += "-thumb.jpg";

        int inWidth = 0;
        int inHeight = 0;

        InputStream in;
        try {
            in = new FileInputStream(filePath);

            // decode image size (decode metadata only, not the whole image)
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, options);
            in.close();
            in = null;

            // save width and height
            inWidth = options.outWidth;
            inHeight = options.outHeight;

            int dstWidth;
            int dstHeight;

            if(inWidth >= inHeight) {
                dstWidth = 640;
                dstHeight = (int) ((float) 640 * (float) inHeight / (float) inWidth);
            }
            else {
                dstWidth = 480;
                dstHeight = (int) ((float) 480 * (float) inHeight / (float) inWidth);
            }

            // decode full image pre-resized
            in = new FileInputStream(filePath);
            options = new BitmapFactory.Options();
            // calc rought re-size (this is no exact resize)
            options.inSampleSize = Math.max(inWidth/dstWidth, inHeight/dstHeight);
            // decode full image
            Bitmap roughBitmap = BitmapFactory.decodeStream(in, null, options);

            int rotate = 0;
            File imageFile = new File(filePath);
            ExifInterface exif = new ExifInterface(
                    imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }

            Matrix matrix = new Matrix();
            matrix.postRotate(rotate);
            roughBitmap = Bitmap.createBitmap(roughBitmap , 0, 0, roughBitmap.getWidth(), roughBitmap.getHeight(), matrix, true);

            // save image
            FileOutputStream out = new FileOutputStream(thumbnailPath);
            roughBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            out.close();
            roughBitmap.recycle();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            thumbnailPath = null;
        } catch (IOException e) {
            e.printStackTrace();
            thumbnailPath = null;
        }
        return thumbnailPath;
    }

    public static void publishImageToFacebook (Context context, String filePath,final FacebookHelperCallback facebookHelperCallback)
    {
        String thumbnailPath = createResizedImage(filePath);
        if(thumbnailPath == null) {
            if(facebookHelperCallback!=null)
                facebookHelperCallback.onFacebookSaveImage(false);
        }

        final File imageFile = new File(thumbnailPath);
        Session session = ParseFacebookUtils.getSession();

        try {
            if(CameraProviderService.getCurrentConnection() != null) {
                CameraProviderService.getCurrentConnection().incrementUploadCount();
            }

            Request request =  Request.newUploadPhotoRequest(session,imageFile,new Request.Callback() {
                        @Override
                        public void onCompleted(Response response) {
                            if (response.getError() == null) {
                                if(facebookHelperCallback!=null) {
                                    if(CameraProviderService.getCurrentConnection() != null) {
                                        CameraProviderService.getCurrentConnection().decrementUploadCount();
                                        facebookHelperCallback.onFacebookSaveImage(true);
                                    }
                                }
                            } else
                            {
                                if(facebookHelperCallback!=null) {
                                    if(CameraProviderService.getCurrentConnection() != null) {
                                        CameraProviderService.getCurrentConnection().decrementUploadCount();
                                        facebookHelperCallback.onFacebookSaveImage(false);
                                    }
                                }
                            }
                            imageFile.delete();
                        }
                    }
            );
            Bundle parameters = request.getParameters(); // <-- THIS IS IMPORTANT
            parameters.putString("message", context.getString(R.string.facebook_post_description));

            request.setParameters(parameters);
            request.executeAsync();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
