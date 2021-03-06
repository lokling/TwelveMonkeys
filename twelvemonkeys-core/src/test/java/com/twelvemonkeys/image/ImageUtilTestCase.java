
package com.twelvemonkeys.image;

import junit.framework.TestCase;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by IntelliJ IDEA.
 *
 * @author $author wmhakur$
 * @version $id: $
 * To change this template use Options | File Templates.
 */
public class ImageUtilTestCase extends TestCase {

    private final static String IMAGE_NAME = "/sunflower.jpg";
    private BufferedImage mOriginal;
    private BufferedImage mImage;
    private Image mScaled;

    public ImageUtilTestCase() throws Exception {
        mImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        mScaled = mImage.getScaledInstance(5, 5, Image.SCALE_FAST);

        // Read image from class path
        InputStream is = getClass().getResourceAsStream(IMAGE_NAME);
        mOriginal = ImageIO.read(is);

        assertNotNull(mOriginal);
    }

    /*
    public void setUp() throws Exception {
        mImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        mScaled = mImage.getScaledInstance(5, 5, Image.SCALE_FAST);

        // Read image from class path
        InputStream is = ClassLoader.getSystemResourceAsStream(IMAGE_NAME);
        mOriginal = ImageIO.read(is);

        assertNotNull(mOriginal);
    }

    protected void tearDown() throws Exception {
        mOriginal = null;
    }
    */

    public void testToBufferedImageNull() {
        BufferedImage img = null;
        boolean threwRuntimeException = false;

        try {
            img = ImageUtil.toBuffered((Image) null);
        }
        catch (RuntimeException ne) {
            threwRuntimeException = true;
        }
        // No input should return null
        assertNull(img);

        // Should have thrown an exception
        assertTrue(threwRuntimeException);
    }

    public void testToBufferedImageTypeNull() {
        BufferedImage img = null;
        boolean threwRuntimeException = false;

        try {
            img = ImageUtil.toBuffered(null, BufferedImage.TYPE_INT_ARGB);
        }
        catch (RuntimeException ne) {
            threwRuntimeException = true;
        }
        // No input should return null
        assertNull(img);

        // Should have thrown an exception
        assertTrue(threwRuntimeException);
    }

    public void testImageIsNotBufferedImage() {
        // Should not be a buffered image
        assertFalse(
                "FOR SOME IMPLEMENTATIONS THIS MIGHT FAIL!\nIn that case, testToBufferedImage() will fail too.",
                mScaled instanceof BufferedImage
        );
    }

    public void testToBufferedImage() {
        BufferedImage sameAsImage = ImageUtil.toBuffered((RenderedImage) mImage);
        BufferedImage bufferedScaled = ImageUtil.toBuffered(mScaled);

        // Should be no need to convert
        assertSame(mImage, sameAsImage);

        // Should have same dimensions
        assertEquals(mScaled.getWidth(null), bufferedScaled.getWidth());
        assertEquals(mScaled.getHeight(null), bufferedScaled.getHeight());

        // Hmmm...
        assertTrue(new Integer(42).equals(bufferedScaled.getProperty("lucky-number"))
                   || bufferedScaled.getPropertyNames() == null
                   || bufferedScaled.getPropertyNames().length == 0);
    }

    public void testToBufferedImageType() {
        // Assumes mImage is TYPE_INT_ARGB
        BufferedImage converted = ImageUtil.toBuffered(mImage, BufferedImage.TYPE_BYTE_INDEXED);
        BufferedImage convertedToo = ImageUtil.toBuffered(mImage, BufferedImage.TYPE_BYTE_BINARY);

        // Should not be the same
        assertNotSame(mImage, converted);
        assertNotSame(mImage, convertedToo);

        // Correct type
        assertTrue(converted.getType() == BufferedImage.TYPE_BYTE_INDEXED);
        assertTrue(convertedToo.getType() == BufferedImage.TYPE_BYTE_BINARY);

        // Should have same dimensions
        assertEquals(mImage.getWidth(), converted.getWidth());
        assertEquals(mImage.getHeight(), converted.getHeight());

        assertEquals(mImage.getWidth(), convertedToo.getWidth());
        assertEquals(mImage.getHeight(), convertedToo.getHeight());
    }

