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
    <title><g:msg code="systemRole.list" default="Role List"/></title>
</head>
<body>
<div class="nav">
    <span class="menuButton"><a class="home" href="${createLink(uri: '/')}"><g:msg code="home" default="Home"/></a></span>
    <span class="menuButton"><g:link class="menu" controller="systemMenu" action="display"><g:msg code="systemMenu.display" default="Menu"/></g:link></span>
    <span class="menuButton"><g:link class="create" action="create"><g:msg code="systemRole.new" default="New System Role"/></g:link></span>
    <g:if test="${ddSource}">
        <span class="menuButton"><g:link class="links" action="links"><g:msg code="generic.define.links" default="Define Links"/></g:link></span>
    </g:if>
</div>
<div class="body">
    <g:if test="${ddSource}">
        <g:pageTitle code="systemRole.list.for" args="${[ddSource.user.name, ddSource.company.name]}" default="Role List for User ${ddSource.user.name} in Company ${ddSource.company.name}" returns="true"/>
    </g:if>
    <g:else>
        <g:pageTitle code="systemRole.list" default="Role List"/>
    </g:else>
    <g:if test="${flash.message}">
        <div class="message"><g:msg code="${flash.message}" args="${flash.args}" default="${flash.defaultMessage}"/></div>
    </g:if>
    <div class="criteria">
        <g:criteria include="code"/>
    </div>
    <div class="list">
        <table>
            <thead>
            <tr>

                <g:sortableColumn property="code" title="Code" titleKey="systemRole.code"/>

                <th><g:msg code="systemRole.name" default="Name"/></th>

                <th><g:msg code="systemRole.activities" default="Activities"/></th>

            </tr>
            </thead>
            <tbody>
            <g:each in="${systemRoleInstanceList}" status="i" var="systemRoleInstance">
                <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">

                    <td><g:link action="show" id="${systemRoleInstance.id}">${display(bean: systemRoleInstance, field: 'code')}</g:link></td>

                    <td><g:msg code="role.name.${systemRoleInstance.code}" default="${systemRoleInstance.name}"/></td>

                    <td><g:drilldown controller="systemActivity" action="list" value="${systemRoleInstance.id}"/></td>

                </tr>
            </g:each>
            </tbody>
        </table>
    </div>
    <div class="paginateButtons">
        <g:paginate total="${systemRoleInstanceTotal}"/>
    </div>
</div>
</body>
</html>