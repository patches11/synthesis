package org.synthesis.webcam

import akka.actor.Actor
import org.opencv.core.{MatOfRect, Size}
import org.opencv.objdetect.CascadeClassifier
import org.synthesis.webcam.OpenCVHelpers._

/**
  * Created by patrickbrown on 9/24/16.
  */
class EyeDetector extends Actor {
  val eyeDetector = new CascadeClassifier(getClass.getResource("/haarcascade_eye.xml").getPath)

  context.system.eventStream.subscribe(self, classOf[WebcamFrame])

  var itCount = 0
  val fpsesF = scala.collection.mutable.ListBuffer[Double]()

  override def receive: Receive = {
    case WebcamFrame(frame) =>
      itCount += 1
      val t0 = System.nanoTime()
      val eyeDetections = new MatOfRect()
      val rightEyeDetections = new MatOfRect()
      eyeDetector.detectMultiScale(frame, eyeDetections, 1.3, 2, 0, new Size(30, 30), new Size(200, 200))
      val eyes = eyeDetections.toArray map {f => frame.submat(f)}
      val eyeImages = eyes.toList map { f =>
        val image = f.toBuffer
        val aRaster = image.getAlphaRaster
        for(x <- 0 until image.getWidth()) {
          for(y <- 0 until image.getHeight()) {
            val offCenterX = Math.abs(x - image.getWidth() / 2)
            val offCenterY = Math.abs(y - image.getHeight() / 2)
            val alpha = Math.max(255 - Math.sqrt(offCenterX * offCenterX + offCenterY * offCenterY) / Math.max(image.getWidth(), image.getHeight()) * 500, 0).toInt
            aRaster.setSample(x, y, 0, alpha)
          }
        }
        image
      }
      context.system.eventStream.publish(EyeImages(eyeImages, eyeDetections))
      fpsesF += (1000000000 / (System.nanoTime() - t0))
      if (itCount % 10 == 0) {
        System.out.print(s"Eye Detect fps ${fpsesF.sum / fpsesF.size} \r")
        fpsesF.clear()
      }
  }
}