    public void testBrightness() {
        final BufferedImage original = mOriginal;
        assertNotNull(original);

        final BufferedImage notBrightened = ImageUtil.toBuffered(ImageUtil.brightness(original, 0f));
        // Assumed: Images should be equal
        if (original != notBrightened) { // Don't care to test if images are same
            for (int y = 0; y < original.getHeight(); y++) {
                for (int x = 0; x < original.getWidth(); x++) {
                    assertEquals(original.getRGB(x, y), notBrightened.getRGB(x, y));
                }
            }
        }

        // Assumed: All pixels should be brighter or equal to original
        final BufferedImage brightened = ImageUtil.toBuffered(ImageUtil.brightness(original, 0.4f));
        final BufferedImage brightenedMore = ImageUtil.toBuffered(ImageUtil.brightness(original, 0.9f));
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                assertTrue(original.getRGB(x, y) <= brightened.getRGB(x, y));
                assertTrue(brightened.getRGB(x, y) <= brightenedMore.getRGB(x, y));
            }
        }

        // Assumed: Image should be all white
        final BufferedImage brightenedMax = ImageUtil.toBuffered(ImageUtil.brightness(original, 2f));
        for (int y = 0; y < brightenedMax.getHeight(); y++) {
            for (int x = 0; x < brightenedMax.getWidth(); x++) {
                assertEquals(0x00FFFFFF, brightenedMax.getRGB(x, y) & 0x00FFFFFF);
            }
        }

        // Assumed: All pixels should be darker or equal to originial
        final BufferedImage brightenedNegative = ImageUtil.toBuffered(ImageUtil.brightness(original, -0.4f));
        final BufferedImage brightenedNegativeMore = ImageUtil.toBuffered(ImageUtil.brightness(original, -0.9f));
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                assertTrue(original.getRGB(x, y) >= brightenedNegative.getRGB(x, y));
                assertTrue(brightenedNegative.getRGB(x, y) >= brightenedNegativeMore.getRGB(x, y));
            }
        }
        // Assumed: Image should be all black
        final BufferedImage brightenedMaxNegative = ImageUtil.toBuffered(ImageUtil.brightness(original, -2f));
        for (int y = 0; y < brightenedMaxNegative.getHeight(); y++) {
            for (int x = 0; x < brightenedMaxNegative.getWidth(); x++) {
               assertEquals(0x0, brightenedMaxNegative.getRGB(x, y) & 0x00FFFFFF);
            }
        }

        /*
        JFrame frame = new JFrame("Sunflower - brightness");
        frame.setSize(sunflower.getWidth() * 4, sunflower.getHeight() * 2);

        Canvas canvas = new Canvas() {
            public void paint(Graphics g) {
                // Draw original for comparison
                g.drawImage(original, 0, 0, null);

                // This should look like original
                g.drawImage(notBrightened, 0, original.getHeight(), null);

                // Different versions
                g.drawImage(brightened, original.getWidth(), 0, null);
                g.drawImage(brightenedMore, original.getWidth() * 2, 0, null);
                g.drawImage(brightenedMax, original.getWidth() * 3, 0, null);

                g.drawImage(brightenedNegative, original.getWidth(), original.getHeight(), null);
                g.drawImage(brightenedNegativeMore, original.getWidth() * 2, original.getHeight(), null);
                g.drawImage(brightenedMaxNegative, original.getWidth() * 3, original.getHeight(), null);
            }
        };

        frame.getContentPane().add(canvas);
        frame.setVisible(true);

        assertTrue(true);
        */
    }


    public void testContrast() {
        final BufferedImage original = mOriginal;

        assertNotNull(original);

        final BufferedImage notContrasted = ImageUtil.toBuffered(ImageUtil.contrast(original, 0f));
        // Assumed: Images should be equal
        if (original != notContrasted) { // Don't care to test if images are same
            for (int y = 0; y < original.getHeight(); y++) {
                for (int x = 0; x < original.getWidth(); x++) {
                    assertEquals("0 constrast should not change image", original.getRGB(x, y), notContrasted.getRGB(x, y));
                }
            }
        }

        // Assumed: Contrast should be greater or equal to original
        final BufferedImage contrasted = ImageUtil.toBuffered(ImageUtil.contrast(original));
        final BufferedImage contrastedDefault = ImageUtil.toBuffered(ImageUtil.contrast(original, 0.5f));
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                int oRGB = original.getRGB(x, y);
                int cRGB = contrasted.getRGB(x, y);
                int dRGB = contrastedDefault.getRGB(x, y);

                int oR = oRGB >> 16 & 0xFF;
                int oG = oRGB >> 8 & 0xFF;
                int oB = oRGB & 0xFF;

                int cR = cRGB >> 16 & 0xFF;
                int cG = cRGB >> 8 & 0xFF;
                int cB = cRGB & 0xFF;

                int dR = dRGB >> 16 & 0xFF;
                int dG = dRGB >> 8 & 0xFF;
                int dB = dRGB & 0xFF;

                // RED
                if (oR < 127) {
                    assertTrue("Contrast should be decreased or same", oR >= cR && cR >= dR);
                }
                else {
                    assertTrue("Contrast should be increased or same", oR <= cR && cR <= dR);
                }
                // GREEN
                if (oG < 127) {
                    assertTrue("Contrast should be decreased or same", oG >= cG && cG >= dG);
                }
                else {
                    assertTrue("Contrast should be increased or same", oG <= cG && cG <= dG);
                }
                // BLUE
                if (oB < 127) {
                    assertTrue("Contrast should be decreased or same", oB >= cB && cB >= dB);
                }
                else {
                    assertTrue("Contrast should be increased or same", oB <= cB && cB <= dB);
                }

            }
        }
        // Assumed: Only primary colors (w/b/r/g/b/c/y/m)
        final BufferedImage contrastedMax = ImageUtil.toBuffered(ImageUtil.contrast(original, 1f));
        for (int y = 0; y < contrastedMax.getHeight(); y++) {
            for (int x = 0; x < contrastedMax.getWidth(); x++) {
                int rgb = contrastedMax.getRGB(x, y);
                int r = rgb >> 16 & 0xFF;
                int g = rgb >> 8 & 0xFF;
                int b = rgb & 0xFF;
                assertTrue("Max contrast should only produce primary colors", r == 0 || r == 255);
                assertTrue("Max contrast should only produce primary colors", g == 0 || g == 255);
                assertTrue("Max contrast should only produce primary colors", b == 0 || b == 255);
            }
        }

        // Assumed: Contrasts should be less than or equal to original
        final BufferedImage contrastedNegative = ImageUtil.toBuffered(ImageUtil.contrast(original, -0.5f));
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                int oRGB = original.getRGB(x, y);
                int cRGB = contrastedNegative.getRGB(x, y);

                int oR = oRGB >> 16 & 0xFF;
                int oG = oRGB >> 8 & 0xFF;
                int oB = oRGB & 0xFF;

                int cR = cRGB >> 16 & 0xFF;
                int cG = cRGB >> 8 & 0xFF;
                int cB = cRGB & 0xFF;

                // RED
                if (oR >= 127) {
                    assertTrue("Contrast should be decreased or same", oR >= cR);
                }
                else {
                    assertTrue("Contrast should be increased or same", oR <= cR);
                }
                // GREEN
                if (oG >= 127) {
                    assertTrue("Contrast should be decreased or same", oG >= cG);
                }
                else {
                    assertTrue("Contrast should be increased or same", oG <= cG);
                }
                // BLUE
                if (oB >= 127) {
                    assertTrue("Contrast should be decreased or same", oB >= cB);
                }
                else {
                    assertTrue("Contrast should be increased or same", oB <= cB);
                }
            }
        }

        // Assumed: All gray (127)!
        final BufferedImage contrastedMoreNegative = ImageUtil.toBuffered(ImageUtil.contrast(original, -1.0f));
        for (int y = 0; y < contrastedMoreNegative.getHeight(); y++) {
            for (int x = 0; x < contrastedMoreNegative.getWidth(); x++) {
                int rgb = contrastedMoreNegative.getRGB(x, y);
                int r = rgb >> 16 & 0xFF;
                int g = rgb >> 8 & 0xFF;
                int b = rgb & 0xFF;
                assertTrue("Minimum contrast should be all gray", r == 127 && g == 127 &&b == 127);
            }
        }

        /*
        JFrame frame = new JFrame("Sunflower - contrast");
        frame.setSize(sunflower.getWidth() * 4, sunflower.getHeight() * 2);

        Canvas canvas = new Canvas() {
            public void paint(Graphics g) {
                // Draw original for comparison
                g.drawImage(original, 0, 0, null);

                // This should look like original
                g.drawImage(notContrasted, 0, original.getHeight(), null);

                // Different versions
                g.drawImage(contrasted, original.getWidth(), 0, null);
                g.drawImage(contrastedDefault, original.getWidth() * 2, 0, null);
                g.drawImage(contrastedMax, original.getWidth() * 3, 0, null);
                g.drawImage(contrastedNegative, original.getWidth() * 2, original.getHeight(), null);
                g.drawImage(contrastedMoreNegative, original.getWidth() * 3, original.getHeight(), null);
            }
        };

        frame.getContentPane().add(canvas);
        frame.setVisible(true);

        assertTrue(true);
        */
    }

    public void testSharpen() {
        final BufferedImage original = mOriginal;

        assertNotNull(original);

        final BufferedImage notSharpened = ImageUtil.sharpen(original, 0f);
        // Assumed: Images should be equal
        if (original != notSharpened) { // Don't care to test if images are same
            for (int y = 0; y < original.getHeight(); y++) {
                for (int x = 0; x < original.getWidth(); x++) {
                    assertEquals("0 sharpen should not change image", original.getRGB(x, y), notSharpened.getRGB(x, y));
                }
            }
        }

        // Assumed: Difference between neighbouring pixels should increase for higher sharpen values
        // Assumed: Dynamics of entire image should not change
        final BufferedImage sharpened = ImageUtil.sharpen(original);
        final BufferedImage sharpenedDefault = ImageUtil.sharpen(original, 0.3f);
        final BufferedImage sharpenedMore = ImageUtil.sharpen(original, 1.3f);

        long diffOriginal = 0;
        long diffSharpened = 0;
        long diffDefault = 0;
        long diffMore = 0;

        long absDiffOriginal = 0;
        long absDiffSharpened = 0;
        long absDiffDefault = 0;
        long absDiffMore = 0;

        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 1; x < original.getWidth(); x++) {
                int oRGB = 0x00FFFFFF & original.getRGB(x, y);
                int sRGB = 0x00FFFFFF & sharpened.getRGB(x, y);
                int dRGB = 0x00FFFFFF & sharpenedDefault.getRGB(x, y);
                int mRGB = 0x00FFFFFF & sharpenedMore.getRGB(x, y);

                int poRGB = 0x00FFFFFF & original.getRGB(x - 1, y);
                int psRGB = 0x00FFFFFF & sharpened.getRGB(x - 1, y);
                int pdRGB = 0x00FFFFFF & sharpenedDefault.getRGB(x - 1, y);
                int pmRGB = 0x00FFFFFF & sharpenedMore.getRGB(x - 1, y);

                diffOriginal += poRGB - oRGB;
                diffSharpened += psRGB - sRGB;
                diffDefault += pdRGB - dRGB;
                diffMore += pmRGB - mRGB;

                absDiffOriginal += Math.abs(poRGB - oRGB);
                absDiffSharpened += Math.abs(psRGB - sRGB);
                absDiffDefault += Math.abs(pdRGB - dRGB);
                absDiffMore += Math.abs(pmRGB - mRGB);
            }
        }

        //*
        showEm(original, notSharpened, sharpened, sharpenedDefault, sharpenedMore, "sharpen");
        //*/

