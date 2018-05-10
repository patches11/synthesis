package org.synthesis.webcam

import akka.actor.{Actor, ActorRef}
import org.opencv.core.{MatOfRect, Size}
import org.opencv.objdetect.CascadeClassifier
import org.synthesis.AfterTheLastMessage
import org.synthesis.webcam.OpenCVHelpers._


/**
  * Created by patrickbrown on 9/24/16.
  */
class FaceDetector() extends Actor {
  val faceDetector = new CascadeClassifier(getClass.getResource("/haarcascade_frontalface_alt.xml").getPath)

  context.system.eventStream.subscribe(self, classOf[WebcamFrame])

  var itCount = 0
  val fpsesF = scala.collection.mutable.ListBuffer[Double]()

  val waiting: Receive = {
    case w: WebcamFrame =>
      self ! AfterTheLastMessage
      context.become(searchingForWork(w, sender))
  }
  def searchingForWork(lastWorkSoFar: WebcamFrame, itsSender: ActorRef): Receive = {
    case AfterTheLastMessage =>
      processWork(lastWorkSoFar)
      context.become(waiting)
    case w: WebcamFrame => context.become(searchingForWork(w, sender))
  }
  context.become(waiting)

  def receive = waiting

  def processWork(webcamFrame: WebcamFrame): Unit = {
    val frame = webcamFrame.frame
    itCount += 1
    val t0 = System.nanoTime()
    val faceDetections = new MatOfRect()
    faceDetector.detectMultiScale(frame, faceDetections, 1.3, 2, 0, new Size(60, 60), new Size(800, 800))
    val faces = faceDetections.toArray map {f => frame.submat(f)}
    val faceImages = faces.toList map { f =>
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
    context.system.eventStream.publish(FaceImages(faceImages, faceDetections))
    fpsesF += (1000000000 / (System.nanoTime() - t0))
    if (itCount % 1000 == 0) {
      System.out.print(s"Face Detect fps ${fpsesF.sum / fpsesF.size} \r")
      fpsesF.clear()
    }
  }
}
