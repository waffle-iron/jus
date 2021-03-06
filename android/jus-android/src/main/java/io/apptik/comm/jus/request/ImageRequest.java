/*
 * Copyright (C) 2015 Apptik Project
 * Copyright (C) 2014 Kalin Maldzhanski
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apptik.comm.jus.request;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.widget.ImageView.ScaleType;

import io.apptik.comm.jus.DefaultRetryPolicy;
import io.apptik.comm.jus.JusLog;
import io.apptik.comm.jus.NetworkResponse;
import io.apptik.comm.jus.ParseError;
import io.apptik.comm.jus.Request;
import io.apptik.comm.jus.Response;
import io.apptik.comm.jus.toolbox.HttpHeaderParser;
import io.apptik.comm.jus.util.BitmapPool;

/**
 * A canned request for getting an image at a given URL and calling
 * back with a decoded Bitmap.
 */
public class ImageRequest extends Request<Bitmap> {

    private static final boolean PREFER_QUALITY_OVER_SPEED = false;

    /**
     * Socket timeout in milliseconds for image requests
     */
    private static final int IMAGE_TIMEOUT_MS = 1000;

    /**
     * Default number of retries for image requests
     */
    private static final int IMAGE_MAX_RETRIES = 3;

    /**
     * Default backoff multiplier for image requests
     */
    private static final float IMAGE_BACKOFF_MULT = 2f;

    private final Config decodeConfig;
    private final int maxWidth;
    private final int maxHeight;
    private ScaleType scaleType;
    private BitmapPool bitmapPool;

    /**
     * Decoding lock so that we don't decode more than one image at a time (to avoid OOM's)
     */
    private static final Object DECODE_LOCK = new Object();

    /**
     * Creates a new image request, decoding to a maximum specified width and
     * height. If both width and height are zero, the image will be decoded to
     * its natural size. If one of the two is nonzero, that dimension will be
     * clamped and the other one will be set to preserve the image's aspect
     * ratio. If both width and height are nonzero, the image will be decoded to
     * be fit in the rectangle of dimensions width x height while keeping its
     * aspect ratio.
     *
     * @param url          URL of the image
     * @param maxWidth     Maximum width to decode this bitmap to, or zero for none
     * @param maxHeight    Maximum height to decode this bitmap to, or zero for
     *                     none
     * @param scaleType    The ImageViews ScaleType used to calculate the needed image size.
     * @param decodeConfig Format to decode the bitmap to
     */
    public ImageRequest(String url, int maxWidth, int maxHeight,
                        ScaleType scaleType, Config decodeConfig) {
        super(Method.GET, url);
        setRetryPolicy(
                new DefaultRetryPolicy(IMAGE_TIMEOUT_MS, IMAGE_MAX_RETRIES, IMAGE_BACKOFF_MULT));
        this.decodeConfig = decodeConfig;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.scaleType = scaleType;
    }

    @Override
    public ImageRequest clone() {
        return new ImageRequest(getUrlString(), maxWidth, maxHeight,
                scaleType, decodeConfig).setBitmapPool(bitmapPool);
    }

    public BitmapPool getBitmapPool() {
        return bitmapPool;
    }

    public ImageRequest setBitmapPool(BitmapPool bitmapPool) {
        this.bitmapPool = bitmapPool;
        return this;
    }

    @Override
    public Priority getPriority() {
        return Priority.LOW;
    }

    /**
     * Scales one side of a rectangle to fit aspect ratio.
     *
     * @param maxPrimary      Maximum size of the primary dimension (i.e. width for
     *                        max width), or zero to maintain aspect ratio with secondary
     *                        dimension
     * @param maxSecondary    Maximum size of the secondary dimension, or zero to
     *                        maintain aspect ratio with primary dimension
     * @param actualPrimary   Actual size of the primary dimension
     * @param actualSecondary Actual size of the secondary dimension
     * @param scaleType       The ScaleType used to calculate the needed image size.
     */
    private static int getResizedDimension(int maxPrimary, int maxSecondary, int actualPrimary,
                                           int actualSecondary, ScaleType scaleType) {

        // If no dominant value at all, just return the actual.
        if ((maxPrimary == 0) && (maxSecondary == 0)) {
            return actualPrimary;
        }

        // If ScaleType.FIT_XY fill the whole rectangle, ignore ratio.
        if (scaleType == ScaleType.FIT_XY) {
            if (maxPrimary == 0) {
                return actualPrimary;
            }
            return maxPrimary;
        }

        // If primary is unspecified, scale primary to match secondary's scaling ratio.
        if (maxPrimary == 0) {
            double ratio = (double) maxSecondary / (double) actualSecondary;
            return (int) (actualPrimary * ratio);
        }

        if (maxSecondary == 0) {
            return maxPrimary;
        }

        double ratio = (double) actualSecondary / (double) actualPrimary;
        int resized = maxPrimary;

        // If ScaleType.CENTER_CROP fill the whole rectangle, preserve aspect ratio.
        if (scaleType == ScaleType.CENTER_CROP) {
            if ((resized * ratio) < maxSecondary) {
                resized = (int) (maxSecondary / ratio);
            }
            return resized;
        }

        if ((resized * ratio) > maxSecondary) {
            resized = (int) (maxSecondary / ratio);
        }
        return resized;
    }

