package com.zijun.tm1;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.os.Bundle;
import android.R.integer;
import android.R.string;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;

public class MainActivity extends Activity implements CvCameraViewListener2 {
	/*********************************************
	 * TAGS, Private Classs Variables
	 */
	private static final double THRESHOLD = 0.85;
	private static final String TAG_STRING = "MainActivity";
	private static final Scalar RECT_COLOR = new Scalar(0, 255, 0, 255);
//	private static final Scalar MIN_RECT_COLOR = new Scalar(0, 0, 255, 0);
	private static final int LAYER =6 ;
	private static final int org_tpl_row = 180;
	private static final double overlap = 0.3;
	private static final double RESIZE_FACTOR=0.7;

	private static final int Expected_width = 640 ;
	private static final int Expected_height = 360; 

	// Takes time to look at this ! 640.360 1920 1280

	private CameraBridgeViewBase mOpenCvCameraView;
	private Mat mRgba;
	private Mat mGray;

	private Mat[] mat_tempalte_pyramid;

	// Variables that you may need for calculation
	// private org.opencv.core.Size currentSize;
	// private double[] resultScore;
	private Mat Mat_result;

	private class singleRect implements Comparable<singleRect> {
		public int x1;
		public int x2;
		public int y1;
		public int y2;
		public double score;

		public singleRect() {
			// TODO Auto-generated constructor stub
		}

		public singleRect(org.opencv.core.Point maxPoint, double maxValue,
				org.opencv.core.Size tmplSize) {

			score = maxValue;
			x1 = (int) maxPoint.x;
			x2 = (int) (maxPoint.x + tmplSize.width);
			y1 = (int) maxPoint.y;
			y2 = (int) (maxPoint.y + tmplSize.height);

		}

		// set return to 1 if less than may lead to a descending order?
		@Override
		public int compareTo(singleRect another) {
			// TODO Auto-generated method stub
			if (this.score > another.score) {
				return 1;
			} else {
				return -1;
			}
		}

	}

