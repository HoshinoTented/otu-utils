import com.github.hoshinotented.osuutils.cli.action.RenderScoresAction
import com.github.hoshinotented.osuutils.database.ScoreHistoryDatabase
import com.github.hoshinotented.osuutils.io.DefaultFileIO
import org.jfree.data.DomainOrder
import org.jfree.data.general.DatasetChangeListener
import org.jfree.data.general.DatasetGroup
import org.jfree.data.xy.XYDataset
import kotlin.io.path.Path

object JustFuck : XYDataset {
  val set = arrayOf(
    doubleArrayOf(99.0, 95.0),
    doubleArrayOf(93.0, 94.0),
    doubleArrayOf(95.3)
  )
  
  override fun getDomainOrder(): DomainOrder {
    return DomainOrder.NONE
  }
  
  override fun getItemCount(series: Int): Int = 5
  
  override fun getX(series: Int, item: Int): Number {
    return getXValue(series, item)
  }
  
  override fun getXValue(series: Int, item: Int): Double {
    var setIdx = 0
    var dataIdx = 0
    
    for (i in 0..<item) {
      val current = set[setIdx]
      if (dataIdx + 1 == current.size) {
        setIdx += 1
        dataIdx = 0
      } else {
        dataIdx += 1
      }
    }
    
    return setIdx.toDouble()
  }
  
  override fun getY(series: Int, item: Int): Number {
    return getYValue(series, item)
  }
  
  override fun getYValue(series: Int, item: Int): Double {
    var setIdx = 0
    var dataIdx = 0
    
    for (i in 0..<item) {
      val current = set[setIdx]
      if (dataIdx + 1 == current.size) {
        setIdx += 1
        dataIdx = 0
      } else {
        dataIdx += 1
      }
    }
    
    return set[setIdx][dataIdx]
  }
  
  override fun getSeriesCount(): Int = 1
  
  override fun getSeriesKey(series: Int): Comparable<*> = "what"
  
  override fun indexOf(seriesKey: Comparable<*>?): Int = 0
  
  override fun addChangeListener(listener: DatasetChangeListener?) {
  }
  
  override fun removeChangeListener(listener: DatasetChangeListener?) {
  }
  
  override fun getGroup(): DatasetGroup? {
    return null
  }
  
  override fun setGroup(group: DatasetGroup?) {
  }
}

fun main() {

//  val chart = ChartFactory.createScatterPlot(
//    "my chart", "date", "accuracy", JustFuck
//  )
//
//  chart.xyPlot.domainAxis = SymbolAxis("date", arrayOf("2025", "2026", "2027"))
//
//  val what = chart.createBufferedImage(640, 480)
//  ImageIO.write(what, "png", Path("out.png").toFile())
  
  val scoreDb = ScoreHistoryDatabase(Path("./src/test/resources/beatmap_histories"), DefaultFileIO)
  RenderScoresAction(Path("out.png").toFile(), "Scores of 181253", "Hoshino Tented", scoreDb.load(181253).scores)
    .run()
}