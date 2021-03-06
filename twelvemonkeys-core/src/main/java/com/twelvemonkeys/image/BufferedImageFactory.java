/*
 * Copyright (c) 2008, Harald Kuhr
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name "TwelveMonkeys" nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.twelvemonkeys.image;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;
import java.lang.reflect.Array;

/**
 * A faster, lighter and easier way to convert an {@code Image} to a
 * {@code BufferedImage} than using a {@code PixelGrabber}.
 * Clients may provide progress listeners to monitor conversion progress.
 * <p/>
 * Supports source image subsampling and source region extraction.
 * Supports source images with 16 bit {@link ColorModel} and
 * {@link DataBuffer#TYPE_USHORT} transfer type, without converting to
 * 32 bit/TYPE_INT.
 * <p/>
 * NOTE: Does not support images with more than one {@code ColorModel} or
 * different types of pixel data. This is not very common.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @version $Id: //depot/branches/personal/haraldk/twelvemonkeys/release-2/twelvemonkeys-core/src/main/java/com/twelvemonkeys/image/BufferedImageFactory.java#1 $
 */
public final class BufferedImageFactory {
    private List<ProgressListener> mListeners;
    private int mPercentageDone;

    private ImageProducer mProducer;
    private boolean mError;
    private boolean mFetching;
    private boolean mReadColorModelOnly;

    private int mX = 0;
    private int mY = 0;
    private int mWidth = -1;
    private int mHeight = -1;

    private int mXSub = 1;
    private int mYSub = 1;

    private int mOffset;
    private int mScanSize;

    private ColorModel mSourceColorModel;
    private Hashtable mSourceProperties; // ImageConsumer API dictates Hashtable

    private Object mSourcePixels;

    private BufferedImage mBuffered;
    private ColorModel mColorModel;

    // NOTE: Just to not expose the inheritance
    private final Consumer mConsumer = new Consumer();

    /**
     * Creates a {@code BufferedImageFactory}.
     * @param pSource the source image
     */
    public BufferedImageFactory(Image pSource) {
        this(pSource.getSource());
    }

    /**
     * Creates a {@code BufferedImageFactory}.
     * @param pSource the source image producer
     */
    public BufferedImageFactory(ImageProducer pSource) {
        mProducer = pSource;
    }

    /**
     * Returns the {@code BufferedImage} extracted from the given
     * {@code ImageSource}. Multiple requests will return the same image.
     *
     * @return the {@code BufferedImage}
     *
     * @throws ImageConversionException if the given {@code ImageSource} cannot
     * be converted for some reason.
     */
    public BufferedImage getBufferedImage() throws ImageConversionException {
        doFetch(false);
        return mBuffered;
    }

    /**
     * Returns the {@code ColorModel} extracted from the
     * given {@code ImageSource}. Multiple requests will return the same model.
     *
     * @return the {@code ColorModel}
     *
     * @throws ImageConversionException if the given {@code ImageSource} cannot
     * be converted for some reason.
     */
    public ColorModel getColorModel() throws ImageConversionException {
        doFetch(true);
        return mBuffered != null ? mBuffered.getColorModel() : mColorModel;
    }

    /**
     * Frees resources used by this {@code BufferedImageFactory}.
     */
    public void dispose() {
        freeResources();
        mBuffered = null;
        mColorModel = null;
    }

    /**
     * Aborts the image prodcution.
     */
    public void abort() {
        mConsumer.imageComplete(ImageConsumer.IMAGEABORTED);
    }

    /**
     * Sets the source region (AOI) for the new image.
     *
     * @param pRect the source region
     */
    public void setSourceRegion(Rectangle pRect) {
        // Refetch everything, if region changed
        if (mX != pRect.x || mY != pRect.y || mWidth != pRect.width || mHeight != pRect.height) {
            dispose();
        }

        mX = pRect.x;
        mY = pRect.y;
        mWidth = pRect.width;
        mHeight = pRect.height;
    }

