package it.di.unipi.sam636694.semelion.utilities

//colori dei jolly
val JOLLY_COLOR = listOf("black","red")
//delay time usato per scoprire carte
const val DELAY_TIME: Long = 500
//delay time dei messaggi della SnackBar
const val SNACKBAR_DELAY_TIME:Long = 2000
const val UNCOVER_DECK_SIZE = 8
//calcolo della posizione corretta, ossia il valore che voglio la carta abbia per poter dare azioni aggiuntive
val POSITION_VALUES = Pair(
    first = {rid:Int,pos:Int -> 7*(rid+1)-pos},
    second= {rid:Int,pos:Int -> pos+1-(7*rid)}
)
//lista delle figure per creare il mazzo scoperta di Semelion
val SEMELION_FIGURES = listOf(
    Pair(10,"D"),
    Pair(9,"C"),
    Pair(9,"P"),
    Pair(9,"F"),
    Pair(8,"C"),
    Pair(8,"P")
    )
//serviceID per NearbyConnections
const val serviceId = "semelion_nearbyConnections"
