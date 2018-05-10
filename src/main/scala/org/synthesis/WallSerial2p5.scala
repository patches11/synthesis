package org.synthesis

import java.awt.Color

import akka.actor.{ActorRef, ActorSystem, Props}
import org.synthesis.serial.{SerialPanel, SetBrightness, SetPixels, Update}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Brown on 6/26/17.
  */
case class WallSerial2p5(system: ActorSystem,  fps: Int,  pixelsX: Int,  pixelsY: Int,  brightness: Double = 1.0,
                        panelsX: Int = 1,  panelsY: Int = 1,  devices: Seq[(String, Boolean)] = Nil,
                        gammaCorrection: Boolean = true) extends WallSerialBase {

  println(s"WallSerial2(system $system, " +
    s"fps $fps, pixelsX $pixelsX, pixelsY $pixelsY, " +
    s"brightness $brightness, gammaCorrection $gammaCorrection, " +
    s"panelsX $panelsX, panelsY $panelsY, " +
    s"devices $devices)")

  override protected val size: Int = panelPixelsX * panelPixelsY * 3

  override protected val panels: Seq[(ActorRef, Boolean)] = devices.map(d => {
    (system.actorOf(Props(SerialPanel(d._1, size))), d._2)
  })

  // Wait then set brightness
  Thread.sleep(2000)

  this.setBrightness()

  private def setBrightness(): Unit = {
    def getBrightness: Byte = {
      (255 * brightness).toByte
    }

    panels.foreach { case (panel, _) => panel ! SetBrightness(getBrightness) }
  }

  override def render(): Unit = {

    iterationCount += 1

    for (panelX <- 0 until panelsX;
         panelY <- 0 until panelsY) yield {
      Future {
        val (panel, flip) = panels(panelY * panelsX + panelX)
        renderPanel(panel, panelPixelsX * panelX, panelPixelsY * panelY, flip)
      }
    }

    if (iterationCount % 240 == 0) {
      setBrightness()
    }
  }

  def renderPanel(panel: ActorRef, offsetX: Int, offsetY: Int, flip: Boolean = false): Unit = {
    val output = Array.ofDim[Byte](size)

    for(y <- 0 until panelPixelsY;
        x <- 0 until panelPixelsX) yield {
      val index = pixelIndex(x, y)
      val color = getColor(x, offsetX, y, offsetY, flip)
      val c = colorWiring(color)
      c.copyToArray(output, index)
    }

    panel ! SetPixels(output)

    panel ! Update
  }

  override def colorWiring(c: Int): Array[Byte] = {
    val color = getColorBytes(new Color(c))
    color.map(_.toByte)
  }

  def getColor(x: Int, offsetX: Int, y: Int, offsetY: Int, flip: Boolean): Int = {
    if (flip) {
      mixer.outputBuffer(offsetX + (panelPixelsX - 1 - x))(offsetY + (panelPixelsY - 1 - y))
    } else {
      mixer.outputBuffer(x + offsetX)(y + offsetY)
    }
  }

  override def pixelIndex(x: Int, y: Int): Int = {
    val mod3 = y % 3
    if ((mod3 == 0 || mod3 == 2) && y < 24) {
      (panelPixelsX * 3) * y + (x * 3)
    } else {
      (panelPixelsX * 3) * (y + 1) - ((x + 1) * 3)
    }
  }

}