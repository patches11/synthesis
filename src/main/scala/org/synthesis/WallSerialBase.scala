package org.synthesis

import java.awt.Color

import akka.actor.{ActorRef, ActorSystem, Props}
import org.synthesis.serial.{SerialPanel, SetPixels}

import scala.concurrent.Future

trait WallSerialBase extends Wall {
  def system: ActorSystem
  def fps: Int
  def pixelsX: Int
  def pixelsY: Int
  def brightness: Double
  def panelsX: Int
  def panelsY: Int
  def devices: Seq[(String, Boolean)]
  def gammaCorrection: Boolean




  val gammatable = new Array[Int](256)

  val gamma = 1.7f

  protected val panelPixelsX = pixelsX / panelsX
  protected val panelPixelsY = pixelsY / panelsY

  var iterationCount: Long = 0
  protected val fpses = scala.collection.mutable.ListBuffer[Double]()

  // TODO: Hard coded for now, fix!!
  protected val size: Int

  (0 until 256).foreach(i => {
    gammatable(i) = (Math.pow(i.toFloat / 255.0, gamma) * 255.0 + 0.5).toInt
  })

  protected val panels: Seq[(ActorRef, Boolean)]

  protected val blackBytes = Array(0.toByte, 0.toByte, 0.toByte)

  def getColor(x: Int, offsetX: Int, y: Int, offsetY: Int, skipped: Int, flip: Boolean): Int = {
    if (flip) {
      mixer.outputBuffer(offsetX + (panelPixelsX - 1 - x))(offsetY + (panelPixelsY - 1 - y + skipped))
    } else {
      mixer.outputBuffer(x + offsetX)(y + offsetY - skipped)
    }
  }

  def getColorBytes(c: Color): Array[Int] = {
    Array(c.getRed, c.getGreen, c.getBlue)
  }

  def correct(a: Array[Int]): Array[Int] = {
    a.map(gammatable(_))
  }

  def adjustBrightness(a: Array[Int]): Array[Byte] = {
    a.map(a => (a * brightness).toByte)
  }

  def colorWiring(c: Int): Array[Byte] = {
    val color = getColorBytes(new Color(c))
    val gammad = if (gammaCorrection) correct(color) else color
    if (brightness < 1.0) adjustBrightness(gammad) else gammad.map(_.toByte)
  }

  def pixelIndex(x: Int, y: Int): Int = {
    if (y % 2 == 0) {
      (panelPixelsX * 3) * y + (x * 3)
    } else {
      (panelPixelsX * 3) * (y + 1) - ((x + 1) * 3)
    }
  }
}
