/*
 *  Trabalho realizado para a disciplina INE5421 (Linguagens Formais e Compiladores) do
 *  curso de Ciência da Computação da Universidade Federal de Santa Catarina.
 *  Disciplina ministrada pela profa. Jerusa Marchi.
 *
 *  Desenvolvido por:
 *  Alvaro Emílio Prüsse, Christian de Pieri e Nathália Liz de Brito.
 *
 */

package br.ufsc.formais;

import br.ufsc.formais.automata.FiniteAutomata;
import br.ufsc.formais.grammar.RegularGrammar;
import br.ufsc.formais.regex.RegularExpression;
import org.apache.commons.cli.*;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        Options options = new Options();

        options.addOption("i", "input", true, "caminho(s) do(s) arquivo(s) de entrada");
        Option option = new Option("i", "input", true, "caminho(s) do(s) arquivo(s) de entrada");
        option.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(option);
        options.addOption("o", "output", true, "caminho arquivo de saída");

        options.addOption("g2a", "gr2af", false, "converte gramática regular para um afnd");
        options.addOption("a2g", "afd2gr", false, "converte um afd para uma gramática regular");
        options.addOption("aua", "union", false, "faz a união entre dois autômatos");
        options.addOption("aia", "intersection", false, "faz a intersecção entre dois autômatos");

        options.addOption("e2a", "er2af", false, "converte uma expressão regular para um afd");

        options.addOption("h", "help", false, "mostra comandos disponíveis");

        options.addOption("d", "determinizar", false, "determiniza autômato finito");
        options.addOption("m", "minimizar", false, "minimiza autômato finito");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("g2a")) {
                doG2A(cmd);
            } else if (cmd.hasOption("a2g")) {
                doA2G(cmd);
            } else if (cmd.hasOption("aua")) {
                doAUA(cmd);
            } else if (cmd.hasOption("aia")) {
                doAIA(cmd);
            } else if (cmd.hasOption("e2a")) {
                doE2A(cmd);
            } else if (cmd.hasOption("d")) {
                doD(cmd);
            } else if (cmd.hasOption("m")) {
                doM(cmd);
            } else {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar trabalho.jar", options);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private static boolean doG2A(CommandLine cmd) {
        if (cmd.hasOption("i") && cmd.hasOption("o")) {
            var input = cmd.getOptionValue("i");
            var output = cmd.getOptionValue("o");
            try {
                var grammar = RegularGrammar.Load(input);
                var automata = grammar.toFiniteAutomata();
                automata.Save(output);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static boolean doA2G(CommandLine cmd) {
        if (cmd.hasOption("i") && cmd.hasOption("o")) {
            var input = cmd.getOptionValue("i");
            var output = cmd.getOptionValue("o");
            try {
                var automata = FiniteAutomata.Load(input);
                var grammar = automata.toRegularGrammar();
                grammar.Save(output);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static boolean doAUA(CommandLine cmd) {
        if (cmd.hasOption("i") && cmd.hasOption("o")) {
            var input = cmd.getOptionValues("i");
            var output = cmd.getOptionValue("o");
            try {
                var automata = FiniteAutomata.Load(input[0]);
                automata.union(FiniteAutomata.Load(input[1]));
                automata.Save(output);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static boolean doAIA(CommandLine cmd) {
        if (cmd.hasOption("i") && cmd.hasOption("o")) {
            var input = cmd.getOptionValues("i");
            var output = cmd.getOptionValue("o");
            try {
                var automata = FiniteAutomata.Load(input[0]);
                automata.intersection(FiniteAutomata.Load(input[1]));
                automata.Save(output);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static boolean doE2A(CommandLine cmd) {
        if (cmd.hasOption("i") && cmd.hasOption("o")) {
            var input = cmd.getOptionValues("i");
            var output = cmd.getOptionValue("o");
            try {
                var regex = RegularExpression.Load(input[0]);
                var automata = regex.toFiniteAutomata();
                automata.Save(output);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static boolean doD(CommandLine cmd) {
        if (cmd.hasOption("i") && cmd.hasOption("o")) {
            var input = cmd.getOptionValue("i");
            var output = cmd.getOptionValue("o");
            try {
                var automata = FiniteAutomata.Load(input);
                automata.determinize();
                automata.Save(output);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static boolean doM(CommandLine cmd) {
        if (cmd.hasOption("i") && cmd.hasOption("o")) {
            var input = cmd.getOptionValue("i");
            var output = cmd.getOptionValue("o");
            try {
                var automata = FiniteAutomata.Load(input);
                automata.minimize();
                automata.Save(output);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
