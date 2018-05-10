package org.synthesis.skeleton

import com.koloboke.collect.map.hash.{HashLongIntMap, HashLongIntMaps, HashLongObjMap, HashLongObjMaps}
import com.koloboke.function.{LongConsumer, LongIntConsumer}
import de.ummels.prioritymap.PriorityMap

import scala.collection.mutable

// http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.187.4455&rep=rep1&type=pdf
case class Skeleton(resample: Int = 1, disThreshold: Int = 2000, difThreshold: Int = 100, poiCount: Int = 6, debug: Boolean = false) {
  type Graph = HashLongObjMap[HashLongIntMap]

  val minSpread = 100

  def track(depthData: Array[Int], width: Int, height: Int): List[Body] = {
    val t0 = System.currentTimeMillis()

    val depthDataR = resample(depthData, width, height)
    val widthR = width / resample
    val heightR = height / resample

    val graph = createGraph(depthDataR, widthR, heightR)


    if (debug) {
      println(s"Graph creation done, took ${System.currentTimeMillis() - t0} millis")
    }

    val t1 = System.currentTimeMillis()

    // Find Centroid
    // Ignoring z for now here
    val centroids = findCentroids(graph, widthR, heightR)

    if (debug) {
      println(s"Cendroid finding done, took ${System.currentTimeMillis() - t1} millis")
    }

    val t2 = System.currentTimeMillis()

    val result = centroids.flatMap {
      case (cx, cy) =>
        // Find AGEX, Accumulative Geodesic EXtrema
        val agex = findAgex(graph, cx, cy, widthR)

        val agexWithDepth = agex.map { case (x, y) =>
          val depth = depthData(x * resample + y * resample * width)
          (x, y, depth)
        }

        val cDepth = depthData(cx * resample + cy * resample * width)

        // Return the result
        Body.create(agexWithDepth, cx, cy, cDepth, resample)
    }

    if (debug) {
      println(s"AGEX finding done, took ${System.currentTimeMillis() - t2} millis")
    }

    result
  }

  private def resample(depthData: Array[Int], width: Int, height: Int): Array[Int] = {
    if (resample == 1) {
      depthData
    } else {
      (0 until height by resample).flatMap { y =>
        (0 until width by resample).map { x =>
          depthData(x + y * width)
        }
      }.toArray
    }
  }

  @inline
  private def key(x: Int, y: Int, width: Int): Long = {
    (x + y * width).toLong
  }

  @inline
  private def key(p: (Int, Int), width: Int): Long = {
    key(p._1, p._2, width)
  }

  @inline
  private def unkey(key: Long, width: Int): (Int, Int) = {
    ((key % width).toInt, (key / width).toInt)
  }

  private def createGraph(depthData: Array[Int], width: Int, height: Int): Graph = {
    val graph = HashLongObjMaps.newMutableMap[HashLongIntMap](width * height / resample)


    // Create Graph of nodes (x, y) to edges defined by endpoint (nx, ny) and depth
    for(x <- 0 until width;
        y <- 0 until height) yield {
      val depth = depthData(x + y * width)

      // Threshold, if farther away ignore
      if (depth < disThreshold && depth != 0) {
        val neighs = neighbors(x, y, width, height)
        val innerMap = graph.getOrDefault(key(x, y, width), HashLongIntMaps.newMutableMap())

        // for each neighbor, check if difference in distance is less than our difference threshold, if so add to map
        neighs.foreach { case (nx, ny) =>
          val diff = distance(x, y, depth, nx, ny, depthData(nx + ny * width))
          if (diff < difThreshold) {
            innerMap.addValue(key(nx, ny, width), diff)
          }
        }

        graph.put(key(x, y, width), innerMap)
      }
    }

    graph
  }

  private def findCentroid(validPoints: Iterable[Long], width: Int, height: Int): Option[(Int, Int)] = {
    var x_avg = 0
    var y_avg = 0
    var count = 0

    val pairs = validPoints.map(p => unkey(p, width))

    for((x, y) <- pairs) yield {
      x_avg += x
      y_avg += y
      count += 1
    }

    if (count == 0) {
      None
    } else {
      x_avg = x_avg / count
      y_avg = y_avg / count

      Some(pairs.minBy { case (x, y) =>
        val dx = x - x_avg
        val dy = y - y_avg
        Math.sqrt(dx * dx + dy * dy)
      })
    }
  }

  private def findCentroids(graph: Graph, width: Int, height: Int): List[(Int, Int)] = {
    val graphInternal: HashLongObjMap[HashLongIntMap] = HashLongObjMaps.newMutableMap[HashLongIntMap](graph)

    val regions = mutable.ListBuffer[mutable.ListBuffer[Long]]()

    while(graphInternal.size > 0) {
      val first: Long = graphInternal.keySet().toLongArray.head
      val list = mutable.ListBuffer[Long]()
      val nexts = mutable.Queue[Long]()

      graphInternal.remove(first).keySet().forEach(new LongConsumer { def accept(l: Long) { nexts.enqueue(l)}})
      while(nexts.nonEmpty) {
        val next = nexts.dequeue()
        val map = graphInternal.remove(next)
        if (map != null) {
          map.keySet().forEach(new LongConsumer { def accept(l: Long) { nexts.enqueue(l)}})
        }
        list += next
      }
      regions += list
    }

    regions.toList.flatMap(findCentroid(_, width, height))
  }

  private def findAgex(graph: Graph, cx: Int, cy: Int, width: Int): List[(Int, Int)] = {
    val agex: mutable.ListBuffer[Long] = mutable.ListBuffer(key(cx, cy, width))

    for(_ <- 1 until poiCount) {
      val r = dijkstra3(graph)(key(cx, cy, width))

      val newPoi = r._1.maxBy(_._2)

      agex += newPoi._1

      val innerMap = graph.getOrDefault(key(cx, cy, width), HashLongIntMaps.newMutableMap())
      innerMap.addValue(newPoi._1, 0)
      graph.put(key(cx, cy, width), innerMap)
    }

    agex.map(p => unkey(p, width)).toList
  }

  private def dijkstra3[N](g: Graph)(source: Long): (mutable.LongMap[Int], mutable.LongMap[Long]) = {

    def go(active: PriorityMap[Long, Int], res: mutable.LongMap[Int], pred: mutable.LongMap[Long]): (mutable.LongMap[Int], mutable.LongMap[Long])  = {
      if (active.isEmpty) (res, pred)
      else {
        val (node, cost) = active.head
        g.get(node) match {
          case nodes: Any =>
            val neighbours = mutable.LongMap[Int]()

            nodes.forEach(new LongIntConsumer {
              override def accept(n: Long, c: Int) = {
                if (!res.contains(n) && cost + c < active.getOrElse(n, Int.MaxValue)) {
                  neighbours.put(n, cost + c)
                }
              }
            })

            val preds = neighbours.mapValues (_ => node)
            res += (node -> cost)
            pred ++= preds
            go(active.tail ++ neighbours, res, pred)
          case _ =>
            res += (node -> cost)
            go(active.tail, res, pred)
        }
      }
    }

    go(PriorityMap(source -> 0), mutable.LongMap.empty, mutable.LongMap.empty)
  }

  private def neighbors(x: Int, y: Int, width: Int, height: Int): List[(Int, Int)] = {
    val seq = for(nx <- x - 1 to x + 1;
                  ny <- y - 1 to y + 1) yield {
      (nx, ny)
    }

    seq.filter { case (nx, ny) => nx >= 0 && ny >= 0 && nx < width && ny < height && (nx != x || ny != y) }.toList
  }

  // right now this is just depth distance, should probably be 3d space distance
  // https://openkinect.org/wiki/Imaging_Information
  private def distance(x: Int, y: Int, depth: Int, nx: Int, ny: Int, nDepth: Int): Int = {
    Math.abs(depth - nDepth)
  }

  // wrong
  private def xySpace(x: Int, y: Int, depth: Int): (Int, Int) = {
    (((x - 58 / 2) * (depth / 1000 - 10) * 2.1).toInt,
      ((y - 45 / 2) * (depth / 1000 - 10) * 2.1).toInt)
  }
}
