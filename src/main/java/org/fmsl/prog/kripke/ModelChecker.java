package org.fmsl.prog.kripke;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.SequencedSet;
import java.util.stream.Collectors;

import org.fmsl.prog.kripke.Formula.And;
import org.fmsl.prog.kripke.Formula.Atom;
import org.fmsl.prog.kripke.Formula.Implies;
import org.fmsl.prog.kripke.Formula.Knows;
import org.fmsl.prog.kripke.Formula.Not;
import org.fmsl.prog.kripke.Formula.Or;
import org.fmsl.prog.kripke.Formula.Possible;

/**
 * Motore ricorsivo di verifica di soddisfacibilità semantica: M, s |= phi.
 * Metodi: check, extension, isSatisfiable, isValid, toNNF
 */
public final class ModelChecker {

    private ModelChecker() {
    }

    /** Local model checking: vale M, q |= phi ? */
    public static boolean check(Model model, String stato_q, Formula phi) {
        Objects.requireNonNull(model, "Il modello non può essere nullo");
        Objects.requireNonNull(stato_q, "Lo stato di valutazione non può essere nullo");
        Objects.requireNonNull(phi, "La formula non può essere nulla");

        return switch (phi) {
            case Atom(var p) ->
                model.isTrueAt(p, stato_q);

            case Not(var p) ->
                !check(model, stato_q, p);

            case And(var left, var right) ->
                check(model, stato_q, left) && check(model, stato_q, right);

            case Or(var left, var right) ->
                check(model, stato_q, left) || check(model, stato_q, right);

            case Implies(var left, var right) ->
                !check(model, stato_q, left) || check(model, stato_q, right);

            // ∀q': se q ~a q' allora mcheck(M, q', ψ)
            case Knows(var a, var f) ->
                model.next(a, stato_q).stream().allMatch(nextQ -> check(model, nextQ, f));

            // ∃q': q ~a q' and mcheck(M, q', ψ)
            case Possible(var a, var f) ->
                model.next(a, stato_q).stream().anyMatch(nextQ -> check(model, nextQ, f));

            // versione ottimizzara?

            // case Knows(var agent, var p) -> {
            // for (String next : model.next(agent, stato_q)) {
            // if (!check(model, next, p))
            // yield false;
            // }
            // yield true;
            // }

            // case Possible(var agent, var p) -> {
            // for (String next : model.next(agent, stato_q)) {
            // if (check(model, next, p))
            // yield true;
            // }
            // yield false;
            // }

        };

    }

    /**
     * Calcolo estensione globale: ||phi||_M = { w in W | M, w |= phi }
     * Tutti gli stati di M in cui phi è vera. Insieme vuoto se non ce n'è nessuno.
     */
    public static SequencedSet<String> extension(Model model, Formula formula) {
        Objects.requireNonNull(model, "Il modello non può essere nullo");
        Objects.requireNonNull(formula, "La formula non può essere nulla");

        return model.stati().stream()
                .filter(q -> check(model, q, formula))
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        Collections::unmodifiableSequencedSet));

    }

    /** Una formula è soddisfacibile se è vera in ALMENO uno stato del modello. */
    public static boolean isSatisfiable(Model model, Formula formula) {
        Objects.requireNonNull(model, "Il modello non può essere nullo");
        Objects.requireNonNull(formula, "La formula non può essere nulla");

        for (String w : model.stati()) {
            if (check(model, w, formula)) {
                return true; // Trovato un testimone, non serve scorrere gli altri
            }
        }
        return false;

        // return model.stati().stream()
        // .anyMatch(stato -> check(model, stato, formula));
    }

    /** Una formula è valida se è vera in TUTTI gli stati del modello (M |= phi). */
    public static boolean isValid(Model model, Formula formula) {
        Objects.requireNonNull(model, "Il modello non può essere nullo");
        Objects.requireNonNull(formula, "La formula non può essere nulla");

        for (String w : model.stati()) {
            if (!check(model, w, formula)) {
                return false; // Trovato un controesempio, fallisce subito
            }
        }
        return true;

        // return model.stati().stream()
        // .allMatch(stato -> check(model, stato, phi));
    }

    //

    /** Trasforma ricorsivamente una formula nella sua Negation Normal Form (NNF) */
    public static Formula toNNF(Formula formula) {
        return switch (formula) {
            case Atom a -> a;
            case And(var l, var r) -> new And(toNNF(l), toNNF(r));
            case Or(var l, var r) -> new Or(toNNF(l), toNNF(r));

            case Implies(var l, var r) -> new Or(toNNF(new Not(l)), toNNF(r));
            case Knows(var a, var phi) -> new Knows(a, toNNF(phi));
            case Possible(var a, var phi) -> new Possible(a, toNNF(phi));

            // Gestione negazioni annidate
            case Not(var inner) -> switch (inner) {
                case Atom a -> new Not(a);
                case Not(var phi) -> toNNF(phi);

                // De Morgan: ¬(l ∧ r) => ¬l ∨ ¬r
                // __________ ¬(l ∨ r) => ¬l ∧ ¬r
                case And(var l, var r) -> new Or(toNNF(new Not(l)), toNNF(new Not(r)));
                case Or(var l, var r) -> new And(toNNF(new Not(l)), toNNF(new Not(r)));

                case Implies(var l, var r) -> new And(toNNF(l), toNNF(new Not(r))); // ¬(l -> r) => l ∧ ¬r
                case Knows(var a, var phi) -> new Possible(a, toNNF(new Not(phi))); // ¬K_a phi => <K_a> ¬phi
                case Possible(var a, var phi) -> new Knows(a, toNNF(new Not(phi))); // ¬<K_a> phi => K_a ¬phi
            };
        };
    }
}