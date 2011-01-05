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
    <title><g:msg code="systemUser.list" default="User List"/></title>
</head>
<body>
<div class="nav">
    <span class="menuButton"><a class="home" href="${createLink(uri: '/')}"><g:msg code="home" default="Home"/></a></span>
    <span class="menuButton"><g:link class="menu" controller="systemMenu" action="display"><g:msg code="systemMenu.display" default="Menu"/></g:link></span>
    <span class="menuButton"><g:link class="create" action="create"><g:msg code="systemUser.new" default="New User"/></g:link></span>
</div>
<div class="body">
    <g:pageTitle code="systemUser.list" default="User List"/>
    <g:if test="${flash.message}">
        <div class="message"><g:msg code="${flash.message}" args="${flash.args}" default="${flash.defaultMessage}"/></div>
    </g:if>
    <div class="criteria">
        <g:criteria include="loginId, name, email, lastLogin, disabledUntil, nextPasswordChange, administrator"/>
    </div>
    <div class="list">
        <table>
            <thead>
            <tr>

                <g:sortableColumn property="loginId" title="Login Id" titleKey="systemUser.loginId"/>

                <g:sortableColumn property="name" title="Name" titleKey="systemUser.name"/>

                <g:sortableColumn property="email" title="Email" titleKey="systemUser.email"/>

                <th><g:msg code="systemUser.country" default="Country"/>

                <th><g:msg code="systemUser.language" default="Language"/>

                <g:sortableColumn property="lastLogin" title="Last Login" titleKey="systemUser.lastLogin"/>

                <g:sortableColumn property="disabledUntil" title="Disabled Until" titleKey="systemUser.disabledUntil"/>

                <g:sortableColumn property="nextPasswordChange" title="Next Password Change" titleKey="systemUser.nextPasswordChange"/>

                <g:sortableColumn property="administrator" title="Administrator" titleKey="systemUser.administrator"/>

                <th><g:msg code="systemUser.companies" default="Companies"/>

            </tr>
            </thead>
            <tbody>
            <g:each in="${systemUserInstanceList}" status="i" var="systemUserInstance">
                <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">

                    <td><g:link action="show" id="${systemUserInstance.id}">${display(bean: systemUserInstance, field: 'loginId')}</g:link></td>

                    <td>${display(bean: systemUserInstance, field: 'name')}</td>

                    <td>${display(bean: systemUserInstance, field: 'email')}</td>

                    <td><g:msg code="country.name.${systemUserInstance.country.code}" default="${systemUserInstance.country.name}"/></td>

                    <td><g:msg code="language.name.${systemUserInstance.language.code}" default="${systemUserInstance.language.name}"/></td>

                    <td>${display(bean: systemUserInstance, field: 'lastLogin', scale: 2)}</td>

                    <td>${display(bean: systemUserInstance, field: 'disabledUntil', scale: 2)}</td>

                    <td>${display(bean: systemUserInstance, field: 'nextPasswordChange', scale: 2)}</td>

                    <td>${display(bean: systemUserInstance, field: 'administrator')}</td>

                    <td><g:drilldown controller="companyUser" action="list" value="${systemUserInstance.id}"/></td>
                </tr>
            </g:each>
            </tbody>
        </table>
    </div>
    <div class="paginateButtons">
        <g:paginate total="${systemUserInstanceTotal}"/>
    </div>
</div>
</body>
</html>
