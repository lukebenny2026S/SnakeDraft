package snakedraft.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * Modal dialog that collects the number of teams, rounds, and team names
 * (the GUI version of DraftController.setUp's prompts).
 */
public class SetupDialog extends JDialog {

    /** What the user chose. */
    public static final class Result {
        public final List<String> teamNames;
        public final int rounds;

        Result(List<String> teamNames, int rounds) {
            this.teamNames = teamNames;
            this.rounds = rounds;
        }
    }

    private static final int MAX_TEAMS = 12;

    private final int poolSize;
    private final JSpinner teamSpinner;
    private final JSpinner roundSpinner;
    private final JPanel namesPanel = new JPanel(new GridBagLayout());
    private final List<JTextField> nameFields = new ArrayList<>();
    private final JLabel error = new JLabel(" ");
    private Result result;
    private boolean firstBuild = true;

    private SetupDialog(Component parent, int poolSize) {
        super((java.awt.Frame)null, "New Snake Draft", true);
        this.poolSize = poolSize;
        int maxTeams = Math.min(MAX_TEAMS, poolSize);
        teamSpinner = new JSpinner(new SpinnerNumberModel(Math.min(4, maxTeams),
            2, maxTeams, 1));
        roundSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 14, 20));

        JLabel title = new JLabel("Set up your draft");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new BorderLayout(0, 10));
        JPanel counts = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        counts.add(new JLabel("Teams (2-" + maxTeams + "):"));
        counts.add(teamSpinner);
        counts.add(new JLabel("   Rounds:"));
        counts.add(roundSpinner);
        form.add(counts, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(namesPanel);
        scroll.setBorder(BorderFactory.createTitledBorder("Team names (pick order in round 1)"));
        scroll.setPreferredSize(new Dimension(380, 260));
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        form.add(scroll, BorderLayout.CENTER);
        root.add(form, BorderLayout.CENTER);

        error.setForeground(new Color(0xc0392b));
        JButton start = new JButton("Start Draft");
        JButton cancel = new JButton("Cancel");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(cancel);
        buttons.add(start);
        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.add(error, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(start);

        teamSpinner.addChangeListener(e -> rebuildNames());
        start.addActionListener(e -> accept());
        cancel.addActionListener(e -> dispose());

        rebuildNames();
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Shows the dialog and blocks until it closes.
     *
     * @param parent   component to center on (may be null)
     * @param poolSize number of draftable players (limits rounds)
     * @return the choices, or null if cancelled
     */
    public static Result show(Component parent, int poolSize) {
        SetupDialog d = new SetupDialog(parent, poolSize);
        d.setVisible(true);
        return d.result;
    }

    private int teamCount() {
        return (Integer)teamSpinner.getValue();
    }

    /** Rebuilds the name fields (keeping typed text) and the max rounds. */
    private void rebuildNames() {
        int n = teamCount();
        while (nameFields.size() < n) {
            nameFields.add(new JTextField("Team " + (nameFields.size() + 1), 18));
        }
        while (nameFields.size() > n) {
            nameFields.remove(nameFields.size() - 1);
        }
        namesPanel.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 6, 3, 6);
        for (int i = 0; i < n; i++) {
            c.gridy = i;
            c.gridx = 0;
            c.weightx = 0;
            c.anchor = GridBagConstraints.WEST;
            namesPanel.add(new JLabel((i + 1) + "."), c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            namesPanel.add(nameFields.get(i), c);
        }
        namesPanel.revalidate();
        namesPanel.repaint();

        int maxRounds = Math.max(1, poolSize / n);
        // First build defaults to 5 rounds; later rebuilds keep the user's
        // choice, clamped to what the player pool can supply.
        int wanted = firstBuild ? 5 : (Integer)roundSpinner.getValue();
        firstBuild = false;
        roundSpinner.setModel(new SpinnerNumberModel(
            Math.min(wanted, maxRounds), 1, maxRounds, 1));
    }

    private void accept() {
        List<String> names = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < nameFields.size(); i++) {
            String name = nameFields.get(i).getText().trim();
            if (name.isEmpty()) {
                error.setText("Team " + (i + 1) + " needs a name.");
                return;
            }
            // Same rule the model uses: case and spaces are ignored.
            if (!seen.add(name.toLowerCase().replaceAll("\\s+", ""))) {
                error.setText("Two teams are named \"" + name + "\".");
                return;
            }
            names.add(name);
        }
        result = new Result(names, (Integer)roundSpinner.getValue());
        dispose();
    }
}
