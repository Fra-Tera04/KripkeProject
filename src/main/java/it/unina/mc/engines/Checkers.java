package it.unina.mc.engines;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unina.mc.core.ast.Formula;
import it.unina.mc.core.ast.Formula.Atl;
import it.unina.mc.core.ast.Formula.CapAtl;
import it.unina.mc.core.ast.Formula.Ctl;
import it.unina.mc.core.ast.Formula.CtlStar;
import it.unina.mc.core.ast.Formula.Ltl;
import it.unina.mc.core.ast.Formula.Node;
import it.unina.mc.core.ast.Formula.Op;
import it.unina.mc.core.model.CGS;
import it.unina.mc.core.model.KripkeStructure;
import it.unina.mc.core.model.State;

public class Checkers {

    // 1. MOTORE CTL (Valuta EXISTS_PATH e FORALL_PATHS su KripkeStructure)
    public static class CtlChecker extends ModelChecker<KripkeStructure, Ctl> {
        @Override
        protected Set<State> evaluateModal(KripkeStructure m, Node<?> n) {
            if (n.op() == Op.EXISTS_PATH || n.op() == Op.FORALL_PATHS) {
                boolean universal = (n.op() == Op.FORALL_PATHS);
                Node<?> temporal = (Node<?>) n.left();
                Set<State> l = evalNode(m, temporal.left());
                Set<State> r = temporal.right() != null ? evalNode(m, temporal.right()) : Set.of();
                return evalFixpoint(m, temporal.op(), l, r, null, universal);
            }
            throw new IllegalArgumentException("Richiesto quantificatore CTL (EXISTS_PATH / FORALL_PATHS): " + n);
        }
    }

    // 2. MOTORE ATL (Valuta COALITION_PATH su CGS)
    public static class AtlChecker extends ModelChecker<CGS, Atl> {
        @Override
        protected Set<State> evaluateModal(CGS m, Node<?> n) {
            if (n.op() == Op.COALITION_PATH) {
                Node<?> temporal = (Node<?>) n.left();
                Set<State> l = evalNode(m, temporal.left());
                Set<State> r = temporal.right() != null ? evalNode(m, temporal.right()) : Set.of();
                return evalFixpoint(m, temporal.op(), l, r, n.coalition(), false);
            }
            throw new IllegalArgumentException("Richiesto quantificatore ATL (COALITION_PATH): " + n);
        }
    }

    // 3. MOTORE LTL (Valuta NEXT, EVENTUALLY, GLOBALLY, UNTIL, RELEASE su cammini)
    public static class LtlChecker extends ModelChecker<KripkeStructure, Ltl> {
        @Override
        protected Set<State> evaluateModal(KripkeStructure m, Node<?> n) {
            return filterStates(m, s -> buildLassos(m, new ArrayList<>(List.of(s))).stream()
                    .allMatch(trace -> evalTrace(m, n, trace, 0)));
        }

        protected boolean evalTrace(KripkeStructure m, Formula<?> f, List<State> trace, int i) {
            int pos = Math.min(i, trace.size() - 1);
            Node<?> n = (Node<?>) f;
            if (n.atom() != null && n.op() == null)
                return evalNode(m, n).contains(trace.get(pos));

            return switch (n.op()) {
                case NOT -> !evalTrace(m, n.left(), trace, pos);
                case AND -> evalTrace(m, n.left(), trace, pos) && evalTrace(m, n.right(), trace, pos);
                case OR -> evalTrace(m, n.left(), trace, pos) || evalTrace(m, n.right(), trace, pos);
                case IMPLIES -> !evalTrace(m, n.left(), trace, pos) || evalTrace(m, n.right(), trace, pos);
                case IFF -> evalTrace(m, n.left(), trace, pos) == evalTrace(m, n.right(), trace, pos);
                case NEXT -> evalTrace(m, n.left(), trace, pos + 1);
                case EVENTUALLY -> {
                    for (int k = pos; k < trace.size(); k++)
                        if (evalTrace(m, n.left(), trace, k))
                            yield true;
                    yield false;
                }
                case GLOBALLY -> {
                    for (int k = pos; k < trace.size(); k++)
                        if (!evalTrace(m, n.left(), trace, k))
                            yield false;
                    yield true;
                }
                case UNTIL -> {
                    for (int k = pos; k < trace.size(); k++) {
                        if (evalTrace(m, n.right(), trace, k))
                            yield true;
                        if (!evalTrace(m, n.left(), trace, k))
                            yield false;
                    }
                    yield false;
                }
                case RELEASE -> {
                    for (int k = pos; k < trace.size(); k++) {
                        if (!evalTrace(m, n.right(), trace, k))
                            yield false;
                        if (evalTrace(m, n.left(), trace, k))
                            yield true;
                    }
                    yield true;
                }
                default -> evalNode(m, n).contains(trace.get(pos));
            };
        }

