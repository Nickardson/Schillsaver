package controller;

import handler.ConfigHandler;
import handler.FFMPEGHandler;
import handler.JobHandler;
import handler.StatisticsHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import misc.Job;
import model.MainScreenModel;
import view.MainScreenView;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class MainScreenController implements EventHandler {
    // todo JavaDoc
    private final Stage primaryStage;

    // todo JavaDoc
    private final MainScreenView view;
    // todo JavaDoc
    private final MainScreenModel model;

    /** The object that handles settings for encoding, decoding, compression, and a number of other features. */
    private final ConfigHandler configHandler;

    // todo JavaDoc
    private final StatisticsHandler statisticsHandler;

    /**
     * Construct a new main screen controller.
     * @param primaryStage todo JavaDoc
     * @param configHandler The object that handles settings for encoding, decoding, compression, and a number of other features.
     * @param statisticsHandler todo JavaDoc
     */
    public MainScreenController(final Stage primaryStage, final ConfigHandler configHandler, final StatisticsHandler statisticsHandler) {
        this.primaryStage = primaryStage;
        this.configHandler = configHandler;
        this.statisticsHandler = statisticsHandler;

        view = new MainScreenView(this);
        model = new MainScreenModel();
    }

    @Override
    public void handle(Event event) {
        final Object source = event.getSource();

        // The button to open the handler selection dialog.
        if(source.equals(view.getButton_createJob())) {
            final JobSetupDialogController jobSetupDialogController = new JobSetupDialogController(primaryStage, configHandler, statisticsHandler);
            jobSetupDialogController.show();


            jobSetupDialogController.setOnHiding(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    final Job job = jobSetupDialogController.getModel().getJob();
                    if(job != null) {
                        job.setId(view.getListView_jobs().getItems().size());
                        view.getListView_jobs().getItems().add(job.getFullDesignation());

                        model.getList_jobs().add(job);
                    }
                }
            });
        }


        // The button to encode the currently selected handler(s).
        if(source.equals(view.getButton_encode())) {
            if (!new File(configHandler.getFfmpegPath()).exists()) {
                showFfmpegPathErrorAndWait();
                return;
            }

            // Only allow files to be encoded if there are actually
            // files in the list of files.
            if(view.getListView_jobs().getItems().size() > 0) {
                final List<FFMPEGHandler> preparedJobs = new ArrayList<>();

                // Only prepare Encode Jobs:
                for(final Job job : model.getList_jobs()) {
                    if(job.getIsEncodeJob()) {
                        final FFMPEGHandler ffmpegHandler = new FFMPEGHandler(job, job.getFiles(), this, configHandler, statisticsHandler);
                        ffmpegHandler.setOnSucceeded(ffmpegHandler);
                        preparedJobs.add(ffmpegHandler);
                    }
                }

                // Run Jobs:
                final JobHandler handler = new JobHandler(this, preparedJobs);
                final Thread thread = new Thread(handler);
                thread.setDaemon(true);
                thread.start();
            }
        }

        // The button to decode the currently selected handler(s).
        if(source.equals(view.getButton_decode())) {
            if (!new File(configHandler.getFfmpegPath()).exists()) {
                showFfmpegPathErrorAndWait();
                return;
            }

            // Only allow files to be decoded if there are actually
            // files in the list of files.
            if(view.getListView_jobs().getItems().size() > 0) {
                final List<FFMPEGHandler> preparedJobs = new ArrayList<>();

                // Only prepare Encode Jobs:
                for(final Job job : model.getList_jobs()) {
                    if(job.getIsEncodeJob() == false) {
                        final FFMPEGHandler ffmpegHandler = new FFMPEGHandler(job, job.getFiles(), this, configHandler, statisticsHandler);
                        ffmpegHandler.setOnSucceeded(ffmpegHandler);
                        preparedJobs.add(ffmpegHandler);
                    }
                }

                // Run Jobs:
                final JobHandler handler = new JobHandler(this, preparedJobs);
                final Thread thread = new Thread(handler);
                thread.setDaemon(true);
                thread.start();
            }
        }

        // The button to remove all files that are currently selected on the scrollpane_selectedFiles.
        if(source.equals(view.getButton_deleteSelectedJobs())) {
            // If a copy of the observable list is not made, then errors can occur.
            // These errors are caused by the ObservableList updating as items are being removed.
            // This causes items that shouldn't be removed to be removed.
            final ObservableList<String> copy = FXCollections.observableArrayList(view.getListView_jobs().getSelectionModel().getSelectedItems());
            view.getListView_jobs().getItems().removeAll(copy);

            // Remove Jobs from the Model while updating
            // the IDs of all Jobs.
            final Iterator<Job> it = model.getList_jobs().iterator();

            while(it.hasNext()) {
                final Job j = it.next();

                if(view.getListView_jobs().getItems().contains(j.getFullDesignation())) {
                    int index = view.getListView_jobs().getItems().indexOf(j.getFullDesignation());
                    j.setId(index);
                    view.getListView_jobs().getItems().set(index, j.getFullDesignation());
                } else {
                    it.remove();
                }
            }

            view.getListView_jobs().getSelectionModel().clearSelection();
        }

        // The button to remove all files from the list.
        if(source.equals(view.getButton_deleteAllJobs())) {
            view.getListView_jobs().getItems().clear();

            model.getList_jobs().clear();

            view.getListView_jobs().getSelectionModel().clearSelection();
        }


        // The button to clear the output screen.
        if(source.equals(view.getButton_clearOutput())) {
            view.getTextArea_output().clear();
        }


        // The button to open the settings dialog.
        if(source.equals(view.getButton_editSettings())) {
            new SettingsDialogController(configHandler).show();
        }
    }

    private void showFfmpegPathErrorAndWait() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("FFmpeg was not found.");
        alert.setContentText("Please change the FFmpeg path in the settings menu.\nIf you have set the path, confirm that the file can be accessed.");
        alert.showAndWait();
    }

    ////////////////////////////////////////////////////////// Getters

    // todo JavaDoc
    public MainScreenView getView() {
        return view;
    }

    // todo JavaDoc
    public MainScreenModel getModel() {
        return model;
    }
}
