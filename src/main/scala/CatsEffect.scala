import cats.effect.{ExitCode, IO, IOApp}

import java.util.UUID

sealed trait Breed
case object Siamese extends Breed
case object Bengal extends Breed

case class Cat(name: String, age: Int, breed: Breed)
case class CatWalker(name: String, id: UUID, dogs: List[Cat])

object IOTest {
  def printCatWalker(catWalker: CatWalker): IO[Unit] =
    IO(println(catWalker))
}

object CatsEffect extends IOApp{
  override def run(
      args: List[String]
  ): IO[ExitCode] = {
    import IOTest._

    printCatWalker(CatWalker("John", UUID.randomUUID(), List(Cat("Tom", 2, Siamese), Cat("Jerry", 3, Bengal)))
    ).map(_ => ExitCode.Success)
  }
}
