package org.synthesis.webcam

import java.awt.image.BufferedImage

import akka.actor.ActorSystem
import org.opencv.core.{Mat, MatOfRect}
import org.opencv.highgui.VideoCapture

/**
  * Created by patrickbrown on 9/24/16.
  */
case class WebcamMaster(system: ActorSystem) extends Runnable {
  var itCount = 0
  val fpsesF = scala.collection.mutable.ListBuffer[Double]()

  val cap = new VideoCapture(0)

  if (!cap.isOpened()) {
    System.out.println("Webcam not found :(")
  } else {

    System.out.println("Found Webcam: " + cap)

    val open = cap.open(0)
    System.out.println("Webcam.open returned: " + open)

    System.out.println("Waiting a bit, Webcam starting")
    try {
      Thread.sleep(1500)
    } catch {
      case ie: InterruptedException => ie.printStackTrace()
    }
  }

  sys.addShutdownHook(cap.release())

  override def run(): Unit = {
    while (cap.isOpened) {
      itCount += 1
      val t0 = System.nanoTime()
      val frame: Mat = new Mat
      cap.read(frame)
      system.eventStream.publish(WebcamFrame(frame))
      fpsesF += (1000000000 / (System.nanoTime() - t0))
      if (itCount % 100 == 0) {
        System.out.print(s"Webcam Capture fps ${fpsesF.sum / fpsesF.size} \r")
        fpsesF.clear()
      }
      try {
        Thread.sleep(1000 / 60)
      } catch {
        case ie: InterruptedException => ie.printStackTrace()
      }
    }
  }
}

case class WebcamFrame(frame: Mat)

trait Detections {
  def images: List[BufferedImage]
  def detections: MatOfRect
}

case class FaceImages(images: List[BufferedImage], detections: MatOfRect) extends Detections

case class EyeImages(images: List[BufferedImage], detections: MatOfRect) extends Detections

case class FingerDetections(detections: MatOfRect, width: Int, height: Int) extends Detections {
  def images = Nil
}