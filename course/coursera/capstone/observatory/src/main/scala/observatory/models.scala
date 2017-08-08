package observatory

import java.time.LocalDate

import com.sksamuel.scrimage.RGBColor

import scala.annotation.tailrec
import scala.collection.{GenIterable, GenSeqLike}
import scala.math._



case class Join(date: Date, location: Location, temperature: Double) {
  def res = (date.toLocalDate(), location, temperature)
}

object Date {
  def apply(date: LocalDate) = new Date(date.getYear, date.getMonth.getValue, date.getDayOfMonth)
}
final case class Date(year: Int, month: Int, day: Int) {
  def toLocalDate() = LocalDate.of(year, month, day)
}

final case class StationId(stn: String, wban: String)

final case class Station(id: StationId, location: Location)

final case class Temperature(stationId: StationId, date: Date, temperature: Double)

object Location {
  def distance(location1: Location, location2: Location): Double = {
    val r = 6371000 // radius of Earth in m

    math.abs(r * Δσ(location1, location2))
  }

  def apply(width: Int, height: Int, maxWidth: Int, maxHeight: Int): Location = {
    val widthRadio = 360 / maxWidth.toDouble
    val heightRatio = 180 / maxHeight.toDouble

    Location(90 - (height * heightRatio), (width * widthRadio) - 180)
  }

  private def Δλ(location1: Location, location2: Location) = location1.λ - location2.λ

  private def Δσ(location1: Location, location2: Location): Double = {
    acos(sin(location1.φ) * sin(location2.φ) + cos(location1.φ) * cos(location2.φ) * cos (Δλ(location1, location2)))
  }

}

final case class Location(lat: Double, lon: Double) {

  def φ = toRadians(lat)
  def λ = toRadians(lon)

}

object Color {
  private def normalize(component: Int) = {
    if (component < 0) 0
    else if (component > 255) 255
    else component
  }

  def withNormalization(red: Int, green: Int, blue: Int) = Color(normalize(red), normalize(green), normalize(blue))

}

final case class Color(red: Int, green: Int, blue: Int) {
  require(0 <= red && red <= 255, "Red component is invalid")
  require(0 <= green && green <= 255, "Green component is invalid")
  require(0 <= blue && blue <= 255, "Blue component is invalid")

  private val alpha = 127

  def pixel = RGBColor(red, green, blue, alpha).toPixel
}


final case class Tile(zoom: Int, x: Int, y: Int) {

  lazy val location = toLocation()

  def zoomIn(newZoom: Int): GenIterable[Tile] = {
    require(newZoom >= zoom)

    @tailrec
    def loop(curZoom: Int, tiles: GenIterable[Tile]): GenIterable[Tile] = {
      if (curZoom == newZoom)
        tiles
      else
        loop(curZoom + 1, tiles.flatMap(_.zoomInOnce))
    }

    loop(zoom, List(this).par)
  }

  def zoomInOnce() = {
    val newZoom = zoom + 1
    Tile(newZoom, 2*x, 2*y) ::
      Tile(newZoom, 2*x + 1, 2*y) ::
      Tile(newZoom, 2*x, 2*y + 1) ::
      Tile(newZoom, 2*x + 1, 2*y + 1) ::
      Nil
  }

  def uri = new java.net.URI("http://tile.openstreetmap.org/" + zoom + "/" + x + "/" + y + ".png")

  def toLocation(): Location = {
    val n = (1 << zoom)

    require(x >= 0 && x <= n - 1)
    require(y >= 0 && y <= n - 1)

    val lat = toDegrees(atan(sinh(Pi * (1.0d - 2.0d * y / n))))
    val lon = 360.0d * x / n - 180.0d

    Location(lat, lon)

  }
}
