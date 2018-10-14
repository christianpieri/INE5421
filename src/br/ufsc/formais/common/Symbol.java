/*
 *  Trabalho realizado para a disciplina INE5421 (Linguagens Formais e Compiladores) do
 *  curso de Ciência da Computação da Universidade Federal de Santa Catarina.
 *  Disciplina ministrada pela profa. Jerusa Marchi.
 *
 *  Desenvolvido por:
 *  Alvaro Emílio Prüsse, Christian de Pieri e Nathália Liz de Brito.
 *
 */

package br.ufsc.formais.common;

public class Symbol {
    private Character character;

    public static final Symbol EPSILON = new Symbol('&');


    public Symbol(Character symbol) {
        this.character = symbol;
    }

    @Override
    public String toString() {
        return "" + character;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj instanceof Symbol) {
            Symbol symbol = (Symbol) obj;
            return this.character == symbol.character;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 + this.character.hashCode();
    }

    public boolean isLowerCase() {
        return Character.isLowerCase(this.character);
    }
}



