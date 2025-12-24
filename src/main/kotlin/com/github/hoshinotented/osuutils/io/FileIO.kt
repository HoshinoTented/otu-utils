package com.github.hoshinotented.osuutils.io

import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

interface FileIO {
  fun writeText(path: Path, content: String, charset: Charset = Charsets.UTF_8)
  fun readText(path: Path, charset: Charset = Charsets.UTF_8): String {
    return path.readText(charset)
  }
  
  fun exists(path: Path): Boolean {
    return path.exists()
  }
}

object DefaultFileIO : FileIO {
  override fun writeText(path: Path, content: String, charset: Charset) {
    path.writeText(content, charset)
  }
}

object DryFileIO : FileIO {
  val logger = Logger.getLogger("DryRun")
  
  override fun writeText(path: Path, content: String, charset: Charset) {
    logger.info("Writing to $path")
    logger.fine(content)
  }
}