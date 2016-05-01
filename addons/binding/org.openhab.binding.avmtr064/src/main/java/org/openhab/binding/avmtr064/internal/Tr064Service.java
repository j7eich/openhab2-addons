/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.avmtr064.internal;

import java.util.ArrayList;
import java.util.Iterator;

public class Tr064Service {
    private String serviceType;
    private String controlUrl;
    private String scpdUrl;
    private ArrayList<Tr064Action> listActions = new ArrayList<Tr064Action>();
    private ArrayList<Tr064Variable> listVariables = new ArrayList<Tr064Variable>();

    public void setServiceType(String sType) {
        this.serviceType = sType;
    }

    public String getServiceType() {
        return this.serviceType;
    }

    public void setControlUrl(String sUrl) {
        this.controlUrl = sUrl;
    }

    public String getControlUrl() {
        return this.controlUrl;
    }

    public String getScpdUrl() {
        return scpdUrl;
    }

    public void setScpdUrl(String scpdUrl) {
        this.scpdUrl = scpdUrl;
    }

    public void addAction(Tr064Action action) {
        this.listActions.add(action);
    }

    public Iterator<Tr064Action> getActions() {
        return this.listActions.iterator();
    }

    public void addVariable(Tr064Variable variable) {
        this.listVariables.add(variable);
    }

    public Iterator<Tr064Variable> getVariables() {
        return this.listVariables.iterator();
    }
}
