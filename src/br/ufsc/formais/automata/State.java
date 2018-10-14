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

public class State implements Comparable<State> {
    private int id;
    private String label;

    @Override
    public int compareTo(State state) {
        return this.getId() - state.getId();
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public State(int id, String label) {
        this.setId(id);
        this.setLabel(label);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof State) {
            State state = (State) obj;
            return getId() == state.getId();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public State copy() {
        return new State(this.id, this.label);
    }
}

