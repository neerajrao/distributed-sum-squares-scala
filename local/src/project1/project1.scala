package project1

import actors.Actor
import scala.collection.mutable

case object Stop

object Boss extends BossTemplate { //BossTemplate implements the spawnCCalculators and spawnCalculators methods
  var N, K, U, W, V, numActors, numMCalculatorsResponded, numCCalculatorsResponded, count: Int = 0
  var cSum, totalSum: Double = 0
  var cDone, mDone = false

  def main(args: Array[String]) {
    N = args(0).toInt
    K = args(1).toInt
    W = K/2 //no. of actors for C calculation
    U = (math.pow(N.toDouble, 2.0/3.0)).toInt //no. of actors for M calculation; determined experimentally as best value
    V = (math.pow(N.toDouble,1.0/3.0)).toInt //determined experimentally as best value
    printf("N = %-10d K = %-10d W = %-10d U = %-10d V = %-10d\n",N,K,W,U,V)

    Boss.start()
  }

  def act() {
    spawnCCalculators(W)

    // Start C calcuation
    if(W > K/2) numActors = K/2 else numActors = W
    count = numActors
    for (i <- 0 until numActors) {
      if (K % 2 == 0) { //even
        //println("Sending "+(i+0.5)+" to CCalculator "+i)
        //println((i-0.5).asInstanceOf[AnyRef].getClass.getSimpleName)
        Boss.CCalculatorsArray(i) ! (i+0.5)
      }
      else{ //odd
        //println("Sending "+(i+1)+" to CCalculator "+(i))
        Boss.CCalculatorsArray(i) ! ((i+1)*1.0)
      }
    }

    loop {
      react {

        case msgFromCCalculator: Double => //Continue with C calcuation
          if(cDone == false){
            numCCalculatorsResponded += 1
            cSum += msgFromCCalculator
            if(numCCalculatorsResponded < (K/2)-numActors+1){
              if (K % 2 == 0) { //even
                //println("Here Sending "+(numCCalculatorsResponded+0.5+numActors-1)+" to CCalculator "+(numCCalculatorsResponded+numActors-1))
                //println(sender.asInstanceOf[AnyRef].getClass.getSimpleName)
                sender ! (numCCalculatorsResponded+0.5+numActors-1)
              }
              else{ //odd
                //println("Here Sending "+(numCCalculatorsResponded+numActors+1)+" to CCalculator "+(numCCalculatorsResponded+numActors-1))
                sender ! ((numCCalculatorsResponded+1+numActors)*1.0)
              }
            }
            else if(numCCalculatorsResponded == K/2){ // C calculation is done; start M calculation
              // C calculation done; kill all CCalculators
              for(cCalculator: CCalculator <- Boss.CCalculatorsArray){
                cCalculator ! Stop
              }
              cDone = true

              // Start M Calculation
              spawnCalculators(U)
              if(U > N/V){ //no. of units required theoretically = N/V (since each actor gets V numbers each to handle)
                numActors = N/V
                if(N % V != 0) numActors += 1
              }
              else{
                numActors = U
              }
              //println("numActors = "+numActors)
              for (i <- 0 until numActors) {
                //println(i)
                if (K % 2 == 0) { //even
                  //println("Sending "+((i*V)+K/2+0.5))
                  Boss.calculatorsArray(i) ! (((i*V)+K/2+0.5))

                }
                else{ //odd
                  //println("Sending "+(((i*V)+K/2+1)*1.0))
                  Boss.calculatorsArray(i) ! (((i*V)+K/2+1)*1.0)
                }
              }
            }
          }
          else{ //C calculation is done, M calculation was started; continue with M calculation
            numMCalculatorsResponded += 1
            //println("numMCalculatorsResponded = "+numMCalculatorsResponded)
            if((numMCalculatorsResponded+numActors-1)*V < N){
              if (K % 2 == 0) { //even
                //println("Here sending "+((numMCalculatorsResponded+numActors-1)*V+K/2+0.5))
                sender ! ((numMCalculatorsResponded+numActors-1)*V+K/2+0.5)
              }
              else{ //odd
                //println("Here sending "+((numMCalculatorsResponded+numActors-1)*V+K/2)*1.0)
                sender ! (((((numMCalculatorsResponded+numActors-1)*V)+K/2)*1.0))
              }
            }
            else{ //now we must wait for the numActors initial values we fed in (before entering this case) to come back
              count -= 1
              if(count == 0){
                //M calculation done; kill all MCalculators
                //println(range+" "+numMCalculatorsResponded)
                for(mCalculator: MCalculator <- calculatorsArray){
                  mCalculator ! Stop
                }
                exit()
              }
            }
          }
      }
    }
  }
}

trait BossTemplate extends Actor { //implements the spawnCCalculators and spawnCalculators methods
  val CCalculatorsArray = new mutable.ArrayBuffer[CCalculator]()
  val calculatorsArray = new mutable.ArrayBuffer[MCalculator]()

  def spawnCCalculators(n: Int) {
    for (j <- 1 until 1 + n) {
      var sq: CCalculator = new CCalculator()
      sq.start()
      CCalculatorsArray += sq
    }
  }
  def spawnCalculators(n: Int) {
    for (j <- 1 until 1 + n) {
      var calc: MCalculator = new MCalculator()
      calc.start()
      calculatorsArray += calc
    }
  }

}

class CCalculator extends Actor { //calculates squares that are used to compute M
def act() {
  loop {
    react {
      case numToSquare: Double =>
        //printf("Received a message from Boss to square %f\n", numToSquare)
        //println(sender.asInstanceOf[AnyRef].getClass.getSimpleName)
        sender ! (numToSquare * numToSquare)
      case Stop =>
        exit()
    }
  }
}
}

class MCalculator extends Actor { //calculates ((k*M^2) + (2*cSum)) and checks if it's a perfect square
  var totalSum, sq, M: Double = 0
  var counterV, numLoops: Int = 0

  def act() {
    loop {
      react {

        case numToCalc: Double =>
          //printf("Received a message from Boss to process "+numTocalc+"\n")
          counterV = 0
          M = numToCalc
          for(i <- 0 until Boss.V){ //loop V times for V numbers
            if(M <= Boss.N+Boss.K/2){ //important for the last worker if N is NOT exactly divisible by V
              sq = M * M
              totalSum = sq*Boss.K + 2*Boss.cSum
              //println(totalSum+"  "+math.sqrt(totalSum)+"  "+math.sqrt(totalSum).toInt+"  "+Boss.cSum+"  "+numTocalc+"   "+totalSum)
              if((math.sqrt(totalSum))/(math.sqrt(totalSum).toInt)==1){ //check for perfect square and if one is found, print the first element of the sequence
                println(((M-Boss.K/2).toInt+1))
              }
              counterV+=1
              M += 1
              sender ! 2.0 //2.0 is just a placeholder for a Double so that Boss gives us the next set of V numbers
            }
          }

        case Stop =>
          exit()

      }
    }
  }
}