    /**
     * Sets the source subsampling for the new image.
     *
     * @param pXSub horisontal subsampling factor
     * @param pYSub vertical subsampling factor
     */
    public void setSourceSubsampling(int pXSub, int pYSub) {
        // Refetch everything, if subsampling changed
        if (mXSub != pXSub || mYSub != pYSub) {
            dispose();
        }

        if (pXSub > 1) {
            mXSub = pXSub;
        }
        if (pYSub > 1) {
            mYSub = pYSub;
        }
    }

    private synchronized void doFetch(boolean pColorModelOnly) throws ImageConversionException {
        if (!mFetching && (!pColorModelOnly && mBuffered == null || mBuffered == null && mSourceColorModel == null)) {
            // NOTE: Subsampling is only applied if extracting full image
            if (!pColorModelOnly && (mXSub > 1 || mYSub > 1)) {
                // If only sampling a region, the region must be scaled too
                if (mWidth > 0 && mHeight > 0) {
                    mWidth = (mWidth + mXSub - 1) / mXSub;
                    mHeight = (mHeight + mYSub - 1) / mYSub;

                    mX = (mX + mXSub - 1) / mXSub;
                    mY = (mY + mYSub - 1) / mYSub;
                }

                mProducer = new FilteredImageSource(mProducer, new SubsamplingFilter(mXSub, mYSub));
            }

            // Start fetching
            mFetching = true;
            mReadColorModelOnly = pColorModelOnly;
            mProducer.startProduction(mConsumer); // Note: If single-thread (synchronous), this call will block


            // Wait until the producer wakes us up, by calling imageComplete
            while (mFetching) {
                try {
                    wait();
                }
                catch (InterruptedException e) {
                    throw new ImageConversionException("Image conversion aborted: " + e.getMessage(), e);
                }
            }

            if (mError) {
                throw new ImageConversionException("Image conversion failed: ImageConsumer.IMAGEERROR.");
            }

            if (pColorModelOnly) {
                createColorModel();
            }
            else {
                createBuffered();
            }
        }
    }

    private void createColorModel() {
        mColorModel = mSourceColorModel;

        // Clean up, in case any objects are copied/cloned, so we can free resources
        freeResources();
    }

    private void createBuffered() {
        if (mWidth > 0 && mHeight > 0) {
            if (mSourceColorModel != null && mSourcePixels != null) {
                // TODO: Fix pixel size / color model problem
                WritableRaster raster = ImageUtil.createRaster(mWidth, mHeight, mSourcePixels, mSourceColorModel);
                mBuffered = new BufferedImage(mSourceColorModel, raster, mSourceColorModel.isAlphaPremultiplied(), mSourceProperties);
            }
            else {
                mBuffered = ImageUtil.createClear(mWidth, mHeight, null);
            }
        }

        // Clean up, in case any objects are copied/cloned, so we can free resources
        freeResources();
    }

    private void freeResources() {
        mSourceColorModel = null;
        mSourcePixels = null;
        mSourceProperties = null;
    }

    private void processProgress(int mScanline) {
        if (mListeners != null) {
            int percent = 100 * mScanline / mHeight;

            //System.out.println("Progress: " + percent + "%");

            if (percent > mPercentageDone) {
                mPercentageDone = percent;

                // TODO: Fix concurrent modification if a listener removes itself...
                for (ProgressListener listener : mListeners) {
                    listener.progress(this, percent);
                }
            }
        }
    }

    /**
     * Adds a progress listener to this factory.
     *
     * @param pListener the progress listener
     */
    public void addProgressListener(ProgressListener pListener) {
        if (mListeners == null) {
            mListeners = new ArrayList<ProgressListener>();
        }
        mListeners.add(pListener);
    }

    /**
     * Removes a progress listener from this factory.
     *
     * @param pListener the progress listener
     */
    public void removeProgressListener(ProgressListener pListener) {
        if (mListeners == null) {
            return;
        }
        mListeners.remove(pListener);
    }

    /**
     * Removes all progress listeners from this factory.
     */
    public void removeAllProgressListeners() {
        if (mListeners != null) {
            mListeners.clear();
        }
    }

