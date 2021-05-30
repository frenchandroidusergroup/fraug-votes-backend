package fraug.votes.backend.test

import fraug.votes.backend.model.adapter
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import org.junit.Test
import org.yaml.snakeyaml.Yaml
import java.io.File

class CreateData {
  val yaml = Yaml()
  val okHttpClient = OkHttpClient.Builder()
    .followRedirects(false)
    .build()

  @Test
  fun createData() {
    val inputDirectory = File("../../asg-conferences/_conferences")

    var id = 0
    val conferences = inputDirectory.listFiles()!!
      //.take(1)
      .filter { it.extension == "md" }
      .map {
        it.toMap(id++)
      }

    File("src/main/resources/data.json").outputStream().sink().buffer().use {
      adapter.toJson(it, conferences)
    }
    println(conferences)
  }

  private fun File.toMap(id: Int): Map<String, Any?> {
    val content = readLines().drop(1).dropLast(1).joinToString("\n").byteInputStream()

    val map:MutableMap<String, Any?> = yaml.load(content)

    val pic = Request.Builder().get().url("https://picsum.photos/200").build()
      .let {
        okHttpClient.newCall(it).execute()
      }.header("Location")

    map.put("id", id.toString())
    map.put("pic", pic)

    return map
  }
}