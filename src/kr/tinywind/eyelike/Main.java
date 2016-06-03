package kr.tinywind.eyelike;

import org.opencv.core.*;
import org.opencv.highgui.VideoCapture;
import org.opencv.objdetect.CascadeClassifier;

import java.io.IOException;
import java.util.Vector;

import static kr.tinywind.eyelike.Constants.*;
import static org.opencv.core.Core.*;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.highgui.Highgui.imwrite;
import static org.opencv.imgproc.Imgproc.GaussianBlur;

public class Main {
    static {
//        System.load(Core.NATIVE_LIBRARY_NAME);
//        System.load("opencv_java249");
        System.load("C:/Users/tinywind/IdeaProjects/eyeLike/lib/x64/opencv_java249.dll");
    }

    private CascadeClassifier face_cascade = new CascadeClassifier();
    private String face_window_name = "Capture - Face";
    private Mat debugImage = new Mat();
    private Mat skinCrCbHist = Mat.zeros(new Size(256, 256), CV_8UC1);
    private FindEyeCenter eyeCenter;
    private FindEyeCorner eyeCorner;
//    private static RNG rng(12345);

    public static void main(String[] args) throws IOException {
        new Main().play();
    }

    private void play() throws IOException {
        Mat frame = new Mat();
        String face_cascade_name = "C:/Users/tinywind/IdeaProjects/eyeLike/resources/haarcascade_frontalface_alt.xml";
        if (!face_cascade.load(face_cascade_name)) {
            System.out.println("--(!)Error loading face cascade, please change face_cascade_name in source code.");
            return;
        }

        String main_window_name = "Capture - Face detection";
//        namedWindow(main_window_name, CV_WINDOW_NORMAL);
//        moveWindow(main_window_name, 400, 100);
//        namedWindow(face_window_name, CV_WINDOW_NORMAL);
//        moveWindow(face_window_name, 10, 100);
//        namedWindow("Right Eye", CV_WINDOW_NORMAL);
//        moveWindow("Right Eye", 10, 600);
//        namedWindow("Left Eye", CV_WINDOW_NORMAL);
//        moveWindow("Left Eye", 10, 800);
//        namedWindow("aa", CV_WINDOW_NORMAL);
//        moveWindow("aa", 10, 800);
//        namedWindow("aaa", CV_WINDOW_NORMAL);
//        moveWindow("aaa", 10, 800);

        ellipse(skinCrCbHist, new Point(113, 155.6), new Size(23.4, 15.2), 43.0, 0.0, 360.0, new Scalar(255, 255, 255), -1);

        VideoCapture capture = new VideoCapture(-1);
        if (capture.isOpened()) {
            while (true) {
                capture.read(frame);
                // mirror it
                flip(frame, frame, 1);
                frame.copyTo(debugImage);

                // Apply the classifier to the frame
                if (!frame.empty()) {
                    detectAndDisplay(frame);
                } else {
                    System.out.println(" --(!) No captured frame -- Break!");
                    break;
                }

                new Imshow(main_window_name).showImage(debugImage);

                int c = System.in.read();
                if ((char) c == 'c') break;
                if ((char) c == 'f') imwrite("frame.png", frame);
            }
        }
    }

