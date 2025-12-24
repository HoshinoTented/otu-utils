import kotlin.math.floor
import kotlin.random.Random

const val barMax = 20

fun printProgress(now: Int, max: Int) {
  val bar = floor((now.toDouble() / max) * 20).toInt()
  print("[ $now / $max ] |")
  
  for (i in 0 until 20) {
    if (i < bar) {
      print("=")
    } else {
      print(" ")
    }
  }
  
  print("|\r")
}

fun main() {
  val random = Random(System.currentTimeMillis())
  
  for (i in 0..100) {
    printProgress(i, 100)
    
    Thread.sleep(random.nextLong(100, 500))
  }
  
  println("Hello, world!")
}