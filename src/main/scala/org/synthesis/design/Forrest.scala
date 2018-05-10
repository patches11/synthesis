package org.synthesis.design

import java.awt.Color

import org.synthesis.{Mixer, MixerChannel, Wall}

import scala.util.Random

/**
  * Created by Brown on 6/16/17.
  */
case class Forrest(wall: Wall) extends Design {

  val trees = Seq(
    Tree(wall.pixelsX / 2, wall.pixelsY - 1, wall.pixelsY * 3 / 5),
    Tree(wall.pixelsX / 5, wall.pixelsY - 6, wall.pixelsY / 4),
    Tree(wall.pixelsX * 4 / 5, wall.pixelsY - 8, wall.pixelsY / 3)
  )

  override def render(channel: MixerChannel): Unit = {
    trees.foreach(_.render(channel))
  }
}

case class Tree(x: Int, initialY: Int, height: Int) {

  val initWidth: Double = height / 4
  val layerReduce: Double = 1.0

  val darkTreeColor: Int = Color.getHSBColor(0.56f, 0.95f, 0.3f).getRGB
  val lightTreeColor: Int = Color.getHSBColor(0.56f, 0.95f, 0.5f).getRGB

  def render(channel: MixerChannel): Unit = {
    var y = 0

    // Stump
    val stumpRadius = height / 30
    0 until (height / 10) foreach { _ =>
      val layerY = initialY - y

      (x - stumpRadius) to (x + stumpRadius) foreach {layerX =>
        channel.setPixel(layerX, layerY, lightTreeColor)
      }

      y +=1
    }

    var radius = initWidth.toInt
    var segment = 0

    while (radius != 0) {
      val layerY = initialY - y

      (x - radius) to (x + radius) foreach { layerX =>
        channel.setPixel(layerX, layerY, trippyColors)
      }

      if (radius < 2 && y < height - 3) {
        radius += 2
        segment += 1
      } else {
        radius -= 1
      }

      y +=1
    }

  }

  def trippyColors: Int = {
    //TODO: Fast flash is caused by the random nextFloat, keep it the same for a bit maybe?
    val paletteShift = ((System.currentTimeMillis() % 100000) / 5.0).toInt
    Color.getHSBColor(paletteShift.toFloat / 5000 , Random.nextFloat, 1.0f).getRGB
  }

}