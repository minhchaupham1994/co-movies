/**
 Copyright 2014 Michael Meyer, mic@batdroid.de

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 This code originally comes from

 http://omtlab.com/android-enable-disable-wifi-programmatically/
 and
 http://omtlab.com/android-enable-disable-hotspot-programmatically/

 **/
package com.bighero2.comovies.externalcode;

import java.lang.reflect.Method;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * This class is use to handle all Wifi Hotspot related functions
 */
public class WifiApControl {
    private static final String TAG = "BDAccessPoint";                                              // tag for this debugging module

    // Methods we want to access by reflection
    private static Method getWifiApState;
    private static Method isWifiApEnabled;
    private static Method setWifiApEnabled;
    private static Method getWifiApConfiguration;
    private static Method setWifiApConfiguration;
    private WifiManager mgr;

    static {
        // lookup methods and fields not defined publicly in the SDK.
        Class<?> cls = WifiManager.class;
        for (Method method : cls.getDeclaredMethods()) {
            String methodName = method.getName();
            switch (methodName) {
                case "getWifiApState":
                    getWifiApState = method;
                    break;
                case "isWifiApEnabled":
                    isWifiApEnabled = method;
                    break;
                case "setWifiApEnabled":
                    setWifiApEnabled = method;
                    break;
                case "getWifiApConfiguration":
                    getWifiApConfiguration = method;
                    break;
                case "setWifiApConfiguration":
                    setWifiApConfiguration = method;
                    break;
            }
        }
    }

    /**
     * Constructor; saves the WifiManager.
     */
    public WifiApControl(WifiManager mgr) {
        this.mgr = mgr;
    }

    /**
     * Checks the availability of necessary all functions
     */
    public static boolean isApSupported() {
        return (getWifiApState != null && isWifiApEnabled != null
                && setWifiApEnabled != null && getWifiApConfiguration != null
                && setWifiApConfiguration != null);
    }

    /**
     * Build a new APControl
     */
    public static WifiApControl getApControl(WifiManager mgr) {
        if (!isApSupported())
            return null;
        return new WifiApControl(mgr);
    }

    /**
     * Checks if Wifi Access Point is enabled
     */

    public boolean isWifiApEnabled() {
        try {
            return (Boolean) isWifiApEnabled.invoke(mgr);
        } catch (Exception e) {
            Log.v(TAG, e.toString(), e); // shouldn't happen
            return false;
        }
    }
    /**
     * Checks the Wifi Access Point state
     */

    public int getWifiApState() {
        try {
            return (Integer) getWifiApState.invoke(mgr);
        } catch (Exception e) {
            Log.v(TAG, e.toString(), e); // shouldn't happen
            return -1;
        }
    }


    /**
     * Gets an WifiApConfiguration
     */
    public WifiConfiguration getWifiApConfiguration() {
        try {
            return (WifiConfiguration) getWifiApConfiguration.invoke(mgr);
        } catch (Exception e) {
            Log.v(TAG, e.toString(), e); // shouldn't happen
            return null;
        }
    }

    /**
     * Sets the Wi-Fi AP Configuration.
     *
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     */
    /* not used, yet
    public boolean setWifiApConfiguration(WifiConfiguration wifiConfig) {
        try {
            return (Boolean) setWifiApConfiguration.invoke(mgr, wifiConfig);
        } catch (Exception e) {
            Log.v(TAG, e.toString(), e); // shouldn't happen
            return false;
        }
    }
    */

    /**
     * Sets the Wi-Fi AP (tethering) enabled/disabled
     *
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     */
    public boolean setWifiApEnabled(WifiConfiguration config, boolean enabled) {
        try {
            return (Boolean) setWifiApEnabled.invoke(mgr, config, enabled);
        } catch (Exception e) {
            Log.v(TAG, e.toString(), e);                                                            // shouldn't happen
            return false;
        }
    }
}
