package org.synthesis.design

import java.io.File

import org.synthesis.{Mixer, MixerChannel, Wall}
import javax.imageio.ImageIO


/**
  * Created by patrickbrown on 5/31/17.
  */
case class Pippa(wall: Wall) extends Design {

  private val pippa = ImageIO.read(new File(getClass.getResource("/Pippa.png").getPath))

  private val pippaPixels = pippa.getRGB(0, 0, pippa.getWidth(), pippa.getHeight(), null, 0, pippa.getWidth())

  def getPixel(x: Int, y: Int, ratio: Double): Int = {
    val (a, r, g, b) = Mixer.getComponentsA(pippaPixels(y * pippa.getWidth() + x))

    val newA = (a * ratio).toInt

    newA << 24 | r << 16 | g << 8 | b
  }

  override def render(channel: MixerChannel): Unit = {
    val ratioX = (pippa.getWidth * 2) / wall.pixelsX
    val ratioY = (pippa.getHeight * 2) / wall.pixelsY
    val ratio = if (ratioX < ratioY) ratioX else ratioY

    val gapX = wall.pixelsX - pippa.getWidth / ratio
    val gapY = wall.pixelsY - pippa.getHeight / ratio

    for (x <- 0 until wall.pixelsX) {
      for (y <- 0 until wall.pixelsY) {
        val faceX = (x - gapX / 2) * ratio
        val faceY = (y - gapY) * ratio

        if (faceX < ratio / 2 || faceX + ratio / 2 >= pippa.getWidth || faceY < ratio / 2 || faceY + ratio / 2 >= pippa.getHeight) {
          channel.setPixel(x, y, 0)
        } else {
          val pixel = getPixel(faceX, faceY, channel.getRatio)
          val a = pixel >> 24 & 0xff
          if (a < 20) {
            channel.setPixel(x, y, 0)
          } else {
            channel.setPixel(x, y, pixel)
          }
        }
      }
    }
  }
}