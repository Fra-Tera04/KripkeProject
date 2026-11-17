package it.unina.mc;

import java.util.Map;
import java.util.Set;

import it.unina.mc.core.ast.Formula;
import it.unina.mc.core.ast.Formula.Atl;
import it.unina.mc.core.ast.Formula.Ctl;
import it.unina.mc.core.ast.Formula.CtlStar;
import it.unina.mc.core.ast.Formula.Ltl;
import it.unina.mc.core.ast.Formula.Prop;
import it.unina.mc.core.model.CGS;
import it.unina.mc.engines.Checkers.AtlChecker;
import it.unina.mc.engines.Checkers.CtlChecker;
import it.unina.mc.engines.Checkers.CtlStarChecker;
import it.unina.mc.engines.Checkers.LtlChecker;

public class Main {

    public static void main(String[] args) {
        // 1. Creiamo un CGS semplice a 3 stati (s0, s1, s2) e 2 agenti ("1", "2")
        CGS game = new CGS();
        game.addState("s0", "init", "safe");
        game.addState("s1", "safe");
        game.addState("s2", "goal");

        // Archi con profili di mosse:
        // Da s0: se 1 e 2 collaborano (push, push) vanno in s2 (goal), altrimenti vanno
        // in s1 o restano in s0
        game.addTransition("s0", "s2", Map.of("1", "push", "2", "push"));
        game.addTransition("s0", "s1", Map.of("1", "push", "2", "wait"));
        game.addTransition("s0", "s0", Map.of("1", "wait", "2", "wait"));

        // Da s1 e s2 si torna o si resta
        game.addTransition("s1", "s0", Map.of("1", "push", "2", "push"));
        game.addTransition("s2", "s2", Map.of("1", "wait", "2", "wait"));

        // 2. Verifica diretta delle label (Set<String>)
        System.out.println("=== 1. VERIFICA LABEL NODI ===");
        System.out.println("s0 è 'safe'? " + game.getState("s0").hasLabel("safe")); // true
        System.out.println("s0 è 'goal'? " + game.getState("s0").hasLabel("goal")); // false

        // 3. Istanziamo i 4 motori
        LtlChecker ltl = new LtlChecker();
        CtlChecker ctl = new CtlChecker();
        CtlStarChecker ctlStar = new CtlStarChecker();
        AtlChecker atl = new AtlChecker();

        // Atomi base (Formula<Prop>: usabili ovunque senza cast!)
        Formula<Prop> safe = Formula.atom("safe");
        Formula<Prop> goal = Formula.atom("goal");

        // 4. Definiamo una formula per ciascuna logica (usando sia metodi static che
        // default)
        Formula<Ltl> fLtl = Formula.G(safe.or(goal)); // LTL: G(safe | goal)
        Formula<Ctl> fCtl = safe.implies(Formula.EF(goal)); // CTL: safe -> EF goal
        Formula<CtlStar> fCtlStar = Formula.E(Formula.F(Formula.G(goal))); // CTL*: E F G goal
        Formula<Atl> fAtlCoop = Formula.atlX(Set.of("1", "2"), goal); // ATL: <<1,2>> X goal
        Formula<Atl> fAtlSolo = Formula.atlX(Set.of("1"), goal); // ATL: <<1>> X goal

        // SICUREZZA A COMPILE-TIME:
        // Se decommenti la riga sotto, il compilatore dà errore perché fAtlCoop è
        // Formula<Atl>:
        // ctl.verify(game, fAtlCoop);

        // 5. Esecuzione delle verifiche
        System.out.println("\n=== 2. RISULTATI VERIFICA ===");
        System.out.println("[LTL]  " + fLtl + " -> Stati: " + ltl.verify(game, fLtl));
        System.out.println("[CTL]  " + fCtl + " -> Stati: " + ctl.verify(game, fCtl));
        System.out.println("[CTL*] " + fCtlStar + " -> Stati: " + ctlStar.verify(game, fCtlStar));
        System.out.println("[ATL]  " + fAtlCoop + " -> Stati: " + atl.verify(game, fAtlCoop)
                + " (Vale in s0? " + atl.holdsAt(game, "s0", fAtlCoop) + ")");
        System.out.println("[ATL]  " + fAtlSolo + " -> Stati: " + atl.verify(game, fAtlSolo)
                + " (Vale in s0? " + atl.holdsAt(game, "s0", fAtlSolo) + ")");
    }
}