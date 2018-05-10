package org.synthesis.serial

/**
  * Created by Brown on 7/7/17.
  */
class DmxProMessageBuilder(val universeSize: Int) {
  val dataSize: Int = universeSize + 1
  private val state = Array.ofDim[Byte](universeSize)

  def set(u: Int, v: Byte): Unit = {
    state(u) = v
  }

  def setMany(values: Map[Int, Byte]): Unit = {
    values.foreach { case ((u, v)) => state(u) = v }
  }

  def setMany(values: Array[Byte]): Unit = {
    values.zipWithIndex.foreach { case ((v, u)) => state(u) = v}
  }

  def build(): Array[Byte] = {
    val message = new Array[Byte](universeSize + 6)
    message(0) = 0x7E.toByte //DMX_PRO_MESSAGE_START

    message(1) = 6.toByte // message type : DMX_PRO_SEND_PACKET

    message(2) = (dataSize & 255).toByte //data size coded on two bytes

    message(3) = ((dataSize >> 8) & 255).toByte
    message(4) = 0

    0 until universeSize foreach {i: Int =>
      message(5 + i) = 0
    }

    message(universeSize + 5) = 0xE7.toByte //DMX_PRO_MESSAGE_END;

    message
  }
}
