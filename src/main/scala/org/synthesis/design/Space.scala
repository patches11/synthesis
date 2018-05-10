package org.synthesis.design

import java.awt.Color

import org.synthesis.design.support.SetPalette
import org.synthesis.palette.Palette
import org.synthesis.{Mixer, MixerChannel, Wall}

import scala.util.Random

case class Space(wall: Wall, defaultPalette: Palette) extends Design {

  private var lastTime = System.currentTimeMillis()

  private val random = new Random(System.nanoTime())

  private var stars: List[Star] = List()

  protected var internalPalette: Palette = defaultPalette

  private val cameraDistance = 5

  private val localBuffer = Array.ofDim[Int](wall.pixelsX,wall.pixelsY)

  override def support: Receive = {
    case SetPalette(p) => internalPalette = p
  }

  override def render(channel: MixerChannel): Unit = {
    for(x <- 0 until wall.pixelsX;
        y <- 0 until wall.pixelsY) yield {
      localBuffer(x)(y) = 0
    }

    val diff = System.currentTimeMillis() - lastTime
    lastTime = System.currentTimeMillis()

    if (random.nextDouble > 0.95) {
      val r = random.nextInt(1000000)
      val d = r / 3 + random.nextInt(r * 5 / 3)
      stars = stars :+ new Star(random.nextDouble * 2 * Math.PI, r, internalPalette.getColor(random.nextInt(internalPalette.length)), d)
    }

    stars = stars.filterNot { star =>
      star.update(diff)
      val remove = star.shouldRemove
      if (!remove) {
        star.render(localBuffer, channel, cameraDistance)
      }
      remove
    }

    for(x <- 0 until wall.pixelsX) {
      for(y <- 0 until wall.pixelsY) {
        channel.setPixel(x, y, localBuffer(x)(y))
      }
    }
  }
}

class Star(val angle: Double, val r: Long, color: Int, var d: Long) {

  val (red, g, b) = Mixer.getComponents(color)

  val Array(hue, saturation, _) = Color.RGBtoHSB(red, g, b, null)

  def update(diffMillis: Long): Unit = {
    d -= diffMillis * 10
  }

  def shouldRemove: Boolean = {
    d <= 0
  }

  def render(buffer: Array[Array[Int]], channel: MixerChannel, cameraDistance: Long): Unit = {
    val rPlane = r * cameraDistance / (d + cameraDistance)

    val halfWidth = channel.getWidth / 2
    val halfHeight = channel.getHeight / 2

    val x = (rPlane * Math.cos(angle)).toInt
    val y = (rPlane * Math.sin(angle)).toInt

    if (x > -halfWidth && x < halfWidth && y > -halfHeight && y < halfHeight) {
      buffer(x + halfWidth)(y + halfHeight) = Color.HSBtoRGB(hue, saturation, (1000000 * 2 - d).toFloat / 1000000 * 2 )
    }
  }
}