//        assertEquals("Difference should not change", diffOriginal, diffSharpened);
        assertTrue("Abs difference should increase", absDiffOriginal < absDiffSharpened);
//        assertEquals("Difference should not change", diffOriginal, diffDefault);
        assertTrue("Abs difference should increase", absDiffOriginal < absDiffDefault);
//        assertEquals("Difference should not change", diffOriginal, diffMore);
        assertTrue("Abs difference should increase", absDiffOriginal < absDiffMore);
//        assertEquals("Difference should not change", diffSharpened, diffMore);
        assertTrue("Abs difference should increase", absDiffSharpened < absDiffMore);
    }

    private void showEm(final BufferedImage pOriginal, final BufferedImage pNotSharpened, final BufferedImage pSharpened, final BufferedImage pSharpenedDefault, final BufferedImage pSharpenedMore, final String pTitle) {
        if (pOriginal != null) {
            return;
        }

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    JFrame frame = new JFrame("Sunflower - " + pTitle);
                    frame.setSize(pOriginal.getWidth() * 4, pOriginal.getHeight() * 2);

                    Canvas canvas = new Canvas() {
                        public void paint(Graphics g) {
                            // Draw original for comparison
                            g.drawImage(pOriginal, 0, 0, null);

                            // This should look like original
                            g.drawImage(pNotSharpened, 0, pOriginal.getHeight(), null);

                            // Different versions
                            g.drawImage(pSharpened, pOriginal.getWidth(), 0, null);
                            g.drawImage(pSharpenedDefault, pOriginal.getWidth() * 2, 0, null);
                            g.drawImage(pSharpenedMore, pOriginal.getWidth() * 3, 0, null);
                        }
                    };

                    frame.getContentPane().add(canvas);
                    frame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            synchronized (ImageUtilTestCase.this) {
                                ImageUtilTestCase.this.notify();
                            }
                        }
                    });
                    frame.setVisible(true);
                }
            });
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        synchronized (ImageUtilTestCase.this) {
            try {
                ImageUtilTestCase.this.wait();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void testBlur() {
        final BufferedImage original = mOriginal;

        assertNotNull(original);

        final BufferedImage notBlurred = ImageUtil.blur(original, 0f);
        // Assumed: Images should be equal
        if (original != notBlurred) { // Don't care to test if images are same
            for (int y = 0; y < original.getHeight(); y++) {
                for (int x = 0; x < original.getWidth(); x++) {
                    assertEquals("0 blur should not change image", original.getRGB(x, y), notBlurred.getRGB(x, y));
                }
            }
        }

        // Assumed: Difference between neighbouring pixels should decrease for higher blur values
        // Assumed: Dynamics of entire image should not change
        final BufferedImage blurred = ImageUtil.blur(original);
        final BufferedImage blurredDefault = ImageUtil.blur(original, 1.5f);
        final BufferedImage blurredMore = ImageUtil.blur(original, 3f);

        long diffOriginal = 0;
        long diffBlurred = 0;
        long diffDefault = 0;
        long diffMore = 0;

        long absDiffOriginal = 0;
        long absDiffBlurred = 0;
        long absDiffDefault = 0;
        long absDiffMore = 0;


        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 1; x < original.getWidth(); x++) {
                int oRGB = 0x00FFFFFF & original.getRGB(x, y);
                int bRGB = 0x00FFFFFF & blurred.getRGB(x, y);
                int dRGB = 0x00FFFFFF & blurredDefault.getRGB(x, y);
                int mRGB = 0x00FFFFFF & blurredMore.getRGB(x, y);

                int poRGB = 0x00FFFFFF & original.getRGB(x - 1, y);
                int pbRGB = 0x00FFFFFF & blurred.getRGB(x - 1, y);
                int pdRGB = 0x00FFFFFF & blurredDefault.getRGB(x - 1, y);
                int pmRGB = 0x00FFFFFF & blurredMore.getRGB(x - 1, y);

                diffOriginal += poRGB - oRGB;
                diffBlurred += pbRGB - bRGB;
                diffDefault += pdRGB - dRGB;
                diffMore += pmRGB - mRGB;

                absDiffOriginal += Math.abs(poRGB - oRGB);
                absDiffBlurred += Math.abs(pbRGB - bRGB);
                absDiffDefault += Math.abs(pdRGB - dRGB);
                absDiffMore += Math.abs(pmRGB - mRGB);
            }
        }

        showEm(original, notBlurred, blurred,  blurredDefault, blurredMore, "blur");

//        assertEquals("Difference should not change", diffOriginal, diffBlurred);
        assertTrue(String.format("Abs difference should decrease: %s <= %s", absDiffOriginal, absDiffBlurred), absDiffOriginal > absDiffBlurred);
//        assertEquals("Difference should not change", diffOriginal, diffDefault);
        assertTrue("Abs difference should decrease", absDiffOriginal > absDiffDefault);
//        assertEquals("Difference should not change", diffOriginal, diffMore);
        assertTrue("Abs difference should decrease", absDiffOriginal > absDiffMore);
//        assertEquals("Difference should not change", diffBlurred, diffMore);
        assertTrue("Abs difference should decrease", absDiffBlurred > absDiffMore);
    }

    public void testIndexImage() {
        BufferedImage sunflower = mOriginal;

        assertNotNull(sunflower);

        BufferedImage image = ImageUtil.createIndexed(sunflower);
        assertNotNull("Image was null", image);
        assertTrue(image.getColorModel() instanceof IndexColorModel);
    }
}