	private List<singleRect> candiList; // new ArrayList<MainActivity.rects>();
										// // stores the candidate ones

	
	private void GetTPLPyramid_byResize(Mat[]tplPyramid,int tplDrawable){
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		 Bitmap bitmap_template = BitmapFactory.decodeResource(getResources(),
				tplDrawable, options);
		 Mat mat_template_arbSize = new Mat();
		 Mat mat_tpl_gray = new Mat();
		 Mat mat_tpl_rgb = new Mat();

		Utils.bitmapToMat(bitmap_template, mat_template_arbSize);
		Imgproc.resize(
				mat_template_arbSize,
				mat_tpl_rgb,
				new org.opencv.core.Size((double) (mat_template_arbSize
						.cols() * org_tpl_row / mat_template_arbSize
						.rows()), (double) org_tpl_row));

		mat_template_arbSize.release();


		Imgproc.cvtColor(mat_tpl_rgb, mat_tpl_gray,
				Imgproc.COLOR_RGBA2GRAY);

		/***** Trun to Binary then trun back to gray *****/
		Imgproc.threshold(mat_tpl_gray, mat_tpl_gray, 20,
				255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

		tplPyramid[0] = new Mat();
		tplPyramid[0] = mat_tpl_gray.clone();

		mat_tpl_gray.release();
		mat_tpl_rgb.release();

		for (int i = 1; i < LAYER; i++) {
			tplPyramid[i] = new Mat(); // ATTENTION??? Single
													// Initializaiton
//			Imgproc.pyrDown(tplPyramid[i - 1],
//					tplPyramid[i]);
			Imgproc.resize(tplPyramid[i-1], tplPyramid[i], new org.opencv.core.Size(), RESIZE_FACTOR, RESIZE_FACTOR, Imgproc.INTER_AREA);
		}
		//tplPyramid_convey=tplPyramid.clone();
		
		
	}
	
	private  void GetTPLPyramid(Mat[] tplPyramid, int tplDrawable){
		
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		 Bitmap bitmap_template = BitmapFactory.decodeResource(getResources(),
				tplDrawable, options);
		 Mat mat_template_arbSize = new Mat();
		 Mat mat_tpl_gray = new Mat();
		 Mat mat_tpl_rgb = new Mat();

		Utils.bitmapToMat(bitmap_template, mat_template_arbSize);
		Imgproc.resize(
				mat_template_arbSize,
				mat_tpl_rgb,
				new org.opencv.core.Size((double) (mat_template_arbSize
						.cols() * org_tpl_row / mat_template_arbSize
						.rows()), (double) org_tpl_row));
//		Log.d(TAG_STRING, "template cols:" + mat_template.cols()
//				+ "  rows:" + mat_template.rows());
		mat_template_arbSize.release();

//		Log.d(TAG_STRING, "Size: cols:" + mat_template.cols()
//				+ " rows:" + mat_template.rows() + " Bitmap: cols"
//				+ bitmap_template.getWidth() + " rows:"
//				+ bitmap_template.getHeight());

		Imgproc.cvtColor(mat_tpl_rgb, mat_tpl_gray,
				Imgproc.COLOR_RGBA2GRAY);

		/***** Trun to Binary then trun back to gray *****/
		Imgproc.threshold(mat_tpl_gray, mat_tpl_gray, 20,
				255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);


//		int TestRow = (int) (mat_gray_template.rows() / 2);
//		for (int i = 0; i < mat_gray_template.cols(); i++) {
//			Log.d("GRAY",
//					i + "  color:"
//							+ (mat_gray_template.get(TestRow, i))[0]);
//
//		}
		//tplPyramid = new Mat[LAYER];
		 // tplPyramid=new Mat[LAYER];
		tplPyramid[0] = new Mat();
		tplPyramid[0] = mat_tpl_gray.clone();

		mat_tpl_gray.release();
		mat_tpl_rgb.release();

		for (int i = 1; i < LAYER; i++) {
			tplPyramid[i] = new Mat(); // ATTENTION??? Single
													// Initializaiton
		//	Imgproc.pyDown(tplPyramid[i - 1],
		//			tplPyramid[i]);}
			org.opencv.core.Size dstSize=new org.opencv.core.Size(tplPyramid[i].cols()*RESIZE_FACTOR, tplPyramid[i].rows()*RESIZE_FACTOR);
			Imgproc.resize(tplPyramid[i-1], tplPyramid[i],dstSize, RESIZE_FACTOR, RESIZE_FACTOR, Imgproc.INTER_AREA);}
			
		//tplPyramid_convey=tplPyramid.clone();
		
	}
	
	private void concat(Mat[]target, Mat[] aMats, Mat[] bMats){
		int aLen=aMats.length;
		int bLen=bMats.length;
		
		System.arraycopy(aMats, 0, target, 0, aLen);
		System.arraycopy(bMats, 0, target, aLen, bLen);
		//target=C.clone();
	}
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG_STRING, "OpenCV loaded successfully");

                mat_tempalte_pyramid=new Mat[LAYER];
                GetTPLPyramid_byResize(mat_tempalte_pyramid, R.drawable.capture3);
                //GetTPLPyramid(mat_tempalte_pyramid, R.drawable.capture3);      
                Log.d(TAG_STRING+"Length","Length:"+mat_tempalte_pyramid.length);
				mOpenCvCameraView.enableView();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		// TODO Auto-generated method stub
		mRgba = inputFrame.rgba();
		Log.d(TAG_STRING, "col: " + mRgba.cols() + "   rows:  " + mRgba.rows());
		mGray = inputFrame.gray();
		double ratio1_col_width = (double) (mRgba.cols() / Expected_width);
		double ratio2_row_height = (double) (mRgba.rows() / Expected_height);
		Log.d(TAG_STRING, "width compression rate: " + ratio1_col_width
				+ "  height compression rate: " + ratio2_row_height);
		// Imgproc.resize(mGray, mGray, new org.opencv.core.Size(mRgba.cols()
		// / ratio, mRgba.rows() / ratio));
		Imgproc.resize(mGray, mGray, new org.opencv.core.Size(Expected_width,
				Expected_height));
		Log.d(TAG_STRING, "col: " + mGray.cols() + "   rows:  " + mGray.rows());
//        Imgproc.Canny(mGray, mGray, 30, 90,3,false);
//		double previous_min = 1;
//		org.opencv.core.Point minPoint = new org.opencv.core.Point();
//		double minVal_width = 0;
//		double minVal_height = 0;
		candiList = new ArrayList<MainActivity.singleRect>();
      
