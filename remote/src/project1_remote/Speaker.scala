package project1_remote

import actors.Actor
import actors.Actor._
import actors.remote.{Node, RemoteActor}
import scala.actors.remote.RemoteActor._
import collection.mutable

object Speaker extends SpeakerTemplate{

  var N, K, W, R, numActors, numMCalculatorsResponded, numCCalculatorsResponded: Int = 0
  var cSum, totalSum: Double = 0
  var cDone, mDone = false
  var numRemotes: Int = 10
  var numRemotesResponded: Int = 0
  var name: Symbol = Configuration.nameS
  var port_no: Int = Configuration.portS

  def main (args: Array[String]) {
    N = args(0).toInt
    K = args(1).toInt //Sequence length
    W = K/2 //No. of C calculators frozen at K/2 determined experimentally as best choice
    printf("N = %-10d K = %-10d\n",N,K)
    Speaker.start()
  }

  def act(){
    alive(port_no)
    register(name, self)
    println("registered self on port "+port_no+" with name "+name)

    spawnCCalculators(W)

    // Start C calcuation
    if(W > K/2) numActors = K/2 else numActors = W
    for (i <- 0 until numActors) {
      if (K % 2 == 0) { //even
        //println("Sending "+(i+0.5)+" to CCalculator "+i)
        //println((i-0.5).asInstanceOf[AnyRef].getClass.getSimpleName)
        Speaker.CCalculatorsArray(i) ! (i+0.5)
      }
      else{ //odd
        //println("Sending "+(i+1)+" to CCalculator "+(i))
        Speaker.CCalculatorsArray(i) ! ((i+1)*1.0)
      }
    }

    loop {
      react {

        case msg: Double => //Continue with C calcuation
          if(sender.isInstanceOf[CCalculator]){
            if(cDone == false){
              numCCalculatorsResponded += 1
              cSum += msg
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
                for(cCalculator: CCalculator <- Speaker.CCalculatorsArray){
                  cCalculator ! Stop
                }
                cDone = true
                R = (N/numRemotes)
                if(N % numRemotes != 0) R += 1
                ListenersArray += select(Node(Configuration.machine1, Configuration.port1),Configuration.name1)
                ListenersArray += select(Node(Configuration.machine2, Configuration.port2),Configuration.name2)
                ListenersArray += select(Node(Configuration.machine3, Configuration.port3),Configuration.name3)
                ListenersArray += select(Node(Configuration.machine4, Configuration.port4),Configuration.name4)
                ListenersArray += select(Node(Configuration.machine5, Configuration.port5),Configuration.name5)
                ListenersArray += select(Node(Configuration.machine6, Configuration.port6),Configuration.name6)
                ListenersArray += select(Node(Configuration.machine7, Configuration.port7),Configuration.name7)
                ListenersArray += select(Node(Configuration.machine8, Configuration.port8),Configuration.name8)
                ListenersArray += select(Node(Configuration.machine9, Configuration.port9),Configuration.name9)
                ListenersArray += select(Node(Configuration.machine10,Configuration.port10),Configuration.name10)
                for(i<-0 until numRemotes){
                  var startAt: Int = (i*R)+1
                  if((i+1)*R > N) R = (N - startAt + 1) //important for the last guy if N is not exactly divisible by numRemotes
                  //println("Sending Listener No. "+i+" startAt = "+startAt+", range = "+R+", K = "+K+", cSum = "+cSum)
                  ListenersArray(i) ! (startAt, R, K, cSum)
                }
              }
            }
        }

        /*case perfectSquareFromListener: Int =>
          println(perfectSquareFromListener)*/

        case allPerfectSquaresList: List[Int] => //received a list of perfect squares from one of the remote machines
          numRemotesResponded += 1
          //println(numRemotesResponded)
          //println(allPerfectSquaresList.asInstanceOf[AnyRef].getClass.getSimpleName())
          allPerfectSquaresList.foreach(println)
          if(numRemotesResponded == numRemotes){
            exit()
          }

        /*case "Stop" =>
          numRemotesResponded += 1
          if(numRemotesResponded == numRemotes){
            exit()
          }*/
      }
    }
  }
}

trait SpeakerTemplate extends Actor { //implements the spawnCCalculators method
  val CCalculatorsArray = new mutable.ArrayBuffer[CCalculator]()
  val ListenersArray = new mutable.ArrayBuffer[scala.actors.AbstractActor]()

  def spawnCCalculators(n: Int) {
    for (j <- 1 until (n+1)) {
      var sq: CCalculator = new CCalculator()
      sq.start()
      CCalculatorsArray += sq
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