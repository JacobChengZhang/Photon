/**
 * JacobZhang.org Copyright (c) 1994-2018 All Rights Reserved.
 */
package org.jacobzhang.photon.model;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.jacobzhang.photon.app.Screen;
import org.jacobzhang.photon.util.CommonUtil;

/**
 * @author JacobChengZhang
 * @version $Id: PhotonImpl.java, v 0.1 2018年12月16日 5:53 AM JacobChengZhang Exp $
 */
public class PhotonImpl implements Photon {
    private Screen screen;
    private Scene  scene;
    private Stage  stage;

    public PhotonImpl(Screen screen) {
        this.screen = screen;
    }

    @Override
    public void init() {
        assert(screen != null);

        scene = new Scene(screen.createScene());

        screen.setScene(scene);
        screen.updateTitle();
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
                    screen.openDirectory(false);
                    break;
                }
                case F: {
                    screen.openDirectory(true);
                    break;
                }
                case H: {
                    screen.toggleHelp();
                    break;
                }
                case UP: {
                    CommonUtil.revealImageInFinder(screen.getCurrentFile());
                    break;
                }
                case LEFT: {
                    screen.prev();
                    break;
                }
                case RIGHT: {
                    screen.next();
                    break;
                }
                case SLASH: {
                    screen.toggleRandom();
                    screen.updateTitle();
                    break;
                }
                case P: {
                    screen.toggleSlideMode();
                    break;
                }
                case ESCAPE: {
                    Platform.exit();
                }
                default: {
                    break;
                }
            }
        });
    }
}