           Log.d(TAG_STRING+"Length","Length:"+mat_tempalte_pyramid.length);
     
           double threshold_level=THRESHOLD;
		for (int i = 0; i < mat_tempalte_pyramid.length; i++) {

			int tpl_width_x_cols = mat_tempalte_pyramid[i].cols();
			int tpl_height_y_rows = mat_tempalte_pyramid[i].rows();
			Log.d("ATTENTION","cols "+tpl_width_x_cols+" rows: "+tpl_height_y_rows);
			Mat_result = new Mat(mGray.cols() - tpl_width_x_cols + 1,
					mGray.rows() - tpl_height_y_rows + 1, CvType.CV_32FC1); // Same
																			// as
																			// the
																			// web
			Imgproc.matchTemplate(mGray, mat_tempalte_pyramid[i], Mat_result,
					Imgproc.TM_CCORR_NORMED);
			// Mat_result.convertTo(Mat_result, CvType.CV_32FC1, alpha, beta)
			// Core.normalize(Mat_result, Mat_result, -1, 1, Core.NORM_MINMAX);
			// Test the value:
//			MinMaxLocResult testLocResult_before = Core.minMaxLoc(Mat_result);
//			double minimallValue = testLocResult_before.minVal;
//			double maximalValue = testLocResult_before.maxVal;
//			if (previous_min >= minimallValue) {
//				previous_min = minimallValue;
//				minVal_width = tpl_width_x_cols;
//				minVal_height = tpl_height_y_rows;
//				minPoint = testLocResult_before.minLoc;
//			}
//			Log.d(TAG_STRING, "before abs: min  " + minimallValue + " max: "
//					+ maximalValue);
			/*******************************************************
			 * 
			 * 
			 *********************************************************/

			// Core.absdiff(Mat_result, Mat.zeros(Mat_result.rows(),
			// Mat_result.cols(), CvType.CV_32FC1), Mat_result);
			// MinMaxLocResult testlocResult_after=Core.minMaxLoc(Mat_result);
			// minimallValue=testlocResult_after.minVal;
			// Log.d(TAG_STRING, "after abs:" +minimallValue);
			int counter_per_pyramid=0; //On every layer there should be less than 5 icon
			while (true) {
				MinMaxLocResult locResult = Core.minMaxLoc(Mat_result);
				Log.d("MAXVALUE","value is:  "+ locResult.maxVal);
				if ((locResult.maxVal > threshold_level+0.005*i) && counter_per_pyramid<5) {
					candiList.add(new singleRect(locResult.maxLoc,
							locResult.maxVal, mat_tempalte_pyramid[i].size()));
           
					Mat_result.put((int) (locResult.maxLoc.y),
							(int) (locResult.maxLoc.x), (double) 0);
					counter_per_pyramid=counter_per_pyramid+1;
				} else {
					break;
				}

			}
		}

		/********************************************************
		 * Draw the point of minValue
		 * 
		 ******************************************************/
//		org.opencv.core.Point minPoint2 = new org.opencv.core.Point(minPoint.x
//				+ minVal_width, minPoint.y + minVal_height);
//		Core.rectangle(mRgba, minPoint, minPoint2, MIN_RECT_COLOR, 3);

		/*******************
		 * NMS_Boxes Then DRAW:
		 ********************/
		Boolean[] SuppressList = new Boolean[candiList.size()];
		SuppressList = NMS_Boxes(candiList);
		// Draw Things
   
		 double Larea=getMaxArea(candiList);
		 
		 //Get the max size of current rects
		 
		 
		 
