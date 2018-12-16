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
     *
     * Only Chinese and English are supported
     */
    private ResourceBundle resourceBundle = null;

    private Locale         currentLocale  = Locale.US;

    @Override
    public void init() {
        checkLocaleAndGetResources();
    }

    @Override
    public void changeLocale() {
        if (currentLocale.equals(Locale.US)) {
            currentLocale = Locale.SIMPLIFIED_CHINESE;
        } else {
            currentLocale = Locale.US;
        }
        resourceBundle = ResourceBundle.getBundle("description", currentLocale);
    }

    @Override
    public String getText(String keyName) {
        return resourceBundle.getString(keyName);
    }

    private void checkLocaleAndGetResources() {
        currentLocale = Locale.getDefault();
        if (!currentLocale.equals(Locale.SIMPLIFIED_CHINESE) && !currentLocale.equals(Locale.US)) {
            System.err.println("Sorry, but only Chinese and English are supported right now.");
            currentLocale = Locale.US;
        }
        resourceBundle = ResourceBundle.getBundle("description", currentLocale);
    }
}
