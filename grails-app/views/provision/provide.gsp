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
    <title><g:msg code="provision.entry" default="Accrual/Prepayment Entry"/></title>
    <g:yuiResources require="connection"/>
</head>
<body>
<div class="nav">
    <span class="menuButton"><a class="home" href="${createLink(uri: '/')}"><g:msg code="home" default="Home"/></a></span>
    <span class="menuButton"><g:link class="menu" controller="systemMenu" action="display"><g:msg code="systemMenu.display" default="Menu"/></g:link></span>
    <span class="menuButton"><g:link class="list" controller="document" action="templates" params="${[ctrl: 'provision', types: 'AC,PR']}"><g:msg code="templateDocument.load" default="Load Template"/></g:link></span>
</div>
<div class="body">
    <g:pageTitle code="provision.entry" default="Accrual/Prepayment Entry"/>
    <g:if test="${flash.message}">
        <div class="message"><g:msg code="${flash.message}" args="${flash.args}" default="${flash.defaultMessage}"/></div>
    </g:if>
    <g:hasErrors bean="${documentInstance}">
        <div class="errors">
            <g:listErrors bean="${documentInstance}"/>
        </div>
    </g:hasErrors>
    <div id="ajaxErrorMessage" class="errors" style="visibility:hidden;"></div>
    <g:form method="post">
        <input type="hidden" id="responseMessage" name="responseMessage" value="${msg(code: 'generic.ajax.response', default: 'Unable to understand the response from the server')}"/>
        <input type="hidden" id="timeoutMessage" name="timeoutMessage" value="${msg(code: 'generic.ajax.timeout', default: 'Operation timed out waiting for a response from the server')}"/>
        <input type="hidden" id="sourceNumber" name="sourceNumber" value="${display(bean: documentInstance, field: 'code')}"/>
        <div class="dialog">
            <table>
                <tbody>

                <tr class="prop">
                    <td valign="top" class="name">
                        <label for="type.id"><g:msg code="document.type" default="Document Type"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'type', 'errors')}">
                        <g:domainSelect initialField="true" onchange="typeChanged()" onblur="getCode(this, '${createLink(controller: 'document', action: 'code')}', 'reference')" name="type.id" options="${documentTypeList}" selected="${documentInstance?.type}" displays="${['code', 'name']}" noSelection="['null': msg(code: 'generic.select', default: '-- select --')]"/>&nbsp;<g:help code="document.type"/>
                    </td>

                    <td valign="top" class="name">
                        <label for="code"><g:msg code="document.provision.code" default="Code"/>:</label>
                    </td>
                    <td valign="top" colspan="3" class="small-value ${hasErrors(bean: documentInstance, field: 'code', 'errors')}">
                        <input ${settings.codeEdit ? '' : 'disabled="true"'} type="text" maxlength="10" size="10" id="code" name="code" value="${display(bean: documentInstance, field: 'code')}"/>&nbsp;<g:help code="document.provision.code"/>
                    </td>
                </tr>

                <tr class="prop">
                    <td valign="top" class="name">
                        <label for="reference"><g:msg code="document.journal.reference" default="Reference"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'reference', 'errors')}">
                        <input type="text" maxLength="30" size="30" id="reference" name="reference" value="${display(bean: documentInstance, field: 'reference')}"/>&nbsp;<g:help code="document.journal.reference"/>
                    </td>

                    <td valign="top" class="name">
                        <label for="description"><g:msg code="document.description" default="Description"/>:</label>
                    </td>
                    <td valign="top" colspan="3" class="small-value ${hasErrors(bean: documentInstance, field: 'description', 'errors')}">
                        <input type="text" maxlength="50" size="45" id="description" name="description" value="${display(bean: documentInstance, field: 'description')}"/>&nbsp;<g:help code="document.description"/>
                    </td>
                </tr>

                <tr class="prop">
                    <td valign="top" class="name">
                        <label for="documentDate"><g:msg code="document.provisionDate" default="Document Date"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'documentDate', 'errors')}">
                        <input type="text" onchange="getPeriod(this, '${createLink(controller: 'document', action: 'period')}', '', true)" size="20" id="documentDate" name="documentDate" value="${display(bean: documentInstance, field: 'documentDate', scale: 1)}"/>&nbsp;<g:help code="document.provisionDate"/>
                    </td>

                    <td valign="top" class="name">
                        <label for="sourceAdjustment"><g:msg code="generalTransaction.adjustment" default="Adjustment"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'sourceAdjustment', 'errors')}">
                        <g:checkBox name="sourceAdjustment" value="${documentInstance?.sourceAdjustment}"></g:checkBox>&nbsp;<g:help code="generalTransaction.adjustment"/>
                    </td>

                    <td valign="top" class="name">
                        <label for="currency"><g:msg code="document.currency" default="Currency"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'currency', 'errors')}">
                        <g:domainSelect name="currency.id" options="${currencyList}" selected="${documentInstance?.currency}" prefix="currency.name" code="code" default="name"/>&nbsp;<g:help code="document.currency"/>
                    </td>
                </tr>

                <tr class="prop">
                    <td valign="top" class="name">
                        <label for="period.id"><g:msg code="document.period" default="Accounting Period"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'period', 'errors')}">
                        <g:select optionKey="id" optionValue="code" from="${periodList}" name="period.id" value="${documentInstance?.period?.id}"/>&nbsp;<g:help code="document.period"/>
                    </td>

                    <td></td>
                    <td></td>

                    <td valign="top" class="name">
                        <label for="sourceTotal"><g:msg code="document.docTotal" default="Total"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'sourceTotal', 'errors')}">
                        <input type="text" size="12" id="sourceTotal" name="sourceTotal" value="${display(bean: documentInstance, field: 'sourceTotal', scale: settings.decimals)}"/>&nbsp;<g:help code="document.docTotal"/>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
        <div class="list entry">
            <table>
                <thead>
                <tr>
                    <th><g:msg code="document.line.accountCode" default="Account"/>&nbsp;<g:help code="document.line.accountCode"/></th>
                    <th><g:msg code="document.line.description" default="Description"/>&nbsp;<g:help code="document.line.description"/></th>
                    <th><g:msg code="document.value" default="Value"/>&nbsp;<g:help code="document.value"/></th>
                </tr>
                </thead>
                <tbody>
                <g:each in="${documentInstance.lines}" status="i" var="documentLine">
                    <tr>
                        <td class="narrow ${hasErrors(bean: documentLine, field: 'accountCode', 'errors')}">
                            <input type="text" maxLength="87" size="30" onchange="getAccount(this, '${createLink(controller: 'document', action: 'account')}', 'provision')" id="lines[${i}].accountCode" name="lines[${i}].accountCode" value="${display(bean: documentLine, field: 'accountCode')}"/>
                        </td>
                        <td class="narrow"><input type="text" maxLength="50" size="40" id="lines[${i}].description" name="lines[${i}].description" value="${display(bean: documentLine, field: 'description')}"/></td>
                        <td class="narrow"><input type="text" size="12" id="lines[${i}].documentValue" name="lines[${i}].documentValue" value="${display(bean: documentLine, field: 'documentValue', scale: settings.decimals)}"/></td>
                    </tr>
                    <tr>
                        <td class="narrow" colspan="2" style="padding-bottom:12px;"><input disabled="true" type="text" size="76" id="lines[${i}].displayName" name="lines[${i}].displayName" value="${display(bean: documentLine, field: 'accountName')}"/></td>
                        <td class="narrow"><input type="hidden" id="lines[${i}].accountName" name="lines[${i}].accountName" value="${display(bean: documentLine, field: 'accountName')}"/></td>
                    </tr>
                </g:each>
                </tbody>
            </table>
        </div>
        <div class="buttons">
            <span class="button"><g:actionSubmit class="save" action="providing" value="${msg(code:'create', 'default':'Create')}"/></span>
            <span class="button"><g:actionSubmit class="edit" action="lines" value="${msg(code:'document.more.lines', 'default':'More Lines')}"/></span>
        </div>
    </g:form>
</div>
</body>
</html>