    /**
     * Converts an array of {@code int} pixles to an array of {@code short}
     * pixels. The conversion is done, by masking out the
     * <em>higher 16 bits</em> of the {@code int}.
     *
     * For eny given {@code int}, the {@code short} value is computed as
     * follows:
     * <blockquote>{@code
     * short value = (short) (intValue & 0x0000ffff);
     * }</blockquote>
     *
     * @param pPixels the pixel data to convert
     * @return an array of {@code short}s, same lenght as {@code pPixels}
     */
    private static short[] toShortPixels(int[] pPixels) {
        short[] pixels = new short[pPixels.length];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (short) (pPixels[i] & 0xffff);
        }
        return pixels;
    }

    /**
     * This interface allows clients of a {@code BufferedImageFactory} to
     * receive notifications of decoding progress.
     *
     * @see BufferedImageFactory#addProgressListener
     * @see BufferedImageFactory#removeProgressListener
     */
    public static interface ProgressListener extends EventListener {

        /**
         * Reports progress to this listener.
         * Invoked by the {@code BufferedImageFactory} to report progress in
         * the image decoding.
         *
         * @param pFactory the factory reporting the progress
         * @param pPercentage the perccentage of progress
         */
        void progress(BufferedImageFactory pFactory, float pPercentage);
    }

    private class Consumer implements ImageConsumer {
        /**
         * Implementation of all setPixels methods.
         * Note that this implementation assumes that all invocations for one
         * image uses the same color model, and that the pixel data has the
         * same type.
         *
         * @param pX x coordinate of pixel data region
         * @param pY y coordinate of pixel data region
         * @param pWidth width of pixel data region
         * @param pHeight height of pixel data region
         * @param pModel the color model of the pixel data
         * @param pPixels the pixel data array
         * @param pOffset the offset into the pixel data array
         * @param pScanSize the scan size of the pixel data array
         */
        private void setPixelsImpl(int pX, int pY, int pWidth, int pHeight, ColorModel pModel, Object pPixels, int pOffset, int pScanSize) {
            setColorModelOnce(pModel);

            if (pPixels == null) {
                return;
            }

            //System.out.println("Setting " + pPixels.getClass().getComponentType() + " pixels: " + Array.getLength(pPixels));

            // Allocate array if neccessary
            if (mSourcePixels == null) {
                /*
                System.out.println("ColorModel: " + pModel);
                System.out.println("Scansize: " + pScanSize + " TrasferType: " + ImageUtil.getTransferType(pModel));
                System.out.println("Creating " + pPixels.getClass().getComponentType() + " array of length " + (mWidth * mHeight));
                */
                // Allocate a suitable source pixel array
                // TODO: Should take pixel "width" into consideration, for byte packed rasters?!
                // OR... Is anything but single-pixel models really supported by the API?
                mSourcePixels = Array.newInstance(pPixels.getClass().getComponentType(), mWidth * mHeight);
                mScanSize = mWidth;
                mOffset = 0;
            }
            else if (mSourcePixels.getClass() != pPixels.getClass()) {
                throw new IllegalStateException("Only one pixel type allowed");
            }

            // AOI stuff
            if (pY < mY) {
                int diff = mY - pY;
                if (diff >= pHeight) {
                    return;
                }
                pOffset += pScanSize * diff;
                pY += diff;
                pHeight -= diff;
            }
            if (pY + pHeight > mY + mHeight) {
                pHeight = (mY + mHeight) - pY;
                if (pHeight <= 0) {
                    return;
                }
            }

            if (pX < mX) {
                int diff = mX - pX;
                if (diff >= pWidth) {
                    return;
                }
                pOffset += diff;
                pX += diff;
                pWidth -= diff;
            }
            if (pX + pWidth > mX + mWidth) {
                pWidth = (mX + mWidth) - pX;
                if (pWidth <= 0) {
                    return;
                }
            }

            int dstOffset = mOffset + (pY - mY) * mScanSize + (pX - mX);

            // Do the pixel copying
            for (int i = pHeight; i > 0; i--) {
                System.arraycopy(pPixels, pOffset, mSourcePixels, dstOffset, pWidth);
                pOffset += pScanSize;
                dstOffset += mScanSize;
            }

            processProgress(pY + pHeight);
        }

        /** {@code ImageConsumer} implementation, do not invoke directly */
        public void setPixels(int pX, int pY, int pWidth, int pHeight, ColorModel pModel, short[] pPixels, int pOffset, int pScanSize) {
            setPixelsImpl(pX, pY, pWidth, pHeight, pModel, pPixels, pOffset, pScanSize);
        }

        private void setColorModelOnce(ColorModel pModel) {
            // NOTE: There seems to be a "bug" in AreaAveragingScaleFilter, as it
            // first passes the original colormodel through in setColorModel, then
            // later replaces it with the default RGB in the first setPixels call
            // (this is probably allowed according to the spec, but it's a waste of
            // time and space).
            if (mSourceColorModel != pModel) {
                if (/*mSourceColorModel == null ||*/ mSourcePixels == null) {
                    mSourceColorModel = pModel;
                }
                else {
                    throw new IllegalStateException("Change of ColorModel after pixel delivery not supported");
                }
            }

            // If color model is all we ask for, stop now
            if (mReadColorModelOnly) {
                mConsumer.imageComplete(ImageConsumer.IMAGEABORTED);
            }
        }

        /** {@code ImageConsumer} implementation, do not invoke */
        public void imageComplete(int pStatus) {
            mFetching = false;

            if (mProducer != null) {
                mProducer.removeConsumer(this);
            }

            switch (pStatus) {
                case IMAGEERROR:
                    new Error().printStackTrace();
                    mError = true;
                break;
            }

            synchronized (BufferedImageFactory.this) {
                BufferedImageFactory.this.notifyAll();
            }
        }

        /** {@code ImageConsumer} implementation, do not invoke directly */
        public void setColorModel(ColorModel pModel) {
            //System.out.println("SetColorModel: " + pModel);
            setColorModelOnce(pModel);
        }

        /** {@code ImageConsumer} implementation, do not invoke directly */
        public void setDimensions(int pWidth, int pHeight) {
            //System.out.println("Setting dimensions: " + pWidth + ", " + pHeight);
            if (mWidth < 0) {
                mWidth = pWidth - mX;
            }
            if (mHeight < 0) {
                mHeight = pHeight - mY;
            }

            // Hmm.. Special case, but is it a good idea?
            if (mWidth <= 0 || mHeight <= 0) {
                imageComplete(STATICIMAGEDONE);
            }
        }

        /** {@code ImageConsumer} implementation, do not invoke directly */
        public void setHints(int pHintflags) {
           // ignore
        }

        /** {@code ImageConsumer} implementation, do not invoke directly */
        public void setPixels(int pX, int pY, int pWidth, int pHeight, ColorModel pModel, byte[] pPixels, int pOffset, int pScanSize) {
            /*if (pModel.getPixelSize() < 8) {
                // Byte packed
                setPixelsImpl(pX, pY, pWidth, pHeight, pModel, toBytePackedPixels(pPixels, pModel.getPixelSize()), pOffset, pScanSize);
            }
            /*
            else if (pModel.getPixelSize() > 8) {
                // Byte interleaved
                setPixelsImpl(pX, pY, pWidth, pHeight, pModel, toByteInterleavedPixels(pPixels), pOffset, pScanSize);
            }
            */
            //else {
                // Default, pixelSize == 8, one byte pr pixel
                setPixelsImpl(pX, pY, pWidth, pHeight, pModel, pPixels, pOffset, pScanSize);
            //}
        }

        /** {@code ImageConsumer} implementation, do not invoke directly */
        public void setPixels(int pX, int pY, int pWeigth, int pHeight, ColorModel pModel, int[] pPixels, int pOffset, int pScanSize) {
            if (ImageUtil.getTransferType(pModel) == DataBuffer.TYPE_USHORT) {
                // NOTE: Workaround for limitation in ImageConsumer API
                // Convert int[] to short[], to be compatible with the ColorModel
                setPixelsImpl(pX, pY, pWeigth, pHeight, pModel, toShortPixels(pPixels), pOffset, pScanSize);
            }
            else {
                setPixelsImpl(pX, pY, pWeigth, pHeight, pModel, pPixels, pOffset, pScanSize);
            }
        }

        /** {@code ImageConsumer} implementation, do not invoke directly */
        public void setProperties(Hashtable pProperties) {
            mSourceProperties = pProperties;
        }
    }
}