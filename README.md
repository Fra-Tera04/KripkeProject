# Kripke Model Checker Project

Progetto universitario per un motore di verifica formale (Model Checking) per logica modale, scritto in Java.

## Indice dei Contenuti

* [Che cos’è un modello di Kripke?](https://www.google.com/search?q=%23che-cos%C3%A8-un-modello-di-kripke)
* [Funzionalità Principali](https://www.google.com/search?q=%23funzionalit%C3%A0-principali)
* [Formule e Sintassi supportata](https://www.google.com/search?q=%23formule-e-sintassi-supportata)
* [Verifica](https://www.google.com/search?q=%23verifica)
* [Requisiti](https://www.google.com/search?q=%23requisiti)
* [Come iniziare](https://www.google.com/search?q=%23come-iniziare)
* [Esempio d'uso](https://www.google.com/search?q=%23esempio-duso)
* [Struttura del repository](https://www.google.com/search?q=%23struttura-del-repository)

## Intro

Progetto iniziato a fine settembre 2026 nell'ambito del corso *Formal Methods for Strategic Reasoning* con il Prof. X, dedicato alla realizzazione di modelli di Kripke per la verifica formale dei sistemi. Nato inizialmente come un semplice verificatore di formule proposizionali, il progetto si è progressivamente evoluto seguendo la traccia del corso per supportare la logica modale, in particolare quella epistemica multi-agente, fino a gettare le basi per logiche temporali e lineari.

Successivamente, il codice è stato strutturato in un progetto organico, versionato con Git e documentato per scopi dimostrativi e di portfolio. L'implementazione unisce rigore accademico e programmazione Java avanzata (Java 21+), sfruttando costrutti moderni come *Sealed Interfaces*, *Records* e *Pattern Matching for switch* per offrire un'architettura immutabile, pulita ed efficiente sia in contesti accademici sia in scenari di stress testing.

## Che cos’è un modello di Kripke?

Un modello di Kripke è una struttura $M = (W, R, V)$, composta da:

* **W**: un insieme di stati;
* **R**: una relazione di transizione tra gli stati;
* **V**: una funzione che associa a ogni stato le proposizioni atomiche vere in quello stato.

Gli stati descrivono le configurazioni del sistema, le transizioni indicano come può evolvere, e le proposizioni etichettano le proprietà vere in ciascuna configurazione.

## Funzionalità Principali

* **Pattern Builder per i Modelli**: Configurazione fluida della struttura di Kripke e generazione automatica di relazioni di equivalenza (riflessive, simmetriche e transitive) tramite il metodo `relEquivalenza`.
* **Fluent API & Syntactic Sugar**: Costruzione di formule logiche in modo dichiarativo e leggibile (es. `atom("p").implies(knows("Alice", atom("q")))`).
* **Recursive Descent Parser Integrato**: Un parser basato su Lexer/Regex che converte istantaneamente stringhe testuali in un Albero Sintattico Astratto (AST), gestendo automaticamente le precedenze (parentesi > operatori unari > AND > OR > Implica).
* **Motore di Model Checking**:
    * Supporta la **verifica locale** ($M, q \models \phi$) e il calcolo dell'**estensione globale** ($\vert{}\vert{}\phi\vert{}\vert{}_M$).
    * Utilizza la **Stream API** per applicare lo *short-circuiting* matematico: funzioni come `isValid` ($\forall$) e `isSatisfiable` ($\exists$) si fermano al primo controesempio o testimone trovato.
    * Converte le formule in forma NNF (*Negation Normal Form*) spingendo le negazioni in profondità tramite le leggi di De Morgan e la dualità modale.


* **Pretty Printing**: Visualizzazione a terminale con struttura ad albero chiara e predicibile (ordinata alfabeticamente) per ispezionare topologia e transizioni epistemiche.

[Indice](https://www.google.com/search?q=%23indice-dei-contenuti)

## Formule e Sintassi supportata

Il parser (`FormulaParser.parse`) riconosce nativamente la seguente sintassi:

* **Atomi**: `pos1`, `rain`, `p`
* **Operatori Booleani**: Negazione (`-`, `~`, `¬`, `!`), Congiunzione (`&`), Disgiunzione (`|`), Implicazione (`->`).
* **Operatori Epistemici (Modali)**:
* Conoscenza: `K_1 p` (L'agente 1 *sa* che p è vera)
* Possibilità: `<K_1> p` (L'agente 1 ritiene *possibile* che p sia vera)


* Le parentesi `( )` possono essere usate liberamente per raggruppare sotto-formule. L'implicazione è associativa a destra.

La classe `Model` permette di aggiungere:
* Stati con le rispettive proposizioni (`addState()`)
* Tansizioni orientate (`connect()`) 
* Creare relazioni di equivalenza per un agente (`relEquivalenza()`), che collega in entrambe le direzioni tutti gli stati del gruppo, inclusi gli auto-collegamenti.

`Formula` offre: 
* Costruttori fluenti e tipi immutabili per atomi, negazione, congiunzione, disgiunzione, implicazione, conoscenza e possibilità epistemica. 
* È disponibile anche `iff`, definita come le due implicazioni in entrambe le direzioni.

[Indice](https://www.google.com/search?q=%23indice-dei-contenuti)

## Verifica

Per la verifica vera e propria usiamo `ModelChecker` che fornisce:

* `check(model, stato, formula)` per la verifica locale;
* `extension(model, formula)` per ottenere gli stati in cui la formula è vera;
* `isSatisfiable(...)` e `isValid(...)` per verificare se una formula è vera, rispettivamente, in almeno uno o in tutti gli stati;
* `toNNF(...)` per trasformare una formula in forma normale negativa.

Le formule epistemiche sono valutate sugli stati restituiti da `Model.next(agente, stato)`. Se non ci sono stati accessibili per un agente, la conoscenza risulta vera e la possibilità falsa per qualsiasi sottoformula.

[Indice](https://www.google.com/search?q=%23indice-dei-contenuti)

## Requisiti

* JDK 21 o successivo: il codice usa `SequencedSet` e pattern matching per `switch`.
* Un IDE Java o gli strumenti di build configurati nel progetto.

## Come iniziare

1. Clonare o scaricare il repository.
2. Aprire la cartella del progetto in un IDE Java oppure importarla con il sistema di build configurato.
3. Eseguire la classe principale contenente gli esempi di costruzione del modello e verifica delle formule.

[Indice](https://www.google.com/search?q=%23indice-dei-contenuti)

## Esempio d'uso

Il seguente snippet dimostra come inizializzare un modello epistemico in cui due robot (agenti 1 e 2) osservano la posizione di un carrello, ma soffrono di incertezza sensoriale su alcuni stati:

```java
import org.fmsl.prog.kripke.*;

public class Main {
    public static void main(String[] args) {
        // 1. Costruzione del modello tramite Builder Pattern
        Model model = new Model()
            .addState("q0", "pos0")
            .addState("q1", "pos1")
            .addState("q2", "pos2")
            // L'agente 1 non distingue tra lo stato q0 e q2
            .relEquivalenza("1", "q0", "q2")
            .relEquivalenza("1", "q1") 
            // L'agente 2 non distingue tra lo stato q0 e q1
            .relEquivalenza("2", "q0", "q1")
            .relEquivalenza("2", "q2");

        // 2. Parsing istantaneo di formule logiche complesse
        Formula f3 = FormulaParser.parse("pos2 -> K_2 K_1 ~pos1");

        // 3. Esecuzione delle query di Model Checking
        System.out.println("M, q2 |= f3 ? " + ModelChecker.check(model, "q2", f3));
        System.out.println("Stati in cui f3 è valida: " + ModelChecker.extension(model, f3));
        System.out.println("La formula f3 è soddisfacibile nel modello? " + ModelChecker.isSatisfiable(model, f3));
    }
}

```

## Struttura del repository

Le classi Java principali si trovano in `src/main/java/org/fmsl/prog/kripke/`:

* `ProjectKripke.java`: esempio eseguibile;
* `Model.java`: stati, proposizioni e relazioni per agente;
* `Formula.java`: tipi di formula, costruttori fluenti e parser;
    * `FormulaParser.java`: il parser testuale basato su Regex;
* `ModelChecker.java`: valutazione e trasformazioni delle formule.

Consultare le altre cartelle del repository per eventuali test e configurazioni di build.

[Indice](https://www.google.com/search?q=%23indice-dei-contenuti)