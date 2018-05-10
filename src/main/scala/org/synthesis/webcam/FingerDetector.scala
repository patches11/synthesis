package org.synthesis.webcam

import akka.actor.{Actor, ActorRef}
import org.opencv.core.{MatOfRect, Size}
import org.opencv.objdetect.CascadeClassifier
import org.synthesis.AfterTheLastMessage


/**
  * Created by patrickbrown on 6/10/17.
  scaleFactor – Parameter specifying how much the image size is reduced at each image scale.

Basically the scale factor is used to create your scale pyramid. More explanation can be found here. In short, as described here, your model has a fixed size defined during training, which is visible in the xml. This means that this size of face is detected in the image if present. However, by rescaling the input image, you can resize a larger face to a smaller one, making it detectable by the algorithm.

1.05 is a good possible value for this, which means you use a small step for resizing, i.e. reduce size by 5%, you increase the chance of a matching size with the model for detection is found. This also means that the algorithm works slower since it is more thorough. You may increase it to as much as 1.4 for faster detection, with the risk of missing some faces altogether.
minNeighbors – Parameter specifying how many neighbors each candidate rectangle should have to retain it.

This parameter will affect the quality of the detected faces. Higher value results in less detections but with higher quality. 3~6 is a good value for it.
minSize – Minimum possible object size. Objects smaller than that are ignored.

This parameter determine how small size you want to detect. You decide it! Usually, [30, 30] is a good start for face detection.
maxSize – Maximum possible object size. Objects bigger than this are ignored.

This parameter determine how big size you want to detect. Again, you decide it! Usually, you don't need to set it manually, the default value assumes you want to detect without an upper limit on the size of the face.

  */

class FingerDetector() extends Actor {
  val fingerDetector = new CascadeClassifier(getClass.getResource("/hand.xml").getPath)

  context.system.eventStream.subscribe(self, classOf[WebcamFrame])

  private var itCount = 0
  private val fpsesF = scala.collection.mutable.ListBuffer[Double]()

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
    val fingerDetections = new MatOfRect()
    fingerDetector.detectMultiScale(frame, fingerDetections, 1.5, 6, 0, new Size(30, 30), new Size(200, 200))
    context.system.eventStream.publish(FingerDetections(fingerDetections, frame.width, frame.height))
    fpsesF += (1000000000 / (System.nanoTime() - t0))
    if (itCount % 1000 == 0) {
      System.out.print(s"Finger Detect fps ${fpsesF.sum / fpsesF.size} \r")
      fpsesF.clear()
    }
  }
}
