/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.avmtr064.internal;

import java.util.Iterator;
import java.util.List;

public class Tr064Action {

    private String name;
    private List<Tr064Argument> argumentsIn;
    private List<Tr064Argument> argumentsOut;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Iterator<Tr064Argument> getArgumentsIn() {
        return argumentsIn.iterator();
    }

    public void addArgumentIn(Tr064Argument argumentIn) {
        this.argumentsIn.add(argumentIn);
    }

    public Iterator<Tr064Argument> getArgumentsOut() {
        return argumentsOut.iterator();
    }

    public void addArgumentOut(Tr064Argument argumentOut) {
        this.argumentsOut.add(argumentOut);
    }
}
