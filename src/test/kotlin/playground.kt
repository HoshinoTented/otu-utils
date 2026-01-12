import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.toJavaInstant

fun main() {
  val now = Clock.System.now()
  DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss")
    .withZone(ZoneId.systemDefault())
    .toFormat()
    .format(now.toJavaInstant())
    .let(::println)
}