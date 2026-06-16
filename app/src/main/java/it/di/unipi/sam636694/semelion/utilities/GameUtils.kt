package it.di.unipi.sam636694.semelion.utilities

import android.util.Log
import it.di.unipi.sam636694.semelion.ui.states.CardUIStates
import it.di.unipi.sam636694.semelion.ui.states.GameIntent
//trova le righe potenti
fun List<CardUIStates>.findPowerRow(): Int{
    val order = this.getPredominantOrder()
    return when(order.size){
        1 ->  {
            val (_,second,third)= order.first()
            if (second == 7 || third == 7) 1 else 0
        }
        else -> 0
    }
}

//calcolo degli ordinamenti predominanti delle righe
fun houseRowOrder(house:String,cards:List<CardUIStates>):Triple<String,Int,Int>{
    //controllo solo le carte dello stesso seme
    val houseCards = cards.filter { it.house == house && it.isRevealed }
    //controllo quante sono in ordinamento crescente
    val crescentOrder = houseCards.foldRight(0){ houseCard,acc ->
        //TROVO L'INDICE DELLA CARTA
        val index = cards.indexOfFirst {  card ->
            card.name == houseCard.name
        }
        Log.d("HRO","index crescent:$index")
        when{
            index == -1 -> acc
            houseCard.value == index+1 -> acc+1
            else -> acc
        }
    }
    //controllo quante sono in ordinamento decrescente
    val decrescentOrder = houseCards.foldRight(0){ houseCard,acc ->
        val index = cards.indexOfFirst {  card -> card.name == houseCard.name }
        Log.d("DECRESCENTORDER","index:$index,houseCard:${houseCard.value} formulaCrescente:${houseCard.value==index+1},formulaDecrescente:${houseCard.value == 7 - index }")
        when{
            index == -1 -> acc
            houseCard.value == 7 - index -> acc+1
            else -> acc
        }
    }
    //ritorno una tripla con seme, #carte in ordine crescente, #carte in ordine decrescente
    return  Triple(house,crescentOrder,decrescentOrder)
}

//aggiustare caso limite in cui hai due triple con stesso numero di ordinamenti crescenti/decrescenti es ("P",2,0) e ("F",2,0)
// -> in realtà potrei accettare che solo 1 dei due ordinamenti vale
// -> lo ho accettato anche perchè altrimenti avrebbe portato a bullismo
fun findMax(triples:List<Triple<String,Int,Int>>,parameter:String):Triple<String,Int,Int>?{
    if (triples.isEmpty()) return null
    var bestTriple = triples.first()
    when (parameter){
        "second" -> triples.forEach { triple -> if (triple.second > bestTriple.second) bestTriple = triple }
        "third" -> triples.forEach { triple -> if (triple.third > bestTriple.third) bestTriple = triple }
    }
    return bestTriple
}

//controllo gli ordinamenti predominanti delle righe
fun List<CardUIStates>.getPredominantOrder():List<Triple<String,Int,Int>>{
    //considero solo le carte rivelate
    val revealed = filter { it.isRevealed }
    val houses = mutableSetOf<String>()

    revealed.forEach { card -> houses.add(card.house) }

    val triples: MutableList<Triple<String,Int,Int>> = mutableListOf()

    houses.forEach { house ->
        Log.d("HRO","seme:$house")
        triples.add(houseRowOrder(house,this))
    }

    if (triples.isEmpty()) return emptyList()

    val maxCrescent = findMax(triples,"second") ?: return emptyList()

    val maxDecrescent = findMax(triples,"third") ?: return emptyList()

    return when {
        maxCrescent.second > maxDecrescent.third ->  listOf(maxCrescent)
        maxCrescent.second < maxDecrescent.third ->  listOf(maxDecrescent)
        maxDecrescent.third == 0 -> listOf(Triple("None",0,0))
        else -> {
            val order = mutableListOf(maxCrescent)
            if (maxCrescent.first != maxDecrescent.first) order.add(maxDecrescent)
            order
        }
    }
}

//sono sicuro che questa funzione venga usata ma non capisco perchè l'ide non lo dica
fun List<CardUIStates>.getBonusActions():List<Triple<String,Int,Int>>{
    //considero solo le carte rivelate
    val revealed = filter { it.isRevealed }
    val houses = mutableSetOf<String>()

    revealed.forEach { card -> houses.add(card.house) }

    val triples: MutableList<Triple<String,Int,Int>> = mutableListOf()

    houses.forEach { house ->
        Log.d("HRO","seme:$house")
        triples.add(houseRowOrder(house,this))
    }
    return triples
}
