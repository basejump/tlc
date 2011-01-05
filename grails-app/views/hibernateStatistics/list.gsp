<%--
 ~  Copyright 2010 Wholly Grails.
 ~
 ~  This file is part of the Three Ledger Core (TLC) software
 ~  from Wholly Grails.
 ~
 ~  TLC is free software: you can redistribute it and/or modify
 ~  it under the terms of the GNU General Public License as published by
 ~  the Free Software Foundation, either version 3 of the License, or
 ~  (at your option) any later version.
 ~
 ~  TLC is distributed in the hope that it will be useful,
 ~  but WITHOUT ANY WARRANTY; without even the implied warranty of
 ~  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 ~  GNU General Public License for more details.
 ~
 ~  You should have received a copy of the GNU General Public License
 ~  along with TLC.  If not, see <http://www.gnu.org/licenses/>.
 --%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="bodyClass" content="system"/>
    <title><g:msg code="hibernate.statistics" default="Hibernate Statistics"/></title>
</head>
<body>
<div class="nav">
    <span class="menuButton"><a class="home" href="${createLink(uri: '/')}"><g:msg code="home" default="Home"/></a></span>
    <span class="menuButton"><g:link class="menu" controller="systemMenu" action="display"><g:msg code="systemMenu.display" default="Menu"/></g:link></span>
</div>
<div class="body">
    <g:pageTitle code="hibernate.statistics" default="Hibernate Statistics"/>
    <g:if test="${flash.message}">
        <div class="message"><g:msg code="${flash.message}" args="${flash.args}" default="${flash.defaultMessage}"/></div>
    </g:if>
    <g:if test="${enabled}">
        <g:form action="disable" method="post">
            <div align="center" style="margin-top:15px;margin-bottom:15px;">
                <g:msg code="hibernate.enabled" default="Hibernate Statistics are currently switched on. Click the Disable button to turn off the gathering of statistics."/>
            </div>
            <g:each in="${tables}" var="table">
                <h2>${table.name}</h2>
                <table>
                <g:each in="${table.rows}" status="i" var="row">
                    <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        <td>${row.name}</td>
                        <td>${row.value}</td>
                    </tr>
                </g:each>
                </table>
            </g:each>
            <div class="buttons">
                <span class="button"><input class="save" type="submit" value="${msg(code: 'hibernate.disable', 'default': 'Disable')}"/></span>
            </div>
        </g:form>
    </g:if>
    <g:else>
        <g:form action="enable" method="post">
            <div align="center" style="margin-top:15px;margin-bottom:15px;">
                <g:msg code="hibernate.disabled" default="Hibernate Statistics are currently switched off. Click the Enable button to turn on the gathering of statistics."/>
            </div>
            <div class="buttons">
                <span class="button"><input class="save" type="submit" value="${msg(code: 'hibernate.enable', 'default': 'Enable')}"/></span>
            </div>
        </g:form>
    </g:else>
</div>
</body>
</html>
