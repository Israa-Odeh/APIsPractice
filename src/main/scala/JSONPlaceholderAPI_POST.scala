
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

// Note from JSON Placeholder: Important: resource will not be really updated on the server but it will be faked as if.

object JSONPlaceholderAPI_POST {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer.type = ActorMaterializer
  import actorSystem.dispatcher

  // The HTTP Post request body.
  private val postBody =
    """
    {
      "userId": 10,
      "id": 1,
      "title": "Using the JSON Placeholder API to test the Post Method.",
      "body": "JSON Placeholder API provides a simulated RESTful API environment for testing without modifying real data, ideal for practicing HTTP methods like POST, GET, and others in a controlled, sandboxed setting."
    }
  """

  // Define the HTTP POST request details for the JSON Placeholder API - related to posts.
  private val request = HttpRequest(
    method = HttpMethods.POST,
    uri = "https://jsonplaceholder.typicode.com/posts",
    entity = HttpEntity(
      ContentTypes.`application/json`,
      postBody, // The actual data I want to send.
    )
  )

  // A function to send the generated HTTP POST request and receive the HttpResponse.
  private def sendRequest(): Future[String] = {
    val responseFuture: Future[HttpResponse] = Http().singleRequest(request)
    val entityFuture: Future[HttpEntity.Strict] = responseFuture.flatMap(response =>
      response.entity.toStrict(2.seconds))
    entityFuture.map(entity => entity.data.utf8String)
  }

  // The main method.
  def main(args: Array[String]): Unit = {
    val responseFuture: Future[String] = sendRequest()

    responseFuture.onComplete {
      case Success(response) =>
        // println(s"Response: $response")

        // Retrieve and format the fields from the response in an organized manner.
        val parsedJson = Json.parse(response)
        val userID = (parsedJson \ "userId").as[Int]
        val postID = (parsedJson \ "id").as[Int]
        val postTitle = (parsedJson \ "title").as[String]
        val postBody = (parsedJson \ "body").as[String]

        // Print the response fields in an organized manner.
        println("\n------------------------")
        println("| The response details |")
        println("------------------------")
        println(s"User ID: $userID")
        println(s"Post ID: $postID")
        println(s"Post title: $postTitle")
        println(s"Post body: $postBody\n")

        // Terminate the actor system.
        actorSystem.terminate()

      case Failure(exception) =>
        println(s"Request failed with: ${exception.getMessage}")
    }
  }
}