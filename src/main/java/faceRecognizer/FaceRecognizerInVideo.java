package faceRecognizer;

import java.io.File;
import java.io.FilenameFilter;
import java.math.RoundingMode;
import java.nio.IntBuffer;
import java.text.DecimalFormat;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.miginfocom.swing.MigLayout;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_face.*;
import static org.bytedeco.javacpp.opencv_highgui.*;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FrameGrabber;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgcodecs.imwrite;

public class FaceRecognizerInVideo {

    private static final String trainingDir = "/home/giannis/Downloads/dataFaces/PhotoData/";
    private static String nameToSave = "";

    private static class MyAL implements java.awt.event.ActionListener {

        //final String toVerificationDir = "/home/giannis/Downloads/javacv/src/UnverifiedPhotoData/";
        private Mat detectedFace;

        public Mat getDetectedFace() {
            return detectedFace;
        }

        public void setDetectedFace(Mat detectedFace) {
            this.detectedFace = detectedFace;
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            saveImage(trainingDir, nameToSave, detectedFace);
        }

        public static void saveImage(String path, String name, Mat face) {
            imwrite(path + name + "-" + System.nanoTime() + ".png", face);
            System.out.println("Photo saved:" + path + name);
        }
    }

    public static void main(String[] args) throws Exception {
        OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
        String trainingDir = "/home/giannis/Downloads/dataFaces/PhotoData/";
//        final String toVerificationDir = "/home/giannis/Downloads/javacv/src/UnverifiedPhotoData/";

        File root = new File(trainingDir);
        FilenameFilter imgFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                name = name.toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
            }
        };
        File[] imageFiles = root.listFiles(imgFilter);
        MatVector images = new MatVector(imageFiles.length);
        Mat labels = new Mat(imageFiles.length, 1, CV_32SC1);
        IntBuffer labelsBuf = labels.createBuffer();