        protected List<List<State>> buildLassos(KripkeStructure m, List<State> path) {
            List<List<State>> res = new ArrayList<>();
            State last = path.getLast();
            List<State> succs = m.getSuccessors(last);
            if (succs.isEmpty())
                return List.of(new ArrayList<>(path));
            for (State next : succs) {
                int idx = path.indexOf(next);
                if (idx != -1) {
                    List<State> lasso = new ArrayList<>(path);
                    List<State> loop = path.subList(idx, path.size());
                    lasso.addAll(loop);
                    res.add(lasso);
                } else {
                    path.add(next);
                    res.addAll(buildLassos(m, path));
                    path.removeLast();
                }
            }
            return res;
        }
    }

    // 4. MOTORE CTL* (Unifica quantificatori di stato e cammini arbitrari LTL)
    public static class CtlStarChecker extends ModelChecker<KripkeStructure, CtlStar> {
        private final LtlChecker ltlHelper = new LtlChecker();

        @Override
        protected Set<State> evaluateModal(KripkeStructure m, Node<?> n) {
            if (n.op() == Op.EXISTS_PATH) {
                return filterStates(m, s -> ltlHelper.buildLassos(m, new ArrayList<>(List.of(s))).stream()
                        .anyMatch(t -> ltlHelper.evalTrace(m, n.left(), t, 0)));
            }
            if (n.op() == Op.FORALL_PATHS) {
                return filterStates(m, s -> ltlHelper.buildLassos(m, new ArrayList<>(List.of(s))).stream()
                        .allMatch(t -> ltlHelper.evalTrace(m, n.left(), t, 0)));
            }
            throw new IllegalArgumentException("Operatore non supportato in CTL*: " + n);
        }
    }

    // 5. MOTORE CAPACITY ATL (Valuta COALITION_PATH con vincoli di budget su CGS)
    public static class CapAtlChecker extends ModelChecker<CGS, CapAtl> {

        @Override
        protected Set<State> evaluateModal(CGS m, Node<?> n) {
            if (n.op() != Op.COALITION_PATH) {
                throw new IllegalArgumentException("Richiesto COALITION_PATH per CapAtlChecker: " + n);
            }
            Node<?> temporal = (Node<?>) n.left();
            Set<State> l = evalNode(m, temporal.left());
            Set<State> r = temporal.right() != null ? evalNode(m, temporal.right()) : Set.of();

            // Se non c'è capacità specificata (formula ATL standard), usa il punto fisso
            // classico
            if (n.capacity() == null || n.capacity().isEmpty()) {
                return evalFixpoint(m, temporal.op(), l, r, n.coalition(), false);
            }

            Map<String, Integer> cap = n.capacity();
            return switch (temporal.op()) {
                case NEXT -> filterStates(m,
                        s -> canEnforceStep(m, s, new ArrayList<>(cap.keySet()), 0, new java.util.HashMap<>(), cap, l));
                case EVENTUALLY ->
                    evalCapUntil(m, cap, new java.util.LinkedHashSet<>(m.getStates()), l, new java.util.HashMap<>());
                case UNTIL -> evalCapUntil(m, cap, l, r, new java.util.HashMap<>());
                case GLOBALLY -> evalCapGlobally(m, cap, l, new java.util.HashMap<>());
                default -> throw new IllegalArgumentException(
                        "Operatore temporale non supportato in CapATL: " + temporal.op());
            };
        }

