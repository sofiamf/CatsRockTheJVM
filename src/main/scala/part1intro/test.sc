

val checkckerboard = List(1,2,3).flatMap(n => List('a', 'b', 'c').map(d => (n, d)))

println(checkckerboard)

println(List(1,2,3).flatMap(n => List('a', 'b', 'c')))