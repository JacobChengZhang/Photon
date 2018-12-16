/**
 * JacobZhang.org Copyright (c) 1994-2018 All Rights Reserved.
 */
package org.jacobzhang.photon.model;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

import org.jacobzhang.photon.app.Screen;
import org.jacobzhang.photon.constant.Constant;
import org.jacobzhang.photon.util.CommonUtil;

/**
 * @author JacobChengZhang
 * @version $Id: PhotonImpl.java, v 0.1 2018年12月16日 5:53 AM JacobChengZhang Exp $
 */
public class PhotonImpl implements Photon {
    private Screen screen;
    private Scene  scene;
    private Stage  stage;
    private SpaceTime spaceTime;

    public PhotonImpl(Screen screen) {
        this.screen = screen;
    }

    @Override
    public void init() {
        assert(screen != null);

        this.spaceTime = new SpaceTimeImpl();
        scene = new Scene(screen.createScene());
        screen.setScene(scene);
        updateTitle();
        setSceneListener();
        stage = screen.getStage();

        initCheck();

        stage.setScene(scene);
        stage.show();
    }

    private void initCheck() {
        if (screen == null || scene == null || stage == null) {
            System.err.println("Init failed!");
            Platform.exit();
        }
    }

    private void setSceneListener() {
        scene.setOnKeyPressed(k -> {
            switch (k.getCode()) {
                case D: {
                    openDirectory();
                    break;
                }
                case F: {
                    openFile();
                    break;
                }
                case H: {
                    toggleHelp();
                    break;
                }
                case UP: {
                    revealInFinder();
                    break;
                }
                case LEFT: {
                    prev();
                    break;
                }
                case RIGHT: {
                    next();
                    break;
                }
                case SLASH: {
                    toggleRandom();
                    break;
                }
                case P: {
                    toggleSlideMode();
                    break;
                }
                case ESCAPE: {
                    exit();
                    break;
                }
                default: {
                    break;
                }
            }
        });
    }

    private void updateTitle() {
        screen.updateTitle(spaceTime.getInOrder(), spaceTime.getSlidePlaying());
    }

    private void openDirectory() {
        openDirectory(false);
    }

    private void openFile() {
        openDirectory(true);
    }

    private void toggleHelp() {
        screen.toggleHelp();
    }

    private void toggleSlideMode() {
        spaceTime.toggleSlideMode();
        if (spaceTime.getSlidePlaying() == true) {
            screen.playSlide();
        }
        updateTitle();
    }

    private void toggleRandom() {
        spaceTime.toggleRandom();
        updateTitle();
    }

    private void revealInFinder() {
        CommonUtil.revealImageInFinder(spaceTime.getCurrentFile());
    }

    @Override
    public void next() {
        spaceTime.next();
        showImage();
    }

    private void prev() {
        spaceTime.prev();
        showImage();
    }

    private void showImage() {
        File file = spaceTime.getCurrentFile();
        if (file != null) {
            screen.showImage(file);
        }
    }

    private void openDirectory(boolean byFile) {
        if (spaceTime.getDirectory(byFile)) {
            showImage();
            spaceTime.addHistory();
            toggleHelp();
        }
    }

    @Override
    public boolean isSlidePlaying() {
        return spaceTime.getSlidePlaying();
    }

    private void exit() {
        Platform.exit();
    }
}
