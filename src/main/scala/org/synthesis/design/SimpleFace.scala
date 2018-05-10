package org.synthesis.design

import java.awt.image.BufferedImage
import java.io.File

import akka.actor.{Actor, ActorSystem, Props}
import org.synthesis.webcam._
import org.synthesis.{Mixer, MixerChannel, Utils, Wall}

/**
  * Created by patrickbrown on 3/18/17.
  */
case class SimpleFace(wall: Wall) extends Design {

  private var faceImages: List[BufferedImage] = List()

  class FaceListener extends Actor {

    context.system.eventStream.subscribe(self, classOf[Detections])

    def receive = {
      case FaceImages(images, _) =>
        faceImages = images
    }
  }

  def getPixel(rgb: Int, ratio: Double): Int = {
    val (a, r, g, b) = Mixer.getComponentsA(rgb)

    val newA = (a * ratio).toInt

    newA << 24 | r << 16 | g << 8 | b
  }

  context.system.actorOf(Props(new FaceListener()))

  override def render(channel: MixerChannel): Unit = {
    val image = faceImages.headOption

    image.foreach { case face =>
      import javax.imageio.ImageIO
      //val outputfile = new File("image.jpg")
      //ImageIO.write(face, "jpg", outputfile)

      // TODO: This is int arithmetic, should it be?
      val ratioX = (face.getWidth * 2) / wall.pixelsX
      val ratioY = (face.getHeight * 2) / wall.pixelsY
      val ratio = if (ratioX < ratioY) ratioX else ratioY

      val gapX = wall.pixelsX - face.getWidth / ratio
      val gapY = wall.pixelsY - face.getHeight / ratio

      val maxDistance = Utils.distance(face.getWidth / 2, face.getHeight / 2, face.getWidth, face.getHeight)

      for (x <- 0 until wall.pixelsX) {
        for (y <- 0 until wall.pixelsY) {
          val faceX = (x - gapX / 2) * ratio
          val faceY = (y - gapY / 2) * ratio

          if (faceX < ratio / 2 || faceX + ratio / 2 >= face.getWidth || faceY < ratio / 2 || faceY + ratio / 2 >= face.getHeight) {
            channel.setPixel(x, y, 0)
          } else {
            val pixels = for(fX <- (faceX - ratio / 2).to(faceX + ratio / 2);
                             fY <- (faceY - ratio / 2).to(faceY + ratio / 2)) yield (fX, fY)
            //channel.setPixel(x, y, Mixer.blend(pixels.toList.map { case (fX, fY) => face.getRGB(fX, fY)}))
            val dis = Utils.distance(face.getWidth / 2, face.getHeight / 2, faceX, faceY)
            channel.setPixel(x, y, getPixel(face.getRGB(pixels.head._1, pixels.head._2), channel.getRatio * (1 - dis / maxDistance)))
          }
        }
      }
    }
  }
}
