/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.avmtr064.internal;

public class Tr064Argument {

    private String name;
    private String associatedVariable;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAssociatedVariable() {
        return associatedVariable;
    }

    public void setAssociatedVariable(String associatedVariable) {
        this.associatedVariable = associatedVariable;
    }
}