        // Induzione sul reticolo dei budget + Minimo Punto Fisso (mu Z) per Until /
        // Finally
        private Set<State> evalCapUntil(
                CGS m,
                Map<String, Integer> budget,
                Set<State> phi1,
                Set<State> phi2,
                Map<Object, Set<State>> memo) {

            if (memo.containsKey(budget))
                return memo.get(budget);

            Set<State> result = leastFixpoint(Z -> {
                Set<State> canStep = filterStates(m,
                        s -> canEnforceDynamic(m, s, new ArrayList<>(budget.keySet()), 0, new java.util.HashMap<>(),
                                budget, Z,
                                nextBudget -> evalCapUntil(m, nextBudget, phi1, phi2, memo)));
                return union(phi2, intersection(phi1, canStep));
            });

            memo.put(budget, result);
            return result;
        }

        // Induzione sul reticolo dei budget + Massimo Punto Fisso (nu Z) per Globally
        private Set<State> evalCapGlobally(
                CGS m,
                Map<String, Integer> budget,
                Set<State> invariant,
                Map<Object, Set<State>> memo) {

            if (memo.containsKey(budget))
                return memo.get(budget);

            Set<State> result = greatestFixpoint(m.getStates(), Z -> {
                Set<State> canStep = filterStates(m,
                        s -> canEnforceDynamic(m, s, new ArrayList<>(budget.keySet()), 0, new java.util.HashMap<>(),
                                budget, Z,
                                nextBudget -> evalCapGlobally(m, nextBudget, invariant, memo)));
                return intersection(invariant, canStep);
            });

            memo.put(budget, result);
            return result;
        }

        // Verifica a 1 passo (NEXT) che non supera il budget
        private boolean canEnforceStep(
                CGS m, State s, List<String> ags, int idx,
                Map<String, String> move, Map<String, Integer> budget, Set<State> target) {

            if (idx == ags.size()) {
                var matching = m.getOutgoing(s).stream()
                        .filter(t -> move.entrySet().stream()
                                .allMatch(e -> e.getValue().equals(t.moves().get(e.getKey()))))
                        .toList();
                return !matching.isEmpty() && matching.stream().allMatch(t -> target.contains(t.to()));
            }
            String ag = ags.get(idx);
            for (String act : m.getAvailableActions(s, ag)) {
                if (m.getActionCost(ag, act) <= budget.getOrDefault(ag, 0)) {
                    move.put(ag, act);
                    if (canEnforceStep(m, s, ags, idx + 1, move, budget, target))
                        return true;
                    move.remove(ag);
                }
            }
            return false;
        }

        // Verifica dinamica multi-passo: se costo == 0 resta in sameBudgetTarget (Z),
        // se costo > 0 usa il sottoproblema con budget ridotto
        private boolean canEnforceDynamic(
                CGS m, State s, List<String> ags, int idx,
                Map<String, String> move, Map<String, Integer> budget,
                Set<State> sameBudgetTarget,
                java.util.function.Function<Map<String, Integer>, Set<State>> lowerBudgetSolver) {

            if (idx == ags.size()) {
                Map<String, Integer> nextBudget = new java.util.LinkedHashMap<>();
                boolean spentAnyResource = false;
                for (String ag : ags) {
                    int cost = m.getActionCost(ag, move.get(ag));
                    nextBudget.put(ag, budget.get(ag) - cost);
                    if (cost > 0) spentAnyResource = true;
                }
                Set<State> validTarget = spentAnyResource ? lowerBudgetSolver.apply(nextBudget) : sameBudgetTarget;
                var matching = m.getOutgoing(s).stream()
                    .filter(t -> move.entrySet().stream().allMatch(e -> e.getValue().equals(t.moves().get(e.getKey()))))
                    .toList();
                return !matching.isEmpty() && matching.stream().allMatch(t -> validTarget.contains(t.to()));
            }
            String ag = ags.get(idx);
            for (String act : m.getAvailableActions(s, ag)) {
                if (m.getActionCost(ag, act) <= budget.getOrDefault(ag, 0)) {
                    move.put(ag, act);
                    if (canEnforceDynamic(m, s, ags, idx + 1, move, budget, sameBudgetTarget, lowerBudgetSolver)) return true;
                    move.remove(ag);
                }
            }
            return false;
        }
    }
}