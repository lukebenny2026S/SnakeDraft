package snakedraft.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import snakedraft.model.Player;
import snakedraft.model.Team;

/**
 * Graphical view of the draft. Shows who is on the clock (the turn track),
 * a scrollable, filterable list of available players, and team rosters.
 * Holds no draft rules and no reference to DraftModel: the controller hands
 * it a BoardState and receives picks through a listener.
 */
public class DraftGui extends JFrame {

    private static final Color NAVY = new Color(0x0f1b33);
    private static final Color AMBER = new Color(0xfca311);
    private static final Color OK = new Color(0x1e8e5a);
    private static final Color BAD = new Color(0xc0392b);

    private static final Map<String, Color> POS_COLORS = new LinkedHashMap<>();
    static {
        POS_COLORS.put("QB", new Color(0xd64550));
        POS_COLORS.put("RB", new Color(0x2e9e6b));
        POS_COLORS.put("WR", new Color(0x3b7dd8));
        POS_COLORS.put("TE", new Color(0xe08a1e));
        POS_COLORS.put("K", new Color(0x8a5cc2));
        POS_COLORS.put("DEF", new Color(0x6b7790));
    }

    private Consumer<String> pickListener = name -> { };
    private Runnable newDraftListener = () -> { };

    // Header
    private final JLabel roundLabel = new JLabel();
    private final JLabel clockLabel = new JLabel();
    private final TurnTrack track = new TurnTrack();

    // Players
    private final PlayerTableModel playerModel = new PlayerTableModel();
    private final JTable playerTable = new JTable(playerModel);
    private final TableRowSorter<PlayerTableModel> sorter =
        new TableRowSorter<>(playerModel);
    private final JTextField search = new JTextField(14);
    private final JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    private final ButtonGroup filterGroup = new ButtonGroup();
    private final JLabel countLabel = new JLabel(" ");
    private final JButton draftButton = new JButton("Select a player to draft");
    private String positionFilter = "ALL";
    private List<String> knownPositions = new ArrayList<>();

    // Rosters
    private final JComboBox<Team> teamBox = new JComboBox<>();
    private final RosterTableModel rosterModel = new RosterTableModel();
    private final JLabel rosterTitle = new JLabel(" ");
    private boolean updatingTeamBox;

    private final JLabel status = new JLabel(" ");
    private Team onClock;
    private boolean draftOver;

    /** Creates the (hidden) main window. */
    public DraftGui() {
        super("SnakeDraft");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        setJMenuBar(buildMenu());
        setMinimumSize(new Dimension(860, 560));
        setSize(1120, 720);
        setLocationRelativeTo(null);
    }

    // ----------------------------------------------------------- listeners

    /** @param listener receives the exact player name chosen for drafting */
    public void setPickListener(Consumer<String> listener) {
        this.pickListener = listener;
    }

    /** @param listener called when the user asks for a new draft */
    public void setNewDraftListener(Runnable listener) {
        this.newDraftListener = listener;
    }

