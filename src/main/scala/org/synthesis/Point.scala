package org.synthesis

/**
  * Created by Brown on 6/19/17.
  */
case class Point(x: Int, y: Int) {
  def inside(wall: Wall): Boolean = {
    y >= 0 && x >= 0  && y < wall.pixelsY && x < wall.pixelsX
  }

  def swap: Point = {
    Point(this.y, this.x)
  }
}