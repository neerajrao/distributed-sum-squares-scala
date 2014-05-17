package project1_remote
import actors.Actor

case object Stop

class MCalculator(V: Int, K: Int, end: Int, cSum: Double) extends Actor { //calculates ((k*M^2) + (2*cSum)) and checks if it's a perfect square
  private var totalSum, sq, M: Double = 0
  private var counterV, numLoops: Int = 0
  private var sqlist: List[Int] = Nil

  def act() {
    loop {
      react {

        case numToCalc: Double => {
          sqlist = Nil //important if this calculator is being reused (U in the Listener < minimum required no. of calcs)
          //printf("Received a message to process "+numToCalc+"\n")
          counterV = 0
          M = numToCalc
          for(i <- 0 until V){ //loop V times for V numbers
            if(M <= end+K/2){ //important for the last worker if end is NOT exactly divisible by V
              //println("aa "+Listener.end+Listener.K/2)
              sq = M * M
              totalSum = sq*K + 2*cSum
              //println(numToCalc+"   "+totalSum+"  "+math.sqrt(totalSum).toInt+"  "+cSum)
              if((math.sqrt(totalSum))/(math.sqrt(totalSum).toInt)==1){ //check for perfect square and if one is found, print the first element of the sequence
                sender ! ((M-K/2).toInt+1)
                //sqlist = (((M-K/2).toInt+1)) :: sqlist
                //println("perfect square found at "+(((M-K/2).toInt+1)))
                //squareArray:+((M-Listener.K/2).toInt+1)
                //println("sq"+squareArray)
              }
              else{
                sender ! 0
              }
              counterV+=1
              M += 1
            }
          }

         //sender ! sqlist //send back all perfect squares found to the Listener. This list may be empty!
        }
        case Stop => {
          //println("Killed here")
          exit()
        }
      }
    }
  }
}