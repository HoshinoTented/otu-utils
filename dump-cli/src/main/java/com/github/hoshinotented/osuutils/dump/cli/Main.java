package com.github.hoshinotented.osuutils.dump.cli;

import com.github.hoshinotented.osuutils.dump.*;
import com.github.hoshinotented.osuutils.dump.deser.LocalOsuParseListener;
import com.github.hoshinotented.osuutils.dump.deser.ParseKt;
import com.github.hoshinotented.osuutils.dump.deser.ParsersKt;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.gson.FormattingStyle;
import com.google.gson.stream.JsonWriter;
import kala.function.CheckedFunction;
import kotlin.jvm.JvmClassMappingKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class Main extends MainArgs implements Callable<Integer> {
  static void main(String[] args) {
    var app = new Main();
    var exit = new CommandLine(app)
        .setExecutionStrategy(app::executeStrategy)
        .execute(args);

    System.exit(exit);
  }


  public void init() {
  }

  int executeStrategy(CommandLine.ParseResult result) {
    init();
    return new CommandLine.RunLast().execute(result);
  }

  public <R> @NotNull R useLocalOsu(
      @NotNull Path file,
      @NotNull CheckedFunction<LittleEndianDataInputStream, R, IOException> block
  ) throws IOException {
    try (var stream = new LittleEndianDataInputStream(Files.newInputStream(file))) {
      return block.apply(stream);
    }
  }

  public void normalize() {
    if (this.fileType == null) {
      this.fileType = new FileType();
      this.fileType.database = true;
    }
  }

  @Override
  public Integer call() throws Exception {
    normalize();

    var fileType = Objects.requireNonNull(this.fileType);

    Class<?> type;
    if (fileType.database) {
      type = LocalOsu.class;
    } else if (fileType.score) {
      type = LocalScore.class;
    } else if (fileType.scores) {
      type = LocalScores.class;
    } else if (fileType.collection) {
      type = LocalCollection.class;
    } else {
      throw new UnsupportedOperationException("unreachable");
    }

    Path filePath = target;

    try (var out = new JsonWriter(new OutputStreamWriter(System.out))) {
      if (this.pretty) {
        out.setFormattingStyle(FormattingStyle.PRETTY);
      }

      var exitCode = useLocalOsu(filePath, in -> {
        try {
          new DumpToJson(in, out, binary)
              .read(type);
        } catch (OsuParseException e) {
          System.err.println(e.getMessage());
          return 1;
        }

        return 0;
      });

      return exitCode;
    }
  }
}
