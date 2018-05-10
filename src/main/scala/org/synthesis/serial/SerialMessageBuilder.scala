package org.synthesis.serial

/**
  * Created by Brown on 7/7/17.
  */
trait SerialMessageBuilder {
  def set(u: Int, v: Byte): Unit

  def setMany(values: Map[Int, Byte]): Unit

  def setMany(values: Array[Byte]): Unit

  def build(): Array[Byte]
}
