package it.unina.mc.core.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class State {
    private final String id;
    private final Set<String> labels = new HashSet<>();

    public State(String id, String... trueLabels) {
        this.id = id;
        this.labels.addAll(Arrays.asList(trueLabels));
    }

    public String getId() {
        return id;
    }

    public boolean hasLabel(String label) {
        return labels.contains(label);
    }

    public Set<String> getLabels() {
        return labels;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof State s && Objects.equals(id, s.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}