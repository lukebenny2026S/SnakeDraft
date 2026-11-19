package snakedraft.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import snakedraft.model.Team;

/**
 * Draws the pick order of the current round as a row of team "chips" joined
 * by arrows. Finished picks are dimmed with a check mark, the team on the
 * clock is highlighted and pulses, and upcoming teams wait to the right.
 * Clicking a chip reports that team.
 */
class TurnTrack extends JPanel {

    private static final int CHIP_W = 124;
    private static final int CHIP_H = 60;
    private static final int GAP = 36;
    private static final int PAD = 24;
    private static final int CHIP_Y = 40;

    private static final Color BG = new Color(0x0f1b33);
    private static final Color UPCOMING_FILL = new Color(0x1f3158);
    private static final Color UPCOMING_LINE = new Color(0x4a639b);
    private static final Color DONE_FILL = new Color(0x15233f);
    private static final Color DONE_LINE = new Color(0x2a3b61);
    private static final Color ACTIVE = new Color(0xfca311);
    private static final Color TEXT = new Color(0xe8eefc);
    private static final Color MUTED = new Color(0x7d8db3);

    private List<Team> order = new ArrayList<>();
    private int clock;
    private float phase;
    private Consumer<Team> clickListener;
    private final Timer pulse;

    TurnTrack() {
        setBackground(BG);
        setOpaque(true);
        pulse = new Timer(45, e -> {
            phase += 0.12f;
            if (clock < order.size()) {
                repaint();
            }
        });
        pulse.start();
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int i = chipAt(e.getX(), e.getY());
                if (i >= 0 && clickListener != null) {
                    clickListener.accept(order.get(i));
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                setCursor(Cursor.getPredefinedCursor(chipAt(e.getX(), e.getY())
                    >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    void setClickListener(Consumer<Team> listener) {
        this.clickListener = listener;
    }

    void setState(List<Team> turnOrder, int clockIndex) {
        this.order = new ArrayList<>(turnOrder);
        this.clock = clockIndex;
        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> {
            if (clock < order.size()) {
                Rectangle r = chipRect(clock);
                r.grow(CHIP_W + GAP, 0);
                scrollRectToVisible(r);
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(contentWidth() + 2 * PAD, CHIP_Y + CHIP_H + 22);
    }

    private int contentWidth() {
        int n = order.size();
        return n == 0 ? 0 : n * CHIP_W + (n - 1) * GAP;
    }

    private int originX() {
        return Math.max(PAD, (getWidth() - contentWidth()) / 2);
    }

    private Rectangle chipRect(int i) {
        return new Rectangle(originX() + i * (CHIP_W + GAP), CHIP_Y, CHIP_W,
            CHIP_H);
    }

    private int chipAt(int x, int y) {
        for (int i = 0; i < order.size(); i++) {
            if (chipRect(i).contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Arrows first so chips sit on top.
        for (int i = 0; i + 1 < order.size(); i++) {
            Rectangle a = chipRect(i);
            drawArrow(g, a.x + CHIP_W + 6, a.y + CHIP_H / 2,
                a.x + CHIP_W + GAP - 6, i >= clock ? UPCOMING_LINE : DONE_LINE);
        }
        for (int i = 0; i < order.size(); i++) {
            drawChip(g, i);
        }
        g.dispose();
    }

    private void drawArrow(Graphics2D g, int x1, int y, int x2, Color c) {
        g.setColor(c);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND));
        g.drawLine(x1, y, x2 - 5, y);
        Path2D head = new Path2D.Float();
        head.moveTo(x2, y);
        head.lineTo(x2 - 9, y - 6);
        head.lineTo(x2 - 9, y + 6);
        head.closePath();
        g.fill(head);
    }

    private void drawChip(Graphics2D g, int i) {
        Rectangle r = chipRect(i);
        boolean done = i < clock;
        boolean active = i == clock;
        Team team = order.get(i);

        if (active) {
            float glow = 0.5f + 0.5f * (float)Math.sin(phase);
            for (int k = 4; k >= 1; k--) {
                int alpha = (int)((12 + 26 * glow) / k);
                g.setColor(new Color(ACTIVE.getRed(), ACTIVE.getGreen(),
                    ACTIVE.getBlue(), Math.min(255, alpha)));
                g.fill(new RoundRectangle2D.Float(r.x - k * 3, r.y - k * 3,
                    r.width + k * 6, r.height + k * 6, 18 + k * 5, 18 + k * 5));
            }
        }

        RoundRectangle2D shape = new RoundRectangle2D.Float(r.x, r.y, r.width,
            r.height, 16, 16);
        g.setColor(active ? ACTIVE : done ? DONE_FILL : UPCOMING_FILL);
        g.fill(shape);
        g.setColor(active ? ACTIVE.brighter() : done ? DONE_LINE : UPCOMING_LINE);
        g.setStroke(new BasicStroke(active ? 2.5f : 1.5f));
        g.draw(shape);

        // Pick-in-round label
        Font small = getFont().deriveFont(Font.BOLD, 10.5f);
        g.setFont(small);
        g.setColor(active ? new Color(0x5a3a00) : MUTED);
        String label = "PICK " + (i + 1);
        g.drawString(label, r.x + 12, r.y + 18);

        // Team name, shrunk with an ellipsis if needed
        Font big = getFont().deriveFont(Font.BOLD, 15f);
        g.setFont(big);
        g.setColor(active ? new Color(0x1b1200) : done ? MUTED : TEXT);
        g.drawString(fit(team.getName(), g.getFontMetrics(), r.width - 24),
            r.x + 12, r.y + 43);

        if (done) {
            drawCheck(g, r.x + r.width - 22, r.y + 14);
        }
        if (active) {
            drawOnClockTag(g, r);
        }
    }

    private void drawCheck(Graphics2D g, int x, int y) {
        g.setColor(new Color(0x4cc38a));
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND));
        g.drawLine(x, y, x + 4, y + 4);
        g.drawLine(x + 4, y + 4, x + 11, y - 5);
    }

    private void drawOnClockTag(Graphics2D g, Rectangle r) {
        Font f = getFont().deriveFont(Font.BOLD, 11f);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        String text = "ON THE CLOCK";
        int w = fm.stringWidth(text) + 20;
        int x = r.x + (r.width - w) / 2;
        int y = r.y - 30;
        g.setColor(ACTIVE);
        g.fill(new RoundRectangle2D.Float(x, y, w, 20, 20, 20));
        Path2D tri = new Path2D.Float();
        tri.moveTo(r.x + r.width / 2f - 6, y + 20);
        tri.lineTo(r.x + r.width / 2f + 6, y + 20);
        tri.lineTo(r.x + r.width / 2f, y + 27);
        tri.closePath();
        g.fill(tri);
        g.setColor(new Color(0x1b1200));
        g.drawString(text, x + 10, y + 14);
    }

    private static String fit(String s, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(s) <= maxWidth) {
            return s;
        }
        String ell = "...";
        int end = s.length();
        while (end > 1 && fm.stringWidth(s.substring(0, end) + ell) > maxWidth) {
            end--;
        }
        return s.substring(0, end) + ell;
    }
}
