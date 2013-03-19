package edu.lipreading.gui;

import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import edu.lipreading.Utils;
import edu.lipreading.vision.AbstractFeatureExtractor;
import edu.lipreading.vision.ColoredStickersFeatureExtractor;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoCapturePanel extends JPanel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    protected String videoInput = null;
    protected BufferedImage image = null;
    protected VideoCanvas canvas;
    protected FrameGrabber grabber = null;
    protected AbstractFeatureExtractor stickersExtractor;
    protected Thread videoGrabber;
    protected final AtomicBoolean threadStop;
    protected JProgressBar progressBar = new JProgressBar();



    /**
     * Create the panel.
     */
    public VideoCapturePanel() {
        stickersExtractor = new ColoredStickersFeatureExtractor();
        canvas = new VideoCanvas();
        canvas.setBackground(Color.LIGHT_GRAY);

        canvas.setBounds(129, 10, 456, 362);
        this.add(canvas);
        canvas.setVisible(true);
        canvas.createBufferStrategy(1);

        threadStop = new AtomicBoolean(true);
    }

    public void startVideo() throws Exception {
        if (grabber == null)
        {
            try{
                grabber = stickersExtractor.getGrabber(videoInput);
            }catch (Exception e){
                if(grabber != null){
                    grabber.stop();
                }
                grabber = null;
                if(e instanceof UnknownHostException){
                    JOptionPane.showMessageDialog(this,
                            "Could not reach host " + e.getMessage()
                                    + "\nplease check the URL and your Internet connection",
                            "Inane warning",
                            JOptionPane.WARNING_MESSAGE);
                }
                throw e;
            }
        }
        try{
            synchronized (threadStop) {
                grabber.start();
            }
        } catch (Exception e){
            if(e.getMessage().contains("Could not setup device")){
                JOptionPane.showMessageDialog(this,
                        "This feature only works with a camera",
                        "Inane warning",
                        JOptionPane.WARNING_MESSAGE);
            }
            throw e;
        }
        threadStop.set(false);
        videoGrabber = new Thread(new Runnable()
        {

            public void run()
            {
                try {
                    getVideoFromSource();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        videoGrabber.start();
    }


    public void stopVideo() throws Exception{
        synchronized (threadStop) {
            threadStop.set(true);
            if (grabber != null){
                grabber.stop();
            }
        }
        //stopRecordingVideo();
    }


    protected void getVideoFromSource() throws Exception {

        IplImage grabbed;
        while(!threadStop.get() && (grabbed = grabber.grab()) != null){
            image = grabbed.getBufferedImage();
            canvas.setImage(image);
            canvas.paint(null);
        }
    }

    public void initGrabber() throws Exception{
        if(Utils.isSourceUrl(videoInput)) {
            progressBar.setValue(0);
            progressBar.setVisible(true);
            Utils.get(videoInput, progressBar);
            videoInput = Utils.getFileNameFromUrl(videoInput);
        }
        grabber = stickersExtractor.getGrabber(videoInput);
    }



    public void setVideoInput(String videoInput){
        this.videoInput = videoInput;
    }



}