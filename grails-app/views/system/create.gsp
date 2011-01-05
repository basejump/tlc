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
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title><g:msg code="companyUser.create.title" default="Test Company Created"/></title>
</head>
<body>
<div class="body">
    <g:pageTitle code="companyUser.create.title" default="Test Company Created" help="sample"/>
    <g:if test="${flash.message}">
        <div class="message"><g:msg code="${flash.message}" args="${flash.args}" default="${flash.defaultMessage}"/></div>
    </g:if>
    <div style="margin-top:20px;">
        <p style="margin-bottom:20px;"><g:msg code="companyUser.create.text" default="Your test company has been created. Please read the notes below then press the Continue button to proceed to your company menu."/></p>
        <g:render template="general"/>
        <div align="center" style="margin-top:10px;">
            <g:form controller="systemMenu" action="display">
                <input type="submit" value="${msg(code: 'companyUser.assign.button', 'default': 'Continue')}"/>
            </g:form>
        </div>
    </div>
</div>
</body>
</html>