    // ------------------------------------------------------------- building

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setBackground(NAVY);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(12, 20, 0, 20));

        JLabel title = new JLabel("SNAKE DRAFT");
        title.setForeground(new Color(0x8fa3d1));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));

        roundLabel.setForeground(new Color(0xe8eefc));
        roundLabel.setFont(roundLabel.getFont().deriveFont(Font.BOLD, 14f));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);
        top.add(roundLabel, BorderLayout.EAST);
        top.setAlignmentX(Component.LEFT_ALIGNMENT);

        clockLabel.setForeground(AMBER);
        clockLabel.setFont(clockLabel.getFont().deriveFont(Font.BOLD, 30f));
        clockLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
        clockLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        track.setClickListener(team -> {
            teamBox.setSelectedItem(team);
        });
        JScrollPane trackScroll = new JScrollPane(track,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        trackScroll.setBorder(BorderFactory.createEmptyBorder());
        trackScroll.getViewport().setBackground(NAVY);
        trackScroll.getHorizontalScrollBar().setUnitIncrement(24);
        trackScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(top);
        header.add(clockLabel);
        header.add(trackScroll);
        return header;
    }

    private JSplitPane buildCenter() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildPlayersPanel(), buildRosterPanel());
        split.setResizeWeight(0.6);
        split.setDividerLocation(660);
        split.setBorder(BorderFactory.createEmptyBorder());
        return split;
    }

    private JPanel buildPlayersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 8));

        JLabel heading = new JLabel("Available players");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));

        JPanel tools = new JPanel(new BorderLayout(10, 0));
        tools.add(filterPanel, BorderLayout.CENTER);
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        searchBox.add(new JLabel("Search:"));
        searchBox.add(search);
        tools.add(searchBox, BorderLayout.EAST);

        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.add(heading, BorderLayout.NORTH);
        north.add(tools, BorderLayout.SOUTH);
        panel.add(north, BorderLayout.NORTH);

        playerTable.setRowSorter(sorter);
        playerTable.setRowHeight(30);
        playerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playerTable.setShowGrid(false);
        playerTable.setIntercellSpacing(new Dimension(0, 1));
        playerTable.getColumnModel().getColumn(1).setMaxWidth(90);
        playerTable.getColumnModel().getColumn(1).setCellRenderer(new PositionRenderer());
        playerTable.getColumnModel().getColumn(0).setCellRenderer(new NameRenderer());
        playerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDraftButton();
            }
        });
        // Enter drafts the highlighted player instead of moving down a row.
        playerTable.getInputMap(JTable.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "draft");
        playerTable.getActionMap().put("draft", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                draftSelected();
            }
        });
        JScrollPane scroll = new JScrollPane(playerTable);
        scroll.getVerticalScrollBar().setUnitIncrement(30);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(10, 0));
        south.add(countLabel, BorderLayout.WEST);
        draftButton.setEnabled(false);
        draftButton.setFont(draftButton.getFont().deriveFont(Font.BOLD, 14f));
        draftButton.setPreferredSize(new Dimension(300, 38));
        draftButton.addActionListener(e -> draftSelected());
        south.add(draftButton, BorderLayout.EAST);
        panel.add(south, BorderLayout.SOUTH);

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        return panel;
    }

    private JPanel buildRosterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 16));

        JLabel heading = new JLabel("Team rosters");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.add(heading, BorderLayout.NORTH);
        north.add(teamBox, BorderLayout.CENTER);
        panel.add(north, BorderLayout.NORTH);

        JTable rosterTable = new JTable(rosterModel);
        rosterTable.setRowHeight(30);
        rosterTable.setShowGrid(false);
        rosterTable.setIntercellSpacing(new Dimension(0, 1));
        rosterTable.setRowSelectionAllowed(false);
        rosterTable.getColumnModel().getColumn(0).setMaxWidth(50);
        rosterTable.getColumnModel().getColumn(2).setMaxWidth(90);
        rosterTable.getColumnModel().getColumn(2).setCellRenderer(new PositionRenderer());
        panel.add(new JScrollPane(rosterTable), BorderLayout.CENTER);

        rosterTitle.setForeground(Color.GRAY);
        panel.add(rosterTitle, BorderLayout.SOUTH);

        teamBox.addActionListener(e -> {
            if (!updatingTeamBox) {
                showRoster((Team)teamBox.getSelectedItem());
            }
        });
        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xcfd4de)),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        status.setText("Select a player and press Draft (or Enter).");
        bar.add(status, BorderLayout.CENTER);
        return bar;
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu draft = new JMenu("Draft");
        JMenuItem again = new JMenuItem("New Draft...");
        again.addActionListener(e -> newDraftListener.run());
        JMenuItem quit = new JMenuItem("Quit");
        quit.addActionListener(e -> dispose());
        draft.add(again);
        draft.addSeparator();
        draft.add(quit);
        bar.add(draft);
        return bar;
    }

    // --------------------------------------------------------------- updates

    /**
     * Redraws everything for a new state of the draft.
     *
     * @param s the plain values to show
     */
    public void showState(BoardState s) {
        onClock = s.onClock();
        draftOver = s.over;

        if (s.over) {
            roundLabel.setText("Draft complete  |  " + s.totalRounds
                + (s.totalRounds == 1 ? " round" : " rounds"));
            clockLabel.setText("Draft complete");
        }
        else {
            roundLabel.setText("Round " + s.round + " of " + s.totalRounds
                + "  |  Pick " + s.pick
                + (s.reversed ? "  |  reverse order" : "  |  forward order"));
            clockLabel.setText(onClock.getName() + " is on the clock");
        }
        track.setState(s.turnOrder, s.over ? s.turnOrder.size() : s.clockIndex);

        buildFilters(s.available);
        playerModel.setPlayers(s.available);
        applyFilter();
        playerTable.clearSelection();
        updateDraftButton();

        updatingTeamBox = true;
        Team keep = s.lastPicker != null ? s.lastPicker
            : (onClock != null ? onClock : (Team)teamBox.getSelectedItem());
        teamBox.removeAllItems();
        for (Team t : s.teams) {
            teamBox.addItem(t);
        }
        teamBox.setSelectedItem(keep);
        updatingTeamBox = false;
        showRoster((Team)teamBox.getSelectedItem());

        if (s.over) {
            setStatus("All picks are in. Choose a team above to review its roster.", OK);
        }
    }

    /** Shows a confirmation for an accepted pick. */
    public void showPickResult(Team team, Player player) {
        setStatus(team.getName() + " drafted " + player.getName() + " ("
            + player.getPosition() + ")", OK);
    }

    /** Shows an error without changing the draft. */
    public void showError(String message) {
        setStatus(message, BAD);
    }

    /** Pops up a blocking error (used when a draft cannot be created). */
    public void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "SnakeDraft",
            JOptionPane.ERROR_MESSAGE);
    }

    private void setStatus(String text, Color color) {
        status.setForeground(color);
        status.setText(text);
    }

    private void showRoster(Team team) {
        rosterModel.setPlayers(team == null ? new ArrayList<>() : team.getRoster());
        if (team == null) {
            rosterTitle.setText(" ");
            return;
        }
        int n = team.getRoster().size();
        rosterTitle.setText(team.getName() + ": " + n
            + (n == 1 ? " player" : " players") + " drafted"
            + (team == onClock ? "  (on the clock)" : ""));
    }

    // --------------------------------------------------------------- filters

    /** Builds the position buttons the first time (and after a new draft). */
    private void buildFilters(List<Player> available) {
        if (!knownPositions.isEmpty()) {
            return;
        }
        for (Player p : available) {
            if (!knownPositions.contains(p.getPosition())) {
                knownPositions.add(p.getPosition());
            }
        }
        knownPositions.sort((a, b) -> {
            List<String> order = new ArrayList<>(POS_COLORS.keySet());
            int ia = order.indexOf(a);
            int ib = order.indexOf(b);
            return Integer.compare(ia < 0 ? 99 : ia, ib < 0 ? 99 : ib);
        });
        filterPanel.removeAll();
        addFilterButton("ALL", true);
        for (String pos : knownPositions) {
            addFilterButton(pos, false);
        }
        filterPanel.revalidate();
        filterPanel.repaint();
    }

    private void addFilterButton(String pos, boolean selected) {
        JToggleButton b = new JToggleButton(pos, selected);
        b.setFocusable(false);
        b.addActionListener(e -> {
            positionFilter = pos;
            applyFilter();
        });
        filterGroup.add(b);
        filterPanel.add(b);
    }

    /** Called when a new draft starts so the position buttons are rebuilt. */
    public void resetFilters() {
        knownPositions = new ArrayList<>();
        positionFilter = "ALL";
        search.setText("");
        for (java.util.Enumeration<javax.swing.AbstractButton> en =
            filterGroup.getElements(); en.hasMoreElements();) {
            filterGroup.remove(en.nextElement());
        }
    }

    private void applyFilter() {
        final String text = search.getText().trim().toLowerCase();
        final String pos = positionFilter;
        sorter.setRowFilter(new RowFilter<PlayerTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends PlayerTableModel, ? extends Integer> e) {
                Player p = e.getModel().get(e.getIdentifier());
                boolean posOk = pos.equals("ALL") || p.getPosition().equals(pos);
                boolean textOk = text.isEmpty()
                    || p.getName().toLowerCase().contains(text);
                return posOk && textOk;
            }
        });
        int shown = playerTable.getRowCount();
        int total = playerModel.getRowCount();
        countLabel.setText("Showing " + shown + " of " + total);
        updateDraftButton();
    }

    // ---------------------------------------------------------------- drafting

    private Player selectedPlayer() {
        int row = playerTable.getSelectedRow();
        return row < 0 ? null
            : playerModel.get(playerTable.convertRowIndexToModel(row));
    }

    private void updateDraftButton() {
        Player p = selectedPlayer();
        if (draftOver) {
            draftButton.setText("Draft complete");
            draftButton.setEnabled(false);
        }
        else if (p == null) {
            draftButton.setText("Select a player to draft");
            draftButton.setEnabled(false);
        }
        else {
            draftButton.setText("Draft " + p.getName() + " for "
                + (onClock == null ? "?" : onClock.getName()));
            draftButton.setEnabled(true);
        }
    }

    private void draftSelected() {
        Player p = selectedPlayer();
        if (p != null && !draftOver) {
            pickListener.accept(p.getName());
        }
    }

    // ----------------------------------------------------------- table models

    private static final class PlayerTableModel extends AbstractTableModel {
        private List<Player> players = new ArrayList<>();

        void setPlayers(List<Player> list) {
            players = new ArrayList<>(list);
            fireTableDataChanged();
        }

        Player get(int i) {
            return players.get(i);
        }

        @Override public int getRowCount() { return players.size(); }
        @Override public int getColumnCount() { return 2; }
        @Override public String getColumnName(int c) { return c == 0 ? "Player" : "Pos"; }
        @Override public Object getValueAt(int r, int c) {
            return c == 0 ? players.get(r).getName() : players.get(r).getPosition();
        }
    }

    private static final class RosterTableModel extends AbstractTableModel {
        private List<Player> players = new ArrayList<>();

        void setPlayers(List<Player> list) {
            players = new ArrayList<>(list);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return players.size(); }
        @Override public int getColumnCount() { return 3; }
        @Override public String getColumnName(int c) {
            return c == 0 ? "Rd" : c == 1 ? "Player" : "Pos";
        }
        @Override public Object getValueAt(int r, int c) {
            return c == 0 ? Integer.valueOf(r + 1)
                : c == 1 ? players.get(r).getName() : players.get(r).getPosition();
        }
    }

    // --------------------------------------------------------------- renderers

    private static final class NameRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
            boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, false, row, col);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
            setFont(getFont().deriveFont(Font.PLAIN, 14f));
            return this;
        }
    }

    /** Draws a position as a small colored pill. */
    private static final class PositionRenderer extends DefaultTableCellRenderer {
        private Color pill = Color.GRAY;
        private Color rowBackground = Color.WHITE;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
            boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, false, row, col);
            rowBackground = sel ? t.getSelectionBackground() : t.getBackground();
            pill = POS_COLORS.getOrDefault(String.valueOf(v), Color.GRAY);
            setHorizontalAlignment(SwingConstants.CENTER);
            setForeground(Color.WHITE);
            setFont(getFont().deriveFont(Font.BOLD, 11f));
            setOpaque(false);
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(rowBackground);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(pill);
            int w = Math.min(getWidth() - 12, 46);
            g2.fillRoundRect((getWidth() - w) / 2, 5, w, getHeight() - 10, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
