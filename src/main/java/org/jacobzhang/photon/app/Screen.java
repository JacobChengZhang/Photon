/**
 * JacobZhang.org Copyright (c) 1994-2018 All Rights Reserved.
 */
package org.jacobzhang.photon.app;

import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.File;

/**
 * @author JacobChengZhang
 * @version $Id: Screen.java, v 0.1 2018年12月16日 5:58 AM JacobChengZhang Exp $
 */
public interface Screen {
    void setScene(Scene scene);

    Parent createScene();

    void startShow();

    void updateTitle(boolean inOrder, boolean slidePlaying);

    void toggleHelp();

    void showImage(File file);

    void playSlide();

    void changeLocale();
}
