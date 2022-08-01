package com.arya.filestore

import cats.effect.{Concurrent, IO}
import cats.implicits.toFunctorOps
import fs2.io.file.{Files, Path}
import fs2.{Pipe, Pull, Stream, text}

class Fs2File(path: Path) {

  def validateChunkData(string: String, key: String): Boolean = string.split(regex).head.equals(key)

  def replaceKeyOrAppend[F[_]](key: String, value: String): Pipe[F, String, String] = {
    def go(stream: Stream[F, String], insertedOrReplace: Boolean, key: String, value: String): Pull[F, String, Unit] = {
      stream.pull.uncons.flatMap {

          case None => if(insertedOrReplace) Pull.done else Pull.output1(s"$key${"$$"}$value") >> Pull.done
          case Some((chunk, stream)) => if(insertedOrReplace){
            Pull.output(chunk) >> go(stream, insertedOrReplace, key, value)
          } else {
            var replace = false
            Pull.output(chunk.map(str => if(validateChunkData(str, key)) {
              replace = true
              s"$key${"$$"}$value"
            }else str)) >> go(stream, insertedOrReplace=replace, key, value)
          }

      }
    }

    s => Stream.suspend(go(s, insertedOrReplace = false, key, value).stream)
  }

  private val regex = "\\$\\$"

  def backup[F[_] : Files : Concurrent]: F[Path] = {
    val backupF: Path = Path(path.toString + ".backup")

    Files[F].readAll(path).through(Files[F].writeAll(backupF))
      .compile
      .drain
      .as(backupF)
  }

  def getAll[F[_] : Files : Concurrent, K, V](key: K): F[List[V]] = {
    Files[F].readAll(path)
      .through(text.utf8.decode)
      .through(text.lines) //.debug()
      .filter(x => x.split(regex).head.asInstanceOf[K].equals(key))
      .compile
      .toList.map(x => x.map(_.split(regex)(1)).map(_.asInstanceOf[V]))
  }

  def getOne[F[_] : Files : Concurrent, K, V](key: K): F[Option[V]] = getAll[F, K, V](key).map(x => x.headOption)

  def putOne[F[_] : Files : Concurrent, K, V](key: K, value: V): F[Unit] = {
    val newFile = Path(path.toString + ".new")
    val restoreToNewFile = Files[F].readAll(path)
      .through(text.utf8.decode)
      .through(text.lines)
      .through(replaceKeyOrAppend(key.toString, value.toString))
      .intersperse("\n")
      .through(text.utf8.encode)
      .through(Files[F].writeAll(newFile))

    val restoreFromNewFile2 = Files[F].readAll(newFile) // Read from new file
      .through(Files[F].writeAll(path)) // Write back to new file

    (restoreToNewFile ++ restoreFromNewFile2).compile.drain

  }

  def delete[F[_] : Files : Concurrent, K](key: K): F[Unit] = {
    val newFile = Path(path.toString + ".new")
    val restoreToFile = Files[F].readAll(path)
      .through(text.utf8.decode)
      .through(text.lines)
      .filter(x => !x.split(regex).head.asInstanceOf[K].equals(key))
      .intersperse("\n")
      .through(text.utf8.encode)
      .through(Files[F].writeAll(newFile))

    val restoreFromNewFile = Files[F].readAll(newFile) // Read from new file
      .through(Files[F].writeAll(path)) // Write back to new file

    (restoreToFile ++ restoreFromNewFile).compile.drain
  }
}

