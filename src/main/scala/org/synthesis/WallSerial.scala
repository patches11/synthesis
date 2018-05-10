package org.synthesis

import java.awt.Color

import akka.actor.{ActorRef, ActorSystem, Props}
import org.synthesis.serial.{SerialPanel, SetPixels, Update}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Brown on 6/26/17.
  */
case class WallSerial(system: ActorSystem, fps: Int, pixelsX: Int, pixelsY: Int,
                      brightness: Double = 1.0, panelsX: Int = 1, panelsY: Int = 1, devices: Seq[(String, Boolean)] = Nil, gammaCorrection: Boolean = true) extends WallSerialBase {

  println(s"WallSerial(system $system, " +
    s"fps $fps, pixelsX $pixelsX, pixelsY $pixelsY, " +
    s"brightness $brightness, gammaCorrection $gammaCorrection, " +
    s"panelsX $panelsX, panelsY $panelsY, " +
    s"devices $devices)")

  // TODO: Hard coded for now, fix!!
  override protected val size: Int = 32 * 25 * 3

  override protected val panels = devices.map(d => {
    (system.actorOf(Props(SerialPanel(d._1, size))), d._2)
  })

  override def render(): Unit = {

    for (panelX <- 0 until panelsX;
         panelY <- 0 until panelsY) yield {
      Future {
        val (panel, flip) = panels(panelY * panelsX + panelX)
        renderPanel(panel, panelPixelsX * panelX, panelPixelsY * panelY, flip)
      }
    }
  }

  def renderPanel(panel: ActorRef, offsetX: Int, offsetY: Int, flip: Boolean = false): Unit = {
    val output = Array.ofDim[Byte](size)

    var skipped = 0
    var y = 0
    while(y < panelPixelsY + skipped) {
      if (y % 4 == 3 && y != 31) {
        skipped += 1
        for (x <- 0 until panelPixelsX) {
          val index = pixelIndex(x, y)
          blackBytes.copyToArray(output, index)
        }
      } else {
        for (x <- 0 until panelPixelsX) {
          val index = pixelIndex(x, y)
          val color = getColor(x, offsetX, y, offsetY, skipped, flip)
          val c = colorWiring(color)
          c.copyToArray(output, index)
        }
      }
      y += 1
    }

    panel ! SetPixels(output)

    panel ! Update
  }

  override def getColor(x: Int, offsetX: Int, y: Int, offsetY: Int, skipped: Int, flip: Boolean): Int = {
    if (flip) {
      mixer.outputBuffer(offsetX + (panelPixelsX - 1 - x))(offsetY + (panelPixelsY - 1 - y + skipped))
    } else {
      mixer.outputBuffer(x + offsetX)(y + offsetY - skipped)
    }
  }

}