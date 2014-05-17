package project1_remote
import actors.Actor
import collection.mutable

trait ListenerTemplate extends Actor { //implements the spawnCCalculators and spawnCalculators methods
val calculatorsArray = new mutable.ArrayBuffer[MCalculator]()

  def spawnCalculators(n: Int, V: Int, K: Int, range: Int, cSum: Double) {
    for (j <- 1 until 1 + n) {
      var calc: MCalculator = new MCalculator(V, K, range, cSum)
      calc.start()
      calculatorsArray += calc
    }
  }

}