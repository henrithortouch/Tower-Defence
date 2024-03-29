
package TD

import java.io.{ByteArrayInputStream, File, InputStream}
import javax.sound.sampled.{AudioFormat, AudioInputStream, AudioSystem, DataLine, SourceDataLine}

import scala.collection.mutable.{ArrayBuffer}

/**
 * Helper object for manipulating stereo sound data. Contains methods for separating and mixing back together left and
 * right audio channels.
 */
object StereoSoundHelper {

  /**
   * Works correctly only for little-endian 16-bit sound data with two channels.
   *
   * Takes a path to a sound file as a parameter and gets `AudioInputStream` for that file to read the data. The data in
   * 16-bit stereo data is stored such as two bytes represent each sample with every other sample of left followed by
   * right. So there would be "part one of first left sample", "part two of first left sample", "part one of first right
   * sample", "part two of first right sample", "part one of second left sample", "part two of second left sample", and
   * so on.
   *
   * So two bytes are used to create the int value for the returned array. The bytes in the "wrong" channel are skipped
   * over.
   *
   * @param inputFile The path to file from where the sound data is read.
   * @param getLeft `true` when the left channel is requested, `false` for right
   * @return An `ArrayBuffer` containing bit values as integers for requested channel.
   */
  private def getChannel(inputFile: String, getLeft:Boolean) : ArrayBuffer[Int] = {
    val fileIn: File = new File(inputFile)                              // file from where sound data is read
    try {
      var ais: AudioInputStream = AudioSystem.getAudioInputStream(fileIn)

      val BUFFER_SIZE:Int = ais.getFormat().getSampleSizeInBits()       // amount of bytes to read each time
      var bytes:Array[Byte] = new Array(BUFFER_SIZE)                    // array to hold the read bytes
      val ints: ArrayBuffer[Int] = ArrayBuffer[Int]()                   // array to save the channels integer data to

      var bytesRead:Int = ais.read(bytes)                               // read the bits from 'ais' to 'bytes' array
                                                                        // bytesRead should be BUFFER_SIZE until ais is
                                                                        // empty and read returns -1

      var newValue:Int = 0                                                // value for the int to store
      while (bytesRead != -1) { // so as long as ais holds bits for us to read
        for (i <- 0 until bytes.length by 2) { // because our data is 16-bit we use two bytes for each int
          if (getLeft && i % 4 == 0) {
            newValue = bytes(i + 1) << 8 | bytes(i) & 0xFF
            ints.append(newValue)
          } else if (!getLeft && i % 4 != 0) {
            newValue = bytes(i + 1) << 8 | bytes(i) & 0xFF
            ints.append(newValue)
          }
        }
        bytesRead = ais.read(bytes) // read the next bytes
      }

      ais.close                                                           // close the stream
      ints                                                                // and return the channel as int array
    } catch {
      case noFile: java.io.FileNotFoundException =>
        println("Tätä tiedostoa ei ole olemassa, tarkista antamasi tiedostopolku.")
        ArrayBuffer[Int](-1)
      case unsupported: javax.sound.sampled.UnsupportedAudioFileException =>
        println("Annettu audiotiedosto ei ole vaaditun formaatin mukainen.")
        ArrayBuffer[Int](-1)
    }
  }

  /**
   * Works correctly only for little-endian 16-bit stereo sound data.
   *
   * Takes a path to a sound file as a parameter and returns data for the left channel as an array buffer.
   *
   * @param inputFile The path to file from where the sound data is read.
   * @return An `ArrayBuffer` containing bit values as integers for the left channel.
   */
  def getLeftChannel(inputFile: String) : ArrayBuffer[Int] = getChannel(inputFile, true)

  /**
   * Works correctly only for little-endian 16-bit stereo sound data.
   *
   * Takes a path to a sound file as a parameter and returns data for the right channel as an array buffer.
   *
   * @param inputFile The path to file from where the sound data is read.
   * @return An `ArrayBuffer` containing bit values as integers for the right channel.
   */
  def getRightChannel(inputFile: String) : ArrayBuffer[Int] = getChannel(inputFile, false)

  /**
   * Works correctly only for little-endian 16-bit stereo sound data and when data for the left and right channels are
   * equally long.
   *
   * Takes audio data as integer arrays for left and right channels, breaks the integers back to bytes and returns a new
   * array containing the byte data that can be used for saving or playing audio.
   *
   * @param left The audio data for the left channel.
   * @param right The audio data for the right channel.
   * @return The sound as an array of bytes, which can be used to play or save audio to file.
   */
  def mixChannels(left: ArrayBuffer[Int], right: ArrayBuffer[Int]) : Array[Byte] = {
    require(left.length == right.length)

    val bytes: Array[Byte] = new Array[Byte]((left.length + right.length) * 2)

    var channelIndex = 0
    for (i <- 0 until bytes.length by 4) {
      bytes(i) = (left(channelIndex) & 0xFF).asInstanceOf[Byte]
      bytes(i + 1) = (left(channelIndex) >> 8).asInstanceOf[Byte]
      bytes(i + 2) = (right(channelIndex) & 0xFF).asInstanceOf[Byte]
      bytes(i + 3) = (right(channelIndex) >> 8).asInstanceOf[Byte]
      channelIndex += 1
    }

    bytes
  }

}
