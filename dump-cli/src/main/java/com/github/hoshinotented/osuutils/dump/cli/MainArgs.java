package com.github.hoshinotented.osuutils.dump.cli;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;

@CommandLine.Command(
    name = "osu-dump"
)
public abstract class MainArgs {
  @CommandLine.Parameters(paramLabel = "FILE", description = "The database file to dump")
  @NotNull Path target;

  @CommandLine.ArgGroup()
  @Nullable FileType fileType;

  @CommandLine.Option(names = "--pretty", negatable = true, defaultValue = "true")
  boolean pretty = true;

  @CommandLine.Option(
      names = "--binary",
      description = "Whether print binary data as number list. If set to false, binary data will be represented by the size of the data.",
      negatable = true, defaultValue = "false")
  boolean binary = false;

  static class FileType {
    @CommandLine.Option(names = "--database", description = "Treat the input file as an 'osu!.db' file. Default.")
    boolean database;
    @CommandLine.Option(names = "--scores", description = "Treat the input file as a 'score.db' file.")
    boolean scores;
    @CommandLine.Option(names = "--score", description = "Treat the input file as a '.osr' file.")
    boolean score;
    @CommandLine.Option(names = "--collection", description = "Treat the input file as a 'collection.db' file.")
    boolean collection;
  }
}
