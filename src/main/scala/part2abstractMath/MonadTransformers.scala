package part2abstractMath

import cats.instances.future._

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

object MonadTransformers {

  def sumAllOptions(value: List[Option[Int]]): Int = ???

  // option transformer
  import cats.data.OptionT
  import cats.instances.list._ // fetch an implicit OptionT[List]

  val listOfNumberOptions: OptionT[List, Int] = OptionT(List(Option(1), Option(2))) // this basically means a
  // list of options of List (middle, left, right)
  val listOfCharOptions: OptionT[List, Char] = OptionT(List(Option('a'), Option('b'), Option.empty[Char]))
  val listOfTuples: OptionT[List, (Int, Char)] = for {
    char <- listOfCharOptions
    number <- listOfNumberOptions
  } yield (number, char)

  // either transformer
  import cats.data.EitherT
  val listOfEithers: EitherT[List, String, Int] = EitherT(List(Left("something wrong"), Right(43), Right(2)))
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
  val futureOfEither: EitherT[Future, String, Int] = EitherT.right(Future(45)) // wrap over future(Right(45)) // EitherT(Future[Either[String, Int]](Right(45)))

  /*
  TODO exercise
  We have a multi-machine cluster for your business which will receive a traffic surge following a media appearance.
  We measure bandwidth in units.
  We want to allocate TWO of our servers to cope with the traffic spike.
  We know the current capacity for each server and we know we'll hold the traffic if the sum of bandwidth is > 250.
   */

  val bandwidths = Map(
    "server1.rockthejvm.com" -> 50,
    "server2.rockthejvm.com" -> 300,
    "server3.rockthejvm.com" -> 170
  )

  type AsyncResponse[T] = EitherT[Future, String, T]

  def getBandwidth (server: String): AsyncResponse[Int] = bandwidths.get(server) match {
    case None => EitherT.left(Future(s"Server $server unreachable"))// EitherT(Future(Left(s"Server $server unreachable")))
    case Some(b) => EitherT.right(Future(b)) // EitherT(Future(Right(b)))
  }

  // TODO 1
  // hint: call getBandwith twice, and combine the results

  def canWithstandSurge(s1: String, s2: String): AsyncResponse[Boolean] = for {
    band1 <- getBandwidth(s1)
    band2 <- getBandwidth(s2)
  } yield band1 + band2 > 250


  // TODO 2
  // hint: call canWithstandSurge + transformers
  def generateTrafficSpikeReport(s1: String, s2: String): AsyncResponse[String] =
    canWithstandSurge(s1, s2).transform {
      case Left(reason) => Left(s"Servers $s1 and $s2 CANNOT cope with the incoming spike: $reason")
      case Right(false) => Left(s"Servers $s1 and $s2 CANNOT cope with the incoming spike: not enough total bandwidth")
      case Right(true) => Right(s"Servers $s1 and $s2 can cope with the incoming spike NO PROBLEM!")
    }


  def main(args: Array[String]): Unit = {
    println(listOfTuples.value)
    val resultFuture = generateTrafficSpikeReport("server1.rockthejvm.com", "server3.rockthejvm.com").value
    resultFuture.foreach(println)
  }

}
