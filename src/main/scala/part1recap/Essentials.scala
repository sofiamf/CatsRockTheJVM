package part1recap


import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try,Success,Failure}

object Essentials {

  // values and variables
  val aBoolean: Boolean = false

  // expression are EVALUATED to a false
  val anIfExpression = if (2 > 3) "bigger" else "smaller"

  // instructions vs expressions
  val theUnit = println("Hello, Scala") // Unit = "void" in other languages

  // OOP
  class Animal
  class Cat extends Animal

  trait Carnivore {
    def eat(animal: Animal): Unit
  }

  // inheritance model: extend <=1 class, but inherit >= 0 traits
  class Crocodeile extends Animal with Carnivore {
    override def eat(animal: Animal): Unit = println("Crunch")
  }

  // singleton
  object MySingleton // singleton pattern in one line

  // companions
  object Carnivore // companion object of the class Carnivore

  // generics
  class MyList[A]

  // method notation
  val three = 1 + 2
  val anotherThree = 1.+(2)

  // functional programming
  val incrementer: Int => Int = x => x + 1
  val incremented = incrementer(45) // 46

  // map, flatMap, filter
  val processedList = List(1,2,3).map(incrementer) // List(2,3,4)
  val aLongerist = List(1,2,3).flatMap(x => List(x, x + 1)) // List(1,2, 2,3, 3,4)

  // for-comprehensions
  val checkckerboard = List(1,2,3).flatMap(n => List('a', 'b', 'c').map(c => (n, c)))
  val anotherCheckerboard = for {
    n <- List(1, 2, 3)
    c <- List('a', 'b', 'c')
  } yield(n, c)

  // options and try
  val anOption: Option[Int] = Option( /* something that might be null */ 3 ) // Some(3)
  val doubledOption: Option[Int] = anOption.map(_ * 2)

  val anAttempt = Try(/* somthing that might throw */ 42) // Success(42)
  val aModifiedAttempt: Try[Int] = anAttempt.map(_ + 10)

  // pattern matching
  val anUnknown: Any = 45
  val ordinal = anUnknown match {
    case 1 => "first"
    case 2 => "second"
    case _ => "unknown"
  }

  val optionDescription = anOption match {
    case Some(value) => s"the option os  not empty: $value"
    case None => "the option is empty"
  }

  // Futures
  // import scala.concurrent.ExecutionContext.Implicits.global

  // from scala 2.13
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
  val aFuture = Future {
    // a bit of code
    42
  }

  // wait for completion (async)
  aFuture.onComplete {
    case Success(value) => println(s"The async meaning of life is $value")
    case Failure(exception) => println(s"Meaning of value failed: $exception")
  }



// partial functions
  val aPartialFunction: PartialFunction[Int, Int] = {
    case 1 => 43
    case 8 => 56
    case 100 => 999
  }

  // some more advanced stuff
  trait HigherKindedType[F[_]]
  trait SequenceChecker[F[_]] {
    def isSequential: Boolean
  }

  val listChecker = new SequenceChecker[List] {
    override def isSequential: Boolean = true
  }
  def main(args: Array[String]): Unit = {

  }

}
