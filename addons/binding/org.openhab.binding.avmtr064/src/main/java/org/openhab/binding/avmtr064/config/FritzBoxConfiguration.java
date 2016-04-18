/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.avmtr064.config;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Class holding configuration data according for FritzBox device
 *
 * @author Jan Siebeneich
 *
 */
public class FritzBoxConfiguration {
    private String ipAddress;
    private Integer port;

    private String user;
    private String password;

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getPort() {
        return this.port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUser() {
        return this.user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String pwd) {
        this.password = pwd;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("IP", this.getIpAddress()).append("port", this.getPort())
                // .append("proto", this.getProtocol())
                .append("user", this.getUser()).append("password", this.getPassword())
                // .append("pollingInterval", this.getPollingInterval())
                // .append("asyncTimeout", this.getAsyncTimeout())
                // .append("syncTimeout", this.getSyncTimeout())
                .toString();
    }
}
