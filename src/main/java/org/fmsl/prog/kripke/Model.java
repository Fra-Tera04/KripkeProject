package org.fmsl.prog.kripke;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Un modello di Kripke: degli "stati", per ogni stato l'elenco delle
 * proposizioni
 * vere lì, e per ogni agente delle frecce tra stati.
 *
 * Una freccia agente: s0 -> s1 significa "se siamo in s0, l'agente considera
 * possibile di essere in s1".
 *
 * Dentro è solo una lista di adiacenza (agente -> stato -> stati
 * raggiungibili):
 * nessuna libreria di grafi usata al momento.
 */
public class Model {

    // val: Stato -> Set di proposizioni vere.
    // rel: Agente -> Stato di origine -> Set di stati destinazione
    private final Map<String, Set<String>> val = new HashMap<>();
    private final Map<String, Map<String, Set<String>>> rel = new HashMap<>();

    // ---------- costruzione ----------

    /**
     * Aggiunge uno o più stati e dice quali proposizioni sono vere lì.
     * 
     * @param state            lo stato da aggiungere
     * @param truePropositions le proposizioni vere nello stato
     * @return il modello modificato
     * @throws NullPointerException se state o truePropositions sono null
     */
    public Model addState(String state, String... truePropositions) {
        Objects.requireNonNull(state, "Lo stato non può essere null");
        Objects.requireNonNull(truePropositions, "Le proposizioni non possono essere null");

        val.computeIfAbsent(state, k -> new HashSet<>()).addAll(Arrays.asList(truePropositions));

        return this;
    }

    /**
     * Aggiunge una sola freccia: da 'from', l'agente considera possibile 'to'.
     * 
     * @param agent
     * @param from
     * @param to
     * @return il modello modificato
     * @throws IllegalArgumentException se from o to non sono stati del modello
     */
    public Model connect(String agent, String from, String to) {
        validateStateExists(from);
        validateStateExists(to);
        Objects.requireNonNull(agent, "L'agente non può essere nullo");

        rel.computeIfAbsent(agent, a -> new HashMap<>())
                .computeIfAbsent(from, s -> new LinkedHashSet<>())
                .add(to);

        return this;
    }

    /**
     * Crea rel di equivalenza tra gli stati dati per l'agente.
     * 
     * @param agent l'agente per cui creare stati di equivalenza
     * @param group i stati da includere nel gruppo di equivalenza
     * @return il modello modificato
     */
    public Model relEquivalenza(String agent, String... group) {
        Objects.requireNonNull(group, "Il gruppo di stati non può essere nullo");
        // qui non so se fare un controllo sul numero del gruppo != 0 o... no

        for (String x : group) {
            for (String y : group) {
                connect(agent, x, y);
            }
        }
        return this;
    }

    // ---------- interrogazioni ----------

    /**
     * Restituisce l'insieme di tutti gli stati del modello.
     *
     * @return unmodifiable set di stati
     */
    public Set<String> stati() {
        return Collections.unmodifiableSet(val.keySet());
    }

    /**
     * soddisfa: Verifica se una proposizione è vera in uno stato
     * 
     * @param prop  la proposizione da verificare
     * @param stato lo stato in cui verificare la proposizione
     * @return true o false
     * @throws IllegalArgumentException se lo stato non esiste nel modello
     */
    public boolean isTrueAt(String prop, String stato) {
        validateStateExists(stato);
        return val.getOrDefault(stato, Set.of()).contains(prop);
    }

    /**
     * Dati uno stato e un agente, restituisce tutti gli stati che l'agente
     * considera possibili da quello stato.
     * 
     * @param agent
     * @param stato
     * @return unmodifiable set di stati
     */
    public Set<String> next(String agent, String stato) {
        validateStateExists(stato);
        return Collections.unmodifiableSet(
                rel.getOrDefault(agent, Map.of())
                        .getOrDefault(stato, Set.of()));
    }

