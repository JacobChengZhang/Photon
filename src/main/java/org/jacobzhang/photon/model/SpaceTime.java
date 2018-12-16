/**
 * JacobZhang.org Copyright (c) 1994-2018 All Rights Reserved.
 */
package org.jacobzhang.photon.model;

import java.io.File;

/**
 * @author JacobChengZhang
 * @version $Id: SpaceTime.java, v 0.1 2018年12月16日 12:23 PM JacobChengZhang Exp $
 */
public interface SpaceTime {

    boolean getInOrder();

    void toggleRandom();

    boolean getSlidePlaying();

    void toggleSlideMode();

    void clearFilesAndHistory();

    File getCurrentFile();

    void prev();

    void next();

    boolean getDirectory(boolean byFile);

    void addHistory();
}
