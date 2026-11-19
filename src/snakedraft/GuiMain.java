package snakedraft;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import snakedraft.controller.GuiController;
import snakedraft.view.DraftGui;

/**
 * Starts the graphical SnakeDraft. Contains no draft logic.
 */
public class GuiMain {

    /**
     * Opens the setup dialog, then the draft window.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info
                    : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            }
            catch (Exception e) {
                // Fall back to the default look and feel.
            }
            new GuiController(new DraftGui()).start();
        });
    }
}
