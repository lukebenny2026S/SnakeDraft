package snakedraft;

import snakedraft.controller.DraftController;
import snakedraft.model.DraftModel;
import snakedraft.model.PlayerPool;
import snakedraft.view.DraftView;

/**
 * Creates the view, model, and controller, connects them, and starts the
 * draft. Contains no logic.
 */
public class Main {

    /**
     * Starts SnakeDraft.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        DraftView view = new DraftView();
        DraftModel model = DraftController.setUp(view,
            PlayerPool.defaultPlayers());
        if (model == null) {
            return;
        }
        new DraftController(model, view).run();
    }
}
