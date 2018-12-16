/**
 * JacobZhang.org Copyright (c) 1994-2018 All Rights Reserved.
 */
package org.jacobzhang.photon.model;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author JacobChengZhang
 * @version $Id: DescriptionImpl.java, v 0.1 2018年12月16日 3:16 PM JacobChengZhang Exp $
 */
public class DescriptionImpl implements Description {
    /**
     * changeable description
     */
    private ResourceBundle resourceBundle = null;

    @Override
    public void init() {
        checkLocaleAndGetResources();
    }

    @Override
    public void changeLocale() {
        // not implemented
    }

    @Override
    public String getText(String keyName) {
        return resourceBundle.getString(keyName);
    }

    private void checkLocaleAndGetResources() {
        Locale currentLocale = Locale.getDefault();
        if (currentLocale != Locale.SIMPLIFIED_CHINESE && currentLocale != Locale.US) {
            System.err.println("Sorry, but only Chinese and English are supported right now.");
            currentLocale = Locale.US;
        }
        resourceBundle = ResourceBundle.getBundle("description", currentLocale);
    }
}
