package edu.lipreading.vision;

//import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.swing.JPanel;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.FrameGrabber.Exception;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import edu.lipreading.Point;
import edu.lipreading.Utils;

public class ColoredStickersFeatureExtractor extends AbstractFeatureExtractor{
	private final static short NUM_OF_STICKERS = 4;

	private final static short X_VECTOR_INDEX = 0;
	private final static short Y_VECTOR_INDEX = 1;
	private final static short COUNT_VECTOR_INDEX = 2;

	private final static short RED_VECTOR_INDEX = 0;
	private final static short GREEN_VECTOR_INDEX = 1;
	private final static short BLUE_VECTOR_INDEX = 2;
	private final static short YELLOW_VECTOR_INDEX = 3;


	private final static short[] RED_MIN = {0, 0, 130};
	private final static short[] RED_MAX = {80, 80, 255};

	private final static short[] GREEN_MIN = {0, 40 ,0};
	private final static short[] GREEN_MAX = {50, 255, 50};

	private final static short[] BLUE_MIN = {40, 0, 0};
	private final static short[] BLUE_MAX = {255, 150, 150};

	private final static short[] YELLOW_MIN = {0, 120, 120};
	private final static short[] YELLOW_MAX = {160, 255, 255};

	private static int[][] summeries;
	private static int channels = 0;
	private static int cols = 0;

	@Override
	protected List<List<Point>> getPoints() throws Exception, InterruptedException {
		List<List<Point>> allColorsVector = new Vector<List<Point>>(); 
		for(int i = 0; i < NUM_OF_STICKERS; i++){
			allColorsVector.add(new Vector<Point>());
		}
		IplImage grabbed;
		CanvasFrame frame = null;
		CanvasFrame painter = new CanvasFrame("Stickers Detection");
		JPanel changingPanel = null;
		if(!Utils.isCI()){
			frame = new CanvasFrame("output", CanvasFrame.getDefaultGamma()/grabber.getGamma());
			frame.setDefaultCloseOperation(CanvasFrame.EXIT_ON_CLOSE);
			changingPanel = new JPanel();
			painter.setContentPane(changingPanel);
		}

		while((grabbed = grabber.grab()) != null){

			if(channels == 0)
				channels = grabbed.nChannels();
			CvMat mat = grabbed.asCvMat();
			cols = mat.cols();
			double[] colorMatrix = mat.get();
			summeries = new int[NUM_OF_STICKERS][3];
			for (int i = 0; i < colorMatrix.length; i += channels) {
				if(isRed(colorMatrix[i], colorMatrix[i + 1], colorMatrix[i + 2])){
					storePoint(RED_VECTOR_INDEX, i);
				}
				if(isGreen(colorMatrix[i], colorMatrix[i + 1], colorMatrix[i + 2])){
					storePoint(GREEN_VECTOR_INDEX, i);
				}
				if(isBlue(colorMatrix[i], colorMatrix[i + 1], colorMatrix[i + 2])){
					storePoint(BLUE_VECTOR_INDEX, i);
				}
				if(isYellow(colorMatrix[i], colorMatrix[i + 1], colorMatrix[i + 2])){
					storePoint(YELLOW_VECTOR_INDEX, i);
				}
			}
			if(!Utils.isCI()){
				frame.showImage(grabbed);
				painter.setSize(frame.getCanvasSize());
			}
			for(int i = 0; i < NUM_OF_STICKERS; i++){
				Point point = new Point(getX(i), getY(i));
				allColorsVector.get(i).add(point);

				if(!Utils.isCI()){
					//Canvas canvas = new Canvas();
					//frame.add(canvas);
					//Graphics graphics = canvas.getGraphics();
					Graphics graphics = changingPanel.getGraphics();

					switch (i){
					case RED_VECTOR_INDEX:
						graphics.setColor(Color.RED);
						break;
					case GREEN_VECTOR_INDEX:
						graphics.setColor(Color.GREEN);
						break;
					case BLUE_VECTOR_INDEX:
						graphics.setColor(Color.BLUE);
						break;
					case YELLOW_VECTOR_INDEX:
						graphics.setColor(Color.YELLOW);
						break;
					}
					graphics.drawOval(point.getX(), point.getY(), 10, 10);
					//canvas.doLayout();
				}
			}
			TimeUnit.MILLISECONDS.sleep(500);
		}
		if(!Utils.isCI()){
			frame.dispose();
		}
		return allColorsVector;
	}


	private short getCoordinate(int i, int coordinate) {
		try{
			return (short)(summeries[i][coordinate] / summeries[i][COUNT_VECTOR_INDEX]);
		}
		catch(RuntimeException e){
			return 0;
		}

	}

	private short getY(int i) {
		return getCoordinate(i, Y_VECTOR_INDEX);
	}

	private short getX(int i) {
		return getCoordinate(i, X_VECTOR_INDEX);
	}

	private boolean isYellow(double r, double g, double b) {
		return isColor(r, g, b, YELLOW_MIN, YELLOW_MAX);
	}

	private boolean isBlue(double r, double g, double b) {
		return isColor(r, g, b, BLUE_MIN, BLUE_MAX);
	}

	private boolean isGreen(double r, double g, double b) {
		return isColor(r, g, b, GREEN_MIN, GREEN_MAX);
	}

	private boolean isRed(double r, double g, double b) {
		return isColor(r, g, b, RED_MIN, RED_MAX);
	}  

	private boolean isColor(double r, double g, double b, short[] min,
			short[] max) {
		return isHigher(r, g, b, min) && isLower(r, g, b, max);
	}


	private boolean isLower(double r, double g, double b, short[] max) {
		return (r <= max[RED_VECTOR_INDEX]) && (g <= max[GREEN_VECTOR_INDEX])
				&& (b <= max[BLUE_VECTOR_INDEX]); 
	}

	private boolean isHigher(double r, double g, double b, short[] min) {
		return (r >= min[RED_VECTOR_INDEX]) && (g >= min[GREEN_VECTOR_INDEX])
				&& (b >= min[BLUE_VECTOR_INDEX]); 
	}

	private void storePoint(short colorIndex, int i) {
		summeries[colorIndex][X_VECTOR_INDEX] += (i / channels) % cols ;
		summeries[colorIndex][Y_VECTOR_INDEX] += (i / channels) / cols;
		summeries[colorIndex][COUNT_VECTOR_INDEX]++;
	}


}
