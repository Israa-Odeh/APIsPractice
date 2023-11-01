
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import play.api.libs.json._

import java.io.{File, FileWriter, PrintWriter}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.io.StdIn.readLine

object WeatherAPI_GET {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer.type = ActorMaterializer
  import actorSystem.dispatcher

  // Declare both fileWriter and printWriter as global variables within this object's scope.
  private var fileWriter: FileWriter = _
  private var printWriter: PrintWriter = _

  // A function to create a file with a specific name.
  private def createFile(inputFileName: String): Unit = {

    val fileName = inputFileName
    val file = new File(fileName)

    if (file.exists()) {
      file.delete()
    }
    fileWriter = new FileWriter(file, true)
    printWriter = new PrintWriter(fileWriter)

  }

  // A function to display the available commands for the user.
  private def displayCommands(): Unit = {
    println("\n------------------------------------------------------------")
    println("This is the list of commands numbers that you can deal with:")
    println("------------------------------------------------------------")
    println("1: To look up the weather of a particular city.")
    println("---------------------------------------------------------")
    println("2: To print the weather for a list of Palestinian cities.")
    println("---------------------------------------------------------")
    println("3: To terminate the program.")
    println("---------------------------------------------------------")
  }

  // A function to ensure that the passed city name is of a valid format.
  private def checkNamePattern(inputCityName: String): Boolean = {
    val namePattern = "^[A-Za-z\\s-]+$".r
    namePattern.pattern.matcher(inputCityName).matches()
  }

  // A function to read the user input and handle the required response accordingly.
  private def readUserCommand(): Unit = {
    print("\nPlease enter the number of the command you want: ")
    val userInput = readLine()
    if(userInput.equals("1")) {
      var userCity = ""
      while(userCity.isBlank) {
        print("Please enter the city name: ")
        userCity = readLine()
        if(!checkNamePattern(userCity)) {
          println("Invalid city name format!")
          userCity = ""
        }
      }
      createFile(s"$userCity Weather Info.txt")
      println("Please be patient; your request is being processed.")
      val resultFuture: Future[String] = sendRequest(requestCityWeather(userCity))
      resultFuture.onComplete {
        case scala.util.Success(result) =>
          val parsedJson = Json.parse(result)
          displayAllDetails(parsedJson)
        case scala.util.Failure(exception) => println(s"An error occurred: ${exception.getMessage}")
      }
      Thread.sleep(1000) // Sleep for a second to ensure the response is received.
      printWriter.close() // Close the PrintWriter when done writing.
      fileWriter.close() // Close the FileWriter also.
      println(s"Your request info is saved in a text file named '$userCity Weather Info.txt'.")
    }
    else if(userInput.equals("2")) {
      createFile("Palestinian Cities Weather.txt")
      println("Please be patient; your request is being processed.")
      val citiesOfPalestine: ArrayBuffer[String] = ArrayBuffer(
        "Jerusalem", "Gaza-Palestine", "Nablus", "Bethlehem-Palestine", "Hebron-Palestine", "Jenin-Palestine",
        "Ramallah", "Jericho-Palestine", "Khan-Yunis", "Rafah", "Ramla", "Yafa")

      for (i <- citiesOfPalestine.indices) {
        val resultFuture: Future[String] = sendRequest(requestCityWeather(citiesOfPalestine(i)))
        resultFuture.onComplete {
          case scala.util.Success(result) =>
            val parsedJson = Json.parse(result)
            displayAllDetails(parsedJson)
          case scala.util.Failure(exception) => println(s"An error occurred: ${exception.getMessage}")
        }
        // To keep the application alive while waiting for the API response.
        Thread.sleep(2000) // Sleep for two seconds to ensure the response is received.
      }
      println("Your request info is saved in a text file named 'Palestinian Cities Weather.txt'.")
      printWriter.close() // Close the PrintWriter when done writing
      fileWriter.close() // Close the FileWriter
    }
    else if(userInput.equals("3")) {
      actorSystem.terminate() // Terminate the ActorSystem when done (if needed).
      sys.exit()
    }
    else {
      println("Invalid command number, try again!")
    }
  }

  // My personal key for the weather API.
  private val apiKey: String = "1ff267820399493f8fe152046232910"

  private var requestCity: HttpRequest = _
  // A Function to define the HTTP GET request details for the weather API related to a specific city.
  private def requestCityWeather(city: String): HttpRequest = {
    requestCity = HttpRequest(
      method = HttpMethods.GET,
      uri = s"http://api.weatherapi.com/v1/current.json?key=$apiKey&q=$city"
    )
    requestCity
  }

  // A function to send the generated HTTP GET request and receive the HttpResponse.
  private def sendRequest(requestedCity: HttpRequest): Future[String] = {
    val responseFuture: Future[HttpResponse] = Http().singleRequest(requestedCity) // Send the HTTPRequest and receive the HttpResponse.
    responseFuture.flatMap { response =>
      response.entity.toStrict(2.seconds).map(entity => entity.data.utf8String)
    }
  }

  // A function to display the titles enclosed within a box.
  private def printInBox(text: String): Unit = {
    val line = "-" * (text.length + 4)
    printWriter.println(s"$line")
    printWriter.println(s"| $text |")
    printWriter.println(s"$line")
  }