    @Override
    public Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
        // Serialize all decode on a global lock to reduce concurrent heap usage.
        synchronized (DECODE_LOCK) {
            try {
                return doParse(response);
            } catch (OutOfMemoryError e) {
                JusLog.error("Caught OOM for " + response.data.length + " byte image, url=" +
                        getUrl());
                return Response.error(new ParseError(e));
            }
        }
    }

    /**
     * The real guts of parseNetworkResponse. Broken out for readability.
     */
    private Response<Bitmap> doParse(NetworkResponse response) {
        byte[] data = response.data;
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        Bitmap bitmap = null;
        if (maxWidth == 0 && maxHeight == 0) {
            decodeOptions.inPreferredConfig = decodeConfig;
            //we don't have specific size to find a reusable bitmap so lets instead create a new
            // one
            try {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
            } catch (IllegalArgumentException ex) {
                JusLog.error("Unbounded decode failed: " + ex.getMessage());
            }
        } else {
            // If we have to resize this image, first get the natural bounds.
            decodeOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
            int actualWidth = decodeOptions.outWidth;
            int actualHeight = decodeOptions.outHeight;

            // Then compute the dimensions we would ideally like to decode to.
            int desiredWidth = getResizedDimension(maxWidth, maxHeight,
                    actualWidth, actualHeight, scaleType);
            int desiredHeight = getResizedDimension(maxHeight, maxWidth,
                    actualHeight, actualWidth, scaleType);

            // Decode to the nearest power of two scaling factor.
            decodeOptions.inJustDecodeBounds = false;

            decodeOptions.inPreferQualityOverSpeed = PREFER_QUALITY_OVER_SPEED;
            decodeOptions.inSampleSize =
                    findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);


            Bitmap tempBitmap = try2decodeByteArray(data, 0, data.length, decodeOptions);


            //TODO shall we optimise this with BitmapDrawable?
            // If necessary, scale down to the maximal acceptable size.
            if (tempBitmap != null && (tempBitmap.getWidth() > desiredWidth ||
                    tempBitmap.getHeight() > desiredHeight)) {
                bitmap = Bitmap.createScaledBitmap(tempBitmap,
                        desiredWidth, desiredHeight, true);
                if (bitmapPool != null) {
                    bitmapPool.addToPool(tempBitmap);
                } else {
                }
            } else {
                bitmap = tempBitmap;
            }
        }

        if (bitmap == null) {
            return Response.error(new ParseError(response));
        } else {
            return Response.success(bitmap, HttpHeaderParser.parseCacheHeaders(response));
        }
    }

    public Bitmap try2decodeByteArray(byte[] data, int offset, int length, BitmapFactory
            .Options decodeOptions) {
        Bitmap tempBitmap = null;
        addInBitmapOptions(decodeOptions);
        try {
            tempBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
        } catch (IllegalArgumentException ex) {
            JusLog.error("1st decode failed: " + ex.getMessage());
            decodeOptions.inBitmap = null;
        }

        //try to catch java.lang.IllegalArgumentException: Problem decoding into existing bitmap
        if (tempBitmap == null) {
            //try again
            addInBitmapOptions(decodeOptions);
            try {
                tempBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
            } catch (IllegalArgumentException ex) {
                JusLog.error("2nd decode failed: " + ex.getMessage());
                decodeOptions.inBitmap = null;
            }
        }

        //giveup and do it without inBitmap
        if (tempBitmap == null) {
            try {
                //just in case
                decodeOptions.inBitmap = null;
                tempBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
            } catch (IllegalArgumentException ex) {
                JusLog.error("3rd decode failed: " + ex.getMessage());
            }
        }
        return tempBitmap;
    }


    private void addInBitmapOptions(BitmapFactory.Options options) {
        // inBitmap only works with mutable bitmaps so force the decoder to
        // return mutable bitmaps.
        options.inMutable = true;
        if (bitmapPool != null) {
            // Try and find a bitmap to use for inBitmap
            options.inBitmap = bitmapPool.getReusableBitmap(options);
        }
    }

    /**
     * Returns the largest power-of-two divisor for use in downscaling a bitmap
     * that will not result in the scaling past the desired dimensions.
     *
     * @param actualWidth   Actual width of the bitmap
     * @param actualHeight  Actual height of the bitmap
     * @param desiredWidth  Desired width of the bitmap
     * @param desiredHeight Desired height of the bitmap
     */
    // Visible for testing.
    static int findBestSampleSize(
            int actualWidth, int actualHeight, int desiredWidth, int desiredHeight) {
        double wr = (double) actualWidth / desiredWidth;
        double hr = (double) actualHeight / desiredHeight;
        double ratio = Math.min(wr, hr);
        float n = 1.0f;
        while ((n * 2) <= ratio) {
            n *= 2;
        }

        return (int) n;
    }

    @Override
    public String toString() {
        return "ImageRequest{" +
                "bitmapPool=" + bitmapPool +
                ", decodeConfig=" + decodeConfig +
                ", maxWidth=" + maxWidth +
                ", maxHeight=" + maxHeight +
                ", scaleType=" + scaleType +
                "} " + super.toString();
    }
}
