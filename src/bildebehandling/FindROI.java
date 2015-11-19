/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bildebehandling;

import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Point2d;
import static org.opencv.core.Core.inRange;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

/**
 *
 * @author anders
 */
public class FindROI {

    private Mat foundContours;
    private List<Point> src_pnt;
    private List<Point> dst_pnt;
    private Mat image;
    private Mat HSVThreshold;
    private Mat closedImage;
    private Mat cannyLines;
    private Mat croppedImage;
    private Point2d center;
    private MatOfPoint2f approxCurve;
    private Mat output;
    private boolean drawContours = false;
    private boolean drawRect = false;
    private boolean drawCorners = false;
    private boolean showCroppedImage;

    public Mat getROI(
            Mat image, 
            Scalar lowerB, 
            Scalar higherB, 
            int size, 
            int sigma, 
            boolean drawContours,
            boolean drawRect, 
            boolean drawCorners, 
            boolean showCroppedImage) {
        this.drawContours = drawContours;
        this.showCroppedImage = showCroppedImage;
        this.drawRect = drawRect;
        this.drawCorners = drawCorners;
        this.image = new Mat();
        this.image = image;
        hsvThreshold(lowerB, higherB);
        closing(HSVThreshold, size, sigma);
        cannyLines(closedImage);
        findROI(cannyLines);
        return output;
    }

    private void hsvThreshold(Scalar lowerB, Scalar higherB) {
        HSVThreshold = new Mat();
        Mat matHSV = new Mat();
        Imgproc.cvtColor(image, matHSV, Imgproc.COLOR_BGR2HSV);
        inRange(matHSV, lowerB, higherB, HSVThreshold);
    }

    private void closing(Mat mat, int size, int sigma) {
        closedImage = new Mat();
        Mat openedImage = new Mat();
        Mat openStruct = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(0.4 * size, 0.2 * size));
        Imgproc.morphologyEx(mat, openedImage, Imgproc.MORPH_OPEN, openStruct);
        Mat closeStruct = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1.5 * size, size));
        Imgproc.morphologyEx(openedImage, closedImage, Imgproc.MORPH_CLOSE, closeStruct);
    }

    private void cannyLines(Mat mat) {
        cannyLines = new Mat();
        Imgproc.Canny(mat, cannyLines, 1, 255);
    }

    private void findROI(Mat cannyLines) {
        center = new Point2d();
        foundContours = new Mat();
        src_pnt = new ArrayList<>();
        dst_pnt = new ArrayList<>();
        approxCurve = new MatOfPoint2f();
        croppedImage = new Mat();

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Rect rect = new Rect();
        Imgproc.findContours(cannyLines, contours, foundContours, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        output = Mat.zeros(image.rows(), image.cols(), CvType.CV_32F);
        if (!contours.isEmpty()) {

            int largestContour = findLargesContour(contours);
            if (drawContours && !showCroppedImage) {
                Imgproc.drawContours(image, contours, largestContour, new Scalar(255, 0, 255), 1);
            }
            int contourSize = (int) contours.get(largestContour).total();
            MatOfPoint2f new_mat = new MatOfPoint2f(contours.get(largestContour).toArray());
            Imgproc.approxPolyDP(new_mat, approxCurve, contourSize * 0.3, true);
            rect = Imgproc.boundingRect(contours.get(largestContour));

            if (approxCurve.total() == 4) {

                getCorners();
                if (drawCorners && !showCroppedImage) {
                    drawCorners();
                }

                findCenter();
                sortCorners(src_pnt, center);
                Mat startM = Converters.vector_Point2f_to_Mat(src_pnt);

                dst_pnt.add(new Point(rect.x, rect.y));
                dst_pnt.add(new Point(rect.x + rect.width, rect.y));
                dst_pnt.add(new Point(rect.x + rect.width, rect.y + rect.height));
                dst_pnt.add(new Point(rect.x, rect.y + rect.height));
                Mat endM = Converters.vector_Point2f_to_Mat(dst_pnt);

                Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);
                Imgproc.warpPerspective(image, output, perspectiveTransform, output.size());                 
                    croppImage(rect);
                    
                    if(showCroppedImage)
                        output = croppedImage;

                    
            } else {
                output = image;
            }
        }
        if (drawRect && !showCroppedImage) {
            drawRectangle(rect);
        }

    }

    
    public void croppImage(Rect rect){
        croppedImage = new Mat(output, rect);
    }
    
    public void drawRectangle(Rect rect) {
        Imgproc.rectangle(output, new Point(rect.x, rect.y),
                new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 255), 1);
    }

    private void sortCorners(List<Point> corners, Point2d center) {
        List<Point> top = new ArrayList<>(2);
        List<Point> bot = new ArrayList<>(2);

        if ((int) corners.size() == 4) {

            for (int i = 0; i < corners.size(); i++) {
                if (corners.get(i).y < center.y) {
                    top.add(corners.get(i));
                } else {
                    bot.add(corners.get(i));
                }
            }
            Point tl = top.get(0).x > top.get(1).x ? top.get(1) : top.get(0);
            Point tr = top.get(0).x > top.get(1).x ? top.get(0) : top.get(1);
            Point bl = bot.get(0).x > bot.get(1).x ? bot.get(1) : bot.get(0);
            Point br = bot.get(0).x > bot.get(1).x ? bot.get(0) : bot.get(1);

            src_pnt.clear();
            src_pnt.add(tl);
            src_pnt.add(tr);
            src_pnt.add(br);
            src_pnt.add(bl);
        }
    }

    public void getCorners() {
        for (int i = 0; i < 4; i++) {
            double temp_double[] = approxCurve.get(i, 0);
            Point p = new Point(temp_double[0], temp_double[1]);
            src_pnt.add(p);
        }
    }

    public void findCenter() {
        for (Point src_pnt1 : src_pnt) {
            center.x = center.x + src_pnt1.x;
            center.y = center.y + src_pnt1.y;
        }
        center.x *= (1. / src_pnt.size());
        center.y *= (1. / src_pnt.size());
    }

    public Mat drawCorners() {
        Scalar[] color = new Scalar[4];
        color[0] = new Scalar(255, 0, 0);
        color[1] = new Scalar(0, 255, 0);
        color[2] = new Scalar(0, 0, 255);
        color[3] = new Scalar(0, 255, 255);
        for (int i = 0; i < 4; i++) {
            Point p = src_pnt.get(i);
            Imgproc.circle(image, new Point(p.x, p.y), 10, color[i], 5); //p1 is colored red
        }
        return image;
    }

    public int findLargesContour(List<MatOfPoint> contours) {
        double largest_area = 0;
        int largest_contour_index = 0;

        for (int x = 0; x < contours.size(); x++) {
            double a = Imgproc.contourArea(contours.get(x), false);  //  Find the area of contour
            if (a > largest_area) {
                largest_area = a;
                largest_contour_index = x; //Store the index of largest contour
            }
        }
        return largest_contour_index;
    }

    public Mat getClosedImage() {
        return closedImage;
    }

    public Mat getHsvMat() {
        return HSVThreshold;
    }

    public Mat getCannyMat() {
        return cannyLines;
    }
    
    public Mat getCroppedImage(){
        return croppedImage;
    }
}
