package org.fmsl.prog.kripke;

import static org.fmsl.prog.kripke.Formula.parseStringToFormula;

/**
 * da rimodellare per le ltl e le ctl
 * da usare un grafo giusto
 * 
 * @author francescorogo
 *         controllare main, modelchecker
 */
public class ProjectKripke {

    public static void main(String[] args) {
        // Stati possibili: W = {q0, q1, q2}
        // 1. Inizializzazione del Modello (Robots and Carriage) tramite il Pattern
        // Builder
        Model model = new Model()
                .addState("q0", "pos0")
                .addState("q1", "pos1")
                .addState("q2", "pos2")

                // Relazioni per l'Agente 1
                .relEquivalenza("1", "q0", "q2")
                .relEquivalenza("1", "q1")

                // Relazioni per l'Agente 2
                .relEquivalenza("2", "q0", "q1")
                .relEquivalenza("2", "q2");

        System.out.println(model);
        System.out.println();

        // 2. Creazione delle formule
        Formula pos1 = Formula.atom("pos1");
        Formula pos2 = Formula.atom("pos2");

        // f1: pos2 -> ¬K_1 pos2
        Formula f1 = pos2.implies(Formula.knows("1", pos2).not());

        // f2: pos2 -> K_1 ¬pos1
        Formula f2 = pos2.implies(Formula.knows("1", pos1.not()));

        // f3: pos2 -> K_2 K_1 ¬pos1
        Formula f3 = pos2.implies(Formula.knows("2", Formula.knows("1", pos1.not())));

        // Formula f4 = Implies(Atom("pos2"), Knows("1", Not(Atom("pos2"))));

        System.out.println("=== FORMULE ANALIZZATE ===");
        System.out.println("f1: " + f1);
        System.out.println("f2: " + f2);
        System.out.println("f3: " + f3);
        System.out.println();

        // 3. Local Model Checking (valutazione in uno stato specifico)
        System.out.println("=== LOCAL MODEL CHECKING (M, q2 |= phi) ===");
        System.out.println("M, q2 |= f1 ? " + ModelChecker.check(model, "q2", f1));
        System.out.println("M, q2 |= f2 ? " + ModelChecker.check(model, "q2", f2));
        System.out.println("M, q2 |= f3 ? " + ModelChecker.check(model, "q2", f3));
        System.out.println();

        // 4. Global Model Checking (ricerca di tutti gli stati validi)
        System.out.println("=== GLOBAL MODEL CHECKING (Estensione ||phi||_M) ===");
        System.out.println("Stati in cui f1 è vera: " + ModelChecker.extension(model, f1));
        System.out.println("Stati in cui f2 è vera: " + ModelChecker.extension(model, f2));
        System.out.println("Stati in cui f3 è vera: " + ModelChecker.extension(model, f3));
        System.out.println();

        // 5. Validità e Soddisfacibilità
        System.out.println("=== VALIDITÀ E SODDISFACIBILITÀ ===");
        // Una formula è valida se l'estensione corrisponde all'intero set di stati
        System.out.println("f1 è valida (M |= f1)? " + ModelChecker.isValid(model, f1));
        System.out.println("f3 è soddisfacibile (esiste uno stato valido)? " + ModelChecker.isSatisfiable(model, f3));
        System.out.println();

        // 6. Conversione in Negation Normal Form (NNF)
        System.out.println("=== NEGATION NORMAL FORM (NNF) ===");
        // Mostra come l'implicazione (costruita con not().or() nella Fluent API)
        // venga correttamente manipolata spingendo le negazioni all'interno
        System.out.println("NNF di f1: " + ModelChecker.toNNF(f1));
        System.out.println("NNF di f3: " + ModelChecker.toNNF(f3));
        System.out.println("");
        System.out.println(f2 + " : " + parseStringToFormula(f2.toString()).equals(f2));
    }
}
