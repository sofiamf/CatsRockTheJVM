package part4typeclasses

import cats.{Applicative, Apply, FlatMap}

object WeakerMonads {

  trait MyFlatMap[M[_]] extends Apply[M] {
    def flatMap[A, B](ma: M[A])(f: A => M[B]): M[B]

    // TODO
    // hint: Apply extends Functor
    def ap[A, B](wf: M[A => B])(wa: M[A]): M[B] =
      flatMap(wa)(a => map(wf)(f => f(a)))
  }

  trait MyMonad[M[_]] extends Applicative[M] with MyFlatMap[M] {
   override def map[A, B](ma: M[A])(f: A => B): M[B] =
     flatMap(ma)(x => pure(f(x)))
  }

  import cats.syntax.flatMap._ // flatMap extension method
  import cats.syntax.functor._ // map extension method

  def getPairs[M[_]: FlatMap](numbers: M[Int], chars: M[Char]): M[(Int, Char)] = for {
    n <- numbers
    c <- chars
  } yield (n, c)


}
