package it.unina.mc.engines;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

import it.unina.mc.core.ast.Formula;
import it.unina.mc.core.ast.Formula.Node;
import it.unina.mc.core.ast.Formula.Op;
import it.unina.mc.core.model.CGS;
import it.unina.mc.core.model.KripkeStructure;
import it.unina.mc.core.model.State;

public abstract class ModelChecker<M extends KripkeStructure, L extends Formula.Logic> {

    public Set<State> verify(M model, Formula<? extends L> formula) {
        Node<?> n = (Node<?>) formula;
        if (n.atom() != null && n.op() == null) {
            return filterStates(model, s -> s.hasLabel(n.atom()));
        }
        return switch (n.op()) {
            case NOT -> difference(model.getStates(), evalNode(model, n.left()));
            case AND -> intersection(evalNode(model, n.left()), evalNode(model, n.right()));
            case OR -> union(evalNode(model, n.left()), evalNode(model, n.right()));
            case IMPLIES -> union(difference(model.getStates(), evalNode(model, n.left())), evalNode(model, n.right()));
            case IFF -> {
                Set<State> l = evalNode(model, n.left());
                Set<State> r = evalNode(model, n.right());
                yield union(intersection(l, r),
                        intersection(difference(model.getStates(), l), difference(model.getStates(), r)));
            }
            default -> evaluateModal(model, n);
        };
    }

    public boolean holdsAt(M model, String stateId, Formula<? extends L> formula) {
        return verify(model, formula).contains(model.getState(stateId));
    }

    @SuppressWarnings("unchecked")
    protected Set<State> evalNode(M model, Formula<?> f) {
        return verify(model, (Formula<L>) f);
    }

    protected abstract Set<State> evaluateModal(M model, Node<?> node);

    // --- Calcolo Unificato dei Punti Fissi (CTL e ATL) ---
    protected Set<State> evalFixpoint(
            M model,
            Op temporalOp,
            Set<State> leftSat,
            Set<State> rightSat,
            Set<String> coalition,
            boolean universal) {

        return switch (temporalOp) {
            case NEXT -> pre(model, leftSat, coalition, universal);
            case EVENTUALLY -> leastFixpoint(Z -> union(leftSat, pre(model, Z, coalition, universal)));
            case GLOBALLY ->
                greatestFixpoint(model.getStates(), Z -> intersection(leftSat, pre(model, Z, coalition, universal)));
            case UNTIL ->
                leastFixpoint(Z -> union(rightSat, intersection(leftSat, pre(model, Z, coalition, universal))));
            case RELEASE -> greatestFixpoint(model.getStates(),
                    Z -> intersection(rightSat, union(leftSat, pre(model, Z, coalition, universal))));
            default ->
                throw new IllegalArgumentException("Operatore temporale non valido per punto fisso: " + temporalOp);
        };
    }

    // --- Pre-Immagine Universale: EX / AX (Kripke) o CPre_A (CGS) ---
    protected Set<State> pre(M model, Set<State> target, Set<String> coalition, boolean universal) {
        if (coalition == null || !(model instanceof CGS cgs)) {
            return filterStates(model, s -> {
                List<State> succs = model.getSuccessors(s);
                return universal ? (!succs.isEmpty() && target.containsAll(succs))
                        : succs.stream().anyMatch(target::contains);
            });
        }
        return filterStates(model,
                s -> canCoalitionEnforce(cgs, s, new ArrayList<>(coalition), 0, new HashMap<>(), target));
    }

    private boolean canCoalitionEnforce(CGS cgs, State s, List<String> ags, int idx, Map<String, String> move,
            Set<State> target) {
        if (idx == ags.size()) {
            var matching = cgs.getOutgoing(s).stream()
                    .filter(t -> move.entrySet().stream().allMatch(e -> e.getValue().equals(t.moves().get(e.getKey()))))
                    .toList();
            return !matching.isEmpty() && matching.stream().allMatch(t -> target.contains(t.to()));
        }
        for (String act : cgs.getAvailableActions(s, ags.get(idx))) {
            move.put(ags.get(idx), act);
            if (canCoalitionEnforce(cgs, s, ags, idx + 1, move, target))
                return true;
            move.remove(ags.get(idx));
        }
        return false;
    }

    protected Set<State> leastFixpoint(UnaryOperator<Set<State>> f) {
        Set<State> z = Set.of();
        while (true) {
            Set<State> next = f.apply(z);
            if (next.equals(z))
                return z;
            z = next;
        }
    }

    protected Set<State> greatestFixpoint(Collection<State> universe, UnaryOperator<Set<State>> f) {
        Set<State> z = new LinkedHashSet<>(universe);
        while (true) {
            Set<State> next = f.apply(z);
            if (next.equals(z))
                return z;
            z = next;
        }
    }

    protected Set<State> filterStates(M m, java.util.function.Predicate<State> p) {
        Set<State> r = new LinkedHashSet<>();
        for (State s : m.getStates())
            if (p.test(s))
                r.add(s);
        return r;
    }

    protected Set<State> union(Collection<State> a, Collection<State> b) {
        Set<State> r = new LinkedHashSet<>(a);
        r.addAll(b);
        return r;
    }

    protected Set<State> intersection(Collection<State> a, Collection<State> b) {
        Set<State> r = new LinkedHashSet<>(a);
        r.retainAll(b);
        return r;
    }

    protected Set<State> difference(Collection<State> a, Collection<State> b) {
        Set<State> r = new LinkedHashSet<>(a);
        r.removeAll(b);
        return r;
    }
}