		Boolean[] maxIdx=new Boolean[candiList.size()];
		for(int i=0;i<candiList.size();i++){
			maxIdx[i]=false;
			if     (getArea(candiList.get(i))>(Larea-10)){
				maxIdx[i]=true;
			}
		}
		
		boolean displayOne=false;
		Log.d(TAG_STRING, "size of rect List  : " + candiList.size());
		// int num_showRects=Math.min(candiList.size(), 3);
		// if (candiList.isEmpty() == false) {
		for (int i = 0; i < candiList.size(); i++) {
//			if (displayOne==true) {
//				break;
//			}
			if (SuppressList[i] == false && maxIdx[i]==true) {
                displayOne=true;
				singleRect toShow_thenDeleteRect = new singleRect();
				toShow_thenDeleteRect = candiList.get(i);

				org.opencv.core.Point pt1 = new org.opencv.core.Point(
						(int) (ratio1_col_width * toShow_thenDeleteRect.x1),
						(int) (ratio2_row_height * toShow_thenDeleteRect.y1));
				org.opencv.core.Point pt2 = new org.opencv.core.Point(
						(int) (ratio1_col_width * toShow_thenDeleteRect.x2),
						(int) (ratio2_row_height * toShow_thenDeleteRect.y2));
				Core.rectangle(mRgba, pt1, pt2, RECT_COLOR, 3);
				
                 Viberate(pt1, pt2,(ratio1_col_width));
				
			} else {
				continue;
			}
		}

		mGray.release();

		return mRgba;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
		mOpenCvCameraView.setCvCameraViewListener(this);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5, this,
				mLoaderCallback);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		// TODO Auto-generated method stub
		mGray = new Mat();
		mRgba = new Mat();
	}

	@Override
	public void onCameraViewStopped() {
		// TODO Auto-generated method stub
		mGray.release();
		mRgba.release();
	}

	// Sort List and do non maxima suppression
	protected Boolean[] NMS_Boxes(List<singleRect> rectList) {
		Collections.sort(rectList);
		Collections.reverse(rectList);
		// if the list is larger than a size:
		if (rectList.size() > 30) {
			for (int i = rectList.size() - 1; i >= 30; i--) {
				rectList.remove(i);
			}
		}
		Boolean[] suppressList = new Boolean[15];
		for (int i = 0; i < 15; i++) {
			suppressList[i] = false;
		}
		// Formally start NMS
		for (int i = 0; i < rectList.size() - 1; i++) {
			for (int j = rectList.size() - 1; j > i; j--) {

				if ((getInterArea(rectList.get(i), rectList.get(j))
						/ getIntraArea(rectList.get(i), rectList.get(j)) > overlap)) {
					suppressList[j] = true;
				}

			}

		}

		return suppressList;
	}

	protected double getInterArea(singleRect aRect, singleRect bRect) {

		int inter_x1 = Math.max(aRect.x1, bRect.x1);
		int inter_y1 = Math.max(aRect.y1, bRect.y1);
		int inter_x2 = Math.min(aRect.x2, bRect.x2);
		int inter_y2 = Math.min(aRect.y2, bRect.y2);

		if (inter_y2 - inter_y1 < 0 || inter_x2 - inter_x1 < 0) {
			return 0;
		} else {
			return (inter_y2 - inter_y1) * (inter_x2 - inter_x1);
		}
	}

	protected double getIntraArea(singleRect aRect, singleRect bRect) {

		int inter_x1 = Math.min(aRect.x1, bRect.x2);
		int inter_y1 = Math.min(aRect.y1, bRect.y2);
		int inter_x2 = Math.max(aRect.x2, bRect.x2);
		int inter_y2 = Math.max(aRect.y2, bRect.y2);

		return (inter_y2 - inter_y1) * (inter_x2 - inter_x1);
	}
	private double getArea(singleRect aRect)
	{
		return Math.abs((aRect.x1-aRect.x2)*(aRect.y1-aRect.y2));
	}

	protected void Viberate(org.opencv.core.Point pt1, org.opencv.core.Point pt2,double ratio) {

		if ((pt1.x + pt2.x) < (1.1 * mGray.cols()*ratio)
				&& (pt1.x + pt2.x) > (0.9 * mGray.cols())*ratio) {
			Intent intentVibrate = new Intent(getApplicationContext(),
					VibrateService.class);
			startService(intentVibrate);
		}
	}
	private double getMaxArea(List<singleRect> list){
		double maxArea=0;
		
		for(int i=0;i<list.size();i++){
			if (maxArea<getArea(list.get(i))){
				maxArea=getArea(list.get(i));
			}
		}
		return maxArea;
	}
	
	

}

