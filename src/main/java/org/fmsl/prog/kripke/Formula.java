package org.fmsl.prog.kripke;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formula logica proposizionale e modale: atomi, connettivi booleani, operatori
 * epistemici.
 * Rappresentazione come Algebraic Data Type.
 * Implementa l'Interpreter Pattern: ogni nodo dell'AST valuta autonomamente
 * la propria verità su uno stato del modello di Kripke (M, s |= phi).
 */
public sealed interface Formula {

    // --- DSL Factory Methods ---

    static Formula atom(String name) {
        return new Atom(name);
    }

    static Formula not(Formula phi) {
        return new Not(phi);
    }

    static Formula and(Formula left, Formula right) {
        return new And(left, right);
    }

    static Formula or(Formula left, Formula right) {
        return new Or(left, right);
    }

    static Formula knows(String agent, Formula phi) {
        return new Knows(agent, phi);
    }

    static Formula possible(String agent, Formula phi) {
        return new Possible(agent, phi);
    }

    static Formula implies(Formula left, Formula right) {
        return new Implies(left, right);
    }

    /*
     * Implementazione del parsing della stringa in una formula
     * Puoi utilizzare un parser esistente o implementare il tuo
     * Restituisci la formula corrispondente alla stringa
     * 
     * @param stringFormula la stringa da parsare
     * 
     * @return la formula corrispondente
     */
    static Formula parseStringToFormula(String stringFormula) {

        return FormulaParser.parse(stringFormula);
    }

    // --- Fluent Combinators ---

    default Formula not() {
        return new Not(this);
    }

    default Formula and(Formula other) {
        return new And(this, other);
    }

    default Formula or(Formula other) {
        return new Or(this, other);
    }

    default Formula implies(Formula other) {
        return new Implies(this, other);
    }

    default Formula iff(Formula other) {
        return this.implies(other).and(other.implies(this));
    }

    // --- Concrete Formula Types ---

    /* ψ */
    record Atom(String name) implements Formula {
        public Atom {
            Objects.requireNonNull(name, "Il nome della proposizione non può essere nullo");
            if (name.isBlank())
                throw new IllegalArgumentException("Il nome non può essere vuoto");
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /* ¬ψ */
    record Not(Formula phi) implements Formula {
        public Not {
            Objects.requireNonNull(phi, "La sotto-formula negata non può essere nulla");
        }

        @Override
        public String toString() {
            return (phi instanceof Atom) ? "¬" + phi : "¬(" + phi + ")";
        }
    }

    /** ψ1 ∧ ψ2 */
    record And(Formula left, Formula right) implements Formula {
        public And {
            Objects.requireNonNull(left, "La sotto-formula sinistra non può essere nulla");
            Objects.requireNonNull(right, "La sotto-formula destra non può essere nulla");
        }

        @Override
        public String toString() {
            return "(" + left + " ∧ " + right + ")";
        }
    }

    /** ψ1 ∨ ψ2 */
    record Or(Formula left, Formula right) implements Formula {
        public Or {
            Objects.requireNonNull(left, "La sotto-formula sinistra non può essere nulla");
            Objects.requireNonNull(right, "La sotto-formula destra non può essere nulla");
        }

        @Override
        public String toString() {
            return "(" + left + " v " + right + ")";
        }
    }

    /** ψ1 → ψ2 */
    record Implies(Formula a, Formula b) implements Formula {
        public Implies {
            Objects.requireNonNull(a, "La prima proposizione non può essere nulla");
            Objects.requireNonNull(b, "La seconda proposizione non può essere nulla");
        }

        @Override
        public String toString() {
            return a + " -> " + b;
        }
    }

    /**
     * K_a ψ : "l'agente a sa ψ" = ψ è vera in TUTTI i mondi che a considera
     * possibili.
     */
    record Knows(String agent, Formula phi) implements Formula {
        public Knows {
            Objects.requireNonNull(agent, "Il nome dell'agente non può essere nullo");
            Objects.requireNonNull(phi, "La sotto-formula non può essere nulla");
            if (agent.isBlank())
                throw new IllegalArgumentException("L'identificativo agente non può essere vuoto");
        }

        @Override
        public String toString() {
            return "K_" + agent + " " + phi;
        }
    }

    /**
     * <K_a> ψ : possibilità epistemica : ψ è vera in ALMENO UNO stato che a
     * considera possibile.
     */
    record Possible(String agent, Formula phi) implements Formula {
        public Possible {
            Objects.requireNonNull(agent, "Il nome dell'agente non può essere nullo");
            Objects.requireNonNull(phi, "La sotto-formula non può essere nulla");
            if (agent.isBlank())
                throw new IllegalArgumentException("L'identificativo agente non può essere vuoto");
        }

        @Override
        public String toString() {
            return "<K_" + agent + "> " + phi;
        }
    }

    // --- Zona Parser ---

    /* Parser per la sintassi delle formule da stringa in Formula */
    class FormulaParser {
        private final Queue<String> tokens;

        /**
         * Punto di ingresso. Divide la stringa in token ignorando gli spazi in
         * automatico.
         */
        public static Formula parse(String input) {
            Queue<String> tokens = new LinkedList<>();
            // Trova: -> | <K_ | > | K_ | ¬ | - | ~ | ! | & | | | ( | ) | parole/numeri
            Matcher m = Pattern.compile("->|<K_|>|K_|!|-|~|¬|&|\\||\\(|\\)|\\w+").matcher(input);
            while (m.find()) {
                tokens.add(m.group());
            }
            return new FormulaParser(tokens).parseImplies();
        }

        private FormulaParser(Queue<String> tokens) {
            this.tokens = tokens;
        }

        // --- LOGICA GRAMMATICALE (Ordine di precedenza dal più basso al più alto) ---

        private Formula parseImplies() {
            Formula left = parseOr();
            if (match("->")) {
                return left.implies(parseImplies()); // Associativo a destra
            }
            return left;
        }

        private Formula parseOr() {
            Formula left = parseAnd(); // sx
            while (match("|")) {
                left = left.or(parseAnd());
            }
            return left;
        }

        private Formula parseAnd() {
            Formula left = parseUnary();
            while (match("&")) {
                left = left.and(parseUnary());
            }
            return left;
        }

        private Formula parseUnary() {
            if (match("~") || match("!") || match("-") || match("¬"))
                return parseUnary().not();

            if (match("K_"))
                return Formula.knows(consumeID(), parseUnary());

            if (match("<K_")) {
                String agent = consumeID();
                if (!match(">"))
                    throw new IllegalArgumentException("Atteso '>' dopo l'agente");
                return Formula.possible(agent, parseUnary());
            }

            return parsePrimary();
        }

        private Formula parsePrimary() {
            if (match("(")) {
                Formula inner = parseImplies();
                if (!match(")"))
                    throw new IllegalArgumentException("Parentesi ')' mancante");
                return inner;
            }
            return Formula.atom(consumeID());
        }

        // --- FUNZIONI DI SUPPORTO (Lexer Tools) ---

        /** Consuma e restituisce il token se corrisponde a quello atteso. */
        private boolean match(String expected) {
            if (expected.equals(tokens.peek())) {
                tokens.poll();
                return true;
            }
            return false;
        }

        /** Consuma e restituisce un identificatore (nome variabile o agente). */
        private String consumeID() {
            String token = tokens.poll();
            if (token == null || !token.matches("\\w+")) {
                throw new IllegalArgumentException("Atteso identificatore (testo/numeri), trovato: " + token);
            }
            return token;
        }
    }
}