int sumSide = 0;
        int counter = 0;
        for (File image : imageFiles) {
            Mat img = imread(image.getAbsolutePath(), CV_LOAD_IMAGE_GRAYSCALE);

            int label = Integer.parseInt(image.getName().split("-")[0]);
            images.put(counter, img);
            labelsBuf.put(counter, label);
            counter++;
        }

        FaceRecognizer faceRecognizer = createLBPHFaceRecognizer();
        faceRecognizer.train(images, labels);

        FrameGrabber grabber = new OpenCVFrameGrabber("");

        try {
            grabber.start();
        } catch (java.lang.Exception e) {
            e.printStackTrace();
        }
        CascadeClassifier frontalFaceCascade = new CascadeClassifier(
                "/home/giannis/OpenCV3.2/OPEN_CV/opencv/data/haarcascades/haarcascade_frontalface_alt2.xml");
        CascadeClassifier profileFaceCascade = new CascadeClassifier(
                "/home/giannis/OpenCV3.2/OPEN_CV/opencv/data/haarcascades_cuda/haarcascade_profileface.xml");

        Frame videoFrame = null;
        Mat videoMat = new Mat();

        //webcam canvas
        CanvasFrame canvas = new CanvasFrame("Webcam");
        canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        canvas.setSize(1000, 800);

        canvas.setLayout(new MigLayout("insets 0, gap 10, fill"));

        ///textField 
        final JTextField jTextField = new javax.swing.JTextField(nameToSave, 10);
        jTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                nameToSave = jTextField.getText();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                nameToSave = jTextField.getText();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                nameToSave = jTextField.getText();
            }
        });
        canvas.add(jTextField);

        ///button
        final JButton jButton = new javax.swing.JButton();
        jButton.setText("Take Picure");
        jButton.setEnabled(true);
        MyAL saveButtonListener = new MyAL();
        jButton.addActionListener(saveButtonListener);
        canvas.add(jButton, "span");

        while (true) {
            videoFrame = grabber.grab();
            videoMat = converterToMat.convert(videoFrame);
            Mat videoMatGray = new Mat();
            // Convert the current frame to grayscale:
            cvtColor(videoMat, videoMatGray, COLOR_BGRA2GRAY);
            equalizeHist(videoMatGray, videoMatGray);
            RectVector detectedFaces = new RectVector();
            RectVector detectedFacesProf = new RectVector();
            // Find the faces in the frame:
            frontalFaceCascade.detectMultiScale(videoMatGray, detectedFaces);
            profileFaceCascade.detectMultiScale(videoMatGray, detectedFacesProf);

            for (int i = 0; i < detectedFaces.size(); i++) {
                Rect detectedFaceBorders = detectedFaces.get(i);
                final Mat detectedFace = new Mat(videoMatGray, detectedFaceBorders);
                saveButtonListener.setDetectedFace(detectedFace);

                int n[] = new int[1];
                double p[] = new double[1];
                faceRecognizer.predict(detectedFace.getUMat(USAGE_DEFAULT), n, p);
                int prediction = n[0];
                double bound = 65.0;

//                Double dble = p[0];
                DecimalFormat df = new DecimalFormat(".00");
                System.out.println(df.format(p[0]));

                rectangle(videoMat, detectedFaceBorders, new Scalar(0, 255, 0, 1));
                String box_text = "Predicted Person = " + (p[0] > bound ? "unknown " + prediction : prediction);
                int pos_x = Math.max(detectedFaceBorders.tl().x() - 10, 0);
                int pos_y = Math.max(detectedFaceBorders.tl().y() - 10, 0);
                putText(videoMat, box_text, new Point(pos_x, pos_y),
                        FONT_HERSHEY_PLAIN, 1.0, new Scalar(255, 0, 0, 2.0));
                pos_x = Math.max(detectedFaceBorders.tl().x() - 25, 0);
                pos_y = Math.max(detectedFaceBorders.tl().y() - 25, 0);
                putText(videoMat, "distance " + df.format(p[0]), new Point(pos_x, pos_y),
                        FONT_HERSHEY_PLAIN, 1.0, new Scalar(0, 0, 255, 2.0));
                if (p[0] > bound) {
                    String dir = "/home/giannis/Downloads/dataFaces/UnverifiedPhotoData/" + prediction + "/";
                    File directory = new File(String.valueOf(dir));
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    MyAL.saveImage(dir, "" + prediction, detectedFace);
                }
            }
            for (int i = 0; i < detectedFacesProf.size(); i++) {
                Rect detectedFaceBorders = detectedFacesProf.get(i);
                final Mat detectedFace = new Mat(videoMatGray, detectedFaceBorders);
                saveButtonListener.setDetectedFace(detectedFace);

                if (detectedFacesProf.size() > 0) {
                sumSide = sumSide + 1;
                System.out.println("Side faces :" + sumSide);
                }
                int n[] = new int[1];
                double p[] = new double[1];
                faceRecognizer.predict(detectedFace.getUMat(USAGE_DEFAULT), n, p);
                int prediction = n[0];
                double bound = 65.0;

//                Double dble = p[0];
                DecimalFormat df = new DecimalFormat(".00");
                System.out.println(df.format(p[0]));

                rectangle(videoMat, detectedFaceBorders, new Scalar(255, 0, 255, 0));
                String box_text = "Predicted Person = " + (p[0] > bound ? "unknown " + prediction : prediction);
                int pos_x = Math.max(detectedFaceBorders.tl().x() - 10, 0);
                int pos_y = Math.max(detectedFaceBorders.tl().y() - 10, 0);
                putText(videoMat, box_text, new Point(pos_x, pos_y),
                        FONT_HERSHEY_PLAIN, 1.0, new Scalar(255, 0, 0, 2.0));
                pos_x = Math.max(detectedFaceBorders.tl().x() - 25, 0);
                pos_y = Math.max(detectedFaceBorders.tl().y() - 25, 0);
                putText(videoMat, "distance " + df.format(p[0]), new Point(pos_x, pos_y),
                        FONT_HERSHEY_PLAIN, 1.0, new Scalar(0, 0, 255, 2.0));
                if (p[0] > bound) {
                    String dir = "/home/giannis/Downloads/dataFaces/UnverifiedPhotoData/" + prediction + "/";
                    File directory = new File(String.valueOf(dir));
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    MyAL.saveImage(dir, "" + prediction, detectedFace);
                }
            }
            canvas.showImage(videoFrame);
            char key = (char) waitKey(20);
            // Exit this loop on escape:
            if (key == 27) {
                destroyAllWindows();
                break;
            }
        }

    }
}
