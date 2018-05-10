package org.synthesis.palette

trait Palette {
  def name: String
  def length: Int
  def getColor(index: Int): Int
}
