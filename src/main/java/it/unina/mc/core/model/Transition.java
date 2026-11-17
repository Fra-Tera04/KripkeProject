package it.unina.mc.core.model;

import java.util.Map;

public record Transition(State from, State to, Map<String, String> moves) {
    public Transition(State from, State to) {
        this(from, to, Map.of());
    }
}