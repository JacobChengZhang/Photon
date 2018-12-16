/**
 * JacobZhang.org Copyright (c) 1994-2018 All Rights Reserved.
 */
package org.jacobzhang.photon.model;

/**
 * @author JacobChengZhang
 * @version $Id: Description.java, v 0.1 2018年12月16日 3:15 PM JacobChengZhang Exp $
 */
public interface Description {
    void init();

    void changeLocale();

    String getText(String keyName);
}
