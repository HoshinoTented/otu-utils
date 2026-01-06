package com.github.hoshinotented.osuutils.util

/**
 * `{title} [ current / max ] |ratio visualize| {subtitle}`
 */
interface ProgressIndicator {
  /**
   * @param ratio the progress of current task, must between `0.0` and `1.0`, no progress bar if [Double.NaN]
   */
  fun progress(ratio: Double, title: String?, subtitle: String?)
  
  /**
   * @param current current processing, from `0` to [max]
   */
  fun progress(current: Int, max: Int, title: String?, subtitle: String?) {
    progress(current.toDouble() / max, title, subtitle)
  }
  
  object Console : ProgressIndicator {
    const val MAX_BAR_COUNT = 20
    
    /**
     * @param current can be -1
     * @param max ditto
     */
    fun doProgress(current: Int, max: Int, ratio: Double, title: String?, subtitle: String?) {
      if (title != null) {
        print("$title ")
      }
      
      if (current != -1 && max != -1) {
        print("[ $current / $max ] ")
      } else {
        print("[ ? / ? ] ")
      }
      
      // build `|========     |`
      val barCount = if (ratio.isNaN()) MAX_BAR_COUNT else (ratio * MAX_BAR_COUNT).toInt()
      val bars = "=".repeat(barCount) + " ".repeat(MAX_BAR_COUNT - barCount)
      print("|$bars|")
      
      if (subtitle != null) print(" $subtitle")
      
      print('\r')
    }
    
    override fun progress(ratio: Double, title: String?, subtitle: String?) {
      if (ratio.isNaN()) {
        print("[ ? / ? ] $subtitle\r")
      } else {
        doProgress(-1, -1, ratio, title, subtitle)
      }
    }
    
    override fun progress(current: Int, max: Int, title: String?, subtitle: String?) {
      doProgress(current, max, current.toDouble() / max, title, subtitle)
    }
  }
  
  object Empty : ProgressIndicator {
    override fun progress(ratio: Double, title: String?, subtitle: String?) {
    }
    
    override fun progress(current: Int, max: Int, title: String?, subtitle: String?) {
    }
  }
}

class AccumulateProgressIndicator(val delegate: ProgressIndicator) : ProgressIndicator by delegate {
  private var title: String? = null
  
  // current progress
  private var acc: Int = 0
  
  // max progress
  private var max: Int = 0
  
  fun init(max: Int, title: String?, subtitle: String?) {
    this.acc = 0
    this.max = max
    
    if (max != 0) {
      this.title = title
      progress(0, max, title, subtitle)
    }
  }
  
  fun progress(subtitle: String?) {
    if (acc < max) {
      // acc + 1 <= max
      progress(++acc, max, title, subtitle)
    } else {
      throw IllegalStateException("Current counter: $acc while max at $max")
    }
  }
  
  fun update(subtitle: String?) {
    progress(acc, max, title, subtitle)
  }
}