package org.synthesis.webcam

import java.awt.Graphics
import java.awt.image.BufferedImage
import javax.swing.JPanel

import akka.actor.{Actor, ActorSystem, Props}
import org.opencv.core._
import org.synthesis.webcam.OpenCVHelpers._

import java.awt.Image.SCALE_AREA_AVERAGING

/**
  * Created by patrickbrown on 9/24/16.
  */
case class WebcamPanel(system: ActorSystem, fps: Int) extends JPanel with Runnable {
  var faceDetections = new MatOfRect()
  var eyeDetections = new MatOfRect()
  var fingerDetections = new MatOfRect()
  var currentFrame = new Mat
  var started = false
  var canvas = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)

  val webcamWidth = 720

  protected override def paintComponent(g: Graphics) {
    super.paintComponent(g)

    if (started) {
      faceDetections.toArray.foreach(rect =>
        Core.rectangle(currentFrame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0))
      )

      eyeDetections.toArray.foreach(rect =>
        Core.rectangle(currentFrame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0))
      )


      fingerDetections.toArray.foreach(rect =>
        Core.rectangle(currentFrame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255))
      )

      g.drawImage(currentFrame.toBuffer.getScaledInstance(webcamWidth, -1, SCALE_AREA_AVERAGING), 0, 0, null)
    }

    g.dispose()
  }

  class WebcamListener extends Actor {

    context.system.eventStream.subscribe(self, classOf[WebcamFrame])

    def receive = {
      case WebcamFrame(frame) =>
        frame.copyTo(currentFrame)
        started = true
    }
  }

  class FaceListener extends Actor {

    context.system.eventStream.subscribe(self, classOf[Detections])

    def receive = {
      case FaceImages(_, detections) =>
        faceDetections = detections
      case EyeImages(_, detections) =>
        eyeDetections = detections
      case FingerDetections(detections, _, _) =>
        fingerDetections = detections
    }
  }

  override def run() {
    canvas = new BufferedImage(getWidth, getHeight, BufferedImage.TYPE_INT_ARGB)

    val faceListener = system.actorOf(Props(new FaceListener()))
    val webcamListener = system.actorOf(Props(new WebcamListener()))

    while (true) {
      this.repaint()
      try {
        Thread.sleep(1000 / fps)
      }
      catch {
        case ex: InterruptedException => {}
      }
    }
  }
}
