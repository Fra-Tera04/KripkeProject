package it.unina.mc.core.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CGS extends KripkeStructure {
    private final Set<String> agents = new LinkedHashSet<>();
    private final Map<String, Map<String, Integer>> actionCosts = new LinkedHashMap<>();

    public void addTransition(String fromId, String toId, Map<String, String> moves) {
        agents.addAll(moves.keySet());
        transitions.add(new Transition(states.get(fromId), states.get(toId), moves));
    }

    public void setActionCost(String agent, String action, int cost) {
        agents.add(agent);
        actionCosts.computeIfAbsent(agent, k -> new LinkedHashMap<>()).put(action, cost);
    }

    public int getActionCost(String agent, String action) {
        return actionCosts.getOrDefault(agent, Map.of()).getOrDefault(action, 0);
    }

    public Set<String> getAvailableActions(State s, String agent) {
        Set<String> actions = new LinkedHashSet<>();
        for (Transition t : getOutgoing(s)) {
            if (t.moves().containsKey(agent)) {
                actions.add(t.moves().get(agent));
            }
        }
        return actions;
    }

    public Set<String> getAgents() {
        return agents;
    }
}