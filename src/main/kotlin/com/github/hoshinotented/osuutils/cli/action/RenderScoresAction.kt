package com.github.hoshinotented.osuutils.cli.action

import com.github.hoshinotented.osuutils.api.endpoints.Score
import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableList
import kala.collection.mutable.MutableMap
import org.jfree.chart.ChartFactory
import org.jfree.chart.axis.SymbolAxis
import org.jfree.data.DomainOrder
import org.jfree.data.general.DatasetChangeListener
import org.jfree.data.general.DatasetGroup
import org.jfree.data.xy.XYDataset
import java.awt.Color
import java.awt.Font
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.time.toJavaInstant

class RenderScoresAction(val outFile: File, val title: String, val userName: String, val scores: ImmutableSeq<Score>) :
  Runnable {
  data class Dataset(val key: Instant, val data: ImmutableSeq<Float>)
  
  class MyDataset(val datasets: ImmutableSeq<Dataset>, val categoryName: String) : XYDataset {
    val itemCount by lazy {
      datasets.fold(0) { acc, ds -> acc + ds.data.size() }
    }
    
    val idxCache by lazy {
      ImmutableSeq.from(
        datasets
          .scan(0) { acc, score ->
            acc + score.data.size()
          })
    }
    
    override fun getDomainOrder(): DomainOrder = DomainOrder.NONE
    override fun getItemCount(series: Int): Int = itemCount
    
    // find which set [item]-th data lives in
    override fun getXValue(series: Int, item: Int): Double {
      val what = idxCache.binarySearch(item)
      if (what >= 0) return what.toDouble()
      
      val insertPoint = -what - 1
      // this means [item] is belongs to [insertPoint - 1]-th set
      return (insertPoint - 1).toDouble()
    }
    
    override fun getYValue(series: Int, item: Int): Double {
      val idx = getXValue(series, item).toInt()
      val setBegin = idxCache.get(idx)
      val indexInSet = item - setBegin
      
      return datasets.get(idx).data.get(indexInSet).toDouble()
    }
    
    override fun getX(series: Int, item: Int): Number = getXValue(series, item)
    override fun getY(series: Int, item: Int): Number = getYValue(series, item)
    override fun getSeriesCount(): Int = 1
    override fun getSeriesKey(series: Int): Comparable<*> = categoryName
    override fun indexOf(seriesKey: Comparable<*>?): Int = 0
    override fun addChangeListener(listener: DatasetChangeListener?) = Unit
    override fun removeChangeListener(listener: DatasetChangeListener?) = Unit
    override fun getGroup(): DatasetGroup? = null
    override fun setGroup(group: DatasetGroup?) = Unit
  }
  
  override fun run() {
    val group = MutableMap.create<Instant, MutableList<Float>>()
    
    scores.forEach { score ->
      val key = score.createdAt.toJavaInstant().truncatedTo(ChronoUnit.DAYS)
      group.getOrPut(key) { MutableList.create() }
        .append(score.accuracy)
    }
    
    val what = group.toSeq().view()
      .map { Dataset(it.component1, it.component2.toSeq()) }
      .sorted { l, r -> l.key.compareTo(r.key) }
      .toSeq()
    
    // rendering
    render(what)
  }
  
  /**
   * @param datasets ordered data set
   */
  fun render(datasets: ImmutableSeq<Dataset>) {
    val chart = ChartFactory.createScatterPlot(title, "_", "accuracy", MyDataset(datasets, userName))
    val dates = datasets.map {
      DateTimeFormatter.ISO_LOCAL_DATE
        .toFormat()
        .format(LocalDateTime.ofInstant(it.key, ZoneId.systemDefault()))
    }
    
    // https://stackoverflow.com/questions/9767201/jfreechart-with-string-and-double
    chart.xyPlot.domainAxis = SymbolAxis("date", dates.toArray(String::class.java)).apply {
      isGridBandsVisible = false
    }
    
    chart.xyPlot.isDomainGridlinesVisible = false
    chart.xyPlot.rangeGridlinePaint = Color.gray
    chart.xyPlot.backgroundPaint = Color.white
    
    ImageIO.write(chart.createBufferedImage(max(680, 100 + 60 * datasets.size()), 420), "png", outFile)
  }
}