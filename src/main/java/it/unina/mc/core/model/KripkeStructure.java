package it.unina.mc.core.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KripkeStructure {
    protected final Map<String, State> states = new LinkedHashMap<>();
    protected final Set<Transition> transitions = new LinkedHashSet<>();

    public State addState(String id, String... labels) {
        State s = new State(id, labels);
        states.put(id, s);
        return s;
    }

    public State getState(String id) {
        return states.get(id);
    }

    public Collection<State> getStates() {
        return states.values();
    }

    public void addTransition(String fromId, String toId) {
        transitions.add(new Transition(states.get(fromId), states.get(toId)));
    }

    public List<Transition> getOutgoing(State s) {
        return transitions.stream().filter(t -> t.from().equals(s)).toList();
    }

    public List<State> getSuccessors(State s) {
        return getOutgoing(s).stream().map(Transition::to).distinct().toList();
    }
}