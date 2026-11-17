package it.unina.mc.core.ast;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface Formula<L extends Formula.Logic> {

    // 1. GERARCHIA MARKER (Aggiunto CapAtl)
    interface Logic {
    }

    interface Ltl extends CtlStar {
    }

    interface Ctl extends CtlStar {
    }

    interface CtlStar extends Logic {
    }

    interface Atl extends CapAtl {
    }

    interface CapAtl extends Logic {
    }

    @SuppressWarnings("unused")
    interface Prop extends Ltl, Ctl, CtlStar, Atl, CapAtl {
    }

    enum Op {
        NOT, AND, OR, IMPLIES, IFF,
        NEXT, EVENTUALLY, GLOBALLY, UNTIL, RELEASE,
        EXISTS_PATH, FORALL_PATHS, COALITION_PATH
    }

    // 2. NODO RECORD UNIVERSALE (Aggiunta Map<String, Integer> capacity)
    record Node<L extends Logic>(
            Op op,
            String atom,
            Set<String> coalition,
            Map<String, Integer> capacity,
            Formula<L> left,
            Formula<L> right) implements Formula<L> {

        public Node(Op op, String atom, Set<String> coalition, Formula<L> left, Formula<L> right) {
            this(op, atom, coalition, null, left, right);
        }

        @Override
        public String toString() {
            if (atom != null && op == null)
                return atom;
            return switch (op) {
                case NOT -> "!" + left;
                case AND -> "(" + left + " & " + right + ")";
                case OR -> "(" + left + " | " + right + ")";
                case IMPLIES -> "(" + left + " -> " + right + ")";
                case IFF -> "(" + left + " <-> " + right + ")";
                case NEXT -> "X " + left;
                case EVENTUALLY -> "F " + left;
                case GLOBALLY -> "G " + left;
                case UNTIL -> "(" + left + " U " + right + ")";
                case RELEASE -> "(" + left + " R " + right + ")";
                case EXISTS_PATH -> "E(" + left + ")";
                case FORALL_PATHS -> "A(" + left + ")";
                case COALITION_PATH -> {
                    if (capacity == null || capacity.isEmpty()) {
                        yield "<<" + String.join(",", coalition) + ">>(" + left + ")";
                    }
                    String capStr = capacity.entrySet().stream()
                            .map(e -> e.getKey() + ":" + e.getValue())
                            .collect(Collectors.joining(","));
                    yield "<<" + capStr + ">>(" + left + ")";
                }
            };
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Logic> Formula<T> cast(Formula<?> f) {
        return (Formula<T>) f;
    }

    // 3. METODI DEFAULT FLUENTI
    default Formula<L> not() {
        return new Node<>(Op.NOT, null, null, this, null);
    }

    default <R extends Logic> Formula<R> and(Formula<R> r) {
        return new Node<>(Op.AND, null, null, cast(this), cast(r));
    }

    default <R extends Logic> Formula<R> or(Formula<R> r) {
        return new Node<>(Op.OR, null, null, cast(this), cast(r));
    }

    default <R extends Logic> Formula<R> implies(Formula<R> r) {
        return new Node<>(Op.IMPLIES, null, null, cast(this), cast(r));
    }

    default <R extends Logic> Formula<R> iff(Formula<R> r) {
        return new Node<>(Op.IFF, null, null, cast(this), cast(r));
    }

    default Formula<Ltl> next() {
        return X(cast(this));
    }

    default Formula<Ltl> finally_() {
        return F(cast(this));
    }

    default Formula<Ltl> globally() {
        return G(cast(this));
    }

    default Formula<Ltl> until(Formula<? extends CtlStar> target) {
        return U(cast(this), target);
    }

    default Formula<Ltl> release(Formula<? extends CtlStar> target) {
        return R(cast(this), target);
    }

    default Formula<Atl> enforcedNextBy(String... agents) {
        return atlX(new LinkedHashSet<>(Arrays.asList(agents)), cast(this));
    }

    default Formula<Atl> enforcedFinallyBy(String... agents) {
        return atlF(new LinkedHashSet<>(Arrays.asList(agents)), cast(this));
    }

    default Formula<Atl> maintainedBy(String... agents) {
        return atlG(new LinkedHashSet<>(Arrays.asList(agents)), cast(this));
    }

    // --- Metodi Fluenti per Capacity ATL ---
    default Formula<CapAtl> enforcedNextWithCap(Map<String, Integer> cap) {
        return capAtlX(cap, cast(this));
    }

    default Formula<CapAtl> enforcedFinallyWithCap(Map<String, Integer> cap) {
        return capAtlF(cap, cast(this));
    }

    default Formula<CapAtl> maintainedWithCap(Map<String, Integer> cap) {
        return capAtlG(cap, cast(this));
    }

    // 4. METODI STATIC FACTORY
    static Formula<Prop> atom(String name) {
        return new Node<>(null, name, null, null, null);
    }

    static <L extends Logic> Formula<L> not(Formula<L> f) {
        return new Node<>(Op.NOT, null, null, f, null);
    }

    static <L extends Logic> Formula<L> and(Formula<? extends L> l, Formula<? extends L> r) {
        return new Node<>(Op.AND, null, null, cast(l), cast(r));
    }

    static <L extends Logic> Formula<L> or(Formula<? extends L> l, Formula<? extends L> r) {
        return new Node<>(Op.OR, null, null, cast(l), cast(r));
    }

    static <L extends Logic> Formula<L> implies(Formula<? extends L> l, Formula<? extends L> r) {
        return new Node<>(Op.IMPLIES, null, null, cast(l), cast(r));
    }

    static <L extends Logic> Formula<L> iff(Formula<? extends L> l, Formula<? extends L> r) {
        return new Node<>(Op.IFF, null, null, cast(l), cast(r));
    }

    // --- LTL ---
    static Formula<Ltl> X(Formula<? extends CtlStar> f) {
        return new Node<>(Op.NEXT, null, null, cast(f), null);
    }

    static Formula<Ltl> F(Formula<? extends CtlStar> f) {
        return new Node<>(Op.EVENTUALLY, null, null, cast(f), null);
    }

    static Formula<Ltl> G(Formula<? extends CtlStar> f) {
        return new Node<>(Op.GLOBALLY, null, null, cast(f), null);
    }

    static Formula<Ltl> U(Formula<? extends CtlStar> l, Formula<? extends CtlStar> r) {
        return new Node<>(Op.UNTIL, null, null, cast(l), cast(r));
    }

    static Formula<Ltl> R(Formula<? extends CtlStar> l, Formula<? extends CtlStar> r) {
        return new Node<>(Op.RELEASE, null, null, cast(l), cast(r));
    }

    // --- CTL ---
    static Formula<Ctl> EX(Formula<? extends CtlStar> f) {
        return new Node<>(Op.EXISTS_PATH, null, null, cast(X(f)), null);
    }

    static Formula<Ctl> AX(Formula<? extends CtlStar> f) {
        return new Node<>(Op.FORALL_PATHS, null, null, cast(X(f)), null);
    }

    static Formula<Ctl> EF(Formula<? extends CtlStar> f) {
        return new Node<>(Op.EXISTS_PATH, null, null, cast(F(f)), null);
    }

    static Formula<Ctl> AF(Formula<? extends CtlStar> f) {
        return new Node<>(Op.FORALL_PATHS, null, null, cast(F(f)), null);
    }

    static Formula<Ctl> EG(Formula<? extends CtlStar> f) {
        return new Node<>(Op.EXISTS_PATH, null, null, cast(G(f)), null);
    }

    static Formula<Ctl> AG(Formula<? extends CtlStar> f) {
        return new Node<>(Op.FORALL_PATHS, null, null, cast(G(f)), null);
    }

    static Formula<Ctl> EU(Formula<? extends CtlStar> l, Formula<? extends CtlStar> r) {
        return new Node<>(Op.EXISTS_PATH, null, null, cast(U(l, r)), null);
    }

    static Formula<Ctl> AU(Formula<? extends CtlStar> l, Formula<? extends CtlStar> r) {
        return new Node<>(Op.FORALL_PATHS, null, null, cast(U(l, r)), null);
    }

    // --- CTL* ---
    static Formula<CtlStar> E(Formula<? extends CtlStar> path) {
        return new Node<>(Op.EXISTS_PATH, null, null, cast(path), null);
    }

    static Formula<CtlStar> A(Formula<? extends CtlStar> path) {
        return new Node<>(Op.FORALL_PATHS, null, null, cast(path), null);
    }

    // --- ATL Standard ---
    static Formula<Atl> atlX(Set<String> ag, Formula<? extends Atl> f) {
        return new Node<>(Op.COALITION_PATH, null, ag, new Node<>(Op.NEXT, null, null, cast(f), null), null);
    }

    static Formula<Atl> atlF(Set<String> ag, Formula<? extends Atl> f) {
        return new Node<>(Op.COALITION_PATH, null, ag, new Node<>(Op.EVENTUALLY, null, null, cast(f), null), null);
    }

    static Formula<Atl> atlG(Set<String> ag, Formula<? extends Atl> f) {
        return new Node<>(Op.COALITION_PATH, null, ag, new Node<>(Op.GLOBALLY, null, null, cast(f), null), null);
    }

    static Formula<Atl> atlU(Set<String> ag, Formula<? extends Atl> l, Formula<? extends Atl> r) {
        return new Node<>(Op.COALITION_PATH, null, ag, new Node<>(Op.UNTIL, null, null, cast(l), cast(r)), null);
    }

    // --- Capacity ATL (Coalizione con Mappa di Capacità Agente -> Budget) ---
    static Formula<CapAtl> capAtlX(Map<String, Integer> cap, Formula<? extends CapAtl> f) {
        return new Node<>(Op.COALITION_PATH, null, cap.keySet(), cap, new Node<>(Op.NEXT, null, null, cast(f), null),
                null);
    }

    static Formula<CapAtl> capAtlF(Map<String, Integer> cap, Formula<? extends CapAtl> f) {
        return new Node<>(Op.COALITION_PATH, null, cap.keySet(), cap,
                new Node<>(Op.EVENTUALLY, null, null, cast(f), null),
                null);
    }

    static Formula<CapAtl> capAtlG(Map<String, Integer> cap, Formula<? extends CapAtl> f) {
        return new Node<>(Op.COALITION_PATH, null, cap.keySet(), cap,
                new Node<>(Op.GLOBALLY, null, null, cast(f), null), null);
    }

    static Formula<CapAtl> capAtlU(Map<String, Integer> cap, Formula<? extends CapAtl> l, Formula<? extends CapAtl> r) {
        return new Node<>(Op.COALITION_PATH, null, cap.keySet(), cap,
                new Node<>(Op.UNTIL, null, null, cast(l), cast(r)), null);
    }
}