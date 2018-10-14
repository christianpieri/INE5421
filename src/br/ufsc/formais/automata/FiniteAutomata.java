/*
 *  Trabalho realizado para a disciplina INE5421 (Linguagens Formais e Compiladores) do
 *  curso de Ciência da Computação da Universidade Federal de Santa Catarina.
 *  Disciplina ministrada pela profa. Jerusa Marchi.
 *
 *  Desenvolvido por:
 *  Alvaro Emílio Prüsse, Christian de Pieri e Nathália Liz de Brito.
 *
 */

package br.ufsc.formais.automata;

import br.ufsc.formais.common.Symbol;
import br.ufsc.formais.grammar.ProductionRule;
import br.ufsc.formais.grammar.RegularGrammar;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class FiniteAutomata {
    private State initialState;
    private State errorState;
    private Set<State> finalStates;
    private Map<State, Map<Symbol, Set<State>>> transitionTable;

    public FiniteAutomata(State initialState, Set<State> finalStates, Map<State, Map<Symbol, Set<State>>> transitionTable) {
        this.initialState = initialState;
        this.finalStates = finalStates;
        this.transitionTable = transitionTable;
    }

    public FiniteAutomata() {
    }

    private FiniteAutomata copy() {
        var ret = new FiniteAutomata();
        ret.finalStates = new HashSet<>(this.finalStates);
        ret.initialState = this.initialState.copy();
        ret.transitionTable = new TreeMap<>(this.transitionTable);
        ret.errorState = this.errorState != null ? this.errorState.copy() : null;
        return ret;
    }

    private boolean isDeterministic() {
        for (var entry : this.transitionTable.entrySet()) {
            var transition = entry.getValue();
            if (transition == null || transition.isEmpty()) {
                continue;
            }

            if (transition.containsKey(Symbol.EPSILON)) {
                return false;
            }

            for (var nextStates : transition.values()) {
                if (nextStates.size() > 1) {
                    return false;
                }
            }
        }

        return true;
    }

    private Set<Symbol> getAlphabet() {
        var symbols = new LinkedHashSet<Symbol>();
        for (var entry : this.transitionTable.entrySet()) {
            var transition = entry.getValue();
            if (transition == null || transition.isEmpty()) {
                continue;
            }
            symbols.addAll(transition.keySet());
        }

        return symbols;
    }

    private Set<Symbol> getAlphabetWithoutEpsilon() {
        var alphabet = this.getAlphabet();
        alphabet.remove(Symbol.EPSILON);
        return alphabet;
    }

    public void determinize() {
        if (this.isDeterministic()) {
            return;
        }
        var closures = this.getEpsilonClosures(this.transitionTable.keySet());
        var states = new HashMap<Set<State>, State>();
        var newTransitionTable = new TreeMap<State, Map<Symbol, Set<State>>>();
        var symbols = this.getAlphabetWithoutEpsilon();

        this.determinizeState(closures.get(this.initialState), closures, symbols, newTransitionTable, states);

        var newInitialState = states.get(closures.get(this.initialState));
        var newFinalStates = new HashSet<State>();

        for (var entry : states.entrySet()) {
            var difSet = new HashSet<>(entry.getKey());
            if (difSet.removeAll(this.finalStates)) {
                newFinalStates.add(entry.getValue());
            }
        }
        this.transitionTable = newTransitionTable;
        this.initialState = newInitialState;
        this.finalStates = newFinalStates;
    }

    private void determinizeState(Set<State> state, Map<State, Set<State>> closures, Set<Symbol> symbols, Map<State, Map<Symbol, Set<State>>> newTransitionTable,
                                  Map<Set<State>, State> states) {
        var size = states.size();
        states.put(state, new State(size, "q" + size));

        var nextTransition = new HashMap<Symbol, Set<State>>();
        for (var symbol : symbols) {
            SortedSet<State> nextComposedState = new TreeSet<>();
            for (var st : state) {
                var transition = this.transitionTable.get(st);
                if (transition.containsKey(symbol)) {
                    for (var nextState : transition.get(symbol)) {
                        nextComposedState.addAll(closures.get(nextState));
                    }
                }
            }

            if (!states.containsKey(nextComposedState) && !nextComposedState.isEmpty()) {
                this.determinizeState(nextComposedState, closures, symbols, newTransitionTable, states);
            }
            var singleNext = new HashSet<State>();
            if (!nextComposedState.isEmpty()) {
                singleNext.add(states.get(nextComposedState));
            }
            nextTransition.put(symbol, singleNext);
        }
        newTransitionTable.put(states.get(state), nextTransition);
    }

    private Map<State, Set<State>> getEpsilonClosures(Set<State> states) {
        var closures = new TreeMap<State, Set<State>>();
        Set<State> closure = null;
        for (var state : states) {
            closure = new TreeSet<>();
            this.getEpsilonClosure(state, closure);
            closures.put(state, closure);
        }
        return closures;
    }

    /*
     * Combina os automatos (permanecem isolados)
     */
    private void combine(FiniteAutomata other) {
        this.applyIdOffset(this.getNextId() + other.getNextId() - 2);
        this.transitionTable.putAll(other.transitionTable);
        var symbols = this.getAlphabet();
        this.transitionTable.entrySet().forEach(stateMapEntry -> {
            symbols.forEach(symbol -> {
                if (!stateMapEntry.getValue().containsKey(symbol)) {
                    stateMapEntry.getValue().put(symbol, new TreeSet<State>());
                }
            });
        });


        this.finalStates.addAll(other.finalStates);
    }

    public void minimize() {
        this.determinize();
        this.removeBadStates();
        var finalStates = new HashSet<State>(this.finalStates);
        var nonFinalStates = new HashSet<State>(this.transitionTable.keySet());
        nonFinalStates.removeAll(finalStates);
        var equivalentClasses = new HashSet<Set<State>>();
        equivalentClasses.add(nonFinalStates);
        equivalentClasses.add(finalStates);
        this.complete();
        this.generateEquivalentClasses(this.getAlphabet(), equivalentClasses);
        this.buildMinimized(this.getAlphabet(), equivalentClasses);
    }

    private void buildMinimized(Set<Symbol> symbols, Set<Set<State>> classes) {
        var newStates = new HashMap<Set<State>, State>();
        var newFinalStates = new HashSet<State>();
        var newTransitionTable = new HashMap<State, Map<Symbol, Set<State>>>();
        State newInitialState = null;
        int id = 0;
        for (var clazz : classes) {
            newStates.put(clazz, new State(id, "q" + id++));
            if (newInitialState == null && clazz.contains(this.initialState)) {
                newInitialState = newStates.get(clazz);
            }
            if (this.finalStates.contains(clazz.iterator().next())) {
                newFinalStates.add(newStates.get(clazz));
            }
        }
        for (var clazz : classes) {
            var state = clazz.iterator().next();//qualquer 1 da classe
            var transitions = new LinkedHashMap<Symbol, Set<State>>();
            for (var symbol : symbols) {
                var nextState = this.transitionTable.get(state).get(symbol).iterator().next();

                var nextClass = newStates.keySet().stream().filter(clz -> clz.contains(nextState)).findFirst();
                if(nextClass.isPresent()) {
                    transitions.put(symbol, new HashSet<>(Arrays.asList(newStates.get(nextClass.get()))));
                }
            }
            newTransitionTable.put(newStates.get(clazz), transitions);
        }
        this.finalStates = newFinalStates;
        this.transitionTable = newTransitionTable;
        this.initialState = newInitialState;

    }

    private void generateEquivalentClasses(Set<Symbol> symbols, Set<Set<State>> classes) {
        var nextClasses = new HashSet<Set<State>>();
        for (var clazz : classes) {
            if (clazz.size() == 1) {
                nextClasses.add(clazz);
                continue;
            }
            HashSet<State> toVisit = new HashSet<>(clazz);
            while (!toVisit.isEmpty()) {
                var state = toVisit.iterator().next();
                var nextClass = new HashSet<State>();
                for (var otherState : toVisit) {
                    if (hasSameEquivalenceClass(state, otherState, symbols, classes)) {
                        nextClass.add(otherState);
                    }
                }
                nextClasses.add(nextClass);
                toVisit.removeAll(nextClass);
            }
        }
        if (nextClasses.size() > classes.size()) {
            classes.clear();
            classes.addAll(nextClasses);
            generateEquivalentClasses(symbols, classes);
        }
    }

    private boolean hasSameEquivalenceClass(State a, State b, Set<Symbol> symbols, Set<Set<State>> classes) {
        if (a.equals(b)) {
            return true;
        }
        var transitionTableA = this.transitionTable.get(a);
        var transitionTableB = this.transitionTable.get(b);
        HashSet<State> nextStates = new HashSet<State>();
        for (var symbol : symbols) {
            nextStates.clear();
            if (transitionTableA.containsKey(symbol)) {
                nextStates.addAll(transitionTableA.get(symbol));
            }
            if (transitionTableA.containsKey(symbol)) {
                nextStates.addAll(transitionTableB.get(symbol));
            }
            if (!classes.stream().anyMatch(clazz -> clazz.containsAll(nextStates))) {
                return false;
            }
        }
        return true;
    }

    /*
     * Remove estados inalcansáveis a partir do estado inicial e de trás pra frente a partir dos estados finais
     */
    private void removeBadStates() {
        var closure = new HashSet<State>();
        this.getClosure(this.initialState, closure);
        var inverseClosure = new HashSet<State>();
        this.finalStates.forEach(state -> this.getInverseClosure(state, inverseClosure));
        closure.retainAll(inverseClosure); //closure = goodStates
        var badStates = new HashSet<>(this.transitionTable.keySet());
        badStates.removeAll(closure);
        this.removeStates(badStates);
    }

    private void getInverseClosure(State state, Set<State> closure) {
        closure.add(state);
        this.transitionTable.entrySet().forEach(stateMapEntry -> stateMapEntry.getValue().entrySet().forEach(symbolSetEntry ->
        {
            if (!closure.contains(stateMapEntry.getKey()) && symbolSetEntry.getValue().contains(state)) {
                this.getInverseClosure(stateMapEntry.getKey(), closure);
            }
        }));
    }

    private void removeStates(Set<State> states) {
        this.finalStates.removeAll(states);
        states.forEach(state -> this.transitionTable.remove(state));
        this.transitionTable.entrySet().forEach(
                stateEntry -> stateEntry.getValue().entrySet().forEach(
                        symbolEntry -> symbolEntry.getValue().removeAll(states)));
    }

    public void union(FiniteAutomata other) {
        this.combine(other);
        this.joinInitialStatesWithEpsilon(other);
        this.determinize();
    }

    private void joinInitialStatesWithEpsilon(FiniteAutomata other) {
        var id = this.getNextId();
        var state = new State(id, "q" + id);
        var transitions = new HashMap<Symbol, Set<State>>();
        var nextStates = new TreeSet<State>();
        nextStates.add(this.initialState);
        nextStates.add(other.initialState);
        transitions.put(Symbol.EPSILON, nextStates);
        this.transitionTable.put(state, transitions);
        this.initialState = state;
    }

    private void complete() {
        var symbols = this.getAlphabetWithoutEpsilon();
        this.setupErrorState();
        this.transitionTable.entrySet().forEach(entry ->
                symbols.forEach(symbol -> {
                            if (!entry.getValue().containsKey(symbol)) {
                                entry.getValue().put(symbol, new HashSet<State>(Arrays.asList(this.errorState)));
                            } else if (entry.getValue().get(symbol).isEmpty()) {
                                entry.getValue().get(symbol).add(this.errorState);
                            }
                        }
                ));

    }

    private void setupErrorState() {
        if (this.errorState != null) {
            return;
        }
        var id = this.getNextId();
        this.errorState = new State(id, "qErro");
        var transitions = new HashMap<Symbol, Set<State>>();
        var symbols = getAlphabetWithoutEpsilon();
        symbols.forEach(s -> transitions.put(s, new HashSet<State>(Arrays.asList(this.errorState))));
        this.transitionTable.put(this.errorState, transitions);
    }

    public void complement() {
        this.complete();
        var newFinalStates = new HashSet<State>();
        for (var state : this.transitionTable.keySet()) {
            if (!this.finalStates.contains(state)) {
                newFinalStates.add(state);
            }
        }
        this.finalStates = newFinalStates;
    }

    public void intersection(FiniteAutomata other) {
        this.complement();
        var otherComplement = other.copy();
        otherComplement.complement();
        this.union(otherComplement);
        this.complement();
    }

    private void applyIdOffset(int id) {
        for (var state : this.transitionTable.keySet()) {
            state.setId(state.getId() + id);
            state.setLabel("q" + state.getId());
        }
    }

    private int getNextId() {
        int highest = -1;
        for (var state : this.transitionTable.keySet()) {
            if (state.getId() > highest) {
                highest = state.getId();
            }
        }

        return highest + 1;
    }

    private void getEpsilonClosure(State state, Set<State> closure) {
        closure.add(state);
        if (this.transitionTable.containsKey(state)) {
            var transition = this.transitionTable.get(state);
            if (transition.containsKey(Symbol.EPSILON)) {
                var nextStates = transition.get(Symbol.EPSILON);
                for (var nextState : nextStates) {
                    if (!closure.contains(nextState)) {
                        this.getEpsilonClosure(nextState, closure);
                    }
                }
            }
        }
    }

    private void getClosure(State state, Set<State> closure) {
        closure.add(state);
        if (this.transitionTable.containsKey(state)) {
            var transition = this.transitionTable.get(state);
            transition.entrySet().forEach(
                    tr -> tr.getValue().forEach(nextState ->
                    {
                        if (!closure.contains(nextState)) {
                            this.getClosure(nextState, closure);
                        }
                    }));
        }
    }

    public static FiniteAutomata Load(String path) throws IOException {
        var symbols = new LinkedHashSet<Symbol>();
        var statesMap = new TreeMap<String, State>();
        var finalStates = new HashSet<State>();
        var transitions = new TreeMap<State, Map<Symbol, Set<State>>>();
        var ret = new FiniteAutomata();
        var initialState = new HashSet<State>(); // workaround

        List<String> lines = Files.readAllLines(Paths.get(path),
                Charset.defaultCharset());

        parseSymbols(lines.remove(0), symbols);
        parseStates(lines, finalStates, initialState, statesMap);
        parseTransitions(lines, statesMap, symbols, transitions);

        ret.finalStates = finalStates;
        ret.transitionTable = transitions;
        ret.initialState = initialState.iterator().next();


        return ret;

    }

    public void Save(String path) throws IOException {
        var fw = new FileWriter(path);
        fw.write(this.toString());
        fw.flush();
    }

    private static void parseSymbols(String line, Set<Symbol> symbols) {
        var parts = line.replaceAll(" ", "").trim().split("\t");
        for (var part : parts) {
            var trimPart = part.trim();
            if (!trimPart.isEmpty()) {
                symbols.add(new Symbol(trimPart.charAt(0)));
            }
        }
    }

    private static State parseState(String part, Boolean[] isInitialIsFinal, Map<String, State> states) {
        String cleanLine = part.trim();
        isInitialIsFinal[0] = cleanLine.startsWith(">");
        isInitialIsFinal[1] = cleanLine.startsWith("*");

        if (isInitialIsFinal[0] || isInitialIsFinal[1]) {
            cleanLine = cleanLine.replaceAll("\\*", "").replaceAll("\\>", "");

        }

        if (!states.containsKey(cleanLine)) {
            states.put(cleanLine, new State(states.size(), cleanLine));
        }

        return states.get(cleanLine);
    }

    private static Set<State> parseNextState(String str, Map<String, State> states) {
        String cleanLine = str.trim();
        var nextStates = new TreeSet<State>();
        if (cleanLine.isEmpty() || "-".equals(cleanLine)) {
            return nextStates;
        }
        boolean isSet = cleanLine.startsWith("{") && cleanLine.endsWith("}");
        if (isSet) {
            cleanLine = cleanLine.replace("{", "").replace("}", "");
            if (!cleanLine.contains(",")) {

                nextStates.add(states.get(cleanLine));
            } else {
                var parts = cleanLine.split(",");
                for (var part : parts) {
                    if (!part.equals("{") && !part.equals("}") && !part.isEmpty()) {
                        nextStates.add(states.get(part));
                    }
                }
            }
        } else {
            nextStates.add(states.get(cleanLine));
        }

        return nextStates;
    }

    private static void parseTransitions
            (List<String> lines, Map<String, State> states, Set<Symbol> symbols, Map<State, Map<Symbol, Set<State>>> transitions) {
        for (var row : lines) {
            var columns = row.replaceAll(" ", "").trim().split("\t");
            var transition = new LinkedHashMap<Symbol, Set<State>>();
            var isInitialIsFinal = new Boolean[2];
            var state = parseState(columns[0], isInitialIsFinal, states);
            int offset = 1;
            for (var symbol : symbols) {
                var nextStates = parseNextState(columns[offset++], states);
                if (nextStates.size() > 0) {
                    transition.put(symbol, nextStates);
                }

            }
            transitions.put(state, transition);
        }

    }

    private static void parseStates
            (List<String> lines, Set<State> finalStates, Set<State> initialState, Map<String, State> states) {
        for (var line : lines) {
            var cleanLine = line.replaceAll(" ", "").trim().split("\t")[0];

            var isInitialIsFinal = new Boolean[2];
            var state = parseState(cleanLine, isInitialIsFinal, states);

            if (isInitialIsFinal[1]) {
                finalStates.add(state);
            }
            if (isInitialIsFinal[0] && initialState.isEmpty()) {
                initialState.add(state);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        var symbols = this.getAlphabet();
        this.fillHeader(sb, symbols);
        this.fillBody(sb, symbols);
        return sb.toString();
    }

    private void fillBody(StringBuilder sb, Set<Symbol> symbols) {
        var states = this.transitionTable.keySet();
        for (var state : states) {
            if (this.isInitialState(state)) {
                sb.append(">");
            }
            if (this.isFinalState(state)) {
                sb.append("*");
            }
            sb.append(state.getLabel());

            var transitions = this.transitionTable.get(state);
            for (var symbol : symbols) {
                sb.append("\t");
                if (transitions.containsKey(symbol)) {
                    sb.append(this.getNextStateSetString(transitions.get(symbol)));
                } else {
                    sb.append("-");
                }
            }
            sb.append("\n");

        }

    }

    public boolean acceptWord(String str) {
        if (!this.isDeterministic()) {

        }
        State currentState = this.initialState;
        try {
            for (int i = 0; i < str.length(); i++) {
                var transition = this.transitionTable.get(currentState);
                var symbol = new Symbol(str.charAt(i));
                currentState = transition.get(symbol).iterator().next();
            }
        } catch (Exception ex) {
            return false;
        }
        return this.isFinalState(currentState);
    }

    private String getNextStateSetString(Set<State> states) {
        if (states == null || states.isEmpty()) {
            return "-";
        } else if (states.size() == 1) {
            return states.iterator().next().getLabel();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (var state : states) {
            sb.append(state.getLabel() + ",");
        }
        var lastPos = sb.lastIndexOf(",");
        sb.replace(lastPos, lastPos + 1, "}");
        return sb.toString();

    }

    private boolean isFinalState(State state) {
        return this.finalStates.contains(state);
    }

    private boolean isInitialState(State state) {
        return this.initialState.equals(state);
    }

    private void fillHeader(StringBuilder sb, Set<Symbol> symbols) {
        for (var symbol : symbols) {
            sb.append("\t" + symbol);
        }
        sb.append("\n");
    }

    public RegularGrammar toRegularGrammar() {
        this.minimize();
        var productions = new LinkedHashMap<Symbol, Set<ProductionRule>>();
        var stateMap = new HashMap<State, Symbol>();
        char c = 'A';
        for (var state : transitionTable.keySet()) {
            stateMap.put(state, new Symbol(c++));
        }
        for (var entry : transitionTable.entrySet()) {
            var state = entry.getKey();
            boolean hasLoop = false;
            var productionRule = new LinkedHashSet<ProductionRule>();
            for (var entryTransition : entry.getValue().entrySet()) {
                var symbol = entryTransition.getKey();
                for (var nextState : entryTransition.getValue()) {
                    hasLoop = hasLoop || state.equals(stateMap.get(nextState));
                    productionRule.add(new ProductionRule(symbol, stateMap.get(nextState)));
                    if (this.isFinalState(nextState)) {
                        productionRule.add(new ProductionRule(symbol, null));
                    }
                }

            }
            if (initialState == state && this.isFinalState(state)) {

                productionRule.add(new ProductionRule(Symbol.EPSILON, null));
                if (hasLoop) {
                    var productionRuleCopy = new LinkedHashSet<ProductionRule>(productionRule);
                    productions.put(new Symbol('Z'), productionRuleCopy);
                    productionRule.remove(new ProductionRule(Symbol.EPSILON, null));
                }

            }
            productions.put(stateMap.get(state), productionRule);
        }
        return new RegularGrammar(productions);
    }
}
