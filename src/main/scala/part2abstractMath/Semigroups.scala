package part2abstractMath

import cats.implicits.catsSyntaxSemigroup


object Semigroups {

  // Semigroups COMBINE elements of the same type
  import cats.Semigroup
  import cats.instances.int._
  import cats.instances.string._
  private val naturalIntSemiGroup: Semigroup[Int] = Semigroup[Int]
  private val intCombination: Int = naturalIntSemiGroup.combine(2, 46)
  private val naturalStringSemigroup: Semigroup[String] = Semigroup[String]
  private val stringCombination: String = naturalStringSemigroup.combine("I love", "Cats") // concatenation

  // specific API
  val test: (Int, Int) => Int = naturalIntSemiGroup.combine
  println(test(2, 3))
  def reduceInts(list: List[Int]): Int = list.reduce(naturalIntSemiGroup.combine)
  def reduceStrings(list: List[String]): String = list.reduce(naturalStringSemigroup.combine)

  // general API
  def reduceThings[T](list: List[T])(implicit semigroup: Semigroup[T]):T = list.reduce(semigroup.combine)

  case class Expense(id:Long, amount: Double)
  implicit val expenseSemiGroup: Semigroup[Expense] = Semigroup.instance[Expense]{(e1, e2) => Expense(Math.max(e1.id, e2.id), e1.amount + e2.amount)}

  // extension methods from Semigroup
  private val aIntSum: Int = 2 |+| 3 // requires the presence of an implicit Semigroup[Int]
  private val aStringConcat: String = "we like " |+| "semigroups"

  // TODO 2: implement reduceThings2 with the |+|
  def reduceThings2[T](list: List[T])(implicit  semigroup: Semigroup[T]): T = list.reduce(_ |+| _)

  def main(args: Array[String]): Unit = {
    println(intCombination)
    println(stringCombination)

    // specific API
    val numbers = (1 to 10).toList
    println(reduceInts(numbers))
    val strings = List("I'm", "starting", "to", "like", "semigroups")
    println(reduceStrings(strings))

    // general API
    println(reduceThings(numbers))
    println(reduceThings(strings))
    import cats.instances.option._
    val numberOptions: List[Option[Int]] = numbers.map(n => Option(n))
    println(reduceThings(numberOptions)) // an Option[Int] containing the sum of all the numbers
    val stringOptions: List[Option[String]] = strings.map(s => Option(s))
    println(reduceThings(stringOptions))

    val listExpense = List(Expense(1, 99), Expense(2, 35), Expense(3, 7))
    println(reduceThings(listExpense))

    println(reduceThings2(listExpense))

  }

}
