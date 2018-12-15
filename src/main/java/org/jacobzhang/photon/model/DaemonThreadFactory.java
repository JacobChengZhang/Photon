/**
 * JacobZhang.org Copyright (c) 1994-2018 All Rights Reserved.
 */
package org.jacobzhang.photon.model;

import java.util.concurrent.ThreadFactory;

/**
 * @author JacobChengZhang
 * @version $Id: DaemonThreadFactory.java, v 0.1 2018年12月16日 5:37 AM JacobChengZhang Exp $
 */
public class DaemonThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    }
}