/********************************************************************************
 * 
 * 
 * Code you may need later!
 ***********************************************************************************/

//
// Mat subMat_toClean = new Mat();
// subMat_toClean = Mat_result
// .colRange(
// Math.max(1, (int) (locResult.maxLoc.x)),
// Math.min(
// (int) (locResult.maxLoc.x + mat_tempalte_pyramid[i]
// .size().width),
// Expected_width-1))
// .rowRange(
// Math.max((int) (locResult.maxLoc.y),1),
// Math.min(
// (int) (locResult.maxLoc.y + mat_tempalte_pyramid[i]
// .size().height),
// Expected_height-1));
// Mat zeroMat = new Mat();
// zeroMat = Mat.zeros(mat_tempalte_pyramid[i].height(),
// mat_tempalte_pyramid[i].width(), CvType.CV_32 FC1);
// zeroMat = Mat.zeros(subMat_toClean.rows(),
// subMat_toClean.cols(), CvType.CV_32FC1);
// zeroMat.copyTo(subMat_toClean);
// zeroMat.release();



//Moved from the old template matching!
//bitmap_template = BitmapFactory.decodeResource(getResources(),
//R.drawable.capture, options);
//Mat mat_template_arbSize = new Mat();
//mat_gray_template = new Mat();
//mat_template = new Mat();
//
//Utils.bitmapToMat(bitmap_template, mat_template_arbSize);
//Imgproc.resize(
//mat_template_arbSize,
//mat_template,
//new org.opencv.core.Size((double) (mat_template_arbSize
//		.cols() * org_tpl_row / mat_template_arbSize
//		.rows()), (double) org_tpl_row));
//Log.d(TAG_STRING, "template cols:" + mat_template.cols()
//+ "  rows:" + mat_template.rows());
//mat_template_arbSize.release();
//
//Log.d(TAG_STRING, "Size: cols:" + mat_template.cols()
//+ " rows:" + mat_template.rows() + " Bitmap: cols"
//+ bitmap_template.getWidth() + " rows:"
//+ bitmap_template.getHeight());
//
//Imgproc.cvtColor(mat_template, mat_gray_template,
//Imgproc.COLOR_RGBA2GRAY);
//
///***** Trun to Binary then trun back to gray *****/
//Imgproc.threshold(mat_gray_template, mat_gray_template, 20,
//255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
////
//// // Imgproc.cvtColor(mat_gray_template, mat_gray_template,
////
//// //double DatainGray;
//// stop debugging
//int TestRow = (int) (mat_gray_template.rows() / 2);
//for (int i = 0; i < mat_gray_template.cols(); i++) {
//Log.d("GRAY",
//	i + "  color:"
//			+ (mat_gray_template.get(TestRow, i))[0]);
//// Log.d("GRAY", i+"  color:"+(mat_gray_template.get(
//// TestRow, i))[1]);
//// Log.d("GRAY", i+"  color:"+(mat_gray_template.get(
//// TestRow, i))[2]);
//}
//mat_tempalte_pyramid = new Mat[LAYER];
//mat_tempalte_pyramid[0] = new Mat();
//mat_tempalte_pyramid[0] = mat_gray_template.clone();
//
//mat_gray_template.release();
//
//for (int i = 1; i < LAYER; i++) {
//mat_tempalte_pyramid[i] = new Mat(); // ATTENTION??? Single
//									// Initializaiton
//Imgproc.pyrDown(mat_tempalte_pyramid[i - 1],
//	mat_tempalte_pyramid[i]);

//}