package de.sciss.foo

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds._

import akka.actor.ActorSystem
import com.beachape.filemanagement.Messages.RegisterCallback
import com.beachape.filemanagement.MonitorActor
import com.beachape.filemanagement.RegistryTypes.Callback

/** Warning: creates (or overwrites) `~/Desktop/test` and `~/Desktop/test/foo`. */
object SomeTests {
  def main(args: Array[String]): Unit = run()

  def run(): Unit = {
    val home        = sys.props("user.home")
    val desktop     = Paths.get(home + "/Desktop")
    val subDir      = Paths.get(home + "/Desktop/test")
    val testFile    = Paths.get(home + "/Desktop/test/foo")

    testFile.toFile.delete()
    subDir  .toFile.delete()

    implicit val system: ActorSystem = ActorSystem("actorSystem")
    val fileMonitorActor = system.actorOf(MonitorActor(concurrency = 1))

    val modifyCallbackFile: Callback = {
      path => println(s"Something was modified in a file: $path")
    }
    val createCallbackFile: Callback = {
      path => println(s"File was created: $path")
    }
    val modifyCallbackDirectory: Callback = {
      path => println(s"Something was modified in a directory: $path")
    }
    val createCallbackDirectory: Callback = {
      path => println(s"Something was created in a directory: $path")
    }

    /*
      This will receive callbacks for just the one file
     */
    fileMonitorActor ! RegisterCallback(
      event = ENTRY_MODIFY,
      path = testFile,
      callback =  modifyCallbackFile
    )
    fileMonitorActor ! RegisterCallback(
      event = ENTRY_CREATE,
      path = testFile,
      callback =  createCallbackFile
    )

    /*
      If desktopFile is modified, this will also receive a callback
      it will receive callbacks for everything under the desktop directory
    */
    fileMonitorActor ! RegisterCallback(
      event = ENTRY_MODIFY,
      path = desktop,
      callback = modifyCallbackDirectory
    )
    fileMonitorActor ! RegisterCallback(
      event = ENTRY_CREATE,
      path = desktop,
      callback = createCallbackDirectory
    )

    println("Wait 4s.")
    Thread.sleep(4000)

    println("Make sub-dir")
    subDir.toFile.mkdirs()

    println("Wait 4s.")
    Thread.sleep(4000)

    println("Write file")
    //modify a monitored file
    val writer = new BufferedWriter(new FileWriter(testFile.toFile))
    writer.write("There's text in here: " + new java.util.Date)
    writer.close()

    // #=> Something was modified in a file: /Users/a13075/Desktop/test.txt
    //     Something was modified in a directory: /Users/a13075/Desktop/test.txt

    println("Wait 4s.")
    Thread.sleep(4000)
    sys.exit()
  }
}