    private void findEyes(Mat frame_gray, Rect face) {
        Mat faceROI = new Mat(frame_gray, face);
        Mat debugFace = faceROI.clone();

        if (kSmoothFaceImage) {
            double sigma = kSmoothFaceFactor * face.width;
            GaussianBlur(faceROI, faceROI, new Size(0, 0), sigma);
        }
        //-- Find eye regions and draw them
        int eye_region_width = (int) (face.width * (kEyePercentWidth / 100.0));
        int eye_region_height = (int) (face.width * (kEyePercentHeight / 100.0));
        int eye_region_top = (int) (face.height * (kEyePercentTop / 100.0));
        Rect leftEyeRegion = new Rect((int) (face.width * (kEyePercentSide / 100.0)), eye_region_top, eye_region_width, eye_region_height);
        Rect rightEyeRegion = new Rect((int) (face.width - eye_region_width - face.width * (kEyePercentSide / 100.0)), eye_region_top, eye_region_width, eye_region_height);

        //-- Find Eye Centers
        Point leftPupil = eyeCenter.findEyeCenter(faceROI, leftEyeRegion, "Left Eye");
        Point rightPupil = eyeCenter.findEyeCenter(faceROI, rightEyeRegion, "Right Eye");
        // get corner regions
        Rect leftRightCornerRegion = leftEyeRegion.clone();
        leftRightCornerRegion.width -= leftPupil.x;
        leftRightCornerRegion.x += leftPupil.x;
        leftRightCornerRegion.height /= 2;
        leftRightCornerRegion.y += leftRightCornerRegion.height / 2;
        Rect leftLeftCornerRegion = leftEyeRegion.clone();
        leftLeftCornerRegion.width = (int) leftPupil.x;
        leftLeftCornerRegion.height /= 2;
        leftLeftCornerRegion.y += leftLeftCornerRegion.height / 2;
        Rect rightLeftCornerRegion = rightEyeRegion.clone();
        rightLeftCornerRegion.width = (int) rightPupil.x;
        rightLeftCornerRegion.height /= 2;
        rightLeftCornerRegion.y += rightLeftCornerRegion.height / 2;
        Rect rightRightCornerRegion = rightEyeRegion.clone();
        rightRightCornerRegion.width -= rightPupil.x;
        rightRightCornerRegion.x += rightPupil.x;
        rightRightCornerRegion.height /= 2;
        rightRightCornerRegion.y += rightRightCornerRegion.height / 2;
        rectangle(debugFace, leftRightCornerRegion.tl(), leftRightCornerRegion.br(), new Scalar(200));
        rectangle(debugFace, leftLeftCornerRegion.tl(), leftLeftCornerRegion.br(), new Scalar(200));
        rectangle(debugFace, rightLeftCornerRegion.tl(), rightLeftCornerRegion.br(), new Scalar(200));
        rectangle(debugFace, rightRightCornerRegion.tl(), rightRightCornerRegion.br(), new Scalar(200));
        // change eye centers to face coordinates
        rightPupil.x += rightEyeRegion.x;
        rightPupil.y += rightEyeRegion.y;
        leftPupil.x += leftEyeRegion.x;
        leftPupil.y += leftEyeRegion.y;
        // draw eye centers
        circle(debugFace, rightPupil, 3, new Scalar(1234));
        circle(debugFace, leftPupil, 3, new Scalar(1234));

        //-- Find Eye Corners
        if (kEnableEyeCorner) {
            Point leftRightCorner = eyeCorner.findEyeCorner(new Mat(faceROI, leftRightCornerRegion), true, false);
            leftRightCorner.x += leftRightCornerRegion.x;
            leftRightCorner.y += leftRightCornerRegion.y;
            Point leftLeftCorner = eyeCorner.findEyeCorner(new Mat(faceROI, leftLeftCornerRegion), true, true);
            leftLeftCorner.x += leftLeftCornerRegion.x;
            leftLeftCorner.y += leftLeftCornerRegion.y;
            Point rightLeftCorner = eyeCorner.findEyeCorner(new Mat(faceROI, rightLeftCornerRegion), false, true);
            rightLeftCorner.x += rightLeftCornerRegion.x;
            rightLeftCorner.y += rightLeftCornerRegion.y;
            Point rightRightCorner = eyeCorner.findEyeCorner(new Mat(faceROI, rightRightCornerRegion), false, false);
            rightRightCorner.x += rightRightCornerRegion.x;
            rightRightCorner.y += rightRightCornerRegion.y;
            circle(faceROI, leftRightCorner, 3, new Scalar(200));
            circle(faceROI, leftLeftCorner, 3, new Scalar(200));
            circle(faceROI, rightLeftCorner, 3, new Scalar(200));
            circle(faceROI, rightRightCorner, 3, new Scalar(200));
        }

        new Imshow(face_window_name).showImage(faceROI);
    }

    private void detectAndDisplay(Mat frame) {
        Vector<Rect> faces = new Vector<>();
        Vector<Mat> rgbChannels = new Vector<>(3);
        split(frame, rgbChannels);
        Mat frame_gray = rgbChannels.get(2).clone();

//        face_cascade.detectMultiScale(frame_gray, faces, 1.1, 2, 0 | CV_HAAR_SCALE_IMAGE | CV_HAAR_FIND_BIGGEST_OBJECT, new Size(150, 150));

        for (int i = 0; i < faces.size(); i++) {
            rectangle(debugImage, faces.get(i).tl(), faces.get(i).br(), new Scalar(1234));
        }
        if (faces.size() > 0) {
            findEyes(frame_gray, faces.get(0));
        }
    }
}