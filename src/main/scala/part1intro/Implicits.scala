package part1intro

object Implicits {

  // implicit classes
  case class Person(name: String) {
    def greet: String = s"Hi, my name is $name!"
  }

  implicit class ImpersonalString(name: String) {
    def greet: String = Person(name).greet
  }

  // Explicit
  /*
  val impersonableString = new ImpersonalString("Peter")
  impersonableString.greet
  */

  val greeting: String = "Peter".greet // new ImpersonalString("Peter").greet

  // importing implicit conversions in scope
  import scala.concurrent.duration._
  val oneSec: FiniteDuration = 1.second

  // implicit arguments and values
  private def increment(x:Int)(implicit amount: Int): Int = x + amount
  implicit val defaultAmount: Int = 10
  val incremented2: Int = increment(2) // 10 // implicit argument 10 is passed by the compiler

  def multiply(x:Int)(implicit times: Int): Int = x * times
  val times2: Int = multiply(2)

  // more complex example
  trait JSONSerializer[T]{
    def toJson(value: T): String
  }

  private def listToJson[T](list: List[T])(implicit serializer: JSONSerializer[T]):String =
    list.map(value => serializer.toJson(value)).mkString("[", ",", "]")

  implicit val personSerializer: JSONSerializer[Person] = (person: Person) =>
    s"""
         |{"name" : "${person.name}"}
         |""".stripMargin
  val personJson: String = listToJson(List(Person("Alice"), Person("Bob")))

  // implicit argument is used to PROVE THE EXISTENCE of a type

  // implicit methods
  implicit def oneArgCaseCassSerializer[T <: Product]: JSONSerializer[T] =
    (value: T) => s"""
         |{"${value.productElementName(0)}" : "${value.productElement(0)}"}
         |""".stripMargin.trim

  case class Cat(name: String)
  val catsToJson: String = listToJson(List(Cat("Tom"), Cat("Garfield")))

  def main(args: Array[String]): Unit = {
    println(oneArgCaseCassSerializer[Cat].toJson(Cat("Garfield")))
    println(oneArgCaseCassSerializer[Person].toJson(Person("David")))

    // in the background: val catsToJson = listToJson(List(Cat("Tom"), Cat("Garfield")))(oneArgCaseClassSerializer[Cat])
    // implicit methods are used to PROVE THE EXISTENCE of a type
    // can be used got implicit conversions (DISCOURAGED)

  }

}
