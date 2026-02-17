package com.isec.platform.modules.ocr.infrastructure.preprocess;

import com.isec.platform.modules.ocr.config.OcrPreprocessProperties;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.Core;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

@Component
@Slf4j
public class OpenCvPreprocessor implements ImagePreprocessor {

    private static volatile boolean openCvAvailable = false;

    static {
        try {
            nu.pattern.OpenCV.loadShared();
            openCvAvailable = true;
            log.info("OpenCV loaded successfully via openpnp");
        } catch (Throwable e) {
            try {
                System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
                openCvAvailable = true;
                log.info("OpenCV loaded via System.loadLibrary");
            } catch (Throwable e2) {
                openCvAvailable = false;
                log.warn("OpenCV native library not loaded: {}", e2.getMessage());
            }
        }
    }

    private final OcrPreprocessProperties properties;

    public OpenCvPreprocessor(OcrPreprocessProperties properties) {
        this.properties = properties;
    }

    @Override
    public BufferedImage preprocess(BufferedImage input) {
        if (!properties.isEnabled() || !openCvAvailable) {
            return input;
        }
        try {
            Mat mat = bufferedImageToMat(input);

            // Convert to grayscale
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);

            // Contrast enhancement (CLAHE)
            if (properties.getClaheClipLimit() > 0) {
                CLAHE clahe = Imgproc.createCLAHE(properties.getClaheClipLimit(),
                        new Size(properties.getClaheTileSize(), properties.getClaheTileSize()));
                clahe.apply(mat, mat);
            }

            // Resize to normalize DPI roughly
            if (properties.getScaleFactor() > 1.0) {
                Imgproc.resize(mat, mat, new Size(mat.width() * properties.getScaleFactor(),
                        mat.height() * properties.getScaleFactor()));
            }

            // Noise reduction
            if (properties.getMedianBlurKernel() >= 3 && properties.getMedianBlurKernel() % 2 == 1) {
                Imgproc.medianBlur(mat, mat, properties.getMedianBlurKernel());
            }

            // Thresholding
            if (properties.isAdaptiveThreshold()) {
                int blockSize = properties.getAdaptiveBlockSize();
                if (blockSize % 2 == 0) blockSize += 1;
                if (blockSize < 3) blockSize = 3;
                Imgproc.adaptiveThreshold(mat, mat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                        Imgproc.THRESH_BINARY, blockSize, properties.getAdaptiveC());
            } else {
                Imgproc.threshold(mat, mat, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
            }

            if (properties.isDeskewEnabled()) {
                mat = deskew(mat);
            }

            return matToBufferedImage(mat);
        } catch (Throwable t) {
            log.warn("Preprocessing failed or OpenCV not available, returning original image: {}", t.getMessage());
            return input;
        }
    }

    private static Mat deskew(Mat mat) {
        Mat nonZero = new Mat();
        Core.findNonZero(mat, nonZero);
        if (nonZero.empty()) return mat;

        MatOfPoint points = new MatOfPoint(nonZero);
        MatOfPoint2f points2f = new MatOfPoint2f(points.toArray());
        RotatedRect box = Imgproc.minAreaRect(points2f);
        double angle = box.angle;
        if (angle < -45.0) angle += 90.0;
        if (Math.abs(angle) < 0.5) return mat;

        Point center = new Point(mat.width() / 2.0, mat.height() / 2.0);
        Mat rotMat = Imgproc.getRotationMatrix2D(center, angle, 1.0);
        Mat rotated = new Mat();
        Imgproc.warpAffine(mat, rotated, rotMat, mat.size(), Imgproc.INTER_CUBIC, Core.BORDER_REPLICATE, new Scalar(255));
        return rotated;
    }

    private static Mat bufferedImageToMat(BufferedImage bi) {
        BufferedImage converted = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        converted.getGraphics().drawImage(bi, 0, 0, null);
        byte[] data = ((DataBufferByte) converted.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(converted.getHeight(), converted.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, data);
        return mat;
    }

    private static BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] b = new byte[bufferSize];
        mat.get(0, 0, b);
        BufferedImage img = new BufferedImage(mat.cols(), mat.rows(), type);
        img.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), b);
        return img;
    }
}
