package org.synthesis.design

import akka.actor.Actor
import org.synthesis.{MixerChannel, Wall}

trait Design extends Actor {
  def wall: Wall

  def render(channel: MixerChannel)

  protected val fpses = scala.collection.mutable.ListBuffer[Double]()
  protected var iterationCount: Long = 0

  def support: Receive = {
    case _ =>
  }

  def base: Receive = {
    case _ =>
  }

  def baseReceive: Receive = {
    case Render(channel) =>
      val t0 = System.nanoTime()
      iterationCount += 1
      render(channel)
      fpses += (1000000000 / (System.nanoTime() - t0))
      if (iterationCount % 120 == 0) {
        System.out.print(s"${this.getClass.getSimpleName} fps ${"%.1f".format(fpses.sum / fpses.size)} \r")
        fpses.clear()
      }
  }

  override final def receive = baseReceive orElse support orElse base
}

case class Render(channel: MixerChannel)