  private var cityName = "";
  // A function to extract and display the location information of the city.
  private def displayLocationDetails(parsedJson: JsValue): Unit = {
    // Store the location details.
    cityName = (parsedJson \ "location" \ "name").as[String]
    var country = (parsedJson \ "location" \ "country").as[String]
    val latitude = (parsedJson \ "location" \ "lat").as[Double]
    val longitude = (parsedJson \ "location" \ "lon").as[Double]
    val timeZoneID = (parsedJson \ "location" \ "tz_id").as[String]
    val localTimeEpoch = (parsedJson \ "location" \ "localtime_epoch").as[Long]
    val localTime = (parsedJson \ "location" \ "localtime").as[String]

    // Split the local time into date and time.
    val Array(date, time) = localTime.split(" ")

    // Ignore a non-existent state :).
    if (country.equalsIgnoreCase("Israel")) {
      country = "Palestine"
    }

    // Print the location details to the textfile.
    printInBox(s"$cityName location details")
    printWriter.println(s"City: $cityName")
    printWriter.println(s"Country: $country")
    printWriter.println(s"Region time zone: $timeZoneID")
    printWriter.println(s"Latitude: $latitude, Longitude: $longitude")
    printWriter.println(s"Local time epoch: $localTimeEpoch")
    printWriter.println(s"Date: $date, Time: $time")
  }

  // A function to extract and display the Weather information of the city.
  private def displayWeatherDetails(parsedJson: JsValue): Unit = {
    // Store the weather details.
    val temperatureC = (parsedJson \ "current" \ "temp_c").as[Double]
    val temperatureF = (parsedJson \ "current" \ "temp_f").as[Double]
    val isDay = (parsedJson \ "current" \ "is_day").as[Int]
    val conditionText = (parsedJson \ "current" \ "condition" \ "text").as[String]
    val windSpeedMPH = (parsedJson \ "current" \ "wind_mph").as[Double]
    val windSpeedKPH = (parsedJson \ "current" \ "wind_kph").as[Double]
    val windDegree = (parsedJson \ "current" \ "wind_degree").as[Int]
    val windDirection = (parsedJson \ "current" \ "wind_dir").as[String]
    val pressureMB = (parsedJson \ "current" \ "pressure_mb").as[Double] // Millibar (mb).
    val pressureIN = (parsedJson \ "current" \ "pressure_in").as[Double] // inches of mercury (inHg).
    val humidity = (parsedJson \ "current" \ "humidity").as[Int]
    val cloud = (parsedJson \ "current" \ "cloud").as[Int]
    val feelsLikeC = (parsedJson \ "current" \ "feelslike_c").as[Double]
    val feelsLikeF = (parsedJson \ "current" \ "feelslike_f").as[Double]
    val visibleKM = (parsedJson \ "current" \ "vis_km").as[Double]
    val visibleMiles = (parsedJson \ "current" \ "vis_miles").as[Double]
    val uv = (parsedJson \ "current" \ "uv").as[Double]
    val gustMPH = (parsedJson \ "current" \ "gust_mph").as[Double]
    val gustKPH = (parsedJson \ "current" \ "gust_kph").as[Double]
    val precipitationMM = (parsedJson \ "current" \ "precip_mm").as[Double]
    val precipitationIN = (parsedJson \ "current" \ "precip_in").as[Double]
    val lastUpdatedEpoch = (parsedJson \ "current" \ "last_updated_epoch").as[Long]
    val lastUpdated = (parsedJson \ "current" \ "last_updated").as[String]

    // Split the last updated into date and time.
    val Array(lastUpdatedDate, lastUpdatedTime) = lastUpdated.split(" ")

    // Print the weather details to the textfile.
    printInBox(s"$cityName weather information")
    printWriter.println(s"Temperature: $temperatureC°C / $temperatureF°F")
    printWriter.println(s"Is day: $isDay")
    printWriter.println(s"Condition: $conditionText")
    printWriter.println(s"Wind: Speed: $windSpeedMPH mph, $windSpeedKPH kph, Degree: $windDegree, Direction: $windDirection")
    printWriter.println(s"Pressure: $pressureMB mb, $pressureIN in")
    printWriter.println(s"Humidity: $humidity")
    printWriter.println(s"Cloud: $cloud")
    printWriter.println(s"Feels like: $feelsLikeC°C, $feelsLikeF°F")
    printWriter.println(s"Visibility: $visibleKM km, $visibleMiles miles")
    printWriter.println(s"Ultraviolet Index: $uv")
    printWriter.println(s"wind gusts: $gustMPH mph, $gustKPH kph")
    printWriter.println(s"Precipitation: $precipitationMM mm, $precipitationIN in")
    printWriter.println(s"Last updated epoch: $lastUpdatedEpoch")
    printWriter.println(s"Last Update: Date: $lastUpdatedDate, Time: $lastUpdatedTime")
  }

  // A function to display both the location and weather information of the city.
  private def displayAllDetails(parsedJson: JsValue): Unit = {
    displayLocationDetails(parsedJson)
    printWriter.println()
    displayWeatherDetails(parsedJson)
    if(!cityName.equalsIgnoreCase("Yafa"))
      printWriter.println("\n\n")
  }

  // The main method.
  def main(args: Array[String]): Unit = {
    while(true) {
      displayCommands()
      readUserCommand()
    }
  }
}