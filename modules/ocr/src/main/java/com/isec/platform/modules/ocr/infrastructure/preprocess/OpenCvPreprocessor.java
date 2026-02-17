package com.isec.platform.modules.ocr.infrastructure.preprocess;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

@Component
@Slf4j
public class OpenCvPreprocessor implements ImagePreprocessor {

    static {
        try {
            nu.pattern.OpenCV.loadShared();
            log.info("OpenCV loaded successfully via openpnp");
        } catch (Throwable e) {
            // Fallback to standard load
            try {
                System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
                log.info("OpenCV loaded via System.loadLibrary");
            } catch (Throwable e2) {
                log.warn("OpenCV native library not loaded: {}", e2.getMessage());
            }
        }
    }

    @Override
    public BufferedImage preprocess(BufferedImage input) {
        try {
            Mat mat = bufferedImageToMat(input);
            // Grayscale
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
            // Noise reduction
            Imgproc.medianBlur(mat, mat, 3);
            // Adaptive thresholding
            Imgproc.adaptiveThreshold(mat, mat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 15, 3);
            // Resize to normalize DPI roughly
            Imgproc.resize(mat, mat, new Size(mat.width() * 1.2, mat.height() * 1.2));
            // Contrast enhancement using histogram equalization
            Imgproc.equalizeHist(mat, mat);
            return matToBufferedImage(mat);
        } catch (Throwable t) {
            log.warn("Preprocessing failed or OpenCV not available, returning original image: {}", t.getMessage());
            return input;
        }
    }

    private static Mat bufferedImageToMat(BufferedImage bi) {
        BufferedImage converted = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        converted.getGraphics().drawImage(bi, 0, 0, null);
        byte[] data = ((DataBufferByte) converted.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(converted.getHeight(), converted.getWidth(), org.opencv.core.CvType.CV_8UC3);
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
