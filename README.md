# 🃏 Semelion

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Google Nearby](https://img.shields.io/badge/Multiplayer-Nearby%20Connections-00897B?style=for-the-badge)
![Room Database](https://img.shields.io/badge/Storage-Room%20DB-orange?style=for-the-badge)

**Un gioco di carte strategico da tavolo per 2 giocatori, moderno, dinamico e competitivo.**  
*Sviluppato come progetto per il corso di Sviluppo Applicazioni Mobili (SAM) — Università di Pisa.*

[Panoramica](#-cosè-semelion) •
[Regole & Obiettivo](#-scopo-del-gioco--condizione-di-vittoria) •
[Meccaniche di Gioco](#-come-funziona-il-gioco) •
[Carte Speciali](#-carte-speciali) •
[Modalità di Gioco](#-modalità-di-gioco) •
[Architettura & Tech Stack](#-architettura-e-tecnologie) •
[Setup & Installazione](#-installazione-e-avvio)

</div>

---

## 📖 Cos'è Semelion?

**Semelion** è un gioco di carte originale a turni progettato per due contendenti, digitalizzato e sviluppato nativamente per dispositivi Android.

Ispirato ai giochi di logica, memoria e controllo del tavolo, Semelion fonde pianificazione a lungo termine, gestione delle probabilità e manipolazione della griglia: i giocatori condividono lo stesso campo di carte coperte e scoperte, cercando di allineare sequenze vincenti sul proprio lato del tavolo e ostacolando al contempo le mosse dell'avversario.

Il gioco offre un'esperienza reattiva basata su **gesti naturali** (Tap per scoprire carte, **Drag & Drop** per scambiarle, **Swipe orizzontali e verticali** per traslare righe e colonne) e supporta sia il multiplayer locale sullo stesso dispositivo sia la modalità **wireless peer-to-peer** a corto raggio.

---

## 🏆 Scopo del Gioco & Condizione di Vittoria

L'obiettivo primario di ciascun giocatore è completare per primo **2 Righe Potenti** (*Power Rows*) all'interno della propria metà del tavolo.

### Cos'è una "Riga Potente"?
Una **Riga Potente** è una sequenza completa di **7 carte dello stesso seme** posizionate in una singola riga della griglia, disposte secondo uno dei due ordinamenti ammessi:
- 📈 **Ordinamento Crescente**: da Asso a 7 ($A \rightarrow 2 \rightarrow 3 \rightarrow 4 \rightarrow 5 \rightarrow 6 \rightarrow 7$)
- 📉 **Ordinamento Decrescente**: da 7 ad Asso ($7 \rightarrow 6 \rightarrow 5 \rightarrow 4 \rightarrow 3 \rightarrow 2 \rightarrow A$)

> ⚠️ **Nota di salvaguardia**: Una volta che una riga diventa "Potente", le carte che ne fanno parte non possono più essere scambiate né la riga può essere alterata o shiftata dal Re avversario.

---

## 🗺️ Il Tavolo da Gioco (La Griglia)

Il piano di gioco è strutturato come una matrice di **4 righe per 7 colonne** (28 carte totali sul tavolo):

```
                   [ Campo Giocatore 2 ]
   Riga 0: [ C0 ] [ C1 ] [ C2 ] [ C3 ] [ C4 ] [ C5 ] [ C6 ]
   Riga 1: [ C0 ] [ C1 ] [ C2 ] [ C3 ] [ C4 ] [ C5 ] [ C6 ]
   -------------------------------------------------------- (Linea di metà campo)
   Riga 2: [ C0 ] [ C1 ] [ C2 ] [ C3 ] [ C4 ] [ C5 ] [ C6 ]
   Riga 3: [ C0 ] [ C1 ] [ C2 ] [ C3 ] [ C4 ] [ C5 ] [ C6 ]
                   [ Campo Giocatore 1 ]
```

### Il Mazzo e il "Mazzo Scoperta"
Il set di gioco complessivo comprende:
- **28 Carte Numeriche**: Valori da 1 (Asso) a 7 per ciascuno dei 4 semi (*Cuori*, *Quadri*, *Fiori*, *Picche*).
- **8 Carte Speciali**: Figure (*Jack*, *Donne*, *Re*) e *Jolly* (Rosso e Nero).

All'inizio della partita:
1. Vengono rimosse **8 carte numeriche** dal mazzo base per formare il **Mazzo Scoperta** (*Uncover Deck*), posto a faccia in su a lato del tavolo.
2. Le **8 carte speciali** vengono mescolate con le restanti 20 carte numeriche per comporre la griglia 4×7: in questo modo, **tutte le figure e i jolly sono garantiti sul tavolo fin dall'inizio**.
3. Quando una figura viene scoperta dal tavolo durante la partita, essa viene scartata e immediatamente sostituita con la carta in cima al *Mazzo Scoperta*.

---

## ⚙️ Come Funziona il Gioco

### 1. Calcolo delle Azioni per Turno
A differenza dei classici giochi a mosse fisse, in Semelion il numero di azioni a disposizione all'inizio del proprio turno è **dinamico** e premia la bontà del proprio schieramento:

$$\text{Azioni del Turno} = \left\lfloor \frac{\text{Carte del proprio lato in Posizione Corretta}}{2} \right\rfloor + 1$$

*(Al secondo giocatore viene concesso +1 azione di compensazione al primo turno per bilanciare il vantaggio della prima mossa).*

### 2. Definizione di "Posizione Corretta"
Una carta rivelata è considerata in **Posizione Corretta** se occupa una colonna corrispondente al proprio valore numerico rispetto alla direzione di lettura della riga:
- In ordine **Crescente**: la carta di valore $N$ si trova nella colonna $N$ (es. il $3$ in colonna 3, l'Asso in colonna 1).
- In ordine **Decrescente**: la carta di valore $N$ si trova nella colonna complementare ($8 - N$).

### 3. Azioni Eseguibili nel Turno
Durante il proprio turno, consumando le azioni a disposizione, il giocatore può:

#### A. Rivela Carta (*Tap*)
Toccare una carta coperta sul tavolo per svelarne valore e seme.

#### B. Scambia Due Carte (*Drag & Drop*)
Trascina una carta sopra un'altra per scambiarle di posizione. Lo scambio è valido se rispetta tre regole fondamentali:
1. **Regola di Equità (*Fairness*)**: Almeno una delle due carte coinvolte deve trovarsi nella propria metà campo (non è consentito scambiare due carte entrambe posizionate nella metà campo avversaria).
2. **Regola di Correttezza (*Correctness*)**: A seguito dello scambio, almeno una delle due carte deve finire in una posizione corretta.
3. **Regola di Protezione (*Power Row Rule*)**: Nessuna delle due carte può appartenere a una riga potente già completata.

---

## ✨ Carte Speciali

Le figure e i jolly introducono colpi di scena e ribaltamenti tattici non appena vengono scoperti sul tavolo:

| Carta | Nome | Effetto Meccanico |
| :---: | :--- | :--- |
| **J** | **Jack** (*Jack's Madness*) | Innesca una serie automatica e casuale di scambi tra carte dello stesso colore del seme del Jack. Il numero di scambi è determinato dal valore della prima carta del *Mazzo Scoperta* diminuito di 1. |
| **Q** | **Donna** (*Queen's Swipe*) | Permette al giocatore di **traslare un'intera colonna** verticalmente verso l'alto o verso il basso (tramite swipe gesture). |
| **K** | **Re** (*King's Rule*) | Permette al giocatore di **traslare un'intera riga** orizzontalmente verso destra o verso sinistra (tramite swipe gesture, non applicabile su righe potenti). |
| **★** | **Jolly** | Carta camaleontica: assume automaticamente il valore e il seme ideali per favorire l'ordinamento predominante della riga. Quando la vera carta sostituita viene scoperta, il Jolly si scambia con essa e viene rimpiazzato dal *Mazzo Scoperta*. |
| **7** | **Il Sette** *(La Trappola)* | Carta a doppio taglio: se un 7 viene rivelato e si trova in una colonna interna (non ai bordi $0$ o $6$), **si ricopre immediatamente e causa la fine immediata del turno del giocatore**! |

---

## 🎮 Modalità di Gioco

### 📱 Quick Play (Pass & Play)
- Due giocatori giocano sullo stesso smartphone o tablet.
- La metà superiore della schermata rappresenta il campo del Giocatore 2, mentre la metà inferiore appartiene al Giocatore 1.
- **Salvataggio & Ripristino (*Resume Match*)**: Se la partita viene interrotta o l'app chiusa, lo stato completo del match viene salvato su database locale Room e potrà essere ripreso al successivo avvio.

### 📡 Wireless Nearby Connections (P2P Locale)
- Due dispositivi Android comunicano direttamente tramite la tecnologia **Google Nearby Connections API** (Bluetooth Low Energy & Wi-Fi Aware / P2P).
- **Zero Server**: Non richiede connessione Internet né server centralizzati; rileva automaticamente le stanze e i dispositivi nel raggio di 20 metri.
- **Sincronizzazione in Tempo Reale**: Mosse, swipe, catene del Jack e cambi di turno vengono serializzati in JSON e scambiati a bassissima latenza.

---

## 🌟 Funzionalità Chiave

- **Profilo Giocatore & Avatar**: Creazione del profilo utente con scelta personalizzata tra 12+ avatar esclusivi.
- **Statistiche di Carriera Dettagliate**: Tracciamento di partite giocate, vinte, perse, pareggiate, Win Rate percentuale, serie di vittorie attuale (*Current Streak*) e record storico (*Best Streak*).
- **Riepilogo Post-Partita**: Schermata finale con banner del vincitore, tempo totale, mosse effettuate e conteggio delle figure scoperte.
- **Log Mosse Live**: Sistema di tracciamento ad eventi che descrive testualmente ogni azione avvenuta sul tavolo (rivelazioni, coperture, scambi, swipe).
- **Feedback Audio & AudioPlayer**: Effetti sonori custom dedicati per ogni figura, fanfara di vittoria, sconfitta, reveal del 7 e voce narrante per Jack, Donna e Re.
- **Manuale Regole Interattivo**: Schermata integrata di consultazione rapida delle regole con schemi visivi per orientamenti crescenti/decrescenti e poteri delle carte.

---

## 🏗️ Architettura e Tecnologie

L'applicazione segue i principi dell'architettura moderna Android:

```
it.di.unipi.sam636694.semelion
├── MainActivity.kt               # Entry point, gestione immersive full-screen e DI
├── appNavigation/                # Navigazione basata su Jetpack Navigation 3 e NavigationBar
│   ├── Navigation.kt
│   ├── NavigationBar.kt
│   └── Routes.kt
├── database/                     # Persistenza dati con Android Room & DAOs
│   ├── Daos.kt
│   ├── RoomEntities.kt           # Tabelle Partite, Utenti, Statistiche, Partecipazioni
│   └── SemelionDB.kt
├── ui/
│   ├── screens/                  # Schermate Jetpack Compose
│   │   ├── SemelionHomeScreen.kt
│   │   ├── Semelion Screen.kt    # Schermata del tavolo da gioco
│   │   ├── Grids.kt              # Griglia con vincoli dinamici, Drag & Drop e Gesture Swipe
│   │   ├── NearbyConnectionsScreen.kt
│   │   ├── MatchStatsScreen.kt
│   │   ├── RulesScreen.kt
│   │   └── UserProfilePage.kt
│   ├── snackbar/                 # Controller globale degli eventi SnackBar
│   ├── states/                   # UIStates, GamePhase, GameIntent (Pattern MVI)
│   └── theme/                    # Design System Material 3 (Colori, Tipografia, Temi)
├── utilities/
│   ├── AudioPlayer.kt            # Gestore riproduzione suoni (SoundPool / MediaPlayer)
│   ├── GameUtils.kt              # Algoritmi di calcolo Power Row, ordinamenti e azioni
│   ├── cardUtils.kt              # Mapping carte, semi e risorse grafiche
│   ├── connectionUtils/          # Wrapper per Google Nearby Connections API
│   └── serializers.kt            # Serializzazione/Deserializzazione Gson per il networking
└── viewModels/
    ├── gameModels/
    │   ├── BaseGameViewModel.kt  # Logica di business e regole di gioco (MVI)
    │   ├── SemelionGameViewModel.kt # Gestione modalità locale e resume match
    │   └── NearbyGameViewModel.kt   # Gestione sincronizzazione wireless P2P
    └── utilityModels/
        ├── UserProfileViewModel.kt
        ├── matchViewModel.kt
        └── logViewModel.kt
```

### Tecnologie Utilizzate:
- **Linguaggio**: Kotlin 2.x
- **UI Toolkit**: Jetpack Compose & Material Design 3
- **Pattern Architetturale**: MVI (Model-View-Intent) con Coroutine e `StateFlow`
- **Gestione Asincrona**: Kotlin Coroutines & Flow
- **Database Locale**: Android Room 2.8+ con KSP (Kotlin Symbol Processing)
- **Networking P2P**: Google Play Services Nearby Connections
- **Serializzazione**: Google Gson & Kotlinx Serialization

---

## 🚀 Installazione e Avvio

### Prerequisiti
- **Android Studio**: Ladybug / Meerkat o versioni successive
- **Android SDK**: Min SDK 24 (Android 7.0 Nougat) — Target SDK 36
- **JDK**: Java 11 o superiore
- Due dispositivi Android fisici con Bluetooth e Localizzazione attivi per testare la modalità wireless P2P.

### Compilazione ed Esecuzione
1. Clona la repository:
   ```bash
   git clone https://github.com/TaleWind28/Semelion.git
   cd Semelion
   ```
2. Apri il progetto con **Android Studio**.
3. Esegui la sincronizzazione dei file Gradle (*Sync Project with Gradle Files*).
4. Avvia l'applicazione su un emulatore o su un dispositivo fisico:
   ```bash
   ./gradlew installDebug
   ```

---

## 👥 Autori e Riconoscimenti

Progetto sviluppato nell'ambito dell'insegnamento di **Sviluppo Applicazioni Mobili (SAM)** presso il Dipartimento di Informatica dell'**Università di Pisa**.
