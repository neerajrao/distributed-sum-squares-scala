package project1_remote

import actors._
import actors.Actor._
import actors.remote._
import scala.actors.remote.RemoteActor._

object Listener7 extends ListenerTemplate{

  var numActors, numMCalculatorsResponded, V, K, range, U, count: Int = 0
  var cSum, totalSum: Double = 0
  var mDone = false
  var startAt: Int = 0
  var allPerfectSquaresList: List[Int] = Nil
  var port_no: Int = Configuration.port7 //default
  var name: Symbol = Configuration.name7 //default

  def main(args: Array[String]){
    Listener7.start()
  }

  def act(){
    alive(port_no)
    register(name, self)
    println("registered self on port "+port_no+" with name "+name)

    loop {
      react{

        case msgFromSpeaker : (Int,Int,Int,Double) => {//(startAt, range, K, cSum)
          startAt = msgFromSpeaker._1
          range = msgFromSpeaker._2
          K = msgFromSpeaker._3
          cSum = msgFromSpeaker._4

          V = (math.pow(range.toDouble,1.0/3.0)).toInt //determined experimentally as best value
          U = (math.pow(range.toDouble, 2.0/3.0)).toInt //determined experimentally as best value

          println("Received startAt = "+startAt+", range = "+range+", K = "+K+", cSum = "+cSum)

          //start M calculation
          spawnCalculators(U,V,K,range+startAt,cSum)

          if(U > range/V){ //no. of units required theoretically = range/V (since each actor gets V numbers each to handle)
            numActors = range/V
            if(range % V != 0) numActors += 1
          }
          else{
            numActors = U
          }
          count = numActors
          //println("numActors = "+numActors)
          for (i <- 0 until numActors) {
            //println(i)
            if (K % 2 == 0) { //even
              //println("Sending "+((i*V)+K/2+0.5))
              Listener7.calculatorsArray(i) ! (((i*V)+K/2+0.5)+startAt-1)
            }
            else{ //odd
              //println("Sending "+(((i*V)+K/2+1)*1.0))
              Listener7.calculatorsArray(i) ! ((((i*V)+K/2+1)*1.0)+startAt-1)
            }
          }
        }
        //case listFromAnMCalculator : List[Int] => {//received a list of the perfect squares sequence starts from an MCalculator
        case listFromAnMCalculator : Int => {//received a list of the perfect squares sequence starts from an MCalculator
          numMCalculatorsResponded += 1
          //println("> "+numMCalculatorsResponded)
          //if(!listFromAnMCalculator.isEmpty) allPerfectSquaresList = listFromAnMCalculator ++ allPerfectSquaresList
          if(listFromAnMCalculator!=0) allPerfectSquaresList = listFromAnMCalculator :: allPerfectSquaresList
          /*val Speaker = select(Node(Configuration.machineS,Configuration.portS),Configuration.nameS)
          if(listFromAnMCalculator!=0) Speaker ! listFromAnMCalculator*/

          if((numMCalculatorsResponded+numActors-1)*V < range+startAt-1){
            if (K % 2 == 0) { //even
              //println("Here sending "+((numMCalculatorsResponded+numActors-1)*V+K/2+0.5))
              sender ! (((numMCalculatorsResponded+numActors-1)*V+K/2+0.5)+startAt-1)
            }
            else{ //odd
              //println("Here sending "+((numMCalculatorsResponded+numActors-1)*V+K/2)*1.0)
              sender ! (((((numMCalculatorsResponded+numActors-1)*V)+K/2)*1.0)+startAt-1)
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

              //printing on local machine just as diagnostic
              //allPerfectSquaresList.foreach(println)

              // All MCalculators have responded. Now we can send the
              // list of perfect squares back to the speaker (NB: the list
              // may very well be empty)
              val Speaker = select(Node(Configuration.machineS,Configuration.portS),Configuration.nameS)
              Speaker ! allPerfectSquaresList

              //Speaker ! "Stop"

              exit()
            }
          }
        }
      }
    }
  }
}