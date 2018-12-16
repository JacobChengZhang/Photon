/**
 * JacobZhang.org Copyright (c) 1994-2018 All Rights Reserved.
 */
package org.jacobzhang.photon.model;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.LinkedList;

import org.jacobzhang.photon.constant.Constant;
import org.jacobzhang.photon.util.CommonUtil;

/**
 * @author JacobChengZhang
 * @version $Id: SpaceTimeImpl.java, v 0.1 2018年12月16日 12:23 PM JacobChengZhang Exp $
 */
public class SpaceTimeImpl implements SpaceTime {
    private File[]              fileList     = null;
    private int                 pos          = 0;
    private boolean             inOrder      = true;
    private LinkedList<Integer> history      = new LinkedList<>();
    private volatile boolean    slidePlaying = false;

    @Override
    public boolean isInOrder() {
        return this.inOrder;
    }

    @Override
    public void toggleOrder() {
        this.inOrder = !this.inOrder;
    }

    @Override
    public boolean isSlidePlaying() {
        return this.slidePlaying;
    }

    @Override
    public void toggleSlide() {
        if (fileList != null) {
            /**
             * Notice that, if you toggle this several times within the sleep interval and stop at the "on" state,
             * you can achieve a higher speed on toggleSlide playing.
             * So, it's not a bug. It is actually a feature.
             */
            if (slidePlaying) {
                slidePlaying = false;
            } else {
                slidePlaying = true;
            }
        }
    }

    @Override
    public void clearFileListAndHistory() {
        fileList = null;
        history = new LinkedList<>();
        pos = 0;
        slidePlaying = false;
    }

    @Override
    public void prev() {
        if (pos <= -history.size() || history.size() == 1) {
            return;
        }

        if (pos >= 0) {
            pos = -2;
        } else {
            pos--;
        }
    }

    @Override
    public void next() {
        if (fileList == null) {
            return;
        }

        if (pos < 0) {
            pos++;
            if (pos == 0) {
                pos = history.get(history.size() - 1);
                nextPos();
            }
        } else {
            nextPos();
            history.add(pos);
            if (history.size() > Constant.HISTORY_CAPABILITY) {
                history.removeFirst();
            }
        }
    }

    @Override
    public File getCurrentFile() {
        if (pos >= 0) {
            return fileList[pos];
        } else {
            return fileList[history.get(history.size() + pos)];
        }
    }

    @Override
    public boolean getDirectory(boolean byFile) {
        if (byFile) {
            FileChooser fc = new FileChooser();
            File file = fc.showOpenDialog(null);
            if (file == null) {
                return false;
            } else {
                if (fileList != null) {
                    clearFileListAndHistory();
                }

                fileList = CommonUtil.getFiles(file.getParentFile());

                int i = 0;
                for (; i < fileList.length; i++) {
                    if (fileList[i].equals(file)) {
                        pos = i;
                        break;
                    }
                }
                if (i == fileList.length) {
                    pos = 0;
                }
            }
        } else {
            DirectoryChooser dc = new DirectoryChooser();
            File file = dc.showDialog(null);
            if (file == null) {
                return false;
            } else {
                if (fileList != null) {
                    clearFileListAndHistory();
                }

                fileList = CommonUtil.getFiles(file);
            }
        }

        if (fileList.length == 0) {
            fileList = null;
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void addHistory() {
        history.add(this.pos);
    }

    private void nextPos() {
        if (inOrder) {
            pos++;
            if (pos == fileList.length) {
                pos = 0;
            }
        } else {
            pos = CommonUtil.nextRandomInt(fileList.length);
        }
    }
}
