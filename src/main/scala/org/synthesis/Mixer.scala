package org.synthesis

import java.awt.Color

import scala.collection.mutable.ArrayBuffer

/**
  * Created by patrickbrown on 3/13/17.
  */


case class MixerChannel(buffer: Mixer, channel: Int, crossfade: Boolean) {
  def setPixel(x: Int, y: Int, color: Int) = buffer.setPixel(channel, x, y, color)

  def setPixel(point: Point, color: Int) = buffer.setPixel(channel, point.x, point.y, color)

  def getWidth = buffer.pixelsX
  def getHeight = buffer.pixelsY

  def inBounds(x: Int, y: Int): Boolean = {
    x >= 0 && x < buffer.pixelsX && y >= 0 && y < buffer.pixelsY
  }

  def getPixel(x: Int, y: Int): Int = {
    buffer.outputBuffer(x)(y)
  }

  def getRatio = buffer.getRatio(channel)
}

case class Mixer(pixelsX: Int, pixelsY: Int, crossfadeChannels: Int, alphaChannels: Int) {
  val outputBuffer: Array[Array[Int]] = Array.ofDim[Int](pixelsX, pixelsY)
  private val buffers = ArrayBuffer[Array[Array[Int]]]()
   var ratios = ArrayBuffer[Double]()
  private val crossfade = ArrayBuffer[Boolean]()

  for(x <- 0 until pixelsX) {
    for(y <- 0 until pixelsY) {
      outputBuffer(x)(y) = 0
    }
  }

  (0 until crossfadeChannels) foreach { _ =>
    register(true)
  }

  (0 until alphaChannels) foreach { _ =>
    register(false)
  }

  private def register(cf: Boolean): Unit = {
    val pixels = Array.ofDim[Int](pixelsX, pixelsY)

    for(x <- 0 until pixelsX) {
      for(y <- 0 until pixelsY) {
        pixels(x)(y) = 0
      }
    }

    ratios.append(0.0)
    buffers.append(pixels)
    crossfade.append(cf)
  }

  def setRatio(channel: MixerChannel, newRatio: Double): Unit = {
    if (channel.channel < buffers.length)
      ratios(channel.channel) = newRatio
  }

  def getRatio(channel: Int): Double = {
    ratios(channel)
  }

  def getChannel(channel: Int): MixerChannel = {
    MixerChannel(this, channel, crossfade(channel))
  }

  def setPixel(index: Int, x: Int, y: Int, color: Int): Unit = {
    buffers(index)(x)(y) = color
    setOutput(x, y)
  }

  def active(channel: Int): Boolean = {
    ratios(channel) > 0.0
  }

  def setOutput(x: Int, y: Int): Unit = {
    val temp = if (active(0) && active(1)) {
      Mixer.blend(buffers(0)(x)(y), buffers(1)(x)(y), ratios(1) / ratios(0))
    } else if (active(0)) {
      buffers(0)(x)(y)
    } else {
      buffers(1)(x)(y)
    }

    val alphaIndex = crossfade.indexOf(false)

    outputBuffer(x)(y) = if (alphaIndex >= 0 && active(alphaIndex)) {
      Mixer.alphaBlend(temp, buffers(alphaIndex)(x)(y))
    } else {
      temp
    }
  }
}

object Mixer {
  def getRGB(a: Int, r: Int, g: Int, b: Int): Int = {
    a << 24 | r << 16 | g << 8 | b
  }

  def getComponents(rgb: Int): (Int, Int, Int) = {
    ((rgb & 0xff0000) >> 16, (rgb & 0xff00) >> 8, rgb & 0xff)
  }

  def getComponentsA(argb: Int): (Int, Int, Int, Int) = {
    (argb >> 24 & 0xff, (argb & 0xff0000) >> 16, (argb & 0xff00) >> 8, argb & 0xff)
  }

  def RGBtoHSB(rgb: Int): (Float, Float, Float) = {
    val (r, g, b) = getComponents(rgb)

    val Array(hue, saturation, brightness) = Color.RGBtoHSB(r, g, b, null)

    (hue, saturation, brightness)
  }

  def HSBtoRGB(hsb: (Float, Float, Float)): Int = {
    Color.HSBtoRGB(hsb._1, hsb._2, hsb._3)
  }

  def darken(rgb: Int, ratio: Double, cutOff: Double): Int = {
    val (h, s, br) = darken(RGBtoHSB(rgb), ratio, cutOff)

    Color.HSBtoRGB(h, s, br)
  }

  def darken(hsb: (Float, Float, Float), ratio: Double, cutOff: Double): (Float, Float, Float) = {
    val (hue, saturation, brightness) = hsb

    val ratioComp = 1 - ratio

    val newBrightness = brightness * ratioComp

    (hue, saturation, if (newBrightness < cutOff) 0 else newBrightness.toFloat)
  }

  def white: Int = 255 << 24 | 255 << 16 | 255 << 8 | 255

  def red: Int = 255 << 24 | 255 << 16 | 0 << 8 | 0

  def green: Int = 255 << 24 | 0 << 16 | 255 << 8 | 0

  def blue: Int = 255 << 24 | 0 << 16 | 0 << 8 | 255

  def color(r: Int, g: Int, b: Int): Int = {
    255 << 24 | r << 16 | g << 8 | b
  }

  def blend(colors: List[Int]): Int = {
    val ratio = 1.0 / colors.length

    val a = colors.map(c => (c >> 24 & 0xff) * ratio).sum.toInt
    val r = colors.map(c => ((c & 0xff0000) >> 16) * ratio).sum.toInt
    val g = colors.map(c => ((c & 0xff00) >> 8) * ratio).sum.toInt
    val b = colors.map(c => (c & 0xff) * ratio).sum.toInt

    a << 24 | r << 16 | g << 8 | b
  }

  def alphaBlend(baseColor: Int, newColor: Int): Int = {
    val a1 = newColor >> 24 & 0xff

    val ratio = a1.toDouble / 255

    blend(baseColor, newColor, ratio)
  }

  def blend(i1: Int, i2: Int, initialR: Double ): Int = {
    val ratio = if ( initialR > 1.0 ) 1.0
    else if ( initialR < 0.0 ) 0.0
    else initialR

    val iRatio = 1.0 - ratio

    val a1 = i1 >> 24 & 0xff
    val r1 = (i1 & 0xff0000) >> 16
    val g1 = (i1 & 0xff00) >> 8
    val b1 = i1 & 0xff

    val a2 = i2 >> 24 & 0xff
    val r2 = (i2 & 0xff0000) >> 16
    val g2 = (i2 & 0xff00) >> 8
    val b2 = i2 & 0xff

    val a = ((a1 * iRatio) + (a2 * ratio)).toInt
    val r = ((r1 * iRatio) + (r2 * ratio)).toInt
    val g = ((g1 * iRatio) + (g2 * ratio)).toInt
    val b = ((b1 * iRatio) + (b2 * ratio)).toInt

    a << 24 | r << 16 | g << 8 | b
  }
}