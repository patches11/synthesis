package org.synthesis

object Utils {

  implicit class Rep(n: Int) {
    def times[A](f: => A) {
      0 until n foreach (_ => f)
    }

    def times[A](f: (Int) => A) {
      0 until n foreach (i => f(i))
    }
  }

  def distance(x: Double, y: Double, x2: Double, y2: Double): Double = {
    Math.sqrt((x2 - x) * (x2 - x) + (y2 - y) * (y2 - y))
  }

  def rotate(point: (Double, Double), origin: (Double, Double), angle: Double): (Double, Double) = {
    val (x, y) = point
    val (ox, oy) = origin

    val oAngle = Math.atan2(oy - y, x - ox)

    val newAngle = oAngle + angle

    val dis = distance(x, y, ox, oy)

    val (nx, ny) = (dis * Math.cos(newAngle), dis * Math.sin(newAngle))

    (
      nx + ox,
      oy - ny
    )
  }
}


case object AfterTheLastMessage