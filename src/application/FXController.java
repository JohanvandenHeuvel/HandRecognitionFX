package application;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.plaf.synth.SynthSeparatorUI;

import org.omg.CORBA.INITIALIZE;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class FXController 
{
	@FXML private Button button;
	@FXML private ImageView currentFrame;
	@FXML private CheckBox grayscale;
	@FXML private CheckBox logoCheckBox;
	
	private ScheduledExecutorService timer;
	private VideoCapture capture;
	private boolean cameraActive;
	private static int cameraID = 0;
	private Mat logo;
	
	public void initialize()
	{
		this.capture = new VideoCapture();
		this.cameraActive = false;
	}
		
	@FXML protected void startCamera()
	{
		this.currentFrame.setFitWidth(600);
		this.currentFrame.setPreserveRatio(true);
		
		if(!this.cameraActive)
		{
			this.capture.open(cameraID);
			if (this.capture.isOpened())
			{
				this.cameraActive = true;
				
				Runnable frameGrabber = new Runnable() 
				{
					@Override public void run()
					{
						Image imageToShow = grabFrame();
						currentFrame.setImage(imageToShow);
					}
				};
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				this.button.setText("Stop Camera");
			}
			else
			{
				System.err.println("Impossible to open the camera connection...");
			}
		}
		else
		{
			this.cameraActive = false;
			this.button.setText("Start Camera");
			
			try
			{
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
			
			this.capture.release();
			this.currentFrame.setImage(null);
		}
	}
	
	@FXML protected void loadLogo()
	{
		if (logoCheckBox.isSelected())
		{
			this.logo = Imgcodecs.imread("resourses/Poli.png");
		}
	}
	
	private Image grabFrame()
	{
		Image imageToShow = null;
		Mat frame = new Mat();
		if(this.capture.isOpened())
		{
			try
			{
				this.capture.read(frame);
				if(!frame.empty())
				{
					if(logoCheckBox.isSelected() && this.logo != null)
					{
						Rect roi = new Rect(
								frame.cols() - logo.cols(), 
								frame.rows() - logo.rows(), 
								logo.cols(),
								logo.rows());						
						Mat imageROI = frame.submat(roi);
						//Core.addWeighted(imageROI, 1.0, logo, 0.8, 0.0, imageROI);
						
						logo.copyTo(imageROI, logo);
					}
					
					if (grayscale.isSelected())
					{
						Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
					}
					
					imageToShow = mat2Image(frame);
				}
			}
			catch (Exception e)
			{
				System.err.println("Exception during the image elaboration: " + e);
			}
		}
		return imageToShow;
	}
	
	private Image mat2Image(Mat frame)
	{
		MatOfByte buffer = new MatOfByte();
		Imgcodecs.imencode(".png", frame, buffer);
		return new Image(new ByteArrayInputStream(buffer.toArray()));
	}
}