    /**
     * Tutti gli stati raggiungibili da 'start' seguendo le frecce di qualunque
     * agente (start incluso).
     * 
     * @param start lo stato di partenza
     * @return insieme di stati raggiungibili da start
     * @throws IllegalArgumentException se start non è uno stato del modello
     */
    public Set<String> reachableFrom(String start) {
        validateStateExists(start);

        Set<String> seen = new LinkedHashSet<>();
        Deque<String> todo = new ArrayDeque<>();

        seen.add(start);
        todo.add(start);

        while (!todo.isEmpty()) {
            String s = todo.poll();
            for (var byState : rel.values())
                for (String n : byState.getOrDefault(s, Set.of()))
                    if (seen.add(n))
                        todo.add(n);

        }
        return Collections.unmodifiableSet(seen);
    }

    /**
     * Verifica che uno stato esista nel modello, altrimenti lancia un'eccezione.
     * 
     * @param stato lo stato da verificare
     * @throws IllegalArgumentException se lo stato non esiste nel modello
     */
    public void validateStateExists(String stato) {
        Optional.ofNullable(stato).orElseThrow(() -> new IllegalArgumentException("Lo stato non può essere null"));

        if (!stati().contains(stato)) {
            throw new IllegalArgumentException("Stato sconosciuto al Modello: " + stati());
        }
    }

    /**
     * Restituisce una vista topologica del grafo ignorando gli agenti.
     */
    public String toStringTopology() {
        if (stati().isEmpty())
            return "Grafo: [vuoto]";

        StringBuilder sb = new StringBuilder();
        List<String> sortedStates = new ArrayList<>(stati());
        Collections.sort(sortedStates);

        for (String s : sortedStates) {
            // TreeSet elimina i duplicati tra agenti diversi e ordina i nodi
            Set<String> destinazioniGlobali = new TreeSet<>();

            for (var agentMap : rel.values()) {
                destinazioniGlobali.addAll(agentMap.getOrDefault(s, Set.of()));
            }

            sb.append(s)
                    .append(" ─> ")
                    .append(destinazioniGlobali.isEmpty() ? "[]" : destinazioniGlobali)
                    .append("\n");
        }
        return sb.toString().stripTrailing();
    }

    public String toStringAgents() {
        if (stati().isEmpty())
            return "Agenti: [vuoto]";

        StringBuilder sb = new StringBuilder();

        // Ordina gli stati alfabeticamente per una lettura sempre coerente
        List<String> sortedStates = new ArrayList<>(stati());
        Collections.sort(sortedStates);

        for (String s : sortedStates) {
            // 1. Stampa lo stato e le sue proposizioni sulla prima riga
            var props = val.getOrDefault(s, Set.of());
            sb.append(s).append(" ").append(props.isEmpty() ? "[]" : props).append("\n");

            // 2. Raccoglie e ordina le transizioni
            List<String> transizioni = new ArrayList<>();
            for (var entry : rel.entrySet()) {
                var agente = entry.getKey();
                var dest = entry.getValue().getOrDefault(s, Set.of());

                if (!dest.isEmpty()) {
                    List<String> sortedDest = new ArrayList<>(dest);
                    Collections.sort(sortedDest);
                    transizioni.add("agente " + agente + " ─> " + sortedDest);
                }
            }
            Collections.sort(transizioni);

            // 3. Stampa le transizioni con i caratteri ad albero (tree-like)
            for (int i = 0; i < transizioni.size(); i++) {
                boolean isLast = (i == transizioni.size() - 1);
                sb.append(isLast ? "  └─ " : "  ├─ ")
                        .append(transizioni.get(i))
                        .append("\n");
            }
        }
        return sb.toString().stripTrailing();
    }

    @Override
    public String toString() {
        return """
                Kripke-Model:
                ____________________________
                Agent view:
                %s
                ____________________________
                Topology view:
                %s
                ____________________________
                """.stripIndent().formatted(toStringAgents(), toStringTopology());
    }
}
