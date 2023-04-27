package part2abstractMath

object Monoids {

  import cats.Semigroup
  import cats.instances.int._
  import cats.syntax.semigroup._ // import the |+| extension method

  val numbers = (1 to 1000).toList
  // |+| is always associative
  val sumLeft = numbers.foldLeft(0)(_ |+| _)
  val sumRight = numbers.foldRight(0)(_ |+| _)

  // define general API
  // def combineFold[T](list: List[T])(implicit semigroup: Semigroup [T]): T =
  //  list.foldLeft(/* WHAT? */)(_ |+| _)

  // MONOIDS
  import cats.Monoid
  val intMonoid = Monoid[Int]
  val combineInt = intMonoid.combine(23, 999) // 1022
  val zero = intMonoid.empty // 0

  import cats.instances.string._ // bring the implicit Monoid[String] in scope
  val emptyString = Monoid[String].empty // ""
  val combineString = Monoid[String].combine("I understand", "monoids")

  import cats.instances.option._ // construct an implicit Monoid[Option[Int]]
  val emptyOption = Monoid[Option[Int]].empty
  val combineOption = Monoid[Option[Int]].combine(Option(2), Option.empty[Int]) // Some(2)
  val combineOption2 = Monoid[Option[Int]].combine(Option(3), Option(6)) // Some(8)

  // extension methods for Monoids - |+|
  val combinedOptionFancy = Option(3) |+| Option(7)

  // TODO 1: implement a combineFold
  def combineFold[T](list: List[T])(implicit monoid: Monoid[T]): T = list.foldLeft(monoid.empty)(_ |+| _)

  // TODO 2: combine a list of phonebooks as Map[String, Int] by using a combineFold and by using an implicit
  // I don't need to construct a monoid myself
  val phonebooks = List(
    Map(
      "Alice" -> 235,
      "Bob" -> 647
    ),
    Map(
      "Charlie" -> 372,
      "Bob" -> 889
    ),
    Map(
      "Tina" -> 123
    )
  )

  import cats.instances.map._
  val massivePhoneBook = combineFold(phonebooks)

  // TODO 3 - shopping cart and online stores with Monoids
  // hint: define your monoid
  // hint #2: use combine by fold

  case class ShoppingCart(items: List[String], total: Double)
  implicit val shoppingCartMonoid: Monoid[ShoppingCart] = Monoid.instance(
    ShoppingCart(List(), 0.0),
    (sa, sb) => ShoppingCart(sa.items ++ sb.items, sa.total + sb.total)
  )
  def checkout(shoppingCarts: List[ShoppingCart]): ShoppingCart = combineFold(shoppingCarts)


  def main(args: Array[String]): Unit = {
    println(sumLeft)
    println(sumRight)
    println(combineInt)
    println(combineFold(numbers))
    println(combineFold(List("I", "like", "monoids")))
    println(combineFold(phonebooks))
    println(massivePhoneBook)

    println(checkout(List(
      ShoppingCart(List("iphone", "android", "TV"), 1500),
      ShoppingCart(List("tablet", "microwave"), 150),
      ShoppingCart(List(), 0)
    )))
  }

}
