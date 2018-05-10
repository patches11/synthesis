package org.synthesis.design

import java.awt.Color

import org.synthesis.{MixerChannel, Wall}

import scala.util.Random

case class Blink(wall: Wall) extends Design {

  private val blinks = Array.ofDim[Light](wall.pixelsX, wall.pixelsY)

  private val random = new Random(System.nanoTime())

  private var h = random.nextFloat

  var spinRate: Float = 0.00085f
  var saturationRandom: Boolean = true
  var blinkOn: Boolean = true

  private var lastTime = System.currentTimeMillis()

  for(x <- 0 until wall.pixelsX;
      y <- 0 until wall.pixelsY) yield {
    blinks(x)(y) = new Light()
  }

  def getSat: Float = {
    if (saturationRandom) {
      random.nextFloat
    } else {
      1.0f
    }
  }

  override def render(channel: MixerChannel): Unit = {
    val diff = System.currentTimeMillis() - lastTime
    lastTime = System.currentTimeMillis()
    val paletteShift = ((System.currentTimeMillis() % 100000) / 5.0).toInt

    // spin colors
    h = (h + spinRate / (diff + 1)) % 1

    for(x <- 0 until wall.pixelsX;
        y <- 0 until wall.pixelsY) yield {

      val light = blinks(x)(y)

      if (light.isOn) {
        light.brightness += random.nextFloat / 100.0f
        if (light.brightness >= 1.0f) {
          light.state = State.Off
        }
      } else if (light.isTransition) {

      } else {
        if (light.brightness <= 0) {
          if (random.nextDouble >= 0.99995) {
            light.state = State.On
            light.hue = h
            light.saturation = (random.nextDouble / 10 + 0.9).toFloat
          }
        } else {
          light.brightness -= random.nextFloat / 100.0f
        }
      }

      light.brightness = Math.min(Math.max(light.brightness, 0.0f), 1.0f)

      channel.setPixel(x, y, light.getColor)
    }
  }
}

object State {
  sealed trait EnumVal {}

  case object Off extends EnumVal
  case object On extends EnumVal
  case object Transition extends EnumVal
}



class Light {
  var hue: Float = 0.0f
  var saturation: Float = 0.0f
  var brightness: Float = 0.0f
  var state: State.EnumVal = State.Off

  def getColor = Color.HSBtoRGB(hue, saturation, brightness)

  def isOff: Boolean = state == State.Off
  def isOn: Boolean = state == State.On
  def isTransition: Boolean = state == State.Transition
}