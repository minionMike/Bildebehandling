package bildebehandling;

import static java.lang.Math.sqrt;
import java.util.ArrayList;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import java.awt.Color;
import java.awt.Font;
import static java.nio.file.Files.size;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;
import static org.opencv.core.Core.inRange;
import org.opencv.core.Scalar;
import org.opencv.core.Size;


/**
 *
 * @author Morten
 */
public class ImageProcessing {
    /**
     * Fields
     */
    
    private Mat lastImage = new Mat();
    private double[] hData;
    private double[] vData;
    private double hAvg = 0;
    /**
     * Constructor
     */
    public void ImageProcessing() {

    }

    /**
     * Non adaptive threshold method
     *
     * @param mat
     * @param thresholdvalue
     * @return
     */
    public Mat Threshold(Mat mat, int thresholdvalue) {
        Mat thresh = new Mat();
        Imgproc.threshold(mat, thresh, thresholdvalue, 255, Imgproc.THRESH_BINARY);
        return thresh;

    }


    /**
     * Adaptive Threshold
     *
     * @param mat
     * @return
     */
    public void adaptThresholdGaussian() {
        Imgproc.cvtColor(lastImage, lastImage, Imgproc.COLOR_BGR2GRAY);
        Imgproc.adaptiveThreshold(lastImage, lastImage, 1, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 10, -1); //funker ikke
    }
    public void gaussFilter(){
        Imgproc.GaussianBlur(lastImage, lastImage, new Size(15,15), 3);
    }

    /**
     *
     * @param binaryImage
     */
    public void VerticalCovered() {
        double[] data = new double[lastImage.width()];
        double sum = 0;
        int c;
        int r;

        for (c = 0; c < lastImage.width(); c++) {
            sum = 0;
            for (r = 0; r < lastImage.height(); r++) {
                sum += lastImage.get(r, c)[0] / 255;
            }
            if (lastImage.height() > 0) {
                data[c] = sum / lastImage.height();
            } else {
                data[c] = 0;
            }
        }
        vData = data;
    }

    /**
     *
     * @param binaryImage
     */
    public void HorizontalCovered() {
        double[] data = new double[lastImage.height()];
        double sum = 0;
        int c;
        int r;

        for (r = 0; r < lastImage.height(); r++) {
            sum = 0;
            for (c = 0; c < lastImage.width(); c++) {
                sum += lastImage.get(r, c)[0] / 255;
            }
            if (lastImage.width() > 0) {
                data[r] = sum / lastImage.width();
            } else {
                data[r] = 0;
            }
        }
        hData = data;
    }
    public void updateTotalAverageCoverage(){
        double hsum = 0;
        
        for(int i = 0; i < hData.length; i++){
            hsum += hData[i];
        }
        hAvg = (hsum/hData.length)*100;
    }

    /**
     *
     * @param binaryImage
     * @return ArrayList<Double>
     */
    public ArrayList StandardDeviationHorizontal(Mat binaryImage) {
        ArrayList<Double> STDDeviation = new ArrayList<>();
        double[] data;
        int c;
        int r;

        for (r = 0; r < binaryImage.height(); r++) {
            double sum = 0;
            double xsum = 0;
            ArrayList<Double> list = new ArrayList<>();

            for (c = 0; c < binaryImage.width(); c++) {

                data = binaryImage.get(r, c);
                sum = sum + data[0];
                list.add(data[0]);
            }

            for (int i = 0; i < list.size(); i++) {
                xsum = xsum + (list.get(i) - (sum / binaryImage.width()))
                        * (list.get(i) - (sum / binaryImage.width()));

            }
            STDDeviation.add(sqrt(xsum / sum));

        }
        System.out.println(STDDeviation.size());
        return STDDeviation;
    }

    /**
     *
     * @param binaryImage
     * @return ArrayList<Double>
     */
    public ArrayList StandardDeviationVertical(Mat binaryImage) {
        ArrayList<Double> STDDeviation = new ArrayList<>();
        double[] data;
        int c;
        int r;

        for (c = 0; c < binaryImage.width(); c++) {
            double sum = 0;
            ArrayList<Double> list = new ArrayList<>();
            for (r = 0; r < binaryImage.height(); r++) {

                data = binaryImage.get(r, c);
                sum = sum + data[0];
                list.add(data[0]);
            }
            double x = 0;
            for (int i = 0; i < list.size(); i++) {
                x = x + (list.get(i) - (sum / binaryImage.height()))
                        * (list.get(i) - (sum / binaryImage.height()));

            }

            STDDeviation.add(sqrt(x / sum));

        }
        System.out.println(STDDeviation.size());
        return STDDeviation;
    }
    /**
     * 
     * @param lowerB
     * @param higherB 
     */

    public void hsvThreshold(Scalar lowerB, Scalar higherB) {
        Mat matHSV = new Mat();
        Imgproc.cvtColor(lastImage, matHSV, Imgproc.COLOR_BGR2HSV);
        inRange(lastImage, lowerB, higherB, lastImage);
    }

    public IntervalXYDataset getDatasetHorizontal() {
        final XYSeries series = new XYSeries("Vertical measurement");
        double[] data = hData;
        if (data.length > 0) {
            for (int i = 0; i < data.length; i++) {
                series.add(i, data[i]);
            }
        }
        else series.add(0,0);

        final XYSeriesCollection dataset = new XYSeriesCollection(series);
        return dataset;
    }

    public IntervalXYDataset getDatasetVertical() {
        final XYSeries series = new XYSeries("Vertical measurement");
        double[] data = vData;
        if (data.length > 0) {
            for (int i = 0; i < data.length; i++) {
                series.add(i, data[i]);
            }
        }
        else series.add(0,0);

        final XYSeriesCollection dataset = new XYSeriesCollection(series);
        return dataset;
    }

    public JFreeChart createChart(IntervalXYDataset dataset, String title) {
        final JFreeChart chart = ChartFactory.createXYBarChart(
                title,
                "X",
                false,
                "Y",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        XYPlot plot = (XYPlot) chart.getPlot();
        final IntervalMarker target = new IntervalMarker(400.0, 700.0);
        target.setLabel("Target Range");
        target.setLabelFont(new Font("SansSerif", Font.ITALIC, 11));
        target.setLabelAnchor(RectangleAnchor.LEFT);
        target.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
        target.setPaint(new Color(222, 222, 255, 128));
        plot.addRangeMarker(target, Layer.BACKGROUND);
        return chart;
    }

    public void setLastImage(Mat lastImage) {
        this.lastImage = lastImage;
    }

    public Mat getLastImage() {
        return lastImage;
    }

    public double gethAvg() {
        return hAvg;
    }
    
}
