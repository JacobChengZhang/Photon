/**
 * JacobZhang.org Copyright (c) 1994-2018 All Rights Reserved.
 */
package org.jacobzhang.photon.model;

import javafx.application.Platform;
import javafx.scene.Scene;

import java.io.File;

import org.jacobzhang.photon.app.Screen;
import org.jacobzhang.photon.util.CommonUtil;

/**
 * @author JacobChengZhang
 * @version $Id: PhotonImpl.java, v 0.1 2018年12月16日 5:53 AM JacobChengZhang Exp $
 */
public class PhotonImpl implements Photon {
    private Screen    screen;
    private Scene     scene;
    private SpaceTime spaceTime;

    public PhotonImpl(Screen screen) {
        this.screen = screen;
    }

    @Override
    public void init() {
        assert (screen != null);

        this.spaceTime = new SpaceTimeImpl();

        scene = new Scene(screen.createScene());
        screen.setScene(scene);
        updateTitle();
        setSceneListener();

        checkStatus();

        screen.startShow();
    }

    private void checkStatus() {
        if (screen == null || scene == null) {
            System.err.println("Photon init failed!");
            Platform.exit();
        }
    }

    private void setSceneListener() {
        scene.setOnKeyPressed(k -> {
            switch (k.getCode()) {
                case D: {
                    openDirectoryRecursively();
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
                    toggleOrder();
                    break;
                }
                case P: {
                    toggleSlide();
                    break;
                }
                case L: {
                    // this case is not explicit
            changeLocale();
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
        screen.updateTitle(spaceTime.isInOrder(), spaceTime.isSlidePlaying());
    }

    private void openDirectoryRecursively() {
        openDirectory(false);
    }

    private void openFile() {
        openDirectory(true);
    }

    private void toggleHelp() {
        screen.toggleHelp();
    }

    private void revealInFinder() {
        CommonUtil.revealImageInFinder(spaceTime.getCurrentFile());
    }

    private void prev() {
        spaceTime.prev();
        showImage();
    }

    @Override
    public void next() {
        spaceTime.next();
        showImage();
    }

    private void toggleOrder() {
        spaceTime.toggleOrder();
        updateTitle();
    }

    private void toggleSlide() {
        spaceTime.toggleSlide();
        if (spaceTime.isSlidePlaying() == true) {
            screen.playSlide();
        }
        updateTitle();
    }

    private void showImage() {
        File file = spaceTime.getCurrentFile();
        if (file != null) {
            screen.showImage(file);
        }
    }

    private void changeLocale() {
        screen.changeLocale();
        updateTitle();
    }

    private void exit() {
        Platform.exit();
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
        return spaceTime.isSlidePlaying();
    }
}
